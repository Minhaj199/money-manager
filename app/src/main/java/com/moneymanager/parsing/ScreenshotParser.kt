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

    // Matches ₹70, ₹ 70, ₹1,000, ₹1,000.50, Rs 70, Rs. 70, INR 70 — currency marker present.
    private val currencyAmount = Regex(
        """(?i)(?:₹|Rs\.?|INR)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)"""
    )

    // Bare number: 70, 70.00, 1,000, 1,000.50 — used only when currency symbol was lost by OCR.
    // Anchored to the full line to avoid matching substrings of IDs or dates.
    private val bareAmount = Regex("""^([0-9]{1,7}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)$""")

    // Google transaction IDs are Base64url-like: mixed-case letters + digits, 10–30 chars,
    // no spaces, no @ symbol. e.g. CICAgPiluMSaTA
    private val googleTxnIdPattern = Regex("""^[A-Za-z0-9_\-]{10,30}$""")

    private val upiRegex = Regex("""(?:[•*]{2,}\s*)?[A-Za-z0-9._-]+@[A-Za-z0-9._-]+""")

    private val dateTimeRegex = Regex(
        """\b(\d{1,2}\s+[A-Za-z]{3,9}\.?\s+\d{4})\s*,?\s*(\d{1,2}:\d{2}\s*(?:am|pm))\b""",
        RegexOption.IGNORE_CASE
    )

    // Lines that are payment-app UI chrome — never a transaction description.
    private val uiTextPatterns = listOf(
        Regex("""^pay again$""", RegexOption.IGNORE_CASE),
        Regex("""^completed$""", RegexOption.IGNORE_CASE),
        Regex("""^pending$""", RegexOption.IGNORE_CASE),
        Regex("""^failed$""", RegexOption.IGNORE_CASE),
        Regex("""^cancell?ed$""", RegexOption.IGNORE_CASE),
        Regex("""^share$""", RegexOption.IGNORE_CASE),
        Regex("""^split expense$""", RegexOption.IGNORE_CASE),
        Regex("""^having issues\??$""", RegexOption.IGNORE_CASE),
        Regex("""^upi\s+(?:transaction\s+)?id""", RegexOption.IGNORE_CASE),
        Regex("""^google\s+transaction\s+id""", RegexOption.IGNORE_CASE),
        Regex("""^google\s*pay""", RegexOption.IGNORE_CASE),
        Regex("""^gpay$""", RegexOption.IGNORE_CASE),
        Regex("""^from\s*:""", RegexOption.IGNORE_CASE),
        Regex("""^to\s*:""", RegexOption.IGNORE_CASE),
        Regex("""^powered\s+by""", RegexOption.IGNORE_CASE),
        Regex("""^powe$""", RegexOption.IGNORE_CASE),
        Regex("""^by$""", RegexOption.IGNORE_CASE),
        Regex("""^unified\s+payments""", RegexOption.IGNORE_CASE),
        Regex("""^upi$""", RegexOption.IGNORE_CASE),
        // Bank/account metadata: "Federal Bank 9340", "HDFC Bank 1234", etc.
        Regex("""(?i)\b(?:federal|hdfc|icici|sbi|axis|kotak|yes|idbi|pnb|bob|canara)\s+bank\b"""),
        // Numeric-only lines with 6+ digits (transaction IDs, account numbers)
        Regex("""^[0-9]{6,}$"""),
        // UPI IDs
        Regex("""[A-Za-z0-9._-]+@[A-Za-z0-9._-]+"""),
        // Purely dots/bullets (masked account numbers like ••••2025@fbl)
        Regex("""^[•*\s]+"""),
        // Single characters (OCR noise like "M")
        Regex("""^[A-Za-z]$"""),
    )

    fun parse(rawText: String): ParsedTransaction {
        val lines = rawText.lineSequence()
            .map { normalizeLine(it) }
            .filter { it.isNotEmpty() }
            .toList()

        val text = lines.joinToString("\n")
        val lower = text.lowercase(Locale.ROOT)

        // Detect provider early — gates contextual extraction strategies.
        val isGooglePay = lower.containsAny("google pay", "gpay", "google transaction id")

        // Pre-identify known IDs so they are never mistaken for amounts.
        val knownTxnId = extractTxnId(lines)
        val knownGoogleTxnId = extractGoogleTransactionId(lines, isGooglePay)

        val amount = extractAmount(lines, knownTxnId, knownGoogleTxnId, isGooglePay)

        val isExpense = lower.containsAny("paid", "sent", "debit", "payment successful", "completed")
        val isIncome = lower.containsAny("received", "credit", "money received")
        val type = if (isIncome && !isExpense) TxnType.INCOME else TxnType.EXPENSE

        val merchant = valueAfterLabel(lines, Regex("""^(?:paid to|sent to|payment to|to)\s*:?\s*""", RegexOption.IGNORE_CASE))
            .ifBlank { valueAfterLabel(lines, Regex("""^(?:received from|from)\s*:?\s*""", RegexOption.IGNORE_CASE)) }

        val description = valueAfterLabel(lines, Regex("""^(?:description|note|message)\s*:?\s*""", RegexOption.IGNORE_CASE))
            .ifBlank { descriptionNearAmount(lines, knownTxnId, knownGoogleTxnId) }

        val upiId = lines.asSequence()
            .mapNotNull { upiRegex.find(it)?.value }
            .map { it.replace(Regex("""^[•*]+\s*"""), "") }
            .firstOrNull().orEmpty()

        val paymentApp = when {
            lower.containsAny("google pay", "gpay") -> "Google Pay"
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
            if (knownTxnId.isNotBlank()) put("transactionId", FieldConfidence.HIGH)
            if (knownGoogleTxnId.isNotBlank()) put("googleTransactionId", FieldConfidence.HIGH)
            if (date != null) put("date", FieldConfidence.HIGH)
        }

        return ParsedTransaction(
            amount = amount, type = type, merchant = merchant, upiId = upiId,
            txnId = knownTxnId, description = description, date = date, paymentApp = paymentApp,
            googleTransactionId = knownGoogleTxnId, time = time,
            paymentMethod = paymentMethod, status = status, confidence = confidence
        )
    }

    // ── Amount ────────────────────────────────────────────────────────────────

    private fun extractAmount(
        lines: List<String>,
        knownTxnId: String,
        knownGoogleTxnId: String,
        isGooglePay: Boolean
    ): Double? {
        // Pass 1: explicit currency prefix — highest confidence, always preferred.
        val withSymbol = lines.asSequence()
            .mapNotNull { line ->
                val match = currencyAmount.find(line) ?: return@mapNotNull null
                val value = match.groupValues[1].replace(",", "").toDoubleOrNull() ?: return@mapNotNull null
                if (value <= 0 || value >= 100_000_000) null else value
            }
            .firstOrNull()
        if (withSymbol != null) return withSymbol

        // Pass 2: bare number — only for Google Pay receipts where OCR drops the currency symbol.
        if (!isGooglePay) return null

        return lines.asSequence()
            .mapNotNull { line ->
                val match = bareAmount.find(line) ?: return@mapNotNull null
                val raw = match.groupValues[1]
                val value = raw.replace(",", "").toDoubleOrNull() ?: return@mapNotNull null
                if (line == knownTxnId || line == knownGoogleTxnId) return@mapNotNull null
                if (value <= 0 || value >= 10_000_000) return@mapNotNull null
                // Reject 4-digit calendar years.
                if (value >= 2000 && value <= 2100 && raw.length == 4) return@mapNotNull null
                // Reject lines that are part of a date/time expression.
                if (dateTimeRegex.containsMatchIn(line)) return@mapNotNull null
                // Reject 4-digit bank account suffixes like 9340 but not round thousands like 1000.
                if (value >= 1000 && value <= 9999 && raw.length == 4
                    && !raw.contains(",") && !raw.contains(".")
                    && value.toLong() % 1000 != 0L) return@mapNotNull null
                value
            }
            .firstOrNull()
    }

    // ── Transaction ID ────────────────────────────────────────────────────────

    private fun extractTxnId(lines: List<String>): String =
        valueAfterLabel(lines, Regex("""^upi\s+(?:transaction\s+)?id\s*:?\s*""", RegexOption.IGNORE_CASE))

    // ── Google Transaction ID ─────────────────────────────────────────────────

    /**
     * The Google transaction ID is a Base64url-like token (e.g. CICAgPiluMSaTA).
     * OCR ordering is unreliable — the token may appear BEFORE the "Google transaction ID"
     * label line. Strategy:
     *   1. Scan ALL lines for a token matching the pattern.
     *   2. Exclude lines that are obviously something else (UPI IDs, all-digit IDs, UI text).
     *   3. If the label is present, prefer the candidate nearest to it; otherwise take the first.
     */
    private fun extractGoogleTransactionId(lines: List<String>, isGooglePay: Boolean): String {
        if (!isGooglePay) {
            return valueAfterLabel(lines, Regex("""^google\s+transaction\s+id\s*:?\s*""", RegexOption.IGNORE_CASE))
        }

        val labelIndex = lines.indexOfFirst {
            Regex("""^google\s+transaction\s+id""", RegexOption.IGNORE_CASE).containsMatchIn(it)
        }

        val candidates = lines.mapIndexedNotNull { idx, line ->
            if (!googleTxnIdPattern.matches(line)) return@mapIndexedNotNull null
            if (line.all { it.isDigit() }) return@mapIndexedNotNull null   // pure numeric = txnId
            if (line.contains('@')) return@mapIndexedNotNull null           // UPI ID
            if (uiTextPatterns.any { it.containsMatchIn(line) }) return@mapIndexedNotNull null
            idx to line
        }

        if (candidates.isEmpty()) return ""

        return if (labelIndex >= 0) {
            candidates.minByOrNull { (idx, _) -> Math.abs(idx - labelIndex) }?.second
                ?: candidates.first().second
        } else {
            candidates.first().second
        }
    }

    // ── Description ───────────────────────────────────────────────────────────

    /**
     * Finds the amount anchor line (with or without currency symbol), then scans up to
     * DESCRIPTION_SEARCH_WINDOW lines after it for a valid user-entered note.
     *
     * Google Pay structure (OCR order may vary from visual order):
     *   To <merchant>
     *   <amount>              ← anchor
     *   <bank metadata>       ← rejected by uiTextPatterns
     *   <description>         ← first line that passes isValidDescription
     *   Pay again             ← rejected
     */
    private fun descriptionNearAmount(
        lines: List<String>,
        knownTxnId: String,
        knownGoogleTxnId: String
    ): String {
        val amountLine = lines.indexOfFirst { currencyAmount.containsMatchIn(it) }
            .takeIf { it >= 0 }
            ?: lines.indexOfFirst { bareAmount.matches(it) }

        if (amountLine < 0) return ""

        return lines.drop(amountLine + 1)
            .take(DESCRIPTION_SEARCH_WINDOW)
            .firstOrNull { isValidDescription(it, knownTxnId, knownGoogleTxnId) }
            .orEmpty()
    }

    private fun isValidDescription(
        candidate: String,
        knownTxnId: String = "",
        knownGoogleTxnId: String = ""
    ): Boolean {
        if (candidate.isBlank() || candidate.length > 120) return false
        if (dateTimeRegex.containsMatchIn(candidate)) return false
        if (currencyAmount.containsMatchIn(candidate)) return false
        if (bareAmount.matches(candidate)) return false
        if (candidate == knownTxnId || candidate == knownGoogleTxnId) return false
        return uiTextPatterns.none { it.containsMatchIn(candidate) }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun normalizeLine(raw: String): String =
        raw.replace("\u20B9", "₹")
            .replace("â‚¹", "₹")
            .trim()
            .replace(Regex("""\s+"""), " ")

    private fun valueAfterLabel(lines: List<String>, label: Regex): String {
        val index = lines.indexOfFirst { label.containsMatchIn(it) }
        if (index < 0) return ""
        return lines[index].replace(label, "").trim()
            .ifBlank { lines.getOrNull(index + 1).orEmpty() }
    }

    private fun parseDate(value: String): Long? {
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

    private companion object {
        const val DESCRIPTION_SEARCH_WINDOW = 6
    }
}
