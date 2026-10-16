package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TutorialTest {
    @Test
    fun noTutorialUnlessAsked() {
        val s = GameState.create("Co", "Ada", Lane.GAMES, seed = 1)
        assertNull(s.tutorial)
        assertNull(s.lesson())
    }

    @Test
    fun everyBusinessGetsThroughTheLessons() {
        for (lane in Lane.entries) {
            val s = GameState.create("Co", "Ada", lane, seed = 2, tutorial = true)
            assertEquals(TutorialStep.WELCOME, s.lesson()!!.step)
            val seen = mutableListOf<TutorialStep>()
            val pilot = Autopilot()
            while (s.week < 260 && !s.isOver) {
                val l = s.lesson()!!
                if (seen.lastOrNull() != l.step) seen += l.step
                if (l.step == TutorialStep.DONE) break
                if (l.acknowledge) s.completeLesson(l.step) else {
                    if (s.pendingPivot != null) s.stayFocused()
                    // Nudge the hire lesson: early Apps/Games/OS often lack the autopilot's profit gate.
                    if (l.step == TutorialStep.HIRE && s.staff.isEmpty() && s.candidates.isNotEmpty()) {
                        s.cash = maxOf(s.cash, 80_000.0)
                        s.hire(s.candidates.first().id)
                    }
                    if (l.step == TutorialStep.FIRST_WORK && lane == Lane.OS && s.os?.project == null) {
                        s.cash = maxOf(s.cash, 500_000.0)
                        s.startOsVersion(OsTarget.PC)
                    }
                    if (l.step == TutorialStep.RESEARCH && s.researched.isEmpty() && s.researchPoints >= 8) {
                        s.research(Research.entries.first { s.researchBlocker(it) == null })
                    }
                    pilot.playWeek(s)
                }
            }
            assertEquals(TutorialStep.DONE, s.lesson()?.step, "$lane got stuck at ${s.lesson()?.step}, week ${s.week}")
            assertEquals(TutorialStep.WELCOME, seen.first())
            assertTrue(TutorialStep.FIRST_WORK in seen, "$lane: $seen")
            s.completeLesson(TutorialStep.DONE)
            assertNull(s.tutorial)
        }
    }

    @Test
    fun doingThingsEarlyTicksThemOff() {
        val s = GameState.create("Co", "Ada", Lane.HARDWARE, seed = 3, tutorial = true)
        s.hire(s.candidates.first().id)
        s.completeLesson(TutorialStep.WELCOME)
        s.startDevice(DeviceCategory.entries.first(), DeviceTier.BUDGET)
        assertEquals(TutorialStep.PLAY, s.lesson()!!.step)
        s.advance()
        // Hiring was done already, so the money comes next.
        assertEquals(TutorialStep.MONEY, s.lesson()!!.step)
        assertTrue(TutorialStep.HIRE in s.tutorial!!.done)
    }

    @Test
    fun theResearchLessonShowsHowClose() {
        val s = GameState.create("Co", "Ada", Lane.GAMES, seed = 4, tutorial = true)
        s.tutorial!!.done = setOf(TutorialStep.WELCOME, TutorialStep.FIRST_WORK, TutorialStep.PLAY, TutorialStep.HIRE, TutorialStep.MONEY)
        // Soft start grants 8 RP (enough for the cheapest unlock); drain to exercise the progress meter.
        s.researchPoints = 3.0
        val l = s.lesson()!!
        assertEquals(TutorialStep.RESEARCH, l.step)
        assertTrue(l.progress!! < 1.0)
        assertTrue("costs" in l.body)
    }

    @Test
    fun skipping() {
        val s = GameState.create("Co", "Ada", Lane.AI, seed = 5, tutorial = true)
        s.skipTutorial()
        assertNull(s.lesson())
        val back = GameState.decode(GameState.create("Co", "Ada", Lane.AI, seed = 5, tutorial = true).encode())
        assertEquals(TutorialStep.WELCOME, back.lesson()!!.step)
    }
}
