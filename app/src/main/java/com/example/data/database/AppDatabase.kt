package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.CategoryDao
import com.example.data.dao.CustomerDao
import com.example.data.dao.JournalDao
import com.example.data.dao.ProductDao
import com.example.data.dao.PurchaseOrderDao
import com.example.data.dao.SettingsDao
import com.example.data.dao.TransactionDao
import com.example.data.dao.UserDao
import com.example.data.model.CategoryEntity
import com.example.data.model.Customer
import com.example.data.model.JournalEntry
import com.example.data.model.JournalType
import com.example.data.model.PriceLevel
import com.example.data.model.Product
import com.example.data.model.PurchaseOrderEntity
import com.example.data.model.StoreSettings
import com.example.data.model.TransactionEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Product::class,
        Customer::class,
        TransactionEntity::class,
        PurchaseOrderEntity::class,
        JournalEntry::class,
        StoreSettings::class,
        UserEntity::class,
        CategoryEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun customerDao(): CustomerDao
    abstract fun transactionDao(): TransactionDao
    abstract fun purchaseOrderDao(): PurchaseOrderDao
    abstract fun journalDao(): JournalDao
    abstract fun settingsDao(): SettingsDao
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vinz_accessories_db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }
        }

        private suspend fun populateInitialData(db: AppDatabase) {
            // Initial Store Settings
            db.settingsDao().saveStoreSettings(
                StoreSettings(
                    id = 1,
                    storeName = "VINZ ACCESSORIES",
                    address = "Jl. Accessories HP No. 88, Plaza Seluler",
                    phone = "0812-3456-7890",
                    footerNotes = "Terima kasih telah berbelanja di VINZ ACCESSORIES!\nBarang yang sudah dibeli tidak dapat ditukar/dikembalikan.\nGaransi aksesoris 7 hari dengan menunjukkan struk ini."
                )
            )

            // Initial Users (Admin & Kasir)
            val initialUsers = listOf(
                UserEntity(
                    username = "admin",
                    fullName = "Administrator Utama (Owner)",
                    role = UserRole.ADMIN,
                    pin = "1234",
                    phone = "0812-3456-7890"
                ),
                UserEntity(
                    username = "kasir1",
                    fullName = "Siti (Kasir 1)",
                    role = UserRole.KASIR,
                    pin = "0000",
                    phone = "0857-1234-5678"
                )
            )
            initialUsers.forEach { db.userDao().insertUser(it) }

            // Initial Categories
            val initialCategories = listOf(
                CategoryEntity(name = "Tempered Glass", description = "Pelindung layar kaca tempered HP"),
                CategoryEntity(name = "Charger", description = "Adaptor charger fast charging & PD"),
                CategoryEntity(name = "Kabel Data", description = "Kabel USB Type-C, Lightning, Micro USB"),
                CategoryEntity(name = "Casing", description = "Softcase, Hardcase, Anti-drop cover"),
                CategoryEntity(name = "Audio & TWS", description = "TWS bluetooth, earphone, headset"),
                CategoryEntity(name = "Holder & Stand", description = "Phone stand meja & car holder"),
                CategoryEntity(name = "Aksesoris", description = "Aksesoris serbaguna lainnya")
            )
            db.categoryDao().insertCategories(initialCategories)

            // Initial Products (Phone Accessories)
            val initialProducts = listOf(
                Product(
                    barcode = "8991001001",
                    name = "Tempered Glass Privacy iPhone 13/14",
                    category = "Tempered Glass",
                    stock = 25,
                    minStockAlert = 5,
                    costPrice = 12000.0,
                    priceEceran = 35000.0,
                    priceGrosir = 22000.0,
                    priceReseller = 26000.0,
                    priceVip = 20000.0
                ),
                Product(
                    barcode = "8991001002",
                    name = "Fast Charger 20W Type-C QuickCharge",
                    category = "Charger",
                    stock = 15,
                    minStockAlert = 5,
                    costPrice = 45000.0,
                    priceEceran = 85000.0,
                    priceGrosir = 65000.0,
                    priceReseller = 72000.0,
                    priceVip = 60000.0
                ),
                Product(
                    barcode = "8991001003",
                    name = "Kabel Data Type-C Braided Fast 1M",
                    category = "Kabel Data",
                    stock = 30,
                    minStockAlert = 8,
                    costPrice = 8000.0,
                    priceEceran = 25000.0,
                    priceGrosir = 15000.0,
                    priceReseller = 18000.0,
                    priceVip = 14000.0
                ),
                Product(
                    barcode = "8991001004",
                    name = "Case Silicon Matte Navy Anti-Drop",
                    category = "Casing",
                    stock = 3, // Low stock!
                    minStockAlert = 5,
                    costPrice = 15000.0,
                    priceEceran = 45000.0,
                    priceGrosir = 30000.0,
                    priceReseller = 35000.0,
                    priceVip = 28000.0
                ),
                Product(
                    barcode = "8991001005",
                    name = "TWS Bluetooth Earphone AirBass Pro",
                    category = "Audio & TWS",
                    stock = 10,
                    minStockAlert = 3,
                    costPrice = 90000.0,
                    priceEceran = 185000.0,
                    priceGrosir = 135000.0,
                    priceReseller = 150000.0,
                    priceVip = 125000.0
                ),
                Product(
                    barcode = "8991001006",
                    name = "Holder HP Desk Stand Alumunium 360",
                    category = "Holder & Stand",
                    stock = 12,
                    minStockAlert = 4,
                    costPrice = 20000.0,
                    priceEceran = 50000.0,
                    priceGrosir = 35000.0,
                    priceReseller = 40000.0,
                    priceVip = 32000.0
                ),
                Product(
                    barcode = "8991001007",
                    name = "MagSafe Magnetic Card Wallet",
                    category = "Aksesoris",
                    stock = 18,
                    minStockAlert = 5,
                    costPrice = 25000.0,
                    priceEceran = 65000.0,
                    priceGrosir = 45000.0,
                    priceReseller = 50000.0,
                    priceVip = 40000.0
                )
            )
            db.productDao().insertProducts(initialProducts)

            // Initial Customers
            val initialCustomers = listOf(
                Customer(
                    name = "Budi Reseller Cell",
                    phone = "0812-9988-7766",
                    address = "Mangga Dua Lt. 2 No. 14",
                    defaultPriceLevel = PriceLevel.RESELLER,
                    totalDebt = 0.0,
                    notes = "Rutin order kabel & casing tiap pekan"
                ),
                Customer(
                    name = "Toko Jaya Aksesoris (Grosir)",
                    phone = "0857-1122-3344",
                    address = "Pasar Glodok Blok B No. 5",
                    defaultPriceLevel = PriceLevel.GROSIR,
                    totalDebt = 150000.0, // Belum lunas
                    notes = "Tempo pembayaran 14 hari"
                ),
                Customer(
                    name = "Siti VIP Member",
                    phone = "0813-4455-6677",
                    address = "Perum Asri Indah C12",
                    defaultPriceLevel = PriceLevel.VIP,
                    totalDebt = 0.0,
                    notes = "Pelanggan setia sejak 2023"
                )
            )
            initialCustomers.forEach { db.customerDao().insertCustomer(it) }

            // Initial Journal Record
            db.journalDao().insertJournal(
                JournalEntry(
                    type = JournalType.INCOME,
                    category = "Modal Awal",
                    amount = 5000000.0,
                    description = "Saldo modal awal kasir VINZ ACCESSORIES"
                )
            )
        }
    }
}
