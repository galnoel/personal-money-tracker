package com.tracker.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tracker.ui.theme.ChartColors
import kotlin.math.cos
import kotlin.math.sin

data class PieSlice(
    val label: String,
    val value: Float,
    val color: Color
)

@Composable
fun PieChart(
    slices: List<PieSlice>,
    modifier: Modifier = Modifier,
    showLabels: Boolean = true
) {
    if (slices.isEmpty()) return

    val total = slices.sumOf { it.value.toDouble() }.toFloat()
    if (total == 0f) return

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .size(180.dp)
                .padding(8.dp)
        ) {
            val strokeWidth = 32.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)
            var startAngle = -90f

            slices.forEach { slice ->
                val sweepAngle = (slice.value / total) * 360f * animatedProgress.value
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
                startAngle += sweepAngle
            }
        }

        if (showLabels) {
            Spacer(modifier = Modifier.height(12.dp))
            slices.take(5).forEach { slice ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(modifier = Modifier.size(8.dp)) {
                        drawCircle(color = slice.color)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = slice.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${((slice.value / total) * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
