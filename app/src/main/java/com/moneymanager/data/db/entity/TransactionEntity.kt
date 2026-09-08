package com.moneymanager.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val fundId: String,
    val amount: Double,
    val type: String,           // INCOME | EXPENSE
    val categoryId: String,
    val description: String,
    val merchant: String,
    val upiId: String,
    val txnId: String,          // external transaction ID for dedup
    val paymentMethod: String,
    val source: String,         // MANUAL | SCREENSHOT | PDF
    val date: Long,
    val importBatchId: String
)
