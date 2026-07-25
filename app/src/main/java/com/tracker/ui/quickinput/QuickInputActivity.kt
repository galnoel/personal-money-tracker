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
            if (user?.email.isNullOrBlank()) {
                startActivity(
                    Intent(this@QuickInputActivity, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                )
                finish()
                return@launch
            }

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
                    QuickInputScreen(
                        onDismiss = { finish() }
                    )
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
