package com.tracker.domain.repository

import com.tracker.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun setAccent(hex: String)
    suspend fun setCurrency(code: String)
    suspend fun sync()
}
