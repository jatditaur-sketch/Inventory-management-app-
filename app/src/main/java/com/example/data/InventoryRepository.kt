package com.example.data

import kotlinx.coroutines.flow.Flow

class InventoryRepository(private val dao: InventoryDao) {

    val allItems: Flow<List<InventoryItem>> = dao.getAllInventoryItems()
    val allMovements: Flow<List<StockMovement>> = dao.getAllStockMovements()
    val allReceipts: Flow<List<SaleReceipt>> = dao.getAllSalesReceipts()

    fun searchItems(query: String): Flow<List<InventoryItem>> {
        return dao.searchInventoryItems("%$query%")
    }

    suspend fun getItemBySku(sku: String): InventoryItem? {
        return dao.getItemBySku(sku)
    }

    suspend fun insertOrUpdateItem(item: InventoryItem): Long {
        return dao.insertOrUpdateItem(item)
    }

    suspend fun updateItem(item: InventoryItem) {
        dao.updateItem(item)
    }

    suspend fun deleteItem(item: InventoryItem) {
        dao.deleteItem(item)
    }

    suspend fun adjustStock(sku: String, delta: Int, sourceName: String) {
        dao.adjustStock(sku, delta, sourceName)
    }

    suspend fun insertMovement(movement: StockMovement): Long {
        return dao.insertMovement(movement)
    }

    suspend fun insertReceipt(receipt: SaleReceipt): Long {
        return dao.insertReceipt(receipt)
    }
}
