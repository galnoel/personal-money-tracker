package com.tracker.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tracker.domain.repository.TransactionRepository
import com.tracker.domain.repository.AccountRepository
import com.tracker.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AuthMode {
    SIGN_IN,
    SIGN_UP
}

data class AuthUiState(
    val mode: AuthMode = AuthMode.SIGN_IN,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isInitializing: Boolean = true,
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val isAnonymous: Boolean = false,
    val accountEmail: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val supabase: SupabaseClient,
    private val transactions: TransactionRepository,
    private val accounts: AccountRepository,
    private val preferences: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            supabase.auth.sessionStatus.collect { status ->
                when (status) {
                    SessionStatus.Initializing -> {
                        _uiState.update { it.copy(isInitializing = true) }
                    }
                    is SessionStatus.Authenticated -> {
                        val user = status.session.user
                        val anonymous = user?.email.isNullOrBlank() && user?.phone.isNullOrBlank()
                        _uiState.update {
                            it.copy(
                                mode = if (anonymous) AuthMode.SIGN_UP else it.mode,
                                isInitializing = false,
                                isLoading = false,
                                isAuthenticated = !anonymous,
                                isAnonymous = anonymous,
                                accountEmail = user?.email
                            )
                        }
                        if (!anonymous) runCatching { preferences.sync() }
                    }
                    is SessionStatus.NotAuthenticated -> {
                        _uiState.update {
                            it.copy(
                                isInitializing = false,
                                isLoading = false,
                                isAuthenticated = false,
                                isAnonymous = false,
                                accountEmail = null
                            )
                        }
                    }
                    is SessionStatus.RefreshFailure -> {
                        _uiState.update {
                            it.copy(
                                isInitializing = false,
                                isLoading = false,
                                isAuthenticated = false,
                                errorMessage = "Your session expired. Please sign in again."
                            )
                        }
                    }
                }
            }
        }
    }

    fun setMode(mode: AuthMode) {
        _uiState.update {
            it.copy(
                mode = mode,
                password = "",
                confirmPassword = "",
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun setEmail(value: String) {
        _uiState.update { it.copy(email = value.trim(), errorMessage = null) }
    }

    fun setPassword(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun setConfirmPassword(value: String) {
        _uiState.update { it.copy(confirmPassword = value, errorMessage = null) }
    }

    fun submit() {
        val state = _uiState.value
        val validationError = validate(state)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
        viewModelScope.launch {
            runCatching {
                if (state.mode == AuthMode.SIGN_IN) {
                    supabase.auth.signInWith(Email) {
                        email = state.email
                        password = state.password
                    }
                } else if (state.isAnonymous) {
                    // Upgrade the anonymous user in place. Its UUID and transaction ownership stay unchanged.
                    supabase.auth.updateUser {
                        email = state.email
                        password = state.password
                    }
                } else {
                    supabase.auth.signUpWith(Email) {
                        email = state.email
                        password = state.password
                    }
                }
            }.onSuccess {
                val currentUser = supabase.auth.currentUserOrNull()
                val signedIn = !currentUser?.email.isNullOrBlank()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAuthenticated = signedIn,
                        isAnonymous = !signedIn && currentUser != null,
                        accountEmail = currentUser?.email,
                        successMessage = if (signedIn) {
                            null
                        } else {
                            "Check your email to confirm the account, then return and sign in."
                        }
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = friendlyMessage(error)
                    )
                }
            }
        }
    }

    fun sendPasswordReset() {
        val email = _uiState.value.email
        if (!email.contains("@")) {
            _uiState.update { it.copy(errorMessage = "Enter your email address first.") }
            return
        }
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { supabase.auth.resetPasswordForEmail(email) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Password reset instructions were sent to $email."
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = friendlyMessage(error))
                    }
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            runCatching {
                transactions.clearLocalCache()
                accounts.clearLocalCache()
                supabase.auth.signOut()
            }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = friendlyMessage(error)) }
                }
        }
    }

    private fun validate(state: AuthUiState): String? = when {
        !state.email.contains("@") || !state.email.contains(".") ->
            "Enter a valid email address."
        state.password.length < 8 ->
            "Password must contain at least 8 characters."
        state.mode == AuthMode.SIGN_UP && state.password != state.confirmPassword ->
            "Passwords do not match."
        else -> null
    }

    private fun friendlyMessage(error: Throwable): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("Invalid login credentials", ignoreCase = true) ->
                "Incorrect email or password."
            message.contains("Email not confirmed", ignoreCase = true) ->
                "Confirm your email first, then sign in."
            message.contains("already registered", ignoreCase = true) ->
                "An account already exists for this email. Sign in instead."
            message.contains("weak", ignoreCase = true) ->
                "Choose a stronger password."
            message.isNotBlank() -> message
            else -> "Authentication failed. Please try again."
        }
    }
}
