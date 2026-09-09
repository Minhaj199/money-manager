package com.moneymanager.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneymanager.domain.model.Category
import com.moneymanager.ui.viewmodel.CategoryViewModel

@Composable
fun CategoryManageScreen(
    onBack: () -> Unit,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val categories by viewModel.categories.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Category?>(null) }
    var toDelete by remember { mutableStateOf<Category?>(null) }

    if (showDialog) {
        CategoryDialog(
            initial = editing,
            onDismiss = { showDialog = false; editing = null },
            onSave = { name, icon, type ->
                viewModel.save(name, icon, type, editing?.id ?: "")
                showDialog = false; editing = null
            }
        )
    }

    toDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete \"${cat.name}\"?") },
            text = { Text("Existing transactions using this category will keep their category ID but the name won't resolve.") },
            confirmButton = { TextButton(onClick = { viewModel.delete(cat); toDelete = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showDialog = true }) {
                Icon(Icons.Default.Add, "Add category")
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (categories.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("No categories", style = MaterialTheme.typography.titleMedium)
                        Text("Tap + to create one, or restore the built-in defaults.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = viewModel::seedDefaults) { Text("Restore default categories") }
                    }
                }
            } else {
                item {
                    TextButton(onClick = viewModel::seedDefaults, modifier = Modifier.fillMaxWidth()) {
                        Text("Restore default categories")
                    }
                }
                val grouped = categories.groupBy { it.type }
                listOf("EXPENSE", "INCOME", "BOTH").forEach { type ->
                    val group = grouped[type] ?: return@forEach
                    item { Text(type.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp, start = 4.dp)) }
                    items(group, key = { it.id }) { cat ->
                        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(cat.icon, style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.width(12.dp))
                                Text(cat.name, Modifier.weight(1f), fontWeight = FontWeight.Medium)
                                IconButton(onClick = { editing = cat; showDialog = true }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { toDelete = cat }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = .8f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryDialog(initial: Category?, onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var icon by remember(initial) { mutableStateOf(initial?.icon ?: "") }
    var type by remember(initial) { mutableStateOf(initial?.type ?: "EXPENSE") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New category" else "Edit category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = icon, onValueChange = { icon = it }, label = { Text("Icon (emoji)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("EXPENSE", "INCOME", "BOTH").forEach { t ->
                        FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t.lowercase().replaceFirstChar { it.uppercase() }) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name, icon, type) }, enabled = name.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
