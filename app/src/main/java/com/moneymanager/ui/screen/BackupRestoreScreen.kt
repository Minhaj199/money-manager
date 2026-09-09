package com.moneymanager.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneymanager.ui.viewmodel.BackupViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupRestoreScreen(
    onBack: () -> Unit,
    onChooseBackupDestination: () -> Unit,
    onChooseRestoreFile: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    if (state.pendingRestore != null) AlertDialog(
        onDismissRequest = viewModel::cancelRestore,
        title = { Text("Restore from backup?") },
        text = { Text("Restoring will replace the data currently stored on this device. The backup is validated before any data is changed.\n\n${state.restoreSummaryText}") },
        confirmButton = { TextButton(onClick = viewModel::confirmRestore) { Text("Restore") } },
        dismissButton = { TextButton(onClick = viewModel::cancelRestore) { Text("Cancel") } }
    )
    ResetDialogs(state.resetStage, state.resetPhrase, state.settings.lastBackupAt, viewModel::advanceReset, viewModel::cancelReset, viewModel::setResetPhrase, viewModel::confirmReset)

    Scaffold(
        topBar = { TopAppBar(title = { Text("Backup & Restore", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Google Drive", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(if (state.settings.destination == null) "Not connected. Choose a Google Drive location to connect." else "Connected to selected backup location", color = if (state.settings.destination == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) { Text("Automatic Backup", fontWeight = FontWeight.Medium); Text("Back up your complete data daily", style = MaterialTheme.typography.bodySmall) }
                Switch(checked = state.settings.automatic, onCheckedChange = viewModel::setAutomatic, enabled = state.settings.destination != null)
            }
            Text("Backup frequency: Daily", style = MaterialTheme.typography.bodyMedium)
            Text(if (state.settings.lastBackupAt == 0L) "Last backup: Never" else "Last backup: ${SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(state.settings.lastBackupAt))}")
            Text("Status: ${state.settings.lastStatus}", style = MaterialTheme.typography.bodyMedium)
            if (state.message.isNotBlank()) Text(state.message, color = if (state.message.contains("failed", true) || state.message.contains("invalid", true)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            Button(onClick = { if (state.settings.destination == null) onChooseBackupDestination() else viewModel.backupNow() }, enabled = !state.working, modifier = Modifier.fillMaxWidth()) { Text(if (state.settings.destination == null) "Choose Google Drive Location" else "Back Up Now") }
            OutlinedButton(onClick = onChooseRestoreFile, enabled = !state.working, modifier = Modifier.fillMaxWidth()) { Text("Restore from Backup") }
            if (state.settings.destination != null) TextButton(onClick = viewModel::disconnect, modifier = Modifier.fillMaxWidth()) { Text("Disconnect Google Drive") }
            HorizontalDivider()
            Text("Data Management", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Resetting permanently removes all local financial data. ${if (state.settings.lastBackupAt == 0L) "No backup found." else "A backup exists."}", style = MaterialTheme.typography.bodySmall)
            Button(onClick = viewModel::beginReset, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), modifier = Modifier.fillMaxWidth()) { Text("Reset All Data") }
        }
    }
}

@Composable
private fun ResetDialogs(stage: Int, phrase: String, lastBackupAt: Long, next: () -> Unit, cancel: () -> Unit, setPhrase: (String) -> Unit, delete: () -> Unit) {
    val lastBackupInfo = if (lastBackupAt == 0L) "No backup found. You may lose your financial records permanently."
    else "Last backup: ${SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(lastBackupAt))}"
    when (stage) {
        1 -> AlertDialog(onDismissRequest = cancel, title = { Text("Reset all data?") }, text = { Text("This permanently deletes all Money Manager funds, sources, categories, transactions, transfers, and local financial settings on this device.\n\n$lastBackupInfo") }, confirmButton = { TextButton(onClick = next) { Text("Continue") } }, dismissButton = { TextButton(onClick = cancel) { Text("Cancel") } })
        2 -> AlertDialog(onDismissRequest = cancel, title = { Text("Reset All Data") }, text = { Text("✓ All funds\n✓ All fund sources\n✓ All categories\n✓ All transactions\n✓ All transfers\n✓ Local financial settings\n\nThis action cannot be undone unless you have a backup.") }, confirmButton = { TextButton(onClick = next) { Text("I Understand, Continue") } }, dismissButton = { TextButton(onClick = cancel) { Text("Cancel") } })
        3 -> AlertDialog(onDismissRequest = cancel, title = { Text("Type confirmation phrase") }, text = { Column { Text("Type DELETE ALL DATA to continue."); OutlinedTextField(value = phrase, onValueChange = setPhrase, singleLine = true) } }, confirmButton = { TextButton(onClick = next, enabled = phrase == "DELETE ALL DATA") { Text("Permanently Delete Data") } }, dismissButton = { TextButton(onClick = cancel) { Text("Cancel") } })
        4 -> AlertDialog(onDismissRequest = cancel, title = { Text("Are you absolutely sure?") }, text = { Text("All local financial data will now be permanently deleted.") }, confirmButton = { TextButton(onClick = delete) { Text("Delete Everything") } }, dismissButton = { TextButton(onClick = cancel) { Text("Cancel") } })
    }
}
