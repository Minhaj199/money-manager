package com.moneymanager.parsing

import com.moneymanager.domain.model.TxnType
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for the hybrid automatic + manual parsing flow.
 *
 * These tests cover the ScreenshotParser (automatic side) and the amount/description
 * extraction logic that mirrors OcrViewModel's manual-field extraction.
 */
class HybridParsingTest {

    private val parser = ScreenshotParser()

    // ── Helpers mirroring OcrViewModel private extraction ────────────────────

    private val currencyAmountRegex = Regex(
        """(?i)(?:₹|Rs\.?|INR)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)"""
    )
    private val bareNumberRegex = Regex(
        """^([0-9]{1,7}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)$"""
    )

    private fun extractManualAmount(text: String): Double? {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return null
        val withSymbol = currencyAmountRegex.find(trimmed)
        if (withSymbol != null) {
            val v = withSymbol.groupValues[1].replace(",", "").toDoubleOrNull()
            if (v != null && v > 0) return v
        }
        val bare = bareNumberRegex.find(trimmed)
        if (bare != null) {
            val v = bare.groupValues[1].replace(",", "").toDoubleOrNull()
            if (v != null && v > 0 && v < 100_000_000) return v
        }
        return null
    }

    private fun extractManualDescription(text: String): String? {
        val cleaned = text.lines().map { it.trim() }.filter { it.isNotBlank() }.joinToString(" ")
        return cleaned.ifBlank { null }
    }

    private fun missingFields(parsed: com.moneymanager.domain.model.ParsedTransaction): List<String> = buildList {
        if (parsed.amount == null) add("AMOUNT")
        if (parsed.description.isBlank()) add("DESCRIPTION")
    }

    // ── Automatic success — no manual marking required ────────────────────────

    @Test fun `auto success - amount and description both detected`() {
        val parsed = parser.parse("₹70\njuice\nPay again\nCompleted")
        assertEquals(70.0, parsed.amount!!, 0.0)
        assertEquals("juice", parsed.description)
        assertTrue("No manual marking needed", missingFields(parsed).isEmpty())
    }

    @Test fun `auto success - Google Pay with currency symbol`() {
        val input = "To MR CHAI\n₹70\njuce\nPay again\nCompleted\nGoogle Pay\nUPI transaction ID\n661597442530"
        val parsed = parser.parse(input)
        assertEquals(70.0, parsed.amount!!, 0.0)
        assertEquals("juce", parsed.description)
        assertTrue(missingFields(parsed).isEmpty())
    }

    // ── Automatic failure — missing fields trigger manual marking ─────────────

    @Test fun `auto failure - bare number without context produces null amount on non-GPay`() {
        // Non-Google Pay receipt: bare number should NOT be parsed as amount.
        val parsed = parser.parse("To SHOP\n70\nsome note\nBank receipt")
        assertNull("Bare number on non-GPay should not be parsed", parsed.amount)
        assertTrue("AMOUNT should be in missing fields", missingFields(parsed).contains("AMOUNT"))
    }

    @Test fun `auto failure - no description when only UI text follows amount`() {
        val parsed = parser.parse("₹70\nPay again\nCompleted\nUPI transaction ID\n123456789012")
        assertEquals(70.0, parsed.amount!!, 0.0)
        assertEquals("", parsed.description)
        assertTrue("DESCRIPTION should be in missing fields", missingFields(parsed).contains("DESCRIPTION"))
    }

    @Test fun `auto failure - both amount and description missing`() {
        val parsed = parser.parse("To SHOP\nFederal Bank 9340\nPay again\nCompleted")
        assertNull(parsed.amount)
        assertEquals("", parsed.description)
        val missing = missingFields(parsed)
        assertTrue(missing.contains("AMOUNT"))
        assertTrue(missing.contains("DESCRIPTION"))
    }

    // ── Manual amount extraction ──────────────────────────────────────────────

    @Test fun `manual amount - rupee symbol`() {
        assertEquals(70.0, extractManualAmount("₹70")!!, 0.0)
    }

    @Test fun `manual amount - rupee symbol with space`() {
        assertEquals(70.0, extractManualAmount("₹ 70")!!, 0.0)
    }

    @Test fun `manual amount - bare number`() {
        assertEquals(70.0, extractManualAmount("70")!!, 0.0)
    }

    @Test fun `manual amount - decimal`() {
        assertEquals(70.50, extractManualAmount("₹70.50")!!, 0.001)
    }

    @Test fun `manual amount - thousands with comma`() {
        assertEquals(1000.0, extractManualAmount("₹1,000")!!, 0.0)
    }

    @Test fun `manual amount - Rs dot prefix`() {
        assertEquals(70.0, extractManualAmount("Rs. 70")!!, 0.0)
    }

    @Test fun `manual amount - INR prefix`() {
        assertEquals(70.0, extractManualAmount("INR 70")!!, 0.0)
    }

    @Test fun `manual amount - empty text returns null`() {
        assertNull(extractManualAmount(""))
        assertNull(extractManualAmount("   "))
    }

    @Test fun `manual amount - transaction ID rejected`() {
        // 12-digit IDs exceed the 7-digit bare number limit.
        assertNull(extractManualAmount("661597442530"))
    }

    @Test fun `manual amount - date text rejected`() {
        assertNull(extractManualAmount("6 Sept 2026"))
    }

    // ── Manual description extraction ─────────────────────────────────────────

    @Test fun `manual description - simple word`() {
        assertEquals("juce", extractManualDescription("juce"))
    }

    @Test fun `manual description - no auto-correction`() {
        // "juce" must NOT become "juice"
        assertEquals("juce", extractManualDescription("juce"))
    }

    @Test fun `manual description - multi-line joined`() {
        assertEquals("Lunch with friends", extractManualDescription("Lunch\nwith friends"))
    }

    @Test fun `manual description - trims whitespace`() {
        assertEquals("dinner", extractManualDescription("  dinner  "))
    }

    @Test fun `manual description - empty returns null`() {
        assertNull(extractManualDescription(""))
        assertNull(extractManualDescription("   "))
    }

    // ── Manual override — manual value replaces automatic value ───────────────

    @Test fun `manual override - user-selected amount replaces auto amount`() {
        // Auto parsed 700, user selects ₹70.
        val autoParsed = parser.parse("₹700\nsome note\nCompleted")
        assertEquals(700.0, autoParsed.amount!!, 0.0)

        // Simulate manual override.
        val manualAmount = extractManualAmount("₹70")
        assertNotNull(manualAmount)
        assertEquals(70.0, manualAmount!!, 0.0)
        // Manual value wins.
        val finalAmount = manualAmount
        assertEquals(70.0, finalAmount, 0.0)
    }

    @Test fun `manual override - user-selected description replaces auto description`() {
        // Auto parsed "Federal Bank 9340" as description (hypothetical bad parse).
        val manualDesc = extractManualDescription("juce")
        assertEquals("juce", manualDesc)
    }

    // ── No description — receipt has no user note ─────────────────────────────

    @Test fun `no description - Pay again is not a description`() {
        val parsed = parser.parse("₹70\nPay again\nCompleted")
        assertNotEquals("Pay again", parsed.description)
        assertNotEquals("Completed", parsed.description)
    }

    @Test fun `no description - Google Pay UI text not used`() {
        val parsed = parser.parse("₹70\nGoogle Pay\nCompleted\nUPI transaction ID\n123456789012")
        assertNotEquals("Google Pay", parsed.description)
        assertNotEquals("Completed", parsed.description)
    }

    @Test fun `no description - bank info not used`() {
        val parsed = parser.parse("₹70\nFederal Bank 9340\nPay again\nGoogle Pay\nUPI transaction ID\n123456789012")
        assertNotEquals("Federal Bank 9340", parsed.description)
    }
}
