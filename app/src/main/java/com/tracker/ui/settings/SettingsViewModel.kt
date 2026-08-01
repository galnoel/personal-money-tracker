package com.tracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tracker.domain.model.AccountBalance
import com.tracker.domain.model.UserPreferences
import com.tracker.domain.repository.AccountRepository
import com.tracker.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val accounts: List<AccountBalance> = emptyList(),
    val isWorking: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val accountsRepository: AccountRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.preferences.collect { preferences ->
                _state.update { it.copy(preferences = preferences) }
            }
        }
        viewModelScope.launch {
            accountsRepository.getAccountBalances(includeArchived = true).catch {
                _state.update { state -> state.copy(message = it.message) }
            }.collect { accounts -> _state.update { it.copy(accounts = accounts) } }
        }
        viewModelScope.launch { runCatching { preferencesRepository.sync() } }
    }

    fun setAccent(hex: String) = run { preferencesRepository.setAccent(hex) }
    fun setCurrency(code: String) = run { preferencesRepository.setCurrency(code) }
    fun create(name: String, opening: Long = 0) = run { accountsRepository.createAccount(name, opening) }
    fun rename(id: String, name: String) = run { accountsRepository.renameAccount(id, name) }
    fun archive(id: String) = run { accountsRepository.archiveAccount(id) }
    fun unarchive(id: String) = run { accountsRepository.setAccountArchived(id, false) }
    fun reconcile(id: String, balance: Long) = run { accountsRepository.reconcileAccount(id, balance) }
    fun move(id: String, direction: Int) = run {
        val ids = _state.value.accounts
            .filterNot { it.account.archived }
            .map { it.account.id }
            .toMutableList()
        val from = ids.indexOf(id)
        val to = (from + direction).coerceIn(ids.indices)
        if (from >= 0 && from != to) {
            java.util.Collections.swap(ids, from, to)
            accountsRepository.reorderAccounts(ids)
        }
    }

    private fun run(block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isWorking = true, message = null) }
            runCatching { block() }
                .onFailure { error -> _state.update { it.copy(message = error.message) } }
            _state.update { it.copy(isWorking = false) }
        }
    }
}
