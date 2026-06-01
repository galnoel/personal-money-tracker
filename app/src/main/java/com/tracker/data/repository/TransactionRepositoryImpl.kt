package com.tracker.data.repository

import com.tracker.data.dao.CategoryTotal
import com.tracker.data.dao.TransactionDao
import com.tracker.data.entity.TransactionEntity
import com.tracker.domain.model.Transaction
import com.tracker.domain.model.TransactionType
import com.tracker.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val dao: TransactionDao
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> {
        return dao.getAllTransactions().map { list -> list.map { it.toDomain() } }
    }

    override fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> {
        return dao.getTransactionsByDateRange(startDate, endDate).map { list -> list.map { it.toDomain() } }
    }

    override fun getTotalIncomeByDateRange(startDate: Long, endDate: Long): Flow<Long> {
        return dao.getTotalIncomeByDateRange(startDate, endDate)
    }

    override fun getTotalExpenseByDateRange(startDate: Long, endDate: Long): Flow<Long> {
        return dao.getTotalExpenseByDateRange(startDate, endDate)
    }

    override fun getCategoryTotals(type: String, startDate: Long, endDate: Long): Flow<List<CategoryTotal>> {
        return dao.getCategoryTotals(type, startDate, endDate)
    }

    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> {
        return dao.getRecentTransactions(limit).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getTransactionById(id: Long): Transaction? {
        return dao.getTransactionById(id)?.toDomain()
    }

    override suspend fun insertTransaction(transaction: Transaction): Long {
        return dao.insert(transaction.toEntity())
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        dao.update(transaction.toEntity())
    }

    override suspend fun deleteTransaction(id: Long) {
        dao.deleteById(id)
    }

    private fun TransactionEntity.toDomain(): Transaction {
        return Transaction(
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

    private fun Transaction.toEntity(): TransactionEntity {
        return TransactionEntity(
            id = id,
            type = type.name,
            amount = amount,
            description = description,
            date = date,
            category = category,
            paymentMethod = paymentMethod,
            createdAt = createdAt
        )
    }
}
