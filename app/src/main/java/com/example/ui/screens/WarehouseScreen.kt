package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.Category
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
    val allCategories by viewModel.categories.collectAsState()

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
                    categories = allCategories,
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
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                                                    RoundedCornerShape(6.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (item.imageUri != null) {
                                                AsyncImage(
                                                    model = item.imageUri,
                                                    contentDescription = null,
                                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Default.Image,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp),
                                                    tint = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }

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
                    modifier = Modifier.height(34.dp).testTag("add_warehouse_fab"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Warehouse", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gudang", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
    categories: List<Category>,
    transfers: List<StockTransfer>,
    onTransferExecute: (itemId: Int, fromWarehouseId: Int, toWarehouseId: Int, quantity: Int, notes: String) -> Unit
) {
    var selectedItem by remember { mutableStateOf<Item?>(null) }
    var fromWh by remember { mutableStateOf<Warehouse?>(null) }
    var toWh by remember { mutableStateOf<Warehouse?>(null) }
    var transferQtyInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }

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

    // Search and Category filters for product list
    var itemSearchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    val categoriesList = remember(categories) { listOf("All") + categories.map { it.name }.distinct() }

    val filteredItems = remember(items, itemSearchQuery, selectedCategory, categories) {
        items.filter { item ->
            val itemCategoryName = categories.find { it.id == item.categoryId }?.name ?: "All"
            (selectedCategory == "All" || itemCategoryName == selectedCategory) &&
            (item.name.contains(itemSearchQuery, ignoreCase = true) || item.skuBarcode.contains(itemSearchQuery))
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().testTag("stock_transfers_tab")) {
        val isCompact = maxWidth < 720.dp

        if (isCompact) {
            var activeMobileTab by remember { mutableStateOf(0) }

            Column(modifier = Modifier.fillMaxSize()) {
                TabRow(
                    selectedTabIndex = activeMobileTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = activeMobileTab == 0,
                        onClick = { activeMobileTab = 0 },
                        text = { Text("📦 Barang (${filteredItems.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = activeMobileTab == 1,
                        onClick = { activeMobileTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("⚙️ Atur Transfer", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                if (selectedItem != null) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                    )
                                }
                            }
                        }
                    )
                    Tab(
                        selected = activeMobileTab == 2,
                        onClick = { activeMobileTab = 2 },
                        text = { Text("📜 Riwayat (${transfers.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                }

                Box(modifier = Modifier.weight(1f).padding(12.dp)) {
                    when (activeMobileTab) {
                        0 -> {
                            TransferProductListArea(
                                searchQuery = itemSearchQuery,
                                onSearchChange = { itemSearchQuery = it },
                                categories = categoriesList,
                                selectedCategory = selectedCategory,
                                onCategorySelect = { selectedCategory = it },
                                filteredItems = filteredItems,
                                categoriesModelList = categories,
                                stocks = stocks,
                                fromWh = fromWh,
                                selectedItem = selectedItem,
                                onItemClick = {
                                    selectedItem = it
                                    activeMobileTab = 1 // Auto transition to set details! Beautiful!
                                }
                            )
                        }
                        1 -> {
                            TransferFormArea(
                                selectedItem = selectedItem,
                                warehouses = warehouses,
                                fromWh = fromWh,
                                onSelectFromWh = { fromWh = it },
                                fromMenuExpanded = fromMenuExpanded,
                                onFromMenuExpandedChange = { fromMenuExpanded = it },
                                toWh = toWh,
                                onSelectToWh = { toWh = it },
                                toMenuExpanded = toMenuExpanded,
                                onToMenuExpandedChange = { toMenuExpanded = it },
                                transferQtyInput = transferQtyInput,
                                onQtyValueChange = { transferQtyInput = it },
                                notesInput = notesInput,
                                onNotesValueChange = { notesInput = it },
                                availableFromStock = availableFromStock,
                                onExecuteTransferClick = {
                                    val qty = transferQtyInput.toIntOrNull() ?: 0
                                    onTransferExecute(selectedItem!!.id, fromWh!!.id, toWh!!.id, qty, notesInput)
                                    focusManager.clearFocus()
                                    transferQtyInput = ""
                                    notesInput = ""
                                    activeMobileTab = 2 // Go to history
                                },
                                onClearSelectedItem = { selectedItem = null }
                            )
                        }
                        2 -> {
                            TransferHistoryArea(
                                transfers = transfers,
                                items = items,
                                warehouses = warehouses
                            )
                        }
                    }
                }
            }
        } else {
            // Wide split screen layout
            Row(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                // Left Area: Toggle between Product list and History
                var leftPanelTab by remember { mutableStateOf(0) }

                Column(modifier = Modifier.weight(1.1f).fillMaxHeight()) {
                    TabRow(
                        selectedTabIndex = leftPanelTab,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Tab(
                            selected = leftPanelTab == 0,
                            onClick = { leftPanelTab = 0 },
                            text = { Text("📦 Pilih Barang (${filteredItems.size})", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = leftPanelTab == 1,
                            onClick = { leftPanelTab = 1 },
                            text = { Text("📜 Riwayat Transfer (${transfers.size})", fontWeight = FontWeight.Bold) }
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        if (leftPanelTab == 0) {
                            TransferProductListArea(
                                searchQuery = itemSearchQuery,
                                onSearchChange = { itemSearchQuery = it },
                                categories = categoriesList,
                                selectedCategory = selectedCategory,
                                onCategorySelect = { selectedCategory = it },
                                filteredItems = filteredItems,
                                categoriesModelList = categories,
                                stocks = stocks,
                                fromWh = fromWh,
                                selectedItem = selectedItem,
                                onItemClick = { selectedItem = it }
                            )
                        } else {
                            TransferHistoryArea(
                                transfers = transfers,
                                items = items,
                                warehouses = warehouses
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 12.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                // Right Area: Transfer form controls
                Column(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight()
                ) {
                    Text(
                        text = "Pengaturan Pengiriman (Mutasi)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    TransferFormArea(
                        selectedItem = selectedItem,
                        warehouses = warehouses,
                        fromWh = fromWh,
                        onSelectFromWh = { fromWh = it },
                        fromMenuExpanded = fromMenuExpanded,
                        onFromMenuExpandedChange = { fromMenuExpanded = it },
                        toWh = toWh,
                        onSelectToWh = { toWh = it },
                        toMenuExpanded = toMenuExpanded,
                        onToMenuExpandedChange = { toMenuExpanded = it },
                        transferQtyInput = transferQtyInput,
                        onQtyValueChange = { transferQtyInput = it },
                        notesInput = notesInput,
                        onNotesValueChange = { notesInput = it },
                        availableFromStock = availableFromStock,
                        onExecuteTransferClick = {
                            val qty = transferQtyInput.toIntOrNull() ?: 0
                            onTransferExecute(selectedItem!!.id, fromWh!!.id, toWh!!.id, qty, notesInput)
                            focusManager.clearFocus()
                            transferQtyInput = ""
                            notesInput = ""
                        },
                        onClearSelectedItem = { selectedItem = null }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferProductListArea(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit,
    filteredItems: List<Item>,
    categoriesModelList: List<Category>,
    stocks: List<WarehouseStock>,
    fromWh: Warehouse?,
    selectedItem: Item?,
    onItemClick: (Item) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Cari barang untuk transfer...") },
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
                    Text("Barang tidak ditemukan", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(130.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredItems) { item ->
                    val isSelected = selectedItem?.id == item.id
                    val stockInWh = if (fromWh != null) {
                        stocks.find { it.warehouseId == fromWh.id && it.itemId == item.id }?.stockQuantity ?: 0
                    } else {
                        item.stockQuantity
                    }

                    Card(
                        onClick = { onItemClick(item) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.88f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                        )
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

                                Surface(
                                    color = if (fromWh != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                                ) {
                                    Text(
                                        text = if (fromWh != null) "${fromWh.name.take(6)}: $stockInWh" else "Stok: $stockInWh",
                                        color = if (fromWh != null) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
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

                            val categoryName = categoriesModelList.find { it.id == item.categoryId }?.name ?: "Umum"
                            Text(
                                categoryName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
fun TransferFormArea(
    selectedItem: Item?,
    warehouses: List<Warehouse>,
    fromWh: Warehouse?,
    onSelectFromWh: (Warehouse) -> Unit,
    fromMenuExpanded: Boolean,
    onFromMenuExpandedChange: (Boolean) -> Unit,
    toWh: Warehouse?,
    onSelectToWh: (Warehouse) -> Unit,
    toMenuExpanded: Boolean,
    onToMenuExpandedChange: (Boolean) -> Unit,
    transferQtyInput: String,
    onQtyValueChange: (String) -> Unit,
    notesInput: String,
    onNotesValueChange: (String) -> Unit,
    availableFromStock: Int,
    onExecuteTransferClick: () -> Unit,
    onClearSelectedItem: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Selected Product Visual Preview Card
            if (selectedItem == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(115.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Ketuk Barang Terlebih Dahulu",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            if (!selectedItem.imageUri.isNullOrEmpty()) {
                                AsyncImage(
                                    model = selectedItem.imageUri,
                                    contentDescription = selectedItem.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = null,
                                    modifier = Modifier.align(Alignment.Center).size(24.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                selectedItem.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "Barcode: ${selectedItem.skuBarcode}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Total Stok: ${selectedItem.stockQuantity} Unit",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(onClick = onClearSelectedItem) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Ganti produk",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Dropdown 1: Dari Gudang Asal
            Column {
                Text(
                    text = "Gudang Asal Pengiriman",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        onClick = { onFromMenuExpandedChange(true) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = fromWh?.name ?: "Pilih Gudang Asal...",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (fromWh == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    DropdownMenu(
                        expanded = fromMenuExpanded,
                        onDismissRequest = { onFromMenuExpandedChange(false) },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        warehouses.forEach { wh ->
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Warehouse, contentDescription = null, tint = MaterialTheme.colorScheme.outline) },
                                text = { Text(wh.name, fontSize = 13.sp) },
                                onClick = {
                                    onSelectFromWh(wh)
                                    onFromMenuExpandedChange(false)
                                }
                            )
                        }
                    }
                }
            }

            // Live Warehouse stock badge
            if (selectedItem != null && fromWh != null) {
                Surface(
                    color = if (availableFromStock > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = if (availableFromStock > 0) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (availableFromStock > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Stok di ${fromWh.name}:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            "$availableFromStock Unit",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (availableFromStock > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Direction arrow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.SwapVert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Dropdown 2: Ke Gudang Tujuan
            Column {
                Text(
                    text = "Gudang Tujuan Penerima",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        onClick = { onToMenuExpandedChange(true) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.MeetingRoom, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Text(
                                    text = toWh?.name ?: "Pilih Gudang Tujuan...",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (toWh == null) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    DropdownMenu(
                        expanded = toMenuExpanded,
                        onDismissRequest = { onToMenuExpandedChange(false) },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        warehouses.forEach { wh ->
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Warehouse, contentDescription = null, tint = MaterialTheme.colorScheme.outline) },
                                text = { Text(wh.name, fontSize = 13.sp) },
                                onClick = {
                                    onSelectToWh(wh)
                                    onToMenuExpandedChange(false)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Inputs Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = transferQtyInput,
                    onValueChange = onQtyValueChange,
                    label = { Text("Jumlah Mutasi") },
                    placeholder = { Text("0") },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.weight(1f).testTag("transfer_quantity_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = onNotesValueChange,
                    label = { Text("Keterangan") },
                    placeholder = { Text("Mutasi stok") },
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.weight(2f).testTag("transfer_notes_input"),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Error warnings
            val transferQty = transferQtyInput.toIntOrNull() ?: 0
            val isSameWh = fromWh != null && toWh != null && fromWh.id == toWh.id
            val isOverStock = transferQty > availableFromStock && fromWh != null && selectedItem != null

            if (isSameWh) {
                Text(
                    text = "Gudang asal & tujuan tidak boleh sama!",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else if (isOverStock) {
                Text(
                    text = "Stok gudang asal tidak mencukupi!",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            // Transfer execute button
            val canTransfer = selectedItem != null &&
                    fromWh != null &&
                    toWh != null &&
                    !isSameWh &&
                    transferQty > 0 &&
                    !isOverStock

            Button(
                onClick = onExecuteTransferClick,
                enabled = canTransfer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("execute_transfer_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Kirim / Mutasi Stok", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TransferHistoryArea(
    transfers: List<StockTransfer>,
    items: List<Item>,
    warehouses: List<Warehouse>
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(
                "Riwayat Transfer (Mutasi) Stok",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (transfers.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Belum ada riwayat mutasi stok.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                val sdf = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    items(transfers.reversed()) { tf -> // reverse to show latest transfers first
                        val item = items.find { it.id == tf.itemId }
                        val srcWhName = warehouses.find { it.id == tf.fromWarehouseId }?.name ?: "Unknown"
                        val destWhName = warehouses.find { it.id == tf.toWarehouseId }?.name ?: "Unknown"

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Product Image representation
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (item?.imageUri != null) {
                                        AsyncImage(
                                            model = item.imageUri,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Image,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
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
                                            color = Color.Gray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
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
                                                color = MaterialTheme.colorScheme.secondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
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
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
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
