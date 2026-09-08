package com.moneymanager.ui.screen

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneymanager.ui.viewmodel.FundViewModel
import com.moneymanager.ui.viewmodel.OcrState
import com.moneymanager.ui.viewmodel.OcrViewModel
import com.moneymanager.ui.viewmodel.PdfReviewViewModel

/** In-app counterpart to the Android share receiver. */
@Composable
fun ImageImportRoute(uri: Uri, onBack: () -> Unit) {
    val viewModel: OcrViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uri) {
        val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        if (bitmap != null) viewModel.processImage(bitmap)
        else viewModel.showError("The selected image could not be opened.")
    }

    when (val current = state) {
        OcrState.Idle, OcrState.Processing -> LoadingImport("Reading screenshot…")
        is OcrState.Ready -> OcrReviewScreen(parsed = current.parsed, onBack = onBack)
        is OcrState.Error -> ImportError(current.message, onBack)
    }
}

@Composable
fun PdfImportRoute(uri: Uri, onBack: () -> Unit) {
    val reviewViewModel: PdfReviewViewModel = hiltViewModel()
    val fundViewModel: FundViewModel = hiltViewModel()
    val funds by fundViewModel.funds.collectAsState()
    val context = LocalContext.current
    val defaultFundId = funds.firstOrNull()?.id.orEmpty()
    var loadedForFund by remember(uri) { mutableStateOf("") }

    LaunchedEffect(uri, defaultFundId) {
        if (defaultFundId.isNotBlank() && loadedForFund != defaultFundId) {
            context.contentResolver.openInputStream(uri)?.let { stream ->
                loadedForFund = defaultFundId
                reviewViewModel.loadPdf(stream, defaultFundId)
            } ?: reviewViewModel.showError("The selected PDF could not be opened.")
        }
    }

    if (funds.isEmpty()) {
        ImportError("Create a fund before importing a statement.", onBack)
    } else {
        PdfReviewScreen(funds = funds, onBack = onBack, viewModel = reviewViewModel)
    }
}

@Composable
private fun LoadingImport(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator()
            Text(message)
        }
    }
}

@Composable
private fun ImportError(message: String, onBack: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(message, style = MaterialTheme.typography.titleMedium)
            Button(onClick = onBack) { Text("Back") }
        }
    }
}
