package com.moneymanager.domain.usecase

import com.moneymanager.data.repository.TransactionRepository
import com.moneymanager.data.repository.TransferRepository
import com.moneymanager.domain.model.Transaction
import com.moneymanager.domain.model.Transfer
import com.moneymanager.domain.model.TxnSource
import com.moneymanager.domain.model.TxnType
import java.util.UUID
import javax.inject.Inject

/**
 * Executes a fund transfer by recording two offsetting transactions
 * (EXPENSE on source fund, INCOME on destination fund) plus a Transfer record.
 * Transfers are excluded from analytics by their source = TRANSFER (handled in queries).
 */
class TransferFundsUseCase @Inject constructor(
    private val txnRepo: TransactionRepository,
    private val transferRepo: TransferRepository
) {
    suspend operator fun invoke(transfer: Transfer) {
        val now = System.currentTimeMillis()
        txnRepo.save(
            Transaction(
                id = UUID.randomUUID().toString(),
                fundId = transfer.fromFundId,
                amount = transfer.amount,
                type = TxnType.EXPENSE,
                categoryId = "transfer",
                description = "Transfer out: ${transfer.note}",
                merchant = "",
                source = TxnSource.TRANSFER,
                date = now,
                importBatchId = transfer.id
            )
        )
        txnRepo.save(
            Transaction(
                id = UUID.randomUUID().toString(),
                fundId = transfer.toFundId,
                amount = transfer.amount,
                type = TxnType.INCOME,
                categoryId = "transfer",
                description = "Transfer in: ${transfer.note}",
                merchant = "",
                source = TxnSource.TRANSFER,
                date = now,
                importBatchId = transfer.id
            )
        )
        transferRepo.save(transfer)
    }
}
