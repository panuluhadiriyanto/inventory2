package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Categories
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    // Items
    @Query("SELECT * FROM items ORDER BY name ASC")
    fun getAllItems(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Int): Item?

    @Query("SELECT * FROM items WHERE skuBarcode = :skuBarcode LIMIT 1")
    suspend fun getItemByBarcode(skuBarcode: String): Item?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: Item)

    @Update
    suspend fun updateItem(item: Item)

    @Delete
    suspend fun deleteItem(item: Item)

    @Query("UPDATE items SET stockQuantity = :newStock WHERE id = :itemId")
    suspend fun updateItemStock(itemId: Int, newStock: Int)

    // Suppliers
    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<Supplier>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: Supplier)

    @Delete
    suspend fun deleteSupplier(supplier: Supplier)

    // Customers
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    // Sales (Transactions)
    @Query("SELECT * FROM sales ORDER BY date DESC")
    fun getAllSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sales WHERE id = :saleId LIMIT 1")
    suspend fun getSaleById(saleId: Int): Sale?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: Sale): Long

    @Query("SELECT * FROM sale_details WHERE saleId = :saleId")
    fun getSaleDetailsBySaleId(saleId: Int): Flow<List<SaleDetail>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleDetail(detail: SaleDetail)

    // Purchases (Transactions)
    @Query("SELECT * FROM purchases ORDER BY date DESC")
    fun getAllPurchases(): Flow<List<Purchase>>

    @Query("SELECT * FROM purchases WHERE id = :purchaseId LIMIT 1")
    suspend fun getPurchaseById(purchaseId: Int): Purchase?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: Purchase): Long

    @Query("SELECT * FROM purchase_details WHERE purchaseId = :purchaseId")
    fun getPurchaseDetailsByPurchaseId(purchaseId: Int): Flow<List<PurchaseDetail>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseDetail(detail: PurchaseDetail)

    // Returns
    @Query("SELECT * FROM returns ORDER BY date DESC")
    fun getAllReturns(): Flow<List<ReturnTrans>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturn(returnTrans: ReturnTrans): Long

    @Query("SELECT * FROM return_details WHERE returnId = :returnId")
    fun getReturnDetailsByReturnId(returnId: Int): Flow<List<ReturnDetail>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReturnDetail(detail: ReturnDetail)

    // Debts (Hutang)
    @Query("SELECT * FROM debts ORDER BY dueDate ASC")
    fun getAllDebts(): Flow<List<Debt>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: Debt)

    @Update
    suspend fun updateDebt(debt: Debt)

    @Delete
    suspend fun deleteDebt(debt: Debt)

    // Receivables (Piutang)
    @Query("SELECT * FROM receivables ORDER BY dueDate ASC")
    fun getAllReceivables(): Flow<List<Receivable>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceivable(receivable: Receivable)

    @Update
    suspend fun updateReceivable(receivable: Receivable)

    @Delete
    suspend fun deleteReceivable(receivable: Receivable)

    // Journal Entries (Akuntansi)
    @Query("SELECT * FROM journal_entries ORDER BY date DESC, id DESC")
    fun getAllJournalEntries(): Flow<List<JournalEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntry(entry: JournalEntry)

    @Query("DELETE FROM journal_entries WHERE referenceId = :refId")
    suspend fun deleteJournalEntriesByRef(refId: String)

    // Warehouses
    @Query("SELECT * FROM warehouses ORDER BY name ASC")
    fun getAllWarehouses(): Flow<List<Warehouse>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarehouse(warehouse: Warehouse): Long

    @Delete
    suspend fun deleteWarehouse(warehouse: Warehouse)

    // Warehouse Stocks
    @Query("SELECT * FROM warehouse_stocks")
    fun getAllWarehouseStocks(): Flow<List<WarehouseStock>>

    @Query("SELECT * FROM warehouse_stocks WHERE warehouseId = :warehouseId")
    fun getStocksByWarehouseId(warehouseId: Int): Flow<List<WarehouseStock>>

    @Query("SELECT * FROM warehouse_stocks WHERE itemId = :itemId")
    suspend fun getStocksByItemId(itemId: Int): List<WarehouseStock>

    @Query("SELECT * FROM warehouse_stocks WHERE warehouseId = :warehouseId AND itemId = :itemId LIMIT 1")
    suspend fun getWarehouseStock(warehouseId: Int, itemId: Int): WarehouseStock?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarehouseStock(stock: WarehouseStock)

    @Query("DELETE FROM warehouse_stocks WHERE warehouseId = :warehouseId AND itemId = :itemId")
    suspend fun deleteWarehouseStock(warehouseId: Int, itemId: Int)

    // Stock Transfers
    @Query("SELECT * FROM stock_transfers ORDER BY date DESC")
    fun getAllStockTransfers(): Flow<List<StockTransfer>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockTransfer(transfer: StockTransfer): Long
}
