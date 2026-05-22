package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.InventoryItem
import com.example.data.SaleReceipt
import com.example.data.StockMovement
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: InventoryViewModel) {
    val activeTab by viewModel.activeTab.collectAsState()
    val lowStockAlerts by viewModel.lowStockCount.collectAsState()
    val totalItemsList by viewModel.items.collectAsState()
    val ocrResult by viewModel.ocrResult.collectAsState()
    val generatedReceipt by viewModel.generatedReceipt.collectAsState()

    var showOcrDialog by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = AccentPurpleBg,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "S",
                                    color = LightPurpleAccent,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "StockSync Pro",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Real-time pulsing indicator dot
                                val infiniteTransition = rememberInfiniteTransition(label = "indicator")
                                val animatedAlpha by infiniteTransition.animateFloat(
                                    initialValue = 0.4f,
                                    targetValue = 1.0f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1200, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "pulse"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(LiveGreen.copy(alpha = animatedAlpha))
                                )
                                Text(
                                    text = "REAL-TIME SYNCED",
                                    fontSize = 9.sp,
                                    color = LiveGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Logged into Wholesale Account", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.clip(CircleShape).testTag("profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile",
                            tint = PrimaryPurple,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = NavBackground,
                tonalElevation = 10.dp,
                modifier = Modifier.height(78.dp)
            ) {
                NavigationBarItem(
                    selected = activeTab == AppTab.HOME,
                    onClick = { viewModel.setTab(AppTab.HOME) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryPurple,
                        selectedTextColor = PrimaryPurple,
                        unselectedIconColor = GraySecondary.copy(alpha = 0.7f),
                        unselectedTextColor = GraySecondary.copy(alpha = 0.7f),
                        indicatorColor = AccentPurpleBg
                    )
                )

                NavigationBarItem(
                    selected = activeTab == AppTab.STOCK,
                    onClick = { viewModel.setTab(AppTab.STOCK) },
                    icon = { Icon(Icons.Default.Inventory, contentDescription = "Stock") },
                    label = { Text("Stock", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryPurple,
                        selectedTextColor = PrimaryPurple,
                        unselectedIconColor = GraySecondary.copy(alpha = 0.7f),
                        unselectedTextColor = GraySecondary.copy(alpha = 0.7f),
                        indicatorColor = AccentPurpleBg
                    )
                )

                NavigationBarItem(
                    selected = activeTab == AppTab.REPORTS,
                    onClick = { viewModel.setTab(AppTab.REPORTS) },
                    icon = { Icon(Icons.Default.Assessment, contentDescription = "Reports") },
                    label = { Text("Reports", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryPurple,
                        selectedTextColor = PrimaryPurple,
                        unselectedIconColor = GraySecondary.copy(alpha = 0.7f),
                        unselectedTextColor = GraySecondary.copy(alpha = 0.7f),
                        indicatorColor = AccentPurpleBg
                    )
                )

                NavigationBarItem(
                    selected = activeTab == AppTab.POS,
                    onClick = { viewModel.setTab(AppTab.POS) },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Billing") },
                    label = { Text("Billing", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryPurple,
                        selectedTextColor = PrimaryPurple,
                        unselectedIconColor = GraySecondary.copy(alpha = 0.7f),
                        unselectedTextColor = GraySecondary.copy(alpha = 0.7f),
                        indicatorColor = AccentPurpleBg
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                AppTab.HOME -> HomeTab(
                    viewModel = viewModel,
                    onOpenOcr = { showOcrDialog = true },
                    onOpenBilling = { viewModel.setTab(AppTab.POS) }
                )
                AppTab.STOCK -> StockTab(
                    viewModel = viewModel,
                    onAddNewProduct = { showAddItemDialog = true }
                )
                AppTab.REPORTS -> ReportsTab(
                    viewModel = viewModel
                )
                AppTab.POS -> PosTab(
                    viewModel = viewModel
                )
            }

            // OCR Scanning Slip Sheet / Dialogue Panel
            if (showOcrDialog) {
                OcrScannerDialog(
                    viewModel = viewModel,
                    onDismiss = { showOcrDialog = false }
                )
            }

            // Printable POS Receipt overlays
            if (generatedReceipt != null) {
                ReceiptDialog(
                    receipt = generatedReceipt!!,
                    onDismiss = { viewModel.clearGeneratedReceipt() }
                )
            }

            // Add Manual Inventory Item dialog
            if (showAddItemDialog) {
                ItemEditDialog(
                    onDismiss = { showAddItemDialog = false },
                    onSave = { newItem ->
                        viewModel.saveManualItem(newItem)
                        showAddItemDialog = false
                    }
                )
            }
        }
    }
}

// ==========================================
// HOME VIEW DASHBOARD
// ==========================================
@Composable
fun HomeTab(
    viewModel: InventoryViewModel,
    onOpenOcr: () -> Unit,
    onOpenBilling: () -> Unit
) {
    val totalItemsList by viewModel.items.collectAsState()
    val lowStockCount by viewModel.lowStockCount.collectAsState()
    val movements by viewModel.movements.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // KPI Grid (Cards)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // SKU Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier
                        .weight(1f)
                        .height(112.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total Active SKUs",
                            color = GraySecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Column {
                            Text(
                                text = "${totalItemsList.size}",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Real-Time Tracking",
                                color = LiveGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Alert Stock Card
                val alertColor = if (lowStockCount > 0) LowStockRed else DarkSurface
                val alertTextColor = if (lowStockCount > 0) LowStockText else GraySecondary
                val textAccent = if (lowStockCount > 0) "REORDER REQUIRED" else "STOCK SECURE"

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = alertColor),
                    modifier = Modifier
                        .weight(1f)
                        .height(112.dp)
                        .border(
                            width = if (lowStockCount > 0) 1.5.dp else 0.dp,
                            color = if (lowStockCount > 0) LowStockRed else Color.Transparent,
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Low Stock Alerts",
                            color = alertTextColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Column {
                            Text(
                                text = String.format("%02d", lowStockCount),
                                color = if (lowStockCount > 0) LowStockText else Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = textAccent,
                                color = if (lowStockCount > 0) LowStockText else LiveGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }

        // Action Buttons Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // OCR handwriting Scanner Button
                Button(
                    onClick = onOpenOcr,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .shadow(4.dp, RoundedCornerShape(24.dp))
                        .testTag("home_scan_button")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = "OCR OCR Scan",
                            tint = AccentPurpleBg,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Scan OCR Slip",
                            color = AccentPurpleBg,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "(AI Handwriting Read)",
                            color = AccentPurpleBg.copy(alpha = 0.8f),
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Create POS Billing Button
                Button(
                    onClick = onOpenBilling,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LightPurpleAccent),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .shadow(4.dp, RoundedCornerShape(24.dp))
                        .testTag("home_billing_button")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.PointOfSale,
                            contentDescription = "POS Bill",
                            tint = AccentPurpleBg,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Create POS Bill",
                            color = AccentPurpleBg,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "(Checkout & Receipt)",
                            color = AccentPurpleBg.copy(alpha = 0.8f),
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Live Movements Log Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Stock Movements",
                    color = PrimaryPurple,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = CircleShape,
                    color = AccentPurpleBg,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = "LIVE FEED",
                        color = LightPurpleAccent,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Movements List
        if (movements.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No recent stock adjustments found.",
                            color = GraySecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(movements.take(15)) { log ->
                val isAddition = log.changeAmount >= 0
                val accentSign = if (isAddition) "+" else ""
                val amountColor = if (isAddition) LiveGreen else LowStockRed

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isAddition) "📥" else "📤",
                                    fontSize = 14.sp
                                )
                            }
                            Column {
                                Text(
                                    text = log.itemName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 180.dp)
                                )
                                Text(
                                    text = "${log.source} • SKU: ${log.sku}",
                                    fontSize = 10.sp,
                                    color = GraySecondary
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$accentSign${log.changeAmount} Units",
                                color = amountColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val format = SimpleDateFormat("HH:mm a", Locale.getDefault())
                            Text(
                                text = format.format(Date(log.timestamp)),
                                color = GraySecondary.copy(alpha = 0.7f),
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// STOCK TAB (INVENTORY LIST)
// ==========================================
@Composable
fun StockTab(
    viewModel: InventoryViewModel,
    onAddNewProduct: () -> Unit
) {
    val itemsList by viewModel.items.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var editingItem by remember { mutableStateOf<InventoryItem?>(null) }
    var adjustStockItem by remember { mutableStateOf<InventoryItem?>(null) }
    var adjustmentQty by remember { mutableStateOf("") }
    var isRestockDirection by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title & Add Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Wholesale Stockroom",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple
                )
                Text(
                    text = "Track safety threshold and levels",
                    fontSize = 11.sp,
                    color = GraySecondary
                )
            }
            Button(
                onClick = onAddNewProduct,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurpleBg),
                modifier = Modifier.testTag("add_item_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = LightPurpleAccent)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Item", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LightPurpleAccent)
            }
        }

        // Search text field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = { Text("Search product name, category or SKU...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = PrimaryPurple,
                unfocusedBorderColor = DarkSurface,
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("stock_search_field")
        )

        // Stock levels list
        if (itemsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No matching items found in warehouse inventory.",
                    color = GraySecondary,
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(itemsList) { item ->
                    val isDepleted = item.quantity <= item.lowThreshold
                    val warningBorderColor = if (isDepleted) LowStockRed else Color.Transparent

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, warningBorderColor, RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Row 1: SKU Tag & Category Title
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = DarkBackground,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    ) {
                                        Text(
                                            text = item.sku,
                                            color = PrimaryPurple,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "• ${item.category}",
                                        color = GraySecondary,
                                        fontSize = 10.sp
                                    )
                                }

                                // Stock Level Status tag
                                Surface(
                                    shape = CircleShape,
                                    color = if (isDepleted) LowStockRed.copy(alpha = 0.2f) else LiveGreen.copy(alpha = 0.15f),
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = if (isDepleted) "LOW STOCK ALERT" else "STOCK IN LIMIT",
                                        color = if (isDepleted) LowStockRed else LiveGreen,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Row 2: Item Name
                            Text(
                                text = item.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Row 3: Stock Information and Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Current Stock Level",
                                        fontSize = 9.sp,
                                        color = GraySecondary
                                    )
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = "${item.quantity}",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isDepleted) LowStockRed else Color.White
                                        )
                                        Text(
                                            text = " ${item.unit}s",
                                            fontSize = 11.sp,
                                            color = GraySecondary,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "Reorder limit: ${item.lowThreshold} units • $${item.price} wholesale",
                                        fontSize = 9.sp,
                                        color = GraySecondary.copy(alpha = 0.8f)
                                    )
                                }

                                // Quick Stock adjuster row/buttons
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            adjustStockItem = item
                                            isRestockDirection = true
                                            adjustmentQty = ""
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(AccentPurpleBg)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Manual Restock", tint = LightPurpleAccent)
                                    }

                                    IconButton(
                                        onClick = {
                                            adjustStockItem = item
                                            isRestockDirection = false
                                            adjustmentQty = ""
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(DarkBackground)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Manual Deduct", tint = Color.LightGray)
                                    }

                                    IconButton(
                                        onClick = { editingItem = item },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(DarkBackground)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Item Details", tint = PrimaryPurple)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Adjust Stock Dialog
    if (adjustStockItem != null) {
        val actionText = if (isRestockDirection) "Restock (Inventory Add)" else "Reduction (Inventory Sale)"
        val directionColor = if (isRestockDirection) LiveGreen else LowStockRed

        Dialog(onDismissRequest = { adjustStockItem = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Warehouse Adjustment",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple
                    )
                    Text(
                        text = adjustStockItem!!.name,
                        fontSize = 14.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = directionColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = actionText.uppercase(),
                            color = directionColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    OutlinedTextField(
                        value = adjustmentQty,
                        onValueChange = { qty ->
                            if (qty.all { it.isDigit() }) adjustmentQty = qty
                        },
                        label = { Text("Adjustment Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = DarkBackground,
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { adjustStockItem = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel", color = Color.White)
                        }

                        Button(
                            onClick = {
                                val parsed = adjustmentQty.toIntOrNull() ?: 1
                                val delta = if (isRestockDirection) parsed else -parsed
                                val sourceName = if (isRestockDirection) "Admin Restock" else "Admin Stock Deduct"
                                viewModel.adjustStock(
                                    sku = adjustStockItem!!.sku,
                                    delta = delta,
                                    sourceName = sourceName
                                )
                                adjustStockItem = null
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Confirm", color = AccentPurpleBg)
                        }
                    }
                }
            }
        }
    }

    // Edit Item Details Dialog
    if (editingItem != null) {
        ItemEditDialog(
            item = editingItem,
            onDismiss = { editingItem = null },
            onSave = { updatedItem ->
                viewModel.saveManualItem(updatedItem)
                editingItem = null
            },
            onDelete = { itemToDelete ->
                viewModel.deleteItem(itemToDelete)
                editingItem = null
            }
        )
    }
}

// ==========================================
// REPORTS TAB (REORDER GENERATOR)
// ==========================================
@Composable
fun ReportsTab(viewModel: InventoryViewModel) {
    val itemsList by viewModel.items.collectAsState()
    val receiptsList by viewModel.receipts.collectAsState()

    val depletedItems = itemsList.filter { it.quantity <= it.lowThreshold }

    var isReorderReportGenerated by remember { mutableStateOf(false) }
    var selectedOrderMultiplier by remember { mutableStateOf(3.0) } // recommended ordering quantity is (SafetyLimit * Multiplier) - CurrentStock

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section A: Manager's Reorder Report
        item {
            Column {
                Text(
                    text = "Automated Reorder Reports",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple
                )
                Text(
                    text = "Warehouse restock and purchase forecasts",
                    fontSize = 11.sp,
                    color = GraySecondary
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Critically Short SKUs",
                                fontSize = 11.sp,
                                color = GraySecondary
                            )
                            Text(
                                text = "${depletedItems.size} items deficient",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (depletedItems.isNotEmpty()) LowStockRed else LiveGreen
                            )
                        }

                        IconButton(
                            onClick = { isReorderReportGenerated = !isReorderReportGenerated },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(AccentPurpleBg)
                        ) {
                            Icon(
                                imageVector = if (isReorderReportGenerated) Icons.Default.Close else Icons.Default.Loop,
                                contentDescription = "Toggle",
                                tint = LightPurpleAccent
                            )
                        }
                    }

                    if (depletedItems.isNotEmpty()) {
                        Divider(color = Color.White.copy(alpha = 0.05f))

                        Text(
                            text = "Set procurement level multiplier:",
                            fontSize = 10.sp,
                            color = GraySecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(1.5, 2.0, 3.0, 5.0).forEach { mult ->
                                val active = selectedOrderMultiplier == mult
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (active) PrimaryPurple else DarkBackground)
                                        .clickable { selectedOrderMultiplier = mult }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${mult}x",
                                        color = if (active) AccentPurpleBg else Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { isReorderReportGenerated = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Print, contentDescription = "Generate", tint = AccentPurpleBg)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate Reorder Purchase Report", color = AccentPurpleBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = LiveGreen.copy(alpha = 0.08f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier.padding(14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "All items exceed safety threshold levels! Warehouse is healthy.",
                                    color = LiveGreen,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // Expanded Reorder Report Preview (Printable View)
        if (isReorderReportGenerated && depletedItems.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White), // POS thermal list style
                    border = BorderStroke(2.dp, PrimaryPurple)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "OFFICIAL RESTOCK PROCUREMENT REPORT",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        HorizontalDivider(color = Color.Black, thickness = 1.dp)

                        Text(
                            text = "STOCKSYNC WAREHOUSE RESTOCK REQUIREMENTS",
                            fontSize = 10.sp,
                            color = Color.DarkGray,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}",
                            color = Color.Gray,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        var printTotalCost = 0.0

                        depletedItems.forEach { item ->
                            val recommendedQty = ((item.lowThreshold * selectedOrderMultiplier).toInt() - item.quantity).coerceAtLeast(1)
                            val estItemCost = recommendedQty * item.price
                            printTotalCost += estItemCost

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item.name,
                                        fontSize = 10.sp,
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "$estItemCost$",
                                        fontSize = 10.sp,
                                        color = Color.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "SKU: ${item.sku} (Current Balance: ${item.quantity} ${item.unit}s)",
                                        fontSize = 8.sp,
                                        color = Color.DarkGray,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Order: $recommendedQty ${item.unit}s x $${item.price}",
                                        fontSize = 8.sp,
                                        color = Color.DarkGray,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }

                        HorizontalDivider(color = Color.Black, thickness = 1.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "TOTAL ESTIMATED OUTLAY:",
                                fontSize = 11.sp,
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "$${String.format("%.2f", printTotalCost)}",
                                fontSize = 11.sp,
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = "Authorized Signature: ___________________",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .padding(top = 16.dp)
                                .align(Alignment.End)
                        )
                    }
                }
            }
        }

        // Section B: Sales & Receipts Log
        item {
            Column {
                Text(
                    text = "Historic POS Invoice Logs",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple
                )
                Text(
                    text = "Historical overview of printable POS receipts",
                    fontSize = 11.sp,
                    color = GraySecondary
                )
            }
        }

        if (receiptsList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No billing invoices logged yet.",
                            color = GraySecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        } else {
            items(receiptsList) { rcpt ->
                var expanded by remember { mutableStateOf(false) }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Receipt, contentDescription = "Invoice", tint = PrimaryPurple, modifier = Modifier.size(20.dp))
                                Column {
                                    Text(
                                        text = rcpt.receiptNumber,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                    Text(
                                        text = format.format(Date(rcpt.timestamp)),
                                        color = GraySecondary,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$${String.format("%.2f", rcpt.totalAmount)}",
                                    color = LiveGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (expanded) "Tap to collapse" else "Tap to details",
                                    fontSize = 8.sp,
                                    color = PrimaryPurple
                                )
                            }
                        }

                        if (expanded) {
                            Divider(color = Color.White.copy(alpha = 0.05f))
                            Text(
                                text = "Parsed checkout items array details:",
                                fontSize = 9.sp,
                                color = PrimaryPurple,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = rcpt.itemsJson,
                                fontSize = 10.sp,
                                color = GraySecondary,
                                style = TextStyle(fontFamily = FontFamily.Monospace),
                                maxLines = 10,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// POS BILLING / BILL POINT OF SALE VIEW
// ==========================================
@Composable
fun PosTab(viewModel: InventoryViewModel) {
    val itemsList by viewModel.items.collectAsState()
    val basket by viewModel.posBasket.collectAsState()
    val customerName by viewModel.customerName.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf("Wholesale Net-30") }

    val filteredPosItems = itemsList.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.sku.contains(searchQuery, ignoreCase = true)
    }

    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Left Column: POS Item Selection list (Tablet friendly responsive layout)
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Point of Sale (POS)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryPurple
            )

            // Mini search to find billing items quickly
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search catalog...", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Find") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = PrimaryPurple,
                    unfocusedBorderColor = DarkSurface,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("pos_items_grid")
            ) {
                items(filteredPosItems) { item ->
                    val isOutOfStock = item.quantity <= 0
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isOutOfStock) {
                                viewModel.addToBasket(item)
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(10.dp)
                                .fillMaxWidth()
                        ) {
                            Text(
                                text = item.sku,
                                fontSize = 8.sp,
                                color = PrimaryPurple,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = item.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.White
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$${item.price}",
                                    fontSize = 12.sp,
                                    color = LiveGreen,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = if (isOutOfStock) LowStockRed.copy(alpha = 0.2f) else DarkBackground
                                ) {
                                    Text(
                                        text = if (isOutOfStock) "OUT" else "${item.quantity} Bal",
                                        fontSize = 8.sp,
                                        color = if (isOutOfStock) LowStockRed else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Right Column: Checkout Basket
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSurface)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "POS Invoice Cart",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryPurple
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (basket.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Empty", tint = GraySecondary.copy(alpha = 0.4f), modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Select products from catalog",
                                color = GraySecondary,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(basket.keys.toList()) { item ->
                        val qty = basket[item] ?: 1
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkBackground)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "SKU: ${item.sku} • Price: $${item.price}",
                                    fontSize = 8.sp,
                                    color = GraySecondary
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = { viewModel.removeFromBasket(item) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.RemoveCircle, contentDescription = "Less", tint = LowStockRed, modifier = Modifier.size(20.dp))
                                }
                                Text(
                                    text = "$qty",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                IconButton(
                                    onClick = { viewModel.addToBasket(item) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.AddCircle, contentDescription = "More", tint = LiveGreen, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

            // Customer Name
            OutlinedTextField(
                value = customerName,
                onValueChange = { viewModel.updateCustomerName(it) },
                placeholder = { Text("Invoice Client Name...", fontSize = 10.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = PrimaryPurple,
                    unfocusedBorderColor = DarkBackground,
                    focusedContainerColor = DarkBackground,
                    unfocusedContainerColor = DarkBackground
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(42.dp)
            )

            // Payment method select
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Cash", "Wholesale Net-30").forEach { pty ->
                    val active = selectedPaymentMethod == pty
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) PrimaryPurple else DarkBackground)
                            .clickable { selectedPaymentMethod = pty }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = pty,
                            color = if (active) AccentPurpleBg else Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Calculations panel
            val subtotal = basket.entries.sumOf { it.key.price * it.value }
            val wholesaleTax = subtotal * 0.05
            val total = subtotal + wholesaleTax

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkBackground)
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal", fontSize = 10.sp, color = GraySecondary)
                    Text(String.format("$%.2f", subtotal), fontSize = 10.sp, color = Color.White)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tax (5% Wholesale)", fontSize = 10.sp, color = GraySecondary)
                    Text(String.format("$%.2f", wholesaleTax), fontSize = 10.sp, color = Color.White)
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("TOTAL AMOUNT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryPurple)
                    Text(String.format("$%.2f", total), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LiveGreen)
                }
            }

            Button(
                onClick = {
                    if (basket.isEmpty()) {
                        Toast.makeText(context, "Pleae add items to checkout!", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.checkoutPOS(selectedPaymentMethod)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("pos_checkout_button")
            ) {
                Text("Process Invoice Receipt", color = AccentPurpleBg)
            }
        }
    }
}

// ==========================================
// OCR SCANNING HANDWRITING READ PANEL
// ==========================================
@Composable
fun OcrScannerDialog(
    viewModel: InventoryViewModel,
    onDismiss: () -> Unit
) {
    val ocrResult by viewModel.ocrResult.collectAsState()
    val isProcessing by viewModel.isOcrProcessing.collectAsState()
    val ocrModeInfo by viewModel.ocrModeInfo.collectAsState()

    var customInwardText by remember { mutableStateOf("") }

    val mockSlips = listOf(
        "SLIP #104 (Inward Restock): Received 150 bags Wholesale Sugar SKU: SUG-REF. 20 packages Black Pepper Ground 5kg. Added Salt +50 Units." to "Messy Restock Receipt",
        "SLIP #821 (Sales Slip): Deduct 24 boxes Wheat (50kg) sold to Client. Checked Wholesale Sugar sale 15 units sold." to "Messy Counterslip Sale",
        "SLIP #402 (Unstructured Notebook): premium wheat restock of 100 boxes. Sugar refined supply 50 count added. Lentils Grade A x20 boxes recieved." to "Warehouse Scrap Notebook"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkBackground),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .border(2.dp, PrimaryPurple, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp)),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Real-Time AI OCR Slip Input",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryPurple
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                Text(
                    text = "Provide messy, handwritten warehouse receipts. StockSync AI extracts item lists, SKUs, adjust directions, and matches categories automatically.",
                    color = GraySecondary,
                    fontSize = 11.sp
                )

                // Simulated handwriting preview paper block!
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFDF6E3), // Classic penmanship notebook paper yellow color
                    border = BorderStroke(1.dp, Color(0xFFE6D6B3)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .padding(vertical = 4.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                        // Drawing notebook lines
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val spacing = 24.dp.toPx()
                            var y = spacing
                            while (y < size.height) {
                                drawLine(
                                    color = Color(0xFFE2D6B5),
                                    start = androidx.compose.ui.geometry.Offset(0f, y),
                                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                                    strokeWidth = 1.dp.toPx()
                                )
                                y += spacing
                            }
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Edit, contentDescription = "Pen", tint = Color(0xFF268BD2), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Hand-written Wholesale Slip Simulator (Gemini Multimodal)",
                                    fontSize = 9.sp,
                                    color = Color.Gray,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (customInwardText.isEmpty()) "« Draw or draft handwritten scraps below... »" else customInwardText,
                                style = TextStyle(
                                    color = Color(0xFF002B36),
                                    fontFamily = FontFamily.Cursive,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 24.sp
                                ),
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Preset handwriting samples picker
                Text(
                    text = "Load Messy Handwritten Slip Presets:",
                    color = PrimaryPurple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    mockSlips.forEachIndexed { idx, (textVal, label) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurface)
                                .clickable { customInwardText = textVal }
                                .padding(10.dp),
                            contentAlignment = Alignment.TopStart
                        ) {
                            Column {
                                Text(label, color = PrimaryPurple, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(textVal, color = GraySecondary, fontSize = 8.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                // Custom handwritten slip terminal
                OutlinedTextField(
                    value = customInwardText,
                    onValueChange = { customInwardText = it },
                    placeholder = { Text("Or sribble / write custom slip notes manually to analyze...", fontSize = 12.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryPurple,
                        unfocusedBorderColor = DarkSurface,
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth().height(96.dp).testTag("custom_ocr_notepad")
                )

                // Button submit to parsing with loading state
                Button(
                    onClick = {
                        if (customInwardText.isBlank()) {
                            customInwardText = mockSlips[0].first
                        }
                        viewModel.runHandwrittenSlipOcr(customInwardText)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("ocr_submit_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isProcessing
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = AccentPurpleBg, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(Icons.Default.CloudQueue, contentDescription = "AI", tint = AccentPurpleBg)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Extract handwriting data (Gemini API)", color = AccentPurpleBg, fontWeight = FontWeight.Black)
                    }
                }

                // Parsing outputs
                AnimatedVisibility(visible = isProcessing) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "AI CLOUD OCR: TRANSCRIBING HANDWRITTEN SLIP...",
                            color = PrimaryPurple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(color = PrimaryPurple, modifier = Modifier.fillMaxWidth())
                    }
                }

                if (ocrResult != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, PrimaryPurple, RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Slip type banner
                            val restock = ocrResult!!.slipType == "RESTOCK"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Slip #${ocrResult!!.slipNumber}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (restock) LiveGreen.copy(alpha = 0.2f) else LowStockRed.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = ocrResult!!.slipType,
                                        color = if (restock) LiveGreen else LowStockRed,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Extracted Products Ledger:",
                                color = PrimaryPurple,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )

                            ocrResult!!.items.forEach { extracted ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(extracted.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("SKU: ${extracted.sku} • Category: ${extracted.category}", fontSize = 9.sp, color = GraySecondary)
                                    }
                                    Text(
                                        text = "${if (restock) "+" else "-"}${extracted.changeAmount} units",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (restock) LiveGreen else LowStockRed
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = ocrModeInfo,
                                color = LiveGreen,
                                fontSize = 9.sp,
                                fontStyle = FontStyle.Italic
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.dismissOcr() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Dismiss", color = Color.White)
                                }

                                Button(
                                    onClick = {
                                        viewModel.commitOcrResult()
                                        Toast.makeText(viewModel.getApplication(), "Stock Levels Updated in Real-Time!", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    },
                                    modifier = Modifier.weight(1f).testTag("ocr_confirm_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                                ) {
                                    Text("Commit to Db", color = AccentPurpleBg)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// RETAIL INVOICE PRINT THERMAL DIALOG overlay
// ==========================================
@Composable
fun ReceiptDialog(
    receipt: SaleReceipt,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(2.dp, PrimaryPurple),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Circular icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AccentPurpleBg)
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Approved", tint = Color.White, modifier = Modifier.size(24.dp))
                }

                Text(
                    text = "TRANSACTION COMPLETED",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "The inventory quantities have been automatically decreased.",
                    fontSize = 11.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Printable Receipt thermal layout
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF9F9F9), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "STOCKSYNC WHOLESALE SERVICES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "INVOICE RECEIPT ID: ${receipt.receiptNumber}",
                        fontSize = 9.sp,
                        color = Color.Black,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(receipt.timestamp))}",
                        fontSize = 8.sp,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = Color.Black, thickness = 1.dp)

                    // Parse json items
                    // For prototype printing fallback, we represent items as static list lines or can display details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("ITEM (QTY)", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black, fontFamily = FontFamily.Monospace)
                        Text("TOTAL", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black, fontFamily = FontFamily.Monospace)
                    }

                    // We can display the raw text or decode it conceptually
                    Text(
                        text = "POS Billing Transaction: items resolved and logged to audit database register. Stock items decreased.",
                        fontSize = 8.sp,
                        color = Color.DarkGray,
                        fontFamily = FontFamily.Monospace
                    )

                    HorizontalDivider(color = Color.Black, thickness = 1.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL AMOUNT PAID:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "$${String.format("%.2f", receipt.totalAmount)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Green,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        text = "THANK YOU FOR YOUR PATRONAGE!",
                        fontSize = 8.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onDismiss() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close", color = Color.Black)
                    }

                    Button(
                        onClick = {
                            // Simulates sending to portable warehouse printer
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = "Print", tint = AccentPurpleBg)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print Receipt", color = AccentPurpleBg)
                    }
                }
            }
        }
    }
}

// ==========================================
// DIALOG FOR EDITING / CREATING PRODUCT
// ==========================================
@Composable
fun ItemEditDialog(
    item: InventoryItem? = null,
    onDismiss: () -> Unit,
    onSave: (InventoryItem) -> Unit,
    onDelete: ((InventoryItem) -> Unit)? = null
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var sku by remember { mutableStateOf(item?.sku ?: "") }
    var category by remember { mutableStateOf(item?.category ?: "Grains") }
    var quantity by remember { mutableStateOf(item?.quantity?.toString() ?: "50") }
    var threshold by remember { mutableStateOf(item?.lowThreshold?.toString() ?: "15") }
    var price by remember { mutableStateOf(item?.price?.toString() ?: "10.0") }
    var unit by remember { mutableStateOf(item?.unit ?: "box") }

    val categories = listOf("Grains", "Sweeteners", "Legumes", "Packaging", "Spices", "Beverages")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (item == null) "Create Wholesale Product" else "Update Item Specifications",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryPurple
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product / Item Name") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryPurple, unfocusedBorderColor = DarkBackground),
                    modifier = Modifier.fillMaxWidth().testTag("edit_item_name")
                )

                OutlinedTextField(
                    value = sku,
                    onValueChange = { sku = it.uppercase() },
                    label = { Text("Item unique SKU") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryPurple, unfocusedBorderColor = DarkBackground),
                    modifier = Modifier.fillMaxWidth().testTag("edit_item_sku")
                )

                // Category scroll options
                Text("Select wholesale category:", color = PrimaryPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val active = category == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) PrimaryPurple else DarkBackground)
                                .clickable { category = cat }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(cat, color = if (active) AccentPurpleBg else Color.White, fontSize = 10.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = threshold,
                        onValueChange = { threshold = it },
                        label = { Text("Threshold Alert") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Price ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit (e.g. bag)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", color = Color.White)
                    }

                    if (item != null && onDelete != null) {
                        Button(
                            onClick = { onDelete(item) },
                            colors = ButtonDefaults.buttonColors(containerColor = LowStockRed),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Delete product", color = LowStockText)
                        }
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank() && sku.isNotBlank()) {
                                onSave(
                                    InventoryItem(
                                        id = item?.id ?: 0,
                                        name = name,
                                        sku = sku,
                                        quantity = quantity.toIntOrNull() ?: 0,
                                        lowThreshold = threshold.toIntOrNull() ?: 10,
                                        category = category,
                                        price = price.toDoubleOrNull() ?: 10.0,
                                        unit = unit
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        modifier = Modifier.weight(1f).testTag("save_item_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Spec", color = AccentPurpleBg)
                    }
                }
            }
        }
    }
}
