package com.moneymanager.domain.model

data class Transfer(
    val id: String,
    val fromFundId: String,
    val toFundId: String,
    val amount: Double,
    val note: String,
    val date: Long
)
