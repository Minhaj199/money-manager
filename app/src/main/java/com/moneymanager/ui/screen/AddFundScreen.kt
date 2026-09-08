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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneymanager.ui.theme.FundColors
import com.moneymanager.ui.viewmodel.FundViewModel

val FundIcons = listOf("💰","🏠","✈️","🎓","💼","🛒","💊","🎬","🚗","📱","🍽️","🎁","💡","🏋️","📚")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddFundScreen(
    editFundId: String?,
    onBack: () -> Unit,
    viewModel: FundViewModel = hiltViewModel()
) {
    val funds by viewModel.funds.collectAsState()
    val existing = remember(editFundId, funds) { funds.find { it.id == editFundId } }

    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var icon by remember(existing) { mutableStateOf(existing?.icon ?: "💰") }
    var colorIndex by remember { mutableIntStateOf(0) }
    val colorHex = remember(colorIndex) { viewModel.nextColor(colorIndex) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing != null) "Edit Fund" else "New Fund") },
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
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Fund Name") },
                placeholder = { Text("e.g. Home Expense") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Icon", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FundIcons.forEach { ic ->
                    FilterChip(
                        selected = icon == ic,
                        onClick = { icon = ic },
                        label = { Text(ic) }
                    )
                }
            }

            Text("Color", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FundColors.forEachIndexed { i, color ->
                    FilterChip(
                        selected = colorIndex == i,
                        onClick = { colorIndex = i },
                        label = { Text("  ") },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = color.copy(alpha = 0.3f),
                            selectedContainerColor = color.copy(alpha = 0.8f)
                        )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        viewModel.saveFund(existing?.id, name.trim(), icon, colorHex)
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = name.isNotBlank()
            ) {
                Text(if (existing != null) "Save Changes" else "Create Fund")
            }
        }
    }
}
