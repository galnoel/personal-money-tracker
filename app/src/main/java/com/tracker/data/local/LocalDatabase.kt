package com.tracker.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "cached_transactions",
    indices = [Index(value = ["userId", "clientId"], unique = true)]
)
data class CachedTransaction(
    @androidx.room.PrimaryKey(autoGenerate = true) val localId: Long = 0,
    val serverId: Long? = null,
    val clientId: String,
    val userId: String,
    val accountId: String?,
    val type: String,
    val amount: Long,
    val description: String,
    val date: Long,
    val category: String,
    val paymentMethod: String,
    val createdAt: Long,
    val updatedAt: Long,
    val pending: Boolean = false,
    val deleted: Boolean = false
)

@Entity(
    tableName = "cached_accounts",
    primaryKeys = ["userId", "id"],
    indices = [Index(value = ["userId", "name"])]
)
data class CachedAccount(
    val userId: String,
    val id: String,
    val name: String,
    val openingBalance: Long,
    val sortOrder: Int,
    val archived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val pending: Boolean = false
)

@Entity(tableName = "cached_transfers", primaryKeys = ["userId", "id"])
data class CachedTransfer(
    val userId: String,
    val id: String,
    val sourceAccountId: String,
    val destinationAccountId: String,
    val amount: Long,
    val description: String,
    val date: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val pending: Boolean = false
)

@Dao
interface TransactionCacheDao {
    @Query("select * from cached_transactions where userId = :userId and deleted = 0 order by date desc")
    fun observe(userId: String): Flow<List<CachedTransaction>>

    @Query("select * from cached_transactions where userId = :userId")
    suspend fun all(userId: String): List<CachedTransaction>

    @Query("select * from cached_transactions where localId = :id limit 1")
    suspend fun byId(id: Long): CachedTransaction?

    @Query("select * from cached_transactions where userId = :userId and clientId = :clientId limit 1")
    suspend fun byClientId(userId: String, clientId: String): CachedTransaction?

    @Query("select * from cached_transactions where userId = :userId and pending = 1")
    suspend fun pending(userId: String): List<CachedTransaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(value: CachedTransaction): Long

    @Update
    suspend fun update(value: CachedTransaction)

    @Query("delete from cached_transactions where localId = :id")
    suspend fun delete(id: Long)

    @Query("delete from cached_transactions where userId = :userId and pending = 0")
    suspend fun clearSynced(userId: String)

    @Query("delete from cached_transactions where userId = :userId")
    suspend fun clearUser(userId: String)
}

@Dao
interface AccountCacheDao {
    @Query("select * from cached_accounts where userId = :userId order by sortOrder")
    fun observe(userId: String): Flow<List<CachedAccount>>
    @Query("select * from cached_accounts where userId = :userId")
    suspend fun all(userId: String): List<CachedAccount>
    @Query("select * from cached_accounts where userId = :userId and pending = 1")
    suspend fun pending(userId: String): List<CachedAccount>
    @Query("select * from cached_accounts where userId = :userId and id = :id limit 1")
    suspend fun byId(userId: String, id: String): CachedAccount?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(value: CachedAccount)
    @Query("delete from cached_accounts where userId = :userId and pending = 0")
    suspend fun clearSynced(userId: String)
    @Query("delete from cached_accounts where userId = :userId")
    suspend fun clearUser(userId: String)
}

@Dao
interface TransferCacheDao {
    @Query("select * from cached_transfers where userId = :userId order by date desc")
    fun observe(userId: String): Flow<List<CachedTransfer>>
    @Query("select * from cached_transfers where userId = :userId")
    suspend fun all(userId: String): List<CachedTransfer>
    @Query("select * from cached_transfers where userId = :userId and pending = 1")
    suspend fun pending(userId: String): List<CachedTransfer>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(value: CachedTransfer)
    @Query("delete from cached_transfers where userId = :userId and pending = 0")
    suspend fun clearSynced(userId: String)
    @Query("delete from cached_transfers where userId = :userId")
    suspend fun clearUser(userId: String)
}

@Database(
    entities = [CachedTransaction::class, CachedAccount::class, CachedTransfer::class],
    version = 2,
    exportSchema = false
)
abstract class LocalDatabase : RoomDatabase() {
    abstract fun transactions(): TransactionCacheDao
    abstract fun accounts(): AccountCacheDao
    abstract fun transfers(): TransferCacheDao
}
