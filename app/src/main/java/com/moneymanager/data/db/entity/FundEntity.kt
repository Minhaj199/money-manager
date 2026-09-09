package com.moneymanager.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "funds")
data class FundEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val colorHex: String,
    val sourceName: String = "",
    val startingBalance: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)
