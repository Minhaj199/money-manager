package com.moneymanager.domain.usecase

import androidx.room.withTransaction
import com.moneymanager.data.db.AppDatabase
import com.moneymanager.data.repository.TransactionRepository
import com.moneymanager.data.repository.TransferRepository
import com.moneymanager.domain.model.Transaction
import com.moneymanager.domain.model.TxnSource
import javax.inject.Inject

/** Updates or deletes a ledger entry atomically. Fund balances are derived from the ledger. */
class ManageTransactionUseCase @Inject constructor(
    private val database: AppDatabase,
    private val transactions: TransactionRepository,
    private val transfers: TransferRepository,
    private val saveTransaction: SaveTransactionUseCase
) {
    suspend fun update(transaction: Transaction) {
        require(transaction.source != TxnSource.TRANSFER) { "Transfers must be changed as transfers" }
        database.withTransaction { saveTransaction(transaction) }
    }

    suspend fun delete(transactionId: String) = database.withTransaction {
        val transaction = transactions.getById(transactionId) ?: return@withTransaction
        if (transaction.source == TxnSource.TRANSFER && transaction.importBatchId.isNotBlank()) {
            // Both offsetting entries share the transfer id, so reversal cannot leave one side behind.
            transactions.deleteByTransferId(transaction.importBatchId)
            transfers.deleteById(transaction.importBatchId)
        } else {
            transactions.delete(transaction)
        }
    }
}
