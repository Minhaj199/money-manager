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
import com.moneymanager.domain.model.Fund
import com.moneymanager.domain.model.TxnType
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
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            StatItem("Allocated", "₹${formatAmount(f.totalIncome)}", color)
                            StatItem("Spent", "₹${formatAmount(f.totalExpent)}", MaterialTheme.colorScheme.error)
                            StatItem("Remaining", "₹${formatAmount(f.balance)}", color)
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
                TransactionRow(txn = txn, fundName = fund?.name ?: "", categoryIcon = cat?.icon ?: "📦")
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
