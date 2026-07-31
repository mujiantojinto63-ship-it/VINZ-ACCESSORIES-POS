package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.CartItem
import com.example.data.model.Customer
import com.example.data.model.JournalEntry
import com.example.data.model.JournalType
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.PriceLevel
import com.example.data.model.Product
import com.example.data.model.PurchaseOrderEntity
import com.example.data.model.PurchaseOrderItem
import com.example.data.model.StoreSettings
import com.example.data.model.TransactionEntity
import com.example.repository.PosRepository
import com.example.data.model.CategoryEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class MainTab {
    KASIR,
    LAPORAN_SALES,
    PRODUK_STOK,
    KATEGORI,
    PELANGGAN,
    PENGGUNA,
    JURNAL_PO,
    STRUK_PRINTER
}

enum class DateRangeFilter(val displayName: String) {
    HARI_INI("Hari Ini"),
    TUJUH_HARI("7 Hari Terakhir"),
    BULAN_INI("Bulan Ini"),
    SEMUA("Semua Data")
}

class PosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PosRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = PosRepository(
            db.productDao(),
            db.customerDao(),
            db.transactionDao(),
            db.purchaseOrderDao(),
            db.journalDao(),
            db.settingsDao(),
            db.userDao(),
            db.categoryDao()
        )
    }

    // Active Navigation Tab
    private val _selectedTab = MutableStateFlow(MainTab.KASIR)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    fun selectTab(tab: MainTab) {
        _selectedTab.value = tab
    }

    // Flows from Repository
    val products: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchaseOrders: StateFlow<List<PurchaseOrderEntity>> = repository.allPurchaseOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val journalEntries: StateFlow<List<JournalEntry>> = repository.allJournalEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val storeSettings: StateFlow<StoreSettings?> = repository.storeSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val users: StateFlow<List<UserEntity>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Logged-in User Session State
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    init {
        viewModelScope.launch {
            users.collect { userList ->
                if (_currentUser.value == null && userList.isNotEmpty()) {
                    // Default to first Admin or first available user
                    _currentUser.value = userList.find { it.role == UserRole.ADMIN } ?: userList.first()
                }
            }
        }
    }

    fun switchCurrentUser(user: UserEntity) {
        _currentUser.value = user
        _userMessage.value = "Berhasil masuk sebagai ${user.fullName} (${user.role.displayName})"
    }

    fun saveUser(user: UserEntity) {
        viewModelScope.launch {
            repository.saveUser(user)
            _userMessage.value = "Pengguna '${user.fullName}' berhasil disimpan!"
        }
    }

    fun deleteUser(userId: Long) {
        viewModelScope.launch {
            val userToDelete = users.value.find { it.id == userId }
            if (userToDelete?.id == _currentUser.value?.id) {
                _userMessage.value = "Tidak dapat menghapus akun yang sedang aktif!"
                return@launch
            }
            repository.deleteUser(userId)
            _userMessage.value = "Pengguna berhasil dihapus!"
        }
    }

    fun updateUserPin(userId: Long, oldPin: String, newPin: String): Boolean {
        val targetUser = users.value.find { it.id == userId } ?: return false
        if (targetUser.pin != oldPin && oldPin.isNotEmpty()) {
            _userMessage.value = "PIN lama salah!"
            return false
        }
        viewModelScope.launch {
            val updatedUser = targetUser.copy(pin = newPin)
            repository.updateUser(updatedUser)
            if (_currentUser.value?.id == userId) {
                _currentUser.value = updatedUser
            }
            _userMessage.value = "PIN untuk ${targetUser.fullName} berhasil diperbarui!"
        }
        return true
    }

    // Category CRUD
    fun saveCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.saveCategory(category)
            _userMessage.value = "Kategori '${category.name}' berhasil disimpan!"
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.updateCategory(category)
            _userMessage.value = "Kategori '${category.name}' diperbarui!"
        }
    }

    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch {
            val catToDelete = categories.value.find { it.id == categoryId }
            if (catToDelete != null) {
                repository.deleteCategory(categoryId)
                _userMessage.value = "Kategori '${catToDelete.name}' dihapus!"
            }
        }
    }

    // Sales Report Filter & States
    private val _salesDateFilter = MutableStateFlow(DateRangeFilter.HARI_INI)
    val salesDateFilter: StateFlow<DateRangeFilter> = _salesDateFilter.asStateFlow()

    fun setSalesDateFilter(filter: DateRangeFilter) {
        _salesDateFilter.value = filter
    }

    val filteredTransactionsForReport: StateFlow<List<TransactionEntity>> = combine(
        transactions,
        _salesDateFilter
    ) { transList, filter ->
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        when (filter) {
            DateRangeFilter.HARI_INI -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val startOfDay = cal.timeInMillis
                transList.filter { it.timestamp >= startOfDay }
            }
            DateRangeFilter.TUJUH_HARI -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val startOf7Days = cal.timeInMillis
                transList.filter { it.timestamp >= startOf7Days }
            }
            DateRangeFilter.BULAN_INI -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val startOfMonth = cal.timeInMillis
                transList.filter { it.timestamp >= startOfMonth }
            }
            DateRangeFilter.SEMUA -> transList
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // POS Cart State
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _selectedCustomer = MutableStateFlow<Customer?>(null)
    val selectedCustomer: StateFlow<Customer?> = _selectedCustomer.asStateFlow()

    private val _activePriceLevel = MutableStateFlow(PriceLevel.ECERAN)
    val activePriceLevel: StateFlow<PriceLevel> = _activePriceLevel.asStateFlow()

    private val _discount = MutableStateFlow(0.0)
    val discount: StateFlow<Double> = _discount.asStateFlow()

    private val _cartNotes = MutableStateFlow("")
    val cartNotes: StateFlow<String> = _cartNotes.asStateFlow()

    // Barcode & Search Filters
    private val _productSearchQuery = MutableStateFlow("")
    val productSearchQuery: StateFlow<String> = _productSearchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow("Semua")
    val selectedCategoryFilter: StateFlow<String> = _selectedCategoryFilter.asStateFlow()

    val filteredProducts: StateFlow<List<Product>> = combine(
        products,
        _productSearchQuery,
        _selectedCategoryFilter
    ) { list, query, category ->
        list.filter { p ->
            val matchesQuery = query.isEmpty() || p.name.contains(query, ignoreCase = true) || p.barcode.contains(query, ignoreCase = true)
            val matchesCategory = category == "Semua" || p.category.equals(category, ignoreCase = true)
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Checkout / Transaction View
    private val _completedTransaction = MutableStateFlow<TransactionEntity?>(null)
    val completedTransaction: StateFlow<TransactionEntity?> = _completedTransaction.asStateFlow()

    // Status Messages / Toast / Feedback
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun clearUserMessage() { _userMessage.value = null }

    // Cart Management
    fun selectCustomer(customer: Customer?) {
        _selectedCustomer.value = customer
        if (customer != null) {
            _activePriceLevel.value = customer.defaultPriceLevel
            // Re-calculate cart items unit prices according to new customer price level
            _cartItems.value = _cartItems.value.map { item ->
                val prod = products.value.find { it.id == item.productId }
                val newPrice = prod?.getPriceForLevel(customer.defaultPriceLevel) ?: item.unitPrice
                item.copy(selectedPriceLevel = customer.defaultPriceLevel, unitPrice = newPrice)
            }
        }
    }

    fun setPriceLevel(priceLevel: PriceLevel) {
        _activePriceLevel.value = priceLevel
        _cartItems.value = _cartItems.value.map { item ->
            val prod = products.value.find { it.id == item.productId }
            val newPrice = prod?.getPriceForLevel(priceLevel) ?: item.unitPrice
            item.copy(selectedPriceLevel = priceLevel, unitPrice = newPrice)
        }
    }

    fun setDiscount(amount: Double) {
        _discount.value = amount.coerceAtLeast(0.0)
    }

    fun setCartNotes(notes: String) {
        _cartNotes.value = notes
    }

    fun addToCart(product: Product, quantity: Int = 1) {
        val currentLevel = _activePriceLevel.value
        val unitPrice = product.getPriceForLevel(currentLevel)
        val existingIndex = _cartItems.value.indexOfFirst { it.productId == product.id && it.selectedPriceLevel == currentLevel }

        if (existingIndex >= 0) {
            val updated = _cartItems.value.toMutableList()
            val oldItem = updated[existingIndex]
            updated[existingIndex] = oldItem.copy(quantity = oldItem.quantity + quantity)
            _cartItems.value = updated
        } else {
            val newItem = CartItem(
                productId = product.id,
                productName = product.name,
                barcode = product.barcode,
                selectedPriceLevel = currentLevel,
                unitPrice = unitPrice,
                quantity = quantity,
                costPrice = product.costPrice
            )
            _cartItems.value = _cartItems.value + newItem
        }
    }

    fun updateCartQuantity(productId: Long, quantity: Int) {
        if (quantity <= 0) {
            _cartItems.value = _cartItems.value.filterNot { it.productId == productId }
        } else {
            _cartItems.value = _cartItems.value.map {
                if (it.productId == productId) it.copy(quantity = quantity) else it
            }
        }
    }

    fun removeFromCart(productId: Long) {
        _cartItems.value = _cartItems.value.filterNot { it.productId == productId }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _selectedCustomer.value = null
        _activePriceLevel.value = PriceLevel.ECERAN
        _discount.value = 0.0
        _cartNotes.value = ""
    }

    fun scanBarcodeAndAddToCart(barcode: String): Boolean {
        val foundProduct = products.value.find { it.barcode.trim() == barcode.trim() }
        return if (foundProduct != null) {
            addToCart(foundProduct, 1)
            _userMessage.value = "Berhasil menambahkan: ${foundProduct.name}"
            true
        } else {
            _userMessage.value = "Produk barcode $barcode tidak ditemukan!"
            false
        }
    }

    fun updateSearchQuery(query: String) { _productSearchQuery.value = query }
    fun setCategoryFilter(category: String) { _selectedCategoryFilter.value = category }

    // Checkout
    fun processCheckout(
        paymentMethod: PaymentMethod,
        paymentStatus: PaymentStatus,
        amountPaid: Double
    ) {
        if (_cartItems.value.isEmpty()) {
            _userMessage.value = "Keranjang belanja masih kosong!"
            return
        }

        viewModelScope.launch {
            try {
                val transaction = repository.processTransaction(
                    customer = _selectedCustomer.value,
                    priceLevel = _activePriceLevel.value,
                    cartItems = _cartItems.value,
                    paymentMethod = paymentMethod,
                    paymentStatus = paymentStatus,
                    amountPaid = amountPaid,
                    discount = _discount.value,
                    notes = _cartNotes.value
                )
                _completedTransaction.value = transaction
                clearCart()
                _userMessage.value = "Transaksi #${transaction.receiptNo} Berhasil Disimpan!"
            } catch (e: Exception) {
                _userMessage.value = "Gagal memproses transaksi: ${e.localizedMessage}"
            }
        }
    }

    fun dismissReceipt() {
        _completedTransaction.value = null
    }

    fun setTransactionForReceipt(transaction: TransactionEntity) {
        _completedTransaction.value = transaction
    }

    // Product CRUD
    fun saveProduct(product: Product) {
        viewModelScope.launch {
            repository.saveProduct(product)
            _userMessage.value = "Produk '${product.name}' berhasil disimpan!"
        }
    }

    fun deleteProduct(productId: Long) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
            _userMessage.value = "Produk dihapus!"
        }
    }

    // Customer CRUD
    fun saveCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.saveCustomer(customer)
            _userMessage.value = "Pelanggan '${customer.name}' berhasil disimpan!"
        }
    }

    fun deleteCustomer(customerId: Long) {
        viewModelScope.launch {
            repository.deleteCustomer(customerId)
            _userMessage.value = "Pelanggan dihapus!"
        }
    }

    fun payDebt(customerId: Long, amount: Double, notes: String) {
        viewModelScope.launch {
            repository.payCustomerDebt(customerId, amount, notes)
            _userMessage.value = "Pembayaran piutang berhasil dicatat!"
        }
    }

    // CSV Import / Export
    fun exportProductsCsv(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val csv = repository.exportProductsToCsv(products.value)
            onResult(csv)
        }
    }

    fun importProductsCsv(csvText: String) {
        viewModelScope.launch {
            try {
                val count = repository.importProductsFromCsv(csvText)
                _userMessage.value = "Berhasil mengimpor $count produk dari CSV!"
            } catch (e: Exception) {
                _userMessage.value = "Gagal impor CSV: ${e.localizedMessage}"
            }
        }
    }

    // Purchase Order
    fun createPO(supplierName: String, supplierPhone: String, items: List<PurchaseOrderItem>, notes: String) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            repository.createPurchaseOrder(supplierName, supplierPhone, items, notes)
            _userMessage.value = "Pesanan Pembelian (PO) berhasil dibuat!"
        }
    }

    fun receivePO(po: PurchaseOrderEntity) {
        viewModelScope.launch {
            repository.receivePurchaseOrder(po)
            _userMessage.value = "Stok dari ${po.poNumber} berhasil diterima & masuk ke persediaan!"
        }
    }

    // Journal Entry
    fun addManualJournal(type: JournalType, category: String, amount: Double, description: String) {
        viewModelScope.launch {
            repository.addJournalEntry(type, category, amount, description)
            _userMessage.value = "Catatan jurnal berhasil ditambahkan!"
        }
    }

    // Settings & Struk Footer Notes
    fun updateStoreSettings(settings: StoreSettings) {
        viewModelScope.launch {
            repository.updateStoreSettings(settings)
            _userMessage.value = "Pengaturan toko & catatan struk berhasil diperbarui!"
        }
    }

    fun parseCartItems(json: String) = repository.parseCartItems(json)
    fun parsePOItems(json: String) = repository.parsePOItems(json)
}
