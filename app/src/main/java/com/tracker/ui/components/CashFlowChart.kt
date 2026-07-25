package com.tracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tracker.domain.model.ChartPoint
import com.tracker.ui.theme.ExpenseRed
import com.tracker.ui.theme.IncomeGreen
import com.tracker.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun CashFlowChart(
    points: List<ChartPoint>,
    formatMoney: (Long) -> String,
    modifier: Modifier = Modifier
) {
    var selected by remember(points) { mutableIntStateOf(-1) }
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LegendDot(IncomeGreen, "Income")
            Spacer(Modifier.width(16.dp))
            LegendDot(ExpenseRed, "Expense")
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(190.dp)) {
            Canvas(
                Modifier.fillMaxSize().pointerInput(points) {
                    detectTapGestures { tap ->
                        if (points.isNotEmpty()) {
                            selected = ((tap.x / size.width) * (points.size - 1))
                                .roundToInt().coerceIn(points.indices)
                        }
                    }
                }
            ) {
                val maxValue = points.maxOfOrNull { maxOf(it.income, it.expense) }
                    ?.coerceAtLeast(1) ?: 1
                repeat(4) { row ->
                    val y = size.height * row / 3f
                    drawLine(Color(0xFFD7DEE8), Offset(0f, y), Offset(size.width, y), 1f)
                }
                fun pathFor(value: (ChartPoint) -> Long): Path {
                    val path = Path()
                    points.forEachIndexed { index, point ->
                        val x = if (points.size == 1) 0f else size.width * index / (points.size - 1f)
                        val y = size.height - (value(point).toFloat() / maxValue * size.height)
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    return path
                }
                if (points.isNotEmpty()) {
                    drawPath(pathFor { it.income }, IncomeGreen, style = androidx.compose.ui.graphics.drawscope.Stroke(4f))
                    drawPath(pathFor { it.expense }, ExpenseRed, style = androidx.compose.ui.graphics.drawscope.Stroke(4f))
                }
                if (selected in points.indices) {
                    val x = if (points.size == 1) 0f else size.width * selected / (points.size - 1f)
                    drawLine(Color(0xFF667085), Offset(x, 0f), Offset(x, size.height), 2f)
                }
            }
            if (selected in points.indices) {
                val point = points[selected]
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xEEFFFFFF),
                    shadowElevation = 4.dp
                ) {
                    Text(
                        "${point.label}  In ${formatMoney(point.income)}  Out ${formatMoney(point.expense)}  Net ${formatMoney(point.net)}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(10.dp).height(10.dp).background(color, RoundedCornerShape(50)))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}
