package com.tracker.ui.theme

import androidx.compose.ui.graphics.Color

val Primary = Color(0xFF4F46E5)
val PrimaryDark = Color(0xFF3730A3)
val PrimaryLight = Color(0xFFE0E7FF)

val IncomeGreen = Color(0xFF059669)
val IncomeDark = Color(0xFF047857)
val IncomeLight = Color(0xFFD1FAE5)
val IncomeSurface = Color(0xFFECFDF5)

val ExpenseRed = Color(0xFFE11D48)
val ExpenseDark = Color(0xFFBE123C)
val ExpenseLight = Color(0xFFFFE4E6)
val ExpenseSurface = Color(0xFFFFF1F2)

val LightBackground = Color(0xFFE9EEF5)
val LightSurface = Color(0xFFF1F4F8)
val LightSurfaceVariant = Color(0xFFE4EAF2)
val GlassWhite = Color(0xCFFFFFFF)
val LightBorder = Color(0xFFFFFFFF)

val TextPrimary = Color(0xFF172033)
val TextSecondary = Color(0xFF667085)
val TextTertiary = Color(0xFF98A2B3)

// Backward-compatible names used by existing input components.
val DarkBackground = LightBackground
val DarkSurface = LightSurface
val DarkSurfaceVariant = LightSurfaceVariant
val DarkCard = GlassWhite
val DarkBorder = Color(0xFFD7DCE8)

val ChartColors = listOf(
    Primary,
    Color(0xFFF59E0B),
    Color(0xFF8B5CF6),
    Color(0xFF06B6D4),
    Color(0xFFEC4899),
    Color(0xFF10B981),
    Color(0xFFF97316)
)

val CategoryColors = mapOf(
    "Food" to Color(0xFFF97316),
    "Transport" to Color(0xFF0EA5E9),
    "Shopping" to Color(0xFFEC4899),
    "Bills" to Color(0xFFEAB308),
    "Entertainment" to Color(0xFF8B5CF6),
    "Health" to Color(0xFFEF4444),
    "Education" to Color(0xFF6366F1),
    "Salary" to Color(0xFF10B981),
    "Freelance" to Color(0xFF14B8A6),
    "Gift" to Color(0xFFD946EF),
    "Other" to Color(0xFF64748B)
)
