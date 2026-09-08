package com.moneymanager.parsing

import com.moneymanager.domain.model.ParsedTransaction
import com.moneymanager.domain.model.TxnType
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses bank statement PDFs into a list of ParsedTransactions.
 * Handles common Indian bank statement formats.
 */
@Singleton
class PdfParser @Inject constructor() {

    // Matches lines like: 08 Sep | UPI-Rahul | 350.00 | Dr
    private val lineRegex = Regex(
        """(\d{1,2}(?:[\s/-][A-Za-z]{3,9}|[/-]\d{1,2})(?:[\s/-]\d{2,4})?)\s*[|]?\s*(.+?)\s*[|]?\s*([\d,]+(?:\.\d{1,2})?)\s*[|]?\s*(Cr|Dr|Credit|Debit)\b""",
        RegexOption.IGNORE_CASE
    )

    fun parse(inputStream: InputStream): List<ParsedTransaction> {
        val text = extractText(inputStream) ?: return emptyList()
        return parseLines(text)
    }

    private fun extractText(inputStream: InputStream): String? = try {
        val doc = PDDocument.load(inputStream)
        val text = PDFTextStripper().getText(doc)
        doc.close()
        text
    } catch (_: Exception) { null }

    fun parseLines(text: String): List<ParsedTransaction> {
        val results = mutableListOf<ParsedTransaction>()

        for (line in text.lines()) {
            val match = lineRegex.find(line) ?: continue
            val (dateStr, desc, amountStr, typeStr) = match.destructured
            val amount = amountStr.replace(",", "").toDoubleOrNull() ?: continue
            val type = if (typeStr.lowercase().startsWith("cr")) TxnType.INCOME else TxnType.EXPENSE
            val date = parseDate(dateStr.trim())
            val merchant = desc.trim()
                .replace(Regex("""UPI[-/]""", RegexOption.IGNORE_CASE), "")
                .trim()

            results += ParsedTransaction(
                amount = amount,
                type = type,
                merchant = merchant,
                upiId = "",
                txnId = "",
                description = desc.trim(),
                date = date,
                paymentApp = ""
            )
        }
        return results
    }

    private fun parseDate(str: String): Long? {
        val formats = listOf("d MMM yyyy", "d MMM yy", "d MMM", "d/M/yyyy", "d-M-yyyy")
        for (fmt in formats) {
            try {
                val parsed = SimpleDateFormat(fmt, Locale.ENGLISH).parse(str)
                if (parsed != null) {
                    // If year is missing (e.g. "08 Sep"), use current year
                    val cal = Calendar.getInstance().apply { time = parsed }
                    if (cal.get(Calendar.YEAR) == 1970) cal.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR))
                    return cal.timeInMillis
                }
            } catch (_: Exception) {}
        }
        return null
    }
}
