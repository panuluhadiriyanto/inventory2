package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

    var activeTab by remember { mutableStateOf(0) } // 0: Laba Rugi, 1: Neraca Balanced, 2: Kas & Jurnal
    var showExpenseDialog by remember { mutableStateOf(false) }

    var expenseDescInput by remember { mutableStateOf("") }
    var expenseAmountInput by remember { mutableStateOf("") }

    val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())

    Column(modifier = modifier.fillMaxSize().padding(16.dp).testTag("accounting_reports_screen")) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Laporan Keuangan Otomatis", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("Neraca keseimbangan, rincian aktivitas HPP & Laba", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = { showExpenseDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AddBusiness, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Bayar Beban")
            }
        }

        // Beautiful custom Tab Row layout
        TabRow(
            selectedTabIndex = activeTab,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }) {
                Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Laba Rugi", fontWeight = FontWeight.Bold)
                }
            }
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }) {
                Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Scale, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Neraca", fontWeight = FontWeight.Bold)
                }
            }
            Tab(selected = activeTab == 2, onClick = { activeTab = 2 }) {
                Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Jurnal Ledger", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Dinamis Content based on active selected index
        Box(modifier = Modifier.weight(1f)) {
            when (activeTab) {
                0 -> IncomeStatementWidget(reportState)
                1 -> BalanceSheetWidget(reportState)
                2 -> JournalLedgerWidget(journalHistory, dateFormat)
            }
        }

        if (showExpenseDialog) {
            AlertDialog(
                onDismissRequest = { showExpenseDialog = false },
                title = { Text("Input Beban Pengeluaran Baru") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Gunakan form ini untuk mencatat beban biaya di luar HPP seperti: Biaya Sewa, Biaya Wifi, Gaji, dll.", fontSize = 12.sp, color = Color.Gray)
                        OutlinedTextField(
                            value = expenseDescInput,
                            onValueChange = { expenseDescInput = it },
                            label = { Text("Deskripsi Beban (Contoh: Gaji Pegawai)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = expenseAmountInput,
                            onValueChange = { expenseAmountInput = it },
                            label = { Text("Jumlah Uang Pengeluaran (Rp)") },
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
                        }
                    ) {
                        Text("Posting Beban")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExpenseDialog = false }) { Text("Batal") }
                }
            )
        }
    }
}

@Composable
fun IncomeStatementWidget(state: AccountingReportState) {
    Card(
        modifier = Modifier.fillMaxSize(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Laporan Laba Rugi (Income Statement)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Periode berjalan otomatis akrual", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                ReportValueLine("Pendapatan Kotor Penjualan", state.revenueTotal, isHeader = true)
                Divider(modifier = Modifier.padding(vertical = 4.dp))
            }

            item {
                ReportValueLine("Harga Pokok Penjualan (HPP)", state.hppTotal, isSubtracted = true)
                Divider(modifier = Modifier.padding(vertical = 4.dp))
            }

            item {
                ReportValueLine("Total Laba Kotor (Gross Profit)", state.labaKotor, isHeader = true, highlightColor = MaterialTheme.colorScheme.primary)
                Divider(modifier = Modifier.padding(vertical = 4.dp))
            }

            item {
                ReportValueLine("Beban Operasional Tambahan", state.bebanTotal, isSubtracted = true)
                Divider(modifier = Modifier.padding(vertical = 4.dp))
            }

            item {
                val accentColor = if (state.labaBersih >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                Card(
                    colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LABA BERSIH (NET INCOME)",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = accentColor
                        )
                        Text(
                            text = formatRupiah(state.labaBersih),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = accentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BalanceSheetWidget(state: AccountingReportState) {
    Card(
        modifier = Modifier.fillMaxSize(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Neraca Keseimbangan (Balance Sheet)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Aset Lancar harus tepat seimbang dengan Pasiva (Persamaan Akuntansi)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Aset Section
            item {
                Text("ASSET (AKTIVA)", fontWeight = FontWeight.Black, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))
                ReportValueLine(" Kas & Bank", state.kasBalance)
                ReportValueLine(" Persediaan Barang", state.persediaanBalance)
                ReportValueLine(" Piutang Usaha", state.piutangBalance)
                Divider()
                ReportValueLine("TOTAL AKTIVA / ASSETS", state.totalAssets, isHeader = true, highlightColor = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Liability & Equity Section
            item {
                Text("LIABILITAS & EKUITAS (PASIVA)", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color(0xFFC62828))
                Spacer(modifier = Modifier.height(6.dp))
                ReportValueLine(" Utang Usaha (Liabilitas)", state.utangBalance)
                ReportValueLine(" Modal Awal Penyetoran", state.modalBalance)
                ReportValueLine(" Laba Ditahan Ditransfer", state.labaBersih)
                Divider()
                ReportValueLine("TOTAL EKUITAS & PASIVA", state.totalPasiva, isHeader = true, highlightColor = Color(0xFFC62828))
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
                            text = if (isBalanced) "Neraca Balanced & Sempurna!" else "Neraca Tidak Balanced!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
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
            message = "Jurnal kosong",
            hint = "Semua histori pembukuan double-entry akan otomatis dicatat di sini secara sistematis."
        )
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(journalHistory) { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(entry.description, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(dateFormat.format(Date(entry.date)), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Akun: ${entry.accountName}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                if (entry.debit > 0) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("DEBIT", fontSize = 9.sp, color = Color(0xFF2E7D32))
                                        Text(formatRupiah(entry.debit), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF2E7D32))
                                    }
                                }
                                if (entry.credit > 0) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("KREDIT", fontSize = 9.sp, color = Color(0xFFC62828))
                                        Text(formatRupiah(entry.credit), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFC62828))
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
    val sizeText = if (isHeader) 14.sp else 13.sp
    val font = if (isHeader) FontWeight.Bold else FontWeight.Normal
    val labelText = if (isSubtracted) "(-) $label" else label

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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
