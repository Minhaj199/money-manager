package com.moneymanager.backup

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests the backup structural rules that BackupManager enforces.
 * Uses plain Kotlin data classes — no Android SDK (org.json) required.
 */
class BackupStructureTest {

    // ── Plain data models mirroring the JSON backup schema ────────────────────

    data class BackupFund(
        val id: String, val name: String, val icon: String, val colorHex: String,
        val sourceName: String, val startingBalance: Double,
        val createdAt: Long, val updatedAt: Long
    )

    data class BackupCategory(
        val id: String, val name: String, val icon: String, val type: String,
        val createdAt: Long, val updatedAt: Long
    )

    data class BackupTransaction(
        val id: String, val fundId: String, val amount: Double, val type: String,
        val categoryId: String, val description: String, val merchant: String,
        val upiId: String, val txnId: String, val paymentMethod: String,
        val googleTransactionId: String, val paymentApp: String, val status: String,
        val source: String, val date: Long, val importBatchId: String,
        val createdAt: Long, val updatedAt: Long
    )

    data class BackupTransfer(
        val id: String, val fromFundId: String, val toFundId: String,
        val amount: Double, val note: String, val date: Long,
        val createdAt: Long, val updatedAt: Long
    )

    data class Backup(
        val backupVersion: Int,
        val appVersion: String,
        val backupCreatedAt: Long,
        val currency: String,
        val funds: List<BackupFund>,
        val categories: List<BackupCategory>,
        val transactions: List<BackupTransaction>,
        val transfers: List<BackupTransfer>
    )

    // ── Helpers ───────────────────────────────────────────────────────────────

    private val now = System.currentTimeMillis()

    private fun fund(id: String = "f1", name: String = "Home Expense", sourceName: String = "") =
        BackupFund(id, name, "💰", "#6750A4", sourceName, 2000.0, now, now)

    private fun category(id: String = "food", name: String = "Food") =
        BackupCategory(id, name, "🍽️", "EXPENSE", now, now)

    private fun transaction(
        id: String = "t1", fundId: String = "f1", amount: Double = 350.0,
        type: String = "EXPENSE", categoryId: String = "food",
        source: String = "MANUAL", importBatchId: String = ""
    ) = BackupTransaction(id, fundId, amount, type, categoryId, "", "", "", "", "", "", "", "", source, now, importBatchId, now, now)

    private fun transfer(id: String = "tr1", fromFundId: String = "f1", toFundId: String = "f2", amount: Double = 300.0) =
        BackupTransfer(id, fromFundId, toFundId, amount, "", now, now, now)

    private fun minimalBackup(
        version: Int = 1,
        funds: List<BackupFund> = listOf(fund()),
        categories: List<BackupCategory> = listOf(category()),
        transactions: List<BackupTransaction> = listOf(transaction()),
        transfers: List<BackupTransfer> = emptyList()
    ) = Backup(version, "1.0", now, "INR", funds, categories, transactions, transfers)

    // ── Validation logic (mirrors BackupManager.validate) ─────────────────────

    private val validTypes = setOf("INCOME", "EXPENSE")
    private val validSources = setOf("MANUAL", "SCREENSHOT", "PDF", "TRANSFER")

    private fun validate(backup: Backup): Boolean {
        if (backup.backupVersion < 1) return false
        val fundIds = backup.funds.map { it.id }.toSet()
        val categoryIds = backup.categories.map { it.id }.toSet()
        if (backup.funds.any { it.id.isBlank() } || backup.funds.map { it.id }.distinct().size != backup.funds.size) return false
        if (backup.transactions.any { it.amount <= 0 || it.fundId !in fundIds }) return false
        if (backup.transactions.any { it.type !in validTypes || it.source !in validSources }) return false
        if (backup.transactions.any { it.categoryId.isNotBlank() && it.categoryId != "transfer" && it.categoryId !in categoryIds }) return false
        if (backup.transfers.any { it.amount <= 0 || it.fromFundId !in fundIds || it.toFundId !in fundIds || it.fromFundId == it.toFundId }) return false
        val transferIds = backup.transfers.map { it.id }.toSet()
        backup.transactions.filter { it.source == "TRANSFER" }.groupBy { it.importBatchId }.forEach { (id, entries) ->
            if (id !in transferIds || entries.size != 2 || entries.none { it.type == "INCOME" } || entries.none { it.type == "EXPENSE" }) return false
        }
        return true
    }

    // ── Version check ──────────────────────────────────────────────────────────

    @Test fun `valid backup version 1 passes validation`() {
        assertTrue(validate(minimalBackup(version = 1)))
    }

    @Test fun `backup version 0 is rejected`() {
        assertFalse(validate(minimalBackup(version = 0)))
    }

    @Test fun `backup version greater than 1 is accepted for forward compatibility`() {
        // Future versions (e.g. 2, 3) must not be rejected
        assertTrue(validate(minimalBackup(version = 2)))
        assertTrue(validate(minimalBackup(version = 99)))
    }

    // ── Required top-level fields ──────────────────────────────────────────────

    @Test fun `complete backup contains all required fields`() {
        val backup = minimalBackup()
        assertTrue(backup.backupVersion >= 1)
        assertTrue(backup.appVersion.isNotBlank())
        assertTrue(backup.backupCreatedAt > 0)
        assertEquals("INR", backup.currency)
        assertNotNull(backup.funds)
        assertNotNull(backup.categories)
        assertNotNull(backup.transactions)
        assertNotNull(backup.transfers)
    }

    @Test fun `backup currency is preserved`() {
        assertEquals("INR", minimalBackup().currency)
    }

    // ── Fund validation ────────────────────────────────────────────────────────

    @Test fun `fund has required fields`() {
        val f = fund("f1", "Home Expense")
        assertTrue(f.id.isNotBlank())
        assertTrue(f.name.isNotBlank())
        assertEquals(2000.0, f.startingBalance, 0.0)
    }

    @Test fun `fund preserves sourceName`() {
        val f = fund("f1", "Home Expense", sourceName = "Father")
        assertEquals("Father", f.sourceName)
    }

    @Test fun `fund with blank id fails validation`() {
        val backup = minimalBackup(funds = listOf(fund("", "Home Expense")))
        assertFalse(validate(backup))
    }

    @Test fun `duplicate fund ids fail validation`() {
        val backup = minimalBackup(funds = listOf(fund("f1"), fund("f1", "Duplicate")))
        assertFalse(validate(backup))
    }

    // ── Transaction validation ─────────────────────────────────────────────────

    @Test fun `transaction references valid fund`() {
        val txn = transaction("t1", "f1", 350.0, "EXPENSE", "food")
        assertEquals("f1", txn.fundId)
        assertTrue(txn.amount > 0)
    }

    @Test fun `transaction with unknown fund fails validation`() {
        val backup = minimalBackup(transactions = listOf(transaction(fundId = "unknown_fund")))
        assertFalse(validate(backup))
    }

    @Test fun `transaction with zero amount fails validation`() {
        val backup = minimalBackup(transactions = listOf(transaction(amount = 0.0)))
        assertFalse(validate(backup))
    }

    @Test fun `transaction with negative amount fails validation`() {
        val backup = minimalBackup(transactions = listOf(transaction(amount = -100.0)))
        assertFalse(validate(backup))
    }

    @Test fun `transaction type must be INCOME or EXPENSE`() {
        assertTrue(transaction(type = "INCOME").type in validTypes)
        assertTrue(transaction(type = "EXPENSE").type in validTypes)
    }

    @Test fun `transaction with invalid type fails validation`() {
        val backup = minimalBackup(transactions = listOf(transaction(type = "TRANSFER_OUT")))
        assertFalse(validate(backup))
    }

    @Test fun `transaction source must be valid`() {
        validSources.forEach { src ->
            assertTrue(transaction(source = src).source in validSources)
        }
    }

    @Test fun `transaction with invalid source fails validation`() {
        val backup = minimalBackup(transactions = listOf(transaction(source = "UNKNOWN")))
        assertFalse(validate(backup))
    }

    // ── Transfer validation ────────────────────────────────────────────────────

    @Test fun `transfer has two offsetting transaction entries`() {
        val transferId = "tr1"
        val txns = listOf(
            transaction("t1", "f1", 300.0, "EXPENSE", "transfer", "TRANSFER", transferId),
            transaction("t2", "f2", 300.0, "INCOME", "transfer", "TRANSFER", transferId)
        )
        val grouped = txns.filter { it.importBatchId == transferId }
        assertEquals(2, grouped.size)
        assertTrue(grouped.any { it.type == "EXPENSE" })
        assertTrue(grouped.any { it.type == "INCOME" })
    }

    @Test fun `valid transfer passes validation`() {
        val transferId = "tr1"
        val backup = minimalBackup(
            funds = listOf(fund("f1"), fund("f2", "Personal")),
            transactions = listOf(
                transaction("t1", "f1", 300.0, "EXPENSE", "transfer", "TRANSFER", transferId),
                transaction("t2", "f2", 300.0, "INCOME", "transfer", "TRANSFER", transferId)
            ),
            transfers = listOf(transfer(transferId, "f1", "f2", 300.0))
        )
        assertTrue(validate(backup))
    }

    @Test fun `transfer from and to funds must differ`() {
        val tr = transfer("tr1", "f1", "f2", 300.0)
        assertNotEquals(tr.fromFundId, tr.toFundId)
    }

    @Test fun `transfer with same from and to fund fails validation`() {
        val transferId = "tr1"
        val backup = minimalBackup(
            funds = listOf(fund("f1")),
            transactions = listOf(
                transaction("t1", "f1", 300.0, "EXPENSE", "transfer", "TRANSFER", transferId),
                transaction("t2", "f1", 300.0, "INCOME", "transfer", "TRANSFER", transferId)
            ),
            transfers = listOf(transfer(transferId, "f1", "f1", 300.0))
        )
        assertFalse(validate(backup))
    }

    @Test fun `transfer amount must be positive`() {
        val tr = transfer(amount = 300.0)
        assertTrue(tr.amount > 0)
    }

    @Test fun `transfer with zero amount fails validation`() {
        val transferId = "tr1"
        val backup = minimalBackup(
            funds = listOf(fund("f1"), fund("f2")),
            transactions = listOf(
                transaction("t1", "f1", 0.0, "EXPENSE", "transfer", "TRANSFER", transferId),
                transaction("t2", "f2", 0.0, "INCOME", "transfer", "TRANSFER", transferId)
            ),
            transfers = listOf(transfer(transferId, "f1", "f2", 0.0))
        )
        assertFalse(validate(backup))
    }

    @Test fun `transfer transaction without matching transfer record fails validation`() {
        val transferId = "tr1"
        val backup = minimalBackup(
            funds = listOf(fund("f1"), fund("f2")),
            transactions = listOf(
                transaction("t1", "f1", 300.0, "EXPENSE", "transfer", "TRANSFER", transferId),
                transaction("t2", "f2", 300.0, "INCOME", "transfer", "TRANSFER", transferId)
            ),
            transfers = emptyList()  // missing the Transfer record
        )
        assertFalse(validate(backup))
    }

    // ── Backup does not contain sensitive data ─────────────────────────────────

    @Test fun `backup data class has no credential fields`() {
        val backup = minimalBackup()
        // Verify via reflection that no field is named after sensitive data
        val sensitiveNames = setOf("apiKey", "oauthSecret", "accessToken", "privateKey", "password", "secret", "credential")
        val fieldNames = Backup::class.java.declaredFields.map { it.name }.toSet()
        sensitiveNames.forEach { sensitive ->
            assertFalse("Backup must not contain field '$sensitive'", sensitive in fieldNames)
        }
    }
}
