package com.moneymanager.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transaction_allocations",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["transactionId"], name = "idx_alloc_txn"), Index(value = ["fundId"], name = "idx_alloc_fund")]
)
data class TransactionAllocationEntity(
    @PrimaryKey val id: String,
    val transactionId: String,
    val fundId: String,
    val amount: Double
)
