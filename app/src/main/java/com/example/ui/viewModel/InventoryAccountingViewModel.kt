package com.example.ui.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.AppRepository
import com.example.data.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class InventoryAccountingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AppRepository

    init {
        val appDao = AppDatabase.getDatabase(application).appDao()
        repository = AppRepository(appDao)
        seedInitialData()
    }

    // --- REACTIVE FLOWS FROM DATABASE ---
    val categories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val items: StateFlow<List<Item>> = repository.allItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers: StateFlow<List<Supplier>> = repository.allSuppliers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<Customer>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sales: StateFlow<List<Sale>> = repository.allSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val purchases: StateFlow<List<Purchase>> = repository.allPurchases
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val returns: StateFlow<List<ReturnTrans>> = repository.allReturns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val debts: StateFlow<List<Debt>> = repository.allDebts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val receivables: StateFlow<List<Receivable>> = repository.allReceivables
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val journalEntries: StateFlow<List<JournalEntry>> = repository.allJournalEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- CARTS & RUNTIME UI STATE ---
    // Sales Cart
    private val _salesCart = MutableStateFlow<List<CartItem>>(emptyList())
    val salesCart: StateFlow<List<CartItem>> = _salesCart.asStateFlow()

    // Purchases Cart
    private val _purchasesCart = MutableStateFlow<List<CartItem>>(emptyList())
    val purchasesCart: StateFlow<List<CartItem>> = _purchasesCart.asStateFlow()

    // Current Active Screen
    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    // --- ACTIONS: MASTER DATA ---
    fun addCategory(name: String) {
        viewModelScope.launch {
            repository.insertCategory(Category(name = name))
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }

    fun addItem(name: String, catId: Int, sku: String, buyPrice: Double, sellPrice: Double, qty: Int, minStock: Int) {
        viewModelScope.launch {
            repository.insertItem(
                Item(
                    name = name,
                    categoryId = catId,
                    skuBarcode = sku,
                    purchasePrice = buyPrice,
                    sellingPrice = sellPrice,
                    stockQuantity = qty,
                    minStockAlert = minStock
                )
            )
        }
    }

    fun updateItem(item: Item) {
        viewModelScope.launch {
            repository.updateItem(item)
        }
    }

    fun deleteItem(item: Item) {
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }

    fun addSupplier(name: String, phone: String, address: String) {
        viewModelScope.launch {
            repository.insertSupplier(Supplier(name = name, phone = phone, address = address))
        }
    }

    fun deleteSupplier(supplier: Supplier) {
        viewModelScope.launch {
            repository.deleteSupplier(supplier)
        }
    }

    fun addCustomer(name: String, phone: String, address: String) {
        viewModelScope.launch {
            repository.insertCustomer(Customer(name = name, phone = phone, address = address))
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
        }
    }

    // --- CART MANAGEMENT ---
    fun addToSalesCart(item: Item, quantity: Int = 1) {
        val current = _salesCart.value.toMutableList()
        val index = current.indexOfFirst { it.item.id == item.id }
        if (index != -1) {
            current[index] = current[index].copy(quantity = current[index].quantity + quantity)
        } else {
            current.add(CartItem(item, quantity))
        }
        _salesCart.value = current
    }

    fun updateSalesCartQuantity(itemId: Int, quantity: Int) {
        if (quantity <= 0) {
            _salesCart.value = _salesCart.value.filter { it.item.id != itemId }
        } else {
            _salesCart.value = _salesCart.value.map {
                if (it.item.id == itemId) it.copy(quantity = quantity) else it
            }
        }
    }

    fun clearSalesCart() {
        _salesCart.value = emptyList()
    }

    fun addToPurchasesCart(item: Item, quantity: Int = 1) {
        val current = _purchasesCart.value.toMutableList()
        val index = current.indexOfFirst { it.item.id == item.id }
        if (index != -1) {
            current[index] = current[index].copy(quantity = current[index].quantity + quantity)
        } else {
            current.add(CartItem(item, quantity))
        }
        _purchasesCart.value = current
    }

    fun updatePurchasesCartQuantity(itemId: Int, quantity: Int) {
        if (quantity <= 0) {
            _purchasesCart.value = _purchasesCart.value.filter { it.item.id != itemId }
        } else {
            _purchasesCart.value = _purchasesCart.value.map {
                if (it.item.id == itemId) it.copy(quantity = quantity) else it
            }
        }
    }

    fun clearPurchasesCart() {
        _purchasesCart.value = emptyList()
    }

    // --- TRANSACTION CHECKOUT ---
    fun checkoutSales(
        customer: Customer?,
        discount: Double,
        paymentType: String,
        amountPaid: Double,
        onSuccess: (Sale) -> Unit
    ) {
        viewModelScope.launch {
            val cartList = _salesCart.value
            if (cartList.isEmpty()) return@launch

            val subtotal = cartList.sumOf { it.item.sellingPrice * it.quantity }
            val totalPrice = maxOf(0.0, subtotal - discount)
            val invoice = "INV-${System.currentTimeMillis() / 1000}"
            val change = if (paymentType == "CASH") maxOf(0.0, amountPaid - totalPrice) else 0.0

            val sale = Sale(
                invoiceNumber = invoice,
                customerId = customer?.id,
                customerName = customer?.name ?: "Pelanggan Umum",
                date = System.currentTimeMillis(),
                subtotal = subtotal,
                discount = discount,
                totalPrice = totalPrice,
                paymentType = paymentType,
                amountPaid = if (paymentType == "CASH") amountPaid else 0.0,
                changeAmount = change
            )

            val details = cartList.map {
                SaleDetail(
                    saleId = 0,
                    itemId = it.item.id,
                    itemName = it.item.name,
                    quantity = it.quantity,
                    unitPrice = it.item.sellingPrice,
                    totalPrice = it.item.sellingPrice * it.quantity
                )
            }

            repository.insertSaleTransaction(sale, details)
            _salesCart.value = emptyList()
            onSuccess(sale)
        }
    }

    fun checkoutPurchase(
        supplier: Supplier,
        paymentType: String,
        amountPaid: Double,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val cartList = _purchasesCart.value
            if (cartList.isEmpty()) return@launch

            val totalPrice = cartList.sumOf { it.item.purchasePrice * it.quantity }
            val invoice = "PO-${System.currentTimeMillis() / 1000}"

            val purchase = Purchase(
                invoiceNumber = invoice,
                supplierId = supplier.id,
                supplierName = supplier.name,
                date = System.currentTimeMillis(),
                totalPrice = totalPrice,
                paymentType = paymentType,
                amountPaid = if (paymentType == "CASH") totalPrice else amountPaid
            )

            val details = cartList.map {
                PurchaseDetail(
                    purchaseId = 0,
                    itemId = it.item.id,
                    itemName = it.item.name,
                    quantity = it.quantity,
                    unitPrice = it.item.purchasePrice,
                    totalPrice = it.item.purchasePrice * it.quantity
                )
            }

            repository.insertPurchaseTransaction(purchase, details)
            _purchasesCart.value = emptyList()
            onSuccess()
        }
    }

    // --- DEBT & RECEIVABLE REPAYMENTS ---
    fun repayDebt(debt: Debt, amount: Double) {
        viewModelScope.launch {
            repository.updateDebtStatus(debt, amount)
        }
    }

    fun collectReceivable(receivable: Receivable, amount: Double) {
        viewModelScope.launch {
            repository.updateReceivableStatus(receivable, amount)
        }
    }

    // --- RETURNS ---
    fun submitReturn(
        invoiceRef: String,
        type: String, // "SALE_RETURN" or "PURCHASE_RETURN"
        reason: String,
        returnedItems: List<Pair<Item, Int>>
    ) {
        viewModelScope.launch {
            if (returnedItems.isEmpty()) return@launch

            val returnNumber = "RET-${System.currentTimeMillis() / 1000}"
            val totalRefund = returnedItems.sumOf { (item, qty) ->
                if (type == "SALE_RETURN") item.sellingPrice * qty else item.purchasePrice * qty
            }

            val returnTrans = ReturnTrans(
                returnNumber = returnNumber,
                date = System.currentTimeMillis(),
                type = type,
                referenceInvoiceNumber = invoiceRef,
                totalRefund = totalRefund,
                reason = reason
            )

            val details = returnedItems.map { (item, qty) ->
                ReturnDetail(
                    returnId = 0,
                    itemId = item.id,
                    itemName = item.name,
                    quantity = qty,
                    refundPrice = if (type == "SALE_RETURN") item.sellingPrice else item.purchasePrice,
                    totalPrice = if (type == "SALE_RETURN") item.sellingPrice * qty else item.purchasePrice * qty
                )
            }

            repository.insertReturnTransaction(returnTrans, details)
        }
    }

    // --- CUSTOM OPERATIONAL EXPENSES ---
    fun recordExpense(description: String, amount: Double) {
        viewModelScope.launch {
            val date = System.currentTimeMillis()
            // Expense entries
            // Debit: Beban Operasional / Lainnya (we represent it within HPP or as custom ledger item, let's designate "Beban Operasional" as name)
            repository.insertJournalEntry(
                JournalEntry(
                    date = date,
                    description = "Beban: $description",
                    accountName = "Beban Operasional",
                    debit = amount,
                    credit = 0.0,
                    referenceId = "OP-EXP-${date / 1000}"
                )
            )
            // Credit: Kas & Bank
            repository.insertJournalEntry(
                JournalEntry(
                    date = date,
                    description = "Pengeluaran Kas: $description",
                    accountName = "Kas & Bank",
                    debit = 0.0,
                    credit = amount,
                    referenceId = "OP-EXP-${date / 1000}"
                )
            )
        }
    }

    // --- REVENUE & ACCOUNTING MODEL GENERATORS ---
    // Automates generation of Finance Report states
    val accountingReport: StateFlow<AccountingReportState> = journalEntries.map { entries ->
        // Group by Accounts
        var kas = 0.0
        var persediaan = 0.0
        var piutang = 0.0
        var utang = 0.0
        var modal = 50000000.0 // seeded default capital
        var pendapatan = 0.0
        var hpp = 0.0
        var beban = 0.0

        for (e in entries) {
            when (e.accountName) {
                "Kas & Bank" -> {
                    kas += (e.debit - e.credit)
                }
                "Persediaan Barang" -> {
                    persediaan += (e.debit - e.credit)
                }
                "Piutang Usaha" -> {
                    piutang += (e.debit - e.credit)
                }
                "Utang Usaha" -> {
                    utang += (e.credit - e.debit) // credit is normal
                }
                "Modal", "Modal Awal" -> {
                    modal = modal + e.credit - e.debit // credit normal
                }
                "Pendapatan Penjualan" -> {
                    pendapatan += (e.credit - e.debit) // credit normal
                }
                "Harga Pokok Penjualan (HPP)" -> {
                    hpp += (e.debit - e.credit) // debit normal
                }
                "Beban Operasional" -> {
                    beban += (e.debit - e.credit) // debit normal
                }
            }
        }

        // Laba Kotor / Bersih
        val labaKotor = pendapatan - hpp
        val labaBersih = labaKotor - beban

        // Equity with retained earnings (Laba Bersih)
        val totalEquity = modal + labaBersih
        val totalAssets = kas + persediaan + piutang
        val totalPasiva = utang + totalEquity

        AccountingReportState(
            kasBalance = kas,
            persediaanBalance = persediaan,
            piutangBalance = piutang,
            utangBalance = utang,
            modalBalance = modal,
            revenueTotal = pendapatan,
            hppTotal = hpp,
            bebanTotal = beban,
            labaKotor = labaKotor,
            labaBersih = labaBersih,
            totalAssets = totalAssets,
            totalPasiva = totalPasiva,
            journalCount = entries.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccountingReportState())

    // --- SEEDING METHOD ON START ---
    private fun seedInitialData() {
        viewModelScope.launch {
            // Check if Categories is empty, then seed all Master data
            repository.allCategories.take(1).collect { currentCats ->
                if (currentCats.isEmpty()) {
                    // Seed Categories
                    repository.insertCategory(Category(name = "Makanan"))
                    repository.insertCategory(Category(name = "Minuman"))
                    repository.insertCategory(Category(name = "Elektronik"))
                    repository.insertCategory(Category(name = "Pakaian"))

                    // Wait a bit, then seed items
                    val dummyCats = listOf(
                        Triple("Makanan", 1, "899123456001"),
                        Triple("Minuman", 2, "899123456002"),
                        Triple("Elektronik", 3, "899123456003"),
                        Triple("Pakaian", 4, "899123456004")
                    )

                    repository.insertSupplier(Supplier(name = "PT. Berkah Sentosa", phone = "081234567890", address = "Kawasan Industri MM2100, Bekasi"))
                    repository.insertSupplier(Supplier(name = "PT. Sinar Distribusi", phone = "082198765432", address = "Suryacipta City, Karawang"))

                    repository.insertCustomer(Customer(name = "Anto Wijaya", phone = "085611223344", address = "Jl. Sudirman No 12, Jakarta"))
                    repository.insertCustomer(Customer(name = "Rina Amelia", phone = "087755667788", address = "Kost Putri Melati, Bandung"))

                    // Create items
                    repository.insertItem(Item(name = "Kopi Susu Espresso", categoryId = 1, skuBarcode = "899123456001", purchasePrice = 8000.0, sellingPrice = 13000.0, stockQuantity = 120, minStockAlert = 10))
                    repository.insertItem(Item(name = "Teh Matcha GreenTea", categoryId = 2, skuBarcode = "899123456002", purchasePrice = 5000.0, sellingPrice = 9000.0, stockQuantity = 80, minStockAlert = 5))
                    repository.insertItem(Item(name = "Powerbank Fast Charge 20W", categoryId = 3, skuBarcode = "899123456003", purchasePrice = 110000.0, sellingPrice = 175000.0, stockQuantity = 15, minStockAlert = 3))
                    repository.insertItem(Item(name = "Kemeja Flanel SlimFit", categoryId = 4, skuBarcode = "899123456004", purchasePrice = 95000.0, sellingPrice = 160000.0, stockQuantity = 3, minStockAlert = 5)) // Will trigger warning in dashboard!

                    // Seed Modal Awal to Kas
                    val date = System.currentTimeMillis()
                    repository.insertJournalEntry(
                        JournalEntry(
                            date = date - 86400000 * 2, // 2 days ago
                            description = "Penyetoran Modal Awal Tunai",
                            accountName = "Kas & Bank",
                            debit = 50000000.0,
                            credit = 0.0,
                            referenceId = "MODAL-INIT"
                        )
                    )
                    repository.insertJournalEntry(
                        JournalEntry(
                            date = date - 86400000 * 2,
                            description = "Penyetoran Modal Awal Tunai",
                            accountName = "Modal",
                            debit = 0.0,
                            credit = 50000000.0,
                            referenceId = "MODAL-INIT"
                        )
                    )
                }
            }
        }
    }
}

// --- SUPPORT CLASSES & ENUMS ---
data class CartItem(
    val item: Item,
    var quantity: Int
)

enum class AppScreen {
    DASHBOARD,
    MASTER_KATEGORI,
    MASTER_BARANG,
    MASTER_SUPLIER,
    MASTER_PELANGGAN,
    TRANSAKSI_PENJUALAN,
    TRANSAKSI_PEMBELIAN,
    TRANSAKSI_RETUR,
    HUTANG,
    PIUTANG,
    LAPORAN_KEUANGAN,
    ABOUT
}

data class AccountingReportState(
    val kasBalance: Double = 50000000.0, // Initial seed
    val persediaanBalance: Double = 0.0,
    val piutangBalance: Double = 0.0,
    val utangBalance: Double = 0.0,
    val modalBalance: Double = 50000000.0,
    val revenueTotal: Double = 0.0,
    val hppTotal: Double = 0.0,
    val bebanTotal: Double = 0.0,
    val labaKotor: Double = 0.0,
    val labaBersih: Double = 0.0,
    val totalAssets: Double = 50000000.0,
    val totalPasiva: Double = 50000000.0,
    val journalCount: Int = 0
)
