package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category
import com.example.data.model.Customer
import com.example.data.model.Item
import com.example.data.model.Supplier
import com.example.ui.viewModel.AppScreen
import com.example.ui.viewModel.InventoryAccountingViewModel
import kotlin.random.Random

@Composable
fun MasterCategoryScreen(
    viewModel: InventoryAccountingViewModel,
    modifier: Modifier = Modifier
) {
    val categories by viewModel.categories.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var categoryNameInput by remember { mutableStateOf("") }

    Box(modifier = modifier.fillMaxSize().testTag("master_category_screen")) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Master Kategori", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Kelola kategori klasifikasi inventaris", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kategori")
                }
            }

            if (categories.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Category,
                    message = "Kategori kosong",
                    hint = "Klik tombol tambah untuk mendaftarkan kategori produk Anda."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(categories) { cat ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text(cat.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                IconButton(onClick = { viewModel.deleteCategory(cat) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Tambah Kategori") },
                text = {
                    OutlinedTextField(
                        value = categoryNameInput,
                        onValueChange = { categoryNameInput = it },
                        label = { Text("Nama Kategori") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (categoryNameInput.isNotBlank()) {
                                viewModel.addCategory(categoryNameInput.trim())
                                categoryNameInput = ""
                                showAddDialog = false
                            }
                        }
                    ) {
                        Text("Simpan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("Batal") }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterItemScreen(
    viewModel: InventoryAccountingViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.items.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var showFormDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<Item?>(null) }

    var nameInput by remember { mutableStateOf("") }
    var barcodeInput by remember { mutableStateOf("") }
    var purchasePriceInput by remember { mutableStateOf("") }
    var sellingPriceInput by remember { mutableStateOf("") }
    var stockInput by remember { mutableStateOf("") }
    var alertInput by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredItems = remember(items, searchQuery) {
        items.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.skuBarcode.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(modifier = modifier.fillMaxSize().testTag("master_item_screen")) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Katalog Barang", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Daftar stok produk & penetapan HPP", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = {
                        editingItem = null
                        nameInput = ""
                        barcodeInput = (8990000000000L + Random.nextLong(100000000L)).toString() // Seed dynamic barcode SKU
                        purchasePriceInput = ""
                        sellingPriceInput = ""
                        stockInput = ""
                        alertInput = "5"
                        selectedCategoryId = categories.firstOrNull()?.id
                        showFormDialog = true
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Barang")
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Telusuri barang / barcode SKU...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ),
                singleLine = true
            )

            if (filteredItems.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.Inventory2,
                    message = "Produk tidak ditemukan",
                    hint = "Masukkan kata pencarian lain atau tambahkan produk baru."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredItems) { item ->
                        val categoryName = categories.find { it.id == item.categoryId }?.name ?: "Umum"
                        val isLowStock = item.stockQuantity <= item.minStockAlert

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isLowStock) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, overflow = TextOverflow.Ellipsis, maxLines = 1)
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            SuggestionChip(
                                                onClick = {},
                                                label = { Text(categoryName) },
                                                colors = SuggestionChipDefaults.suggestionChipColors(
                                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            )
                                            Text(
                                                text = "SKU: ${item.skuBarcode}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row {
                                        IconButton(
                                            onClick = {
                                                editingItem = item
                                                nameInput = item.name
                                                barcodeInput = item.skuBarcode
                                                purchasePriceInput = item.purchasePrice.toInt().toString()
                                                sellingPriceInput = item.sellingPrice.toInt().toString()
                                                stockInput = item.stockQuantity.toString()
                                                alertInput = item.minStockAlert.toString()
                                                selectedCategoryId = item.categoryId
                                                showFormDialog = true
                                            }
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = { viewModel.deleteItem(item) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }

                                Divider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Harga Beli (HPP)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(formatRupiah(item.purchasePrice), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    }
                                    Column {
                                        Text("Harga Jual", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(formatRupiah(item.sellingPrice), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Sisa Stok", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "${item.stockQuantity} Pcs",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = if (isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isLowStock) {
                                                Icon(
                                                    imageVector = Icons.Default.Warning,
                                                    contentDescription = "Peringatan",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showFormDialog) {
            AlertDialog(
                onDismissRequest = { showFormDialog = false },
                title = { Text(if (editingItem == null) "Tambah Barang Baru" else "Ubah Barang") },
                text = {
                    Box(modifier = Modifier.heightIn(max = 420.dp)) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            item {
                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it },
                                    label = { Text("Nama Barang") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = barcodeInput,
                                        onValueChange = { barcodeInput = it },
                                        label = { Text("Barcode SKU") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    Button(
                                        onClick = { barcodeInput = (8990000000000L + Random.nextLong(100000000L)).toString() },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text("Acak SKU", fontSize = 11.sp)
                                    }
                                }
                            }
                            item {
                                // Category Selection Composable dropdown
                                Column {
                                    Text("Pilih Kategori", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Box {
                                        Surface(
                                            onClick = { categoryDropdownExpanded = true },
                                            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)).height(48.dp),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                val catName = categories.find { it.id == selectedCategoryId }?.name ?: "Klik untuk Pilih"
                                                Text(catName, fontSize = 14.sp)
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                            }
                                        }
                                        DropdownMenu(
                                            expanded = categoryDropdownExpanded,
                                            onDismissRequest = { categoryDropdownExpanded = false }
                                        ) {
                                            categories.forEach { cat ->
                                                DropdownMenuItem(
                                                    text = { Text(cat.name) },
                                                    onClick = {
                                                        selectedCategoryId = cat.id
                                                        categoryDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = purchasePriceInput,
                                        onValueChange = { purchasePriceInput = it },
                                        label = { Text("Harga Beli (Rp)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = sellingPriceInput,
                                        onValueChange = { sellingPriceInput = it },
                                        label = { Text("Harga Jual (Rp)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            item {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = stockInput,
                                        onValueChange = { stockInput = it },
                                        label = { Text("Stok Awal") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = alertInput,
                                        onValueChange = { alertInput = it },
                                        label = { Text("Min Peringatan") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (nameInput.isNotBlank() && selectedCategoryId != null) {
                                val buy = purchasePriceInput.toDoubleOrNull() ?: 0.0
                                val sell = sellingPriceInput.toDoubleOrNull() ?: 0.0
                                val qty = stockInput.toIntOrNull() ?: 0
                                val alert = alertInput.toIntOrNull() ?: 5

                                val curr = editingItem
                                if (curr == null) {
                                    viewModel.addItem(nameInput, selectedCategoryId!!, barcodeInput, buy, sell, qty, alert)
                                } else {
                                    viewModel.updateItem(
                                        curr.copy(
                                            name = nameInput,
                                            skuBarcode = barcodeInput,
                                            categoryId = selectedCategoryId!!,
                                            purchasePrice = buy,
                                            sellingPrice = sell,
                                            stockQuantity = qty,
                                            minStockAlert = alert
                                        )
                                    )
                                }
                                showFormDialog = false
                            }
                        }
                    ) {
                        Text("Simpan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showFormDialog = false }) { Text("Batal") }
                }
            )
        }
    }
}

@Composable
fun MasterSupplierScreen(
    viewModel: InventoryAccountingViewModel,
    modifier: Modifier = Modifier
) {
    val suppliers by viewModel.suppliers.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    var nameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var addressInput by remember { mutableStateOf("") }

    Box(modifier = modifier.fillMaxSize().testTag("master_supplier_screen")) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Pemasok Utama", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Daftar mitra dagang suplai barang", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Supplier")
                }
            }

            if (suppliers.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.LocalShipping,
                    message = "Pemasok kosong",
                    hint = "Klik tambah supplier untuk mendaftarkan mitra suplai."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(suppliers) { sup ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Box(
                                            modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                                        }
                                        Text(sup.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                    IconButton(onClick = { viewModel.deleteSupplier(sup) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                                    Text(sup.phone, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                                    Text(sup.address, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Tambah Supplier Baru") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("Nama Perusahaan / Supplier") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = phoneInput, onValueChange = { phoneInput = it }, label = { Text("No Telepon") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = addressInput, onValueChange = { addressInput = it }, label = { Text("Alamat Kantor") }, modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (nameInput.isNotBlank()) {
                                viewModel.addSupplier(nameInput.trim(), phoneInput.trim(), addressInput.trim())
                                nameInput = ""
                                phoneInput = ""
                                addressInput = ""
                                showAddDialog = false
                            }
                        }
                    ) {
                        Text("Simpan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("Batal") }
                }
            )
        }
    }
}

@Composable
fun MasterCustomerScreen(
    viewModel: InventoryAccountingViewModel,
    modifier: Modifier = Modifier
) {
    val customers by viewModel.customers.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    var nameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var addressInput by remember { mutableStateOf("") }

    Box(modifier = modifier.fillMaxSize().testTag("master_customer_screen")) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Daftar Pelanggan", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Kelola database kontak pelanggan", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pelanggan")
                }
            }

            if (customers.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.People,
                    message = "Pelanggan kosong",
                    hint = "Klik tambah pelanggan untuk menyimpan data kontak baru."
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(customers) { cus ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Box(
                                            modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                                        }
                                        Text(cus.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                    IconButton(onClick = { viewModel.deleteCustomer(cus) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                                    Text(cus.phone, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                                    Text(cus.address, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Tambah Kontak Pelanggan") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("Nama Lengkap") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = phoneInput, onValueChange = { phoneInput = it }, label = { Text("No Handphone") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = addressInput, onValueChange = { addressInput = it }, label = { Text("Alamat Tinggal") }, modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (nameInput.isNotBlank()) {
                                viewModel.addCustomer(nameInput.trim(), phoneInput.trim(), addressInput.trim())
                                nameInput = ""
                                phoneInput = ""
                                addressInput = ""
                                showAddDialog = false
                            }
                        }
                    ) {
                        Text("Simpan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("Batal") }
                }
            )
        }
    }
}

@Composable
fun EmptyStateView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String,
    hint: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = message,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = hint,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            lineHeight = 18.sp
        )
    }
}
