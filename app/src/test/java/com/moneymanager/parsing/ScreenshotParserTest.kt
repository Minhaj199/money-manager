package com.moneymanager.parsing

import com.moneymanager.domain.model.TxnType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class ScreenshotParserTest {

    private val parser = ScreenshotParser()

    // ── Existing receipt (regression) ─────────────────────────────────────────

    private val existingReceipt = """
        To: TECLYN MOBILE SALES AND SERVICE
        ₹100
        screen guard
        Pay again
        Completed
        5 Sept 2026, 2:17 pm
        UPI transaction ID
        624855917066
        To: TECLYN MOBILE SALES AND SERVICE
        ••••4946@myesaf
        From: MINHAJ P K (Federal Bank)
        Google Pay ••••kkal@okaxis
        Google transaction ID
        CICAgPiw8sezKA
    """.trimIndent()

    @Test fun `existing receipt - all fields parse correctly`() {
        val parsed = parser.parse(existingReceipt)

        assertEquals(100.0, parsed.amount!!, 0.0)
        assertNotEquals(624855917066.0, parsed.amount)
        assertEquals("624855917066", parsed.txnId)
        assertEquals("CICAgPiw8sezKA", parsed.googleTransactionId)
        assertEquals("TECLYN MOBILE SALES AND SERVICE", parsed.merchant)
        assertEquals("screen guard", parsed.description)
        assertEquals("4946@myesaf", parsed.upiId)
        assertEquals("2026-09-05", SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(parsed.date!!))
        assertEquals("14:17", parsed.time)
        assertEquals(TxnType.EXPENSE, parsed.type)
        assertEquals("Google Pay", parsed.paymentApp)
        assertEquals("UPI", parsed.paymentMethod)
        assertEquals("Completed", parsed.status)
    }

    // ── New Google Pay receipt (the failing case) ──────────────────────────────

    private val juceReceipt = """
        To MR CHAI
        ₹70
        juce
        Pay again
        Completed
        6 Sept 2026, 6:36 pm
        Federal Bank 9340
        UPI transaction ID
        661597442530
        To: MR CHAI
        ••••2025@fbl
        From: MINHAJ P K (Federal Bank)
        Google Pay • ••••kkal@okaxis
        Google transaction ID
        CICAgPilUMSaTA
    """.trimIndent()

    @Test fun `Google Pay juce receipt - amount and description extracted`() {
        val parsed = parser.parse(juceReceipt)

        assertEquals(70.0, parsed.amount!!, 0.0)
        assertEquals("juce", parsed.description)
        assertEquals("661597442530", parsed.txnId)
        assertEquals("CICAgPilUMSaTA", parsed.googleTransactionId)
        assertEquals("18:36", parsed.time)
        assertEquals("Google Pay", parsed.paymentApp)
        assertEquals("UPI", parsed.paymentMethod)
        assertEquals("Completed", parsed.status)
        assertEquals(TxnType.EXPENSE, parsed.type)
    }

    @Test fun `Google Pay juce receipt - date parses correctly`() {
        val parsed = parser.parse(juceReceipt)
        assertEquals("2026-09-06", SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(parsed.date!!))
    }

    // ── Amount format variants ─────────────────────────────────────────────────

    @Test fun `amount - rupee symbol no space`() {
        assertEquals(70.0, parser.parse("₹70\nsome note").amount!!, 0.0)
    }

    @Test fun `amount - rupee symbol with space`() {
        assertEquals(70.0, parser.parse("₹ 70\nsome note").amount!!, 0.0)
    }

    @Test fun `amount - thousands with comma`() {
        assertEquals(1000.0, parser.parse("₹1,000\nsome note").amount!!, 0.0)
    }

    @Test fun `amount - thousands with comma and decimal`() {
        assertEquals(1000.50, parser.parse("₹ 1,000.50\nsome note").amount!!, 0.001)
    }

    @Test fun `amount - Rs dot prefix`() {
        assertEquals(70.0, parser.parse("Rs. 70\nsome note").amount!!, 0.0)
    }

    @Test fun `amount - INR prefix`() {
        assertEquals(70.0, parser.parse("INR 70\nsome note").amount!!, 0.0)
    }

    @Test fun `amount - decimal value`() {
        assertEquals(70.00, parser.parse("₹70.00\nsome note").amount!!, 0.0)
    }

    // ── Description variants ───────────────────────────────────────────────────

    @Test fun `description - juice`() {
        assertEquals("juice", parser.parse("₹70\njuice\nPay again\nCompleted").description)
    }

    @Test fun `description - dinner`() {
        assertEquals("dinner", parser.parse("₹250\ndinner\nPay again\nCompleted").description)
    }

    @Test fun `description - groceries`() {
        assertEquals("groceries", parser.parse("₹1,200\ngroceries\nPay again").description)
    }

    @Test fun `description - multi-word`() {
        assertEquals("Lunch with friends", parser.parse("₹500\nLunch with friends\nPay again").description)
    }

    // ── UI text must never become description ──────────────────────────────────

    @Test fun `Pay again is not a description`() {
        val parsed = parser.parse("₹70\nPay again\nCompleted")
        assertNotEquals("Pay again", parsed.description)
        assertNotEquals("Completed", parsed.description)
    }

    @Test fun `Completed is not a description`() {
        val parsed = parser.parse("₹70\nCompleted\nUPI transaction ID\n123456789012")
        assertNotEquals("Completed", parsed.description)
    }

    @Test fun `no description when only UI text follows amount`() {
        val parsed = parser.parse("₹70\nPay again\nCompleted\nUPI transaction ID\n123456789012")
        assertEquals("", parsed.description)
    }

    // ── Transaction IDs must never be parsed as amounts ────────────────────────

    @Test fun `standalone long identifier is never an amount`() {
        val parsed = parser.parse("UPI transaction ID\n624855917066")
        assertEquals("624855917066", parsed.txnId)
        assertNull(parsed.amount)
    }

    @Test fun `Google transaction ID is never an amount`() {
        val parsed = parser.parse("Google transaction ID\nCICAgPilUMSaTA")
        assertEquals("CICAgPilUMSaTA", parsed.googleTransactionId)
        assertNull(parsed.amount)
    }
}
