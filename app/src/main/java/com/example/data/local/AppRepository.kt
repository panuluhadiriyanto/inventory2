package com.example.data.local

import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class AppRepository(private val appDao: AppDao) {
    // Categories
    val allCategories: Flow<List<Category>> = appDao.getAllCategories()
    suspend fun insertCategory(category: Category) = appDao.insertCategory(category)
    suspend fun deleteCategory(category: Category) = appDao.deleteCategory(category)

    // Items
    val allItems: Flow<List<Item>> = appDao.getAllItems()
    suspend fun getItemById(id: Int): Item? = appDao.getItemById(id)
    suspend fun getItemByBarcode(skuBarcode: String): Item? = appDao.getItemByBarcode(skuBarcode)
    suspend fun insertItem(item: Item) = appDao.insertItem(item)
    suspend fun updateItem(item: Item) = appDao.updateItem(item)
    suspend fun deleteItem(item: Item) = appDao.deleteItem(item)

    // Suppliers
    val allSuppliers: Flow<List<Supplier>> = appDao.getAllSuppliers()
    suspend fun insertSupplier(supplier: Supplier) = appDao.insertSupplier(supplier)
    suspend fun deleteSupplier(supplier: Supplier) = appDao.deleteSupplier(supplier)

    // Customers
    val allCustomers: Flow<List<Customer>> = appDao.getAllCustomers()
    suspend fun insertCustomer(customer: Customer) = appDao.insertCustomer(customer)
    suspend fun deleteCustomer(customer: Customer) = appDao.deleteCustomer(customer)

    // Sales (Penjualan)
    val allSales: Flow<List<Sale>> = appDao.getAllSales()
    fun getSaleDetailsBySaleId(saleId: Int): Flow<List<SaleDetail>> = appDao.getSaleDetailsBySaleId(saleId)

    suspend fun insertSaleTransaction(sale: Sale, details: List<SaleDetail>) {
        val saleId = appDao.insertSale(sale).toInt()
        var totalHpp = 0.0

        for (detail in details) {
            val dbItem = appDao.getItemById(detail.itemId)
            if (dbItem != null) {
                // Decrement stock
                val updatedStock = maxOf(0, dbItem.stockQuantity - detail.quantity)
                appDao.updateItemStock(dbItem.id, updatedStock)
                totalHpp += (dbItem.purchasePrice * detail.quantity)
            }
            appDao.insertSaleDetail(detail.copy(saleId = saleId))
        }

        // --- ACCOUNTING AUTOMATIONS ---
        // 1. Sale record
        if (sale.paymentType == "CASH") {
            // Debit: Kas & Bank
            appDao.insertJournalEntry(
                JournalEntry(
                    date = sale.date,
                    description = "Penjualan Tunai ${sale.invoiceNumber}",
                    accountName = "Kas & Bank",
                    debit = sale.totalPrice,
                    credit = 0.0,
                    referenceId = sale.invoiceNumber
                )
            )
        } else {
            // Debit: Piutang Usaha
            appDao.insertJournalEntry(
                JournalEntry(
                    date = sale.date,
                    description = "Penjualan Kredit ${sale.invoiceNumber} - ${sale.customerName}",
                    accountName = "Piutang Usaha",
                    debit = sale.totalPrice,
                    credit = 0.0,
                    referenceId = sale.invoiceNumber
                )
            )
            // Save Receivable
            if (sale.customerId != null) {
                val cal = Calendar.getInstance()
                cal.timeInMillis = sale.date
                cal.add(Calendar.DAY_OF_YEAR, 30) // Due in 30 days
                appDao.insertReceivable(
                    Receivable(
                        saleId = saleId,
                        customerId = sale.customerId,
                        customerName = sale.customerName,
                        amount = sale.totalPrice,
                        remainingAmount = sale.totalPrice,
                        dueDate = cal.timeInMillis,
                        status = "BELUM_LUNAS"
                    )
                )
            }
        }

        // Credit: Pendapatan Penjualan
        appDao.insertJournalEntry(
            JournalEntry(
                date = sale.date,
                description = "Pendapatan Penjualan ${sale.invoiceNumber}",
                accountName = "Pendapatan Penjualan",
                debit = 0.0,
                credit = sale.totalPrice,
                referenceId = sale.invoiceNumber
            )
        )

        // 2. Inventory reduction & Cost of Goods Sold (HPP)
        if (totalHpp > 0) {
            // Debit: Harga Pokok Penjualan (HPP)
            appDao.insertJournalEntry(
                JournalEntry(
                    date = sale.date,
                    description = "HPP Penjualan ${sale.invoiceNumber}",
                    accountName = "Harga Pokok Penjualan (HPP)",
                    debit = totalHpp,
                    credit = 0.0,
                    referenceId = sale.invoiceNumber
                )
            )
            // Credit: Persediaan Barang
            appDao.insertJournalEntry(
                JournalEntry(
                    date = sale.date,
                    description = "Pengurangan Persediaan ${sale.invoiceNumber}",
                    accountName = "Persediaan Barang",
                    debit = 0.0,
                    credit = totalHpp,
                    referenceId = sale.invoiceNumber
                )
            )
        }
    }

    // Purchases (Pembelian)
    val allPurchases: Flow<List<Purchase>> = appDao.getAllPurchases()
    fun getPurchaseDetailsByPurchaseId(purchaseId: Int): Flow<List<PurchaseDetail>> = appDao.getPurchaseDetailsByPurchaseId(purchaseId)

    suspend fun insertPurchaseTransaction(purchase: Purchase, details: List<PurchaseDetail>) {
        val purchaseId = appDao.insertPurchase(purchase).toInt()

        for (detail in details) {
            val dbItem = appDao.getItemById(detail.itemId)
            if (dbItem != null) {
                // Increment stock & update purchasePrice
                val updatedStock = dbItem.stockQuantity + detail.quantity
                appDao.updateItem(
                    dbItem.copy(
                        stockQuantity = updatedStock,
                        purchasePrice = detail.unitPrice // Adapting price to current cost
                    )
                )
            }
            appDao.insertPurchaseDetail(detail.copy(purchaseId = purchaseId))
        }

        // --- ACCOUNTING AUTOMATIONS ---
        // Debit: Persediaan Barang
        appDao.insertJournalEntry(
            JournalEntry(
                date = purchase.date,
                description = "Pembelian Persediaan ${purchase.invoiceNumber}",
                accountName = "Persediaan Barang",
                debit = purchase.totalPrice,
                credit = 0.0,
                referenceId = purchase.invoiceNumber
            )
        )

        if (purchase.paymentType == "CASH") {
            // Credit: Kas & Bank
            appDao.insertJournalEntry(
                JournalEntry(
                    date = purchase.date,
                    description = "Pembelian Tunai ${purchase.invoiceNumber}",
                    accountName = "Kas & Bank",
                    debit = 0.0,
                    credit = purchase.totalPrice,
                    referenceId = purchase.invoiceNumber
                )
            )
        } else {
            // Credit: Utang Usaha
            appDao.insertJournalEntry(
                JournalEntry(
                    date = purchase.date,
                    description = "Pembelian Kredit ${purchase.invoiceNumber} - ${purchase.supplierName}",
                    accountName = "Utang Usaha",
                    debit = 0.0,
                    credit = purchase.totalPrice,
                    referenceId = purchase.invoiceNumber
                )
            )
            // Save Debt
            val cal = Calendar.getInstance()
            cal.timeInMillis = purchase.date
            cal.add(Calendar.DAY_OF_YEAR, 30) // Due in 30 days
            appDao.insertDebt(
                Debt(
                    purchaseId = purchaseId,
                    supplierId = purchase.supplierId,
                    supplierName = purchase.supplierName,
                    amount = purchase.totalPrice,
                    remainingAmount = purchase.totalPrice,
                    dueDate = cal.timeInMillis,
                    status = "BELUM_LUNAS"
                )
            )
        }
    }

    // Returns (Retur)
    val allReturns: Flow<List<ReturnTrans>> = appDao.getAllReturns()
    fun getReturnDetailsByReturnId(returnId: Int): Flow<List<ReturnDetail>> = appDao.getReturnDetailsByReturnId(returnId)

    suspend fun insertReturnTransaction(returnTrans: ReturnTrans, details: List<ReturnDetail>) {
        val returnId = appDao.insertReturn(returnTrans).toInt()
        var totalCostRefunded = 0.0

        for (detail in details) {
            val dbItem = appDao.getItemById(detail.itemId)
            if (dbItem != null) {
                if (returnTrans.type == "SALE_RETURN") {
                    // Item returned from customer -> Stock increases
                    val updatedStock = dbItem.stockQuantity + detail.quantity
                    appDao.updateItemStock(dbItem.id, updatedStock)
                    totalCostRefunded += (dbItem.purchasePrice * detail.quantity)
                } else {
                    // Item returned to supplier -> Stock decreases
                    val updatedStock = maxOf(0, dbItem.stockQuantity - detail.quantity)
                    appDao.updateItemStock(dbItem.id, updatedStock)
                    totalCostRefunded += (dbItem.purchasePrice * detail.quantity)
                }
            }
            appDao.insertReturnDetail(detail.copy(returnId = returnId))
        }

        // --- ACCOUNTING AUTOMATIONS FOR RETURN ---
        if (returnTrans.type == "SALE_RETURN") {
            // SALE RETURN (Retur Penjualan)
            // Debit: Pendapatan Penjualan (reducing revenue) with totalRefund
            appDao.insertJournalEntry(
                JournalEntry(
                    date = returnTrans.date,
                    description = "Retur Penjualan ${returnTrans.returnNumber}",
                    accountName = "Pendapatan Penjualan",
                    debit = returnTrans.totalRefund,
                    credit = 0.0,
                    referenceId = returnTrans.returnNumber
                )
            )
            // Credit: Kas & Bank with totalRefund
            appDao.insertJournalEntry(
                JournalEntry(
                    date = returnTrans.date,
                    description = "Refund Retur Penjualan ${returnTrans.returnNumber}",
                    accountName = "Kas & Bank",
                    debit = 0.0,
                    credit = returnTrans.totalRefund,
                    referenceId = returnTrans.returnNumber
                )
            )
            // Restore inventory asset value
            if (totalCostRefunded > 0) {
                // Debit: Persediaan Barang
                appDao.insertJournalEntry(
                    JournalEntry(
                        date = returnTrans.date,
                        description = "Restorasi Persediaan Retur ${returnTrans.returnNumber}",
                        accountName = "Persediaan Barang",
                        debit = totalCostRefunded,
                        credit = 0.0,
                        referenceId = returnTrans.returnNumber
                    )
                )
                // Credit: Harga Pokok Penjualan (reduction of HPP / expense)
                appDao.insertJournalEntry(
                    JournalEntry(
                        date = returnTrans.date,
                        description = "Pengurangan HPP Retur ${returnTrans.returnNumber}",
                        accountName = "Harga Pokok Penjualan (HPP)",
                        debit = 0.0,
                        credit = totalCostRefunded,
                        referenceId = returnTrans.returnNumber
                    )
                )
            }
        } else {
            // PURCHASE RETURN (Retur Pembelian)
            // Debit: Kas & Bank (refund from supplier) or reduce Utang, standard is Kas/refund received
            appDao.insertJournalEntry(
                JournalEntry(
                    date = returnTrans.date,
                    description = "Kas Refund Retur Pembelian ${returnTrans.returnNumber}",
                    accountName = "Kas & Bank",
                    debit = returnTrans.totalRefund,
                    credit = 0.0,
                    referenceId = returnTrans.returnNumber
                )
            )
            // Credit: Persediaan Barang (reducing inventory)
            appDao.insertJournalEntry(
                JournalEntry(
                    date = returnTrans.date,
                    description = "Pengurangan Persediaan Retur ${returnTrans.returnNumber}",
                    accountName = "Persediaan Barang",
                    debit = 0.0,
                    credit = returnTrans.totalRefund,
                    referenceId = returnTrans.returnNumber
                )
            )
        }
    }

    // Debts (Hutang)
    val allDebts: Flow<List<Debt>> = appDao.getAllDebts()

    suspend fun payDebt(debtId: Int, paidAmount: Double) {
        val allDebtsList = appDao.getAllDebts() // standard read is done on repository layer, let's write simple query or check state in viewmodel
        // Since we are updating, we need the actual debt object. To be fast, let's load or pass the completed entity.
    }

    suspend fun updateDebtStatus(debt: Debt, paidAmount: Double) {
        val updatedRemaining = maxOf(0.0, debt.remainingAmount - paidAmount)
        val status = if (updatedRemaining <= 0.0) "LUNAS" else "BELUM_LUNAS"
        appDao.updateDebt(
            debt.copy(
                remainingAmount = updatedRemaining,
                status = status
            )
        )

        // Journal:
        // Debit: Utang Usaha (debt accounts payable reduces)
        appDao.insertJournalEntry(
            JournalEntry(
                date = System.currentTimeMillis(),
                description = "Pelunasan Utang ke ${debt.supplierName}",
                accountName = "Utang Usaha",
                debit = paidAmount,
                credit = 0.0,
                referenceId = "PAY-DEBT-${debt.id}"
            )
        )
        // Credit: Kas & Bank
        appDao.insertJournalEntry(
            JournalEntry(
                date = System.currentTimeMillis(),
                description = "Kas Keluar Pelunasan Utang",
                accountName = "Kas & Bank",
                debit = 0.0,
                credit = paidAmount,
                referenceId = "PAY-DEBT-${debt.id}"
            )
        )
    }

    // Receivables (Piutang)
    val allReceivables: Flow<List<Receivable>> = appDao.getAllReceivables()

    suspend fun updateReceivableStatus(receivable: Receivable, receivedAmount: Double) {
        val updatedRemaining = maxOf(0.0, receivable.remainingAmount - receivedAmount)
        val status = if (updatedRemaining <= 0.0) "LUNAS" else "BELUM_LUNAS"
        appDao.updateReceivable(
            receivable.copy(
                remainingAmount = updatedRemaining,
                status = status
            )
        )

        // Journal:
        // Debit: Kas & Bank (cash flow increases)
        appDao.insertJournalEntry(
            JournalEntry(
                date = System.currentTimeMillis(),
                description = "Penerimaan Piutang dari ${receivable.customerName}",
                accountName = "Kas & Bank",
                debit = receivedAmount,
                credit = 0.0,
                referenceId = "COL-PIU-${receivable.id}"
            )
        )
        // Credit: Piutang Usaha (receivable asset reduces)
        appDao.insertJournalEntry(
            JournalEntry(
                date = System.currentTimeMillis(),
                description = "Pengurangan Piutang ${receivable.customerName}",
                accountName = "Piutang Usaha",
                debit = 0.0,
                credit = receivedAmount,
                referenceId = "COL-PIU-${receivable.id}"
            )
        )
    }

    // Journal Entries (Akuntansi)
    val allJournalEntries: Flow<List<JournalEntry>> = appDao.getAllJournalEntries()

    suspend fun insertJournalEntry(entry: JournalEntry) = appDao.insertJournalEntry(entry)
}
