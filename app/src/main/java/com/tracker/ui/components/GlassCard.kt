package com.tracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = modifier
            .shadow(18.dp, shape, ambientColor = Color(0x1A53618B), spotColor = Color(0x1A53618B))
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xF2FFFFFF), Color(0xBFFFFFFF))
                )
            )
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.82f)), shape),
        content = content
    )
}
