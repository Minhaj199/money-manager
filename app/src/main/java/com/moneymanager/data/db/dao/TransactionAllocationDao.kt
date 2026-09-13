package com.moneymanager.data.db.dao

import androidx.room.*
import com.moneymanager.data.db.entity.TransactionAllocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionAllocationDao {

    @Query("SELECT * FROM transaction_allocations WHERE transactionId = :transactionId")
    suspend fun getByTransactionId(transactionId: String): List<TransactionAllocationEntity>

    /** Returns all allocations, joined for fund-level balance aggregation. */
    @Query("SELECT * FROM transaction_allocations")
    fun observeAll(): Flow<List<TransactionAllocationEntity>>

    @Query("SELECT * FROM transaction_allocations WHERE fundId = :fundId")
    fun observeByFund(fundId: String): Flow<List<TransactionAllocationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(allocations: List<TransactionAllocationEntity>)

    @Query("DELETE FROM transaction_allocations WHERE transactionId = :transactionId")
    suspend fun deleteByTransactionId(transactionId: String)

    @Query("DELETE FROM transaction_allocations WHERE transactionId IN (:transactionIds)")
    suspend fun deleteByTransactionIds(transactionIds: List<String>)

    @Query("DELETE FROM transaction_allocations")
    suspend fun deleteAll()
}
