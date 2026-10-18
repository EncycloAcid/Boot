package com.boot.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.boot.core.Employee
import com.boot.core.Format
import com.boot.core.GameState
import com.boot.core.Lane

/*
 * Tap someone in the office and their card pops up beside them: who they are, what they're good
 * at, what they're working on and how they're doing, with a way into the Team tab to manage them.
 */

/** Width of the card that pops up beside a person in the office. */
internal val PersonCardWidth = 300.dp

/** The card for the person at [key]: -1 for the founder, otherwise an employee's id. */
@Composable
internal fun PersonCard(state: GameState, key: Int, onManage: (() -> Unit)?, onClose: () -> Unit) {
    val colors = gameColors
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .width(PersonCardWidth)
            .shadow(12.dp, shape, clip = false)
            .clip(shape)
            .background(colors.card)
            .border(1.dp, colors.separator, shape)
            .padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val e = state.staff.firstOrNull { it.id == key }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (e != null) StaffPortrait(e, 48.dp) else FounderPortrait(state, 48.dp)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    e?.name ?: state.founder.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (e != null) "Level ${e.level} ${e.role.title.lowercase()}" else "Founder · that's you",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
            }
        }
        Column(Modifier.padding(end = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (e != null) EmployeeDetails(state, e) else FounderDetails(state)
            if (onManage != null) {
                TonalButton(if (e != null) "Manage in Team" else "Level and skills", onManage, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun EmployeeDetails(state: GameState, e: Employee) {
    val colors = gameColors
    e.trait?.let { t ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(t.title, colors.milestone)
            Text(t.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Text(doing(state, e), style = MaterialTheme.typography.bodyMedium)
    storyLine(state, e.id)?.let { (kind, line) ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(kind.title, MaterialTheme.colorScheme.primary)
            Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Figure("Design", "${e.design}", colors.design, Modifier.weight(1f))
        Figure("Tech", "${e.tech}", colors.tech, Modifier.weight(1f))
        Figure("Research", "${e.research}", colors.milestone, Modifier.weight(1f))
    }
    Bar("Morale", e.morale, if (e.morale < 0.35) colors.bad else colors.good, Format.percent(e.morale))
    Bar(if (e.maxLevel) "Max level" else "Experience", if (e.maxLevel) 1.0 else e.xp / e.xpToNext, colors.milestone, if (e.maxLevel) "" else "to level ${e.level + 1}")
    Text(
        "Skill ${e.skill} · ${Format.money(e.salary)}/wk · " +
            (if (state.week - e.hiredWeek < 1) "joined this week" else "joined ${Format.weeks(state.week - e.hiredWeek)} ago"),
        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun FounderDetails(state: GameState) {
    val colors = gameColors
    listOfNotNull(state.founder.trait, state.founder.trait2).forEach { t ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(t.title, colors.milestone)
            Text(t.detail.substringAfter(": ").replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Text(
        if (state.founder.freelancing) "Freelancing on the side to pay the rent." else "Running ${state.companyName}.",
        style = MaterialTheme.typography.bodyMedium,
    )
    FounderLevel(state)
    for (lane in state.lanes) {
        val share = state.founder.attentionShare(lane, state.lanes)
        Bar(lane.businessName, share, colors.lane(lane), Format.percent(share) + " of you")
    }
    Text(
        "${if (state.staff.isEmpty()) "Just you" else "You and ${state.staff.size} " + (if (state.staff.size == 1) "other" else "others")} · ${state.office.title}",
        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** What someone is busy with this week, in a line. */
private fun doing(state: GameState, e: Employee): String {
    if (e.leaveUntil > state.week) return "On leave for ${Format.weeks(e.leaveUntil - state.week)}."
    val lane = e.lane ?: return "Waiting to be put on a lane."
    val g = state.games
    if (lane == Lane.GAMES && g != null) {
        val p = g.project ?: g.project2
        if (p != null) return "Working on ${p.title}, ${Format.percent(p.completion)} done."
        g.contract?.let { return "Working on a contract: ${it.title}." }
        return "The studio is free; waiting for the next game."
    }
    return "Working in the ${lane.businessName.lowercase()}."
}

@Composable
private fun Figure(label: String, value: String, tint: Color, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = tint)
    }
}

@Composable
private fun Bar(label: String, value: Double, tint: Color, note: String) {
    val colors = gameColors
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row {
            Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            Text(note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(colors.fill)) {
            Box(Modifier.fillMaxWidth(value.toFloat().coerceIn(0f, 1f)).height(5.dp).clip(CircleShape).background(tint))
        }
    }
}
