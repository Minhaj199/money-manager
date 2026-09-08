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

    suspend fun save(transfer: Transfer) = dao.upsert(
        TransferEntity(transfer.id, transfer.fromFundId, transfer.toFundId, transfer.amount, transfer.note, transfer.date)
    )
}
