package com.moneymanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.db.entity.FundEntity
import com.moneymanager.data.repository.FundRepository
import com.moneymanager.domain.model.Fund
import com.moneymanager.ui.theme.FundColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class FundViewModel @Inject constructor(private val repo: FundRepository) : ViewModel() {

    val funds: StateFlow<List<Fund>> = repo.observeFunds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveFund(id: String?, name: String, icon: String, colorHex: String, startingBalance: Double) {
        viewModelScope.launch {
            repo.upsert(
                FundEntity(
                    id = id ?: UUID.randomUUID().toString(),
                    name = name,
                    icon = icon,
                    colorHex = colorHex,
                    startingBalance = startingBalance
                )
            )
        }
    }

    fun deleteFund(fund: Fund) {
        viewModelScope.launch {
            repo.getById(fund.id)?.let {
                repo.delete(it)
            }
        }
    }

    fun nextColor(index: Int): String {
        val color = FundColors[index % FundColors.size]
        val argb = color.value.toLong().and(0xFFFFFFFFL)
        return "#%06X".format(argb.and(0xFFFFFF))
    }
}
