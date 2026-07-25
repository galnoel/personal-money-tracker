package com.tracker.domain.model

data class Transfer(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sourceAccountId: String,
    val destinationAccountId: String,
    val amount: Long,
    val description: String = "",
    val date: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
