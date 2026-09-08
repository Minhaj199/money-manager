package com.moneymanager.domain.model

import java.time.Instant

enum class TxnType { INCOME, EXPENSE }
enum class TxnSource { MANUAL, SCREENSHOT, PDF, TRANSFER }

data class Transaction(
    val id: String,
    val fundId: String,
    val amount: Double,
    val type: TxnType,
    val categoryId: String,
    val description: String,
    val merchant: String,
    val upiId: String = "",
    val txnId: String = "",
    val paymentMethod: String = "",
    val source: TxnSource = TxnSource.MANUAL,
    val date: Long = Instant.now().toEpochMilli(),
    val importBatchId: String = ""
)
