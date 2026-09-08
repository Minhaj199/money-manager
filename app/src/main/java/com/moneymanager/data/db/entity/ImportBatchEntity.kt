package com.moneymanager.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "import_batches")
data class ImportBatchEntity(
    @PrimaryKey val id: String,
    val source: String,      // SCREENSHOT | PDF
    val rawHash: String,     // SHA-256 of raw content for dedup
    val createdAt: Long = System.currentTimeMillis()
)
