package com.tracker.data.dao

import androidx.room.*
import com.tracker.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = :type AND date BETWEEN :startDate AND :endDate")
    fun getTotalByTypeAndDateRange(type: String, startDate: Long, endDate: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'IN' AND date BETWEEN :startDate AND :endDate")
    fun getTotalIncomeByDateRange(startDate: Long, endDate: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'OUT' AND date BETWEEN :startDate AND :endDate")
    fun getTotalExpenseByDateRange(startDate: Long, endDate: Long): Flow<Long>

    @Query("""
        SELECT category, SUM(amount) as total 
        FROM transactions 
        WHERE type = :type AND date BETWEEN :startDate AND :endDate 
        GROUP BY category 
        ORDER BY total DESC
    """)
    fun getCategoryTotals(type: String, startDate: Long, endDate: Long): Flow<List<CategoryTotal>>

    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>>
}

data class CategoryTotal(
    val category: String,
    val total: Long
)
