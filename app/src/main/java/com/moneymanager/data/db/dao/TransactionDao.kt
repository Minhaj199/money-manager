package com.moneymanager.data.db.dao

import androidx.room.*
import com.moneymanager.data.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun getAll(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE fundId = :fundId ORDER BY date DESC")
    fun observeByFund(fundId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit")
    fun observeRecent(limit: Int = 20): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amount) FROM transactions WHERE fundId = :fundId AND type = 'INCOME'")
    suspend fun totalIncome(fundId: String): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE fundId = :fundId AND type = 'EXPENSE'")
    suspend fun totalExpense(fundId: String): Double?

    @Query("""
        SELECT * FROM transactions 
        WHERE (txnId != '' AND txnId = :txnId)
           OR (:merchant != '' AND ABS(amount - :amount) < 0.01 AND merchant = :merchant
               AND ABS(date - :date) < 86400000)
        LIMIT 5
    """)
    suspend fun findPotentialDuplicates(txnId: String, amount: Double, merchant: String, date: Long): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(txn: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(txns: List<TransactionEntity>)

    @Delete
    suspend fun delete(txn: TransactionEntity)

    @Query("DELETE FROM transactions WHERE importBatchId = :importBatchId")
    suspend fun deleteByImportBatchId(importBatchId: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
