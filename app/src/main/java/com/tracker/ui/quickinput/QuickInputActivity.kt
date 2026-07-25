package com.tracker.ui.quickinput

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.content.Intent
import android.os.Build
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
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class QuickInputActivity : ComponentActivity() {
    @Inject
    lateinit var supabase: SupabaseClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureBlurredBackdrop()

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
                MoneyTrackerTheme {
                    QuickInputScreen(
                        onDismiss = { finish() }
                    )
                }
            }
        }
    }

    private fun configureBlurredBackdrop() {
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

        val attributes = window.attributes
        attributes.dimAmount = 0.45f

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            attributes.blurBehindRadius = 28
        }

        window.attributes = attributes
    }
}
