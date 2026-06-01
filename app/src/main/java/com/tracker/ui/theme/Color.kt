package com.tracker.ui.theme

import androidx.compose.ui.graphics.Color

// Primary palette - Green
val Primary = Color(0xFF39FF14)
val PrimaryDark = Color(0xFF00C853)
val PrimaryLight = Color(0xFFB9F6CA)

// Income colors - Green spectrum
val IncomeGreen = Color(0xFF00E676)
val IncomeDark = Color(0xFF00C853)
val IncomeLight = Color(0xFFB9F6CA)
val IncomeSurface = Color(0xFF0D3320)

// Expense colors - Coral/Red spectrum
val ExpenseRed = Color(0xFFFF5252)
val ExpenseDark = Color(0xFFFF1744)
val ExpenseLight = Color(0xFFFF8A80)
val ExpenseSurface = Color(0xFF3D1515)

// Dark theme surfaces
val DarkBackground = Color(0xFF000000)
val DarkSurface = Color(0xFF1A1F27)
val DarkSurfaceVariant = Color(0xFF242B35)
val DarkCard = Color(0xFF1E2530)
val DarkBorder = Color(0xFF2D3440)

// Text colors
val TextPrimary = Color(0xFFECEFF4)
val TextSecondary = Color(0xFF8B95A5)
val TextTertiary = Color(0xFF5A6577)

// Chart colors
val ChartColors = listOf(
    Primary,
    Color(0xFFFF6D00),
    Color(0xFF7C4DFF),
    Color(0xFFFFD600),
    Color(0xFF00B0FF),
    Color(0xFFFF4081),
    Color(0xFF76FF03),
    Color(0xFFFF9100),
    Color(0xFF536DFE),
    Color(0xFFEA80FC),
    Color(0xFF00E676),
    Color(0xFFFF6E40)
)

// Category colors
val CategoryColors = mapOf(
    "Food" to Color(0xFFFF6D00),
    "Transport" to Color(0xFF00B0FF),
    "Shopping" to Color(0xFFFF4081),
    "Bills" to Color(0xFFFFD600),
    "Entertainment" to Color(0xFF7C4DFF),
    "Health" to Color(0xFFFF5252),
    "Education" to Color(0xFF536DFE),
    "Salary" to Color(0xFF00E676),
    "Freelance" to Color(0xFF1DE9B6),
    "Gift" to Color(0xFFEA80FC),
    "Other" to Color(0xFF8B95A5)
)
