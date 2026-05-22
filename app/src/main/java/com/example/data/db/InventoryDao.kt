package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {

    @Query("SELECT * FROM inventory_items ORDER BY name ASC")
    fun getAllItems(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    suspend fun getItemById(id: Int): InventoryItem?

    @Query("SELECT * FROM inventory_items WHERE sku = :sku")
    suspend fun getItemBySku(sku: String): InventoryItem?

    @Query("SELECT * FROM inventory_items WHERE name = :name LIMIT 1")
    suspend fun getItemByName(name: String): InventoryItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItem): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItems(items: List<InventoryItem>)

    @Update
    suspend fun updateItem(item: InventoryItem)

    // Direct stock update
    @Query("UPDATE inventory_items SET stockQuantity = :newQuantity WHERE id = :id")
    suspend fun updateStockQuantity(id: Int, newQuantity: Int)

    // Select low-stock items
    @Query("SELECT * FROM inventory_items WHERE stockQuantity <= lowStockThreshold")
    fun getLowStockItems(): Flow<List<InventoryItem>>

    // --- Movements ---
    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC")
    fun getAllMovements(): Flow<List<StockMovement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: StockMovement): Long

    // --- Receipts ---
    @Query("SELECT * FROM sale_receipts ORDER BY timestamp DESC")
    fun getAllReceipts(): Flow<List<SaleReceipt>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceipt(receipt: SaleReceipt): Long

    // Combine stock change and movement insertion into a single Transaction for safety
    @Transaction
    suspend fun processStockAdjustment(itemId: Int, changeAmount: Int, source: String, reference: String, note: String) {
        val item = getItemById(itemId) ?: return
        val newQty = (item.stockQuantity + changeAmount).coerceAtLeast(0)
        updateStockQuantity(itemId, newQty)
        insertMovement(
            StockMovement(
                itemId = itemId,
                itemName = item.name,
                changeAmount = changeAmount,
                source = source,
                reference = reference,
                note = note
            )
        )
    }
}
