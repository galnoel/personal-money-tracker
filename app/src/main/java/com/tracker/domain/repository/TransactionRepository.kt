package com.tracker.domain.repository

import com.tracker.data.dao.CategoryTotal
import com.tracker.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>>
    fun getTotalIncomeByDateRange(startDate: Long, endDate: Long): Flow<Long>
    fun getTotalExpenseByDateRange(startDate: Long, endDate: Long): Flow<Long>
    fun getCategoryTotals(type: String, startDate: Long, endDate: Long): Flow<List<CategoryTotal>>
    fun getRecentTransactions(limit: Int): Flow<List<Transaction>>
    suspend fun getTransactionById(id: Long): Transaction?
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(id: Long)
}
