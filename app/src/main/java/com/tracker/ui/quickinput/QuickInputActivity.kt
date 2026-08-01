package com.tracker.ui.quickinput

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import com.tracker.ui.MainActivity
import com.tracker.ui.theme.MoneyTrackerTheme
import com.tracker.ui.theme.Primary
import com.tracker.domain.model.UserPreferences
import com.tracker.domain.repository.PreferencesRepository
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class QuickInputActivity : ComponentActivity() {
    @Inject
    lateinit var supabase: SupabaseClient
    @Inject lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureBackdrop()

        lifecycleScope.launch {
            supabase.auth.awaitInitialization()
            val user = supabase.auth.currentUserOrNull()
            val isAuthenticated = !user?.email.isNullOrBlank()

            setContent {
                val preferences by preferencesRepository.preferences.collectAsStateWithLifecycle(
                    initialValue = UserPreferences()
                )
                val accent = remember(preferences.accentHex) {
                    runCatching {
                        ComposeColor(android.graphics.Color.parseColor(preferences.accentHex))
                    }.getOrDefault(Primary)
                }
                MoneyTrackerTheme(accent = accent) {
                    if (isAuthenticated) {
                        QuickInputScreen(onDismiss = { finish() })
                    } else {
                        AuthenticationRequiredPopup(
                            onDismiss = { finish() },
                            onSignIn = {
                                startActivity(
                                    Intent(this@QuickInputActivity, MainActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    }
                                )
                                finish()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun configureBackdrop() {
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

        val attributes = window.attributes
        attributes.dimAmount = 0.45f

        window.attributes = attributes
    }
}

@Composable
private fun AuthenticationRequiredPopup(
    onDismiss: () -> Unit,
    onSignIn: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor.Black.copy(alpha = 0.22f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .widthIn(max = 380.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {},
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(14.dp).size(28.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Sign in to add money",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Your launcher will stay open until you choose to sign in.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onSignIn,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open sign in")
                }
                TextButton(onClick = onDismiss) {
                    Text("Not now")
                }
            }
        }
    }
}
