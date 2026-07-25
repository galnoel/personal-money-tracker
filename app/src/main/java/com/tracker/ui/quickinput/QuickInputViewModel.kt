package com.tracker.ui.quickinput

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tracker.domain.model.Transaction
import com.tracker.domain.model.TransactionType
import com.tracker.domain.model.PaymentAccount
import com.tracker.domain.model.Transfer
import com.tracker.domain.repository.AccountRepository
import com.tracker.domain.repository.PreferencesRepository
import com.tracker.domain.repository.TransactionRepository
import com.tracker.ui.components.CategoryIcons
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

data class QuickInputUiState(
    val mode: QuickInputMode = QuickInputMode.EXPENSE,
    val type: TransactionType = TransactionType.OUT,
    val amountText: String = "",
    val description: String = "",
    val category: String = "Food",
    val paymentMethod: String = "Cash",
    val paymentMethods: List<String> = CategoryIcons.paymentMethods,
    val accounts: List<PaymentAccount> = emptyList(),
    val accountId: String? = null,
    val destinationAccountId: String? = null,
    val currencyCode: String = "SGD",
    val date: Long = System.currentTimeMillis(),
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val isEditing: Boolean = false,
    val editId: Long = 0
)

enum class QuickInputMode(val label: String) { INCOME("Income"), EXPENSE("Expense"), TRANSFER("Transfer") }

@HiltViewModel
class QuickInputViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val accountsRepository: AccountRepository,
    preferencesRepository: PreferencesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickInputUiState())
    val uiState: StateFlow<QuickInputUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.preferences.collect { preferences ->
                _uiState.update { it.copy(currencyCode = preferences.currencyCode) }
            }
        }
        viewModelScope.launch {
            accountsRepository.getAccounts()
                .catch { emit(emptyList()) }
                .collect { accounts ->
                    val active = accounts.filterNot { it.archived }
                    _uiState.update {
                        it.copy(
                            accounts = active,
                            paymentMethods = active.map(PaymentAccount::name),
                            accountId = it.accountId ?: active.firstOrNull()?.id,
                            destinationAccountId = it.destinationAccountId
                                ?: active.getOrNull(1)?.id
                        )
                    }
                }
        }

        val editId = savedStateHandle.get<Long>("editId") ?: 0L
        if (editId > 0) {
            viewModelScope.launch {
                repository.getTransactionById(editId)?.let { tx ->
                    _uiState.update {
                        it.copy(
                            isEditing = true,
                            editId = tx.id,
                            type = tx.type,
                            mode = if (tx.type == TransactionType.IN) QuickInputMode.INCOME else QuickInputMode.EXPENSE,
                            amountText = formatCentsToInput(tx.amount),
                            description = tx.description,
                            category = tx.category,
                            paymentMethod = tx.paymentMethod,
                            accountId = tx.accountId,
                            date = tx.date
                        )
                    }
                }
            }
        }
    }

    fun setMode(mode: QuickInputMode) {
        _uiState.update {
            it.copy(
                mode = mode,
                type = if (mode == QuickInputMode.INCOME) TransactionType.IN else TransactionType.OUT
            )
        }
    }

    fun setType(type: TransactionType) {
        _uiState.update { it.copy(type = type) }
    }

    fun setAmount(text: String) {
        val raw = text.replace(",", "").filter { it.isDigit() || it == '.' }
        if (raw.isBlank()) {
            _uiState.update { it.copy(amountText = "", errorMessage = null) }
            return
        }
        val wholeRaw = raw.substringBefore(".").take(15)
        val whole = wholeRaw.trimStart('0').ifBlank { "0" }
        val groupedWhole = whole
            .reversed()
            .chunked(3)
            .joinToString(",")
            .reversed()
        val decimals = if (raw.contains(".")) raw.substringAfter(".").replace(".", "").take(2) else null
        val formatted = if (decimals != null) "$groupedWhole.$decimals" else groupedWhole
        _uiState.update { it.copy(amountText = formatted, errorMessage = null) }
    }

    fun setDescription(desc: String) {
        _uiState.update { it.copy(description = desc) }
    }

    fun setCategory(category: String) {
        _uiState.update { it.copy(category = category) }
    }

    fun setPaymentMethod(method: String) {
        _uiState.update {
            val account = it.accounts.firstOrNull { account -> account.name == method }
            it.copy(paymentMethod = method, accountId = account?.id ?: it.accountId)
        }
    }

    fun setAccount(id: String) {
        _uiState.update {
            it.copy(accountId = id, paymentMethod = it.accounts.firstOrNull { a -> a.id == id }?.name ?: it.paymentMethod)
        }
    }

    fun setDestinationAccount(id: String) {
        _uiState.update { it.copy(destinationAccountId = id) }
    }

    fun addPaymentMethod(method: String) {
        val clean = method.trim().replace(Regex("\\s+"), " ").take(40)
        if (clean.isBlank()) return
        viewModelScope.launch {
            runCatching { accountsRepository.createAccount(clean) }
                .onSuccess { account ->
                    _uiState.update { it.copy(paymentMethod = account.name, accountId = account.id) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message ?: "Could not create account") }
                }
        }
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
                if (state.mode == QuickInputMode.TRANSFER) {
                    val source = state.accountId
                    val destination = state.destinationAccountId
                    if (source == null || destination == null || source == destination) {
                        _uiState.update {
                            it.copy(isSaving = false, errorMessage = "Choose two different accounts")
                        }
                        return@launch
                    }
                    accountsRepository.createTransfer(
                        Transfer(
                            sourceAccountId = source,
                            destinationAccountId = destination,
                            amount = cents,
                            description = state.description,
                            date = state.date
                        )
                    )
                    _uiState.update { it.copy(isSaving = false, isSaved = true) }
                    return@launch
                }
                val transaction = Transaction(
                    id = if (state.isEditing) state.editId else 0,
                    type = state.type,
                    amount = cents,
                    description = state.description.ifBlank { state.category },
                    date = state.date,
                    category = state.category,
                    paymentMethod = state.accounts.firstOrNull { it.id == state.accountId }?.name
                        ?: state.paymentMethod,
                    accountId = state.accountId
                )

                if (state.isEditing) {
                    repository.updateTransaction(transaction)
                } else {
                    repository.insertTransaction(transaction)
                }

                _uiState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                val detail = e.message
                    ?.lineSequence()
                    ?.firstOrNull()
                    ?.take(180)
                    .orEmpty()
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = if (detail.isBlank()) {
                            "Could not save the transaction. Check your connection and sign in again."
                        } else {
                            "Could not save: $detail"
                        }
                    )
                }
            }
        }
    }

    private fun parseAmountToCents(text: String): Long? {
        if (text.isBlank()) return null
        return try {
            text.replace(",", "").toBigDecimal().movePointRight(2).toLong()
        } catch (e: Exception) {
            null
        }
    }

    private fun formatCentsToInput(cents: Long): String {
        val whole = cents / 100
        val frac = cents % 100
        val formattedWhole = String.format(Locale.US, "%,d", whole)
        return if (frac > 0) "$formattedWhole.${String.format("%02d", frac)}" else formattedWhole
    }
}
