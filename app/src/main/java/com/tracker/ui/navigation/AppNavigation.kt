package com.tracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Rounded.Dashboard)
    object Transactions : Screen("transactions", "History", Icons.Rounded.ReceiptLong)
}

val bottomNavItems = listOf(Screen.Dashboard, Screen.Transactions)
