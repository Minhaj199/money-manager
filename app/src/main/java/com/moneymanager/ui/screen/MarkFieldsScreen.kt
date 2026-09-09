package com.moneymanager.ui.screen

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.moneymanager.ui.viewmodel.MarkableField
import com.moneymanager.ui.viewmodel.OcrBlock
import com.moneymanager.ui.viewmodel.OcrViewModel

/**
 * Screen shown when automatic parsing could not confidently determine one or more fields.
 * The user taps OCR text blocks on the screenshot to identify each missing field.
 */
@Composable
fun MarkFieldsScreen(
    bitmap: Bitmap,
    blocks: List<OcrBlock>,
    missingFields: List<MarkableField>,
    manualValues: Map<MarkableField, String>,
    onFieldSelected: (MarkableField, String) -> Boolean,
    onResetField: (MarkableField) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit
) {
    // Which field is currently being marked (null = summary view).
    var activeField by remember { mutableStateOf<MarkableField?>(null) }
    var extractionError by remember { mutableStateOf(false) }

    // When a new missing field arrives and none is active, auto-select the first one.
    LaunchedEffect(missingFields) {
        if (activeField == null && missingFields.isNotEmpty()) {
            activeField = missingFields.first()
        }
        extractionError = false
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (activeField != null) "Mark ${activeField!!.label()}" else "Mark Fields",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (activeField != null) activeField = null else onBack()
                    }) { Icon(Icons.Default.ArrowBack, "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (activeField != null) {
                FieldMarkingView(
                    field = activeField!!,
                    bitmap = bitmap,
                    blocks = blocks,
                    extractionError = extractionError,
                    onBlockTapped = { block ->
                        extractionError = false
                        val ok = onFieldSelected(activeField!!, block.text)
                        if (!ok) {
                            extractionError = true
                        } else {
                            activeField = null
                        }
                    }
                )
            } else {
                FieldSummaryView(
                    missingFields = missingFields,
                    manualValues = manualValues,
                    onMarkField = { activeField = it },
                    onChangeField = { field ->
                        onResetField(field)
                        activeField = field
                    },
                    onContinue = onContinue
                )
            }
        }
    }
}

// ── Field marking view (screenshot + tappable blocks) ────────────────────────

@Composable
private fun FieldMarkingView(
    field: MarkableField,
    bitmap: Bitmap,
    blocks: List<OcrBlock>,
    extractionError: Boolean,
    onBlockTapped: (OcrBlock) -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(
                    "Tap the ${field.label().lowercase()} in the screenshot",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (extractionError) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Couldn't read the ${field.label().lowercase()} from that selection. Please try again.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Screenshot with overlaid tappable OCR blocks.
        TappableScreenshot(
            bitmap = bitmap,
            blocks = blocks,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            onBlockTapped = onBlockTapped
        )
    }
}

@Composable
private fun TappableScreenshot(
    bitmap: Bitmap,
    blocks: List<OcrBlock>,
    modifier: Modifier = Modifier,
    onBlockTapped: (OcrBlock) -> Unit
) {
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    val bitmapW = bitmap.width.toFloat()
    val bitmapH = bitmap.height.toFloat()

    Box(modifier = modifier) {
        androidx.compose.foundation.Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Receipt screenshot",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { imageSize = it.size }
        )

        // Overlay tappable hit areas for each OCR block.
        if (imageSize != IntSize.Zero) {
            val scaleX = imageSize.width / bitmapW
            val scaleY = imageSize.height / bitmapH

            blocks.forEach { block ->
                val left = block.bounds.left * scaleX
                val top = block.bounds.top * scaleY
                val width = block.bounds.width() * scaleX
                val height = block.bounds.height() * scaleX

                Box(
                    modifier = Modifier
                        .offset(x = left.dp, y = top.dp)
                        .size(width = width.dp, height = height.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(2.dp)
                        )
                        .clickable { onBlockTapped(block) }
                )
            }
        }
    }
}

// ── Field summary view (list of fields + Mark/Change buttons) ─────────────────

@Composable
private fun FieldSummaryView(
    missingFields: List<MarkableField>,
    manualValues: Map<MarkableField, String>,
    onMarkField: (MarkableField) -> Unit,
    onChangeField: (MarkableField) -> Unit,
    onContinue: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Fields that still need marking.
        if (missingFields.isNotEmpty()) {
            Text(
                "Some fields could not be detected automatically.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            missingFields.forEach { field ->
                FieldMarkRow(
                    label = field.label(),
                    value = null,
                    onAction = { onMarkField(field) },
                    actionLabel = "Mark ${field.label()}"
                )
            }
        }

        // Fields already manually confirmed.
        val confirmedFields = MarkableField.entries.filter { it in manualValues }
        if (confirmedFields.isNotEmpty()) {
            if (missingFields.isNotEmpty()) Divider()
            confirmedFields.forEach { field ->
                val displayValue = if (field == MarkableField.AMOUNT)
                    "₹${manualValues[field]}" else manualValues[field].orEmpty()
                FieldMarkRow(
                    label = field.label(),
                    value = displayValue,
                    onAction = { onChangeField(field) },
                    actionLabel = "Change"
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text("Continue", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FieldMarkRow(
    label: String,
    value: String?,
    onAction: () -> Unit,
    actionLabel: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (value != null) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                if (value != null) {
                    Text(value, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text("—", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

private fun MarkableField.label() = when (this) {
    MarkableField.AMOUNT -> "Amount"
    MarkableField.DESCRIPTION -> "Description"
}
