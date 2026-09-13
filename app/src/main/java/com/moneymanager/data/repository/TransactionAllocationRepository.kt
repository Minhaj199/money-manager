package com.moneymanager.data.repository

import com.moneymanager.data.db.dao.TransactionAllocationDao
import com.moneymanager.data.db.entity.TransactionAllocationEntity
import com.moneymanager.domain.model.TransactionAllocation
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionAllocationRepository @Inject constructor(
    private val dao: TransactionAllocationDao
) {
    suspend fun getByTransactionId(transactionId: String): List<TransactionAllocation> {
        return dao.getByTransactionId(transactionId).map { it.toDomain() }
    }

    suspend fun saveAll(allocations: List<TransactionAllocation>) {
        dao.upsertAll(allocations.map { it.toEntity() })
    }

    suspend fun deleteByTransactionId(transactionId: String) {
        dao.deleteByTransactionId(transactionId)
    }

    private fun TransactionAllocationEntity.toDomain() = TransactionAllocation(
        id = id,
        transactionId = transactionId,
        fundId = fundId,
        amount = amount
    )

    private fun TransactionAllocation.toEntity() = TransactionAllocationEntity(
        id = id,
        transactionId = transactionId,
        fundId = fundId,
        amount = amount
    )
}
