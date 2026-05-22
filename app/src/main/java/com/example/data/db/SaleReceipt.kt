package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sale_receipts")
data class SaleReceipt(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val receiptNumber: String, // REC-2026-XXXX
    val timestamp: Long = System.currentTimeMillis(),
    val itemsJson: String, // Moshi serialized list of sold items
    val subtotal: Double,
    val tax: Double,
    val total: Double,
    val customerName: String = "Wholesale Customer",
    val paymentMethod: String = "Cash"
)
