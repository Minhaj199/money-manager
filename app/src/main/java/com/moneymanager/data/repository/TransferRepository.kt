package com.moneymanager.data.repository

import com.moneymanager.data.db.dao.TransferDao
import com.moneymanager.data.db.entity.TransferEntity
import com.moneymanager.domain.model.Transfer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferRepository @Inject constructor(private val dao: TransferDao) {

    fun observeAll(): Flow<List<Transfer>> = dao.observeAll().map { list ->
        list.map { Transfer(it.id, it.fromFundId, it.toFundId, it.amount, it.note, it.date) }
    }

    suspend fun save(transfer: Transfer) {
        val now = System.currentTimeMillis()
        val existing = dao.getById(transfer.id)
        dao.upsert(TransferEntity(
            id = transfer.id,
            fromFundId = transfer.fromFundId,
            toFundId = transfer.toFundId,
            amount = transfer.amount,
            note = transfer.note,
            date = transfer.date,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        ))
    }

    suspend fun deleteById(id: String) = dao.deleteById(id)
}
