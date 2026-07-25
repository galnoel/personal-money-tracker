package com.tracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tracker.domain.model.CategoryTotal
import com.tracker.domain.model.PeriodType
import com.tracker.domain.model.Transaction
import com.tracker.domain.model.TransactionType
import com.tracker.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

data class DashboardUiState(
    val selectedPeriod: PeriodType = PeriodType.MONTH,
    val totalIncome: Long = 0,
    val totalExpense: Long = 0,
    val allTimeBalance: Long = 0,
    val categoryTotals: List<CategoryTotal> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val periodLabel: String = "",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    private val selectedPeriod = MutableStateFlow(PeriodType.MONTH)
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            selectedPeriod
                .combine(repository.getAllTransactions()) { period, all ->
                    val filtered = filterForPeriod(all, period)
                    val income = filtered.filter { it.type == TransactionType.IN }.sumOf { it.amount }
                    val expense = filtered.filter { it.type == TransactionType.OUT }.sumOf { it.amount }
                    val lifetimeIncome = all.filter { it.type == TransactionType.IN }.sumOf { it.amount }
                    val lifetimeExpense = all.filter { it.type == TransactionType.OUT }.sumOf { it.amount }
                    DashboardUiState(
                        selectedPeriod = period,
                        totalIncome = income,
                        totalExpense = expense,
                        allTimeBalance = lifetimeIncome - lifetimeExpense,
                        categoryTotals = filtered
                            .filter { it.type == TransactionType.OUT }
                            .groupBy { it.category }
                            .map { (category, items) ->
                                CategoryTotal(category, items.sumOf { it.amount })
                            }
                            .sortedByDescending { it.total },
                        recentTransactions = all.take(5),
                        periodLabel = periodLabel(period),
                        isLoading = false
                    )
                }
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Unable to reach Supabase"
                        )
                    }
                }
                .collect { _uiState.value = it }
        }
    }

    fun selectPeriod(period: PeriodType) {
        selectedPeriod.value = period
    }

    fun retry() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching { repository.refresh() }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message)
                    }
                }
        }
    }

    fun formatAmount(cents: Long): String {
        val whole = cents / 100
        val fraction = kotlin.math.abs(cents % 100)
        return String.format(Locale.getDefault(), "%,d.%02d", whole, fraction)
    }

    private fun filterForPeriod(
        transactions: List<Transaction>,
        period: PeriodType
    ): List<Transaction> {
        if (period == PeriodType.ALL) return transactions
        val (start, end) = dateRange(period)
        return transactions.filter { it.date in start until end }
    }

    private fun dateRange(period: PeriodType): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val now = LocalDate.now()
        val (start, end) = when (period) {
            PeriodType.ALL -> LocalDate.of(1970, 1, 1).atStartOfDay() to
                LocalDate.of(3000, 1, 1).atStartOfDay()
            PeriodType.DAY -> now.atStartOfDay() to now.plusDays(1).atStartOfDay()
            PeriodType.WEEK -> {
                val weekStart = now.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)
                weekStart.atStartOfDay() to weekStart.plusWeeks(1).atStartOfDay()
            }
            PeriodType.MONTH -> now.withDayOfMonth(1).atStartOfDay() to
                now.plusMonths(1).withDayOfMonth(1).atStartOfDay()
            PeriodType.SIX_MONTHS -> now.minusMonths(5).withDayOfMonth(1).atStartOfDay() to
                now.plusMonths(1).withDayOfMonth(1).atStartOfDay()
            PeriodType.YEAR -> now.withDayOfYear(1).atStartOfDay() to
                now.plusYears(1).withDayOfYear(1).atStartOfDay()
        }
        return start.atZone(zone).toInstant().toEpochMilli() to
            end.atZone(zone).toInstant().toEpochMilli()
    }

    private fun periodLabel(period: PeriodType): String {
        val now = LocalDate.now()
        return when (period) {
            PeriodType.ALL -> "Across every transaction"
            PeriodType.DAY -> now.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
            PeriodType.WEEK -> "This week"
            PeriodType.MONTH -> now.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            PeriodType.SIX_MONTHS -> "Last 6 months"
            PeriodType.YEAR -> now.year.toString()
        }
    }
}
