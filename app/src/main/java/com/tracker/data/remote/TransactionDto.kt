package com.tracker.data.remote

import com.tracker.domain.model.Transaction
import com.tracker.domain.model.TransactionType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransactionDto(
    val id: Long,
    val type: String,
    val amount: Long,
    val description: String,
    val date: Long,
    val category: String,
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("created_at") val createdAt: Long
) {
    fun toDomain() = Transaction(
        id = id,
        type = if (type == "IN") TransactionType.IN else TransactionType.OUT,
        amount = amount,
        description = description,
        date = date,
        category = category,
        paymentMethod = paymentMethod,
        createdAt = createdAt
    )
}

@Serializable
data class TransactionWriteDto(
    val type: String,
    val amount: Long,
    val description: String,
    val date: Long,
    val category: String,
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("created_at") val createdAt: Long
)

fun Transaction.toWriteDto() = TransactionWriteDto(
    type = type.name,
    amount = amount,
    description = description,
    date = date,
    category = category,
    paymentMethod = paymentMethod,
    createdAt = createdAt
)
