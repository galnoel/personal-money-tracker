package com.tracker.data.remote

import com.tracker.domain.model.Transaction
import com.tracker.domain.model.TransactionType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TransactionDto(
    val id: Long,
    @SerialName("client_id") val clientId: String,
    @SerialName("account_id") val accountId: String? = null,
    val type: String,
    val amount: Long,
    val description: String,
    val date: Long,
    val category: String,
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
) {
    fun toDomain() = Transaction(
        id = id,
        clientId = clientId,
        accountId = accountId,
        type = if (type == "IN") TransactionType.IN else TransactionType.OUT,
        amount = amount,
        description = description,
        date = date,
        category = category,
        paymentMethod = paymentMethod,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

@Serializable
data class TransactionWriteDto(
    @SerialName("client_id") val clientId: String,
    @SerialName("account_id") val accountId: String? = null,
    val type: String,
    val amount: Long,
    val description: String,
    val date: Long,
    val category: String,
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)

fun Transaction.toWriteDto() = TransactionWriteDto(
    clientId = clientId,
    accountId = accountId,
    type = type.name,
    amount = amount,
    description = description,
    date = date,
    category = category,
    paymentMethod = paymentMethod,
    createdAt = createdAt,
    updatedAt = updatedAt
)
