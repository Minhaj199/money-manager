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

    // ── Existing receipt — regression guard ───────────────────────────────────

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
        val p = parser.parse(existingReceipt)
        assertEquals(100.0, p.amount!!, 0.0)
        assertNotEquals(624855917066.0, p.amount)
        assertEquals("624855917066", p.txnId)
        assertEquals("CICAgPiw8sezKA", p.googleTransactionId)
        assertEquals("TECLYN MOBILE SALES AND SERVICE", p.merchant)
        assertEquals("screen guard", p.description)
        assertEquals("4946@myesaf", p.upiId)
        assertEquals("2026-09-05", SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(p.date!!))
        assertEquals("14:17", p.time)
        assertEquals(TxnType.EXPENSE, p.type)
        assertEquals("Google Pay", p.paymentApp)
        assertEquals("UPI", p.paymentMethod)
        assertEquals("Completed", p.status)
    }

    // ── Exact OCR log input (the real failing case) ───────────────────────────

    // This is the verbatim normalized OCR output from the device logs.
    // OCR lines are NOT in visual order — the parser must handle this.
    private val realOcrInput = """
        UPI transaction ID
        661597442530
        To: MR CHAI
        •2025@fbl
        M
        To MR CHAI
        70
        Federal Bank 9340
        juce
        Pay again
        6 Sept 2026, 6:36 pm
        CICAgPiluMSaTA
        Completed
        From: MINHAJ PK (Federal Bank)
        Google transaction ID
        Google Pay• *kkal@okaxis
        POWE
        BY
        UPI
        UNIFIED PAYMENTS INTERFACE
        GPay
    """.trimIndent()

    @Test fun `real OCR log - amount extracted without currency symbol`() {
        assertEquals(70.0, parser.parse(realOcrInput).amount!!, 0.0)
    }

    @Test fun `real OCR log - description is juce`() {
        assertEquals("juce", parser.parse(realOcrInput).description)
    }

    @Test fun `real OCR log - merchant is MR CHAI`() {
        assertEquals("MR CHAI", parser.parse(realOcrInput).merchant)
    }

    @Test fun `real OCR log - txnId is correct`() {
        assertEquals("661597442530", parser.parse(realOcrInput).txnId)
    }

    @Test fun `real OCR log - googleTransactionId is CICAgPiluMSaTA not the UPI line`() {
        assertEquals("CICAgPiluMSaTA", parser.parse(realOcrInput).googleTransactionId)
    }

    @Test fun `real OCR log - time is 18h36`() {
        assertEquals("18:36", parser.parse(realOcrInput).time)
    }

    @Test fun `real OCR log - paymentApp is Google Pay`() {
        assertEquals("Google Pay", parser.parse(realOcrInput).paymentApp)
    }

    @Test fun `real OCR log - paymentMethod is UPI`() {
        assertEquals("UPI", parser.parse(realOcrInput).paymentMethod)
    }

    @Test fun `real OCR log - status is Completed`() {
        assertEquals("Completed", parser.parse(realOcrInput).status)
    }

    @Test fun `real OCR log - date is 6 Sept 2026`() {
        assertEquals("2026-09-06", SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(parser.parse(realOcrInput).date!!))
    }

    // ── Amount: currency symbol present ───────────────────────────────────────

    @Test fun `amount - rupee symbol no space`() {
        assertEquals(70.0, parser.parse("₹70\nsome note\nGoogle Pay").amount!!, 0.0)
    }

    @Test fun `amount - rupee symbol with space`() {
        assertEquals(70.0, parser.parse("₹ 70\nsome note\nGoogle Pay").amount!!, 0.0)
    }

    @Test fun `amount - thousands with comma`() {
        assertEquals(1000.0, parser.parse("₹1,000\nsome note\nGoogle Pay").amount!!, 0.0)
    }

    @Test fun `amount - thousands with comma and decimal`() {
        assertEquals(1000.50, parser.parse("₹ 1,000.50\nsome note\nGoogle Pay").amount!!, 0.001)
    }

    @Test fun `amount - Rs dot prefix`() {
        assertEquals(70.0, parser.parse("Rs. 70\nsome note\nGoogle Pay").amount!!, 0.0)
    }

    @Test fun `amount - INR prefix`() {
        assertEquals(70.0, parser.parse("INR 70\nsome note\nGoogle Pay").amount!!, 0.0)
    }

    @Test fun `amount - decimal value`() {
        assertEquals(70.00, parser.parse("₹70.00\nsome note\nGoogle Pay").amount!!, 0.0)
    }

    // ── Amount: currency symbol lost by OCR (Google Pay bare-number fallback) ─

    @Test fun `amount - bare 70 on Google Pay receipt`() {
        val input = "To JOHN\n70\nFederal Bank\ndinner\nPay again\nGoogle Pay\nUPI transaction ID\n123456789012"
        assertEquals(70.0, parser.parse(input).amount!!, 0.0)
    }

    @Test fun `amount - bare 1000 on Google Pay receipt`() {
        val input = "To SHOP\n1000\nFederal Bank\ngroceries\nPay again\nGoogle Pay\nUPI transaction ID\n123456789012"
        assertEquals(1000.0, parser.parse(input).amount!!, 0.0)
    }

    @Test fun `amount - bare 1000 dot 50 on Google Pay receipt`() {
        val input = "To RAJ\n1000.50\nFederal Bank\nlunch\nPay again\nGoogle Pay\nUPI transaction ID\n123456789012"
        assertEquals(1000.50, parser.parse(input).amount!!, 0.001)
    }

    // ── IDs must NEVER become the amount ──────────────────────────────────────

    @Test fun `UPI transaction ID 661597442530 is never the amount`() {
        assertNotEquals(661597442530.0, parser.parse(realOcrInput).amount)
    }

    @Test fun `standalone long identifier is never an amount`() {
        val p = parser.parse("UPI transaction ID\n624855917066")
        assertEquals("624855917066", p.txnId)
        assertNull(p.amount)
    }

    @Test fun `Google transaction ID is never an amount`() {
        val p = parser.parse("Google transaction ID\nCICAgPilUMSaTA\nGoogle Pay")
        assertEquals("CICAgPilUMSaTA", p.googleTransactionId)
        assertNull(p.amount)
    }

    @Test fun `year 2026 is never the amount`() {
        val input = "To X\n70\n6 Sept 2026, 6:36 pm\nGoogle Pay\nUPI transaction ID\n123456789012"
        assertEquals(70.0, parser.parse(input).amount!!, 0.0)
    }

    @Test fun `4-digit account fragment 9340 is never the amount`() {
        val input = "To X\n70\nFederal Bank 9340\nGoogle Pay\nUPI transaction ID\n123456789012"
        assertEquals(70.0, parser.parse(input).amount!!, 0.0)
    }

    // ── Description variants ──────────────────────────────────────────────────

    @Test fun `description - juice after currency amount`() {
        assertEquals("juice", parser.parse("₹70\njuice\nPay again\nCompleted").description)
    }

    @Test fun `description - dinner after currency amount`() {
        assertEquals("dinner", parser.parse("₹250\ndinner\nPay again\nCompleted").description)
    }

    @Test fun `description - groceries after currency amount`() {
        assertEquals("groceries", parser.parse("₹1,200\ngroceries\nPay again").description)
    }

    @Test fun `description - multi-word after currency amount`() {
        assertEquals("Lunch with friends", parser.parse("₹500\nLunch with friends\nPay again").description)
    }

    @Test fun `description - juce after bare amount with intervening bank line`() {
        val input = "To MR CHAI\n70\nFederal Bank 9340\njuce\nPay again\nGoogle Pay\nUPI transaction ID\n661597442530"
        assertEquals("juce", parser.parse(input).description)
    }

    @Test fun `description - dinner after bare amount`() {
        val input = "To JOHN\n250\ndinner\nPay again\nGoogle Pay\nUPI transaction ID\n123456789012"
        assertEquals("dinner", parser.parse(input).description)
    }

    @Test fun `description - groceries after bare amount`() {
        val input = "To SHOP\n1200\ngroceries\nPay again\nGoogle Pay\nUPI transaction ID\n123456789012"
        assertEquals("groceries", parser.parse(input).description)
    }

    // ── UI text must NEVER become description ─────────────────────────────────

    @Test fun `Pay again is not a description`() {
        val p = parser.parse("₹70\nPay again\nCompleted")
        assertNotEquals("Pay again", p.description)
        assertNotEquals("Completed", p.description)
    }

    @Test fun `Completed is not a description`() {
        assertNotEquals("Completed", parser.parse("₹70\nCompleted\nUPI transaction ID\n123456789012").description)
    }

    @Test fun `no description when only UI text follows amount`() {
        assertEquals("", parser.parse("₹70\nPay again\nCompleted\nUPI transaction ID\n123456789012").description)
    }

    @Test fun `no description when only UI text follows bare amount on Google Pay`() {
        val input = "To X\n70\nPay again\nCompleted\nGoogle Pay\nUPI transaction ID\n123456789012"
        assertEquals("", parser.parse(input).description)
    }

    // ── Google transaction ID ─────────────────────────────────────────────────

    @Test fun `googleTransactionId is not the UPI account line`() {
        assertNotEquals("Google Pay• *kkal@okaxis", parser.parse(realOcrInput).googleTransactionId)
    }

    @Test fun `googleTransactionId found before label line in OCR`() {
        // Simulates the real OCR ordering where the ID appears before the label.
        val input = "UPI transaction ID\n661597442530\nCICAgPiluMSaTA\nCompleted\nGoogle transaction ID\nGoogle Pay• *kkal@okaxis\nGoogle Pay\nUPI"
        assertEquals("CICAgPiluMSaTA", parser.parse(input).googleTransactionId)
    }
}
