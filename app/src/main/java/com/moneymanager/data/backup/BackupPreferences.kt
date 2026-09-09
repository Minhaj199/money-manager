package com.moneymanager.data.backup

import android.content.Context
import android.net.Uri
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class BackupSettings(
    val destination: Uri? = null,
    val automatic: Boolean = false,
    val lastBackupAt: Long = 0L,
    val lastStatus: String = "Not connected"
)

@Singleton
class BackupPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private val prefs = context.getSharedPreferences("backup_settings", Context.MODE_PRIVATE)

    fun read() = BackupSettings(
        destination = prefs.getString("destination", null)?.let(Uri::parse),
        automatic = prefs.getBoolean("automatic", false),
        lastBackupAt = prefs.getLong("last_backup_at", 0L),
        lastStatus = prefs.getString("last_status", "Not connected").orEmpty()
    )

    fun setDestination(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        prefs.edit().putString("destination", uri.toString()).putString("last_status", "Connected").apply()
    }

    fun clearDestination() = prefs.edit().remove("destination").putString("last_status", "Not connected").apply()

    fun setAutomatic(enabled: Boolean) {
        prefs.edit().putBoolean("automatic", enabled).apply()
        if (enabled) scheduleDaily() else WorkManager.getInstance(context).cancelUniqueWork(AUTO_BACKUP_WORK)
    }

    fun record(result: BackupResult) = prefs.edit().apply {
        when (result) {
            is BackupResult.Success -> putLong("last_backup_at", result.summary.createdAt).putString("last_status", "Backup completed successfully")
            is BackupResult.Failure -> putString("last_status", result.message)
        }
    }.apply()

    private fun scheduleDaily() {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            AUTO_BACKUP_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<AutomaticBackupWorker>(1, TimeUnit.DAYS).build()
        )
    }

    private companion object { const val AUTO_BACKUP_WORK = "money-manager-automatic-backup" }
}
