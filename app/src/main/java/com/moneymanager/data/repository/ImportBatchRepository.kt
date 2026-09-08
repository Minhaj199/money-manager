package com.moneymanager.data.repository

import com.moneymanager.data.db.dao.ImportBatchDao
import com.moneymanager.data.db.entity.ImportBatchEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImportBatchRepository @Inject constructor(private val dao: ImportBatchDao) {
    suspend fun findByHash(hash: String) = dao.findByHash(hash)
    suspend fun insert(batch: ImportBatchEntity) = dao.insert(batch)
}
