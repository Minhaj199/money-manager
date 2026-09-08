package com.moneymanager.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transfers")
data class TransferEntity(
    @PrimaryKey val id: String,
    val fromFundId: String,
    val toFundId: String,
    val amount: Double,
    val note: String,
    val date: Long
)
