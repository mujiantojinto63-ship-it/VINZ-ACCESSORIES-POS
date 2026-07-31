package com.example.repository

import com.example.data.dao.CategoryDao
import com.example.data.dao.CustomerDao
import com.example.data.dao.JournalDao
import com.example.data.dao.ProductDao
import com.example.data.dao.PurchaseOrderDao
import com.example.data.dao.SettingsDao
import com.example.data.dao.TransactionDao
import com.example.data.dao.UserDao
import com.example.data.model.CartItem
import com.example.data.model.CategoryEntity
import com.example.data.model.Customer
import com.example.data.model.JournalEntry
import com.example.data.model.JournalType
import com.example.data.model.POStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.PriceLevel
import com.example.data.model.Product
import com.example.data.model.PurchaseOrderEntity
import com.example.data.model.PurchaseOrderItem
import com.example.data.model.StoreSettings
import com.example.data.model.TransactionEntity
import com.example.data.model.UserEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PosRepository(
    private val productDao: ProductDao,
    private val customerDao: CustomerDao,
    private val transactionDao: TransactionDao,
    private val poDao: PurchaseOrderDao,
    private val journalDao: JournalDao,
    private val settingsDao: SettingsDao,
    private val userDao: UserDao,
    private val categoryDao: CategoryDao
) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val cartListType = Types.newParameterizedType(List::class.java, CartItem::class.java)
    private val cartAdapter = moshi.adapter<List<CartItem>>(cartListType)
    private val poListType = Types.newParameterizedType(List::class.java, PurchaseOrderItem::class.java)
    private val poAdapter = moshi.adapter<List<PurchaseOrderItem>>(poListType)

    // Flow streams
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val allCustomers: Flow<List<Customer>> = customerDao.getAllCustomers()
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allPurchaseOrders: Flow<List<PurchaseOrderEntity>> = poDao.getAllPurchaseOrders()
    val allJournalEntries: Flow<List<JournalEntry>> = journalDao.getAllJournalEntries()
    val storeSettings: Flow<StoreSettings?> = settingsDao.getStoreSettings()
    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers()
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    // Users
    suspend fun saveUser(user: UserEntity): Long = userDao.insertUser(user)
    suspend fun updateUser(user: UserEntity) = userDao.updateUser(user)
    suspend fun deleteUser(id: Long) = userDao.deleteUser(id)
    suspend fun getUserByUsername(username: String): UserEntity? = userDao.getUserByUsername(username)

    // Categories
    suspend fun saveCategory(category: CategoryEntity): Long = categoryDao.insertCategory(category)
    suspend fun updateCategory(category: CategoryEntity) = categoryDao.updateCategory(category)
    suspend fun deleteCategory(id: Long) = categoryDao.deleteCategory(id)

    // Products
    suspend fun getProductByBarcode(barcode: String): Product? = productDao.getProductByBarcode(barcode)
    suspend fun saveProduct(product: Product): Long = productDao.insertProduct(product)
    suspend fun updateProduct(product: Product) = productDao.updateProduct(product)
    suspend fun deleteProduct(id: Long) = productDao.deleteProduct(id)

    // Customers
    suspend fun saveCustomer(customer: Customer): Long = customerDao.insertCustomer(customer)
    suspend fun updateCustomer(customer: Customer) = customerDao.updateCustomer(customer)
    suspend fun deleteCustomer(id: Long) = customerDao.deleteCustomer(id)
    suspend fun payCustomerDebt(customerId: Long, amount: Double, notes: String) {
        val cust = customerDao.getCustomerById(customerId) ?: return
        val newDebt = (cust.totalDebt - amount).coerceAtLeast(0.0)
        customerDao.updateCustomerDebt(customerId, -amount)
        journalDao.insertJournal(
            JournalEntry(
                type = JournalType.INCOME,
                category = "Pelunasan Piutang",
                amount = amount,
                description = "Pelunasan piutang pelanggan: ${cust.name} ($notes)",
                refId = "CUST-$customerId"
            )
        )
    }

    // Checkout / Process Sales Transaction
    suspend fun processTransaction(
        customer: Customer?,
        priceLevel: PriceLevel,
        cartItems: List<CartItem>,
        paymentMethod: PaymentMethod,
        paymentStatus: PaymentStatus,
        amountPaid: Double,
        discount: Double,
        notes: String
    ): TransactionEntity {
        val subtotal = cartItems.sumOf { it.totalPrice }
        val total = (subtotal - discount).coerceAtLeast(0.0)
        val change = if (paymentMethod == PaymentMethod.TUNAI && paymentStatus == PaymentStatus.LUNAS) {
            (amountPaid - total).coerceAtLeast(0.0)
        } else {
            0.0
        }

        val receiptNo = "VINZ-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())
        val itemsJsonString = cartAdapter.toJson(cartItems)

        val transaction = TransactionEntity(
            receiptNo = receiptNo,
            customerId = customer?.id,
            customerName = customer?.name ?: "Pelanggan Umum",
            priceLevelUsed = priceLevel,
            subtotal = subtotal,
            discount = discount,
            total = total,
            paymentMethod = paymentMethod,
            paymentStatus = paymentStatus,
            amountPaid = amountPaid,
            change = change,
            notes = notes,
            itemsJson = itemsJsonString
        )

        val transId = transactionDao.insertTransaction(transaction)

        // Reduce stock for products
        cartItems.forEach { item ->
            productDao.updateStock(item.productId, -item.quantity)
        }

        // If Belum Lunas (Debt / Credit), update Customer's totalDebt
        val unpaidAmount = total - amountPaid
        if (paymentStatus == PaymentStatus.BELUM_LUNAS && customer != null && unpaidAmount > 0) {
            customerDao.updateCustomerDebt(customer.id, unpaidAmount)
        }

        // Record Journal Entry
        val journalAmount = if (paymentStatus == PaymentStatus.LUNAS) total else amountPaid
        if (journalAmount > 0) {
            journalDao.insertJournal(
                JournalEntry(
                    type = JournalType.INCOME,
                    category = "Penjualan Kasir",
                    amount = journalAmount,
                    description = "Penjualan #$receiptNo (${paymentMethod.displayName} - ${paymentStatus.displayName})",
                    refId = receiptNo
                )
            )
        }

        return transaction.copy(id = transId)
    }

    // Purchase Orders (PO)
    suspend fun createPurchaseOrder(
        supplierName: String,
        supplierPhone: String,
        items: List<PurchaseOrderItem>,
        notes: String
    ): Long {
        val totalCost = items.sumOf { it.subtotal }
        val poNo = "PO-" + SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date())
        val itemsJsonStr = poAdapter.toJson(items)

        val po = PurchaseOrderEntity(
            poNumber = poNo,
            supplierName = supplierName,
            supplierPhone = supplierPhone,
            status = POStatus.PENDING,
            totalCost = totalCost,
            itemsJson = itemsJsonStr,
            notes = notes
        )
        return poDao.insertPO(po)
    }

    suspend fun receivePurchaseOrder(po: PurchaseOrderEntity) {
        if (po.status == POStatus.DITERIMA) return

        val items = poAdapter.fromJson(po.itemsJson) ?: emptyList()
        // Increase stock for each item in PO
        items.forEach { item ->
            productDao.updateStock(item.productId, item.quantity)
        }

        // Update PO status
        poDao.updatePO(po.copy(status = POStatus.DITERIMA))

        // Log in Journal as Expense
        journalDao.insertJournal(
            JournalEntry(
                type = JournalType.EXPENSE,
                category = "Pembelian Stok (PO)",
                amount = po.totalCost,
                description = "Pembelian Stok ${po.poNumber} dari ${po.supplierName}",
                refId = po.poNumber
            )
        )
    }

    // Journal Entry Manual
    suspend fun addJournalEntry(type: JournalType, category: String, amount: Double, description: String) {
        journalDao.insertJournal(
            JournalEntry(
                type = type,
                category = category,
                amount = amount,
                description = description
            )
        )
    }

    // Settings
    suspend fun updateStoreSettings(settings: StoreSettings) {
        settingsDao.saveStoreSettings(settings)
    }

    // CSV Export & Import
    suspend fun exportProductsToCsv(products: List<Product>): String {
        val sb = StringBuilder()
        sb.append("Barcode,Nama,Kategori,Stok,MinStok,HargaBeli,Eceran,Grosir,Reseller,VIP\n")
        products.forEach { p ->
            sb.append("${p.barcode},\"${p.name.replace("\"", "\"\"")}\",\"${p.category}\",${p.stock},${p.minStockAlert},${p.costPrice.toLong()},${p.priceEceran.toLong()},${p.priceGrosir.toLong()},${p.priceReseller.toLong()},${p.priceVip.toLong()}\n")
        }
        return sb.toString()
    }

    suspend fun importProductsFromCsv(csvText: String): Int {
        var importedCount = 0
        val lines = csvText.lines()
        val productsToSave = mutableListOf<Product>()

        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue
            val tokens = parseCsvLine(line)
            if (tokens.size >= 7) {
                try {
                    val barcode = tokens[0].trim()
                    val name = tokens[1].trim()
                    val category = tokens.getOrNull(2)?.trim()?.ifEmpty { "Aksesoris" } ?: "Aksesoris"
                    val stock = tokens.getOrNull(3)?.trim()?.toIntOrNull() ?: 0
                    val minStock = tokens.getOrNull(4)?.trim()?.toIntOrNull() ?: 5
                    val cost = tokens.getOrNull(5)?.trim()?.toDoubleOrNull() ?: 0.0
                    val eceran = tokens.getOrNull(6)?.trim()?.toDoubleOrNull() ?: 0.0
                    val grosir = tokens.getOrNull(7)?.trim()?.toDoubleOrNull() ?: eceran
                    val reseller = tokens.getOrNull(8)?.trim()?.toDoubleOrNull() ?: eceran
                    val vip = tokens.getOrNull(9)?.trim()?.toDoubleOrNull() ?: eceran

                    if (barcode.isNotEmpty() && name.isNotEmpty()) {
                        productsToSave.add(
                            Product(
                                barcode = barcode,
                                name = name,
                                category = category,
                                stock = stock,
                                minStockAlert = minStock,
                                costPrice = cost,
                                priceEceran = eceran,
                                priceGrosir = grosir,
                                priceReseller = reseller,
                                priceVip = vip
                            )
                        )
                        importedCount++
                    }
                } catch (_: Exception) {}
            }
        }

        if (productsToSave.isNotEmpty()) {
            productDao.insertProducts(productsToSave)
        }
        return importedCount
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            if (ch == '"') {
                inQuotes = !inQuotes
            } else if (ch == ',' && !inQuotes) {
                result.add(cur.toString())
                cur.clear()
            } else {
                cur.append(ch)
            }
        }
        result.add(cur.toString())
        return result
    }

    fun parseCartItems(itemsJson: String): List<CartItem> {
        return try {
            cartAdapter.fromJson(itemsJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun parsePOItems(itemsJson: String): List<PurchaseOrderItem> {
        return try {
            poAdapter.fromJson(itemsJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
