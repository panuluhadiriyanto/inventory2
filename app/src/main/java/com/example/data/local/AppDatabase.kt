package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*

@Database(
    entities = [
        Category::class,
        Item::class,
        Supplier::class,
        Customer::class,
        Sale::class,
        SaleDetail::class,
        Purchase::class,
        PurchaseDetail::class,
        ReturnTrans::class,
        ReturnDetail::class,
        Debt::class,
        Receivable::class,
        JournalEntry::class,
        Warehouse::class,
        WarehouseStock::class,
        StockTransfer::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finventory_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
