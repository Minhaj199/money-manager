package com.moneymanager.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.repository.CategoryRepository
import com.moneymanager.data.repository.FundRepository
import com.moneymanager.data.repository.TransactionRepository
import com.moneymanager.domain.model.*
import com.moneymanager.domain.usecase.CheckDuplicateUseCase
import com.moneymanager.domain.usecase.SaveTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class AddTxnUiState(
    val funds: List<Fund> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedFundId: String = "",
    val selectedCategoryId: String = "",
    val amount: String = "",
    val type: TxnType = TxnType.EXPENSE,
    val description: String = "",
    val merchant: String = "",
    val upiId: String = "",
    val txnId: String = "",
    /** Blank means use the moment the transaction is saved. */
    val dateText: String = "",
    val duplicateWarning: List<Transaction> = emptyList(),
    val saved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val fundRepo: FundRepository,
    private val categoryRepo: CategoryRepository,
    private val saveUseCase: SaveTransactionUseCase,
    private val checkDuplicate: CheckDuplicateUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AddTxnUiState())
    val state: StateFlow<AddTxnUiState> = _state.asStateFlow()
    private var bypassDuplicateWarning = false
    private var duplicateConfirmationSource = TxnSource.MANUAL

    init {
        val preselectedFundId = savedStateHandle.get<String>("fundId") ?: ""
        viewModelScope.launch {
            combine(fundRepo.observeFunds(), categoryRepo.observeAll()) { funds, cats ->
                _state.update {
                    it.copy(
                        funds = funds,
                        categories = cats,
                        selectedFundId = currentFundId(it.selectedFundId, preselectedFundId, funds),
                        selectedCategoryId = currentCategoryId(it.selectedCategoryId, it.type, cats)
                    )
                }
            }.collect()
        }
    }

    fun update(block: AddTxnUiState.() -> AddTxnUiState) = _state.update(block)

    fun selectType(type: TxnType) = _state.update { current ->
        val categoryStillMatches = current.categories.any {
            it.id == current.selectedCategoryId && (it.type == type.name || it.type == "BOTH")
        }
        current.copy(
            type = type,
            selectedCategoryId = if (categoryStillMatches) current.selectedCategoryId
            else current.categories.firstOrNull { it.type == type.name || it.type == "BOTH" }?.id.orEmpty()
        )
    }

    fun prefill(parsed: ParsedTransaction, funds: List<Fund>) {
        _state.update { current ->
            val parsedType = parsed.type ?: TxnType.EXPENSE
            current.copy(
                amount = parsed.amount?.toString() ?: "",
                type = parsedType,
                merchant = parsed.merchant,
                upiId = parsed.upiId,
                txnId = parsed.txnId,
                description = parsed.description,
                dateText = parsed.date?.let(::formatDate) ?: "",
                selectedFundId = funds.firstOrNull()?.id ?: current.selectedFundId,
                selectedCategoryId = current.categories.firstOrNull {
                    it.type == parsedType.name || it.type == "BOTH"
                }?.id.orEmpty()
            )
        }
    }

    fun save(source: TxnSource = TxnSource.MANUAL) {
        val bypassDuplicateCheck = bypassDuplicateWarning
        bypassDuplicateWarning = false
        duplicateConfirmationSource = source
        val s = _state.value
        val amount = s.amount.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _state.update { it.copy(error = "Enter a valid amount") }
            return
        }
        if (s.selectedFundId.isBlank()) {
            _state.update { it.copy(error = "Select a fund") }
            return
        }
        val date = parseDate(s.dateText)
        if (s.dateText.isNotBlank() && date == null) {
            _state.update { it.copy(error = "Use a valid date, e.g. 8 Sep 2026") }
            return
        }
        viewModelScope.launch {
            val txn = Transaction(
                id = UUID.randomUUID().toString(),
                fundId = s.selectedFundId,
                amount = amount,
                type = s.type,
                categoryId = s.selectedCategoryId,
                description = s.description,
                merchant = s.merchant,
                upiId = s.upiId,
                txnId = s.txnId,
                source = source,
                date = date ?: System.currentTimeMillis()
            )
            val dupeResult = checkDuplicate(txn)
            if (dupeResult.isDuplicate && !bypassDuplicateCheck) {
                _state.update { it.copy(duplicateWarning = dupeResult.matches) }
                return@launch
            }
            saveUseCase(txn)
            _state.update { it.copy(saved = true, duplicateWarning = emptyList()) }
        }
    }

    fun dismissDuplicateAndSave() {
        bypassDuplicateWarning = true
        _state.update { it.copy(duplicateWarning = emptyList()) }
        save(duplicateConfirmationSource)
    }

    fun clearError() = _state.update { it.copy(error = null) }

    private fun currentFundId(current: String, preselected: String, funds: List<Fund>): String =
        when {
            preselected.isNotBlank() && funds.any { it.id == preselected } -> preselected
            current.isNotBlank() && funds.any { it.id == current } -> current
            else -> funds.firstOrNull()?.id.orEmpty()
        }

    private fun currentCategoryId(current: String, type: TxnType, categories: List<Category>): String =
        if (categories.any { it.id == current && (it.type == type.name || it.type == "BOTH") }) current
        else categories.firstOrNull { it.type == type.name || it.type == "BOTH" }?.id.orEmpty()

    private fun formatDate(millis: Long): String =
        SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(millis)

    private fun parseDate(value: String): Long? {
        if (value.isBlank()) return null
        val formats = listOf("d MMM yyyy", "yyyy-MM-dd", "d/M/yyyy")
        return formats.firstNotNullOfOrNull { pattern ->
            runCatching {
                SimpleDateFormat(pattern, Locale.getDefault()).apply { isLenient = false }.parse(value)?.time
            }.getOrNull()
        }
    }
}
