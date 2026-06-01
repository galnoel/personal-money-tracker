package com.tracker.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tracker.domain.model.PeriodType
import com.tracker.domain.model.TransactionType
import com.tracker.ui.components.*
import com.tracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToTransactions: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Money Tracker",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = state.periodLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Period selector
        item {
            ScrollableTabRow(
                selectedTabIndex = PeriodType.entries.indexOf(state.selectedPeriod),
                modifier = Modifier.padding(bottom = 16.dp),
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 20.dp,
                divider = {}
            ) {
                PeriodType.entries.forEach { period ->
                    val selected = state.selectedPeriod == period
                    Tab(
                        selected = selected,
                        onClick = { viewModel.selectPeriod(period) },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = period.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Balance card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Balance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val balance = state.totalIncome - state.totalExpense
                    val balanceColor = if (balance >= 0) IncomeGreen else ExpenseRed
                    Text(
                        text = "${if (balance >= 0) "+" else "-"}${viewModel.formatAmount(kotlin.math.abs(balance))}",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp
                        ),
                        color = balanceColor
                    )
                }
            }
        }

        // Income & Expense cards
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Income",
                    amount = "+${viewModel.formatAmount(state.totalIncome)}",
                    icon = Icons.Rounded.TrendingUp,
                    color = IncomeGreen,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Expense",
                    amount = "-${viewModel.formatAmount(state.totalExpense)}",
                    icon = Icons.Rounded.TrendingDown,
                    color = ExpenseRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Category breakdown
        if (state.categoryTotals.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Expense by Category",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        PieChart(
                            slices = state.categoryTotals.map { cat ->
                                PieSlice(
                                    label = cat.category,
                                    value = cat.total.toFloat(),
                                    color = CategoryIcons.getColor(cat.category)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Recent transactions
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = onNavigateToTransactions) {
                    Text("See All")
                }
            }
        }

        if (state.recentTransactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = TextTertiary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No transactions yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextTertiary
                        )
                        Text(
                            text = "Tap + on the widget to add one!",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextTertiary
                        )
                    }
                }
            }
        }

        items(state.recentTransactions) { transaction ->
            TransactionItem(
                transaction = transaction,
                formatAmount = viewModel::formatAmount,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun TransactionItem(
    transaction: com.tracker.domain.model.Transaction,
    formatAmount: (Long) -> String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val isIncome = transaction.type == TransactionType.IN

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CategoryIcons.getColor(transaction.category).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CategoryIcons.getIcon(transaction.category),
                    contentDescription = null,
                    tint = CategoryIcons.getColor(transaction.category),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Description & metadata
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                    Text(
                        text = transaction.paymentMethod,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextTertiary
                    )
                }
            }

            // Amount & date
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isIncome) "+" else "-"}${formatAmount(transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = if (isIncome) IncomeGreen else ExpenseRed
                )
                Text(
                    text = dateFormat.format(Date(transaction.date)),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }
        }
    }
}
