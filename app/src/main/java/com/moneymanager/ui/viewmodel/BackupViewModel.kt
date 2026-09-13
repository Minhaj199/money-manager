package com.moneymanager.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.backup.BackupManager
import com.moneymanager.data.backup.BackupPreferences
import com.moneymanager.data.backup.BackupResult
import com.moneymanager.data.backup.BackupSettings
import com.moneymanager.data.preferences.NotificationPreferences
import com.moneymanager.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupUiState(
    val settings: BackupSettings = BackupSettings(),
    val working: Boolean = false,
    val message: String = "",
    val pendingRestore: Uri? = null,
    val resetStage: Int = 0,
    val resetPhrase: String = "",
    val restoreSummaryText: String = "",
    val notificationsEnabled: Boolean = true
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val manager: BackupManager,
    private val preferences: BackupPreferences,
    private val categoryRepo: CategoryRepository,
    private val notificationPreferences: NotificationPreferences
) : ViewModel() {
    private val _state = MutableStateFlow(BackupUiState(settings = preferences.read()))
    val state: StateFlow<BackupUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            notificationPreferences.isTransactionSuccessEnabled.collect { enabled ->
                _state.update { it.copy(notificationsEnabled = enabled) }
            }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { notificationPreferences.setTransactionSuccessEnabled(enabled) }
    }

    fun setDestinationAndBackup(uri: Uri) {
        preferences.setDestination(uri)
        backupNow()
    }

    fun backupNow() {
        val destination = preferences.read().destination
        if (destination == null) { _state.update { it.copy(message = "Choose a Google Drive or other document destination first.") }; return }
        _state.update { it.copy(working = true, message = "Backing up…") }
        viewModelScope.launch {
            val result = manager.writeTo(destination)
            preferences.record(result)
            _state.update { it.copy(settings = preferences.read(), working = false, message = result.message()) }
        }
    }

    fun setAutomatic(enabled: Boolean) {
        preferences.setAutomatic(enabled)
        _state.update { it.copy(settings = preferences.read()) }
    }

    fun disconnect() { preferences.clearDestination(); _state.update { it.copy(settings = preferences.read()) } }
    fun selectRestore(uri: Uri) {
        val settings = preferences.read()
        val summary = if (settings.lastBackupAt == 0L) "No backup found on this device."
        else "Last backup: ${java.text.SimpleDateFormat("d MMM yyyy, h:mm a", java.util.Locale.getDefault()).format(java.util.Date(settings.lastBackupAt))}"
        _state.update { it.copy(pendingRestore = uri, message = "Restoring will replace local data.", restoreSummaryText = summary) }
    }
    fun cancelRestore() = _state.update { it.copy(pendingRestore = null) }
    fun confirmRestore() {
        val uri = _state.value.pendingRestore ?: return
        _state.update { it.copy(working = true, pendingRestore = null, message = "Restoring…") }
        viewModelScope.launch {
            val result = manager.restoreFrom(uri)
            _state.update { it.copy(working = false, message = result.message()) }
        }
    }

    fun beginReset() = _state.update { it.copy(resetStage = 1) }
    fun advanceReset() = _state.update { it.copy(resetStage = it.resetStage + 1) }
    fun cancelReset() = _state.update { it.copy(resetStage = 0, resetPhrase = "") }
    fun setResetPhrase(value: String) = _state.update { it.copy(resetPhrase = value) }
    fun confirmReset() {
        _state.update { it.copy(working = true, resetStage = 0, message = "Resetting data…") }
        viewModelScope.launch {
            val result = manager.resetAllData()
            if (result is BackupResult.Success) categoryRepo.seedDefaults()
            _state.update { it.copy(working = false, resetPhrase = "", message = if (result is BackupResult.Success) "All data has been reset. Default categories restored." else result.message()) }
        }
    }

    private fun BackupResult.message() = when (this) {
        is BackupResult.Success -> "Backup completed successfully"
        is BackupResult.Failure -> message
    }
}
