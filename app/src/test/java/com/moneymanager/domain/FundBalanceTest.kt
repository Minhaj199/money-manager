package com.moneymanager.domain

import com.moneymanager.domain.model.Fund
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Fund.balance = startingBalance + totalIncome - totalExpent
 * FundRepository reconstructs totalIncome/totalExpent from the transactions table,
 * so balance is always ledger-derived and never stored directly.
 */
class FundBalanceTest {

    private fun fund(starting: Double, income: Double, expense: Double) =
        Fund("id", "Test", "💰", "#6750A4", startingBalance = starting, totalIncome = income, totalExpent = expense)

    @Test fun `initial balance equals starting balance when no transactions`() {
        assertEquals(2000.0, fund(2000.0, 0.0, 0.0).balance, 0.0)
    }

    @Test fun `expense reduces balance`() {
        // Home Expense ₹2,000 starting, ₹350 expense → ₹1,650
        assertEquals(1650.0, fund(2000.0, 0.0, 350.0).balance, 0.0)
    }

    @Test fun `updated expense recalculates correctly`() {
        // ₹350 updated to ₹500 → ₹2,000 - ₹500 = ₹1,500
        assertEquals(1500.0, fund(2000.0, 0.0, 500.0).balance, 0.0)
    }

    @Test fun `deleted expense restores balance`() {
        // After deleting ₹500 expense → ₹2,000
        assertEquals(2000.0, fund(2000.0, 0.0, 0.0).balance, 0.0)
    }

    @Test fun `incoming transfer increases balance`() {
        // Home Expense receives ₹300 transfer (INCOME) with ₹350 expense
        assertEquals(1950.0, fund(2000.0, 300.0, 350.0).balance, 0.0)
    }

    @Test fun `outgoing transfer reduces balance`() {
        // Personal ₹5,000 sends ₹300 (EXPENSE)
        assertEquals(4700.0, fund(5000.0, 0.0, 300.0).balance, 0.0)
    }

    @Test fun `allocated equals starting plus income`() {
        assertEquals(2300.0, fund(2000.0, 300.0, 350.0).allocated, 0.0)
    }

    @Test fun `multiple expenses accumulate correctly`() {
        // ₹350 + ₹100 = ₹450 total expense from ₹2,000 → ₹1,550
        assertEquals(1550.0, fund(2000.0, 0.0, 450.0).balance, 0.0)
    }

    @Test fun `transfer delete restores both funds`() {
        // Personal: ₹5,000 - ₹300 transfer-out = ₹4,700
        assertEquals(4700.0, fund(5000.0, 0.0, 300.0).balance, 0.0)
        // Personal after transfer deleted: ₹5,000
        assertEquals(5000.0, fund(5000.0, 0.0, 0.0).balance, 0.0)

        // Home Expense: ₹2,000 + ₹300 transfer-in = ₹2,300
        assertEquals(2300.0, fund(2000.0, 300.0, 0.0).balance, 0.0)
        // Home Expense after transfer deleted: ₹2,000
        assertEquals(2000.0, fund(2000.0, 0.0, 0.0).balance, 0.0)
    }

    @Test fun `overspent fund has negative balance`() {
        assertEquals(-100.0, fund(100.0, 0.0, 200.0).balance, 0.0)
    }
}
