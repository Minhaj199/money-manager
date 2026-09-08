package com.moneymanager.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneymanager.domain.model.TxnSource
import com.moneymanager.domain.model.TxnType
import com.moneymanager.ui.component.DropdownField
import com.moneymanager.ui.component.DuplicateWarningDialog
import com.moneymanager.ui.viewmodel.AddTransactionViewModel

@Composable
fun AddTransactionScreen(
    onBack: () -> Unit,
    source: TxnSource = TxnSource.MANUAL,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.saved) { if (state.saved) onBack() }

    if (state.duplicateWarning.isNotEmpty()) {
        DuplicateWarningDialog(
            onDismiss = { viewModel.update { copy(duplicateWarning = emptyList()) } },
            onConfirm = { viewModel.dismissDuplicateAndSave() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Transaction", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Type toggle
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TxnType.entries.forEach { type ->
                    FilterChip(
                        selected = state.type == type,
                        onClick = { viewModel.selectType(type) },
                        label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            // Amount
            OutlinedTextField(
                value = state.amount,
                onValueChange = { viewModel.update { copy(amount = it) } },
                label = { Text("Amount (₹)") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text("₹") }
            )

            // Fund
            DropdownField(
                label = "Fund",
                selected = state.funds.find { it.id == state.selectedFundId },
                items = state.funds,
                itemLabel = { "${it.icon} ${it.name}" },
                onSelect = { viewModel.update { copy(selectedFundId = it.id) } }
            )

            // Category
            val filteredCats = state.categories.filter {
                it.type == state.type.name || it.type == "BOTH"
            }
            DropdownField(
                label = "Category",
                selected = filteredCats.find { it.id == state.selectedCategoryId },
                items = filteredCats,
                itemLabel = { "${it.icon} ${it.name}" },
                onSelect = { viewModel.update { copy(selectedCategoryId = it.id) } }
            )

            // Merchant / Description
            OutlinedTextField(
                value = state.merchant,
                onValueChange = { viewModel.update { copy(merchant = it) } },
                label = { Text("Merchant / Person") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.description,
                onValueChange = { viewModel.update { copy(description = it) } },
                label = { Text("Description (optional)") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                LaunchedEffect(it) { viewModel.clearError() }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.save(source) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Save Transaction", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
