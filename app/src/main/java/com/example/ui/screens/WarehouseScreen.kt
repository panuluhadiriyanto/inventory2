package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Item
import com.example.data.model.Warehouse
import com.example.data.model.WarehouseStock
import com.example.data.model.StockTransfer
import com.example.ui.viewModel.InventoryAccountingViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarehouseScreen(
    viewModel: InventoryAccountingViewModel,
    modifier: Modifier = Modifier
) {
    val warehouses by viewModel.warehouses.collectAsState()
    val warehouseStocks by viewModel.warehouseStocks.collectAsState()
    val stockTransfers by viewModel.stockTransfers.collectAsState()
    val allItems by viewModel.items.collectAsState()

    var activeTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Stok & Lokasi", "Transfer Stok")

    var showAddWarehouseDialog by remember { mutableStateOf(false) }
    var selectedWarehouseDetails by remember { mutableStateOf<Warehouse?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("warehouse_screen")
    ) {
        // Tab Header
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = activeTab == index,
                    onClick = { activeTab = index },
                    modifier = Modifier.height(48.dp),
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (activeTab == index) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (activeTab) {
                0 -> WarehouseLocationsTab(
                    warehouses = warehouses,
                    stocks = warehouseStocks,
                    items = allItems,
                    onAddClick = { showAddWarehouseDialog = true },
                    onWarehouseSelect = { selectedWarehouseDetails = it },
                    onDeleteWarehouse = { viewModel.deleteWarehouse(it) }
                )
                1 -> StockTransfersTab(
                    warehouses = warehouses,
                    stocks = warehouseStocks,
                    items = allItems,
                    transfers = stockTransfers,
                    onTransferExecute = { itemId, fromId, toId, qty, notes ->
                        viewModel.transferStock(itemId, fromId, toId, qty, notes)
                    }
                )
            }
        }
    }

    // Dialog Tambah Gudang Baru
    if (showAddWarehouseDialog) {
        var nameInput by remember { mutableStateOf("") }
        var addressInput by remember { mutableStateOf("") }
        var nameError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddWarehouseDialog = false },
            title = { Text("Tambah Gudang Baru", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = {
                            nameInput = it
                            nameError = it.trim().isEmpty()
                        },
                        label = { Text("Nama Gudang") },
                        isError = nameError,
                        modifier = Modifier.fillMaxWidth().testTag("add_warehouse_name"),
                        singleLine = true
                    )
                    if (nameError) {
                        Text("Nama gudang tidak boleh kosong!", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        label = { Text("Alamat Gudang") },
                        modifier = Modifier.fillMaxWidth().testTag("add_warehouse_address"),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameInput.trim().isEmpty()) {
                            nameError = true
                        } else {
                            viewModel.addWarehouse(nameInput.trim(), addressInput.trim())
                            showAddWarehouseDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_warehouse")
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddWarehouseDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog Detail Stok Gudang
    if (selectedWarehouseDetails != null) {
        val selectedWh = selectedWarehouseDetails!!
        val whStocks = warehouseStocks.filter { it.warehouseId == selectedWh.id }
        var searchQuery by remember { mutableStateOf("") }

        val filteredStocks = whStocks.mapNotNull { ws ->
            val originItem = allItems.find { it.id == ws.itemId }
            if (originItem != null && (searchQuery.isEmpty() || originItem.name.contains(searchQuery, ignoreCase = true) || originItem.skuBarcode.contains(searchQuery))) {
                Pair(originItem, ws.stockQuantity)
            } else {
                null
            }
        }

        AlertDialog(
            onDismissRequest = { selectedWarehouseDetails = null },
            title = {
                Column {
                    Text(selectedWh.name, fontWeight = FontWeight.Bold)
                    Text(
                        selectedWh.address.ifEmpty { "Tanpa alamat" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Column(modifier = Modifier.heightIn(max = 450.dp).fillMaxWidth()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Cari Produk di Gudang") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Item") },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        singleLine = true
                    )

                    if (filteredStocks.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Tidak ada barang di gudang ini.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        ) {
                            items(filteredStocks) { (item, qty) ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.name, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                            Text("SKU: ${item.skuBarcode}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        }
                                        Surface(
                                            color = if (qty <= item.minStockAlert) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = if (qty <= item.minStockAlert) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "$qty unit",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedWarehouseDetails = null }) {
                    Text("Tutup")
                }
            }
        )
    }
}

@Composable
fun WarehouseLocationsTab(
    warehouses: List<Warehouse>,
    stocks: List<WarehouseStock>,
    items: List<Item>,
    onAddClick: () -> Unit,
    onWarehouseSelect: (Warehouse) -> Unit,
    onDeleteWarehouse: (Warehouse) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Lokasi Gudang", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Mendefinisikan lokasi penyimpanan barang usaha Anda", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = onAddClick,
                    modifier = Modifier.testTag("add_warehouse_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Warehouse")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gudang")
                }
            }

            if (warehouses.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada gudang terdefinisi.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    items(warehouses) { wh ->
                        val whStocks = stocks.filter { it.warehouseId == wh.id }
                        val uniqueProdCount = whStocks.count { it.stockQuantity > 0 }
                        val totalWhStock = whStocks.sumOf { it.stockQuantity }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onWarehouseSelect(wh) },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(wh.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        wh.address.ifEmpty { "Tidak ada alamat lengkap" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Column {
                                            Text("Item Unik", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                            Text("$uniqueProdCount Jenis", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        }
                                        Column {
                                            Text("Total Stok", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                            Text("$totalWhStock Unit", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }

                                // Delete Button: Jangan ijinkan hapus Gudang Utama (id = 1) agar system inventory tetap valid
                                if (wh.id != 1) {
                                    IconButton(
                                        onClick = { onDeleteWarehouse(wh) },
                                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus Gudang")
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

@Composable
fun StockTransfersTab(
    warehouses: List<Warehouse>,
    stocks: List<WarehouseStock>,
    items: List<Item>,
    transfers: List<StockTransfer>,
    onTransferExecute: (itemId: Int, fromWarehouseId: Int, toWarehouseId: Int, quantity: Int, notes: String) -> Unit
) {
    var selectedItem by remember { mutableStateOf<Item?>(null) }
    var fromWh by remember { mutableStateOf<Warehouse?>(null) }
    var toWh by remember { mutableStateOf<Warehouse?>(null) }
    var transferQtyInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }

    var itemMenuExpanded by remember { mutableStateOf(false) }
    var fromMenuExpanded by remember { mutableStateOf(false) }
    var toMenuExpanded by remember { mutableStateOf(false) }

    // Hitung stok yang tersedia di `fromWh` untuk `selectedItem`
    val availableFromStock = remember(stocks, fromWh, selectedItem) {
        if (fromWh != null && selectedItem != null) {
            stocks.find { it.warehouseId == fromWh!!.id && it.itemId == selectedItem!!.id }?.stockQuantity ?: 0
        } else {
            0
        }
    }

    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Form Column
            Card(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Form Transfer Stok", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    // 1. Pilih Item
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedItem?.name ?: "Pilih Barang...",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Barang / Produk") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { itemMenuExpanded = true },
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = if (selectedItem == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.outline,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { itemMenuExpanded = true })

                        DropdownMenu(
                            expanded = itemMenuExpanded,
                            onDismissRequest = { itemMenuExpanded = false }
                        ) {
                            items.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text("${item.name} (${item.skuBarcode})") },
                                    onClick = {
                                        selectedItem = item
                                        itemMenuExpanded = false
                                        // Reset ketersediaan jika gudang tidak menyediakan
                                    }
                                )
                            }
                        }
                    }

                    // 2. Dari Gudang (Source)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = fromWh?.name ?: "Pilih Gudang Asal...",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Dari Gudang") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { fromMenuExpanded = true },
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = if (fromWh == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.outline,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { fromMenuExpanded = true })

                        DropdownMenu(
                            expanded = fromMenuExpanded,
                            onDismissRequest = { fromMenuExpanded = false }
                        ) {
                            warehouses.forEach { wh ->
                                DropdownMenuItem(
                                    text = { Text(wh.name) },
                                    onClick = {
                                        fromWh = wh
                                        fromMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Info Stok Tersedia
                    if (selectedItem != null && fromWh != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Stok Tersedia di ${fromWh!!.name}:", style = MaterialTheme.typography.bodySmall)
                                Text("$availableFromStock Unit", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    // Direction Arrow
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.SwapVert,
                            contentDescription = "Transfer",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // 3. Ke Gudang (Destination)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = toWh?.name ?: "Pilih Gudang Tujuan...",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Ke Gudang Tujuan") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { toMenuExpanded = true },
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = if (toWh == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.outline,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Box(modifier = Modifier.matchParentSize().clickable { toMenuExpanded = true })

                        DropdownMenu(
                            expanded = toMenuExpanded,
                            onDismissRequest = { toMenuExpanded = false }
                        ) {
                            warehouses.forEach { wh ->
                                DropdownMenuItem(
                                    text = { Text(wh.name) },
                                    onClick = {
                                        toWh = wh
                                        toMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 4. Jumlah Transfer & Notes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = transferQtyInput,
                            onValueChange = { transferQtyInput = it.filter { c -> c.isDigit() } },
                            label = { Text("Jumlah") },
                            modifier = Modifier.weight(1f).testTag("transfer_quantity_input"),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = notesInput,
                            onValueChange = { notesInput = it },
                            label = { Text("Keterangan") },
                            modifier = Modifier.weight(2f).testTag("transfer_notes_input"),
                            singleLine = true
                        )
                    }

                    // Transfer Button & Validation
                    val transferQty = transferQtyInput.toIntOrNull() ?: 0
                    val canTransfer = selectedItem != null &&
                            fromWh != null &&
                            toWh != null &&
                            fromWh!!.id != toWh!!.id &&
                            transferQty > 0 &&
                            transferQty <= availableFromStock

                    Button(
                        onClick = {
                            if (canTransfer) {
                                onTransferExecute(selectedItem!!.id, fromWh!!.id, toWh!!.id, transferQty, notesInput)
                                focusManager.clearFocus()
                                // Reset inputs
                                transferQtyInput = ""
                                notesInput = ""
                            }
                        },
                        enabled = canTransfer,
                        modifier = Modifier.fillMaxWidth().testTag("execute_transfer_button")
                    ) {
                        Icon(Icons.Default.Transform, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kirim / Transfer Stok")
                    }

                    // Pesan Error Validasi
                    if (fromWh != null && toWh != null && fromWh!!.id == toWh!!.id) {
                        Text("Gudang asal dan tujuan tidak boleh sama!", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    } else if (transferQty > availableFromStock && fromWh != null && selectedItem != null) {
                        Text("Stok tidak mencukupi untuk transfer!", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // History Column
            Card(
                modifier = Modifier
                    .weight(1.8f)
                    .fillMaxHeight(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize()
                ) {
                    Text("Riwayat Transfer Stok", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (transfers.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Belum ada riwayat transfer barang.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        val sdf = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f).fillMaxWidth()
                        ) {
                            items(transfers) { tf ->
                                val item = items.find { it.id == tf.itemId }
                                val srcWhName = warehouses.find { it.id == tf.fromWarehouseId }?.name ?: "Unknown"
                                val destWhName = warehouses.find { it.id == tf.toWarehouseId }?.name ?: "Unknown"

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = item?.name ?: "Produk Terhapus",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = tf.notes.ifEmpty { "Transfer Gudang" },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = srcWhName,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                                Icon(
                                                    Icons.AutoMirrored.Filled.ArrowForward,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp).padding(horizontal = 2.dp),
                                                    tint = Color.Gray
                                                )
                                                Text(
                                                    text = destWhName,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "${tf.quantity} Unit",
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = sdf.format(Date(tf.date)),
                                            fontSize = 11.sp,
                                            color = Color.Gray
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
