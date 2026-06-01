package com.tracker.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class BarData(
    val label: String,
    val income: Float,
    val expense: Float
)

@Composable
fun BarChart(
    data: List<BarData>,
    modifier: Modifier = Modifier,
    incomeColor: Color = MaterialTheme.colorScheme.secondary,
    expenseColor: Color = MaterialTheme.colorScheme.tertiary
) {
    if (data.isEmpty()) return

    val maxValue = data.maxOf { maxOf(it.income, it.expense) }.coerceAtLeast(1f)

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
    }

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            val barAreaWidth = size.width
            val barAreaHeight = size.height - 24.dp.toPx()
            val groupCount = data.size
            val groupWidth = barAreaWidth / groupCount
            val barWidth = (groupWidth * 0.3f).coerceAtMost(24.dp.toPx())
            val gap = 2.dp.toPx()

            // Grid lines
            val gridColor = Color(0xFF2D3440)
            for (i in 0..3) {
                val y = barAreaHeight * (i / 3f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(barAreaWidth, y),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                )
            }

            data.forEachIndexed { index, bar ->
                val groupCenterX = groupWidth * index + groupWidth / 2

                // Income bar
                val incomeHeight = (bar.income / maxValue) * barAreaHeight * animatedProgress.value
                drawRoundRect(
                    color = incomeColor,
                    topLeft = Offset(
                        groupCenterX - barWidth - gap / 2,
                        barAreaHeight - incomeHeight
                    ),
                    size = Size(barWidth, incomeHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                // Expense bar
                val expenseHeight = (bar.expense / maxValue) * barAreaHeight * animatedProgress.value
                drawRoundRect(
                    color = expenseColor,
                    topLeft = Offset(
                        groupCenterX + gap / 2,
                        barAreaHeight - expenseHeight
                    ),
                    size = Size(barWidth, expenseHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }

        // Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            data.forEach { bar ->
                Text(
                    text = bar.label,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
