package com.tracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "IN" or "OUT"
    val amount: Long, // stored in cents
    val description: String,
    val date: Long, // epoch millis
    val category: String,
    val paymentMethod: String,
    val createdAt: Long = System.currentTimeMillis()
)
