package com.moneymanager.di

import android.content.Context
import androidx.room.Room
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
        Room.databaseBuilder(ctx, AppDatabase::class.java, "money_manager.db").build()

    @Provides fun provideFundDao(db: AppDatabase): FundDao = db.fundDao()
    @Provides fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()
    @Provides fun provideTransferDao(db: AppDatabase): TransferDao = db.transferDao()
    @Provides fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideImportBatchDao(db: AppDatabase): ImportBatchDao = db.importBatchDao()
}
