package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

    var discountInput by remember { mutableStateOf("") }
    var amountPaidInput by remember { mutableStateOf("") }
    var paymentType by remember { mutableStateOf("CASH") } // "CASH" or "CREDIT"
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }

    var expandedCustomerMenu by remember { mutableStateOf(false) }
    var showBarcodeScannerSim by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf<Sale?>(null) }

    // Search or Select items manually
    var expandedItemMenu by remember { mutableStateOf(false) }
    var itemSearchQuery by remember { mutableStateOf("") }
    val matchingItems = remember(allItems, itemSearchQuery) {
        allItems.filter { it.name.contains(itemSearchQuery, ignoreCase = true) || it.skuBarcode.contains(itemSearchQuery) }
    }

    // Calculations
    val subtotal = cart.sumOf { it.item.sellingPrice * it.quantity }
    val rxDiscount = discountInput.toDoubleOrNull() ?: 0.0
    val total = maxOf(0.0, subtotal - rxDiscount)
    val rxAmountPaid = amountPaidInput.toDoubleOrNull() ?: 0.0
    val changeAmount = if (paymentType == "CASH") maxOf(0.0, rxAmountPaid - total) else 0.0

    Box(modifier = modifier.fillMaxSize().testTag("sales_transaction_screen")) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Kasir Penjualan", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Catat transaksi keluar, cetak struk otomatis", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Barcode Scanner button
                Button(
                    onClick = { showBarcodeScannerSim = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pindai Barcode")
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Cart Items list
                Column(modifier = Modifier.weight(1.2f)) {
                    // Manual Item Add Selector
                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        OutlinedTextField(
                            value = itemSearchQuery,
                            onValueChange = {
                                itemSearchQuery = it
                                expandedItemMenu = true
                            },
                            label = { Text("Cari Produk / Ketik Barcode...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { expandedItemMenu = !expandedItemMenu }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        DropdownMenu(
                            expanded = expandedItemMenu && matchingItems.isNotEmpty(),
                            onDismissRequest = { expandedItemMenu = false },
                            modifier = Modifier.fillMaxWidth(0.6f).heightIn(max = 240.dp)
                        ) {
                            matchingItems.forEach { item ->
                                val isCritical = item.stockQuantity <= item.minStockAlert
                                DropdownMenuItem(
                                    text = {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("${item.name} (${item.stockQuantity} Pcs)")
                                            Text(formatRupiah(item.sellingPrice), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                        }
                                    },
                                    onClick = {
                                        viewModel.addToSalesCart(item)
                                        itemSearchQuery = ""
                                        expandedItemMenu = false
                                    },
                                    enabled = item.stockQuantity > 0
                                )
                            }
                        }
                    }

                    if (cart.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Keranjang sales kosong", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Pindai barcode barang atau pilih produk secara manual di atas.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(cart) { entry ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(entry.item.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(
                                                text = "${formatRupiah(entry.item.sellingPrice)} x ${entry.quantity}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            IconButton(
                                                onClick = { viewModel.updateSalesCartQuantity(entry.item.id, entry.quantity - 1) },
                                                modifier = Modifier.size(28.dp).background(MaterialTheme.colorScheme.surface, CircleShape)
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                                            }
                                            Text("${entry.quantity}", fontWeight = FontWeight.Bold)
                                            IconButton(
                                                onClick = { viewModel.updateSalesCartQuantity(entry.item.id, entry.quantity + 1) },
                                                modifier = Modifier.size(28.dp).background(MaterialTheme.colorScheme.surface, CircleShape),
                                                enabled = entry.quantity < entry.item.stockQuantity
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Billing and Checkout calculations
                Card(
                    modifier = Modifier.weight(0.8f).fillMaxHeight(),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Ringkasan Kasir", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                        // Customer Selection
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                onClick = { expandedCustomerMenu = true },
                                modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)).height(48.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val clientName = selectedCustomer?.name ?: "Pelanggan Umum (Tunai)"
                                    Text(clientName, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }
                            DropdownMenu(
                                expanded = expandedCustomerMenu,
                                onDismissRequest = { expandedCustomerMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Pelanggan Umum (Tunai)") },
                                    onClick = {
                                        selectedCustomer = null
                                        expandedCustomerMenu = false
                                    }
                                )
                                allCustomers.forEach { cus ->
                                    DropdownMenuItem(
                                        text = { Text(cus.name) },
                                        onClick = {
                                            selectedCustomer = cus
                                            expandedCustomerMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // Payment Type Toggle (Tunai atau Kredit)
                        Column {
                            Text("Metode Pembayaran", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { paymentType = "CASH" },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (paymentType == "CASH") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Tunai", fontSize = 12.sp, color = if (paymentType == "CASH") Color.White else MaterialTheme.colorScheme.onSurface)
                                }
                                Button(
                                    onClick = {
                                        paymentType = "CREDIT"
                                        amountPaidInput = "0"
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (paymentType == "CREDIT") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    contentPadding = PaddingValues(0.dp),
                                    enabled = selectedCustomer != null // Credit model requires valid contact save
                                ) {
                                    Text("Piutang", fontSize = 12.sp, color = if (paymentType == "CREDIT") Color.White else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            if (selectedCustomer == null && paymentType == "CASH") {
                                Text("*Pilih pelanggan kontak jika ingin transaksi piutang", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Discount field
                        OutlinedTextField(
                            value = discountInput,
                            onValueChange = { discountInput = it },
                            label = { Text("Diskon Tambahan (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (paymentType == "CASH") {
                            OutlinedTextField(
                                value = amountPaidInput,
                                onValueChange = { amountPaidInput = it },
                                label = { Text("Jumlah Uang Tunai (Rp)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Summaries Billing Lines
                        Divider()

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatRupiah(subtotal), fontSize = 12.sp)
                        }
                        if (rxDiscount > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Diskon:", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                Text("- ${formatRupiah(rxDiscount)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Akhir:", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(formatRupiah(total), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        if (paymentType == "CASH" && rxAmountPaid > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Kembalian:", fontSize = 12.sp, color = Color(0xFF2E7D32))
                                Text(formatRupiah(changeAmount), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = {
                                viewModel.checkoutSales(
                                    customer = selectedCustomer,
                                    discount = rxDiscount,
                                    paymentType = paymentType,
                                    amountPaid = rxAmountPaid,
                                    onSuccess = { createdSale ->
                                        showReceiptDialog = createdSale
                                        discountInput = ""
                                        amountPaidInput = ""
                                        selectedCustomer = null
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = cart.isNotEmpty() && (paymentType == "CREDIT" || rxAmountPaid >= total)
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cetak & Selesaikan")
                        }
                    }
                }
            }
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
                cartItemsList = cart, // passing historical simulation values or just dummy for previewing
                onDismiss = { showReceiptDialog = null }
            )
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

    var paymentType by remember { mutableStateOf("CASH") } // "CASH" or "CREDIT"
    var selectedSupplier by remember { mutableStateOf<Supplier?>(null) }
    var expandedSupplierMenu by remember { mutableStateOf(false) }

    var itemSearchQuery by remember { mutableStateOf("") }
    var expandedItemMenu by remember { mutableStateOf(false) }
    val matchingItems = remember(allItems, itemSearchQuery) {
        allItems.filter { it.name.contains(itemSearchQuery, ignoreCase = true) || it.skuBarcode.contains(itemSearchQuery) }
    }

    val total = cart.sumOf { it.item.purchasePrice * it.quantity }

    Box(modifier = modifier.fillMaxSize().testTag("purchase_transaction_screen")) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Transaksi Kulakan / Pembelian", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Restock produk & tambah persediaan inventaris", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Cart items
                Column(modifier = Modifier.weight(1.2f)) {
                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        OutlinedTextField(
                            value = itemSearchQuery,
                            onValueChange = {
                                itemSearchQuery = it
                                expandedItemMenu = true
                            },
                            label = { Text("Cari barang restock...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { expandedItemMenu = !expandedItemMenu }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        DropdownMenu(
                            expanded = expandedItemMenu && matchingItems.isNotEmpty(),
                            onDismissRequest = { expandedItemMenu = false },
                            modifier = Modifier.fillMaxWidth(0.6f).heightIn(max = 240.dp)
                        ) {
                            matchingItems.forEach { item ->
                                DropdownMenuItem(
                                    text = {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("${item.name} (${item.stockQuantity} Pcs)")
                                            Text(formatRupiah(item.purchasePrice), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                        }
                                    },
                                    onClick = {
                                        viewModel.addToPurchasesCart(item)
                                        itemSearchQuery = ""
                                        expandedItemMenu = false
                                    }
                                )
                            }
                        }
                    }

                    if (cart.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Keranjang restock kosong", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Pilih barang yang ingin kulakan di atas.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(cart) { entry ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(entry.item.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Text(
                                                text = "${formatRupiah(entry.item.purchasePrice)} x ${entry.quantity}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            IconButton(
                                                onClick = { viewModel.updatePurchasesCartQuantity(entry.item.id, entry.quantity - 1) },
                                                modifier = Modifier.size(28.dp).background(MaterialTheme.colorScheme.surface, CircleShape)
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                                            }
                                            Text("${entry.quantity}", fontWeight = FontWeight.Bold)
                                            IconButton(
                                                onClick = { viewModel.updatePurchasesCartQuantity(entry.item.id, entry.quantity + 1) },
                                                modifier = Modifier.size(28.dp).background(MaterialTheme.colorScheme.surface, CircleShape)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Supplier & Checkout summary
                Card(
                    modifier = Modifier.weight(0.8f).fillMaxHeight(),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Ringkasan Kulakan", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                        // Supplier dropdown selector
                        Column {
                            Text("Pilih Supplier Mitra", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                                Surface(
                                    onClick = { expandedSupplierMenu = true },
                                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)).height(48.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val supName = selectedSupplier?.name ?: "Klik untuk Pilih Supplier"
                                        Text(supName, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                }
                                DropdownMenu(
                                    expanded = expandedSupplierMenu,
                                    onDismissRequest = { expandedSupplierMenu = false }
                                ) {
                                    allSuppliers.forEach { sup ->
                                        DropdownMenuItem(
                                            text = { Text(sup.name) },
                                            onClick = {
                                                selectedSupplier = sup
                                                expandedSupplierMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Payment Type (Tunai / Kredit)
                        Column {
                            Text("Metode Pembayaran", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { paymentType = "CASH" },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (paymentType == "CASH") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Tunai", fontSize = 12.sp, color = if (paymentType == "CASH") Color.White else MaterialTheme.colorScheme.onSurface)
                                }
                                Button(
                                    onClick = { paymentType = "CREDIT" },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (paymentType == "CREDIT") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Hutang", fontSize = 12.sp, color = if (paymentType == "CREDIT") Color.White else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }

                        Divider()

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Hutang/Beli:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(formatRupiah(total), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = {
                                if (selectedSupplier != null) {
                                    viewModel.checkoutPurchase(
                                        supplier = selectedSupplier!!,
                                        paymentType = paymentType,
                                        amountPaid = if (paymentType == "CASH") total else 0.0,
                                        onSuccess = {
                                            selectedSupplier = null
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = cart.isNotEmpty() && selectedSupplier != null
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Simpan Kulakan")
                        }
                    }
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

    Column(modifier = modifier.fillMaxSize().padding(16.dp).testTag("return_transaction_screen")) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Retur Transaksi", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Pengembalian barang dari pelanggan atau ke supplier", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = {
                    invoiceRef = ""
                    reasonInput = ""
                    returnedItemQty = ""
                    selectedItem = null
                    showFormDialog = true
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.SettingsBackupRestore, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Buat Retur")
            }
        }

        if (returnsHistory.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.SettingsBackupRestore,
                message = "Belum ada retur",
                hint = "Klik Buat Retur untuk mencatat retur barang baru."
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(returnsHistory) { ret ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(ret.returnNumber, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(
                                        text = if (ret.type == "SALE_RETURN") "Sumbangan kembali (Retur Jual)" else "Pengembalian suplai (Retur Beli)",
                                        color = if (ret.type == "SALE_RETURN") MaterialTheme.colorScheme.primary else Color(0xFFC62828),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = formatRupiah(ret.totalRefund),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Ref. Transaksi: ${ret.referenceInvoiceNumber}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Alasan: ${ret.reason}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
        }

        if (showFormDialog) {
            AlertDialog(
                onDismissRequest = { showFormDialog = false },
                title = { Text("Form Retur Barang") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Type Selection Row
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { returnType = "SALE_RETURN" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = if (returnType == "SALE_RETURN") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Text("Retur Jual", color = if (returnType == "SALE_RETURN") Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                            }
                            Button(
                                onClick = { returnType = "PURCHASE_RETURN" },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = if (returnType == "PURCHASE_RETURN") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Text("Retur Beli", color = if (returnType == "PURCHASE_RETURN") Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                            }
                        }

                        OutlinedTextField(
                            value = invoiceRef,
                            onValueChange = { invoiceRef = it },
                            label = { Text("Invoice / PO Rujukan") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Item Selection dropdown
                        Column {
                            Text("Pilih Produk", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Box {
                                Surface(
                                    onClick = { expandedItemDropdown = true },
                                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)).height(48.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val itmName = selectedItem?.name ?: "Klik untuk Pilih"
                                        Text(itmName, fontSize = 13.sp)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                }
                                DropdownMenu(
                                    expanded = expandedItemDropdown,
                                    onDismissRequest = { expandedItemDropdown = false },
                                    modifier = Modifier.fillMaxWidth(0.8f).heightIn(max = 200.dp)
                                ) {
                                    items.forEach { itm ->
                                        DropdownMenuItem(
                                            text = { Text(itm.name) },
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
                                label = { Text("Jumlah") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = reasonInput,
                                onValueChange = { reasonInput = it },
                                label = { Text("Alasan") },
                                modifier = Modifier.weight(2f)
                            )
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
                        }
                    ) {
                        Text("Kirim Retur")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFormDialog = false }) { Text("Batal") }
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
