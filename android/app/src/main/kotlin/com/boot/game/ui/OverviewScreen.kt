package com.boot.game.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.boot.core.Format
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.core.expansionBlocker
import com.boot.core.isEcosystem
import com.boot.core.rankings
import com.boot.game.GameStore
import kotlin.math.abs

/** The company at a glance: what it's worth, how it's doing, and the world around it. */
fun LazyListScope.overviewItems(store: GameStore, state: GameState) {
    item { Hero(state) }
    item { QuickStats(state) }
    item { LanesSection(store, state) }
    item { RankingsSection(state) }
    item { RevenueSection(state) }
    item { QuarterlySection(state) }
    item { EraSection(state) }
    item { ReputationSection(state) }
}

/** The expressive hero: the number that matters most, on a slowly turning cookie shape. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Hero(state: GameState) {
    val colors = gameColors
    val spin = rememberInfiniteTransition(label = "hero")
    val angle by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(60_000, easing = LinearEasing), RepeatMode.Restart), label = "angle")
    val weeklyProfit = state.trailingProfit(weeks = 4) / 4
    Surface(shape = RoundedCornerShape(30.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
        Box {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 48.dp, y = (-40).dp)
                    .size(180.dp)
                    .rotate(angle)
                    .clip(MaterialShapes.Cookie12Sided.toShape())
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            )
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                val listing = state.listing
                Text(
                    listing?.let { "${it.ticker} · \$" + String.format(java.util.Locale.US, "%.2f", it.price) + " a share" } ?: "Valuation",
                    style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    Format.money(listing?.marketCap ?: state.valuation),
                    style = MaterialTheme.typography.displaySmallEmphasized,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.size(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeroChip("You own ${Format.percent(state.venture.founderEquity)}", MaterialTheme.colorScheme.onPrimaryContainer)
                    HeroChip("${state.researchPoints.toInt()} RP", MaterialTheme.colorScheme.onPrimaryContainer)
                    HeroChip(
                        "${Format.signedMoney(weeklyProfit)}/wk",
                        if (weeklyProfit >= 0) colors.good else colors.bad,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroChip(text: String, color: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        maxLines = 1,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun QuickStats(state: GameState) {
    val colors = gameColors
    GroupedSection(null) {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile(
                "Reputation", "${state.reputation.score.toInt()}", Modifier.weight(1f),
                if (state.reputation.scars.isEmpty()) "Clean" else "${state.reputation.scars.size} scar(s)",
            )
            val runway = state.runwayWeeks
            StatTile(
                "Runway",
                when {
                    !runway.isFinite() -> "∞"
                    runway > 260 -> "5+ yrs"
                    runway >= 52 -> String.format(java.util.Locale.US, "%.1f yrs", runway / 52)
                    else -> "${runway.toInt()} wks"
                },
                Modifier.weight(1f),
                if (!runway.isFinite()) "Profitable" else "at this burn",
                if (runway < 8) colors.bad else Color.Unspecified,
            )
            StatTile("Team", "${state.staff.size + 1}", Modifier.weight(1f), state.headquarters.name)
        }
    }
}

@Composable
private fun LanesSection(store: GameStore, state: GameState) {
    val colors = gameColors
    val footer = when {
        state.pivot == null -> "The Pivot Point arrives between year 2 and year 4. A second lane is optional."
        state.expansionBlocker == null -> "Every lane is profitable. You can expand under Money."
        else -> null
    }
    GroupedSection("Your lanes", footer = footer) {
        state.lanes.forEachIndexed { i, lane ->
            if (i > 0) RowDivider(68.dp)
            val profit = state.trailingProfit(lane, 4) / 4
            ListRow(
                lane.businessName,
                subtitle = state.statusLine(lane),
                leading = { LaneBadge(lane, 40.dp) },
                value = (state.trailingRevenue(lane, 4) / 4).perWeek(),
                valueColor = MaterialTheme.colorScheme.onSurface,
                valueDetail = "${Format.signedMoney(profit)} profit",
                valueDetailColor = if (profit >= 0) colors.good else colors.bad,
                chevron = true,
                onClick = { store.openLane(lane) },
            )
        }
        if (state.isEcosystem) {
            RowDivider()
            ListRow(
                "Ecosystem company",
                subtitle = "${state.stats.ecosystemWeeks} profitable weeks. Hold it for 52.",
                leading = { Icon(Icons.Filled.EmojiEvents, null, tint = colors.milestone, modifier = Modifier.size(28.dp)) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RevenueSection(state: GameState) {
    val colors = gameColors
    val weeks = state.ledger.takeLast(52)
    val sources = Lane.entries.filter { lane -> weeks.any { (it.revenue[lane] ?: 0.0) > 0 } }
    val hasOther = weeks.any { it.otherRevenue > 0 }
    val costColor = MaterialTheme.colorScheme.onSurfaceVariant
    GroupedSection("Revenue and costs · 52 weeks") {
        CardBlock {
            StackedBarChart(
                bars = weeks.map { w -> sources.map { (w.revenue[it] ?: 0.0) to colors.lane(it) } + (w.otherRevenue to colors.other) },
                line = weeks.map { it.totalCosts },
                lineColor = costColor,
                format = Format::money,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                sources.forEach { Legend(it.title, colors.lane(it)) }
                if (hasOther) Legend("Other", colors.other)
                Legend("Costs (dashed)", costColor)
            }
        }
    }
}

@Composable
private fun Legend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun EraSection(state: GameState) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        EraCard(state)
        EraEffects(state)
        EraTimeline(state)
    }
}

@Composable
private fun ReputationSection(state: GameState) {
    val colors = gameColors
    GroupedSection(
        "Reputation",
        footer = if (state.reputation.scars.isEmpty()) {
            "No scars. People trust you, for now."
        } else {
            "Scars follow you into every lane. Ship a predatory game and your social app launches to suspicion."
        },
    ) {
        CardBlock {
            Meter("Score", state.reputation.score / 100, MaterialTheme.colorScheme.primary, "${state.reputation.score.toInt()}/100")
            for (scar in state.reputation.scars) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Meter(scar.kind.title, scar.severity, colors.warn)
                    Hint(scar.note)
                }
            }
        }
    }
}

/** The league table: the top of the industry, and the companies around you. */
@Composable
private fun RankingsSection(state: GameState) {
    val colors = gameColors
    val all = state.rankings()
    val me = all.indexOfFirst { it.mine }
    val shown = (all.take(3).indices + ((me - 2)..(me + 1)).filter { it in all.indices }).distinct().sorted()
    val next = all.getOrNull(me - 1)
    GroupedSection(
        "Industry rankings · you're #${me + 1} of ${all.size}",
        footer = next?.let { "Next to beat: ${it.name}, worth ${Format.money(it.value)}." } ?: "You're the biggest company in the world.",
    ) {
        var last = -1
        for (i in shown) {
            if (last >= 0) {
                if (i > last + 1) {
                    RowDivider()
                    Text("⋯", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }
                RowDivider()
            }
            val c = all[i]
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (c.mine) colors.milestone.copy(alpha = 0.1f) else Color.Transparent)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${i + 1}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(32.dp))
                Text(
                    (c.lane?.emoji?.let { "$it  " } ?: "⭐  ") + c.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (c.mine) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f),
                )
                Text(Format.money(c.value), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            last = i
        }
    }
}

/** The last four quarters, like an annual report. */
@Composable
private fun QuarterlySection(state: GameState) {
    val colors = gameColors
    val quarters = (0 until 4).map { q -> Triple(q, state.trailingRevenue(weeks = 13, offset = q * 13), state.trailingProfitAt(q)) }
        .filter { it.second > 0 || it.third != 0.0 }
    if (quarters.isEmpty()) return
    GroupedSection("Quarterly results", footer = "Most recent first. Each quarter is 13 weeks.") {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("", modifier = Modifier.weight(1f))
            Text("Revenue", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(96.dp))
            Text("Profit", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(96.dp))
        }
        quarters.forEach { (q, revenue, profit) ->
            RowDivider()
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (q == 0) "Last 13 weeks" else "${q * 13}–${q * 13 + 13} weeks ago", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(Format.money(revenue), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(96.dp))
                Text(Format.signedMoney(profit), style = MaterialTheme.typography.bodyMedium, color = if (profit >= 0) colors.good else colors.bad, modifier = Modifier.width(96.dp))
            }
        }
    }
}

private fun GameState.trailingProfitAt(quarter: Int): Double {
    val w = ledger
    val end = w.size - quarter * 13
    if (end <= 0) return 0.0
    return w.subList(maxOf(0, end - 13), end).sumOf { it.profit }
}
