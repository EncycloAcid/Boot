package com.boot.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.core.Research
import com.boot.core.researchBlocker
import com.boot.game.GameStore
import com.boot.game.StudioPage
import com.boot.game.Tab

/** A suggestion for what to do now: what it is, why, and a button that takes you there. */
data class NextStep(val key: String, val title: String, val detail: String, val action: String, val go: (GameStore) -> Unit)

/**
 * The one thing most worth doing right now, or null when all's well. Money trouble comes first,
 * then the people, the office and research. The studio's own cards already say when a game can
 * be started or shipped, so this doesn't repeat them.
 */
fun nextStep(state: GameState): NextStep? {
    val runway = state.runwayWeeks
    if (state.cash < 0) {
        return NextStep(
            "overdrawn", "You're overdrawn",
            "Every week in the red hurts. A bank loan buys time; contracts and cutting staff help too.",
            "See money", { it.companyPage = 1; it.tab = Tab.COMPANY },
        )
    }
    if (runway.isFinite() && runway < 8) {
        return NextStep(
            "runway", "Money runs out in ${runway.toInt()} weeks",
            "At this rate you'll be overdrawn soon. Look at what's costing you, or line up a loan or an investor.",
            "See money", { it.companyPage = 1; it.tab = Tab.COMPANY },
        )
    }
    if (state.staff.isEmpty() && state.candidates.isNotEmpty() && state.cash > 20_000) {
        return NextStep(
            "first-hire", "Hire your first person",
            "On your own you can only do so much. Someone good at your business doubles what gets done.",
            "Meet candidates", { it.tab = Tab.TEAM },
        )
    }
    if (state.staff.size > state.office.desks) {
        return NextStep(
            "crowded", "There aren't enough desks",
            "${state.staff.size} people, ${state.office.desks} desks. Morale drops every week until you move somewhere bigger.",
            "See offices", { it.tab = Tab.TEAM },
        )
    }
    val affordable = Research.entries.filter { r ->
        (r.lane == null || r.lane in state.lanes) && (r.kind != Research.Kind.STUDIO || Lane.GAMES in state.lanes) &&
            (r.kind != Research.Kind.ENGINE || Lane.GAMES in state.lanes) && state.researchBlocker(r) == null
    }.minByOrNull { it.rp }
    if (affordable != null && state.researchPoints >= affordable.rp * 2) {
        return NextStep(
            "research-${affordable.name}", "${state.researchPoints.toInt()} research points to spend",
            "${affordable.title} costs ${affordable.rp} RP: ${affordable.detail.replaceFirstChar { it.lowercase() }}",
            "Research", { it.tab = Tab.RESEARCH },
        )
    }
    val g = state.games
    if (g != null && g.pitches.isNotEmpty() && g.signed.isEmpty()) {
        return NextStep(
            "pitches", "${g.pitches.size} pitch${if (g.pitches.size == 1) "" else "es"} for your label",
            "Developers want you to publish their games. Pick ones in combos you know work.",
            "See pitches", { it.openStudio(StudioPage.MARKET) },
        )
    }
    return null
}

/** The next step, as a card on the office screen. */
@Composable
fun NextStepCard(store: GameStore, state: GameState) {
    val step = nextStep(state) ?: return
    if (store.dismissedStep == step.key) return
    val colors = gameColors
    GroupedSection("Next step") {
        CardBlock {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.size(36.dp).background(colors.warn.copy(alpha = 0.16f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Lightbulb, null, tint = colors.warn, modifier = Modifier.size(20.dp)) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(step.title, style = MaterialTheme.typography.titleMedium)
                    Hint(step.detail)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlainButton("Not now", { store.dismissedStep = step.key })
                Spacer(Modifier.weight(1f))
                TonalButton(step.action, { step.go(store) })
            }
        }
    }
}
