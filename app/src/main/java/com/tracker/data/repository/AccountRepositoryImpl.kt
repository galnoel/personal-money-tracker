package com.tracker.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.tracker.data.local.AccountCacheDao
import com.tracker.data.local.CachedAccount
import com.tracker.data.local.CachedTransfer
import com.tracker.data.local.TransferCacheDao
import com.tracker.data.remote.PaymentAccountDto
import com.tracker.data.remote.PaymentAccountWriteDto
import com.tracker.data.remote.TransferDto
import com.tracker.data.remote.TransferWriteDto
import com.tracker.data.sync.SyncWorker
import com.tracker.domain.model.AccountBalance
import com.tracker.domain.model.PaymentAccount
import com.tracker.domain.model.TransactionType
import com.tracker.domain.model.Transfer
import com.tracker.domain.repository.AccountRepository
import com.tracker.domain.repository.TransactionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    private val transactions: TransactionRepository,
    private val accountCache: AccountCacheDao,
    private val transferCache: TransferCacheDao,
    @ApplicationContext private val context: Context
) : AccountRepository {
    private val mutex = Mutex()
    private var initializedUser: String? = null

    override fun getAccounts(): Flow<List<PaymentAccount>> = flow {
        val user = userId()
        initialize(user)
        emitAll(accountCache.observe(user).map { rows -> rows.map(CachedAccount::domain) })
    }

    override fun getTransfers(): Flow<List<Transfer>> = flow {
        val user = userId()
        initialize(user)
        emitAll(transferCache.observe(user).map { rows -> rows.map(CachedTransfer::domain) })
    }

    override fun getAccountBalances(): Flow<List<AccountBalance>> =
        combine(getAccounts(), getTransfers(), transactions.getAllTransactions()) { accounts, transfers, txs ->
            accounts.filterNot { it.archived }.sortedBy { it.sortOrder }.map { account ->
                val activity = txs.filter {
                    it.accountId == account.id ||
                        (it.accountId == null && it.paymentMethod.equals(account.name, true))
                }.sumOf { if (it.type == TransactionType.IN) it.amount else -it.amount }
                val movement = transfers.sumOf {
                    when (account.id) {
                        it.destinationAccountId -> it.amount
                        it.sourceAccountId -> -it.amount
                        else -> 0L
                    }
                }
                AccountBalance(account, account.openingBalance + activity + movement)
            }
        }

    override suspend fun createAccount(name: String, openingBalance: Long): PaymentAccount {
        val user = userId()
        val clean = name.trim().replace(Regex("\\s+"), " ").take(60)
        require(clean.isNotBlank()) { "Account name is required." }
        require(accountCache.all(user).none { it.name.equals(clean, true) }) { "That account already exists." }
        val now = System.currentTimeMillis()
        val account = PaymentAccount(
            id = UUID.randomUUID().toString(),
            name = clean,
            openingBalance = openingBalance,
            sortOrder = accountCache.all(user).size,
            createdAt = now,
            updatedAt = now
        )
        accountCache.upsert(account.cache(user, true))
        scheduleSync()
        return account
    }

    override suspend fun renameAccount(id: String, name: String) = mutateAccount(id) {
        it.copy(name = name.trim().take(60).also { value -> require(value.isNotBlank()) })
    }

    override suspend fun reorderAccounts(ids: List<String>) {
        val user = userId()
        accountCache.all(user).forEach { row ->
            val order = ids.indexOf(row.id)
            if (order >= 0 && order != row.sortOrder) {
                accountCache.upsert(row.copy(sortOrder = order, updatedAt = System.currentTimeMillis(), pending = true))
            }
        }
        scheduleSync()
    }

    override suspend fun archiveAccount(id: String) = mutateAccount(id) { it.copy(archived = true) }

    override suspend fun reconcileAccount(id: String, desiredBalance: Long) {
        val current = getAccountBalances().first().first { it.account.id == id }
        mutateAccount(id) {
            it.copy(openingBalance = it.openingBalance + desiredBalance - current.balance)
        }
    }

    override suspend fun createTransfer(transfer: Transfer) {
        require(transfer.sourceAccountId != transfer.destinationAccountId) { "Choose two different accounts." }
        require(transfer.amount > 0) { "Transfer amount must be greater than zero." }
        val row = transfer.cache(userId(), true)
        transferCache.upsert(row)
        scheduleSync()
    }

    override suspend fun refresh() {
        mutex.withLock {
            val user = userId()
            accountCache.pending(user).forEach { pushAccount(it) }
            transferCache.pending(user).forEach { pushTransfer(it) }
            val accounts = supabase.from("payment_accounts").select().decodeList<PaymentAccountDto>()
            val pendingAccounts = accountCache.pending(user).map { it.id }.toSet()
            val pendingTransfers = transferCache.pending(user).map { it.id }.toSet()
            accountCache.clearSynced(user)
            transferCache.clearSynced(user)
            accounts.filterNot { it.id in pendingAccounts }.forEach {
                accountCache.upsert(it.toCache(user))
            }
            supabase.from("transfers").select().decodeList<TransferDto>()
                .filterNot { it.id in pendingTransfers }.forEach {
                    transferCache.upsert(it.toCache(user))
                }
        }
    }

    override suspend fun clearLocalCache() {
        supabase.auth.currentUserOrNull()?.id?.let { user ->
            accountCache.clearUser(user)
            transferCache.clearUser(user)
        }
        initializedUser = null
    }

    private suspend fun initialize(user: String) {
        if (initializedUser == user) return
        initializedUser = user
        runCatching { refresh() }
        if (accountCache.all(user).isEmpty()) seedDefaults(user)
    }

    private suspend fun seedDefaults(user: String) {
        val now = System.currentTimeMillis()
        listOf("Cash", "Debit Card").forEachIndexed { order, name ->
            accountCache.upsert(
                PaymentAccount(UUID.randomUUID().toString(), name, sortOrder = order, createdAt = now, updatedAt = now)
                    .cache(user, true)
            )
        }
        scheduleSync()
    }

    private suspend fun mutateAccount(id: String, change: (PaymentAccount) -> PaymentAccount) {
        val user = userId()
        val row = accountCache.byId(user, id) ?: error("Account not found.")
        val changed = change(row.domain()).copy(updatedAt = System.currentTimeMillis()).cache(user, true)
        accountCache.upsert(changed)
        scheduleSync()
    }

    private suspend fun pushAccount(row: CachedAccount) {
        val dto = PaymentAccountWriteDto(
            row.id, row.userId, row.name, row.openingBalance, row.sortOrder,
            row.archived, row.createdAt, row.updatedAt
        )
        supabase.from("payment_accounts").upsert(dto)
        accountCache.upsert(row.copy(pending = false))
    }

    private suspend fun pushTransfer(row: CachedTransfer) {
        supabase.from("transfers").upsert(
            TransferWriteDto(
                row.id, row.userId, row.sourceAccountId, row.destinationAccountId,
                row.amount, row.description, row.date, row.createdAt, row.updatedAt
            )
        )
        transferCache.upsert(row.copy(pending = false))
    }

    private fun scheduleSync() {
        transactions.notifySyncPending()
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
        ?: error("Please sign in before accessing accounts.")
}

private fun PaymentAccount.cache(user: String, pending: Boolean) = CachedAccount(
    user, id, name, openingBalance, sortOrder, archived, createdAt, updatedAt, pending
)
private fun CachedAccount.domain() = PaymentAccount(id, name, openingBalance, sortOrder, archived, createdAt, updatedAt)
private fun PaymentAccountDto.toCache(user: String) = CachedAccount(
    user, id, name, openingBalance, sortOrder, archived, createdAt, updatedAt
)
private fun Transfer.cache(user: String, pending: Boolean) = CachedTransfer(
    user, id, sourceAccountId, destinationAccountId, amount, description, date, createdAt, updatedAt, pending
)
private fun CachedTransfer.domain() = Transfer(
    id, sourceAccountId, destinationAccountId, amount, description, date, createdAt, updatedAt
)
private fun TransferDto.toCache(user: String) = CachedTransfer(
    user, id, sourceAccountId, destinationAccountId, amount, description, date, createdAt, updatedAt
)
