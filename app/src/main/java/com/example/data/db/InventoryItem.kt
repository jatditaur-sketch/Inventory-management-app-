package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val sku: String,
    val category: String,
    val price: Double,
    val stockQuantity: Int,
    val lowStockThreshold: Int
) : Serializable
