package com.moneymanager.data.repository

import com.moneymanager.data.db.dao.CategoryDao
import com.moneymanager.data.db.entity.CategoryEntity
import com.moneymanager.domain.model.Category
import com.moneymanager.domain.model.DefaultCategories
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(private val dao: CategoryDao) {

    fun observeAll(): Flow<List<Category>> = dao.observeAll().map { list ->
        list.map { Category(it.id, it.name, it.icon, it.type) }
    }

    suspend fun seedDefaults() = dao.insertAll(
        DefaultCategories.map { CategoryEntity(it.id, it.name, it.icon, it.type) }
    )

    suspend fun upsert(category: Category) {
        val now = System.currentTimeMillis()
        val existing = dao.getById(category.id)
        dao.upsertAll(listOf(CategoryEntity(
            id = category.id,
            name = category.name,
            icon = category.icon,
            type = category.type,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )))
    }

    suspend fun getAll(): List<Category> = dao.getAll().map { Category(it.id, it.name, it.icon, it.type) }
}
