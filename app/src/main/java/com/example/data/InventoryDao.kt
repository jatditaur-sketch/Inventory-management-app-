package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {

    // --- Inventory Items ---
    @Query("SELECT * FROM inventory_items ORDER BY category ASC, name ASC")
    fun getAllInventoryItems(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items WHERE sku = :sku LIMIT 1")
    suspend fun getItemBySku(sku: String): InventoryItem?

    @Query("SELECT * FROM inventory_items WHERE name LIKE :query OR sku LIKE :query")
    fun searchInventoryItems(query: String): Flow<List<InventoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateItem(item: InventoryItem): Long

    @Update
    suspend fun updateItem(item: InventoryItem)

    @Delete
    suspend fun deleteItem(item: InventoryItem)

    @Transaction
    suspend fun adjustStock(sku: String, delta: Int, sourceName: String) {
        val item = getItemBySku(sku)
        if (item != null) {
            val newQty = (item.quantity + delta).coerceAtLeast(0)
            updateItem(item.copy(quantity = newQty))
            insertMovement(
                StockMovement(
                    itemName = item.name,
                    sku = sku,
                    changeAmount = delta,
                    source = sourceName
                )
            )
        }
    }

    // --- Stock Movements ---
    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC LIMIT 50")
    fun getAllStockMovements(): Flow<List<StockMovement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: StockMovement): Long

    // --- Sales Receipts ---
    @Query("SELECT * FROM sales_receipts ORDER BY timestamp DESC")
    fun getAllSalesReceipts(): Flow<List<SaleReceipt>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: SaleReceipt): Long
}
