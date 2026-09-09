package com.moneymanager.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.repository.CategoryRepository
import com.moneymanager.domain.model.Category
import com.moneymanager.domain.model.DefaultCategories
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(private val repo: CategoryRepository) : ViewModel() {
    val categories: StateFlow<List<Category>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(name: String, icon: String, type: String, editingId: String = "") {
        if (name.isBlank()) return
        viewModelScope.launch {
            repo.upsert(Category(editingId.ifBlank { UUID.randomUUID().toString() }, name.trim(), icon.ifBlank { "📦" }, type))
        }
    }

    fun delete(category: Category) = viewModelScope.launch { repo.delete(category) }

    fun seedDefaults() = viewModelScope.launch { repo.seedDefaults() }
}
