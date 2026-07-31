package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val barcode: String,
    val name: String,
    val category: String = "Aksesoris",
    val stock: Int = 0,
    val minStockAlert: Int = 5,
    val costPrice: Double = 0.0,
    val priceEceran: Double = 0.0,
    val priceGrosir: Double = 0.0,
    val priceReseller: Double = 0.0,
    val priceVip: Double = 0.0,
    val imageUrl: String? = null
) {
    fun getPriceForLevel(level: PriceLevel): Double {
        val p = when (level) {
            PriceLevel.ECERAN -> priceEceran
            PriceLevel.GROSIR -> if (priceGrosir > 0) priceGrosir else priceEceran
            PriceLevel.RESELLER -> if (priceReseller > 0) priceReseller else priceEceran
            PriceLevel.VIP -> if (priceVip > 0) priceVip else priceEceran
        }
        return if (p > 0) p else priceEceran
    }
}
