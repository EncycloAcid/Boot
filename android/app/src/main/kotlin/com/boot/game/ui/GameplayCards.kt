package com.boot.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.boot.core.ChartEntry
import com.boot.core.Format
import com.boot.core.GameClock
import com.boot.core.GameState
import com.boot.core.Goal
import com.boot.core.currentGoals
import com.boot.core.openGoals
import java.util.Locale

/** The next few goals, each with how close you are and what it pays. */
@Composable
fun GoalsCard(state: GameState, onMore: () -> Unit) {
    val goals = state.currentGoals
    val done = state.goalsDone.size
    val total = done + state.openGoals.size
    GroupedSection("Goals · $done of $total", action = { PlainButton("All goals", onMore) }) {
        if (goals.isEmpty()) CardBlock { Hint("Every goal reached. Legendary.") }
        goals.forEachIndexed { i, goal ->
            if (i > 0) RowDivider(52.dp)
            GoalRow(goal, state, status = GoalStatus.ACTIVE)
        }
    }
}

enum class GoalStatus { DONE, ACTIVE, LATER }

@Composable
fun GoalRow(goal: Goal, state: GameState, status: GoalStatus) {
    val colors = gameColors
    val tint = when (status) {
        GoalStatus.DONE -> colors.good
        GoalStatus.ACTIVE -> colors.milestone
        GoalStatus.LATER -> MaterialTheme.colorScheme.outline
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(28.dp).background(tint.copy(alpha = 0.14f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(
                when (status) {
                    GoalStatus.DONE -> Icons.Filled.CheckCircle
                    GoalStatus.ACTIVE -> Icons.Filled.Flag
                    GoalStatus.LATER -> Icons.Filled.Lock
                },
                null, tint = tint, modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    goal.title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f),
                    color = if (status == GoalStatus.LATER) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                goal.lane?.let { Text(it.emoji, style = MaterialTheme.typography.bodyMedium) }
            }
            if (status == GoalStatus.ACTIVE) {
                val progress = goal.progress(state)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(colors.milestone.copy(alpha = 0.16f)),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(progress.toFloat().coerceIn(0.02f, 1f))
                                .height(6.dp)
                                .background(colors.milestone, CircleShape),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                (if (status == GoalStatus.DONE) "Earned: " else "Reward: ") + goal.reward.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Every goal: reached, in progress, and still to come. */
@Composable
fun GoalsList(state: GameState) {
    val open = state.openGoals
    val current = state.currentGoals.toSet()
    GroupedSection("In progress") {
        if (current.isEmpty()) CardBlock { Hint("Nothing left. You did it all.") }
        current.forEachIndexed { i, g ->
            if (i > 0) RowDivider(56.dp)
            GoalRow(g, state, GoalStatus.ACTIVE)
        }
    }
    val later = open.filter { it !in current }
    if (later.isNotEmpty()) {
        Spacer(Modifier.size(20.dp))
        GroupedSection("Still to come") {
            later.forEachIndexed { i, g ->
                if (i > 0) RowDivider(56.dp)
                GoalRow(g, state, GoalStatus.LATER)
            }
        }
    }
    if (state.goalsDone.isNotEmpty()) {
        Spacer(Modifier.size(20.dp))
        GroupedSection("Reached") {
            state.goalsDone.asReversed().forEachIndexed { i, g ->
                if (i > 0) RowDivider(56.dp)
                GoalRow(g, state, GoalStatus.DONE)
            }
        }
    }
}

/** This week's best-sellers, yours highlighted. */
@Composable
fun ChartCard(state: GameState, limit: Int, action: (@Composable () -> Unit)? = null, onOpen: ((Int) -> Unit)? = null) {
    val g = state.games ?: return
    val chart = g.market.chart.take(limit)
    val mine = g.releases.filter { it.chartRank > limit }.minByOrNull { it.chartRank }
    GroupedSection(
        "Sales chart · this week",
        action = action,
        footer = if (chart.isEmpty()) null else mine?.let { "${it.title} is at #${it.chartRank}." },
    ) {
        if (chart.isEmpty()) CardBlock { Hint("Nothing on sale yet. Rival studios ship games all the time; yours will show up here too.") }
        chart.forEachIndexed { i, e ->
            if (i > 0) RowDivider(56.dp)
            ChartRow(i + 1, e, onOpen)
        }
    }
}

@Composable
private fun ChartRow(rank: Int, e: ChartEntry, onOpen: ((Int) -> Unit)?) {
    val colors = gameColors
    val highlight = e.mine
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (highlight) colors.milestone.copy(alpha = 0.08f) else Color.Transparent)
            .then(if (highlight && onOpen != null && e.releaseId != null) Modifier.clickable { onOpen(e.releaseId!!) } else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "$rank",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = if (rank == 1) colors.warn else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(28.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                e.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
                fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal,
            )
            Text(
                (if (highlight) "You" else if (e.owned) "${e.studio} · yours" else e.studio) + " · " + String.format(Locale.US, "%.1f", e.score) + "/10",
                style = MaterialTheme.typography.labelMedium,
                color = if (highlight || e.owned) colors.milestone else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Text(Format.money(e.weeklySales) + "/wk", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Game of the Year, every year so far. */
/** Real releases on the way. The giants pull players away from anything that launches next to them. */
@Composable
fun ComingSoonCard(state: GameState, limit: Int) {
    val g = state.games ?: return
    val soon = g.market.upcoming.take(limit)
    if (soon.isEmpty()) return
    val colors = gameColors
    GroupedSection("Coming soon", footer = "The biggest launches steal players for a few weeks. Don't release yours right next to one.") {
        soon.forEachIndexed { i, a ->
            if (i > 0) RowDivider(56.dp)
            val weeks = a.week - state.week
            ListRow(
                a.title,
                subtitle = "${a.studio} · ${a.size.title} ${a.genre.lower}" + if (a.mega) " · the big one" else "",
                leading = {
                    Icon(
                        if (a.mega) Icons.Filled.Whatshot else Icons.Filled.Event, null,
                        tint = if (a.mega) colors.warn else MaterialTheme.colorScheme.outline, modifier = Modifier.size(24.dp),
                    )
                },
                value = when (weeks) {
                    0 -> "This week"
                    1 -> "Next week"
                    else -> "In $weeks weeks"
                },
                valueDetail = GameClock.label(a.week),
            )
        }
    }
}

@Composable
fun AwardsCard(state: GameState) {
    val g = state.games ?: return
    val awards = g.market.awards
    if (awards.isEmpty()) return
    val colors = gameColors
    GroupedSection("Game of the Year", footer = "Held every December. Ship something great that year to get nominated.") {
        awards.asReversed().forEachIndexed { i, a ->
            if (i > 0) RowDivider(52.dp)
            ListRow(
                a.title,
                subtitle = (state.startYear?.let { "${it + a.year - 1}" } ?: "Year ${a.year}") + " · ${if (a.mine) "You" else a.studio} · ${String.format(Locale.US, "%.1f", a.score)}/10",
                leading = {
                    Icon(Icons.Filled.EmojiEvents, null, tint = if (a.mine) colors.milestone else MaterialTheme.colorScheme.outline, modifier = Modifier.size(24.dp))
                },
            )
        }
    }
}
