package com.moneymanager.data.repository

import com.moneymanager.data.db.dao.FundDao
import com.moneymanager.data.db.entity.FundEntity
import com.moneymanager.domain.model.Fund
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FundRepository @Inject constructor(
    private val fundDao: FundDao,
    private val txnRepo: TransactionRepository
) {
    fun observeFunds(): Flow<List<Fund>> =
        combine(fundDao.observeAll(), txnRepo.observeAll()) { entities, txns ->
            entities.map { e ->
                val income = txns.filter { it.fundId == e.id && it.type.name == "INCOME" }.sumOf { it.amount }
                val expense = txns.filter { it.fundId == e.id && it.type.name == "EXPENSE" }.sumOf { it.amount }
                Fund(e.id, e.name, e.icon, e.colorHex, e.startingBalance, income, expense)
            }
        }

    suspend fun upsert(fund: FundEntity) = fundDao.upsert(fund)
    suspend fun delete(fund: FundEntity) = fundDao.delete(fund)
    suspend fun getById(id: String) = fundDao.getById(id)
}
