package com.moneymanager.parsing

import com.moneymanager.domain.model.ParsedTransaction
import com.moneymanager.domain.model.TxnType
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses raw OCR text from a payment screenshot into a ParsedTransaction.
 * Handles common UPI apps: GPay, PhonePe, Paytm, BHIM, Amazon Pay.
 */
@Singleton
class ScreenshotParser @Inject constructor() {

    private val amountRegex = Regex("""[₹Rs.]*\s*([\d,]+(?:\.\d{1,2})?)""")
    private val upiRegex = Regex("""[\w.\-]+@[\w]+""")
    private val txnIdRegex = Regex("""(?:UPI Ref|Txn ID|Transaction ID|Ref No)[:\s#]*([A-Z0-9]{8,})""", RegexOption.IGNORE_CASE)
    private val paidToRegex = Regex("""(?:Paid to|Sent to|Payment to|To)\s+([A-Za-z ]+)""", RegexOption.IGNORE_CASE)
    private val receivedFromRegex = Regex("""(?:Received from|From)\s+([A-Za-z ]+)""", RegexOption.IGNORE_CASE)
    private val dateRegex = Regex("""(\d{1,2}[\s/-][A-Za-z]{3,9}[\s/-]\d{2,4}|\d{1,2}/\d{1,2}/\d{2,4})""")

    private val paymentApps = listOf("gpay", "google pay", "phonepe", "paytm", "bhim", "amazon pay", "cred")

    fun parse(rawText: String): ParsedTransaction {
        val text = rawText.trim()
        val lower = text.lowercase()

        val amount = amountRegex.findAll(text)
            .mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }
            .filter { it > 0 }
            .maxOrNull()

        val isExpense = lower.containsAny("paid", "sent", "debit", "payment successful", "money sent")
        val isIncome = lower.containsAny("received", "credit", "money received")
        val type = when {
            isIncome && !isExpense -> TxnType.INCOME
            else -> TxnType.EXPENSE
        }

        val merchant = paidToRegex.find(text)?.groupValues?.get(1)?.trim()
            ?: receivedFromRegex.find(text)?.groupValues?.get(1)?.trim()
            ?: ""

        val upiId = upiRegex.find(text)?.value ?: ""
        val txnId = txnIdRegex.find(text)?.groupValues?.get(1) ?: ""
        val paymentApp = paymentApps.firstOrNull { lower.contains(it) } ?: ""

        val dateStr = dateRegex.find(text)?.value
        val date = dateStr?.let { parseDate(it) }

        val description = buildDescription(merchant, upiId, paymentApp)

        return ParsedTransaction(amount, type, merchant, upiId, txnId, description, date, paymentApp)
    }

    private fun buildDescription(merchant: String, upiId: String, app: String): String {
        val parts = listOfNotNull(
            merchant.takeIf { it.isNotBlank() }?.let { "Payment to $it" },
            upiId.takeIf { it.isNotBlank() },
            app.takeIf { it.isNotBlank() }?.replaceFirstChar { it.uppercase() }
        )
        return parts.firstOrNull() ?: "Payment"
    }

    private fun parseDate(str: String): Long? {
        val formats = listOf("d MMM yyyy", "d/M/yyyy", "d-MMM-yyyy", "d MMM yy", "d/M/yy")
        for (fmt in formats) {
            try {
                return SimpleDateFormat(fmt, Locale.ENGLISH).parse(str)?.time
            } catch (_: Exception) {}
        }
        return null
    }

    private fun String.containsAny(vararg keywords: String) = keywords.any { this.contains(it) }
}
