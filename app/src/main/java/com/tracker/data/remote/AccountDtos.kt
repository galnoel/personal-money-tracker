package com.tracker.data.remote

import com.tracker.domain.model.PaymentAccount
import com.tracker.domain.model.Transfer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaymentAccountDto(
    val id: String,
    val name: String,
    @SerialName("opening_balance") val openingBalance: Long,
    @SerialName("sort_order") val sortOrder: Int,
    val archived: Boolean,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
) {
    fun toDomain() = PaymentAccount(id, name, openingBalance, sortOrder, archived, createdAt, updatedAt)
}

@Serializable
data class PaymentAccountWriteDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    @SerialName("opening_balance") val openingBalance: Long,
    @SerialName("sort_order") val sortOrder: Int,
    val archived: Boolean = false,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)

@Serializable
data class TransferDto(
    val id: String,
    @SerialName("source_account_id") val sourceAccountId: String,
    @SerialName("destination_account_id") val destinationAccountId: String,
    val amount: Long,
    val description: String,
    val date: Long,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
) {
    fun toDomain() = Transfer(id, sourceAccountId, destinationAccountId, amount, description, date, createdAt, updatedAt)
}

@Serializable
data class TransferWriteDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("source_account_id") val sourceAccountId: String,
    @SerialName("destination_account_id") val destinationAccountId: String,
    val amount: Long,
    val description: String,
    val date: Long,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)
