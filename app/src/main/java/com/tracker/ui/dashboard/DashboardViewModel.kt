package com.tracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tracker.domain.model.CategoryTotal
import com.tracker.domain.model.AccountBalance
import com.tracker.domain.model.ChartPoint
import com.tracker.domain.model.SyncStatus
import com.tracker.domain.model.PeriodType
import com.tracker.domain.model.Transaction
import com.tracker.domain.model.TransactionType
import com.tracker.domain.repository.TransactionRepository
import com.tracker.domain.repository.AccountRepository
import com.tracker.domain.repository.PreferencesRepository
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
    val accountBalances: List<AccountBalance> = emptyList(),
    val periodOffset: Int = 0,
    val chartPoints: List<ChartPoint> = emptyList(),
    val currencyCode: String = "SGD",
    val syncStatus: SyncStatus = SyncStatus.Synced,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val accountsRepository: AccountRepository,
    preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val selectedPeriod = MutableStateFlow(PeriodType.MONTH)
    private val periodOffset = MutableStateFlow(0)
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.syncStatus.collect { status ->
                _uiState.update { it.copy(syncStatus = status) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.preferences.collect { preferences ->
                _uiState.update { it.copy(currencyCode = preferences.currencyCode) }
            }
        }
        viewModelScope.launch {
            combine(
                selectedPeriod,
                periodOffset,
                repository.getAllTransactions(),
                accountsRepository.getAccountBalances()
            ) { period, offset, all, accountBalances ->
                    val filtered = filterForPeriod(all, period, offset)
                    val income = filtered.filter { it.type == TransactionType.IN }.sumOf { it.amount }
                    val expense = filtered.filter { it.type == TransactionType.OUT }.sumOf { it.amount }
                    val lifetimeIncome = all.filter { it.type == TransactionType.IN }.sumOf { it.amount }
                    val lifetimeExpense = all.filter { it.type == TransactionType.OUT }.sumOf { it.amount }
                    DashboardUiState(
                        selectedPeriod = period,
                        totalIncome = income,
                        totalExpense = expense,
                        allTimeBalance = accountBalances.sumOf { it.balance }
                            .takeIf { accountBalances.isNotEmpty() }
                            ?: (lifetimeIncome - lifetimeExpense),
                        categoryTotals = filtered
                            .filter { it.type == TransactionType.OUT }
                            .groupBy { it.category }
                            .map { (category, items) ->
                                CategoryTotal(category, items.sumOf { it.amount })
                            }
                            .sortedByDescending { it.total },
                        recentTransactions = all.take(5),
                        periodLabel = periodLabel(period, offset),
                        accountBalances = accountBalances,
                        periodOffset = offset,
                        chartPoints = buildChart(filtered, period, offset),
                        currencyCode = _uiState.value.currencyCode,
                        syncStatus = repository.syncStatus.value,
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
        periodOffset.value = 0
    }

    fun previousPeriod() { periodOffset.value -= 1 }

    fun nextPeriod() {
        if (periodOffset.value < 0) periodOffset.value += 1
    }

    fun retry() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                accountsRepository.refresh()
                repository.refresh()
            }
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

    fun formatMoney(cents: Long): String {
        val symbol = runCatching {
            java.util.Currency.getInstance(_uiState.value.currencyCode).symbol
        }.getOrDefault(_uiState.value.currencyCode)
        return "$symbol ${formatAmount(cents)}"
    }

    private fun buildChart(
        transactions: List<Transaction>,
        period: PeriodType,
        offset: Int
    ): List<ChartPoint> {
        val zone = ZoneId.systemDefault()
        val start = periodDates(period, offset).first
        val count = when (period) {
            PeriodType.DAY -> 24
            PeriodType.WEEK -> 7
            PeriodType.MONTH -> start.lengthOfMonth()
            PeriodType.SIX_MONTHS -> 6
            PeriodType.YEAR -> 12
        }
        return (0 until count).map { index ->
            val bucket = transactions.filter {
                val local = java.time.Instant.ofEpochMilli(it.date).atZone(zone).toLocalDateTime()
                when (period) {
                    PeriodType.DAY -> local.hour == index
                    PeriodType.WEEK -> local.toLocalDate() == start.plusDays(index.toLong())
                    PeriodType.MONTH -> local.dayOfMonth == index + 1
                    PeriodType.SIX_MONTHS,
                    PeriodType.YEAR -> local.toLocalDate().withDayOfMonth(1) ==
                        start.plusMonths(index.toLong())
                }
            }
            ChartPoint(
                label = when (period) {
                    PeriodType.DAY -> String.format("%02d:00", index)
                    PeriodType.WEEK -> start.plusDays(index.toLong())
                        .format(DateTimeFormatter.ofPattern("EEE d"))
                    PeriodType.MONTH -> (index + 1).toString()
                    PeriodType.SIX_MONTHS,
                    PeriodType.YEAR -> start.plusMonths(index.toLong())
                        .format(DateTimeFormatter.ofPattern("MMM"))
                },
                income = bucket.filter { it.type == TransactionType.IN }.sumOf { it.amount },
                expense = bucket.filter { it.type == TransactionType.OUT }.sumOf { it.amount }
            )
        }
    }

    private fun filterForPeriod(
        transactions: List<Transaction>,
        period: PeriodType,
        offset: Int
    ): List<Transaction> {
        val (start, end) = dateRange(period, offset)
        return transactions.filter { it.date in start until end }
    }

    private fun dateRange(period: PeriodType, offset: Int): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val (start, end) = periodDates(period, offset)
        return start.atStartOfDay(zone).toInstant().toEpochMilli() to
            end.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    private fun periodDates(period: PeriodType, offset: Int): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        return when (period) {
            PeriodType.DAY -> {
                val start = today.plusDays(offset.toLong())
                start to start.plusDays(1)
            }
            PeriodType.WEEK -> {
                val start = today
                    .with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)
                    .plusWeeks(offset.toLong())
                start to start.plusWeeks(1)
            }
            PeriodType.MONTH -> {
                val start = today.withDayOfMonth(1).plusMonths(offset.toLong())
                start to start.plusMonths(1)
            }
            PeriodType.SIX_MONTHS -> {
                val currentStartMonth = if (today.monthValue <= 6) 1 else 7
                val start = today.withMonth(currentStartMonth).withDayOfMonth(1)
                    .plusMonths(offset.toLong() * 6)
                start to start.plusMonths(6)
            }
            PeriodType.YEAR -> {
                val start = today.withDayOfYear(1).plusYears(offset.toLong())
                start to start.plusYears(1)
            }
        }
    }

    private fun periodLabel(period: PeriodType, offset: Int): String {
        val (start, endExclusive) = periodDates(period, offset)
        val end = endExclusive.minusDays(1)
        return when (period) {
            PeriodType.DAY -> if (offset == 0) {
                "Today - ${start.format(DateTimeFormatter.ofPattern("d MMM yyyy"))}"
            } else {
                start.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))
            }
            PeriodType.WEEK -> if (start.year == end.year) {
                "${start.format(DateTimeFormatter.ofPattern("d MMM"))} - " +
                    end.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
            } else {
                "${start.format(DateTimeFormatter.ofPattern("d MMM yyyy"))} - " +
                    end.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
            }
            PeriodType.MONTH -> start.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            PeriodType.SIX_MONTHS ->
                "${start.format(DateTimeFormatter.ofPattern("MMM"))} - " +
                    end.format(DateTimeFormatter.ofPattern("MMM yyyy"))
            PeriodType.YEAR -> start.year.toString()
        }
    }
}
