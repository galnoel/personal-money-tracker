package com.tracker.ui.quickinput

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tracker.domain.model.TransactionType
import com.tracker.ui.components.CategoryIcons
import com.tracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickInputScreen(
    viewModel: QuickInputViewModel = hiltViewModel(),
    onDismiss: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showPaymentMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            kotlinx.coroutines.delay(300)
            onDismiss()
        }
    }

    // Tapping the dimmed background dismisses.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.58f))
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(horizontal = 18.dp, vertical = 28.dp)
                .fillMaxWidth()
                .widthIn(max = 430.dp)
                .heightIn(max = 680.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) { /* prevent click-through */ }
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(28.dp)
                ),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.9f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Handle bar
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(DarkBorder)
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = if (state.isEditing) "Edit Transaction" else "Quick Add",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Type toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TransactionType.entries.forEach { type ->
                        val selected = state.type == type
                        val color = if (type == TransactionType.IN) IncomeGreen else ExpenseRed
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setType(type) },
                            label = {
                                Text(
                                    type.label,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.2f),
                                selectedLabelColor = color,
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextSecondary
                            ),
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        if (type == TransactionType.IN) Icons.Rounded.TrendingUp
                                        else Icons.Rounded.TrendingDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = color
                                    )
                                }
                            } else null,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amount input
                OutlinedTextField(
                    value = state.amountText,
                    onValueChange = { viewModel.setAmount(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Amount") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = state.errorMessage != null,
                    supportingText = state.errorMessage?.let { { Text(it, color = ExpenseRed) } },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = DarkBorder,
                        cursorColor = Primary,
                        focusedLabelColor = Primary,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.AttachMoney,
                            contentDescription = null,
                            tint = Primary
                        )
                    },
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                OutlinedTextField(
                    value = state.description,
                    onValueChange = { viewModel.setDescription(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Description (optional)") },
                    placeholder = { Text("What was this for?") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = DarkBorder,
                        cursorColor = Primary,
                        focusedLabelColor = Primary,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Description,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Category & Payment method row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Category
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedCard(
                            onClick = { showCategoryMenu = true },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = Color.Transparent
                            ),
                            border = BorderStroke(1.dp, DarkBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    CategoryIcons.getIcon(state.category),
                                    contentDescription = null,
                                    tint = CategoryIcons.getColor(state.category),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = state.category,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showCategoryMenu,
                            onDismissRequest = { showCategoryMenu = false },
                            containerColor = DarkCard
                        ) {
                            CategoryIcons.categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat, color = TextPrimary) },
                                    onClick = {
                                        viewModel.setCategory(cat)
                                        showCategoryMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            CategoryIcons.getIcon(cat),
                                            contentDescription = null,
                                            tint = CategoryIcons.getColor(cat),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // Payment method
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedCard(
                            onClick = { showPaymentMenu = true },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = Color.Transparent
                            ),
                            border = BorderStroke(1.dp, DarkBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    CategoryIcons.getPaymentIcon(state.paymentMethod),
                                    contentDescription = null,
                                    tint = Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = state.paymentMethod,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1
                                )
                                Icon(
                                    Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = TextTertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showPaymentMenu,
                            onDismissRequest = { showPaymentMenu = false },
                            containerColor = DarkCard
                        ) {
                            CategoryIcons.paymentMethods.forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method, color = TextPrimary) },
                                    onClick = {
                                        viewModel.setPaymentMethod(method)
                                        showPaymentMenu = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            CategoryIcons.getPaymentIcon(method),
                                            contentDescription = null,
                                            tint = Primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Date picker
                val dateFormat = remember { SimpleDateFormat("EEE, MMM dd yyyy", Locale.getDefault()) }
                OutlinedCard(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = dateFormat.format(Date(state.date)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }

                if (showDatePicker) {
                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = state.date
                    )
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let { viewModel.setDate(it) }
                                showDatePicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Save button
                Button(
                    onClick = { viewModel.save() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !state.isSaving,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        contentColor = DarkBackground
                    )
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = DarkBackground,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Rounded.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (state.isEditing) "Update" else "Save",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                // Success animation
                AnimatedVisibility(
                    visible = state.isSaved,
                    enter = fadeIn() + scaleIn()
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = IncomeGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
