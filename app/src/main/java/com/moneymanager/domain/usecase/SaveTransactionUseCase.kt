package com.moneymanager.domain.usecase

import com.moneymanager.data.repository.TransactionRepository
import com.moneymanager.domain.model.Transaction
import javax.inject.Inject

class SaveTransactionUseCase @Inject constructor(
    private val txnRepo: TransactionRepository
) {
    suspend operator fun invoke(txn: Transaction) = txnRepo.save(txn)
}
