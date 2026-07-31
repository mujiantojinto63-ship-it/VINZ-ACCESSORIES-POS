package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val receiptNo: String,
    val timestamp: Long = System.currentTimeMillis(),
    val customerId: Long? = null,
    val customerName: String = "Pelanggan Umum",
    val priceLevelUsed: PriceLevel = PriceLevel.ECERAN,
    val subtotal: Double,
    val discount: Double = 0.0,
    val total: Double,
    val paymentMethod: PaymentMethod,
    val paymentStatus: PaymentStatus,
    val amountPaid: Double,
    val change: Double,
    val notes: String = "",
    val itemsJson: String
)

data class CartItem(
    val productId: Long,
    val productName: String,
    val barcode: String,
    val selectedPriceLevel: PriceLevel,
    val unitPrice: Double,
    val quantity: Int,
    val costPrice: Double = 0.0
) {
    val totalPrice: Double get() = unitPrice * quantity
}
