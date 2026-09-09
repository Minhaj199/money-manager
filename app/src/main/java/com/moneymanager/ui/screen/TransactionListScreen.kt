package com.moneymanager.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import com.moneymanager.ui.component.TransactionRow
import com.moneymanager.ui.component.DropdownField
import com.moneymanager.ui.viewmodel.TransactionListViewModel
import com.moneymanager.domain.model.TxnType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionListScreen(
    categories: List<Category>,
    funds: List<Fund>,
    onTransactionClick: (String) -> Unit,
    viewModel: TransactionListViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsState()
    var search by remember { mutableStateOf("") }
    var fundFilter by remember { mutableStateOf("All funds") }
    var sourceFilter by remember { mutableStateOf("All sources") }
    var categoryFilter by remember { mutableStateOf("All categories") }
    var typeFilter by remember { mutableStateOf("All types") }
    var dateFilter by remember { mutableStateOf("") }
    val visible = transactions.filter { txn ->
        val fund = funds.find { it.id == txn.fundId }
        val category = categories.find { it.id == txn.categoryId }
        val haystack = listOf(txn.description, txn.merchant, txn.upiId, txn.txnId, txn.googleTransactionId).joinToString(" ").lowercase()
        (search.isBlank() || haystack.contains(search.lowercase())) &&
            (fundFilter == "All funds" || fund?.id == fundFilter) &&
            (sourceFilter == "All sources" || fund?.sourceName == sourceFilter) &&
            (categoryFilter == "All categories" || category?.id == categoryFilter) &&
            (typeFilter == "All types" || txn.type.name == typeFilter) &&
            (dateFilter.isBlank() || SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(txn.date)).contains(dateFilter))
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("All Transactions", fontWeight = FontWeight.Bold) })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(search, { search = it }, label = { Text("Search transactions") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    DropdownField("Fund", fundFilter, listOf("All funds") + funds.map { it.id }, { item -> if (item == "All funds") item else funds.find { it.id == item }?.name ?: item }, { fundFilter = it })
                    DropdownField("Fund source", sourceFilter, listOf("All sources") + funds.map { it.sourceName }.filter { it.isNotBlank() }.distinct(), { it }, { sourceFilter = it })
                    DropdownField("Category", categoryFilter, listOf("All categories") + categories.map { it.id }, { item -> if (item == "All categories") item else categories.find { it.id == item }?.name ?: item }, { categoryFilter = it })
                    DropdownField("Transaction type", typeFilter, listOf("All types") + TxnType.entries.map { it.name }, { it.lowercase().replaceFirstChar(Char::uppercase) }, { typeFilter = it })
                    OutlinedTextField(dateFilter, { dateFilter = it }, label = { Text("Date filter (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            }
            items(visible) { txn ->
                val fund = funds.find { it.id == txn.fundId }
                val cat = categories.find { it.id == txn.categoryId }
                TransactionRow(
                    txn = txn,
                    fundName = fund?.name ?: "—",
                    categoryIcon = cat?.icon ?: "📦",
                    onClick = { onTransactionClick(txn.id) }
                )
                HorizontalDivider(
                    Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
            if (visible.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Text("No transactions yet.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}
