package com.moneymanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.repository.CategoryRepository
import com.moneymanager.data.repository.FundRepository
import com.moneymanager.data.repository.TransactionRepository
import com.moneymanager.domain.model.Fund
import com.moneymanager.domain.model.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val funds: List<Fund> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val totalBalance: Double = 0.0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val fundRepo: FundRepository,
    private val txnRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        fundRepo.observeFunds(),
        txnRepo.observeRecent(20)
    ) { funds, recent ->
        HomeUiState(
            funds = funds,
            recentTransactions = recent,
            totalBalance = funds.sumOf { it.balance }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    init {
        viewModelScope.launch { categoryRepo.seedDefaults() }
    }
}
