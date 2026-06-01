package com.tracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tracker.data.dao.CategoryTotal
import com.tracker.domain.model.PeriodType
import com.tracker.domain.repository.TransactionRepository
import com.tracker.domain.model.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.*
import java.time.temporal.WeekFields
import java.util.*
import javax.inject.Inject

data class DashboardUiState(
    val selectedPeriod: PeriodType = PeriodType.MONTH,
    val totalIncome: Long = 0,
    val totalExpense: Long = 0,
    val balance: Long = 0,
    val categoryTotals: List<CategoryTotal> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val periodLabel: String = ""
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadData(PeriodType.MONTH)
    }

    fun selectPeriod(period: PeriodType) {
        loadData(period)
    }

    private fun loadData(period: PeriodType) {
        val (startDate, endDate) = getDateRange(period)
        val label = getPeriodLabel(period)

        _uiState.update { it.copy(selectedPeriod = period, periodLabel = label) }

        viewModelScope.launch {
            repository.getTotalIncomeByDateRange(startDate, endDate).collect { income ->
                _uiState.update { it.copy(totalIncome = income) }
            }
        }

        viewModelScope.launch {
            repository.getTotalExpenseByDateRange(startDate, endDate).collect { expense ->
                _uiState.update { it.copy(totalExpense = expense, balance = _uiState.value.totalIncome - expense) }
            }
        }

        viewModelScope.launch {
            repository.getCategoryTotals("OUT", startDate, endDate).collect { categories ->
                _uiState.update { it.copy(categoryTotals = categories) }
            }
        }

        viewModelScope.launch {
            repository.getRecentTransactions(5).collect { transactions ->
                _uiState.update { it.copy(recentTransactions = transactions) }
            }
        }
    }

    private fun getDateRange(period: PeriodType): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val now = LocalDate.now()
        val (start, end) = when (period) {
            PeriodType.DAY -> now.atStartOfDay() to now.plusDays(1).atStartOfDay()
            PeriodType.WEEK -> {
                val weekStart = now.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)
                weekStart.atStartOfDay() to weekStart.plusWeeks(1).atStartOfDay()
            }
            PeriodType.MONTH -> now.withDayOfMonth(1).atStartOfDay() to now.plusMonths(1).withDayOfMonth(1).atStartOfDay()
            PeriodType.SIX_MONTHS -> now.minusMonths(5).withDayOfMonth(1).atStartOfDay() to now.plusMonths(1).withDayOfMonth(1).atStartOfDay()
            PeriodType.YEAR -> now.withDayOfYear(1).atStartOfDay() to now.plusYears(1).withDayOfYear(1).atStartOfDay()
        }
        return start.atZone(zone).toInstant().toEpochMilli() to end.atZone(zone).toInstant().toEpochMilli()
    }

    private fun getPeriodLabel(period: PeriodType): String {
        val now = LocalDate.now()
        return when (period) {
            PeriodType.DAY -> now.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy"))
            PeriodType.WEEK -> "This Week"
            PeriodType.MONTH -> now.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"))
            PeriodType.SIX_MONTHS -> "Last 6 Months"
            PeriodType.YEAR -> now.year.toString()
        }
    }

    fun formatAmount(cents: Long): String {
        val whole = cents / 100
        val frac = cents % 100
        return String.format("%,d.%02d", whole, frac)
    }
}
