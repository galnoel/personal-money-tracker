package com.tracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.tracker.ui.quickinput.QuickInputActivity

/** A single-purpose launcher button. It never navigates to MainActivity. */
class MoneyTrackerWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val hex = context.getSharedPreferences("widget_preferences", Context.MODE_PRIVATE)
            .getString("accent_hex", "#4F46E5") ?: "#4F46E5"
        val accent = runCatching {
            Color(android.graphics.Color.parseColor(hex))
        }.getOrDefault(Color(0xFF4F46E5))
        provideContent { QuickAddButton(accent) }
    }

    @Composable
    private fun QuickAddButton(accentColor: Color) {
        val surface = ColorProvider(Color(0xFFE9EEF5))
        val accent = ColorProvider(accentColor)
        val white = ColorProvider(Color.White)
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(20.dp)
                .background(surface)
                .padding(8.dp)
                .clickable(actionStartActivity<QuickInputActivity>()),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(16.dp)
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "+",
                    style = TextStyle(color = white, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
