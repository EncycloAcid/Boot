package com.boot.desktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.boot.core.Format
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.core.Lesson
import com.boot.core.MENTOR
import com.boot.core.Research
import com.boot.core.TutorialStep
import com.boot.core.completeLesson
import com.boot.core.lesson
import com.boot.core.researchBlocker
import com.boot.core.skipTutorial
import com.boot.desktop.DesktopStore
import com.boot.desktop.Tab

/** Office mentor lesson + next-step cards — makes the tutorial toggle real. */
@Composable
fun OfficeHints(store: DesktopStore, state: GameState) {
    state.lesson()?.let { lesson ->
        GlassPanel {
            Hint("$MENTOR · lesson ${lesson.number} of ${lesson.of}")
            Text(lesson.title, style = MaterialTheme.typography.titleMedium, color = hudColors.text)
            Hint(lesson.body)
            lesson.progress?.let { Hint("Research ${Format.percent(it)}", hudColors.research) }
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (lesson.step != TutorialStep.DONE) {
                    MacButton("Skip tutorial", onClick = { store.perform { skipTutorial() } }, primary = false)
                }
                Spacer(Modifier.weight(1f))
                MacButton(lesson.action, onClick = { followLesson(store, state, lesson) })
            }
        }
    }
    val step = desktopNextStep(store, state) ?: return
    if (store.dismissedStep == step.key) return
    if (state.tutorial != null && step.key !in setOf("overdrawn", "runway")) return
    GlassPanel {
        Text("Next step", style = MaterialTheme.typography.labelLarge, color = hudColors.accent)
        Text(step.title, style = MaterialTheme.typography.titleMedium, color = hudColors.text)
        Hint(step.detail)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MacButton("Not now", onClick = { store.dismissedStep = step.key }, primary = false)
            Spacer(Modifier.weight(1f))
            MacButton(step.action, onClick = step.go)
        }
    }
}

private data class DeskStep(
    val key: String,
    val title: String,
    val detail: String,
    val action: String,
    val go: () -> Unit,
)

private fun desktopNextStep(store: DesktopStore, state: GameState): DeskStep? {
    val runway = state.runwayWeeks
    if (state.cash < 0) {
        return DeskStep(
            "overdrawn", "You're overdrawn",
            "Every week in the red hurts. A bank loan buys time.",
            "See money",
        ) { store.companyPage = 1; store.tab = Tab.COMPANY }
    }
    if (runway.isFinite() && runway < 8) {
        return DeskStep(
            "runway", "Money runs out in ${runway.toInt()} weeks",
            "At this rate you'll be overdrawn soon.",
            "See money",
        ) { store.companyPage = 1; store.tab = Tab.COMPANY }
    }
    if (state.staff.isEmpty() && state.candidates.isNotEmpty() && state.cash > 20_000) {
        return DeskStep(
            "first-hire", "Hire your first person",
            "Someone good at your business doubles what gets done.",
            "Meet candidates",
        ) { store.tab = Tab.TEAM }
    }
    if (state.staff.size > state.office.desks) {
        return DeskStep(
            "crowded", "There aren't enough desks",
            "${state.staff.size} people, ${state.office.desks} desks. Move somewhere bigger.",
            "See offices",
        ) { store.tab = Tab.TEAM }
    }
    val affordable = Research.entries.filter { r ->
        (r.lane == null || r.lane in state.lanes) &&
            (r.kind != Research.Kind.STUDIO || Lane.GAMES in state.lanes) &&
            (r.kind != Research.Kind.ENGINE || Lane.GAMES in state.lanes) &&
            state.researchBlocker(r) == null
    }.minByOrNull { it.rp }
    if (affordable != null && state.researchPoints >= affordable.rp * 2) {
        return DeskStep(
            "research-${affordable.name}",
            "${state.researchPoints.toInt()} research points to spend",
            "${affordable.title} costs ${affordable.rp} RP.",
            "Research",
        ) { store.tab = Tab.RESEARCH }
    }
    return null
}

private fun followLesson(store: DesktopStore, state: GameState, lesson: Lesson) {
    if (lesson.acknowledge) store.perform { completeLesson(lesson.step) }
    when (lesson.step) {
        TutorialStep.WELCOME, TutorialStep.DONE -> Unit
        TutorialStep.FIRST_WORK ->
            if (state.startingLane == Lane.GAMES && state.games != null) store.designGame()
            else store.openLane(state.startingLane)
        TutorialStep.PLAY -> if (store.canAdvance) store.advance(1)
        TutorialStep.HIRE -> store.tab = Tab.TEAM
        TutorialStep.MONEY -> {
            store.companyPage = 1
            store.tab = Tab.COMPANY
        }
        TutorialStep.RESEARCH -> store.tab = Tab.RESEARCH
    }
}
