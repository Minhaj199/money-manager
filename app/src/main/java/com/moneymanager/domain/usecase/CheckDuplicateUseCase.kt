package com.moneymanager.domain.usecase

import com.moneymanager.data.repository.TransactionRepository
import com.moneymanager.domain.model.Transaction
import javax.inject.Inject

data class DuplicateCheckResult(
    val isDuplicate: Boolean,
    val matches: List<Transaction>
)

class CheckDuplicateUseCase @Inject constructor(
    private val txnRepo: TransactionRepository
) {
    suspend operator fun invoke(txn: Transaction): DuplicateCheckResult {
        val matches = txnRepo.findDuplicates(txn)
        return DuplicateCheckResult(matches.isNotEmpty(), matches)
    }
}
