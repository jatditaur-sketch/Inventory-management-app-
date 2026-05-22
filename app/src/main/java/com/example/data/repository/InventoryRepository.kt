package com.example.data.repository

import com.example.data.db.InventoryDao
import com.example.data.db.InventoryItem
import com.example.data.db.SaleReceipt
import com.example.data.db.StockMovement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class InventoryRepository(private val inventoryDao: InventoryDao) {

    val allItems: Flow<List<InventoryItem>> = inventoryDao.getAllItems()
    val lowStockItems: Flow<List<InventoryItem>> = inventoryDao.getLowStockItems()
    val allMovements: Flow<List<StockMovement>> = inventoryDao.getAllMovements()
    val allReceipts: Flow<List<SaleReceipt>> = inventoryDao.getAllReceipts()

    suspend fun insertItem(item: InventoryItem): Long {
        return inventoryDao.insertItem(item)
    }

    suspend fun updateItem(item: InventoryItem) {
        inventoryDao.updateItem(item)
    }

    suspend fun getItemById(id: Int): InventoryItem? {
        return inventoryDao.getItemById(id)
    }

    suspend fun getItemBySku(sku: String): InventoryItem? {
        return inventoryDao.getItemBySku(sku)
    }

    suspend fun getItemByName(name: String): InventoryItem? {
        return inventoryDao.getItemByName(name)
    }

    suspend fun processStockAdjustment(itemId: Int, changeAmount: Int, source: String, reference: String, note: String) {
        inventoryDao.processStockAdjustment(itemId, changeAmount, source, reference, note)
    }

    suspend fun insertReceipt(receipt: SaleReceipt): Long {
        return inventoryDao.insertReceipt(receipt)
    }

    // Backup Seeder to ensure we always have beautiful sample records on first run
    suspend fun ensureMinimumSeedData() {
        val existing = allItems.firstOrNull()
        if (existing.isNullOrEmpty()) {
            val initialItems = listOf(
                InventoryItem(
                    name = "Premium Wheat (50kg)",
                    sku = "WHT-50",
                    category = "Grains",
                    price = 45.0,
                    stockQuantity = 45,
                    lowStockThreshold = 25
                ),
                InventoryItem(
                    name = "Wholesale Sugar (Refined)",
                    sku = "SGR-100",
                    category = "Sweeteners",
                    price = 60.0,
                    stockQuantity = 110,
                    lowStockThreshold = 30
                ),
                InventoryItem(
                    name = "Lentils Grade A (20kg)",
                    sku = "LNT-20",
                    category = "Pulses",
                    price = 32.5,
                    stockQuantity = 12,
                    lowStockThreshold = 20
                ),
                InventoryItem(
                    name = "Basmati Rice Extra Long",
                    sku = "RCE-25",
                    category = "Grains",
                    price = 58.0,
                    stockQuantity = 8,
                    lowStockThreshold = 15
                )
            )
            inventoryDao.insertItems(initialItems)

            // Log initial movements for the 4 items
            for (i in 1..4) {
                val item = initialItems[i - 1]
                inventoryDao.insertMovement(
                    StockMovement(
                        itemId = i,
                        itemName = item.name,
                        changeAmount = item.stockQuantity,
                        source = "Manual Adjustment",
                        reference = "Seed Data",
                        note = "Initial inventory upload"
                    )
                )
            }
        }
    }
}
