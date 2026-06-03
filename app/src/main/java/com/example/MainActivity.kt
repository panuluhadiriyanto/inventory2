package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewModel.AppScreen
import com.example.ui.viewModel.InventoryAccountingViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppLayout()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppLayout() {
    val viewModel: InventoryAccountingViewModel = viewModel()
    val currentScreen by viewModel.currentScreen.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(310.dp),
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            ) {
                // Header of Drawer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 24.dp, vertical = 28.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Text(
                                "AsetKu Corp.",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Sistem Keuangan & Logistik",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DrawerCategoryLabel("Dashboard Utama")
                    DrawerItemRow(
                        label = "Dashboard",
                        icon = Icons.Default.Dashboard,
                        selected = currentScreen == AppScreen.DASHBOARD,
                        onClick = {
                            viewModel.navigateTo(AppScreen.DASHBOARD)
                            scope.launch { drawerState.close() }
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                    DrawerCategoryLabel("Alur Transaksi")
                    DrawerItemRow(
                        label = "Penjualan (Kasir)",
                        icon = Icons.Default.ShoppingCart,
                        selected = currentScreen == AppScreen.TRANSAKSI_PENJUALAN,
                        onClick = {
                            viewModel.navigateTo(AppScreen.TRANSAKSI_PENJUALAN)
                            scope.launch { drawerState.close() }
                        }
                    )
                    DrawerItemRow(
                        label = "Pembelian (Restock)",
                        icon = Icons.Default.LocalShipping,
                        selected = currentScreen == AppScreen.TRANSAKSI_PEMBELIAN,
                        onClick = {
                            viewModel.navigateTo(AppScreen.TRANSAKSI_PEMBELIAN)
                            scope.launch { drawerState.close() }
                        }
                    )
                    DrawerItemRow(
                        label = "Retur Barang",
                        icon = Icons.Default.SettingsBackupRestore,
                        selected = currentScreen == AppScreen.TRANSAKSI_RETUR,
                        onClick = {
                            viewModel.navigateTo(AppScreen.TRANSAKSI_RETUR)
                            scope.launch { drawerState.close() }
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                    DrawerCategoryLabel("Piutang & Utang")
                    DrawerItemRow(
                        label = "Utang ke Supplier",
                        icon = Icons.Default.MoneyOff,
                        selected = currentScreen == AppScreen.HUTANG,
                        onClick = {
                            viewModel.navigateTo(AppScreen.HUTANG)
                            scope.launch { drawerState.close() }
                        }
                    )
                    DrawerItemRow(
                        label = "Piutang Pelanggan",
                        icon = Icons.Default.AttachMoney,
                        selected = currentScreen == AppScreen.PIUTANG,
                        onClick = {
                            viewModel.navigateTo(AppScreen.PIUTANG)
                            scope.launch { drawerState.close() }
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                    DrawerCategoryLabel("Data Master")
                    DrawerItemRow(
                        label = "Katalog Barang",
                        icon = Icons.Default.Inventory2,
                        selected = currentScreen == AppScreen.MASTER_BARANG,
                        onClick = {
                            viewModel.navigateTo(AppScreen.MASTER_BARANG)
                            scope.launch { drawerState.close() }
                        }
                    )
                    DrawerItemRow(
                        label = "Kategori",
                        icon = Icons.Default.Category,
                        selected = currentScreen == AppScreen.MASTER_KATEGORI,
                        onClick = {
                            viewModel.navigateTo(AppScreen.MASTER_KATEGORI)
                            scope.launch { drawerState.close() }
                        }
                    )
                    DrawerItemRow(
                        label = "Suppliers",
                        icon = Icons.Default.Business,
                        selected = currentScreen == AppScreen.MASTER_SUPLIER,
                        onClick = {
                            viewModel.navigateTo(AppScreen.MASTER_SUPLIER)
                            scope.launch { drawerState.close() }
                        }
                    )
                    DrawerItemRow(
                        label = "Pelanggan",
                        icon = Icons.Default.People,
                        selected = currentScreen == AppScreen.MASTER_PELANGGAN,
                        onClick = {
                            viewModel.navigateTo(AppScreen.MASTER_PELANGGAN)
                            scope.launch { drawerState.close() }
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                    DrawerCategoryLabel("Akuntansi & Pembukuan")
                    DrawerItemRow(
                        label = "Laporan Keuangan",
                        icon = Icons.Default.Analytics,
                        selected = currentScreen == AppScreen.LAPORAN_KEUANGAN,
                        onClick = {
                            viewModel.navigateTo(AppScreen.LAPORAN_KEUANGAN)
                            scope.launch { drawerState.close() }
                        }
                    )
                    DrawerItemRow(
                        label = "Tentang Aplikasi",
                        icon = Icons.Default.Info,
                        selected = currentScreen == AppScreen.ABOUT,
                        onClick = {
                            viewModel.navigateTo(AppScreen.ABOUT)
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = getScreenTitleInIndonesian(currentScreen),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        // Quick Action button to launch POS / Checkout directly
                        if (currentScreen != AppScreen.TRANSAKSI_PENJUALAN) {
                            IconButton(onClick = { viewModel.navigateTo(AppScreen.TRANSAKSI_PENJUALAN) }) {
                                Icon(Icons.Default.AddShoppingCart, contentDescription = "Kasir POS", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Crossfade(targetState = currentScreen, label = "screen_crossfade") { target ->
                    when (target) {
                        AppScreen.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                        AppScreen.MASTER_KATEGORI -> MasterCategoryScreen(viewModel = viewModel)
                        AppScreen.MASTER_BARANG -> MasterItemScreen(viewModel = viewModel)
                        AppScreen.MASTER_SUPLIER -> MasterSupplierScreen(viewModel = viewModel)
                        AppScreen.MASTER_PELANGGAN -> MasterCustomerScreen(viewModel = viewModel)
                        AppScreen.TRANSAKSI_PENJUALAN -> SalesTransactionScreen(viewModel = viewModel)
                        AppScreen.TRANSAKSI_PEMBELIAN -> PurchaseTransactionScreen(viewModel = viewModel)
                        AppScreen.TRANSAKSI_RETUR -> ReturnTransactionScreen(viewModel = viewModel)
                        AppScreen.HUTANG -> DebtScreen(viewModel = viewModel)
                        AppScreen.PIUTANG -> ReceivableScreen(viewModel = viewModel)
                        AppScreen.LAPORAN_KEUANGAN -> AccountingReportsScreen(viewModel = viewModel)
                        AppScreen.ABOUT -> AboutScreen()
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerCategoryLabel(label: String) {
    Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun DrawerItemRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

fun getScreenTitleInIndonesian(screen: AppScreen): String {
    return when (screen) {
        AppScreen.DASHBOARD -> "Dashboard Utama"
        AppScreen.MASTER_KATEGORI -> "Daftar Kategori"
        AppScreen.MASTER_BARANG -> "Katalog Persediaan Barang"
        AppScreen.MASTER_SUPLIER -> "Mitra Supplier"
        AppScreen.MASTER_PELANGGAN -> "Mitra Pelanggan"
        AppScreen.TRANSAKSI_PENJUALAN -> "Penjualan (Kasir POS)"
        AppScreen.TRANSAKSI_PEMBELIAN -> "Pembelian (Restock Barang)"
        AppScreen.TRANSAKSI_RETUR -> "Retur Pengembalian"
        AppScreen.HUTANG -> "Utang Supplier (Payables)"
        AppScreen.PIUTANG -> "Piutang Dagang (Receivables)"
        AppScreen.LAPORAN_KEUANGAN -> "Laporan Keuangan & Jurnal"
        AppScreen.ABOUT -> "Tentang Pengembang"
    }
}
