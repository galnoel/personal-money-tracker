package com.tracker.domain.repository

import com.tracker.domain.model.AccountBalance
import com.tracker.domain.model.PaymentAccount
import com.tracker.domain.model.Transfer
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAccounts(): Flow<List<PaymentAccount>>
    fun getTransfers(): Flow<List<Transfer>>
    fun getAccountBalances(): Flow<List<AccountBalance>>
    suspend fun createAccount(name: String, openingBalance: Long = 0): PaymentAccount
    suspend fun renameAccount(id: String, name: String)
    suspend fun reorderAccounts(ids: List<String>)
    suspend fun archiveAccount(id: String)
    suspend fun reconcileAccount(id: String, desiredBalance: Long)
    suspend fun createTransfer(transfer: Transfer)
    suspend fun refresh()
    suspend fun clearLocalCache()
}
