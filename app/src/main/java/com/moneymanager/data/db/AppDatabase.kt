package com.moneymanager.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.moneymanager.data.db.dao.*
import com.moneymanager.data.db.entity.*

@Database(
    entities = [
        FundEntity::class,
        TransactionEntity::class,
        TransferEntity::class,
        CategoryEntity::class,
        ImportBatchEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fundDao(): FundDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transferDao(): TransferDao
    abstract fun categoryDao(): CategoryDao
    abstract fun importBatchDao(): ImportBatchDao
}
