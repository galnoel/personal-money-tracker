package com.tracker.ui.transactions

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tracker.domain.model.Transaction
import com.tracker.ui.dashboard.TransactionItem
import com.tracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    viewModel: TransactionListViewModel = hiltViewModel(),
    onAddTransaction: () -> Unit = {},
    onEditTransaction: (Long) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransaction,
                containerColor = Primary,
                contentColor = DarkBackground,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Transaction")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header
            Text(
                text = "Transactions",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            } else if (state.transactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = TextTertiary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No transactions yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap the + button to add your first one",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextTertiary
                        )
                    }
                }
            } else {
                // Group transactions by date
                val grouped = state.transactions.groupBy { tx ->
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    sdf.format(Date(tx.date))
                }

                val dateLabelFormat = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault())
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                    Date(System.currentTimeMillis() - 86400000)
                )

                LazyColumn(
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    grouped.forEach { (dateKey, transactions) ->
                        item(key = "header_$dateKey") {
                            val label = when (dateKey) {
                                today -> "Today"
                                yesterday -> "Yesterday"
                                else -> dateLabelFormat.format(
                                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateKey)!!
                                )
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = TextSecondary,
                                modifier = Modifier.padding(
                                    horizontal = 20.dp,
                                    vertical = 8.dp
                                )
                            )
                        }
                        items(
                            items = transactions,
                            key = { it.id }
                        ) { transaction ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        viewModel.deleteTransaction(transaction.id)
                                        true
                                    } else false
                                }
                            )
                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 20.dp, vertical = 4.dp)
                                            .background(
                                                ExpenseRed.copy(alpha = 0.2f),
                                                RoundedCornerShape(16.dp)
                                            ),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            Icons.Rounded.Delete,
                                            contentDescription = "Delete",
                                            tint = ExpenseRed,
                                            modifier = Modifier.padding(end = 20.dp)
                                        )
                                    }
                                },
                                enableDismissFromStartToEnd = false
                            ) {
                                TransactionItem(
                                    transaction = transaction,
                                    formatAmount = viewModel::formatAmount,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                                    onClick = { onEditTransaction(transaction.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
