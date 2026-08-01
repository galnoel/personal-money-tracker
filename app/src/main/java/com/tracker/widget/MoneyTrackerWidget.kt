package com.tracker.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
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
        val accent = ColorProvider(accentColor)
        val white = ColorProvider(Color.White)
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(20.dp)
                .background(white)
                .padding(6.dp)
                .clickable(actionRunCallback<OpenQuickInputAction>()),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "+",
                style = TextStyle(color = accent, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}

class OpenQuickInputAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        context.startActivity(
            Intent(context, QuickInputActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            }
        )
    }
}
