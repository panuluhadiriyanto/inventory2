package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.viewModel.AppScreen
import com.example.ui.viewModel.CartItem
import com.example.ui.viewModel.InventoryAccountingViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SalesTransactionScreen(
    viewModel: InventoryAccountingViewModel,
    modifier: Modifier = Modifier
) {
    val cart by viewModel.salesCart.collectAsState()
    val allItems by viewModel.items.collectAsState()
    val allCustomers by viewModel.customers.collectAsState()
    val allCategories by viewModel.categories.collectAsState()

    var discountInput by remember { mutableStateOf("") }
    var amountPaidInput by remember { mutableStateOf("") }
    var paymentType by remember { mutableStateOf("CASH") } // "CASH" or "CREDIT"
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }

    var expandedCustomerMenu by remember { mutableStateOf(false) }
    var showBarcodeScannerSim by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf<Sale?>(null) }
    var itemSearchQuery by remember { mutableStateOf("") }
    var showAddCustomerDialog by remember { mutableStateOf(false) }

    // Filtered items + Category filter
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = remember(allCategories) { listOf("All") + allCategories.map { it.name }.distinct() }
    val filteredItems = remember(allItems, itemSearchQuery, selectedCategory, allCategories) {
        allItems.filter { item ->
            val itemCategoryName = allCategories.find { it.id == item.categoryId }?.name ?: "All"
            (selectedCategory == "All" || itemCategoryName == selectedCategory) &&
            (item.name.contains(itemSearchQuery, ignoreCase = true) || item.skuBarcode.contains(itemSearchQuery))
        }
    }

    // Calculations
    val subtotal = cart.sumOf { it.item.sellingPrice * it.quantity }
    val rxDiscount = discountInput.toDoubleOrNull() ?: 0.0
    val total = maxOf(0.0, subtotal - rxDiscount)
    val rxAmountPaid = amountPaidInput.toDoubleOrNull() ?: 0.0
    val changeAmount = if (paymentType == "CASH") maxOf(0.0, rxAmountPaid - total) else 0.0

    // Focus manager to clear focus on key events
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    Box(modifier = modifier.fillMaxSize().testTag("sales_transaction_screen")) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isCompact = maxWidth < 720.dp

            if (isCompact) {
                // Phone/Compact Screen: Mobile-friendly Tabbed Layout
                var activeMobileTab by remember { mutableStateOf(0) }
                val cartItemCount = cart.sumOf { it.quantity }

                Column(modifier = Modifier.fillMaxSize()) {
                    TabRow(
                        selectedTabIndex = activeMobileTab,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Tab(
                            selected = activeMobileTab == 0,
                            onClick = { activeMobileTab = 0 },
                            text = { Text("🏷️ Produk (${filteredItems.size})", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = activeMobileTab == 1,
                            onClick = { activeMobileTab = 1 },
                            text = { Text("🛒 Keranjang ($cartItemCount)", fontWeight = FontWeight.Bold) }
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        if (activeMobileTab == 0) {
                            // Tab 0: Search & Catalog Product List
                            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                                SalesProductListArea(
                                    searchQuery = itemSearchQuery,
                                    onSearchChange = { itemSearchQuery = it },
                                    onScannerClick = { showBarcodeScannerSim = true },
                                    categories = categories,
                                    selectedCategory = selectedCategory,
                                    onCategorySelect = { selectedCategory = it },
                                    filteredItems = filteredItems,
                                    allCategories = allCategories,
                                    cart = cart,
                                    onItemClick = { viewModel.addToSalesCart(it) }
                                )
                            }
                        } else {
                            // Tab 1: Shopping Cart & Instant Receipt Billing
                            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                                SalesCartSummaryArea(
                                    cart = cart,
                                    customers = allCustomers,
                                    selectedCustomer = selectedCustomer,
                                    onSelectCustomer = { selectedCustomer = it },
                                    onAddCustomerClick = { showAddCustomerDialog = true },
                                    paymentType = paymentType,
                                    onPaymentTypeChange = { paymentType = it },
                                    discountInput = discountInput,
                                    onDiscountChange = { discountInput = it },
                                    amountPaidInput = amountPaidInput,
                                    onAmountPaidChange = { amountPaidInput = it },
                                    subtotal = subtotal,
                                    discount = rxDiscount,
                                    total = total,
                                    amountPaid = rxAmountPaid,
                                    changeAmount = changeAmount,
                                    onUpdateCartQty = { itemId, qty -> viewModel.updateSalesCartQuantity(itemId, qty) },
                                    onCheckoutClick = {
                                        viewModel.checkoutSales(
                                            customer = selectedCustomer,
                                            discount = rxDiscount,
                                            paymentType = paymentType,
                                            amountPaid = rxAmountPaid,
                                            onSuccess = { createdSale ->
                                                showReceiptDialog = createdSale
                                                // Reset local fields
                                                discountInput = ""
                                                amountPaidInput = ""
                                                selectedCustomer = null
                                            }
                                        )
                                        focusManager.clearFocus()
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // Wide Screen: Split Layout (Daftar Produk on the Left, Keranjang Bill on the Right)
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.weight(1.1f).padding(16.dp)) {
                        SalesProductListArea(
                            searchQuery = itemSearchQuery,
                            onSearchChange = { itemSearchQuery = it },
                            onScannerClick = { showBarcodeScannerSim = true },
                            categories = categories,
                            selectedCategory = selectedCategory,
                            onCategorySelect = { selectedCategory = it },
                            filteredItems = filteredItems,
                            allCategories = allCategories,
                            cart = cart,
                            onItemClick = { viewModel.addToSalesCart(it) }
                        )
                    }

                    // Separation Divider line
                    Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outlineVariant))

                    Column(
                        modifier = Modifier
                            .weight(0.9f)
                            .fillMaxHeight()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Keranjang Belanja",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        SalesCartSummaryArea(
                            cart = cart,
                            customers = allCustomers,
                            selectedCustomer = selectedCustomer,
                            onSelectCustomer = { selectedCustomer = it },
                            onAddCustomerClick = { showAddCustomerDialog = true },
                            paymentType = paymentType,
                            onPaymentTypeChange = { paymentType = it },
                            discountInput = discountInput,
                            onDiscountChange = { discountInput = it },
                            amountPaidInput = amountPaidInput,
                            onAmountPaidChange = { amountPaidInput = it },
                            subtotal = subtotal,
                            discount = rxDiscount,
                            total = total,
                            amountPaid = rxAmountPaid,
                            changeAmount = changeAmount,
                            onUpdateCartQty = { itemId, qty -> viewModel.updateSalesCartQuantity(itemId, qty) },
                            onCheckoutClick = {
                                viewModel.checkoutSales(
                                    customer = selectedCustomer,
                                    discount = rxDiscount,
                                    paymentType = paymentType,
                                    amountPaid = rxAmountPaid,
                                    onSuccess = { createdSale ->
                                        showReceiptDialog = createdSale
                                        // Reset fields
                                        discountInput = ""
                                        amountPaidInput = ""
                                        selectedCustomer = null
                                    }
                                )
                                focusManager.clearFocus()
                            }
                        )
                    }
                }
            }
        }

        // --- QUICK-ADD CUSTOMER DIALOG ---
        if (showAddCustomerDialog) {
            var name by remember { mutableStateOf("") }
            var phone by remember { mutableStateOf("") }
            var address by remember { mutableStateOf("") }
            var nameError by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showAddCustomerDialog = false },
                title = { Text("Registrasi Pelanggan Baru", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                nameError = it.trim().isEmpty()
                            },
                            label = { Text("Nama Pelanggan") },
                            isError = nameError,
                            modifier = Modifier.fillMaxWidth().testTag("add_customer_name"),
                            singleLine = true
                        )
                        if (nameError) {
                            Text("Nama pelanggan tidak boleh kosong!", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Nomor Telepon") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )

                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Alamat") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (name.trim().isEmpty()) {
                                nameError = true
                            } else {
                                viewModel.addCustomer(name.trim(), phone.trim(), address.trim())
                                showAddCustomerDialog = false
                            }
                        },
                        modifier = Modifier.testTag("confirm_quick_add_customer")
                    ) {
                        Text("Simpan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddCustomerDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }

        // --- BARCODE SCANNER SIMULATOR DIALOG/OVERLAY ---
        if (showBarcodeScannerSim) {
            BarcodeScannerSimulator(
                allItems = allItems,
                onDetected = { item ->
                    viewModel.addToSalesCart(item)
                    showBarcodeScannerSim = false
                },
                onDismiss = { showBarcodeScannerSim = false }
            )
        }

        // --- THERMAL INVOICE RECEIPT DIALOG ---
        if (showReceiptDialog != null) {
            SalesReceiptDialog(
                sale = showReceiptDialog!!,
                cartItemsList = cart,
                onDismiss = { showReceiptDialog = null }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesProductListArea(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onScannerClick: () -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit,
    filteredItems: List<Item>,
    allCategories: List<Category>,
    cart: List<CartItem>,
    onItemClick: (Item) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Simple and high-contrast Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Cari Produk / SKU Barcode...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = {
                IconButton(onClick = onScannerClick) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode", tint = MaterialTheme.colorScheme.primary)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Categories selector
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { onCategorySelect(cat) },
                    label = { Text(cat) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Inventory,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Produk tidak ditemukan", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            // Product Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredItems) { item ->
                    val quantityInCart = cart.find { it.item.id == item.id }?.quantity ?: 0
                    val outOfStock = item.stockQuantity <= 0

                    Card(
                        onClick = { if (!outOfStock) onItemClick(item) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.85f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = if (quantityInCart > 0) 2.dp else 1.dp,
                            color = if (quantityInCart > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (outOfStock) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                            // Product Image/Badge Section
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(
                                        if (outOfStock) MaterialTheme.colorScheme.surfaceVariant 
                                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                                        RoundedCornerShape(8.dp)
                                    )
                            ) {
                                if (!item.imageUri.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = item.imageUri,
                                        contentDescription = item.name,
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Image,
                                        contentDescription = null,
                                        modifier = Modifier.align(Alignment.Center).size(36.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }

                                // Cart counter badge
                                if (quantityInCart > 0) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                                    ) {
                                        Text(
                                            "${quantityInCart}x",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Stock status badge
                                Surface(
                                    color = if (outOfStock) MaterialTheme.colorScheme.errorContainer 
                                            else if (item.stockQuantity <= item.minStockAlert) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                            else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                                ) {
                                    Text(
                                        text = if (outOfStock) "Habis" else "Stok: ${item.stockQuantity}",
                                        color = if (outOfStock) MaterialTheme.colorScheme.onErrorContainer 
                                                else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                item.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            val categoryName = allCategories.find { it.id == item.categoryId }?.name ?: "Umum"
                            Text(
                                categoryName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                formatRupiah(item.sellingPrice),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesCartSummaryArea(
    cart: List<CartItem>,
    customers: List<Customer>,
    selectedCustomer: Customer?,
    onSelectCustomer: (Customer?) -> Unit,
    onAddCustomerClick: () -> Unit,
    paymentType: String,
    onPaymentTypeChange: (String) -> Unit,
    discountInput: String,
    onDiscountChange: (String) -> Unit,
    amountPaidInput: String,
    onAmountPaidChange: (String) -> Unit,
    subtotal: Double,
    discount: Double,
    total: Double,
    amountPaid: Double,
    changeAmount: Double,
    onUpdateCartQty: (Int, Int) -> Unit,
    onCheckoutClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (cart.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Keranjang Anda kosong", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Klik produk untuk menambahkannya", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            // Cart Items List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(cart) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.item.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(formatRupiah(entry.item.sellingPrice), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { onUpdateCartQty(entry.item.id, entry.quantity - 1) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Kurang", tint = MaterialTheme.colorScheme.error)
                                }
                                Text("${entry.quantity}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                                IconButton(
                                    onClick = { onUpdateCartQty(entry.item.id, entry.quantity + 1) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Tambah", tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { onUpdateCartQty(entry.item.id, 0) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Customer Selection Card
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                var expandedCustomerDropdown by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = if (selectedCustomer != null) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = selectedCustomer?.name ?: "Pelanggan Umum (Eceran)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row {
                        TextButton(
                            onClick = { expandedCustomerDropdown = true },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Pilih", fontSize = 12.sp)
                        }
                        TextButton(
                            onClick = onAddCustomerClick,
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Daftar", fontSize = 12.sp)
                        }
                    }
                }

                DropdownMenu(
                    expanded = expandedCustomerDropdown,
                    onDismissRequest = { expandedCustomerDropdown = false },
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    DropdownMenuItem(
                        text = { Text("Eceran / Pelanggan Umum") },
                        onClick = {
                            onSelectCustomer(null)
                            expandedCustomerDropdown = false
                        }
                    )
                    customers.forEach { cust ->
                        DropdownMenuItem(
                            text = { Text("${cust.name} (${cust.phone})") },
                            onClick = {
                                onSelectCustomer(cust)
                                expandedCustomerDropdown = false
                            }
                        )
                    }
                }
            }
        }

        // Checkout Inputs
        if (cart.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Payment Mode segmented choice
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onPaymentTypeChange("CASH") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (paymentType == "CASH") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Tunai (CASH)", color = if (paymentType == "CASH") Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { onPaymentTypeChange("CREDIT") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (paymentType == "CREDIT") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Piutang (CREDIT)", color = if (paymentType == "CREDIT") Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = discountInput,
                        onValueChange = onDiscountChange,
                        label = { Text("Diskon (Rp)") },
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )

                    if (paymentType == "CASH") {
                        OutlinedTextField(
                            value = amountPaidInput,
                            onValueChange = onAmountPaidChange,
                            label = { Text("Uang Tunai (Rp)") },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                // Billing Summary Sheet
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal", style = MaterialTheme.typography.bodyMedium)
                            Text(formatRupiah(subtotal), fontWeight = FontWeight.SemiBold)
                        }
                        if (discount > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Diskon", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                                Text("- ${formatRupiah(discount)}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Bayar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(formatRupiah(total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }

                        if (paymentType == "CASH" && amountPaid > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Kembalian", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = formatRupiah(changeAmount),
                                    fontWeight = FontWeight.Bold,
                                    color = if (changeAmount >= 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                val promptError = when {
                    paymentType == "CASH" && amountPaid < total -> "Uang dibayar kurang!"
                    paymentType == "CREDIT" && selectedCustomer == null -> "Piutang harus pilih Pelanggan!"
                    else -> null
                }

                if (promptError != null) {
                    Text(
                        text = promptError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                Button(
                    onClick = onCheckoutClick,
                    enabled = (promptError == null),
                    modifier = Modifier.fillMaxWidth().testTag("sales_checkout_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (paymentType == "CASH") "Selesaikan Pembayaran" else "Simpan Transaksi Piutang",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseTransactionScreen(
    viewModel: InventoryAccountingViewModel,
    modifier: Modifier = Modifier
) {
    val cart by viewModel.purchasesCart.collectAsState()
    val allItems by viewModel.items.collectAsState()
    val allSuppliers by viewModel.suppliers.collectAsState()
    val allCategories by viewModel.categories.collectAsState()

    var paymentType by remember { mutableStateOf("CASH") } // "CASH" or "CREDIT"
    var selectedSupplier by remember { mutableStateOf<Supplier?>(null) }
    var itemSearchQuery by remember { mutableStateOf("") }
    var showAddSupplierDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Auto-select first supplier if none is selected and list changes
    LaunchedEffect(allSuppliers) {
        if (selectedSupplier == null && allSuppliers.isNotEmpty()) {
            selectedSupplier = allSuppliers.first()
        }
    }

    // Filtered items + Category filter
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = remember(allCategories) { listOf("All") + allCategories.map { it.name }.distinct() }
    val filteredItems = remember(allItems, itemSearchQuery, selectedCategory, allCategories) {
        allItems.filter { item ->
            val itemCategoryName = allCategories.find { it.id == item.categoryId }?.name ?: "All"
            (selectedCategory == "All" || itemCategoryName == selectedCategory) &&
            (item.name.contains(itemSearchQuery, ignoreCase = true) || item.skuBarcode.contains(itemSearchQuery))
        }
    }

    val total = cart.sumOf { it.item.purchasePrice * it.quantity }

    Box(modifier = modifier.fillMaxSize().testTag("purchase_transaction_screen")) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isCompact = maxWidth < 720.dp

            if (isCompact) {
                // Phone layout
                var activeMobileTab by remember { mutableStateOf(0) }
                val cartItemCount = cart.sumOf { it.quantity }

                Column(modifier = Modifier.fillMaxSize()) {
                    TabRow(
                        selectedTabIndex = activeMobileTab,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Tab(
                            selected = activeMobileTab == 0,
                            onClick = { activeMobileTab = 0 },
                            text = { Text("🏷️ Stok Pasok (${filteredItems.size})", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = activeMobileTab == 1,
                            onClick = { activeMobileTab = 1 },
                            text = { Text("📥 Keranjang PO ($cartItemCount)", fontWeight = FontWeight.Bold) }
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        if (activeMobileTab == 0) {
                            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                                PurchaseProductListArea(
                                    searchQuery = itemSearchQuery,
                                    onSearchChange = { itemSearchQuery = it },
                                    categories = categories,
                                    selectedCategory = selectedCategory,
                                    onCategorySelect = { selectedCategory = it },
                                    filteredItems = filteredItems,
                                    allCategories = allCategories,
                                    cart = cart,
                                    onItemClick = { viewModel.addToPurchasesCart(it) }
                                )
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                                PurchaseCartSummaryArea(
                                    cart = cart,
                                    suppliers = allSuppliers,
                                    selectedSupplier = selectedSupplier,
                                    onSelectSupplier = { selectedSupplier = it },
                                    onAddSupplierClick = { showAddSupplierDialog = true },
                                    paymentType = paymentType,
                                    onPaymentTypeChange = { paymentType = it },
                                    total = total,
                                    onUpdateCartQty = { itemId, qty -> viewModel.updatePurchasesCartQuantity(itemId, qty) },
                                    onCheckoutClick = {
                                        selectedSupplier?.let { supplier ->
                                            viewModel.checkoutPurchase(
                                                supplier = supplier,
                                                paymentType = paymentType,
                                                amountPaid = total,
                                                onSuccess = {
                                                    showSuccessDialog = true
                                                    paymentType = "CASH"
                                                }
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // Wide split screen layout
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.weight(1.1f).padding(16.dp)) {
                        PurchaseProductListArea(
                            searchQuery = itemSearchQuery,
                            onSearchChange = { itemSearchQuery = it },
                            categories = categories,
                            selectedCategory = selectedCategory,
                            onCategorySelect = { selectedCategory = it },
                            filteredItems = filteredItems,
                            allCategories = allCategories,
                            cart = cart,
                            onItemClick = { viewModel.addToPurchasesCart(it) }
                        )
                    }

                    Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(MaterialTheme.colorScheme.outlineVariant))

                    Column(
                        modifier = Modifier
                            .weight(0.9f)
                            .fillMaxHeight()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Daftar Rencana Kulakan (PO)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        PurchaseCartSummaryArea(
                            cart = cart,
                            suppliers = allSuppliers,
                            selectedSupplier = selectedSupplier,
                            onSelectSupplier = { selectedSupplier = it },
                            onAddSupplierClick = { showAddSupplierDialog = true },
                            paymentType = paymentType,
                            onPaymentTypeChange = { paymentType = it },
                            total = total,
                            onUpdateCartQty = { itemId, qty -> viewModel.updatePurchasesCartQuantity(itemId, qty) },
                            onCheckoutClick = {
                                selectedSupplier?.let { supplier ->
                                    viewModel.checkoutPurchase(
                                        supplier = supplier,
                                        paymentType = paymentType,
                                        amountPaid = total,
                                        onSuccess = {
                                            showSuccessDialog = true
                                            paymentType = "CASH"
                                        }
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }

        // --- QUICK-ADD SUPPLIER DIALOG ---
        if (showAddSupplierDialog) {
            var name by remember { mutableStateOf("") }
            var phone by remember { mutableStateOf("") }
            var address by remember { mutableStateOf("") }
            var nameError by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showAddSupplierDialog = false },
                title = { Text("Registrasi Pemasok (Supplier) Baru", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                nameError = it.trim().isEmpty()
                            },
                            label = { Text("Nama Supplier / Perusahaan") },
                            isError = nameError,
                            modifier = Modifier.fillMaxWidth().testTag("add_supplier_name"),
                            singleLine = true
                        )
                        if (nameError) {
                            Text("Nama supplier tidak boleh kosong!", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Nomor HP Supplier") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )

                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Alamat") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (name.trim().isEmpty()) {
                                nameError = true
                            } else {
                                viewModel.addSupplier(name.trim(), phone.trim(), address.trim())
                                showAddSupplierDialog = false
                            }
                        },
                        modifier = Modifier.testTag("confirm_quick_add_supplier")
                    ) {
                        Text("Simpan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddSupplierDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }

        // --- PROCURE PLACED SUCCESS CHIP/ALERT ---
        if (showSuccessDialog) {
            AlertDialog(
                onDismissRequest = { showSuccessDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                        Text("Kulakan / Pasokan Sukses", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Text("Transaksi PO pembelian telah berhasil dicatat. Stok barang di Default Gudang telah diperbarui dan pembukuan jurnal akuntansi telah disesuaikan secara otomatis.")
                },
                confirmButton = {
                    Button(onClick = { showSuccessDialog = false }) {
                        Text("Selesai")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseProductListArea(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit,
    filteredItems: List<Item>,
    allCategories: List<Category>,
    cart: List<CartItem>,
    onItemClick: (Item) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Cari barang kulakan...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { onCategorySelect(cat) },
                    label = { Text(cat) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Inventory,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Produk tidak ditemukan", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredItems) { item ->
                    val quantityInCart = cart.find { it.item.id == item.id }?.quantity ?: 0

                    Card(
                        onClick = { onItemClick(item) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.85f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = if (quantityInCart > 0) 2.dp else 1.dp,
                            color = if (quantityInCart > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                                        RoundedCornerShape(8.dp)
                                    )
                            ) {
                                if (!item.imageUri.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = item.imageUri,
                                        contentDescription = item.name,
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Image,
                                        contentDescription = null,
                                        modifier = Modifier.align(Alignment.Center).size(36.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }

                                if (quantityInCart > 0) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
                                    ) {
                                        Text(
                                            "${quantityInCart}x",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                                ) {
                                    Text(
                                        "Milik: ${item.stockQuantity}",
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                item.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            val categoryName = allCategories.find { it.id == item.categoryId }?.name ?: "Umum"
                            Text(
                                categoryName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                "Hrg Beli: " + formatRupiah(item.purchasePrice),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseCartSummaryArea(
    cart: List<CartItem>,
    suppliers: List<Supplier>,
    selectedSupplier: Supplier?,
    onSelectSupplier: (Supplier?) -> Unit,
    onAddSupplierClick: () -> Unit,
    paymentType: String,
    onPaymentTypeChange: (String) -> Unit,
    total: Double,
    onUpdateCartQty: (Int, Int) -> Unit,
    onCheckoutClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (cart.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Keranjang kulakan kosong", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Klik katalog barang untuk merencana", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(cart) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.item.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("Beli: " + formatRupiah(entry.item.purchasePrice), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { onUpdateCartQty(entry.item.id, entry.quantity - 1) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Kurang", tint = MaterialTheme.colorScheme.error)
                                }
                                Text("${entry.quantity}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                                IconButton(
                                    onClick = { onUpdateCartQty(entry.item.id, entry.quantity + 1) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Tambah", tint = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { onUpdateCartQty(entry.item.id, 0) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Supplier Selection Row
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                var expandedSupplierDropdown by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = if (selectedSupplier != null) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = selectedSupplier?.name ?: "Harap Pilih Supplier...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (selectedSupplier == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row {
                        TextButton(
                            onClick = { expandedSupplierDropdown = true },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Pilih", fontSize = 12.sp)
                        }
                        TextButton(
                            onClick = onAddSupplierClick,
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Daftar", fontSize = 12.sp)
                        }
                    }
                }

                DropdownMenu(
                    expanded = expandedSupplierDropdown,
                    onDismissRequest = { expandedSupplierDropdown = false },
                    modifier = Modifier.fillMaxWidth(0.8f)
                ) {
                    suppliers.forEach { supp ->
                        DropdownMenuItem(
                            text = { Text("${supp.name} (${supp.phone})") },
                            onClick = {
                                onSelectSupplier(supp)
                                expandedSupplierDropdown = false
                            }
                        )
                    }
                }
            }
        }

        if (cart.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Payment type segmented choice
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onPaymentTypeChange("CASH") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (paymentType == "CASH") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Tunai (CASH)", color = if (paymentType == "CASH") Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { onPaymentTypeChange("CREDIT") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (paymentType == "CREDIT") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Utang (CREDIT)", color = if (paymentType == "CREDIT") Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Jumlah Item", style = MaterialTheme.typography.bodyMedium)
                            Text("${cart.sumOf { it.quantity }} Unit", fontWeight = FontWeight.SemiBold)
                        }
                        Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Pembelian PO", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(formatRupiah(total), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                val promptError = when {
                    selectedSupplier == null -> "Wajib pilih Supplier!"
                    else -> null
                }

                if (promptError != null) {
                    Text(
                        text = promptError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }

                Button(
                    onClick = onCheckoutClick,
                    enabled = (promptError == null),
                    modifier = Modifier.fillMaxWidth().testTag("purchase_checkout_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.TransitEnterexit, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (paymentType == "CASH") "Bayar + Masukkan Stok" else "Simpan PO sebagai Utang",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReturnTransactionScreen(
    viewModel: InventoryAccountingViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.items.collectAsState()
    val returnsHistory by viewModel.returns.collectAsState()

    var showFormDialog by remember { mutableStateOf(false) }

    var invoiceRef by remember { mutableStateOf("") }
    var returnType by remember { mutableStateOf("SALE_RETURN") } // "SALE_RETURN" or "PURCHASE_RETURN"
    var reasonInput by remember { mutableStateOf("") }
    var returnedItemQty by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf<Item?>(null) }
    var expandedItemDropdown by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "SALE_RETURN", "PURCHASE_RETURN"

    val totalSaleReturns = returnsHistory.filter { it.type == "SALE_RETURN" }.sumOf { it.totalRefund }
    val totalPurchaseReturns = returnsHistory.filter { it.type == "PURCHASE_RETURN" }.sumOf { it.totalRefund }

    val filteredReturns = returnsHistory.filter {
        selectedFilter == "ALL" || it.type == selectedFilter
    }

    @Composable
    fun ReturnFormContent(isCompact: Boolean = false) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Tipe Retur",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    onClick = { returnType = "SALE_RETURN" },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = if (returnType == "SALE_RETURN") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, if (returnType == "SALE_RETURN") MaterialTheme.colorScheme.primary else Color.Transparent)
                ) {
                    Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Retur Jual (Pelanggan)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (returnType == "SALE_RETURN") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Surface(
                    onClick = { returnType = "PURCHASE_RETURN" },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = if (returnType == "PURCHASE_RETURN") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, if (returnType == "PURCHASE_RETURN") MaterialTheme.colorScheme.error else Color.Transparent)
                ) {
                    Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Retur Beli (Ke Supplier)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (returnType == "PURCHASE_RETURN") MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            OutlinedTextField(
                value = invoiceRef,
                onValueChange = { invoiceRef = it },
                label = { Text("No. Invoice / PO Rujukan") },
                placeholder = { Text("INV-xxx atau PO-xxx") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            )

            // Item Selection dropdown
            Column {
                Text("Pilih Produk", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                Box {
                    Surface(
                        onClick = { expandedItemDropdown = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .height(52.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (selectedItem?.imageUri != null) {
                                    AsyncImage(
                                        model = selectedItem!!.imageUri,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.outline)
                                }
                                val itmName = selectedItem?.name ?: "Klik untuk pilih barang"
                                Text(itmName, fontSize = 13.sp)
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = expandedItemDropdown,
                        onDismissRequest = { expandedItemDropdown = false },
                        modifier = Modifier.fillMaxWidth(if (isCompact) 0.85f else 0.35f).heightIn(max = 240.dp)
                    ) {
                        items.forEach { itm ->
                            DropdownMenuItem(
                                leadingIcon = {
                                    if (itm.imageUri != null) {
                                        AsyncImage(
                                            model = itm.imageUri,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(20.dp))
                                    }
                                },
                                text = { Text(itm.name, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                                onClick = {
                                    selectedItem = itm
                                    expandedItemDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = returnedItemQty,
                    onValueChange = { returnedItemQty = it },
                    label = { Text("Jumlah Qty") },
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = reasonInput,
                    onValueChange = { reasonInput = it },
                    label = { Text("Alasan Retur") },
                    placeholder = { Text("Rusak/Salah kirim") },
                    modifier = Modifier.weight(2f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Quick Preset Reasons
            Text("Pilih Preset Alasan:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            val presets = listOf("Cacat Pabrik 📦", "Kemasan Rusak 💔", "Salah Varian 🏷️", "Kedaluwarsa ⏳")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(presets.size) { i ->
                    SuggestionChip(
                        onClick = { reasonInput = presets[i].replace(Regex("[^a-zA-Z ]"), "").trim() },
                        label = { Text(presets[i], fontSize = 11.sp) }
                    )
                }
            }

            if (!isCompact) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (invoiceRef.isNotBlank() && selectedItem != null && returnedItemQty.toIntOrNull() != null) {
                            val qty = returnedItemQty.toInt()
                            viewModel.submitReturn(
                                invoiceRef = invoiceRef.trim(),
                                type = returnType,
                                reason = reasonInput.trim(),
                                returnedItems = listOf(Pair(selectedItem!!, qty))
                            )
                            // Reset
                            invoiceRef = ""
                            reasonInput = ""
                            returnedItemQty = ""
                            selectedItem = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(8.dp),
                    enabled = invoiceRef.isNotBlank() && selectedItem != null && returnedItemQty.isNotBlank()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kirim Data Retur", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().padding(16.dp).testTag("return_transaction_screen")) {
        val isWide = maxWidth >= 768.dp

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // LEFT COLUMN (Main Screen with list & stats)
            Column(
                modifier = Modifier
                    .weight(if (isWide) 1.2f else 1f)
                    .fillMaxHeight()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Retur Transaksi", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Pengembalian barang terintegrasi dengan akuntansi otomatis", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!isWide) {
                        Button(
                            onClick = {
                                invoiceRef = ""
                                reasonInput = ""
                                returnedItemQty = ""
                                selectedItem = null
                                showFormDialog = true
                            },
                            modifier = Modifier.height(34.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                Icons.Default.SettingsBackupRestore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Buat Retur", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Stats Dashboard (Sederhana/Minimalist)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Retur Jual (Refund)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            Text(formatRupiah(totalSaleReturns), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Retur Beli (Klaim)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            Text(formatRupiah(totalPurchaseReturns), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // Filter Buttons in Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == "ALL",
                        onClick = { selectedFilter = "ALL" },
                        label = { Text("Semua") }
                    )
                    FilterChip(
                        selected = selectedFilter == "SALE_RETURN",
                        onClick = { selectedFilter = "SALE_RETURN" },
                        label = { Text("Retur Jual") }
                    )
                    FilterChip(
                        selected = selectedFilter == "PURCHASE_RETURN",
                        onClick = { selectedFilter = "PURCHASE_RETURN" },
                        label = { Text("Retur Beli") }
                    )
                }

                // Returns History List
                if (filteredReturns.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Default.SettingsBackupRestore,
                        message = "Belum ada catatan retur",
                        hint = "Retur barang yang dikirim atau diterima akan tercantum di sini."
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(filteredReturns) { ret ->
                            val isSaleReturn = ret.type == "SALE_RETURN"
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    // Let's have a colorful status strip on left
                                    Box(
                                        modifier = Modifier
                                            .width(5.dp)
                                            .height(90.dp)
                                            .background(if (isSaleReturn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                                    )

                                    Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(ret.returnNumber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                // Date
                                                val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(ret.date))
                                                Text(dateStr, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                            }
                                            
                                            SuggestionChip(
                                                onClick = {},
                                                label = {
                                                    Text(
                                                        text = if (isSaleReturn) "Retur Jual [Masuk]" else "Retur Beli [Keluar]",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                },
                                                colors = SuggestionChipDefaults.suggestionChipColors(
                                                    containerColor = if (isSaleReturn) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                                                    labelColor = if (isSaleReturn) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                                ),
                                                border = null
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            Column {
                                                Text("Ref Invoice: ${ret.referenceInvoiceNumber}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                                                    Text("Alasan: ${ret.reason}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            Text(
                                                text = formatRupiah(ret.totalRefund),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 15.sp,
                                                color = if (isSaleReturn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // RIGHT COLUMN (Only visible on wide screen - Side panel)
            if (isWide) {
                Card(
                    modifier = Modifier
                        .weight(0.8f)
                        .fillMaxHeight(),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxSize()
                    ) {
                        Text(
                            text = "Buat Pencatatan Retur",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        ReturnFormContent(isCompact = false)
                    }
                }
            }
        }

        // Dialog version for non-wide devices
        if (!isWide && showFormDialog) {
            AlertDialog(
                onDismissRequest = { showFormDialog = false },
                title = { Text("Form Retur Barang", fontWeight = FontWeight.Bold) },
                text = {
                    Box(modifier = Modifier.heightIn(max = 420.dp)) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            item {
                                ReturnFormContent(isCompact = true)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (invoiceRef.isNotBlank() && selectedItem != null && returnedItemQty.toIntOrNull() != null) {
                                val qty = returnedItemQty.toInt()
                                viewModel.submitReturn(
                                    invoiceRef = invoiceRef.trim(),
                                    type = returnType,
                                    reason = reasonInput.trim(),
                                    returnedItems = listOf(Pair(selectedItem!!, qty))
                                )
                                showFormDialog = false
                            }
                        },
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(8.dp),
                        enabled = invoiceRef.isNotBlank() && selectedItem != null && returnedItemQty.isNotBlank()
                    ) {
                        Text("Kirim Retur", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showFormDialog = false },
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Batal", fontSize = 11.sp)
                    }
                }
            )
        }
    }
}

// --- SPECIFIC INTEGRATIONS: BARCODE CAMERA SOUND SIMULATOR ---
@Composable
fun BarcodeScannerSimulator(
    allItems: List<Item>,
    onDetected: (Item) -> Unit,
    onDismiss: () -> Unit
) {
    var searchCode by remember { mutableStateOf("") }
    var matchWarning by remember { mutableStateOf(false) }

    // Laser Animation states
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(24.dp).background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
                }
                Text("Pemindai Barcode Kameraku")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Interactive Scanner Screen with scanning crosshair and moving laser!
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(Color.Black, RoundedCornerShape(12.dp))
                        .border(2.dp, MaterialTheme.colorScheme.tertiary, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    // Crosshair Corners
                    Box(modifier = Modifier.size(24.dp).border(2.dp, Color.White, RoundedCornerShape(topStart = 4.dp)).align(Alignment.TopStart))
                    Box(modifier = Modifier.size(24.dp).border(2.dp, Color.White, RoundedCornerShape(topEnd = 4.dp)).align(Alignment.TopEnd))
                    Box(modifier = Modifier.size(24.dp).border(2.dp, Color.White, RoundedCornerShape(bottomStart = 4.dp)).align(Alignment.BottomStart))
                    Box(modifier = Modifier.size(24.dp).border(2.dp, Color.White, RoundedCornerShape(bottomEnd = 4.dp)).align(Alignment.BottomEnd))

                    // Animated scan laser!
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.02f)
                            .align(Alignment.TopCenter)
                            .offset(y = 130.dp * laserOffset)
                            .background(Color.Red)
                    )

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("[ KAMERA AKTIF ]", color = Color.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Hadapkan kode batang ke lensa", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                    }
                }

                Text("Simulasi Pengetikan / Laser Handheld:", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))

                OutlinedTextField(
                    value = searchCode,
                    onValueChange = { searchCode = it },
                    placeholder = { Text("Ketik kode SKU (Contoh: 899123456001)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // Quick buttons for Seeded Barcodes
                Text("Klik pintasan barcode (seed) untuk tiru scan:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.Start))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { searchCode = "899123456001" },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Kopi", color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 11.sp)
                    }
                    Button(
                        onClick = { searchCode = "899123456002" },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Teh", color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 11.sp)
                    }
                    Button(
                        onClick = { searchCode = "899123456003" },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("PB20", color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 11.sp)
                    }
                }

                if (matchWarning) {
                    Text("Kode SKU Barcode tidak terdaftar!", color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val found = allItems.find { it.skuBarcode == searchCode.trim() }
                    if (found != null) {
                        onDetected(found)
                    } else {
                        matchWarning = true
                    }
                }
            ) {
                Text("Picu Deteksi!")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}

// --- THERMAL INVOICE RECEIPTS PREVIEW COMPOSABLE ---
@Composable
fun SalesReceiptDialog(
    sale: Sale,
    cartItemsList: List<CartItem>,
    onDismiss: () -> Unit
) {
    val formatter = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Print, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Struk Transaksi Penjualan")
            }
        },
        text = {
            // Elegant Paper invoice look
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "FINVENTORY OUTLET",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Jl. Jenderal Sudirman No 42, JKT",
                        color = Color.DarkGray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Telp: 021-98765432",
                        color = Color.DarkGray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "--------------------------------",
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("No:", color = Color.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text(sale.invoiceNumber, color = Color.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tgl:", color = Color.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text(formatter.format(Date(sale.date)), color = Color.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Klien:", color = Color.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text(sale.customerName, color = Color.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }

                    Text(
                        text = "--------------------------------",
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Cart items rows
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        cartItemsList.forEach { ci ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(ci.item.name, color = Color.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(" ${ci.quantity} x ${formatRupiahSimple(ci.item.sellingPrice)}", color = Color.DarkGray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    Text(formatRupiahSimple(ci.item.sellingPrice * ci.quantity), color = Color.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    Text(
                        text = "--------------------------------",
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("SUBTOTAL:", color = Color.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text(formatRupiahSimple(sale.subtotal), color = Color.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    if (sale.discount > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("DISKON:", color = Color.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text("-${formatRupiahSimple(sale.discount)}", color = Color.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TOTAL:", color = Color.Black, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text(formatRupiahSimple(sale.totalPrice), color = Color.Black, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold)
                    }

                    Text(
                        text = "--------------------------------",
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("METODE:", color = Color.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text(if (sale.paymentType == "CASH") "TUNAI" else "PIUTANG", color = Color.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }

                    if (sale.paymentType == "CASH") {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("DIBAYAR:", color = Color.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text(formatRupiahSimple(sale.amountPaid), color = Color.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("KEMBALI:", color = Color.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text(formatRupiahSimple(sale.changeAmount), color = Color.Black, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "TERIMA KASIH",
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Barang yang dibeli tidak dapat ditukar",
                        color = Color.DarkGray,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss
            ) {
                Text("Print / Bagikan Struk")
            }
        }
    )
}

fun formatRupiahSimple(value: Double): String {
    return "Rp" + String.format(Locale.US, "%,.0f", value)
}
