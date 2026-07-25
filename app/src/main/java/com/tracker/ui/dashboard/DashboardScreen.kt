package com.tracker.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tracker.domain.model.PeriodType
import com.tracker.domain.model.Transaction
import com.tracker.domain.model.TransactionType
import com.tracker.ui.components.CategoryIcons
import com.tracker.ui.components.GlassCard
import com.tracker.ui.components.PieChart
import com.tracker.ui.components.PieSlice
import com.tracker.ui.components.StatCard
import com.tracker.ui.theme.ExpenseRed
import com.tracker.ui.theme.IncomeGreen
import com.tracker.ui.theme.LightBackground
import com.tracker.ui.theme.Primary
import com.tracker.ui.theme.TextSecondary
import com.tracker.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    accountEmail: String? = null,
    onSignOut: () -> Unit = {},
    onNavigateToTransactions: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val background = Brush.verticalGradient(
        listOf(Color(0xFFE8EDFF), LightBackground, Color(0xFFF9F7FF))
    )

    when {
        state.isLoading -> Box(
            Modifier.fillMaxSize().background(background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Primary)
        }

        state.errorMessage != null -> ErrorState(state.errorMessage.orEmpty(), viewModel::retry)

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize().background(background),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Good to see you",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            "Your money",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (!accountEmail.isNullOrBlank()) {
                            Text(
                                accountEmail,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextTertiary,
                                maxLines = 1
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.75f),
                            shadowElevation = 4.dp
                        ) {
                            Icon(
                                Icons.Rounded.CloudDone,
                                contentDescription = "Synced with Supabase",
                                tint = Primary,
                                modifier = Modifier.padding(12.dp).size(22.dp)
                            )
                        }
                        Surface(
                            onClick = onSignOut,
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.75f),
                            shadowElevation = 4.dp
                        ) {
                            Icon(
                                Icons.Rounded.Logout,
                                contentDescription = "Sign out",
                                tint = TextSecondary,
                                modifier = Modifier.padding(12.dp).size(22.dp)
                            )
                        }
                    }
                }
            }

            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF4F46E5), Color(0xFF7C3AED))
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.82f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "TOTAL BALANCE · ALL TIME",
                                    color = Color.White.copy(alpha = 0.82f),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                            val balance = state.allTimeBalance
                            Text(
                                "${if (balance < 0) "-" else ""}${viewModel.formatAmount(abs(balance))}",
                                color = Color.White,
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Income minus expenses since your first record",
                                color = Color.White.copy(alpha = 0.72f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            item {
                Column {
                    Text(
                        "Movement period",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PeriodType.entries.forEach { period ->
                            val selected = period == state.selectedPeriod
                            Surface(
                                onClick = { viewModel.selectPeriod(period) },
                                shape = RoundedCornerShape(50),
                                color = if (selected) Primary else Color.White.copy(alpha = 0.72f),
                                shadowElevation = if (selected) 5.dp else 0.dp
                            ) {
                                Text(
                                    period.label,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                                    color = if (selected) Color.White else TextSecondary,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }

            item {
                Column {
                    Text(
                        state.periodLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth(),
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
            }

            if (state.categoryTotals.isNotEmpty()) {
                item {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(20.dp)) {
                            Text(
                                "Spending mix",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Where your money went in this period",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(Modifier.height(12.dp))
                            PieChart(
                                slices = state.categoryTotals.map {
                                    PieSlice(
                                        label = it.category,
                                        value = it.total.toFloat(),
                                        color = CategoryIcons.getColor(it.category)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Latest mutations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = onNavigateToTransactions) { Text("View & print") }
                }
            }

            if (state.recentTransactions.isEmpty()) {
                item {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Rounded.ReceiptLong,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(42.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text("No transactions yet", color = TextSecondary)
                            Text(
                                "Use the + button to add your first mutation",
                                color = TextTertiary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            items(state.recentTransactions, key = { it.id }) { transaction ->
                TransactionItem(
                    transaction = transaction,
                    formatAmount = viewModel::formatAmount
                )
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(LightBackground).padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(Modifier.fillMaxWidth()) {
            Column(
                Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Couldn’t sync your money", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(message, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) { Text("Try again") }
            }
        }
    }
}

@Composable
fun TransactionItem(
    transaction: Transaction,
    formatAmount: (Long) -> String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    val isIncome = transaction.type == TransactionType.IN
    GlassCard(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CategoryIcons.getColor(transaction.category).copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    CategoryIcons.getIcon(transaction.category),
                    contentDescription = null,
                    tint = CategoryIcons.getColor(transaction.category),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    transaction.description,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${transaction.category} · ${transaction.paymentMethod}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    maxLines = 1
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${if (isIncome) "+" else "-"}${formatAmount(transaction.amount)}",
                    color = if (isIncome) IncomeGreen else ExpenseRed,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    dateFormat.format(Date(transaction.date)),
                    color = TextTertiary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
