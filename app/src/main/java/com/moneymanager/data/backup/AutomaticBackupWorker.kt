package com.moneymanager.data.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Daily best-effort backup. It never changes local financial data. */
class AutomaticBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val entry = EntryPointAccessors.fromApplication(applicationContext, BackupWorkerEntryPoint::class.java)
        val settings = entry.preferences().read()
        if (!settings.automatic || settings.destination == null) return Result.success()
        return when (val result = entry.backupManager().writeTo(settings.destination)) {
            is BackupResult.Success -> { entry.preferences().record(result); Result.success() }
            is BackupResult.Failure -> { entry.preferences().record(result); Result.retry() }
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BackupWorkerEntryPoint {
    fun backupManager(): BackupManager
    fun preferences(): BackupPreferences
}
