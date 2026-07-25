package com.tracker.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.tracker.data.local.CachedTransaction
import com.tracker.data.local.TransactionCacheDao
import com.tracker.data.remote.TransactionDto
import com.tracker.data.remote.toWriteDto
import com.tracker.data.sync.SyncWorker
import com.tracker.domain.model.CategoryTotal
import com.tracker.domain.model.Transaction
import com.tracker.domain.model.TransactionType
import com.tracker.domain.model.SyncStatus
import com.tracker.domain.repository.TransactionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    private val cache: TransactionCacheDao,
    @ApplicationContext private val context: Context
) : TransactionRepository {
    private val _syncStatus = MutableStateFlow(SyncStatus.Synced)
    override val syncStatus: StateFlow<SyncStatus> = _syncStatus
    private val syncMutex = Mutex()
    private var initializedUser: String? = null

    override fun notifySyncPending() { _syncStatus.value = SyncStatus.Syncing }
    override fun notifySyncOffline() { _syncStatus.value = SyncStatus.Offline }

    override fun getAllTransactions(): Flow<List<Transaction>> = flow {
        val user = userId()
        if (initializedUser != user) {
            initializedUser = user
            runCatching { refresh() }
        }
        emitAll(cache.observe(user).map { rows -> rows.map(CachedTransaction::toDomain) })
    }

    override fun getTransactionsByDateRange(startDate: Long, endDate: Long) =
        getAllTransactions().map { list -> list.filter { it.date in startDate until endDate } }

    override fun getTotalIncomeByDateRange(startDate: Long, endDate: Long) =
        getTransactionsByDateRange(startDate, endDate).map { list ->
            list.filter { it.type == TransactionType.IN }.sumOf { it.amount }
        }

    override fun getTotalExpenseByDateRange(startDate: Long, endDate: Long) =
        getTransactionsByDateRange(startDate, endDate).map { list ->
            list.filter { it.type == TransactionType.OUT }.sumOf { it.amount }
        }

    override fun getCategoryTotals(type: String, startDate: Long, endDate: Long) =
        getTransactionsByDateRange(startDate, endDate).map { list ->
            list.filter { it.type.name == type }.groupBy { it.category }
                .map { (category, rows) -> CategoryTotal(category, rows.sumOf { it.amount }) }
                .sortedByDescending { it.total }
        }

    override fun getRecentTransactions(limit: Int) =
        getAllTransactions().map { it.take(limit) }

    override suspend fun getTransactionById(id: Long): Transaction? = cache.byId(id)?.toDomain()

    override suspend fun insertTransaction(transaction: Transaction): Long {
        val user = userId()
        val row = transaction.toCache(user, pending = true)
        val localId = cache.insert(row)
        scheduleSync()
        return localId
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        val old = cache.byId(transaction.id) ?: return
        val updated = transaction.copy(
            clientId = old.clientId,
            accountId = transaction.accountId ?: old.accountId,
            createdAt = old.createdAt,
            updatedAt = System.currentTimeMillis()
        ).toCache(old.userId, pending = true, serverId = old.serverId, localId = old.localId)
        cache.update(updated)
        scheduleSync()
    }

    override suspend fun deleteTransaction(id: Long) {
        val row = cache.byId(id) ?: return
        cache.update(row.copy(deleted = true, pending = true, updatedAt = System.currentTimeMillis()))
        scheduleSync()
    }

    override suspend fun refresh() {
        notifySyncPending()
        syncMutex.withLock {
            try {
                val user = userId()
                cache.pending(user).forEach { row ->
                    if (row.deleted) pushDelete(row) else push(row)
                }
                val remote = supabase.from(TABLE).select().decodeList<TransactionDto>()
                val pendingIds = cache.pending(user).map { it.clientId }.toSet()
                cache.clearSynced(user)
                remote.filterNot { it.clientId in pendingIds }.forEach { dto ->
                    val existing = cache.byClientId(user, dto.clientId)
                    cache.insert(dto.toCache(user, existing?.localId ?: 0))
                }
                _syncStatus.value = SyncStatus.Synced
            } catch (error: Exception) {
                _syncStatus.value = SyncStatus.Offline
                throw error
            }
        }
    }

    override suspend fun clearLocalCache() {
        supabase.auth.currentUserOrNull()?.id?.let { cache.clearUser(it) }
        initializedUser = null
    }

    private suspend fun push(row: CachedTransaction) {
        val tx = row.toDomain()
        val existing = supabase.from(TABLE).select {
            filter { eq("client_id", row.clientId) }
        }.decodeSingleOrNull<TransactionDto>()
        val remote = if (existing == null) {
            supabase.from(TABLE).insert(tx.toWriteDto()) { select() }.decodeSingle<TransactionDto>()
        } else {
            supabase.from(TABLE).update(tx.toWriteDto()) {
                select()
                filter { eq("client_id", row.clientId) }
            }.decodeSingle<TransactionDto>()
        }
        cache.update(remote.toCache(row.userId, row.localId))
    }

    private suspend fun pushDelete(row: CachedTransaction) {
        supabase.from(TABLE).delete { filter { eq("client_id", row.clientId) } }
        cache.delete(row.localId)
    }

    private fun scheduleSync() {
        _syncStatus.value = SyncStatus.Syncing
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "financial-cache-immediate",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun userId(): String = supabase.auth.currentUserOrNull()?.id
        ?: error("Please sign in before accessing transactions.")

    private companion object { const val TABLE = "transactions" }
}

private fun Transaction.toCache(
    userId: String,
    pending: Boolean,
    serverId: Long? = id.takeIf { it > 0 },
    localId: Long = id.takeIf { it > 0 } ?: 0
) = CachedTransaction(
    localId, serverId, clientId, userId, accountId, type.name, amount, description,
    date, category, paymentMethod, createdAt, updatedAt, pending, false
)

private fun CachedTransaction.toDomain() = Transaction(
    id = localId,
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

private fun TransactionDto.toCache(userId: String, localId: Long) = CachedTransaction(
    localId = localId,
    serverId = id,
    clientId = clientId,
    userId = userId,
    accountId = accountId,
    type = type,
    amount = amount,
    description = description,
    date = date,
    category = category,
    paymentMethod = paymentMethod,
    createdAt = createdAt,
    updatedAt = updatedAt
)
