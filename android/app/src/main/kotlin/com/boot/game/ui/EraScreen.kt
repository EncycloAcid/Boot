package com.boot.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.boot.core.EraPhase
import com.boot.core.Format
import com.boot.core.GameClock
import com.boot.core.GameState
import com.boot.core.Lane
import kotlin.math.abs

/** The era: what it is, where it is in its life, and what people say is next. */
@Composable
fun EraCard(state: GameState) {
    val w = state.world
    val era = w.era
    val scheme = MaterialTheme.colorScheme
    val colors = gameColors
    val weeks = state.week - w.eraStartWeek
    Surface(shape = RoundedCornerShape(28.dp), color = scheme.tertiaryContainer, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.background(Brush.linearGradient(listOf(scheme.tertiaryContainer, scheme.primaryContainer)))) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(era.emoji, style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("THE ERA", style = MaterialTheme.typography.labelMedium, color = scheme.onTertiaryContainer.copy(alpha = 0.7f))
                        Text(era.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = scheme.onTertiaryContainer)
                    }
                }
                Text(era.blurb, style = MaterialTheme.typography.bodyMedium, color = scheme.onTertiaryContainer)
                if (era != com.boot.core.Era.BOOTSTRAP_YEARS) {
                    // Dawn, peak, fading: where the era is in its life.
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        EraPhase.entries.forEach { p ->
                            val on = p == w.phase
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(28.dp)
                                    .background(if (on) scheme.onTertiaryContainer else scheme.onTertiaryContainer.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                FitText(
                                    p.title, MaterialTheme.typography.labelMedium,
                                    color = if (on) scheme.tertiaryContainer else scheme.onTertiaryContainer.copy(alpha = 0.7f),
                                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                                )
                            }
                        }
                    }
                }
                Text(
                    "${Format.weeks(weeks)} so far · felt at ${Format.percent(w.eraIntensity)} strength",
                    style = MaterialTheme.typography.labelMedium, color = scheme.onTertiaryContainer.copy(alpha = 0.75f),
                )
                w.nextEra?.let { next ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(scheme.surface.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                    ) {
                        Column {
                            Text("ON THE HORIZON", style = MaterialTheme.typography.labelSmall, color = colors.milestone, fontWeight = FontWeight.Bold)
                            Text(next.rumor, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

/** What the era does to each business, and to costs and investors. */
@Composable
fun EraEffects(state: GameState) {
    val w = state.world
    val colors = gameColors
    fun pct(x: Double) = if (abs(x - 1) < 0.005) "—" else (if (x > 1) "+" else "") + Format.percent(x - 1)
    @Composable
    fun row(label: String, x: Double, mine: Boolean = false, goodWhenUp: Boolean = true) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = if (mine) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.weight(1f))
            val better = if (goodWhenUp) x > 1.005 else x < 0.995
            val worse = if (goodWhenUp) x < 0.995 else x > 1.005
            Text(
                pct(x), style = MaterialTheme.typography.bodyLarge,
                color = when {
                    better -> colors.good
                    worse -> colors.warn
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
    GroupedSection("Demand this era", footer = "At today's strength. Eras build up at dawn and ease off as they fade.") {
        Lane.entries.forEachIndexed { i, lane ->
            if (i > 0) RowDivider()
            row("${lane.emoji}  ${lane.title}", w.demand(lane), mine = lane in state.lanes)
        }
    }
    GroupedSection("Costs and money") {
        row("Salaries", w.salaries, goodWhenUp = false)
        RowDivider()
        row("GPU prices", w.gpuPrice, goodWhenUp = false)
        RowDivider()
        row("Hardware parts", w.hardwareCost, goodWhenUp = false)
        RowDivider()
        row("Investor appetite", w.investorAppetite)
    }
}

/** Every era so far, oldest first. */
@Composable
fun EraTimeline(state: GameState) {
    val w = state.world
    GroupedSection("Timeline") {
        w.history.forEachIndexed { i, span ->
            if (i > 0) RowDivider(56.dp)
            ListRow(
                span.era.title,
                subtitle = "${GameClock.label(span.startWeek)} to ${GameClock.label(span.endWeek)}",
                leading = { Text(span.era.emoji, style = MaterialTheme.typography.titleLarge) },
            )
        }
        if (w.history.isNotEmpty()) RowDivider(56.dp)
        ListRow(
            w.era.title,
            subtitle = "Since ${GameClock.label(w.eraStartWeek)} · now",
            leading = { Text(w.era.emoji, style = MaterialTheme.typography.titleLarge) },
        )
    }
}
