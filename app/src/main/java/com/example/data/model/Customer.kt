package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val defaultPriceLevel: PriceLevel = PriceLevel.ECERAN,
    val totalDebt: Double = 0.0,
    val notes: String = ""
)
