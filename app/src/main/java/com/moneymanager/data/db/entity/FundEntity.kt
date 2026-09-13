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
    /** -1.0 means no minimum balance configured; ≥ 0.0 is an active threshold. */
    val minimumBalance: Double = -1.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)
