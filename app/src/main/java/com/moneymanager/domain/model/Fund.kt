package com.moneymanager.domain.model

data class Fund(
    val id: String,
    val name: String,
    val icon: String,
    val colorHex: String,
    val startingBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpent: Double = 0.0
) {
    val allocated: Double get() = startingBalance + totalIncome
    val balance: Double get() = allocated - totalExpent
}
