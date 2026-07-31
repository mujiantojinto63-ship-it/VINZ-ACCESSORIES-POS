package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val type: JournalType,
    val category: String,
    val amount: Double,
    val description: String,
    val refId: String? = null
)

@Entity(tableName = "store_settings")
data class StoreSettings(
    @PrimaryKey val id: Int = 1,
    val storeName: String = "VINZ ACCESSORIES",
    val address: String = "Jl. Accessories HP No. 88, Plaza Seluler",
    val phone: String = "0812-3456-7890",
    val footerNotes: String = "Terima kasih telah berbelanja di VINZ ACCESSORIES!\nBarang yang sudah dibeli tidak dapat ditukar/dikembalikan.\nGaransi aksesoris 7 hari dengan menunjukan struk ini.",
    val qrisCode: String = "00020101021126580014ID.CO.QRIS.WWW01189360091400000000005204581253033605802ID5916VINZ ACCESSORIES6007JAKARTA61051234562070703A0163044455",
    val bankName: String = "BCA",
    val bankAccountNo: String = "8820-1293-88",
    val bankAccountName: String = "VINZ ACCESSORIES"
)
