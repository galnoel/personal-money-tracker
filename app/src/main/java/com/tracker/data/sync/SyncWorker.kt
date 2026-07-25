package com.tracker.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tracker.domain.repository.TransactionRepository
import com.tracker.domain.repository.AccountRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val transactions: TransactionRepository,
    private val accounts: AccountRepository,
    private val supabase: SupabaseClient
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (supabase.auth.currentSessionOrNull() == null) return Result.success()
        return try {
            accounts.refresh()
            transactions.refresh()
            Result.success()
        } catch (_: Exception) {
            transactions.notifySyncOffline()
            Result.retry()
        }
    }
}
