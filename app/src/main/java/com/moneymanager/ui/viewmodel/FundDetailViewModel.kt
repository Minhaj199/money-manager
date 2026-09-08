package com.moneymanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.repository.FundRepository
import com.moneymanager.data.repository.TransactionRepository
import com.moneymanager.domain.model.Fund
import com.moneymanager.domain.model.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FundDetailViewModel @Inject constructor(
    private val fundRepo: FundRepository,
    private val txnRepo: TransactionRepository
) : ViewModel() {

    private val _fundId = MutableStateFlow("")
    val fund: StateFlow<Fund?> = _fundId
        .filter { it.isNotBlank() }
        .flatMapLatest { id -> fundRepo.observeFunds().map { list -> list.find { it.id == id } } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val transactions: StateFlow<List<Transaction>> = _fundId
        .filter { it.isNotBlank() }
        .flatMapLatest { txnRepo.observeByFund(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun load(fundId: String) { _fundId.value = fundId }
}
