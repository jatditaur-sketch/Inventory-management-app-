package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_movements")
data class StockMovement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemId: Int,
    val itemName: String,
    val changeAmount: Int,  // e.g. +100 for restock, -24 for sale
    val timestamp: Long = System.currentTimeMillis(),
    val source: String,     // "OCR Restock", "OCR Sale", "POS Sale", "Manual Adjustment"
    val reference: String,  // e.g. "Slip #821", "Receipt #REC-101"
    val note: String = ""
)
