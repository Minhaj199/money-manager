package com.moneymanager.data.db.dao

import androidx.room.*
import com.moneymanager.data.db.entity.ImportBatchEntity

@Dao
interface ImportBatchDao {
    @Query("SELECT * FROM import_batches WHERE rawHash = :hash LIMIT 1")
    suspend fun findByHash(hash: String): ImportBatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(batch: ImportBatchEntity)
}
