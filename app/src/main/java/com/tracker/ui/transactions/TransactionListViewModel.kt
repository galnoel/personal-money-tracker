package com.tracker.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tracker.domain.model.Transaction
import com.tracker.domain.repository.TransactionRepository
import com.tracker.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject

data class TransactionListUiState(
    val transactions: List<Transaction> = emptyList(),
    val selectedMonth: YearMonth = YearMonth.now(),
    val currencyCode: String = "SGD",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class TransactionListViewModel @Inject constructor(
    private val repository: TransactionRepository,
    preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val selectedMonth = MutableStateFlow(YearMonth.now())
    private val _uiState = MutableStateFlow(TransactionListUiState())
    val uiState: StateFlow<TransactionListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.preferences.collect { preferences ->
                _uiState.update { it.copy(currencyCode = preferences.currencyCode) }
            }
        }
        viewModelScope.launch {
            selectedMonth.combine(repository.getAllTransactions()) { month, transactions ->
                val zone = ZoneId.systemDefault()
                val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                TransactionListUiState(
                    transactions = transactions.filter { it.date in start until end },
                    selectedMonth = month,
                    currencyCode = _uiState.value.currencyCode,
                    isLoading = false
                )
            }.catch { error ->
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = error.message ?: "Unable to load transactions")
                }
            }.collect { _uiState.value = it }
        }
    }

    fun previousMonth() {
        selectedMonth.update { it.minusMonths(1) }
    }

    fun nextMonth() {
        selectedMonth.update { it.plusMonths(1) }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            runCatching { repository.deleteTransaction(id) }
                .onFailure { error -> _uiState.update { it.copy(errorMessage = error.message) } }
        }
    }

    fun formatAmount(cents: Long): String {
        val whole = cents / 100
        val fraction = kotlin.math.abs(cents % 100)
        val symbol = runCatching {
            java.util.Currency.getInstance(_uiState.value.currencyCode).symbol
        }.getOrDefault(_uiState.value.currencyCode)
        return "$symbol ${String.format(Locale.getDefault(), "%,d.%02d", whole, fraction)}"
    }
}
