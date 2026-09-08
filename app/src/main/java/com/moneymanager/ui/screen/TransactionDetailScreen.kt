package com.moneymanager.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneymanager.domain.model.Category
import com.moneymanager.domain.model.Fund
import com.moneymanager.domain.model.TxnType
import com.moneymanager.ui.component.formatAmount
import com.moneymanager.ui.viewmodel.TransactionListViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionDetailScreen(transactionId: String, funds: List<Fund>, categories: List<Category>, onBack: () -> Unit, viewModel: TransactionListViewModel = hiltViewModel()) {
    val transactions by viewModel.transactions.collectAsState()
    val transaction = transactions.find { it.id == transactionId }
    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("Transaction details", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }) { padding ->
        transaction?.let { txn ->
            val isIncome = txn.type == TxnType.INCOME
            val fund = funds.find { it.id == txn.fundId }
            val category = categories.find { it.id == txn.categoryId }
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = RoundedCornerShape(28.dp), color = if (isIncome) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(24.dp)) {
                        Text(if (isIncome) "INCOME" else "EXPENSE", style = MaterialTheme.typography.labelMedium)
                        Text("${if (isIncome) "+" else "−"}₹${formatAmount(txn.amount)}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Text(txn.merchant.ifBlank { txn.description.ifBlank { "Transaction" } }, style = MaterialTheme.typography.titleMedium)
                    }
                }
                DetailRow("Type", if (isIncome) "Income" else "Expense")
                DetailRow("Fund", fund?.let { "${it.icon} ${it.name}" } ?: "Unassigned")
                DetailRow("Category", category?.let { "${it.icon} ${it.name}" } ?: "Other")
                DetailRow("Merchant", txn.merchant.ifBlank { "—" })
                DetailRow("Description", txn.description.ifBlank { "—" })
                DetailRow("Date & time", SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(txn.date)))
                DetailRow("Source", when (txn.source.name) { "SCREENSHOT" -> "Payment screenshot"; "PDF" -> "PDF statement"; "TRANSFER" -> "Fund transfer"; else -> "Manual entry" })
                DetailRow("UPI", txn.upiId.ifBlank { "—" })
                DetailRow("Transaction ID", txn.txnId.ifBlank { "—" })
            }
        }
    }
}

@Composable private fun DetailRow(label: String, value: String) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}
