package com.tracker.data.repository

import com.tracker.data.remote.TransactionDto
import com.tracker.data.remote.toWriteDto
import com.tracker.domain.model.CategoryTotal
import com.tracker.domain.model.Transaction
import com.tracker.domain.model.TransactionType
import com.tracker.domain.repository.TransactionRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : TransactionRepository {

    private val cache = MutableStateFlow<List<Transaction>>(emptyList())
    private val refreshMutex = Mutex()
    @Volatile private var hasLoaded = false
    @Volatile private var loadedUserId: String? = null

    override fun getAllTransactions(): Flow<List<Transaction>> = flow {
        ensureLoaded()
        emitAll(cache)
    }

    override fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<Transaction>> =
        getAllTransactions().map { transactions ->
            transactions.filter { it.date in startDate until endDate }
        }

    override fun getTotalIncomeByDateRange(startDate: Long, endDate: Long): Flow<Long> =
        getTransactionsByDateRange(startDate, endDate).map { transactions ->
            transactions.filter { it.type == TransactionType.IN }.sumOf { it.amount }
        }

    override fun getTotalExpenseByDateRange(startDate: Long, endDate: Long): Flow<Long> =
        getTransactionsByDateRange(startDate, endDate).map { transactions ->
            transactions.filter { it.type == TransactionType.OUT }.sumOf { it.amount }
        }

    override fun getCategoryTotals(
        type: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<CategoryTotal>> =
        getTransactionsByDateRange(startDate, endDate).map { transactions ->
            transactions
                .filter { it.type.name == type }
                .groupBy { it.category }
                .map { (category, items) -> CategoryTotal(category, items.sumOf { it.amount }) }
                .sortedByDescending { it.total }
        }

    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> =
        getAllTransactions().map { it.take(limit) }

    override suspend fun getTransactionById(id: Long): Transaction? {
        ensureLoaded()
        return cache.value.firstOrNull { it.id == id }
    }

    override suspend fun insertTransaction(transaction: Transaction): Long {
        ensureAuthenticated()
        val inserted = supabase.from(TABLE)
            .insert(transaction.toWriteDto()) { select() }
            .decodeSingle<TransactionDto>()
            .toDomain()
        cache.value = (cache.value + inserted).sortedByDescending { it.date }
        return inserted.id
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        ensureAuthenticated()
        supabase.from(TABLE).update(transaction.toWriteDto()) {
            filter { eq("id", transaction.id) }
        }
        refresh()
    }

    override suspend fun deleteTransaction(id: Long) {
        ensureAuthenticated()
        supabase.from(TABLE).delete {
            filter { eq("id", id) }
        }
        cache.value = cache.value.filterNot { it.id == id }
    }

    override suspend fun refresh() {
        loadRemote(force = true)
    }

    private suspend fun ensureLoaded() {
        loadRemote(force = false)
    }

    private suspend fun loadRemote(force: Boolean) {
        refreshMutex.withLock {
            ensureAuthenticated()
            val currentUserId = supabase.auth.currentUserOrNull()?.id
                ?: error("Please sign in before accessing transactions.")
            if (hasLoaded && loadedUserId == currentUserId && !force) return
            if (loadedUserId != currentUserId) {
                cache.value = emptyList()
                hasLoaded = false
            }
            cache.value = supabase.from(TABLE)
                .select(
                    columns = Columns.list(
                        "id",
                        "type",
                        "amount",
                        "description",
                        "date",
                        "category",
                        "payment_method",
                        "created_at"
                    )
                )
                .decodeList<TransactionDto>()
                .map(TransactionDto::toDomain)
                .sortedByDescending { it.date }
            hasLoaded = true
            loadedUserId = currentUserId
        }
    }

    private suspend fun ensureAuthenticated() {
        check(supabase.auth.currentSessionOrNull() != null) {
            "Please sign in before accessing transactions."
        }
    }

    private companion object {
        const val TABLE = "transactions"
    }
}
