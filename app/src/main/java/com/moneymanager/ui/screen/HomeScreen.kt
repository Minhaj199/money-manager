package com.moneymanager.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneymanager.domain.model.Category
import com.moneymanager.domain.model.Fund
import com.moneymanager.ui.component.FundCard
import com.moneymanager.ui.component.TransactionRow
import com.moneymanager.ui.component.formatAmount
import com.moneymanager.ui.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    onAddTransaction: () -> Unit,
    onFundClick: (String) -> Unit,
    onTransfer: () -> Unit,
    onImportScreenshot: () -> Unit,
    onImportPdf: () -> Unit,
    categories: List<Category>,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallFloatingActionButton(
                    onClick = onTransfer,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) { Icon(Icons.Default.SwapHoriz, "Transfer") }
                SmallFloatingActionButton(
                    onClick = onImportPdf,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ) { Icon(Icons.Default.PictureAsPdf, "Import PDF statement") }
                SmallFloatingActionButton(
                    onClick = onImportScreenshot,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) { Icon(Icons.Default.PhotoCamera, "Scan payment screenshot") }
                FloatingActionButton(onClick = onAddTransaction) {
                    Icon(Icons.Default.Add, "Add transaction")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                    Text(greeting(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(20.dp))
                    Text("Total Available", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                    Text(
                        "₹${formatAmount(state.totalBalance)}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))
                    FilledTonalButton(onClick = onImportScreenshot) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Scan payment screenshot")
                    }
                    TextButton(onClick = onImportPdf) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Import a PDF statement")
                    }
                }
            }

            item {
                Text(
                    "Your Funds",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            items(state.funds) { fund ->
                FundCard(
                    fund = fund,
                    onClick = { onFundClick(fund.id) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            if (state.funds.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No funds yet. Tap + to get started.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            items(state.recentTransactions) { txn ->
                val fund = state.funds.find { it.id == txn.fundId }
                val cat = categories.find { it.id == txn.categoryId }
                TransactionRow(
                    txn = txn,
                    fundName = fund?.name ?: "—",
                    categoryIcon = cat?.icon ?: "📦"
                )
                HorizontalDivider(
                    Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            if (state.recentTransactions.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No transactions yet.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}

private fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning 👋"
        hour < 17 -> "Good afternoon 👋"
        else -> "Good evening 👋"
    }
}
