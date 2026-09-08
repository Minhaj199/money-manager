package com.moneymanager.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.moneymanager.domain.model.ParsedTransaction
import com.moneymanager.parsing.ScreenshotParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed class OcrState {
    object Idle : OcrState()
    object Processing : OcrState()
    data class Ready(val parsed: ParsedTransaction) : OcrState()
    data class Error(val message: String) : OcrState()
}

@HiltViewModel
class OcrViewModel @Inject constructor(private val parser: ScreenshotParser) : ViewModel() {

    private val _state = MutableStateFlow<OcrState>(OcrState.Idle)
    val state: StateFlow<OcrState> = _state.asStateFlow()

    fun processImage(bitmap: Bitmap) {
        _state.value = OcrState.Processing
        viewModelScope.launch {
            try {
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val result = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
                val parsed = parser.parse(result.text)
                _state.value = OcrState.Ready(parsed)
            } catch (e: Exception) {
                _state.value = OcrState.Error(e.message ?: "OCR failed")
            }
        }
    }

    fun showError(message: String) {
        _state.value = OcrState.Error(message)
    }
}
