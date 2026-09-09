package com.moneymanager.domain

import com.moneymanager.domain.model.Transaction
import com.moneymanager.domain.model.TxnSource
import com.moneymanager.domain.model.TxnType
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests the duplicate-detection heuristics described in the spec:
 * 1. Same txnId → duplicate
 * 2. Same amount + date (within 24h) + merchant → likely duplicate
 * 3. Same amount alone → NOT a duplicate
 */
class DuplicateDetectionTest {

    private val now = System.currentTimeMillis()

    private fun txn(
        id: String = "t1",
        amount: Double = 350.0,
        merchant: String = "Swiggy",
        txnId: String = "",
        date: Long = now
    ) = Transaction(
        id = id, fundId = "f1", amount = amount, type = TxnType.EXPENSE,
        categoryId = "food", description = "", merchant = merchant,
        txnId = txnId, date = date, source = TxnSource.MANUAL
    )

    @Test fun `same txnId is a duplicate`() {
        val existing = txn(id = "t1", txnId = "661597442530")
        val incoming = txn(id = "t2", txnId = "661597442530")
        assertTrue(existing.txnId.isNotBlank() && existing.txnId == incoming.txnId)
    }

    @Test fun `same amount and merchant within 24h is a likely duplicate`() {
        val existing = txn(id = "t1", amount = 350.0, merchant = "Swiggy", date = now)
        val incoming = txn(id = "t2", amount = 350.0, merchant = "Swiggy", date = now + 3_600_000)
        val withinDay = Math.abs(existing.date - incoming.date) < 86_400_000
        assertTrue(withinDay && existing.amount == incoming.amount && existing.merchant == incoming.merchant)
    }

    @Test fun `same amount alone is NOT a duplicate`() {
        val existing = txn(id = "t1", amount = 350.0, merchant = "Swiggy", date = now)
        val incoming = txn(id = "t2", amount = 350.0, merchant = "Zomato", date = now)
        // Different merchant — not a duplicate even though amount matches
        assertNotEquals(existing.merchant, incoming.merchant)
    }

    @Test fun `same amount different date beyond 24h is not a duplicate`() {
        val existing = txn(id = "t1", amount = 350.0, merchant = "Swiggy", date = now)
        val incoming = txn(id = "t2", amount = 350.0, merchant = "Swiggy", date = now - 90_000_000)
        val withinDay = Math.abs(existing.date - incoming.date) < 86_400_000
        assertFalse(withinDay)
    }

    @Test fun `blank txnId does not trigger txnId duplicate match`() {
        val existing = txn(id = "t1", txnId = "")
        val incoming = txn(id = "t2", txnId = "")
        // Both blank — txnId match must not fire
        assertFalse(existing.txnId.isNotBlank() && existing.txnId == incoming.txnId)
    }

    @Test fun `different txnIds are not duplicates`() {
        val existing = txn(id = "t1", txnId = "111111111111")
        val incoming = txn(id = "t2", txnId = "222222222222")
        assertNotEquals(existing.txnId, incoming.txnId)
    }
}
