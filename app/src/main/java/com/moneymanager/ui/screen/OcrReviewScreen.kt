package com.moneymanager.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneymanager.domain.model.ParsedTransaction
import com.moneymanager.domain.model.TxnSource
import com.moneymanager.ui.component.DropdownField
import com.moneymanager.ui.component.DuplicateWarningDialog
import com.moneymanager.ui.viewmodel.AddTransactionViewModel

/**
 * Review screen shown after OCR parsing a screenshot.
 * All fields are pre-filled but fully editable before saving.
 */
@Composable
fun OcrReviewScreen(
    parsed: ParsedTransaction,
    onBack: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Pre-fill once when funds are loaded
    LaunchedEffect(state.funds) {
        if (state.funds.isNotEmpty() && state.amount.isBlank()) {
            viewModel.prefill(parsed, state.funds)
        }
    }
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
                title = { Text("Review Transaction", fontWeight = FontWeight.SemiBold) },
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
            // Parsed badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "📸 Parsed from screenshot — review before saving",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            // Amount display
            if (state.amount.isNotBlank()) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        "₹${state.amount}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Reuse AddTransactionScreen fields via shared ViewModel
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.moneymanager.domain.model.TxnType.entries.forEach { type ->
                    FilterChip(
                        selected = state.type == type,
                        onClick = { viewModel.selectType(type) },
                        label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            DropdownField(
                label = "Fund",
                selected = state.funds.find { it.id == state.selectedFundId },
                items = state.funds,
                itemLabel = { "${it.icon} ${it.name}" },
                onSelect = { viewModel.update { copy(selectedFundId = it.id) } }
            )

            OutlinedTextField(
                value = state.amount,
                onValueChange = { viewModel.update { copy(amount = it) } },
                label = { Text("Amount") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            val filteredCats = state.categories.filter { it.type == state.type.name || it.type == "BOTH" }
            DropdownField(
                label = "Category",
                selected = filteredCats.find { it.id == state.selectedCategoryId },
                items = filteredCats,
                itemLabel = { "${it.icon} ${it.name}" },
                onSelect = { viewModel.update { copy(selectedCategoryId = it.id) } }
            )

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
                label = { Text("Description") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.upiId,
                onValueChange = { viewModel.update { copy(upiId = it) } },
                label = { Text("UPI ID (optional)") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.txnId,
                onValueChange = { viewModel.update { copy(txnId = it) } },
                label = { Text("Transaction ID (optional)") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.dateText,
                onValueChange = { viewModel.update { copy(dateText = it) } },
                label = { Text("Date (e.g. 8 Sep 2026)") },
                placeholder = { Text("Leave blank for now") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.save(TxnSource.SCREENSHOT) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Confirm Transaction", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
