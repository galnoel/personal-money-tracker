package com.tracker.di

import android.content.Context
import androidx.room.Room
import com.tracker.BuildConfig
import com.tracker.data.local.LocalDatabase
import com.tracker.data.local.TransactionCacheDao
import com.tracker.data.local.AccountCacheDao
import com.tracker.data.local.TransferCacheDao
import com.tracker.data.repository.AccountRepositoryImpl
import com.tracker.data.repository.PreferencesRepositoryImpl
import com.tracker.data.repository.TransactionRepositoryImpl
import com.tracker.domain.repository.AccountRepository
import com.tracker.domain.repository.PreferencesRepository
import com.tracker.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LocalDatabase =
        Room.databaseBuilder(context, LocalDatabase::class.java, "money-cache.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideTransactionCache(database: LocalDatabase): TransactionCacheDao = database.transactions()

    @Provides fun provideAccountCache(database: LocalDatabase): AccountCacheDao = database.accounts()
    @Provides fun provideTransferCache(database: LocalDatabase): TransferCacheDao = database.transfers()

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        check(BuildConfig.SUPABASE_URL.isNotBlank()) {
            "SUPABASE_URL is missing. Add it to the project .env file."
        }
        check(BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()) {
            "SUPABASE_PUBLISHABLE_KEY is missing. Add it to the project .env file."
        }
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
        ) {
            install(Auth)
            install(Postgrest)
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        impl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: PreferencesRepositoryImpl): PreferencesRepository
}
