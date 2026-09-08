package com.moneymanager.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneymanager.domain.model.Category
import com.moneymanager.ui.component.TransactionRow
import com.moneymanager.ui.component.formatAmount
import com.moneymanager.ui.component.parseColor
import com.moneymanager.ui.viewmodel.FundDetailViewModel

@Composable
fun FundDetailScreen(
    fundId: String,
    categories: List<Category>,
    onBack: () -> Unit,
    onAddTransaction: (String) -> Unit,
    onTransactionClick: (String) -> Unit,
    viewModel: FundDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(fundId) { viewModel.load(fundId) }
    val fund by viewModel.fund.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(fund?.name ?: "", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAddTransaction(fundId) }) {
                Icon(Icons.Default.Add, "Add transaction")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            fund?.let { f ->
                item {
                    val color = parseColor(f.colorHex)
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
                    ) {
                        Column(Modifier.padding(22.dp)) {
                            Text("REMAINING", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .56f))
                            Text("₹${formatAmount(f.balance)}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
                            Spacer(Modifier.height(16.dp))
                            val progress = if (f.allocated > 0) (f.totalExpent / f.allocated).toFloat().coerceIn(0f, 1f) else 0f
                            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp), color = color, trackColor = color.copy(alpha = .18f))
                            Spacer(Modifier.height(8.dp))
                            Text("₹${formatAmount(f.totalExpent)} spent of ₹${formatAmount(f.allocated)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .62f))
                            Spacer(Modifier.height(18.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                StatItem("Allocated", "₹${formatAmount(f.allocated)}", color)
                                StatItem("Spent", "₹${formatAmount(f.totalExpent)}", MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            items(transactions) { txn ->
                val cat = categories.find { it.id == txn.categoryId }
                TransactionRow(txn = txn, fundName = fund?.name ?: "", categoryIcon = cat?.icon ?: "📦", onClick = { onTransactionClick(txn.id) })
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }

            if (transactions.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No transactions yet.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
    }
}
