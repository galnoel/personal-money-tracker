package com.tracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.tracker.ui.theme.CategoryColors

object CategoryIcons {
    fun getIcon(category: String): ImageVector {
        return when (category) {
            "Food" -> Icons.Rounded.Restaurant
            "Transport" -> Icons.Rounded.DirectionsCar
            "Shopping" -> Icons.Rounded.ShoppingBag
            "Bills" -> Icons.Rounded.Receipt
            "Entertainment" -> Icons.Rounded.SportsEsports
            "Health" -> Icons.Rounded.Favorite
            "Education" -> Icons.Rounded.School
            "Salary" -> Icons.Rounded.AccountBalance
            "Freelance" -> Icons.Rounded.Laptop
            "Gift" -> Icons.Rounded.CardGiftcard
            else -> Icons.Rounded.MoreHoriz
        }
    }

    fun getColor(category: String): Color {
        return CategoryColors[category] ?: CategoryColors["Other"]!!
    }

    val categories = listOf(
        "Food", "Transport", "Shopping", "Bills",
        "Entertainment", "Health", "Education",
        "Salary", "Freelance", "Gift", "Other"
    )

    val paymentMethods = listOf(
        "Cash", "Bank Transfer", "Credit Card",
        "Debit Card", "E-Wallet", "Other"
    )

    fun getPaymentIcon(method: String): ImageVector {
        return when (method) {
            "Cash" -> Icons.Rounded.Money
            "Bank Transfer" -> Icons.Rounded.AccountBalance
            "Credit Card" -> Icons.Rounded.CreditCard
            "Debit Card" -> Icons.Rounded.CreditCard
            "E-Wallet" -> Icons.Rounded.Wallet
            else -> Icons.Rounded.Payment
        }
    }
}
