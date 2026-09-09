package com.moneymanager.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.moneymanager.BuildConfig
import com.moneymanager.data.db.AppDatabase
import com.moneymanager.data.db.entity.CategoryEntity
import com.moneymanager.data.db.entity.FundEntity
import com.moneymanager.data.db.entity.TransactionEntity
import com.moneymanager.data.db.entity.TransferEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class BackupSummary(
    val createdAt: Long,
    val fundCount: Int,
    val categoryCount: Int,
    val transactionCount: Int,
    val transferCount: Int
)

sealed class BackupResult {
    data class Success(val summary: BackupSummary) : BackupResult()
    data class Failure(val message: String) : BackupResult()
}

/**
 * Serializes the complete financial ledger. It deliberately excludes credentials, caches and
 * source files. A document Uri may point to Google Drive's Android document provider, so Drive
 * remains optional and the app continues to work entirely locally.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase
) {
    suspend fun writeTo(uri: Uri): BackupResult {
        return try {
            val json = exportJson()
            context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(json.toString()) }
                ?: return BackupResult.Failure("Unable to open backup destination")
            BackupResult.Success(summary(json))
        } catch (_: Exception) {
            BackupResult.Failure("Backup failed. Your local data was not changed.")
        }
    }

    suspend fun restoreFrom(uri: Uri): BackupResult = try {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: return BackupResult.Failure("Unable to read backup file. Check the file is accessible.")
        val json = try { JSONObject(text) } catch (e: Exception) {
            return BackupResult.Failure("File is not valid JSON: ${e.message}")
        }
        val snapshot = validate(json) ?: return BackupResult.Failure(
            "Backup is invalid or incomplete. Expected funds, categories, transactions and transfers arrays with a backupVersion ≥ 1."
        )
        database.withTransaction {
            // Validation is complete before any delete, so replacement is atomic.
            database.transactionDao().deleteAll()
            database.transferDao().deleteAll()
            database.importBatchDao().deleteAll()
            database.categoryDao().deleteAll()
            database.fundDao().deleteAll()
            database.fundDao().upsertAll(snapshot.funds)
            database.categoryDao().upsertAll(snapshot.categories)
            database.transactionDao().upsertAll(snapshot.transactions)
            snapshot.transfers.forEach { database.transferDao().upsert(it) }
        }
        BackupResult.Success(snapshot.summary)
    } catch (e: Exception) {
        BackupResult.Failure("Restore failed: ${e.message ?: "unknown error"}. Existing local data was kept unchanged.")
    }

    suspend fun resetAllData(): BackupResult = try {
        database.withTransaction {
            database.transactionDao().deleteAll()
            database.transferDao().deleteAll()
            database.importBatchDao().deleteAll()
            database.categoryDao().deleteAll()
            database.fundDao().deleteAll()
        }
        BackupResult.Success(BackupSummary(System.currentTimeMillis(), 0, 0, 0, 0))
    } catch (_: Exception) {
        BackupResult.Failure("Could not reset all data. No partial reset was applied.")
    }

    private suspend fun exportJson(): JSONObject = database.withTransaction {
        val funds = database.fundDao().getAll()
        val categories = database.categoryDao().getAll()
        val transactions = database.transactionDao().getAll()
        val transfers = database.transferDao().getAll()
        JSONObject().apply {
            put("backupVersion", BACKUP_VERSION)
            put("appVersion", BuildConfig.VERSION_NAME)
            put("backupCreatedAt", System.currentTimeMillis())
            put("currency", "INR")
            put("funds", JSONArray().apply { funds.forEach { put(it.toJson()) } })
            put("categories", JSONArray().apply { categories.forEach { put(it.toJson()) } })
            put("transactions", JSONArray().apply { transactions.forEach { put(it.toJson()) } })
            put("transfers", JSONArray().apply { transfers.forEach { put(it.toJson()) } })
        }
    }

    private fun validate(json: JSONObject): Snapshot? {
        val version = json.optInt("backupVersion")
        if (version < 1) return null  // reject malformed; accept any version >= 1 for forward compatibility
        val funds = json.requiredArray("funds")?.map(::fundFromJson) ?: return null
        val categories = json.requiredArray("categories")?.map(::categoryFromJson) ?: return null
        val transactions = json.requiredArray("transactions")?.map(::transactionFromJson) ?: return null
        val transfers = json.requiredArray("transfers")?.map(::transferFromJson) ?: return null
        if (!funds.uniqueIds() || !categories.uniqueIds() || !transactions.uniqueIds() || !transfers.uniqueIds()) return null
        val fundIds = funds.map { it.id }.toSet()
        val categoryIds = categories.map { it.id }.toSet()
        if (transactions.any { it.amount <= 0 || it.fundId !in fundIds || (it.categoryId.isNotBlank() && it.categoryId != "transfer" && it.categoryId !in categoryIds) }) return null
        if (transfers.any { it.amount <= 0 || it.fromFundId !in fundIds || it.toFundId !in fundIds || it.fromFundId == it.toFundId }) return null
        if (transactions.any { it.type !in setOf("INCOME", "EXPENSE") || it.source !in setOf("MANUAL", "SCREENSHOT", "PDF", "TRANSFER") }) return null
        val transferIds = transfers.map { it.id }.toSet()
        if (transactions.filter { it.source == "TRANSFER" }.groupBy { it.importBatchId }.any { (id, entries) ->
                id !in transferIds || entries.size != 2 || entries.none { it.type == "INCOME" } || entries.none { it.type == "EXPENSE" }
            }) return null
        return Snapshot(funds, categories, transactions, transfers, summary(json))
    }

    private fun summary(json: JSONObject) = BackupSummary(
        createdAt = json.optLong("backupCreatedAt"),
        fundCount = json.optJSONArray("funds")?.length() ?: 0,
        categoryCount = json.optJSONArray("categories")?.length() ?: 0,
        transactionCount = json.optJSONArray("transactions")?.length() ?: 0,
        transferCount = json.optJSONArray("transfers")?.length() ?: 0
    )

    private data class Snapshot(val funds: List<FundEntity>, val categories: List<CategoryEntity>, val transactions: List<TransactionEntity>, val transfers: List<TransferEntity>, val summary: BackupSummary)

    private fun JSONObject.requiredArray(name: String): List<JSONObject>? {
        val array = optJSONArray(name) ?: return null
        return buildList {
            repeat(array.length()) { index -> add(array.optJSONObject(index) ?: return null) }
        }
    }
    @JvmName("uniqueFundIds") private fun List<FundEntity>.uniqueIds() = map { it.id }.all { it.isNotBlank() } && map { it.id }.distinct().size == size
    @JvmName("uniqueCategoryIds") private fun List<CategoryEntity>.uniqueIds() = map { it.id }.all { it.isNotBlank() } && map { it.id }.distinct().size == size
    @JvmName("uniqueTransactionIds") private fun List<TransactionEntity>.uniqueIds() = map { it.id }.all { it.isNotBlank() } && map { it.id }.distinct().size == size
    @JvmName("uniqueTransferIds") private fun List<TransferEntity>.uniqueIds() = map { it.id }.all { it.isNotBlank() } && map { it.id }.distinct().size == size

    private fun FundEntity.toJson() = JSONObject().apply { put("id", id); put("name", name); put("icon", icon); put("colorHex", colorHex); put("sourceName", sourceName); put("startingBalance", startingBalance); put("createdAt", createdAt); put("updatedAt", updatedAt) }
    private fun CategoryEntity.toJson() = JSONObject().apply { put("id", id); put("name", name); put("icon", icon); put("type", type); put("createdAt", createdAt); put("updatedAt", updatedAt) }
    private fun TransactionEntity.toJson() = JSONObject().apply { put("id", id); put("fundId", fundId); put("amount", amount); put("type", type); put("categoryId", categoryId); put("description", description); put("merchant", merchant); put("upiId", upiId); put("txnId", txnId); put("paymentMethod", paymentMethod); put("googleTransactionId", googleTransactionId); put("paymentApp", paymentApp); put("status", status); put("source", source); put("date", date); put("importBatchId", importBatchId); put("createdAt", createdAt); put("updatedAt", updatedAt) }
    private fun TransferEntity.toJson() = JSONObject().apply { put("id", id); put("fromFundId", fromFundId); put("toFundId", toFundId); put("amount", amount); put("note", note); put("date", date); put("createdAt", createdAt); put("updatedAt", updatedAt) }

    private fun fundFromJson(o: JSONObject) = FundEntity(o.string("id"), o.string("name"), o.string("icon"), o.string("colorHex"), o.string("sourceName"), o.optDouble("startingBalance"), o.optLong("createdAt"), o.optLong("updatedAt"))
    private fun categoryFromJson(o: JSONObject) = CategoryEntity(o.string("id"), o.string("name"), o.string("icon"), o.string("type"), o.optLong("createdAt"), o.optLong("updatedAt"))
    private fun transactionFromJson(o: JSONObject) = TransactionEntity(o.string("id"), o.string("fundId"), o.optDouble("amount"), o.string("type"), o.string("categoryId"), o.optString("description"), o.optString("merchant"), o.optString("upiId"), o.optString("txnId"), o.optString("paymentMethod"), o.optString("googleTransactionId"), o.optString("paymentApp"), o.optString("status"), o.string("source"), o.optLong("date"), o.optString("importBatchId"), o.optLong("createdAt"), o.optLong("updatedAt"))
    private fun transferFromJson(o: JSONObject) = TransferEntity(o.string("id"), o.string("fromFundId"), o.string("toFundId"), o.optDouble("amount"), o.string("note"), o.optLong("date"), o.optLong("createdAt"), o.optLong("updatedAt"))
    private fun JSONObject.string(name: String): String = optString(name).takeIf { it.isNotBlank() } ?: throw IllegalArgumentException("Missing $name")

    private companion object { const val BACKUP_VERSION = 1 }
}
