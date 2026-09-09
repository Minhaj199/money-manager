package com.moneymanager.ui.viewmodel

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.moneymanager.domain.model.ParsedTransaction
import com.moneymanager.parsing.ScreenshotParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/** Which fields the user must manually mark. */
enum class MarkableField { AMOUNT, DESCRIPTION }

/** Source of a parsed field value. */
enum class FieldSource { AUTOMATIC, MANUAL }

/**
 * A single OCR text block with its bounding box on the original image.
 * Used so the user can tap near a block rather than drawing a rectangle.
 */
data class OcrBlock(val text: String, val bounds: Rect)

sealed class OcrState {
    object Idle : OcrState()
    object Processing : OcrState()
    data class NeedsMarking(
        val parsed: ParsedTransaction,
        val bitmap: Bitmap,
        val blocks: List<OcrBlock>,
        val missingFields: List<MarkableField>,
        /** Manually confirmed values so far (field → value). */
        val manualValues: Map<MarkableField, String> = emptyMap(),
        val sources: Map<MarkableField, FieldSource> = emptyMap()
    ) : OcrState()
    data class Ready(val parsed: ParsedTransaction) : OcrState()
    data class Error(val message: String) : OcrState()
}

@HiltViewModel
class OcrViewModel @Inject constructor(private val parser: ScreenshotParser) : ViewModel() {

    private val _state = MutableStateFlow<OcrState>(OcrState.Idle)
    val state: StateFlow<OcrState> = _state.asStateFlow()

    fun processImage(bitmap: Bitmap) {
        _state.value = OcrState.Processing
        viewModelScope.launch {
            try {
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val result = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()

                if (result.text.isBlank()) {
                    Log.d("OCR_DEBUG", "========== RAW OCR TEXT: EMPTY ==========")
                } else {
                    Log.d("OCR_DEBUG", "========== RAW OCR TEXT ==========")
                    Log.d("OCR_DEBUG", result.text)
                    Log.d("OCR_DEBUG", "==================================")
                }

                val blocks = extractBlocks(result)
                val parsed = parser.parse(result.text)

                Log.d("PARSER_RESULT", "========== PARSED RESULT ==========")
                Log.d("PARSER_RESULT", "AUTO_PARSE amount=${parsed.amount}")
                Log.d("PARSER_RESULT", "AUTO_PARSE description='${parsed.description}'")
                Log.d("PARSER_RESULT", "AUTO_PARSE merchant='${parsed.merchant}'")
                Log.d("PARSER_RESULT", "AUTO_PARSE txnId='${parsed.txnId}'")
                Log.d("PARSER_RESULT", "AUTO_PARSE googleTransactionId='${parsed.googleTransactionId}'")
                Log.d("PARSER_RESULT", "AUTO_PARSE date=${parsed.date}")
                Log.d("PARSER_RESULT", "AUTO_PARSE time='${parsed.time}'")
                Log.d("PARSER_RESULT", "AUTO_PARSE paymentApp='${parsed.paymentApp}'")
                Log.d("PARSER_RESULT", "AUTO_PARSE paymentMethod='${parsed.paymentMethod}'")
                Log.d("PARSER_RESULT", "AUTO_PARSE status='${parsed.status}'")
                Log.d("PARSER_RESULT", "====================================")

                val missing = missingFields(parsed)
                Log.d("PARSER_RESULT", "fields requiring manual selection: $missing")

                if (missing.isEmpty()) {
                    _state.value = OcrState.Ready(parsed)
                } else {
                    _state.value = OcrState.NeedsMarking(
                        parsed = parsed,
                        bitmap = bitmap,
                        blocks = blocks,
                        missingFields = missing
                    )
                }
            } catch (e: Exception) {
                _state.value = OcrState.Error(e.message ?: "OCR failed")
            }
        }
    }

    /**
     * Called when the user taps an OCR block (or a fallback crop) for a field.
     * Extracts the field value from the selected text and updates state.
     * Returns false if extraction fails (caller should show retry message).
     */
    fun applyManualField(field: MarkableField, selectedText: String): Boolean {
        val current = _state.value as? OcrState.NeedsMarking ?: return false
        val extracted = when (field) {
            MarkableField.AMOUNT -> extractManualAmount(selectedText)
            MarkableField.DESCRIPTION -> extractManualDescription(selectedText)
        }
        if (extracted == null) {
            Log.d("MANUAL_SELECTION", "MANUAL_OCR field=$field result='$selectedText' -> extraction failed")
            return false
        }

        Log.d("MANUAL_SELECTION", "MANUAL_SELECTION field=$field")
        Log.d("MANUAL_SELECTION", "MANUAL_OCR result='$selectedText'")
        when (field) {
            MarkableField.AMOUNT -> Log.d("MANUAL_SELECTION", "MANUAL_AMOUNT result=$extracted")
            MarkableField.DESCRIPTION -> Log.d("MANUAL_SELECTION", "MANUAL_DESCRIPTION result=$extracted")
        }

        val newManual = current.manualValues + (field to extracted)
        val newSources = current.sources + (field to FieldSource.MANUAL)

        // Build the final ParsedTransaction merging auto + manual values.
        val finalParsed = mergeParsed(current.parsed, newManual)

        val stillMissing = current.missingFields.filter { it !in newManual }
        if (stillMissing.isEmpty()) {
            Log.d("MANUAL_SELECTION", "FINAL_PARSE amount=${finalParsed.amount}")
            Log.d("MANUAL_SELECTION", "FINAL_PARSE description='${finalParsed.description}'")
            _state.value = OcrState.Ready(finalParsed)
        } else {
            _state.value = current.copy(
                parsed = finalParsed,
                manualValues = newManual,
                sources = newSources,
                missingFields = stillMissing
            )
        }
        return true
    }

    /** Allow the user to re-mark a field that was already manually set. */
    fun resetManualField(field: MarkableField) {
        val current = _state.value as? OcrState.NeedsMarking ?: return
        val newManual = current.manualValues - field
        val newSources = current.sources - field
        val missingFields = (current.missingFields + field).distinct()
        _state.value = current.copy(
            manualValues = newManual,
            sources = newSources,
            missingFields = missingFields
        )
    }

    /** Proceed to review with whatever has been collected (called from NeedsMarking "Continue"). */
    fun proceedToReview() {
        val current = _state.value as? OcrState.NeedsMarking ?: return
        val finalParsed = mergeParsed(current.parsed, current.manualValues)
        Log.d("MANUAL_SELECTION", "FINAL_PARSE amount=${finalParsed.amount}")
        Log.d("MANUAL_SELECTION", "FINAL_PARSE description='${finalParsed.description}'")
        _state.value = OcrState.Ready(finalParsed)
    }

    fun showError(message: String) {
        _state.value = OcrState.Error(message)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun missingFields(parsed: ParsedTransaction): List<MarkableField> = buildList {
        if (parsed.amount == null) add(MarkableField.AMOUNT)
        if (parsed.description.isBlank()) add(MarkableField.DESCRIPTION)
    }

    private fun mergeParsed(base: ParsedTransaction, manual: Map<MarkableField, String>): ParsedTransaction {
        var result = base
        manual[MarkableField.AMOUNT]?.toDoubleOrNull()?.let { result = result.copy(amount = it) }
        manual[MarkableField.DESCRIPTION]?.let { result = result.copy(description = it) }
        return result
    }

    /**
     * Extracts all individual word/line blocks from ML Kit result with their bounding boxes.
     * Prefers line-level blocks for better tap targets.
     */
    private fun extractBlocks(result: Text): List<OcrBlock> = buildList {
        for (block in result.textBlocks) {
            for (line in block.lines) {
                val bounds = line.boundingBox ?: continue
                val text = line.text.trim()
                if (text.isNotEmpty()) add(OcrBlock(text, bounds))
            }
        }
    }

    // ── Amount extraction from user-selected text ─────────────────────────────

    private val currencyAmountRegex = Regex(
        """(?i)(?:₹|Rs\.?|INR)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)"""
    )
    private val bareNumberRegex = Regex(
        """^([0-9]{1,7}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)$"""
    )

    /**
     * Extracts a monetary value from user-selected OCR text.
     * Returns the numeric string (e.g. "70.0") or null if invalid.
     */
    private fun extractManualAmount(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return null

        // Try currency-prefixed first.
        val withSymbol = currencyAmountRegex.find(trimmed)
        if (withSymbol != null) {
            val value = withSymbol.groupValues[1].replace(",", "").toDoubleOrNull()
            if (value != null && value > 0) return value.toString()
        }

        // Try bare number (user selected just "70").
        val bare = bareNumberRegex.find(trimmed)
        if (bare != null) {
            val value = bare.groupValues[1].replace(",", "").toDoubleOrNull()
            if (value != null && value > 0 && value < 100_000_000) return value.toString()
        }

        return null
    }

    /**
     * Cleans user-selected description text.
     * Joins multiple lines, trims whitespace. Does NOT auto-correct spelling.
     */
    private fun extractManualDescription(text: String): String? {
        val cleaned = text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(" ")
        return cleaned.ifBlank { null }
    }
}
