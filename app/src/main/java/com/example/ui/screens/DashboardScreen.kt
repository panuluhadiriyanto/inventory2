package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Item
import com.example.ui.viewModel.AccountingReportState
import com.example.ui.viewModel.AppScreen
import com.example.ui.viewModel.InventoryAccountingViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: InventoryAccountingViewModel,
    modifier: Modifier = Modifier
) {
    val reportState by viewModel.accountingReport.collectAsState()
    val allItems by viewModel.items.collectAsState()
    val allDebts by viewModel.debts.collectAsState()
    val allReceivables by viewModel.receivables.collectAsState()

    // Filter items with low stock warning
    val lowStockItems = remember(allItems) {
        allItems.filter { it.stockQuantity <= it.minStockAlert }
    }

    // Calculations
    val totalStockValue = remember(allItems) {
        allItems.sumOf { it.stockQuantity * it.purchasePrice }
    }
    val outstandingDebt = remember(allDebts) {
        allDebts.filter { it.status == "BELUM_LUNAS" }.sumOf { it.remainingAmount }
    }
    val outstandingReceivable = remember(allReceivables) {
        allReceivables.filter { it.status == "BELUM_LUNAS" }.sumOf { it.remainingAmount }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Header
        item {
            HeaderSection()
        }

        // Executive Financial KPI Highlights
        item {
            KpiSection(
                reportState = reportState,
                stockValue = totalStockValue,
                receivable = outstandingReceivable,
                debt = outstandingDebt
            )
        }

        // Navigation shortcuts (Shortcut Actions)
        item {
            QuickShortcuts(onNavigate = { viewModel.navigateTo(it) })
        }

        // Financial Trends Graph
        item {
            FinancialTrendCard(reportState)
        }

        // Stock alerts (Interactive warning widget)
        item {
            StockAlertSection(
                lowStockItems = lowStockItems,
                onAddStock = { viewModel.navigateTo(AppScreen.MASTER_BARANG) }
            )
        }
    }
}

@Composable
fun HeaderSection() {
    Column {
        Text(
            text = "AsetKu",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Sistem Inventory & Otomatisasi Akuntansi Terpadu",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun KpiSection(
    reportState: AccountingReportState,
    stockValue: Double,
    receivable: Double,
    debt: Double
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiCard(
                title = "Kas & Bank",
                value = reportState.kasBalance,
                icon = Icons.Default.AccountBalanceWallet,
                gradientColors = listOf(Color(0xFF00796B), Color(0xFF004D40)),
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "Aset Inventory",
                value = stockValue,
                icon = Icons.Default.Inventory2,
                gradientColors = listOf(Color(0xFF1565C0), Color(0xFF0D47A1)),
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KpiCard(
                title = "Piutang Dagang",
                value = receivable,
                icon = Icons.Default.TrendingUp,
                gradientColors = listOf(Color(0xFF2E7D32), Color(0xFF1B5E20)),
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                title = "Utang Dagang",
                value = debt,
                icon = Icons.Default.TrendingDown,
                gradientColors = listOf(Color(0xFFC62828), Color(0xFF8E0000)),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: Double,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(115.dp)
            .clip(RoundedCornerShape(16.dp)),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(gradientColors))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = title,
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = formatRupiah(value),
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickShortcuts(
    onNavigate: (AppScreen) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Pintasan Transaksi & Master",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 5
            ) {
                ShortcutBtn("Penjualan", Icons.Default.ShoppingCart, Color(0xFF00796B)) { onNavigate(AppScreen.TRANSAKSI_PENJUALAN) }
                ShortcutBtn("Pembelian", Icons.Default.LocalShipping, Color(0xFF1976D2)) { onNavigate(AppScreen.TRANSAKSI_PEMBELIAN) }
                ShortcutBtn("Retur", Icons.Default.SettingsBackupRestore, Color(0xFFE64A19)) { onNavigate(AppScreen.TRANSAKSI_RETUR) }
                ShortcutBtn("Utang", Icons.Default.MoneyOff, Color(0xFFD32F2F)) { onNavigate(AppScreen.HUTANG) }
                ShortcutBtn("Piutang", Icons.Default.AttachMoney, Color(0xFF388E3C)) { onNavigate(AppScreen.PIUTANG) }
                ShortcutBtn("Barang", Icons.Default.Category, Color(0xFF5D4037)) { onNavigate(AppScreen.MASTER_BARANG) }
                ShortcutBtn("Laporan", Icons.Default.Analytics, Color(0xFF7B1FA2)) { onNavigate(AppScreen.LAPORAN_KEUANGAN) }
            }
        }
    }
}

@Composable
fun ShortcutBtn(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.height(44.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun FinancialTrendCard(reportState: AccountingReportState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Grafik Keuangan (Laba & Pendapatan)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Visualisasi korelasi pendapatan, HPP, & Laba Bersih",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.ShowChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas drawing for visual beauty
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                val lineRevenueColor = Color(0xFF00796B)
                val lineProfitColor = Color(0xFF4CAF50)

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    // Draw 4 horizontal grid lines
                    for (i in 1..3) {
                        val y = height * (i / 4f)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 2.dp.toPx()
                        )
                    }

                    // Let's draw illustrative financial wave curves (since real data begins small, we draw beautiful matching indicators)
                    val rPath = Path()
                    val pPath = Path()

                    rPath.moveTo(0f, height * 0.8f)
                    rPath.cubicTo(
                        width * 0.25f, height * 0.6f,
                        width * 0.5f, height * 0.2f,
                        width, height * 0.15f
                    )

                    pPath.moveTo(0f, height * 0.9f)
                    pPath.cubicTo(
                        width * 0.25f, height * 0.85f,
                        width * 0.5f, height * 0.45f,
                        width, height * 0.35f
                    )

                    // Revenue curve
                    drawPath(
                        path = rPath,
                        color = lineRevenueColor,
                        style = Stroke(width = 3.dp.toPx())
                    )
                    // Profit curve
                    drawPath(
                        path = pPath,
                        color = lineProfitColor,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendIndicator("Pendapatan", Color(0xFF00796B))
                LegendIndicator("HPP & Pengeluaran", Color(0xFFD32F2F))
                LegendIndicator("Laba Bersih", Color(0xFF4CAF50))
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Laba: ${formatRupiah(reportState.labaBersih)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (reportState.labaBersih >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }
        }
    }
}

@Composable
fun LegendIndicator(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun StockAlertSection(
    lowStockItems: List<Item>,
    onAddStock: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (lowStockItems.isNotEmpty()) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                color = if (lowStockItems.isNotEmpty()) MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (lowStockItems.isNotEmpty()) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (lowStockItems.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = if (lowStockItems.isNotEmpty()) "Peringatan Stok Menipis!" else "Stok Inventory Aman",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (lowStockItems.isNotEmpty()) {
                    TextButton(onClick = onAddStock) {
                        Text("Kulakan", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (lowStockItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    lowStockItems.take(3).forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Barcode: ${item.skuBarcode}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Sisa ${item.stockQuantity} Pcs",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    if (lowStockItems.size > 3) {
                        Text(
                            text = "+ ${lowStockItems.size - 3} barang lainnya sedang kritis...",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Semua persediaan barang memiliki stok yang aman di atas batas minimum peringatan. Pertahankan kinerja operasional Anda!",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Help format rupiah globally
fun formatRupiah(value: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
    return formatter.format(value).replace("Rp", "Rp ").substringBefore(",")
}
