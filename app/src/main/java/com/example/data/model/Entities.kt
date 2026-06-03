package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)

@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val categoryId: Int,
    val skuBarcode: String,
    val purchasePrice: Double,
    val sellingPrice: Double,
    val stockQuantity: Int,
    val minStockAlert: Int = 5
)

@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val address: String
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val address: String
)

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val invoiceNumber: String,
    val customerId: Int?,
    val customerName: String,
    val date: Long,
    val subtotal: Double,
    val discount: Double,
    val totalPrice: Double,
    val paymentType: String, // "CASH", "CREDIT"
    val amountPaid: Double,
    val changeAmount: Double
)

@Entity(tableName = "sale_details")
data class SaleDetail(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val saleId: Int,
    val itemId: Int,
    val itemName: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double
)

@Entity(tableName = "purchases")
data class Purchase(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val invoiceNumber: String,
    val supplierId: Int,
    val supplierName: String,
    val date: Long,
    val totalPrice: Double,
    val paymentType: String, // "CASH", "CREDIT"
    val amountPaid: Double
)

@Entity(tableName = "purchase_details")
data class PurchaseDetail(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val purchaseId: Int,
    val itemId: Int,
    val itemName: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double
)

@Entity(tableName = "returns")
data class ReturnTrans(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val returnNumber: String,
    val date: Long,
    val type: String, // "SALE_RETURN", "PURCHASE_RETURN"
    val referenceInvoiceNumber: String,
    val totalRefund: Double,
    val reason: String
)

@Entity(tableName = "return_details")
data class ReturnDetail(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val returnId: Int,
    val itemId: Int,
    val itemName: String,
    val quantity: Int,
    val refundPrice: Double,
    val totalPrice: Double
)

@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val purchaseId: Int?,
    val supplierId: Int,
    val supplierName: String,
    val amount: Double,
    val remainingAmount: Double,
    val dueDate: Long,
    val date: Long = System.currentTimeMillis(),
    val status: String // "BELUM_LUNAS", "LUNAS"
)

@Entity(tableName = "receivables")
data class Receivable(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val saleId: Int?,
    val customerId: Int,
    val customerName: String,
    val amount: Double,
    val remainingAmount: Double,
    val dueDate: Long,
    val date: Long = System.currentTimeMillis(),
    val status: String // "BELUM_LUNAS", "LUNAS"
)

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long,
    val description: String,
    val accountName: String, // "Kas & Bank", "Persediaan Barang", "Piutang Usaha", "Utang Usaha", "Pendapatan Penjualan", "Harga Pokok Penjualan (HPP)", "Modal"
    val debit: Double,
    val credit: Double,
    val referenceId: String
)
