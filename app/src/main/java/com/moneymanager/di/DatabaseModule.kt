package com.moneymanager.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.moneymanager.data.db.AppDatabase
import com.moneymanager.data.db.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "money_manager.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE funds ADD COLUMN startingBalance REAL NOT NULL DEFAULT 0.0")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE transactions ADD COLUMN googleTransactionId TEXT NOT NULL DEFAULT ''")
            database.execSQL("ALTER TABLE transactions ADD COLUMN paymentApp TEXT NOT NULL DEFAULT ''")
            database.execSQL("ALTER TABLE transactions ADD COLUMN status TEXT NOT NULL DEFAULT ''")
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE funds ADD COLUMN sourceName TEXT NOT NULL DEFAULT ''")
            database.execSQL("ALTER TABLE funds ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE categories ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE categories ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE transactions ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE transactions ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE transfers ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE transfers ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            database.execSQL("UPDATE funds SET updatedAt = createdAt WHERE updatedAt = 0")
            database.execSQL("UPDATE transactions SET createdAt = date, updatedAt = date WHERE createdAt = 0")
            database.execSQL("UPDATE transfers SET createdAt = date, updatedAt = date WHERE createdAt = 0")
        }
    }

    @Provides fun provideFundDao(db: AppDatabase): FundDao = db.fundDao()
    @Provides fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()
    @Provides fun provideTransferDao(db: AppDatabase): TransferDao = db.transferDao()
    @Provides fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideImportBatchDao(db: AppDatabase): ImportBatchDao = db.importBatchDao()
}
