package com.boot.game.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp
import com.boot.core.GameProject
import com.boot.core.Milestone
import kotlin.math.max
import kotlin.math.min

/**
 * Where a game is in its making: one track from the first week to content complete, with the
 * vertical slice, alpha and beta marked on it. A milestone that's been reviewed is filled in.
 */
@Composable
fun BuildTimeline(p: GameProject, modifier: Modifier = Modifier) {
    val colors = gameColors
    val done by animateFloatAsState(p.completion.toFloat(), MaterialTheme.motionScheme.defaultSpatialSpec(), label = "build")
    val track = MaterialTheme.colorScheme.primary
    val trackBack = track.copy(alpha = 0.16f)
    val reached = colors.milestone
    val surface = colors.card
    val idle = MaterialTheme.colorScheme.outline
    Column(modifier.testTag("timeline"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(Modifier.fillMaxWidth().height(18.dp)) {
            val h = 6.dp.toPx()
            val y = size.height / 2
            val r = 7.dp.toPx()
            val inset = r
            val w = size.width - inset * 2
            drawRoundRect(trackBack, Offset(inset, y - h / 2), Size(w, h), CornerRadius(h / 2))
            drawRoundRect(track, Offset(inset, y - h / 2), Size(w * done, h), CornerRadius(h / 2))
            Milestone.entries.forEachIndexed { i, _ ->
                val x = inset + w * (i + 1) / 3f
                val passed = done >= (i + 1) / 3f - 0.001f
                val reviewed = i in p.milestones
                drawCircle(surface, r, Offset(x, y))
                drawCircle(if (reviewed || passed) reached else idle, r - 1.5.dp.toPx(), Offset(x, y), style = if (reviewed) androidx.compose.ui.graphics.drawscope.Fill else Stroke(2.dp.toPx()))
            }
        }
        val labels: @Composable () -> Unit = {
            Text("Start", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Milestone.entries.forEachIndexed { i, m ->
                Text(
                    m.title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (i in p.milestones) FontWeight.Bold else FontWeight.Normal,
                    color = if (i in p.milestones) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        // Each label sits under its marker, kept inside the card at the ends.
        Layout(labels, Modifier.fillMaxWidth()) { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
            val width = constraints.maxWidth
            val inset = 7.dp.roundToPx()
            val track = width - inset * 2
            layout(width, placeables.maxOf { it.height }) {
                placeables.forEachIndexed { i, pl ->
                    val x = if (i == 0) 0 else (inset + track * i / 3 - pl.width / 2).coerceIn(0, width - pl.width)
                    pl.place(x, 0)
                }
            }
        }
    }
}

/** The forecast score, week by week, as a small line with the latest reading and how it's moved. */
@Composable
fun ForecastTrend(forecasts: List<Double>, modifier: Modifier = Modifier) {
    if (forecasts.size < 2) return
    val colors = gameColors
    val now = forecasts.last()
    val back = forecasts[max(0, forecasts.size - 5)]
    val moved = now - back
    val tint = when {
        moved > 0.05 -> colors.good
        moved < -0.05 -> colors.bad
        else -> MaterialTheme.colorScheme.primary
    }
    val grid = MaterialTheme.colorScheme.outlineVariant
    Row(modifier.testTag("forecast"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(0.38f)) {
            Text("Forecast", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            Text(String.format(java.util.Locale.US, "%.1f", now), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = tint, maxLines = 1)
            Text(
                when {
                    moved > 0.05 -> String.format(java.util.Locale.US, "up %.1f in a month", moved)
                    moved < -0.05 -> String.format(java.util.Locale.US, "down %.1f in a month", -moved)
                    else -> "steady"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Box(Modifier.weight(0.62f)) {
            Canvas(Modifier.fillMaxWidth().height(44.dp)) {
                val lo = min(forecasts.min(), now - 0.5)
                val hi = max(forecasts.max(), now + 0.5)
                val span = max(0.5, hi - lo)
                fun yOf(v: Double) = (size.height - (v - lo) / span * size.height).toFloat()
                // The 7.0 line: around where a game starts to be a hit.
                if (7.0 in lo..hi) drawLine(grid, Offset(0f, yOf(7.0)), Offset(size.width, yOf(7.0)), 1.dp.toPx())
                val step = size.width / (forecasts.size - 1)
                val path = Path()
                forecasts.forEachIndexed { i, v ->
                    val x = i * step
                    if (i == 0) path.moveTo(x, yOf(v)) else path.lineTo(x, yOf(v))
                }
                drawPath(path, tint, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                drawCircle(tint, 3.5.dp.toPx(), Offset(size.width, yOf(now)))
            }
        }
    }
}
