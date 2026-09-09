package com.moneymanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.repository.TransactionRepository
import com.moneymanager.domain.usecase.SaveTransactionUseCase
import com.moneymanager.domain.model.ParsedTransaction
import com.moneymanager.domain.model.Transaction
import com.moneymanager.domain.model.TxnSource
import com.moneymanager.domain.model.TxnType
import com.moneymanager.parsing.PdfParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject

data class PdfReviewItem(
    val parsed: ParsedTransaction,
    val selected: Boolean = true,
    val fundId: String = "",
    val categoryId: String = "",
    val isDuplicate: Boolean = false
)

data class PdfReviewState(
    val items: List<PdfReviewItem> = emptyList(),
    val loading: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class PdfReviewViewModel @Inject constructor(
    private val pdfParser: PdfParser,
    private val txnRepo: TransactionRepository,
    private val saveTransaction: SaveTransactionUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(PdfReviewState())
    val state: StateFlow<PdfReviewState> = _state.asStateFlow()

    fun loadPdf(stream: InputStream, defaultFundId: String) {
        _state.update { it.copy(loading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val parsed = stream.use { pdfParser.parse(it) }
                val items = parsed.map { p ->
                    val dupes = txnRepo.findDuplicates(
                        Transaction(
                            id = "", fundId = defaultFundId, amount = p.amount ?: 0.0,
                            type = p.type ?: TxnType.EXPENSE, categoryId = "",
                            description = p.description, merchant = p.merchant,
                            txnId = p.txnId, date = p.date ?: System.currentTimeMillis()
                        )
                    )
                    PdfReviewItem(
                        parsed = p,
                        selected = dupes.isEmpty(),
                        fundId = defaultFundId,
                        categoryId = if (p.type == TxnType.INCOME) "other_inc" else "other_exp",
                        isDuplicate = dupes.isNotEmpty()
                    )
                }
                _state.update { it.copy(items = items, loading = false, error = null) }
            } catch (exception: Exception) {
                _state.update {
                    it.copy(loading = false, error = exception.message ?: "Unable to read this PDF")
                }
            }
        }
    }

    fun toggleItem(index: Int) = _state.update { s ->
        s.copy(items = s.items.toMutableList().also { it[index] = it[index].copy(selected = !it[index].selected) })
    }

    fun updateFund(index: Int, fundId: String) = _state.update { s ->
        s.copy(items = s.items.toMutableList().also { it[index] = it[index].copy(fundId = fundId) })
    }

    fun showError(message: String) = _state.update { it.copy(loading = false, error = message) }

    fun saveSelected() {
        val selected = _state.value.items.filter { it.selected && it.fundId.isNotBlank() }
        viewModelScope.launch {
            val txns = selected.map { item ->
                Transaction(
                    id = UUID.randomUUID().toString(),
                    fundId = item.fundId,
                    amount = item.parsed.amount ?: 0.0,
                    type = item.parsed.type ?: TxnType.EXPENSE,
                    categoryId = item.categoryId.ifBlank { "other_exp" },
                    description = item.parsed.description,
                    merchant = item.parsed.merchant,
                    upiId = item.parsed.upiId,
                    txnId = item.parsed.txnId,
                    source = TxnSource.PDF,
                    date = item.parsed.date ?: System.currentTimeMillis()
                )
            }
            txns.forEach { saveTransaction(it) }
            _state.update { it.copy(saved = true) }
        }
    }
}
