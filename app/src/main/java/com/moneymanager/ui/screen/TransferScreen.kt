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
import com.moneymanager.ui.component.DropdownField
import com.moneymanager.ui.viewmodel.TransferViewModel

@Composable
fun TransferScreen(
    onBack: () -> Unit,
    viewModel: TransferViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.saved) { if (state.saved) onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transfer Between Funds", fontWeight = FontWeight.SemiBold) },
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
            DropdownField(
                label = "From Fund",
                selected = state.funds.find { it.id == state.fromFundId },
                items = state.funds,
                itemLabel = { "${it.icon} ${it.name}  ·  ₹${it.balance.toLong()}" },
                onSelect = { viewModel.update { copy(fromFundId = it.id) } }
            )

            DropdownField(
                label = "To Fund",
                selected = state.funds.find { it.id == state.toFundId },
                items = state.funds.filter { it.id != state.fromFundId },
                itemLabel = { "${it.icon} ${it.name}" },
                onSelect = { viewModel.update { copy(toFundId = it.id) } }
            )

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

            OutlinedTextField(
                value = state.note,
                onValueChange = { viewModel.update { copy(note = it) } },
                label = { Text("Note (optional)") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.execute() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = state.fromFundId.isNotBlank() && state.toFundId.isNotBlank()
            ) {
                Text("Transfer", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
