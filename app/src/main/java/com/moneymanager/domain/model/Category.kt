package com.moneymanager.domain.model

data class Category(
    val id: String,
    val name: String,
    val icon: String,
    val type: String  // INCOME | EXPENSE | BOTH
)

val DefaultCategories = listOf(
    Category("food", "Food & Dining", "🍽️", "EXPENSE"),
    Category("transport", "Transport", "🚗", "EXPENSE"),
    Category("shopping", "Shopping", "🛍️", "EXPENSE"),
    Category("health", "Health", "💊", "EXPENSE"),
    Category("bills", "Bills & Utilities", "💡", "EXPENSE"),
    Category("entertainment", "Entertainment", "🎬", "EXPENSE"),
    Category("education", "Education", "📚", "EXPENSE"),
    Category("home", "Home", "🏠", "EXPENSE"),
    Category("other_exp", "Other", "📦", "EXPENSE"),
    Category("salary", "Salary", "💼", "INCOME"),
    Category("gift", "Gift", "🎁", "INCOME"),
    Category("freelance", "Freelance", "💻", "INCOME"),
    Category("other_inc", "Other Income", "💰", "INCOME"),
)
