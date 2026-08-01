package com.tracker.ui.transactions

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tracker.domain.model.TransactionType
import com.tracker.domain.model.Transaction
import com.tracker.ui.components.NeumorphicCard
import com.tracker.ui.dashboard.TransactionItem
import com.tracker.ui.theme.ExpenseRed
import com.tracker.ui.theme.IncomeGreen
import com.tracker.ui.theme.LightBackground
import com.tracker.ui.theme.TextSecondary
import com.tracker.ui.theme.TextTertiary
import java.time.format.DateTimeFormatter

@Composable
fun TransactionListScreen(
    viewModel: TransactionListViewModel = hiltViewModel(),
    onAddTransaction: () -> Unit = {},
    onEditTransaction: (Long) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accent = MaterialTheme.colorScheme.primary
    val context = LocalContext.current
    val monthLabel = state.selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    val income = state.transactions.filter { it.type == TransactionType.IN }.sumOf { it.amount }
    val expense = state.transactions.filter { it.type == TransactionType.OUT }.sumOf { it.amount }
    var pendingDelete by remember { mutableStateOf<Transaction?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransaction,
                containerColor = accent,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp)
            ) { Icon(Icons.Rounded.Add, contentDescription = "Add transaction") }
        }
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(LightBackground),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = scaffoldPadding.calculateTopPadding() + 22.dp,
                bottom = 110.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Mutations",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Filter a month, then print or save it as PDF",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            item {
                NeumorphicCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = viewModel::previousMonth) {
                                Icon(Icons.Rounded.ChevronLeft, "Previous month")
                            }
                            Text(
                                monthLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(onClick = viewModel::nextMonth) {
                                Icon(Icons.Rounded.ChevronRight, "Next month")
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Income", color = TextTertiary, style = MaterialTheme.typography.labelMedium)
                                Text("+${viewModel.formatAmount(income)}", color = IncomeGreen, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Expense", color = TextTertiary, style = MaterialTheme.typography.labelMedium)
                                Text("-${viewModel.formatAmount(expense)}", color = ExpenseRed, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = {
                                TransactionReportPrinter.print(
                                    context,
                                    state.selectedMonth,
                                    state.transactions,
                                    state.currencyCode
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.Print, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Print $monthLabel")
                        }
                    }
                }
            }

            when {
                state.isLoading -> item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accent)
                    }
                }

                state.errorMessage != null -> item {
                    NeumorphicCard(Modifier.fillMaxWidth()) {
                        Text(
                            state.errorMessage.orEmpty(),
                            modifier = Modifier.padding(20.dp),
                            color = ExpenseRed
                        )
                    }
                }

                state.transactions.isEmpty() -> item {
                    NeumorphicCard(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.fillMaxWidth().padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Rounded.ReceiptLong,
                                contentDescription = null,
                                tint = TextTertiary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("No mutations in $monthLabel", color = TextSecondary)
                        }
                    }
                }
            }

            items(state.transactions, key = { it.id }) { transaction ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            pendingDelete = transaction
                            false
                        } else false
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        Box(
                            Modifier.fillMaxSize().padding(end = 18.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(Icons.Rounded.Delete, "Delete", tint = ExpenseRed)
                        }
                    }
                ) {
                    TransactionItem(
                        transaction = transaction,
                        formatAmount = viewModel::formatAmount,
                        onClick = { onEditTransaction(transaction.id) }
                    )
                }
            }
        }
    }

    pendingDelete?.let { transaction ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = com.tracker.ui.theme.LightSurface,
            shape = RoundedCornerShape(22.dp),
            icon = {
                Icon(Icons.Rounded.Delete, contentDescription = null, tint = ExpenseRed)
            },
            title = { Text("Delete transaction?") },
            text = {
                Text(
                    "\"${transaction.description}\" will be permanently removed. This action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(transaction.id)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text("Yes, delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Keep it")
                }
            }
        )
    }
}
