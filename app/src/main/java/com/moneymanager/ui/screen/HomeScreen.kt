package com.moneymanager.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneymanager.domain.model.Category
import com.moneymanager.ui.component.FundCard
import com.moneymanager.ui.component.TransactionRow
import com.moneymanager.ui.component.formatAmount
import com.moneymanager.ui.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    onAddTransaction: () -> Unit,
    onAddIncome: () -> Unit,
    onFundClick: (String) -> Unit,
    onTransfer: () -> Unit,
    onImportScreenshot: () -> Unit,
    onImportPdf: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onSettings: () -> Unit,
    categories: List<Category>,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var showQuickAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDark) "Switch to light mode" else "Switch to dark mode"
                        )
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showQuickAdd = true }, icon = { Icon(Icons.Default.Add, null) }, text = { Text("Add") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column {
                    Text(greeting(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(SimpleDateFormat("d MMMM", Locale.getDefault()).format(Date()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(20.dp))
                    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(22.dp)) {
                            Text("TOTAL BALANCE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .70f))
                            Spacer(Modifier.height(4.dp))
                            Text("₹${formatAmount(state.totalBalance)}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.height(12.dp))
                            Text("Across ${state.funds.size} ${if (state.funds.size == 1) "fund" else "funds"}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = .75f))
                        }
                    }
                }
            }
            item {
                Surface(onClick = onImportScreenshot, shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .14f), modifier = Modifier.size(48.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.PhotoCamera, null, tint = MaterialTheme.colorScheme.onPrimary) }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Add payment instantly", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                            Text("Share a payment screenshot", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .76f))
                        }
                        Text("→", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
            item { SectionTitle("Your funds") }
            items(state.funds, key = { it.id }) { fund -> FundCard(fund, { onFundClick(fund.id) }) }
            if (state.funds.isEmpty()) item {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .52f), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Give every rupee a job", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text("Create a fund for home, savings, travel, or anything else.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { Spacer(Modifier.height(4.dp)); SectionTitle("Recent transactions") }
            items(state.recentTransactions, key = { it.id }) { txn ->
                val fund = state.funds.find { it.id == txn.fundId }
                val category = categories.find { it.id == txn.categoryId }
                TransactionRow(txn, fund?.name ?: "Unassigned", category?.icon ?: "•", onClick = { onTransactionClick(txn.id) })
            }
            if (state.recentTransactions.isEmpty()) item {
                Text("Your latest payments will appear here.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp))
            }
        }
    }

    if (showQuickAdd) {
        ModalBottomSheet(onDismissRequest = { showQuickAdd = false }) {
            Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Quick add", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Choose how you want to add money movement.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                QuickAddOption("Import a screenshot", "Read payment details automatically", Icons.Default.PhotoCamera, true) { showQuickAdd = false; onImportScreenshot() }
                QuickAddOption("Add expense", "Record a payment manually", Icons.Default.Add, false) { showQuickAdd = false; onAddTransaction() }
                QuickAddOption("Add income", "Record money coming in", Icons.Default.Add, false) { showQuickAdd = false; onAddIncome() }
                QuickAddOption("Transfer money", "Move money between funds", Icons.Default.SwapHoriz, false) { showQuickAdd = false; onTransfer() }
                QuickAddOption("Import PDF", "Review a bank statement", Icons.Default.PictureAsPdf, false) { showQuickAdd = false; onImportPdf() }
            }
        }
    }
}

@Composable private fun SectionTitle(text: String) = Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

@Composable
private fun QuickAddOption(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, featured: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = if (featured) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .48f), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (featured) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(14.dp))
            Column { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 0..11 -> "Good morning 👋"
    in 12..16 -> "Good afternoon 👋"
    else -> "Good evening 👋"
}
