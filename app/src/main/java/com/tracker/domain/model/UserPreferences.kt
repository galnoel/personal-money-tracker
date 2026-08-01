package com.tracker.domain.model

data class UserPreferences(
    val accentHex: String = "#4F46E5",
    val currencyCode: String = "SGD",
    val updatedAt: Long = System.currentTimeMillis()
)

enum class SyncStatus { Synced, Syncing, Offline, Failed }

data class ChartPoint(
    val label: String,
    val income: Long,
    val expense: Long
) {
    val net: Long get() = income - expense
}
