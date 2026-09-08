package com.moneymanager.domain.model

data class Fund(
    val id: String,
    val name: String,
    val icon: String,
    val colorHex: String,
    val totalIncome: Double = 0.0,
    val totalExpent: Double = 0.0
) {
    val balance: Double get() = totalIncome - totalExpent
}
