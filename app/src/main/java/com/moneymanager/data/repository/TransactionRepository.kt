package com.moneymanager.data.repository

import com.moneymanager.data.db.dao.TransactionDao
import com.moneymanager.data.db.entity.TransactionEntity
import com.moneymanager.domain.model.Transaction
import com.moneymanager.domain.model.TxnSource
import com.moneymanager.domain.model.TxnType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(private val dao: TransactionDao) {

    fun observeAll(): Flow<List<Transaction>> = dao.observeAll().map { it.map(::toDomain) }
    fun observeByFund(fundId: String): Flow<List<Transaction>> = dao.observeByFund(fundId).map { it.map(::toDomain) }
    fun observeRecent(limit: Int = 20): Flow<List<Transaction>> = dao.observeRecent(limit).map { it.map(::toDomain) }

    suspend fun save(txn: Transaction) = dao.upsert(toEntity(txn))
    suspend fun saveAll(txns: List<Transaction>) = dao.upsertAll(txns.map(::toEntity))
    suspend fun delete(txn: Transaction) = dao.delete(toEntity(txn))

    suspend fun findDuplicates(txn: Transaction): List<Transaction> =
        dao.findPotentialDuplicates(txn.txnId, txn.amount, txn.merchant, txn.date).map(::toDomain)

    private fun toDomain(e: TransactionEntity) = Transaction(
        e.id, e.fundId, e.amount, TxnType.valueOf(e.type), e.categoryId,
        e.description, e.merchant, e.upiId, e.txnId, e.paymentMethod,
        TxnSource.valueOf(e.source), e.date, e.importBatchId
    )

    private fun toEntity(t: Transaction) = TransactionEntity(
        t.id, t.fundId, t.amount, t.type.name, t.categoryId,
        t.description, t.merchant, t.upiId, t.txnId, t.paymentMethod,
        t.source.name, t.date, t.importBatchId
    )
}
