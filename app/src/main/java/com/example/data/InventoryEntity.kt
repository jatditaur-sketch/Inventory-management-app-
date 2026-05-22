package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "inventory_items")
@JsonClass(generateAdapter = true)
data class InventoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val sku: String,
    val quantity: Int,
    val lowThreshold: Int,
    val category: String,
    val price: Double = 0.0,
    val unit: String = "box"
)

@Entity(tableName = "stock_movements")
@JsonClass(generateAdapter = true)
data class StockMovement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemName: String,
    val sku: String,
    val changeAmount: Int,  // + for addition (restock), - for reduction (sale/OCR sale)
    val timestamp: Long = System.currentTimeMillis(),
    val source: String       // e.g. "OCR-Slip #821", "POS Sale Receipt", "Manual Adjustment"
)

@Entity(tableName = "sales_receipts")
@JsonClass(generateAdapter = true)
data class SaleReceipt(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val receiptNumber: String,
    val itemsJson: String,  // JSON representation of list of (itemName, price, quantity)
    val totalAmount: Double,
    val timestamp: Long = System.currentTimeMillis()
)
