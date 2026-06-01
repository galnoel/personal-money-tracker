package com.tracker.ui.quickinput

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import com.tracker.ui.theme.MoneyTrackerTheme

@AndroidEntryPoint
class QuickInputActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureBlurredBackdrop()

        setContent {
            MoneyTrackerTheme {
                QuickInputScreen(
                    onDismiss = { finish() }
                )
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
