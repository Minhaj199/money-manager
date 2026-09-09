package com.moneymanager.parsing

import com.moneymanager.domain.model.FieldConfidence
import com.moneymanager.domain.model.ParsedTransaction
import com.moneymanager.domain.model.TxnType
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Parses a receipt from ordered OCR lines, keeping labels attached to their values. */
@Singleton
class ScreenshotParser @Inject constructor() {

    // Matches ₹70, ₹ 70, ₹1,000, ₹1,000.50, Rs 70, Rs. 70, INR 70 — currency marker is mandatory.
    private val currencyAmount = Regex(
        """(?i)(?:₹|Rs\.?|INR)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)"""
    )

    private val upiRegex = Regex("""(?:[•*]{2,}\s*)?[A-Za-z0-9._-]+@[A-Za-z0-9._-]+""")

    private val dateTimeRegex = Regex(
        """\b(\d{1,2}\s+[A-Za-z]{3,9}\.?\s+\d{4})\s*,?\s*(\d{1,2}:\d{2}\s*(?:am|pm))\b""",
        RegexOption.IGNORE_CASE
    )

    // Lines that are Google Pay / payment-app UI chrome — never a transaction description.
    private val uiTextPatterns = listOf(
        Regex("""^pay again$""", RegexOption.IGNORE_CASE),
        Regex("""^completed$""", RegexOption.IGNORE_CASE),
        Regex("""^pending$""", RegexOption.IGNORE_CASE),
        Regex("""^failed$""", RegexOption.IGNORE_CASE),
        Regex("""^cancelled?$""", RegexOption.IGNORE_CASE),
        Regex("""^share$""", RegexOption.IGNORE_CASE),
        Regex("""^split expense$""", RegexOption.IGNORE_CASE),
        Regex("""^having issues\??$""", RegexOption.IGNORE_CASE),
        Regex("""^upi\s+(?:transaction\s+)?id""", RegexOption.IGNORE_CASE),
        Regex("""^google\s+transaction\s+id""", RegexOption.IGNORE_CASE),
        Regex("""^google\s+pay""", RegexOption.IGNORE_CASE),
        Regex("""^from\s*:""", RegexOption.IGNORE_CASE),
        Regex("""^to\s*:""", RegexOption.IGNORE_CASE),
        Regex("""^federal\s+bank""", RegexOption.IGNORE_CASE),
        Regex("""^upi$""", RegexOption.IGNORE_CASE),
        // Numeric-only lines (transaction IDs, account numbers)
        Regex("""^[0-9]{6,}$"""),
        // UPI IDs
        Regex("""[A-Za-z0-9._-]+@[A-Za-z0-9._-]+"""),
        // Lines that are purely dots/bullets (masked account numbers like ••••2025@fbl)
        Regex("""^[•*\s]+"""),
    )

    fun parse(rawText: String): ParsedTransaction {
        val lines = rawText.lineSequence()
            .map { normalizeLine(it) }
            .filter { it.isNotEmpty() }
            .toList()
        val text = lines.joinToString("\n")
        val lower = text.lowercase(Locale.ROOT)

        val amount = extractAmount(lines)

        val isExpense = lower.containsAny("paid", "sent", "debit", "payment successful", "completed")
        val isIncome = lower.containsAny("received", "credit", "money received")
        val type = if (isIncome && !isExpense) TxnType.INCOME else TxnType.EXPENSE

        val merchant = valueAfterLabel(lines, Regex("""^(?:paid to|sent to|payment to|to)\s*:?\s*""", RegexOption.IGNORE_CASE))
            .ifBlank { valueAfterLabel(lines, Regex("""^(?:received from|from)\s*:?\s*""", RegexOption.IGNORE_CASE)) }

        val description = valueAfterLabel(lines, Regex("""^description\s*:?\s*""", RegexOption.IGNORE_CASE))
            .ifBlank { descriptionNearAmount(lines) }

        val txnId = valueAfterLabel(lines, Regex("""^upi\s+(?:transaction\s+)?id\s*:?\s*""", RegexOption.IGNORE_CASE))
        val googleTransactionId = valueAfterLabel(lines, Regex("""^google\s+transaction\s+id\s*:?\s*""", RegexOption.IGNORE_CASE))

        val upiId = lines.asSequence()
            .mapNotNull { upiRegex.find(it)?.value }
            .map { it.replace(Regex("""^[•*]+\s*"""), "") }
            .firstOrNull().orEmpty()

        val paymentApp = when {
            lower.contains("google pay") -> "Google Pay"
            lower.contains("phonepe") -> "PhonePe"
            lower.contains("paytm") -> "Paytm"
            lower.contains("bhim") -> "BHIM"
            lower.contains("amazon pay") -> "Amazon Pay"
            else -> ""
        }

        val dateTime = dateTimeRegex.find(text)
        val date = dateTime?.groupValues?.get(1)?.let(::parseDate)
        val time = dateTime?.groupValues?.get(2)?.let(::to24HourTime).orEmpty()

        val status = lines.firstOrNull { it.equals("Completed", ignoreCase = true) }.orEmpty()
        val paymentMethod = if (lower.contains("upi")) "UPI" else ""

        val confidence = buildMap {
            if (amount != null) put("amount", FieldConfidence.HIGH)
            if (merchant.isNotBlank()) put("merchant", FieldConfidence.HIGH)
            if (description.isNotBlank()) put("description", FieldConfidence.HIGH)
            if (upiId.isNotBlank()) put("upiId", FieldConfidence.HIGH)
            if (txnId.isNotBlank()) put("transactionId", FieldConfidence.HIGH)
            if (googleTransactionId.isNotBlank()) put("googleTransactionId", FieldConfidence.HIGH)
            if (date != null) put("date", FieldConfidence.HIGH)
        }

        return ParsedTransaction(
            amount = amount, type = type, merchant = merchant, upiId = upiId,
            txnId = txnId, description = description, date = date, paymentApp = paymentApp,
            googleTransactionId = googleTransactionId, time = time,
            paymentMethod = paymentMethod, status = status, confidence = confidence
        )
    }

    // ── Amount ────────────────────────────────────────────────────────────────

    private fun extractAmount(lines: List<String>): Double? =
        lines.asSequence()
            .mapNotNull { currencyAmount.find(it)?.groupValues?.get(1) }
            .mapNotNull { it.replace(",", "").toDoubleOrNull() }
            .firstOrNull { it > 0 && it < 100_000_000 }

    // ── Description ───────────────────────────────────────────────────────────

    /**
     * Google Pay places the optional note directly below the large currency amount line.
     * We scan forward from the amount line and return the first line that looks like a
     * genuine user-entered note rather than app UI chrome or receipt metadata.
     */
    private fun descriptionNearAmount(lines: List<String>): String {
        val amountLine = lines.indexOfFirst { currencyAmount.containsMatchIn(it) }
        if (amountLine < 0) return ""
        return lines.drop(amountLine + 1)
            .firstOrNull { isValidDescription(it) }
            .orEmpty()
    }

    private fun isValidDescription(candidate: String): Boolean {
        if (candidate.isBlank() || candidate.length > 120) return false
        if (dateTimeRegex.containsMatchIn(candidate)) return false
        if (currencyAmount.containsMatchIn(candidate)) return false
        return uiTextPatterns.none { it.containsMatchIn(candidate) }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun normalizeLine(raw: String): String =
        raw.replace("\u20B9", "₹")   // normalize any variant rupee codepoints
            .replace("â‚¹", "₹")     // fix common UTF-8 mis-encoding
            .trim()
            .replace(Regex("""\s+"""), " ")

    private fun valueAfterLabel(lines: List<String>, label: Regex): String {
        val index = lines.indexOfFirst { label.containsMatchIn(it) }
        if (index < 0) return ""
        return lines[index].replace(label, "").trim()
            .ifBlank { lines.getOrNull(index + 1).orEmpty() }
    }

    private fun parseDate(value: String): Long? {
        // Normalise "Sept" → "Sep", "June" → "Jun", "July" → "Jul" so SimpleDateFormat parses them.
        val normalised = value
            .replace(Regex("""\bSept\b""", RegexOption.IGNORE_CASE), "Sep")
            .replace(Regex("""\bJune\b""", RegexOption.IGNORE_CASE), "Jun")
            .replace(Regex("""\bJuly\b""", RegexOption.IGNORE_CASE), "Jul")
            .replace(".", "")
            .trim()
        return listOf("d MMM yyyy", "d MMMM yyyy", "d/M/yyyy").firstNotNullOfOrNull { format ->
            runCatching {
                SimpleDateFormat(format, Locale.ENGLISH).apply { isLenient = false }.parse(normalised)?.time
            }.getOrNull()
        }
    }

    private fun to24HourTime(value: String): String = runCatching {
        val parsed = SimpleDateFormat("h:mm a", Locale.ENGLISH).parse(value.trim().uppercase(Locale.ROOT))
        if (parsed == null) "" else SimpleDateFormat("HH:mm", Locale.ENGLISH).format(parsed)
    }.getOrDefault("")

    private fun String.containsAny(vararg keywords: String) = keywords.any { contains(it) }
}
