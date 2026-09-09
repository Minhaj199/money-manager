package com.moneymanager.domain.model

/** Parsed result from OCR or PDF — not yet saved */
enum class FieldConfidence { HIGH, MEDIUM, LOW }

data class ParsedTransaction(
    val amount: Double?,
    val type: TxnType?,
    val merchant: String,
    val upiId: String,
    val txnId: String,
    val description: String,
    val date: Long?,
    val paymentApp: String,
    /** Provider reference; this is an identifier and must never be parsed as a number. */
    val googleTransactionId: String = "",
    /** Local receipt time in 24-hour HH:mm format. */
    val time: String = "",
    val paymentMethod: String = "",
    val status: String = "",
    val confidence: Map<String, FieldConfidence> = emptyMap()
)
