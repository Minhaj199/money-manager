package com.moneymanager.data.db.dao

import androidx.room.*
import com.moneymanager.data.db.entity.TransferEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {
    @Query("SELECT * FROM transfers ORDER BY date DESC")
    fun observeAll(): Flow<List<TransferEntity>>

    @Query("SELECT * FROM transfers ORDER BY date DESC")
    suspend fun getAll(): List<TransferEntity>

    @Query("SELECT * FROM transfers WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): TransferEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(transfer: TransferEntity)

    @Query("DELETE FROM transfers WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM transfers")
    suspend fun deleteAll()
}
