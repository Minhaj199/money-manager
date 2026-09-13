package com.moneymanager.domain.model

data class TransactionAllocation(
    val id: String,
    val transactionId: String,
    val fundId: String,
    val amount: Double
)
