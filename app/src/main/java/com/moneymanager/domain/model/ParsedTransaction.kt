package com.moneymanager.domain.model

/** Parsed result from OCR or PDF — not yet saved */
data class ParsedTransaction(
    val amount: Double?,
    val type: TxnType?,
    val merchant: String,
    val upiId: String,
    val txnId: String,
    val description: String,
    val date: Long?,
    val paymentApp: String
)
