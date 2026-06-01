package com.tracker.ui.quickinput

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tracker.domain.model.Transaction
import com.tracker.domain.model.TransactionType
import com.tracker.domain.repository.TransactionRepository
import com.tracker.ui.components.CategoryIcons
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuickInputUiState(
    val type: TransactionType = TransactionType.OUT,
    val amountText: String = "",
    val description: String = "",
    val category: String = "Food",
    val paymentMethod: String = "Cash",
    val date: Long = System.currentTimeMillis(),
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val isEditing: Boolean = false,
    val editId: Long = 0
)

@HiltViewModel
class QuickInputViewModel @Inject constructor(
    private val repository: TransactionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickInputUiState())
    val uiState: StateFlow<QuickInputUiState> = _uiState.asStateFlow()

    init {
        val editId = savedStateHandle.get<Long>("editId") ?: 0L
        if (editId > 0) {
            viewModelScope.launch {
                repository.getTransactionById(editId)?.let { tx ->
                    _uiState.update {
                        it.copy(
                            isEditing = true,
                            editId = tx.id,
                            type = tx.type,
                            amountText = formatCentsToInput(tx.amount),
                            description = tx.description,
                            category = tx.category,
                            paymentMethod = tx.paymentMethod,
                            date = tx.date
                        )
                    }
                }
            }
        }
    }

    fun setType(type: TransactionType) {
        _uiState.update { it.copy(type = type) }
    }

    fun setAmount(text: String) {
        // Allow only digits and one decimal point
        val filtered = text.filter { it.isDigit() || it == '.' }
        val parts = filtered.split(".")
        val sanitized = if (parts.size > 2) {
            parts[0] + "." + parts.drop(1).joinToString("")
        } else filtered
        // Limit decimal to 2 places
        val final = if (sanitized.contains(".")) {
            val decPart = sanitized.substringAfter(".")
            sanitized.substringBefore(".") + "." + decPart.take(2)
        } else sanitized
        _uiState.update { it.copy(amountText = final, errorMessage = null) }
    }

    fun setDescription(desc: String) {
        _uiState.update { it.copy(description = desc) }
    }

    fun setCategory(category: String) {
        _uiState.update { it.copy(category = category) }
    }

    fun setPaymentMethod(method: String) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    fun setDate(date: Long) {
        _uiState.update { it.copy(date = date) }
    }

    fun save() {
        val state = _uiState.value
        val cents = parseAmountToCents(state.amountText)
        if (cents == null || cents <= 0) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid amount") }
            return
        }

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                val transaction = Transaction(
                    id = if (state.isEditing) state.editId else 0,
                    type = state.type,
                    amount = cents,
                    description = state.description.ifBlank { state.category },
                    date = state.date,
                    category = state.category,
                    paymentMethod = state.paymentMethod
                )

                if (state.isEditing) {
                    repository.updateTransaction(transaction)
                } else {
                    repository.insertTransaction(transaction)
                }

                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = "Failed to save") }
            }
        }
    }

    private fun parseAmountToCents(text: String): Long? {
        if (text.isBlank()) return null
        return try {
            val value = text.toDouble()
            (value * 100).toLong()
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun formatCentsToInput(cents: Long): String {
        val whole = cents / 100
        val frac = cents % 100
        return if (frac > 0) "$whole.${String.format("%02d", frac)}" else whole.toString()
    }
}
