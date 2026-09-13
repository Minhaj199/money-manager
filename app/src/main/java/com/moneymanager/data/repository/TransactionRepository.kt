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
    suspend fun getById(id: String): Transaction? = dao.getById(id)?.let(::toDomain)
    suspend fun deleteByTransferId(transferId: String) = dao.deleteByImportBatchId(transferId)

    suspend fun findDuplicates(txn: Transaction): List<Transaction> =
        dao.findPotentialDuplicates(txn.txnId, txn.amount, txn.merchant, txn.date).map(::toDomain)

    suspend fun fundBalance(fundId: String, startingBalance: Double): Double {
        val income = dao.totalIncome(fundId) ?: 0.0
        val expense = dao.totalExpense(fundId) ?: 0.0
        return startingBalance + income - expense
    }

    private fun toDomain(e: TransactionEntity) = Transaction(
        id = e.id, fundId = e.fundId, amount = e.amount, type = TxnType.valueOf(e.type),
        categoryId = e.categoryId, description = e.description, merchant = e.merchant,
        upiId = e.upiId, txnId = e.txnId, paymentMethod = e.paymentMethod,
        googleTransactionId = e.googleTransactionId, paymentApp = e.paymentApp,
        status = e.status, source = TxnSource.valueOf(e.source), date = e.date,
        importBatchId = e.importBatchId, createdAt = e.createdAt, updatedAt = e.updatedAt
    )

    private fun toEntity(t: Transaction) = TransactionEntity(
        id = t.id, fundId = t.fundId, amount = t.amount, type = t.type.name,
        categoryId = t.categoryId, description = t.description, merchant = t.merchant,
        upiId = t.upiId, txnId = t.txnId, paymentMethod = t.paymentMethod,
        googleTransactionId = t.googleTransactionId, paymentApp = t.paymentApp,
        status = t.status, source = t.source.name, date = t.date, importBatchId = t.importBatchId,
        createdAt = t.createdAt, updatedAt = t.updatedAt
    )
}
