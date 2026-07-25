package com.tracker.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tracker.ui.auth.AuthScreen
import com.tracker.ui.auth.AuthViewModel
import com.tracker.ui.dashboard.DashboardScreen
import com.tracker.ui.navigation.Screen
import com.tracker.ui.navigation.bottomNavItems
import com.tracker.ui.quickinput.QuickInputActivity
import com.tracker.ui.theme.*
import com.tracker.ui.transactions.TransactionListScreen
import com.tracker.ui.settings.SettingsScreen
import com.tracker.domain.model.UserPreferences
import com.tracker.domain.repository.PreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val preferences by preferencesRepository.preferences.collectAsStateWithLifecycle(
                initialValue = UserPreferences()
            )
            val accent = remember(preferences.accentHex) {
                runCatching {
                    Color(android.graphics.Color.parseColor(preferences.accentHex))
                }.getOrDefault(Primary)
            }
            MoneyTrackerTheme(accent = accent) {
                MoneyTrackerRoot(
                    onAddTransaction = {
                        startActivity(Intent(this, QuickInputActivity::class.java))
                    },
                    onEditTransaction = { id ->
                        startActivity(
                            Intent(this, QuickInputActivity::class.java).apply {
                                putExtra("editId", id)
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun MoneyTrackerRoot(
    onAddTransaction: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    when {
        authState.isInitializing -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightBackground),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        authState.isAuthenticated -> {
            MainApp(
                onAddTransaction = onAddTransaction,
                onEditTransaction = onEditTransaction,
                accountEmail = authState.accountEmail,
                onSignOut = authViewModel::signOut
            )
        }
        else -> {
            AuthScreen(
                state = authState,
                onModeChange = authViewModel::setMode,
                onEmailChange = authViewModel::setEmail,
                onPasswordChange = authViewModel::setPassword,
                onConfirmPasswordChange = authViewModel::setConfirmPassword,
                onSubmit = authViewModel::submit
            )
        }
    }
}

@Composable
fun MainApp(
    onAddTransaction: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    accountEmail: String?,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val accent = MaterialTheme.colorScheme.primary
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        containerColor = LightBackground,
        bottomBar = {
            NavigationBar(
                containerColor = LightSurface,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                // Left nav items
                bottomNavItems.forEachIndexed { index, screen ->
                    if (index == 1) {
                        // Center FAB space
                        NavigationBarItem(
                            selected = false,
                            onClick = onAddTransaction,
                            icon = {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = accent,
                                    shadowElevation = 8.dp,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = androidx.compose.ui.Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Rounded.Add,
                                            contentDescription = "Add",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            },
                            label = { Text("Add") },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent
                            )
                        )
                    }

                    val selected = currentDestination?.hierarchy?.any {
                        it.route == screen.route
                    } == true

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                screen.icon,
                                contentDescription = screen.title,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text(screen.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = accent,
                            selectedTextColor = accent,
                            unselectedIconColor = TextTertiary,
                            unselectedTextColor = TextTertiary,
                            indicatorColor = accent.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    accountEmail = accountEmail,
                    onSignOut = onSignOut,
                    onNavigateToTransactions = {
                        navController.navigate(Screen.Transactions.route)
                    }
                )
            }
            composable(Screen.Transactions.route) {
                TransactionListScreen(
                    onAddTransaction = onAddTransaction,
                    onEditTransaction = onEditTransaction
                )
            }
            composable(Screen.Accounts.route) {
                SettingsScreen()
            }
        }
    }
}
