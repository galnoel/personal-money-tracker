package com.tracker.domain.model

data class PaymentAccount(
    val id: String,
    val name: String,
    val openingBalance: Long = 0,
    val sortOrder: Int = 0,
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class AccountBalance(
    val account: PaymentAccount,
    val balance: Long
)
