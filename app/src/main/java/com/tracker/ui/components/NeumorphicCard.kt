package com.tracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tracker.ui.theme.LightSurface

@Composable
fun NeumorphicCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .shadow(
                elevation = 9.dp,
                shape = shape,
                ambientColor = Color(0x305E6878),
                spotColor = Color(0x405E6878)
            )
            .clip(shape)
            .background(LightSurface)
            .border(1.dp, Color.White.copy(alpha = 0.9f), shape),
        content = content
    )
}
