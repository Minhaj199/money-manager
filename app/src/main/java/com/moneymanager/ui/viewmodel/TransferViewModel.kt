package com.moneymanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.repository.FundRepository
import com.moneymanager.data.repository.TransferRepository
import com.moneymanager.domain.model.Fund
import com.moneymanager.domain.usecase.TransferFundsUseCase
import com.moneymanager.domain.model.Transfer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class TransferUiState(
    val funds: List<Fund> = emptyList(),
    val fromFundId: String = "",
    val toFundId: String = "",
    val amount: String = "",
    val note: String = "",
    val saved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val fundRepo: FundRepository,
    private val transferUseCase: TransferFundsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(TransferUiState())
    val state: StateFlow<TransferUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            fundRepo.observeFunds().collect { funds ->
                _state.update { it.copy(funds = funds) }
            }
        }
    }

    fun update(block: TransferUiState.() -> TransferUiState) = _state.update(block)

    fun execute() {
        val s = _state.value
        val amount = s.amount.toDoubleOrNull()
        if (amount == null || amount <= 0) { _state.update { it.copy(error = "Enter valid amount") }; return }
        if (s.fromFundId == s.toFundId) { _state.update { it.copy(error = "Select different funds") }; return }
        viewModelScope.launch {
            transferUseCase(Transfer(UUID.randomUUID().toString(), s.fromFundId, s.toFundId, amount, s.note, System.currentTimeMillis()))
            _state.update { it.copy(saved = true) }
        }
    }
}
