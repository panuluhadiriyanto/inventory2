package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Debt
import com.example.data.model.Receivable
import com.example.ui.viewModel.AccountingReportState
import com.example.ui.viewModel.InventoryAccountingViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DebtScreen(
    viewModel: InventoryAccountingViewModel,
    modifier: Modifier = Modifier
) {
    val debts by viewModel.debts.collectAsState()
    var repayTarget by remember { mutableStateOf<Debt?>(null) }
    var amountInput by remember { mutableStateOf("") }

    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Column(modifier = modifier.fillMaxSize().padding(16.dp).testTag("debt_screen")) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text("Pengelolaan Utang Dagang (Payables)", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Pantau jatuh tempo & pelunasan utang ke supplier", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (debts.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.MoneyOff,
                message = "Tidak ada utang dagang",
                hint = "Utang dagang akan otomatis tercatat ketika Anda melakukan transaksi pembelian / kulakan dengan metode Kredit."
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(debts) { debt ->
                    val isLunas = debt.status == "LUNAS"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isLunas) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.08f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(debt.supplierName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Ref PO: #${debt.purchaseId ?: "Manual"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(debt.status) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (isLunas) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                        labelColor = if (isLunas) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Jumlah Utang", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatRupiah(debt.amount), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                                Column {
                                    Text("Sisa Saldo", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatRupiah(debt.remainingAmount), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isLunas) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Jatuh Tempo", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(dateFormat.format(Date(debt.dueDate)), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }

                            if (!isLunas) {
                                Button(
                                    onClick = {
                                        repayTarget = debt
                                        amountInput = debt.remainingAmount.toInt().toString()
                                    },
                                    modifier = Modifier.padding(top = 12.dp).align(Alignment.End),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Bayar Utang")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (repayTarget != null) {
            AlertDialog(
                onDismissRequest = { repayTarget = null },
                title = { Text("Tulis Pelunasan Utang") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Membayar utang ke ${repayTarget!!.supplierName}", fontSize = 13.sp)
                        Text("Sisa tagihan: ${formatRupiah(repayTarget!!.remainingAmount)}", fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { amountInput = it },
                            label = { Text("Jumlah Uang Tunai Dibayar (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amt = amountInput.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                viewModel.repayDebt(repayTarget!!, amt)
                                repayTarget = null
                                amountInput = ""
                            }
                        }
                    ) {
                        Text("Konfirmasi Bayar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { repayTarget = null }) { Text("Batal") }
                }
            )
        }
    }
}

@Composable
fun ReceivableScreen(
    viewModel: InventoryAccountingViewModel,
    modifier: Modifier = Modifier
) {
    val receivables by viewModel.receivables.collectAsState()
    var collectTarget by remember { mutableStateOf<Receivable?>(null) }
    var amountInput by remember { mutableStateOf("") }

    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Column(modifier = modifier.fillMaxSize().padding(16.dp).testTag("receivable_screen")) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text("Pengelolaan Piutang Usaha (Receivables)", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Pantau jatuh tempo & penagihan dari pelanggan umum/agen", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (receivables.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.AttachMoney,
                message = "Tidak ada piutang usaha",
                hint = "Piutang usaha otomatis tercatat saat transaksi penjualan dicicil atau di-checkout dengan metode Kredit/Piutang."
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(receivables) { receivable ->
                    val isLunas = receivable.status == "LUNAS"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isLunas) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            else Color(0xFFF1F8E9)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(receivable.customerName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Ref Invoice: ${receivable.saleId ?: "Manual"}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(receivable.status) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = if (isLunas) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                                        labelColor = if (isLunas) Color(0xFF2E7D32) else Color(0xFFE65100)
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Jumlah Piutang", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatRupiah(receivable.amount), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                                Column {
                                    Text("Sisa Saldo", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatRupiah(receivable.remainingAmount), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isLunas) MaterialTheme.colorScheme.onSurface else Color(0xFFE65100))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Jatuh Tempo", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(dateFormat.format(Date(receivable.dueDate)), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }

                            if (!isLunas) {
                                Button(
                                    onClick = {
                                        collectTarget = receivable
                                        amountInput = receivable.remainingAmount.toInt().toString()
                                    },
                                    modifier = Modifier.padding(top = 12.dp).align(Alignment.End),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.CallReceived, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Terima Tagihan")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (collectTarget != null) {
            AlertDialog(
                onDismissRequest = { collectTarget = null },
                title = { Text("Terima Cicilan Piutang") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Terima pembayaran tagihan dari ${collectTarget!!.customerName}", fontSize = 13.sp)
                        Text("Sisa piutang: ${formatRupiah(collectTarget!!.remainingAmount)}", fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { amountInput = it },
                            label = { Text("Jumlah Uang Diterima (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amt = amountInput.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                viewModel.collectReceivable(collectTarget!!, amt)
                                collectTarget = null
                                amountInput = ""
                            }
                        }
                    ) {
                        Text("Konfirmasi Terima")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { collectTarget = null }) { Text("Batal") }
                }
            )
        }
    }
}

@Composable
fun AccountingReportsScreen(
    viewModel: InventoryAccountingViewModel,
    modifier: Modifier = Modifier
) {
    val reportState by viewModel.accountingReport.collectAsState()
    val journalHistory by viewModel.journalEntries.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Laba Rugi, 1: Neraca, 2: Kas & Jurnal (for narrow screens)
    var showExpenseDialog by remember { mutableStateOf(false) }

    var expenseDescInput by remember { mutableStateOf("") }
    var expenseAmountInput by remember { mutableStateOf("") }

    val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())

    BoxWithConstraints(modifier = modifier.fillMaxSize().padding(16.dp).testTag("accounting_reports_screen")) {
        val isWide = maxWidth >= 820.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Laporan Keuangan", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Pencatatan akuntansi & neraca seimbang real-time", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = { showExpenseDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.height(34.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.AddBusiness,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bayar Beban", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Simple KPI summaries ribbon at the top
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Kas Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Kas & Bank 💳", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Text(formatRupiah(reportState.kasBalance), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                // Laba Bersih Card
                val isProfit = reportState.labaBersih >= 0
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isProfit) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    ),
                    border = BorderStroke(1.dp, if (isProfit) Color(0xFFC8E6C9) else Color(0xFFFFCDD2))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(if (isProfit) "Laba Bersih 📈" else "Rugi Bersih 📉", style = MaterialTheme.typography.bodySmall, color = if (isProfit) Color(0xFF2E7D32) else Color(0xFFC62828))
                        Text(formatRupiah(reportState.labaBersih), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (isProfit) Color(0xFF2E7D32) else Color(0xFFC62828))
                    }
                }

                // Total Aset (Aktiva)
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Total Aset 🏛️", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Text(formatRupiah(reportState.totalAssets), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            if (isWide) {
                // RESPONSIVE SPLIT-PANE LAYOUT
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left Column (Report Selector + Selected Report Statement) - weight 1.1
                    Column(
                        modifier = Modifier.weight(1.1f).fillMaxHeight()
                    ) {
                        // Narrower Tab Selector inside tablet view
                        TabRow(
                            selectedTabIndex = if (activeTab > 1) 0 else activeTab,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) {
                                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Laba Rugi", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                                Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Scale, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Neraca Keuangan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            if (activeTab == 0) {
                                IncomeStatementWidget(reportState, isWide = true)
                            } else {
                                BalanceSheetWidget(reportState, isWide = true)
                            }
                        }
                    }

                    // Right Column (Double-Entry Ledger Jurnal history, always visible) - weight 0.9
                    Card(
                        modifier = Modifier.weight(0.9f).fillMaxHeight(),
                        elevation = CardDefaults.cardElevation(2.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp).fillMaxSize()) {
                            Text(
                                "Jurnal Ledger (Double-Entry Log)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            JournalLedgerWidget(journalHistory, dateFormat)
                        }
                    }
                }
            } else {
                // STANDARD NARROW MOBILE TABS
                TabRow(
                    selectedTabIndex = activeTab,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text("Laba Rugi", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Scale, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text("Neraca", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Tab(selected = activeTab == 2, onClick = { activeTab = 2 }) {
                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text("Jurnal Ledger", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (activeTab) {
                        0 -> IncomeStatementWidget(reportState, isWide = false)
                        1 -> BalanceSheetWidget(reportState, isWide = false)
                        2 -> JournalLedgerWidget(journalHistory, dateFormat)
                    }
                }
            }
        }

        if (showExpenseDialog) {
            AlertDialog(
                onDismissRequest = { showExpenseDialog = false },
                title = { Text("Posting Beban Biaya Baru", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Mencatat beban operasional langsung memotong akumulasi modal & kas rill usaha.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        
                        // Expense Preset Chips (Sederhana & Responsive)
                        Text("Pilih Pintasan Kategori:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        val extraPresets = listOf(
                            Pair("📶 Wifi", "Biaya Internet & Wifi Bulanan"),
                            Pair("💡 Listrik", "Pembayaran Token Listrik Toko"),
                            Pair("🏢 Sewa", "Angsuran Sewa Tempat Usaha"),
                            Pair("👥 Gaji", "Penyaluran Gaji Karyawan Toko"),
                            Pair("📦 Kirim", "Biaya Kurir & Ekspedisi Toko")
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(extraPresets) { preset ->
                                val (label, desc) = preset
                                SuggestionChip(
                                    onClick = { expenseDescInput = desc },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = expenseDescInput,
                            onValueChange = { expenseDescInput = it },
                            label = { Text("Deskripsi Pengeluaran") },
                            placeholder = { Text("Contoh: Pembelian Plastik Packing") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = expenseAmountInput,
                            onValueChange = { expenseAmountInput = it },
                            label = { Text("Jumlah Biaya (Rp)") },
                            placeholder = { Text("0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amt = expenseAmountInput.toDoubleOrNull() ?: 0.0
                            if (expenseDescInput.isNotBlank() && amt > 0) {
                                viewModel.recordExpense(expenseDescInput.trim(), amt)
                                showExpenseDialog = false
                                expenseDescInput = ""
                                expenseAmountInput = ""
                            }
                        },
                        modifier = Modifier.height(34.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(8.dp),
                        enabled = expenseDescInput.isNotBlank() && expenseAmountInput.isNotBlank()
                    ) {
                        Text("Posting Jurnal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showExpenseDialog = false },
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Batal", fontSize = 11.sp)
                    }
                }
            )
        }
    }
}

@Composable
fun IncomeStatementWidget(state: AccountingReportState, isWide: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxSize(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Laporan Laba Rugi", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Periode penjualan dan penyesuaian biaya berjalan", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Divider(modifier = Modifier.padding(vertical = 4.dp))
            }

            item {
                ReportValueLine("Pendapatan Kotor Penjualan", state.revenueTotal, isHeader = true)
            }

            item {
                ReportValueLine("Harga Pokok Penjualan (HPP)", state.hppTotal, isSubtracted = true)
            }

            item {
                ReportValueLine("Total Laba Kotor (Gross Profit)", state.labaKotor, isHeader = true, highlightColor = MaterialTheme.colorScheme.primary)
                Divider(modifier = Modifier.padding(vertical = 4.dp))
            }

            item {
                ReportValueLine("Tambahan Beban Operasional", state.bebanTotal, isSubtracted = true)
                Divider(modifier = Modifier.padding(vertical = 4.dp))
            }

            // Visual Segmented Margin Bar (Sederhana & Informatif!)
            item {
                if (state.revenueTotal > 0) {
                    val hppPct = (state.hppTotal / state.revenueTotal).toFloat().coerceIn(0f, 1f)
                    val bebanPct = (state.bebanTotal / state.revenueTotal).toFloat().coerceIn(0f, 1f)
                    val profitPct = (state.labaBersih.coerceAtLeast(0.0) / state.revenueTotal).toFloat().coerceIn(0f, 1f)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            "Alokasi Pendapatan Penjualan", 
                            style = MaterialTheme.typography.bodySmall, 
                            fontWeight = FontWeight.Bold, 
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            if (profitPct > 0) {
                                Box(modifier = Modifier.fillMaxHeight().weight(profitPct.coerceAtLeast(0.01f)).background(Color(0xFF2E7D32)))
                            }
                            if (hppPct > 0) {
                                Box(modifier = Modifier.fillMaxHeight().weight(hppPct.coerceAtLeast(0.01f)).background(Color(0xFFE57373)))
                            }
                            if (bebanPct > 0) {
                                Box(modifier = Modifier.fillMaxHeight().weight(bebanPct.coerceAtLeast(0.01f)).background(Color(0xFFFFB74D)))
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(6.dp).background(Color(0xFF2E7D32), CircleShape))
                                Text("Laba Bersih (${(profitPct * 100).toInt()}%)", fontSize = 9.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(6.dp).background(Color(0xFFE57373), CircleShape))
                                Text("HPP (${(hppPct * 100).toInt()}%)", fontSize = 9.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(6.dp).background(Color(0xFFFFB74D), CircleShape))
                                Text("Beban (${(bebanPct * 100).toInt()}%)", fontSize = 9.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            item {
                val accentColor = if (state.labaBersih >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                Card(
                    colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LABA/RUGI BERSIH (NET INCOME)",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = accentColor
                        )
                        Text(
                            text = formatRupiah(state.labaBersih),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = accentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BalanceSheetWidget(state: AccountingReportState, isWide: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxSize(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Neraca Keseimbangan (Balance Sheet)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Formula: Aktiva (Aset) = Pasiva (Utang + Modal + Laba)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Divider(modifier = Modifier.padding(vertical = 4.dp))
            }

            if (isWide) {
                // Side-by-side Aktiva & Pasiva for Tablets (Classic double entry ledger look!)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Section Aktiva (LEFT)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AKTIVA (ASSETS)", fontWeight = FontWeight.Black, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(6.dp))
                            ReportValueLine("Kas & Tabungan Bank", state.kasBalance)
                            ReportValueLine("Nilai Persediaan Gudang", state.persediaanBalance)
                            ReportValueLine("Tagihan Piutang Sales", state.piutangBalance)
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            ReportValueLine("TOTAL AKTIVA / ASSETS", state.totalAssets, isHeader = true, highlightColor = MaterialTheme.colorScheme.primary)
                        }

                        // Section Pasiva (RIGHT)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("PASIVA (LIABILITIES & EQUITY)", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color(0xFFC62828))
                            Spacer(modifier = Modifier.height(6.dp))
                            ReportValueLine("Utang Dagang Usaha", state.utangBalance)
                            ReportValueLine("Akumulasi Modal Awal", state.modalBalance)
                            ReportValueLine("Laba Ditahan Saat Ini", state.labaBersih)
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            ReportValueLine("TOTAL PASIVA / LIABILITAS", state.totalPasiva, isHeader = true, highlightColor = Color(0xFFC62828))
                        }
                    }
                }
            } else {
                // Stacked format for Small Devices
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("AKTIVA (ASSET)", fontWeight = FontWeight.Black, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        ReportValueLine("Kas & Bank", state.kasBalance)
                        ReportValueLine("Persediaan Barang", state.persediaanBalance)
                        ReportValueLine("Piutang Usaha", state.piutangBalance)
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        ReportValueLine("TOTAL AKTIVA / ASSETS", state.totalAssets, isHeader = true, highlightColor = MaterialTheme.colorScheme.primary)
                    }
                }

                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("PASIVA (LIABILITAS & EQUITY)", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color(0xFFC62828))
                        Spacer(modifier = Modifier.height(4.dp))
                        ReportValueLine("Utang Toko Usaha", state.utangBalance)
                        ReportValueLine("Modal Pemilik", state.modalBalance)
                        ReportValueLine("Laba Ditahan Berjalan", state.labaBersih)
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        ReportValueLine("TOTAL PASIVA", state.totalPasiva, isHeader = true, highlightColor = Color(0xFFC62828))
                    }
                }
            }

            // Balance Check Toast indicator inside lists!
            item {
                val isBalanced = Math.abs(state.totalAssets - state.totalPasiva) < 1.0
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isBalanced) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isBalanced) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = if (isBalanced) Color(0xFF2E7D32) else Color(0xFFC62828),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBalanced) "Neraca Seimbang & Valid!" else "Neraca Tidak Seimbang!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isBalanced) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun JournalLedgerWidget(
    journalHistory: List<com.example.data.model.JournalEntry>,
    dateFormat: SimpleDateFormat
) {
    if (journalHistory.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.Book,
            message = "Jurnal Ledger Kosong",
            hint = "Pencatatan double-entry otomatis belum membukukan transaksi harian apa pun."
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(journalHistory) { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(entry.description, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(dateFormat.format(Date(entry.date)), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Akun: ${entry.accountName}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (entry.debit > 0) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("DEBIT", fontSize = 8.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                        Text(formatRupiah(entry.debit), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF2E7D32))
                                    }
                                }
                                if (entry.credit > 0) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("KREDIT", fontSize = 8.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                                        Text(formatRupiah(entry.credit), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFC62828))
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
fun ReportValueLine(
    label: String,
    value: Double,
    isHeader: Boolean = false,
    isSubtracted: Boolean = false,
    highlightColor: Color? = null
) {
    val sizeText = if (isHeader) 13.sp else 12.sp
    val font = if (isHeader) FontWeight.Bold else FontWeight.Normal
    val labelText = if (isSubtracted) "(-) $label" else label

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(labelText, fontWeight = font, fontSize = sizeText, color = if (isSubtracted) Color.Gray else MaterialTheme.colorScheme.onSurface)
        Text(
            text = if (isSubtracted) "- ${formatRupiah(value)}" else formatRupiah(value),
            fontWeight = FontWeight.Bold,
            fontSize = sizeText,
            color = highlightColor ?: if (isSubtracted) Color.Gray else MaterialTheme.colorScheme.onSurface
        )
    }
}
