package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiApiClient
import com.example.data.api.OcrResult
import com.example.data.db.AppDatabase
import com.example.data.db.InventoryItem
import com.example.data.db.SaleReceipt
import com.example.data.db.StockMovement
import com.example.data.repository.InventoryRepository
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@JsonClass(generateAdapter = true)
data class CartItem(
    val itemId: Int,
    val name: String,
    val quantity: Int,
    val price: Double,
    val total: Double
)

sealed interface OcrProcessState {
    object Idle : OcrProcessState
    object Scanning : OcrProcessState
    data class Success(val result: OcrResult, val logs: List<String>) : OcrProcessState
    data class Error(val message: String) : OcrProcessState
}

class InventoryViewModel(
    application: Application,
    private val repository: InventoryRepository
) : AndroidViewModel(application) {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val listMyType = Types.newParameterizedType(List::class.java, CartItem::class.java)
    private val jsonAdapter = moshi.adapter<List<CartItem>>(listMyType)

    // --- Flows observed by UI ---
    val allItems: StateFlow<List<InventoryItem>> = repository.allItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockItems: StateFlow<List<InventoryItem>> = repository.lowStockItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMovements: StateFlow<List<StockMovement>> = repository.allMovements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReceipts: StateFlow<List<SaleReceipt>> = repository.allReceipts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- State for POS / Shopping Cart ---
    var cart = mutableStateMapOf<Int, Int>() // itemId -> quantity
        private set

    private val _customerName = MutableStateFlow("")
    val customerName = _customerName.asStateFlow()

    private val _paymentMethod = MutableStateFlow("Cash")
    val paymentMethod = _paymentMethod.asStateFlow()

    private val _checkoutSuccessReceipt = MutableStateFlow<SaleReceipt?>(null)
    val checkoutSuccessReceipt = _checkoutSuccessReceipt.asStateFlow()

    // --- OCR State ---
    private val _ocrState = MutableStateFlow<OcrProcessState>(OcrProcessState.Idle)
    val ocrState = _ocrState.asStateFlow()

    // --- Search / Category State ---
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("All")

    init {
        // Run clean-start database checks to ensure we seed items
        viewModelScope.launch {
            try {
                repository.ensureMinimumSeedData()
            } catch (e: Exception) {
                Log.e("InventoryViewModel", "Error ensuring database seeding", e)
            }
        }
    }

    // --- Category List Fetch ---
    val categories: StateFlow<List<String>> = allItems
        .map { items -> listOf("All") + items.map { it.category }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All"))

    // --- POS Cart Operations ---
    fun addToCart(item: InventoryItem) {
        val currentQty = cart[item.id] ?: 0
        if (currentQty < item.stockQuantity) {
            cart[item.id] = currentQty + 1
        }
    }

    fun updateCartQuantity(item: InventoryItem, quantity: Int) {
        val targetQty = quantity.coerceIn(0, item.stockQuantity)
        if (targetQty == 0) {
            cart.remove(item.id)
        } else {
            cart[item.id] = targetQty
        }
    }

    fun removeFromCart(item: InventoryItem) {
        cart.remove(item.id)
    }

    fun clearCart() {
        cart.clear()
        _customerName.value = ""
        _paymentMethod.value = "Cash"
        _checkoutSuccessReceipt.value = null
    }

    fun setCustomerName(name: String) {
        _customerName.value = name
    }

    fun setPaymentMethod(method: String) {
        _paymentMethod.value = method
    }

    fun setSuccessReceipt(receipt: SaleReceipt?) {
        _checkoutSuccessReceipt.value = receipt
    }

    fun dismissOcr() {
        _ocrState.value = OcrProcessState.Idle
    }

    /**
     * Submit Point of Sale and Billing cart items.
     * Deducts stock and logs transaction.
     */
    fun checkout(itemsSnapshot: List<InventoryItem>): Boolean {
        if (cart.isEmpty()) return false

        val cartItems = mutableListOf<CartItem>()
        var subtotal = 0.0

        // Create transaction logs
        viewModelScope.launch {
            try {
                val timestamp = System.currentTimeMillis()
                val receiptNum = "REC-${timestamp % 100000000}-${(100..999).random()}"

                for ((itemId, qty) in cart) {
                    val dbItem = repository.getItemById(itemId) ?: continue
                    
                    // Deduct stock quantity
                    val finalQty = (dbItem.stockQuantity - qty).coerceAtLeast(0)
                    repository.processStockAdjustment(
                        itemId = itemId,
                        changeAmount = -qty,
                        source = "POS Sale",
                        reference = receiptNum,
                        note = "Standard customer checkout"
                    )

                    val itemTotal = dbItem.price * qty
                    cartItems.add(
                        CartItem(
                            itemId = itemId,
                            name = dbItem.name,
                            quantity = qty,
                            price = dbItem.price,
                            total = itemTotal
                        )
                    )
                    subtotal += itemTotal
                }

                val tax = subtotal * 0.05 // 5% flat wholesale tax
                val total = subtotal + tax
                val jsonItems = jsonAdapter.toJson(cartItems) ?: "[]"

                val receipt = SaleReceipt(
                    receiptNumber = receiptNum,
                    timestamp = timestamp,
                    itemsJson = jsonItems,
                    subtotal = subtotal,
                    tax = tax,
                    total = total,
                    customerName = _customerName.value.ifBlank { "Walk-in Wholesale Client" },
                    paymentMethod = _paymentMethod.value
                )

                val receiptId = repository.insertReceipt(receipt)
                _checkoutSuccessReceipt.value = receipt.copy(id = receiptId.toInt())

                // Empty cart immediately upon completing checkout successfully
                cart.clear()
            } catch (e: Exception) {
                Log.e("InventoryViewModel", "POS checkout compilation failed", e)
            }
        }
        return true
    }

    // --- Slip OCR AI Scanner ---
    fun parseHandwrittenSlip(bitmap: Bitmap?, textLog: String?) {
        _ocrState.value = OcrProcessState.Scanning

        viewModelScope.launch {
            try {
                val ocrResult = GeminiApiClient.parseHandwrittenSlip(bitmap, textLog)
                if (ocrResult == null) {
                    _ocrState.value = OcrProcessState.Error("Failed to parse slip. Could not connect to Gemini API safely or read the details.")
                    return@launch
                }

                val logs = mutableListOf<String>()
                val action = ocrResult.action // "RESTOCK" or "SALE"
                val reference = ocrResult.slipNumber?.ifBlank { null } ?: "OCR Slip #${(1000..9999).random()}"

                logs.add("Parsed Action: $action")
                logs.add("Slip Reference: $reference")

                // Process items parsed
                for (ocrItem in ocrResult.items) {
                    val matchedItem = repository.getItemByName(ocrItem.name)
                        ?: repository.allItems.first().firstOrNull { it.name.lowercase().contains(ocrItem.name.lowercase()) || ocrItem.name.lowercase().contains(it.name.lowercase()) }

                    val changeValue = if (action == "RESTOCK") ocrItem.quantity else -ocrItem.quantity

                    if (matchedItem != null) {
                        repository.processStockAdjustment(
                            itemId = matchedItem.id,
                            changeAmount = changeValue,
                            source = "OCR $action",
                            reference = reference,
                            note = "Auto-rendered from handwritten slip"
                        )
                        logs.add("Updated: ${matchedItem.name} by $changeValue (New: ${matchedItem.stockQuantity + changeValue})")
                    } else {
                        // Create a brand new wholesale product with default properties for seamless automation
                        val defaultPrice = 25.0
                        val defaultThreshold = 10
                        val newItem = InventoryItem(
                            name = ocrItem.name,
                            sku = "SKU-" + ocrItem.name.uppercase().take(3).trim() + "-" + (100..999).random(),
                            category = "OCR Imported",
                            price = defaultPrice,
                            stockQuantity = if (action == "RESTOCK") ocrItem.quantity else 0,
                            lowStockThreshold = defaultThreshold
                        )
                        val newId = repository.insertItem(newItem).toInt()

                        repository.processStockAdjustment(
                            itemId = newId,
                            changeAmount = if (action == "RESTOCK") 0 else -ocrItem.quantity, // Adjust if selling, initially seeded with ocrItem.quantity if restocking
                            source = "OCR $action",
                            reference = reference,
                            note = "Auto-created new SKU from handwritten slip"
                        )
                        logs.add("New Cargo: '${ocrItem.name}' registered to database (SKU: ${newItem.sku}, Added: ${ocrItem.quantity})")
                    }
                }

                _ocrState.value = OcrProcessState.Success(ocrResult, logs)
            } catch (e: Exception) {
                Log.e("InventoryViewModel", "Error running Slip OCR translation", e)
                _ocrState.value = OcrProcessState.Error(e.message ?: "Unknown scanning error occurred.")
            }
        }
    }

    // --- Helper to deserialize items from SaleReceipt ---
    fun getReceiptItems(receipt: SaleReceipt): List<CartItem> {
        return try {
            jsonAdapter.fromJson(receipt.itemsJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- Manual Direct Product Management ---
    fun createOrUpdateProduct(
        id: Int,
        name: String,
        sku: String,
        category: String,
        price: Double,
        stock: Int,
        threshold: Int
    ) {
        viewModelScope.launch {
            if (id == 0) {
                // New item
                val newItem = InventoryItem(
                    name = name,
                    sku = sku.ifBlank { "SKU-" + name.uppercase().take(3).trim() + "-" + (100..999).random() },
                    category = category.ifBlank { "General" },
                    price = price,
                    stockQuantity = stock,
                    lowStockThreshold = threshold
                )
                val newId = repository.insertItem(newItem)
                // Log movement
                repository.processStockAdjustment(
                    itemId = newId.toInt(),
                    changeAmount = stock,
                    source = "Manual Adjustment",
                    reference = "Product Creation",
                    note = "Initial stock allocation"
                )
            } else {
                // Update item
                val currentItem = repository.getItemById(id) ?: return@launch
                val delta = stock - currentItem.stockQuantity

                val updated = InventoryItem(
                    id = id,
                    name = name,
                    sku = sku,
                    category = category,
                    price = price,
                    stockQuantity = stock,
                    lowStockThreshold = threshold
                )
                repository.updateItem(updated)

                if (delta != 0) {
                    repository.processStockAdjustment(
                        itemId = id,
                        changeAmount = delta,
                        source = "Manual Adjustment",
                        reference = "Product Update",
                        note = "Stock count manual modification"
                    )
                }
            }
        }
    }
}

class InventoryViewModelFactory(
    private val application: Application,
    private val repository: InventoryRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InventoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InventoryViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
