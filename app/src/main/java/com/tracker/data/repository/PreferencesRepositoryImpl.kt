package com.tracker.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tracker.domain.model.UserPreferences
import com.tracker.domain.repository.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Currency
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import androidx.glance.appwidget.updateAll
import com.tracker.widget.MoneyTrackerWidget

private val Context.themeDataStore by preferencesDataStore("user_preferences")

@Serializable
private data class PreferencesDto(
    @SerialName("user_id") val userId: String,
    @SerialName("accent_hex") val accentHex: String,
    @SerialName("currency_code") val currencyCode: String,
    @SerialName("updated_at") val updatedAt: Long
)

@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabase: SupabaseClient
) : PreferencesRepository {
    private val accentKey = stringPreferencesKey("accent_hex")
    private val currencyKey = stringPreferencesKey("currency_code")
    private val updatedKey = longPreferencesKey("preferences_updated_at")

    override val preferences: Flow<UserPreferences> = context.themeDataStore.data.map { values ->
        UserPreferences(
            accentHex = values[accentKey] ?: "#4F46E5",
            currencyCode = values[currencyKey] ?: defaultCurrency(),
            updatedAt = values[updatedKey] ?: 0L
        )
    }

    override suspend fun setAccent(hex: String) {
        require(Regex("^#[0-9A-Fa-f]{6}$").matches(hex)) { "Use a color such as #4F46E5." }
        saveLocal(accent = hex.uppercase(Locale.US))
        context.getSharedPreferences("widget_preferences", Context.MODE_PRIVATE)
            .edit().putString("accent_hex", hex.uppercase(Locale.US)).apply()
        MoneyTrackerWidget().updateAll(context)
        runCatching { push() }
    }

    override suspend fun setCurrency(code: String) {
        val normalized = code.uppercase(Locale.US)
        require(Regex("^[A-Z]{3}$").matches(normalized))
        saveLocal(currency = normalized)
        runCatching { push() }
    }

    override suspend fun sync() {
        val user = supabase.auth.currentUserOrNull()?.id ?: return
        val remote = runCatching {
            supabase.from("user_preferences").select {
                filter { eq("user_id", user) }
            }.decodeSingleOrNull<PreferencesDto>()
        }.getOrNull()
        val local = preferences.firstValue()
        if (remote == null || local.updatedAt >= remote.updatedAt) {
            push()
        } else {
            context.themeDataStore.edit {
                it[accentKey] = remote.accentHex
                it[currencyKey] = remote.currencyCode
                it[updatedKey] = remote.updatedAt
            }
            context.getSharedPreferences("widget_preferences", Context.MODE_PRIVATE)
                .edit().putString("accent_hex", remote.accentHex).apply()
            MoneyTrackerWidget().updateAll(context)
        }
    }

    private suspend fun saveLocal(accent: String? = null, currency: String? = null) {
        context.themeDataStore.edit {
            accent?.let { value -> it[accentKey] = value }
            currency?.let { value -> it[currencyKey] = value }
            it[updatedKey] = System.currentTimeMillis()
        }
    }

    private suspend fun push() {
        val user = supabase.auth.currentUserOrNull()?.id ?: return
        val local = preferences.firstValue()
        supabase.from("user_preferences").upsert(
            PreferencesDto(user, local.accentHex, local.currencyCode, local.updatedAt)
        )
    }

    private suspend fun Flow<UserPreferences>.firstValue() = first()

    private fun defaultCurrency(): String = runCatching {
        Currency.getInstance(Locale.getDefault()).currencyCode
    }.getOrDefault("SGD")
}
