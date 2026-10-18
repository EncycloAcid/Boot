package com.boot.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.boot.core.assignCrew
import com.boot.core.areaOf
import com.boot.core.crewBoost
import com.boot.core.worksOn
import com.boot.core.Chemistry
import com.boot.core.Employee
import com.boot.core.FocusArea
import com.boot.core.Format
import com.boot.core.GameProject
import com.boot.core.GameState
import com.boot.core.Specialty
import com.boot.core.chemistryFactor
import com.boot.core.keyAreas
import com.boot.core.specialise
import com.boot.core.specialiseBlocker
import com.boot.core.specialiseCost
import com.boot.core.specialistBoost
import com.boot.core.specialists
import com.boot.core.specialistsNeeded
import com.boot.core.teamPairs
import com.boot.game.GameStore
import java.util.Locale

/*
 * Who's making the game: which areas have specialists on them, and how the team gets on.
 */

/** A specialist's craft, as a small coloured tag. */
@Composable
fun SpecialtyPill(s: Specialty) = Pill(s.person, MaterialTheme.colorScheme.primary)

/** Specialists on each stage's areas, and the pairs on the team who work well or badly together. */
@Composable
fun TeamFitSection(store: GameStore, state: GameState, p: GameProject, team: Int = 1) {
    val colors = gameColors
    val specialists = state.specialists()
    val need = specialistsNeeded(p.concept.size)
    val key = keyAreas(p.concept).toSet()
    val pairs = state.teamPairs()
    val known = pairs.filter { it.known }
    val chemistry = state.chemistryFactor(pairs)
    GroupedSection(
        "Who's making it",
        Modifier.testTag("team-fit"),
        footer = "Specialists make their areas better: writers for story and dialogues, game designers for gameplay and levels, engine programmers for the engine and AI, " +
            "artists for the world and graphics, composers for sound. A ${p.concept.size.lower} game wants $need on each. " +
            "Some pairs work well together and some clash: move someone to another lane to split them up.",
    ) {
        FocusArea.STAGES.forEachIndexed { stage, title ->
            if (stage > 0) RowDivider()
            val areas = FocusArea.forStage(stage)
            val boost = state.specialistBoost(p.concept, stage, p.focus[stage], specialists, p)
            ListRow(
                title.substringAfter("· "),
                subtitle = areas.joinToString("\n") { area ->
                    val who = specialists.filter { it.specialty?.areas?.contains(area) == true && worksOn(p, it, area) }
                    val names = if (who.isEmpty()) "no specialist" else who.joinToString { it.name.substringBefore(' ') } + if (who.size < need) " (needs $need)" else ""
                    (if (area in key) "★ " else "") + "${area.title}: $names"
                },
                value = String.format(Locale.US, "+%.0f%%", (boost - 1) * 100),
                valueColor = if (boost > 1.001) colors.good else MaterialTheme.colorScheme.onSurfaceVariant,
                valueDetail = if (stage == p.stage && !p.isReady) "this stage" else null,
            )
        }
        RowDivider()
        ListRow(
            "Team chemistry",
            subtitle = if (known.isEmpty()) {
                "Nobody's sure yet who gets on with whom. You'll find out as they work together."
            } else {
                known.joinToString("\n") { "${it.a.name.substringBefore(' ')} + ${it.b.name.substringBefore(' ')}: ${it.chemistry.title.lowercase()}" } +
                    (if (pairs.size > known.size) "\nOthers are still finding out." else "")
            },
            value = String.format(Locale.US, "%+.0f%%", (chemistry - 1) * 100),
            valueColor = when {
                chemistry > 1.001 -> colors.good
                chemistry < 0.999 -> colors.bad
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        if (key.isNotEmpty()) {
            CardBlock { Hint("★ What ${p.concept.genreLower} games need most. Specialists there count in the reviews too.") }
        }
    }
    if (!p.isReady) CrewSection(store, state, p, team)
}

/** Who works on which area of the current stage. */
@Composable
fun CrewSection(store: GameStore, state: GameState, p: GameProject, team: Int = 1) {
    val stage = p.stage
    val areas = FocusArea.forStage(stage)
    val people = state.staff(com.boot.core.Lane.GAMES).filter { it.leaveUntil < state.week }
    if (people.isEmpty()) return
    val boost = state.crewBoost(p, stage)
    GroupedSection(
        "Who works on what · ${FocusArea.STAGES[stage].substringAfter("· ")}",
        Modifier.testTag("crew"),
        footer = "Put people on the areas that suit them: design for the design-heavy areas, tech for the rest, specialists on their own. " +
            "Anyone left to \"where needed\" goes where the effort is. Leave an area short, or crowd one that gets little effort, and people are wasted.",
    ) {
        ValueRow(
            "Effect on this stage",
            String.format(Locale.US, "%+.0f%%", (boost - 1) * 100),
            when {
                boost > 1.001 -> gameColors.good
                boost < 0.999 -> gameColors.bad
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        people.forEach { e ->
            RowDivider()
            val area = p.areaOf(stage, e)
            ListRow(
                e.name,
                subtitle = "Design ${e.design} · Tech ${e.tech}" + (e.specialty?.let { " · ${it.person.lowercase()}" } ?: ""),
                trailing = {
                    MenuButton(area?.title ?: "Where needed", listOf<FocusArea?>(null) + areas, { it?.title ?: "Where needed" }) { pick ->
                        store.perform { assignCrew(team, stage, e.id, pick) }
                    }
                },
            )
        }
    }
}

/** Picks a specialty for [e]'s course. */
@Composable
fun SpecialiseDialog(store: GameStore, state: GameState, e: Employee, onDismiss: () -> Unit) {
    val (rp, cost) = state.specialiseCost(e)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Make ${e.name.substringBefore(' ')} a specialist?") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "A course costs $rp RP and ${Format.money(cost)}, and they're away for three weeks." +
                        (e.specialty?.let { " They're a ${it.person.lowercase()} now." } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                )
                for (s in Specialty.entries) {
                    val blocker = state.specialiseBlocker(e, s)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { Hint(blocker ?: s.areas.joinToString(" and ") { it.lower }) }
                        PlainButton(s.person, {
                            onDismiss()
                            store.perform { specialise(e.id, s) }
                        }, enabled = blocker == null)
                    }
                }
            }
        },
        confirmButton = { PlainButton("Cancel", onDismiss) },
        shape = RoundedCornerShape(28.dp),
    )
}

/** How someone gets on with the people they've worked out they click (or clash) with. */
fun GameState.chemistryNote(e: Employee): String? {
    val mine = teamPairs().filter { it.known && (it.a.id == e.id || it.b.id == e.id) }
    if (mine.isEmpty()) return null
    return mine.joinToString(" · ") { pair ->
        val other = if (pair.a.id == e.id) pair.b else pair.a
        (if (pair.chemistry == Chemistry.GOOD) "Clicks with " else "Clashes with ") + other.name.substringBefore(' ')
    }
}

/** An area's name mid-sentence: lower case, except for AI. */
val FocusArea.lower: String get() = if (this == FocusArea.AI) "AI" else title.lowercase()
