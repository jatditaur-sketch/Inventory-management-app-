package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [InventoryItem::class, StockMovement::class, SaleReceipt::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun inventoryDao(): InventoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "stocksync_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDatabase(database.inventoryDao())
                    }
                }
            }

            suspend fun populateDatabase(dao: InventoryDao) {
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
                        stockQuantity = 12, // Below low-stock alert of 20!
                        lowStockThreshold = 20
                    ),
                    InventoryItem(
                        name = "Basmati Rice Extra Long",
                        sku = "RCE-25",
                        category = "Grains",
                        price = 58.0,
                        stockQuantity = 8, // Below low stock threshold of 15!
                        lowStockThreshold = 15
                    )
                )
                dao.insertItems(initialItems)

                // Log seed movements
                dao.insertMovement(
                    StockMovement(
                        itemId = 1,
                        itemName = "Premium Wheat (50kg)",
                        changeAmount = 45,
                        source = "Manual Adjustment",
                        reference = "Seed Data",
                        note = "Initial stock intake"
                    )
                )
                dao.insertMovement(
                    StockMovement(
                        itemId = 2,
                        itemName = "Wholesale Sugar (Refined)",
                        changeAmount = 110,
                        source = "Manual Adjustment",
                        reference = "Seed Data",
                        note = "Initial stock intake"
                    )
                )
                dao.insertMovement(
                    StockMovement(
                        itemId = 3,
                        itemName = "Lentils Grade A (20kg)",
                        changeAmount = 12,
                        source = "Manual Adjustment",
                        reference = "Seed Data",
                        note = "Initial stock intake"
                    )
                )
                dao.insertMovement(
                    StockMovement(
                        itemId = 4,
                        itemName = "Basmati Rice Extra Long",
                        changeAmount = 8,
                        source = "Manual Adjustment",
                        reference = "Seed Data",
                        note = "Initial stock intake"
                    )
                )
            }
        }
    }
}
