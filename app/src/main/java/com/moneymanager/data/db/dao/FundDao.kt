package com.moneymanager.data.db.dao

import androidx.room.*
import com.moneymanager.data.db.entity.FundEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FundDao {
    @Query("SELECT * FROM funds ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<FundEntity>>

    @Query("SELECT * FROM funds WHERE id = :id")
    suspend fun getById(id: String): FundEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(fund: FundEntity)

    @Delete
    suspend fun delete(fund: FundEntity)
}
