package com.moneymanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.repository.TransactionRepository
import com.moneymanager.domain.model.Transaction
import com.moneymanager.domain.usecase.ManageTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionListViewModel @Inject constructor(
    repo: TransactionRepository,
    private val manageTransaction: ManageTransactionUseCase
) : ViewModel() {
    val transactions: StateFlow<List<Transaction>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(transactionId: String) = viewModelScope.launch {
        manageTransaction.delete(transactionId)
    }
}
