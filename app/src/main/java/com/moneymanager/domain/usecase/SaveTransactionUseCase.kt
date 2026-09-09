package com.moneymanager.domain.usecase

import com.moneymanager.data.repository.TransactionRepository
import com.moneymanager.domain.model.Transaction
import javax.inject.Inject

class SaveTransactionUseCase @Inject constructor(
    private val txnRepo: TransactionRepository
) {
    /** Single write path for manual, OCR and PDF transactions. */
    suspend operator fun invoke(txn: Transaction) {
        val existing = txnRepo.getById(txn.id)
        val now = System.currentTimeMillis()
        txnRepo.save(txn.copy(
            createdAt = existing?.createdAt ?: txn.createdAt.takeIf { it > 0 } ?: now,
            updatedAt = now
        ))
    }
}
