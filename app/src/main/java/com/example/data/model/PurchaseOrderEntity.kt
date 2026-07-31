package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "purchase_orders")
data class PurchaseOrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val poNumber: String,
    val supplierName: String,
    val supplierPhone: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: POStatus = POStatus.PENDING,
    val totalCost: Double,
    val itemsJson: String,
    val notes: String = ""
)

data class PurchaseOrderItem(
    val productId: Long,
    val productName: String,
    val costPrice: Double,
    val quantity: Int
) {
    val subtotal: Double get() = costPrice * quantity
}
