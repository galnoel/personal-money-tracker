package com.tracker.domain.model

data class Transaction(
    val id: Long = 0,
    val clientId: String = java.util.UUID.randomUUID().toString(),
    val accountId: String? = null,
    val type: TransactionType,
    val amount: Long, // in cents
    val description: String,
    val date: Long, // epoch millis
    val category: String,
    val paymentMethod: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val displayAmount: String
        get() {
            val whole = amount / 100
            val frac = amount % 100
            val sign = if (type == TransactionType.OUT) "-" else "+"
            return "$sign${String.format("%,d.%02d", whole, frac)}"
        }
}
