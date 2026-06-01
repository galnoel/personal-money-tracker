package com.tracker.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tracker.domain.model.Transaction
import com.tracker.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionListUiState(
    val transactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class TransactionListViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionListUiState())
    val uiState: StateFlow<TransactionListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllTransactions().collect { transactions ->
                _uiState.update { it.copy(transactions = transactions, isLoading = false) }
            }
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    fun formatAmount(cents: Long): String {
        val whole = cents / 100
        val frac = cents % 100
        return String.format("%,d.%02d", whole, frac)
    }
}
