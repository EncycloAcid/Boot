package com.boot.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.max

/** Hairline grid with labels at zero, half and the top of the scale. */
private fun DrawScope.grid(top: Double, measurer: TextMeasurer, style: TextStyle, gridColor: Color, format: (Double) -> String) {
    for (f in listOf(0.0, 0.5, 1.0)) {
        val y = size.height * (1 - f).toFloat()
        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        if (f > 0) drawText(measurer, format(top * f), topLeft = Offset(4f, y + 2f), style = style)
    }
}

/**
 * Stacked weekly bars (one colored segment per revenue source) with an optional dashed line on top,
 * used for revenue against costs.
 */
@Composable
fun StackedBarChart(
    bars: List<List<Pair<Double, Color>>>,
    line: List<Double>?,
    lineColor: Color,
    format: (Double) -> String,
    modifier: Modifier = Modifier,
) {
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier.fillMaxWidth().height(180.dp)) {
        if (bars.isEmpty()) return@Canvas
        val maxBar = bars.maxOf { stack -> stack.sumOf { max(0.0, it.first) } }
        val top = max(max(maxBar, line?.maxOrNull() ?: 0.0), 1.0) * 1.1
        grid(top, measurer, labelStyle, gridColor, format)
        val slot = size.width / bars.size
        val barWidth = max(1f, slot * 0.7f)
        bars.forEachIndexed { i, stack ->
            var base = size.height
            for ((value, color) in stack) {
                if (value <= 0) continue
                val h = (value / top * size.height).toFloat()
                drawRect(color, Offset(i * slot + (slot - barWidth) / 2, base - h), Size(barWidth, h))
                base -= h
            }
        }
        if (line != null && line.size > 1) {
            val path = Path()
            line.forEachIndexed { i, v ->
                val x = i * slot + slot / 2
                val y = (size.height - v / top * size.height).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, lineColor, style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
        }
    }
}

/** A filled area for counts that only matter in shape (users over time). */
@Composable
fun AreaChart(values: List<Double>, color: Color, format: (Double) -> String, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier.fillMaxWidth().height(140.dp)) {
        if (values.size < 2) return@Canvas
        val top = max(values.max(), 1.0) * 1.1
        grid(top, measurer, labelStyle, gridColor, format)
        val step = size.width / (values.size - 1)
        val path = Path().apply {
            moveTo(0f, size.height)
            values.forEachIndexed { i, v -> lineTo(i * step, (size.height - v / top * size.height).toFloat()) }
            lineTo(size.width, size.height)
            close()
        }
        drawPath(path, Brush.verticalGradient(listOf(color.copy(alpha = 0.45f), color.copy(alpha = 0.05f))))
        val edge = Path()
        values.forEachIndexed { i, v ->
            val p = Offset(i * step, (size.height - v / top * size.height).toFloat())
            if (i == 0) edge.moveTo(p.x, p.y) else edge.lineTo(p.x, p.y)
        }
        drawPath(edge, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}

/** A plain line for rates (weekly churn). */
@Composable
fun LineChart(values: List<Double>, color: Color, format: (Double) -> String, modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier.fillMaxWidth().height(140.dp)) {
        if (values.size < 2) return@Canvas
        val top = max(values.max(), 1e-6) * 1.2
        grid(top, measurer, labelStyle, gridColor, format)
        val step = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { i, v ->
            val p = Offset(i * step, (size.height - v / top * size.height).toFloat())
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        val last = values.last()
        drawRoundRect(
            color, topLeft = Offset(size.width - 6.dp.toPx(), (size.height - last / top * size.height).toFloat() - 3.dp.toPx()),
            size = Size(6.dp.toPx(), 6.dp.toPx()), cornerRadius = CornerRadius(3.dp.toPx()),
        )
    }
}
