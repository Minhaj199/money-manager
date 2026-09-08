package com.moneymanager.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneymanager.domain.model.Fund
import com.moneymanager.ui.component.FundCard
import com.moneymanager.ui.viewmodel.FundViewModel

@Composable
fun FundListScreen(
    onAddFund: () -> Unit,
    onFundClick: (String) -> Unit,
    onEditFund: (String) -> Unit,
    viewModel: FundViewModel = hiltViewModel()
) {
    val funds by viewModel.funds.collectAsState()
    var fundToDelete by remember { mutableStateOf<Fund?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Funds", fontWeight = FontWeight.Bold) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddFund) {
                Icon(Icons.Default.Add, "Add fund")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 100.dp, top = 8.dp)
        ) {
            items(funds) { fund ->
                Box {
                    FundCard(
                        fund = fund,
                        onClick = { onFundClick(fund.id) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                    Row(
                        modifier = Modifier.align(Alignment.TopEnd).padding(top = 14.dp, end = 24.dp)
                    ) {
                        IconButton(onClick = { onEditFund(fund.id) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        }
                        IconButton(onClick = { fundToDelete = fund }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                        }
                    }
                }
            }
            if (funds.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💰", style = MaterialTheme.typography.headlineLarge)
                            Spacer(Modifier.height(12.dp))
                            Text("No funds yet", style = MaterialTheme.typography.titleMedium)
                            Text("Tap + to create your first fund", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }

    fundToDelete?.let { fund ->
        AlertDialog(
            onDismissRequest = { fundToDelete = null },
            title = { Text("Delete ${fund.name}?") },
            text = { Text("This will not delete existing transactions.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteFund(fund); fundToDelete = null }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { fundToDelete = null }) { Text("Cancel") } }
        )
    }
}
