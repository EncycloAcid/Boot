package com.boot.game.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.core.Lesson
import com.boot.core.MENTOR
import com.boot.core.TutorialStep
import com.boot.core.completeLesson
import com.boot.core.lesson
import com.boot.core.skipTutorial
import com.boot.game.GameStore
import com.boot.game.Tab

/** Rosa always looks the same. */
private const val MENTOR_LOOK = 4_242

/** The mentor's portrait, in a teal jacket. */
@Composable
fun MentorPortrait(size: androidx.compose.ui.unit.Dp = 44.dp) {
    Avatar(lookFor(MENTOR_LOOK), Color(0xFF2A9D8F), size)
}

/** The tutorial's current lesson: Rosa, what to do and why, and a button that takes you there. */
@Composable
fun LessonCard(store: GameStore, state: GameState) {
    val lesson = state.lesson() ?: return
    val colors = gameColors
    GroupedSection("Getting started") {
        CardBlock(spacing = 12.dp) {
            Row(verticalAlignment = Alignment.Top) {
                MentorPortrait(44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Hint("$MENTOR · lesson ${lesson.number} of ${lesson.of}")
                    Text(lesson.title, style = MaterialTheme.typography.titleMedium)
                    Text(lesson.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            lesson.progress?.let { Meter("Research points", it, colors.tech) }
            StepDots(lesson)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (lesson.step != TutorialStep.DONE) PlainButton("Skip tutorial", { store.perform { skipTutorial() } })
                Spacer(Modifier.weight(1f))
                TonalButton(lesson.action, { follow(store, state, lesson) })
            }
        }
    }
}

/** How far through the lessons you are: one dot per lesson, the current one drawn long. */
@Composable
private fun StepDots(lesson: Lesson) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..lesson.of) {
            val now = i == lesson.number
            Box(
                Modifier
                    .height(6.dp)
                    .width(if (now) 18.dp else 6.dp)
                    .background(
                        if (i <= lesson.number) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        if (now) RoundedCornerShape(3.dp) else CircleShape,
                    ),
            )
        }
    }
}

/** Do what the lesson's button says: go to the right place, and tick off lessons that only need reading. */
private fun follow(store: GameStore, state: GameState, lesson: Lesson) {
    if (lesson.acknowledge) store.perform { completeLesson(lesson.step) }
    when (lesson.step) {
        TutorialStep.WELCOME, TutorialStep.DONE -> Unit
        TutorialStep.FIRST_WORK ->
            if (state.startingLane == Lane.GAMES && state.games != null) store.designGame() else store.openLane(state.startingLane)
        TutorialStep.PLAY -> if (store.canAdvance) store.advance(1)
        TutorialStep.HIRE -> store.tab = Tab.TEAM
        TutorialStep.MONEY -> {
            store.companyPage = 1
            store.tab = Tab.COMPANY
        }
        TutorialStep.RESEARCH -> store.tab = Tab.RESEARCH
    }
}
