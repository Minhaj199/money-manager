package com.moneymanager.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
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
import com.moneymanager.domain.model.TxnType
import com.moneymanager.ui.component.DropdownField
import com.moneymanager.ui.component.DuplicateWarningDialog
import com.moneymanager.ui.viewmodel.AddTransactionViewModel

/** A review-first confirmation screen: OCR suggests, the user decides. */
@Composable
fun OcrReviewScreen(parsed: ParsedTransaction, onBack: () -> Unit, viewModel: AddTransactionViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.funds) { if (state.funds.isNotEmpty() && state.amount.isBlank()) viewModel.prefill(parsed, state.funds) }
    LaunchedEffect(state.saved) { if (state.saved) onBack() }
    if (state.duplicateWarning.isNotEmpty()) DuplicateWarningDialog(onDismiss = { viewModel.update { copy(duplicateWarning = emptyList()) } }, onConfirm = { viewModel.dismissDuplicateAndSave() })

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Review payment", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }) },
        bottomBar = {
            Surface(shadowElevation = 10.dp) {
                Button(onClick = { viewModel.save(TxnSource.SCREENSHOT) }, modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp), shape = RoundedCornerShape(18.dp)) { Text("Confirm transaction", fontWeight = FontWeight.Bold) }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column { Text("Payment detected", fontWeight = FontWeight.Bold); Text("Review or edit every detail before saving.", style = MaterialTheme.typography.bodySmall) }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("₹${state.amount.ifBlank { "—" }}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(if (state.type == TxnType.INCOME) "Income" else "Expense", color = if (state.type == TxnType.INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleSmall)
            }
            Text("Payment details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            EditableField("Amount", state.amount, { viewModel.update { copy(amount = it) } }, KeyboardType.Decimal, "₹")
            EditableField("Merchant / Person", state.merchant, { viewModel.update { copy(merchant = it) } })
            EditableField("UPI ID", state.upiId, { viewModel.update { copy(upiId = it) } }, hint = "Not detected")
            EditableField("Date", state.dateText, { viewModel.update { copy(dateText = it) } }, hint = "Today")
            DropdownField("Fund", state.funds.find { it.id == state.selectedFundId }, state.funds, { "${it.icon} ${it.name}" }, { viewModel.update { copy(selectedFundId = it.id) } })
            val availableCategories = state.categories.filter { it.type == state.type.name || it.type == "BOTH" }
            DropdownField("Category", availableCategories.find { it.id == state.selectedCategoryId }, availableCategories, { "${it.icon} ${it.name}" }, { viewModel.update { copy(selectedCategoryId = it.id) } })
            EditableField("Description", state.description, { viewModel.update { copy(description = it) } }, hint = "Payment description")
            EditableField("Transaction ID", state.txnId, { viewModel.update { copy(txnId = it) } }, hint = "Optional")
            EditableField("Google Transaction ID", state.googleTransactionId, { viewModel.update { copy(googleTransactionId = it) } }, hint = "Not detected")
            EditableField("Time", state.time, { viewModel.update { copy(time = it) } }, hint = "Not detected")
            if (state.paymentApp.isNotBlank()) EditableField("Payment App", state.paymentApp, { viewModel.update { copy(paymentApp = it) } })
            if (state.paymentMethod.isNotBlank()) EditableField("Payment Method", state.paymentMethod, { viewModel.update { copy(paymentMethod = it) } })
            if (state.status.isNotBlank()) EditableField("Status", state.status, { viewModel.update { copy(status = it) } })
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.height(84.dp))
        }
    }
}

@Composable
private fun EditableField(label: String, value: String, onChange: (String) -> Unit, keyboardType: KeyboardType = KeyboardType.Text, hint: String = "", prefix: String = "") {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, placeholder = { if (hint.isNotBlank()) Text(hint) }, singleLine = true,
        trailingIcon = { Icon(Icons.Default.Edit, "Edit $label", modifier = Modifier.size(18.dp)) },
        prefix = { if (prefix.isNotBlank()) Text(prefix) }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType), shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()
    )
}
