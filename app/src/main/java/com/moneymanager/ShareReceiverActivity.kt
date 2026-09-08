package com.moneymanager

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneymanager.ui.screen.OcrReviewScreen
import com.moneymanager.ui.screen.PdfReviewScreen
import com.moneymanager.ui.screen.ImageImportRoute
import com.moneymanager.ui.screen.PdfImportRoute
import com.moneymanager.ui.theme.MoneyManagerTheme
import com.moneymanager.ui.viewmodel.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent?.getParcelableExtra<Uri>(android.content.Intent.EXTRA_STREAM)
        val mimeType = intent?.type ?: ""

        setContent {
            MoneyManagerTheme {
                when {
                    mimeType.startsWith("image/") && uri != null ->
                        ImageImportRoute(uri, onBack = ::finish)
                    mimeType == "application/pdf" && uri != null ->
                        PdfImportRoute(uri, onBack = ::finish)
                    else -> {
                        LaunchedEffect(Unit) { finish() }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageShareHandler(uri: Uri, onDone: () -> Unit) {
    val ocrViewModel: OcrViewModel = hiltViewModel()
    val state by ocrViewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uri) {
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it)
        }
        if (bitmap != null) ocrViewModel.processImage(bitmap)
        else onDone()
    }

    when (val s = state) {
        is OcrState.Idle, is OcrState.Processing -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator()
                    Text("Reading screenshot…")
                }
            }
        }
        is OcrState.Ready -> OcrReviewScreen(parsed = s.parsed, onBack = onDone)
        is OcrState.Error -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Could not read screenshot", style = MaterialTheme.typography.titleMedium)
                    Text(s.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Button(onClick = onDone, shape = RoundedCornerShape(12.dp)) { Text("Close") }
                }
            }
        }
    }
}

@Composable
private fun PdfShareHandler(uri: Uri, onDone: () -> Unit) {
    val pdfViewModel: PdfReviewViewModel = hiltViewModel()
    val fundViewModel: FundViewModel = hiltViewModel()
    val context = LocalContext.current
    val funds by fundViewModel.funds.collectAsState()

    LaunchedEffect(uri) {
        context.contentResolver.openInputStream(uri)?.let { stream ->
            val defaultFundId = funds.firstOrNull()?.id ?: ""
            pdfViewModel.loadPdf(stream, defaultFundId)
        }
    }

    PdfReviewScreen(funds = funds, onBack = onDone, viewModel = pdfViewModel)
}
