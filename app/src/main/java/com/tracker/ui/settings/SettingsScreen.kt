package com.tracker.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tracker.ui.components.NeumorphicCard
import com.tracker.ui.theme.LightBackground
import com.tracker.ui.theme.Primary
import java.util.Locale

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var dialog by remember { mutableStateOf<AccountDialog?>(null) }
    val accents = listOf("#4F46E5", "#2563EB", "#0F766E", "#7C3AED", "#DB2777", "#EA580C")

    LazyColumn(
        Modifier.fillMaxSize().background(LightBackground),
        contentPadding = PaddingValues(20.dp, 24.dp, 20.dp, 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Accounts & settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Balances are all-time; reports never include transfers.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            NeumorphicCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("App color", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        accents.forEach { hex ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            Surface(
                                modifier = Modifier.size(42.dp).clickable { viewModel.setAccent(hex) },
                                shape = CircleShape,
                                color = color,
                                border = if (state.preferences.accentHex.equals(hex, true)) {
                                    androidx.compose.foundation.BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface)
                                } else null
                            ) {}
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    var custom by remember(state.preferences.accentHex) { mutableStateOf(state.preferences.accentHex) }
                    OutlinedTextField(
                        custom,
                        { custom = it.take(7).uppercase(Locale.US) },
                        label = { Text("Custom hex") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { if (Regex("^#[0-9A-F]{6}$").matches(custom)) viewModel.setAccent(custom) }) {
                                Icon(Icons.Rounded.Check, "Apply color")
                            }
                        }
                    )
                    Spacer(Modifier.height(14.dp))
                    Text("Base currency", fontWeight = FontWeight.SemiBold)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("SGD", "USD", "EUR", "GBP", "IDR", "JPY").forEach { code ->
                            FilterChip(
                                selected = code == state.preferences.currencyCode,
                                onClick = { viewModel.setCurrency(code) },
                                label = { Text(code) }
                            )
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Financial accounts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                FilledTonalButton(onClick = { dialog = AccountDialog.Create }) {
                    Icon(Icons.Rounded.Add, null)
                    Text("Add")
                }
            }
        }
        items(state.accounts.filterNot { it.account.archived }, key = { it.account.id }) { item ->
            var showActions by remember(item.account.id) { mutableStateOf(false) }
            NeumorphicCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AccountBalanceWallet, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.account.name, fontWeight = FontWeight.SemiBold)
                        Text(formatCents(item.balance, state.preferences.currencyCode), style = MaterialTheme.typography.bodySmall)
                    }
                    Box {
                        IconButton(onClick = { showActions = true }) {
                            Icon(Icons.Rounded.MoreVert, "Account actions")
                        }
                        DropdownMenu(expanded = showActions, onDismissRequest = { showActions = false }) {
                            DropdownMenuItem(
                                text = { Text("Move up") },
                                leadingIcon = { Icon(Icons.Rounded.KeyboardArrowUp, null) },
                                onClick = { viewModel.move(item.account.id, -1); showActions = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Move down") },
                                leadingIcon = { Icon(Icons.Rounded.KeyboardArrowDown, null) },
                                onClick = { viewModel.move(item.account.id, 1); showActions = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                                onClick = { dialog = AccountDialog.Edit(item.account.id, item.account.name); showActions = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Reconcile balance") },
                                leadingIcon = { Icon(Icons.Rounded.Balance, null) },
                                onClick = { dialog = AccountDialog.Reconcile(item.account.id, item.account.name); showActions = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Archive") },
                                leadingIcon = { Icon(Icons.Rounded.Archive, null) },
                                onClick = { dialog = AccountDialog.Archive(item.account.id, item.account.name); showActions = false }
                            )
                        }
                    }
                }
            }
        }
        if (state.accounts.any { it.account.archived }) {
            item {
                Column {
                    Text(
                        "Archived accounts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Hidden from Dashboard and Quick Add",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(state.accounts.filter { it.account.archived }, key = { "archived-${it.account.id}" }) { item ->
                NeumorphicCard(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.account.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                formatCents(item.balance, state.preferences.currencyCode),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        FilledTonalButton(onClick = { viewModel.unarchive(item.account.id) }) {
                            Icon(Icons.Rounded.Unarchive, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Restore")
                        }
                    }
                }
            }
        }
        state.message?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
    }
    dialog?.let { value ->
        AccountDialogUi(
            value,
            onDismiss = { dialog = null },
            onCreate = viewModel::create,
            onRename = viewModel::rename,
            onReconcile = viewModel::reconcile,
            onArchive = viewModel::archive
        )
    }
}

private sealed interface AccountDialog {
    data object Create : AccountDialog
    data class Edit(val id: String, val name: String) : AccountDialog
    data class Reconcile(val id: String, val name: String) : AccountDialog
    data class Archive(val id: String, val name: String) : AccountDialog
}

@Composable
private fun AccountDialogUi(
    dialog: AccountDialog,
    onDismiss: () -> Unit,
    onCreate: (String, Long) -> Unit,
    onRename: (String, String) -> Unit,
    onReconcile: (String, Long) -> Unit,
    onArchive: (String) -> Unit
) {
    var text by remember { mutableStateOf(if (dialog is AccountDialog.Edit) dialog.name else "") }
    val title = when (dialog) {
        AccountDialog.Create -> "New account"
        is AccountDialog.Edit -> "Rename account"
        is AccountDialog.Reconcile -> "Reconcile ${dialog.name}"
        is AccountDialog.Archive -> "Archive ${dialog.name}?"
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (dialog is AccountDialog.Archive) Text("Historical references remain safe and the account is hidden.")
            else OutlinedTextField(
                text, { text = it },
                label = { Text(if (dialog is AccountDialog.Reconcile) "Real current balance" else "Account name") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = if (dialog is AccountDialog.Reconcile) androidx.compose.ui.text.input.KeyboardType.Decimal
                    else androidx.compose.ui.text.input.KeyboardType.Text
                ),
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = {
                when (dialog) {
                    AccountDialog.Create -> onCreate(text, 0)
                    is AccountDialog.Edit -> onRename(dialog.id, text)
                    is AccountDialog.Reconcile -> onReconcile(dialog.id, parseCents(text) ?: 0)
                    is AccountDialog.Archive -> onArchive(dialog.id)
                }
                onDismiss()
            }) { Text("Confirm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun parseCents(value: String) = value.replace(",", "").toBigDecimalOrNull()?.movePointRight(2)?.toLong()
private fun formatCents(value: Long, currency: String): String {
    val symbol = runCatching { java.util.Currency.getInstance(currency).symbol }.getOrDefault(currency)
    return "$symbol ${String.format(Locale.getDefault(), "%,.2f", value / 100.0)}"
}
