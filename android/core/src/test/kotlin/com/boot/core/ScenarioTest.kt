package com.boot.core

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScenarioTest {
    @AfterTest
    fun resetClock() {
        GameClock.startYear = null
    }

    @Test
    fun everyScenarioStartsAsDescribed() {
        for (sc in Scenario.entries) {
            val s = GameState.scenario(sc, "Co", "Ada", seed = 4)
            assertEquals(sc.lane, s.startingLane, sc.name)
            assertEquals(sc.year, s.startYear, sc.name)
            assertEquals(sc.years * 52, s.scenario!!.deadlineWeek)
            assertEquals(sc.difficulty, s.difficulty)
            sc.cash?.let { assertEquals(it, s.cash, 1e-6) }
            assertEquals(sc.staff, s.staff.size)
            sc.era?.let { assertEquals(it, s.world.era) }
            assertTrue(s.laneOpen(sc.lane), "${sc.name}: ${sc.lane} must exist in ${s.calendarYear}")
        }
    }

    @Test
    fun winningEarlyIsGold() {
        val s = GameState.scenario(Scenario.GIANT_SLAYER, "Co", "Ada", seed = 4)
        s.stats.rivalsBeaten = 1
        s.advance()
        assertEquals(Medal.GOLD, s.scenario!!.medal)
        val e = s.events.single { it.kind is ScenarioOver }
        assertTrue(e.title.startsWith("Gold medal"))
        while (s.events.first().kind !is ScenarioOver) s.choose(s.events[0].choices.lastIndex)
        s.choose(0) // keep playing
        assertNull(s.ending)
        assertEquals(Medal.GOLD, GameState.decode(s.encode()).scenario!!.medal)
    }

    @Test
    fun runningOutOfTimeFails() {
        val s = GameState.scenario(Scenario.FRONTIER_LAB, "Co", "Ada", seed = 4)
        s.week = s.scenario!!.deadlineWeek - 1
        s.advance()
        assertTrue(s.scenario!!.failed)
        while (s.events.first().kind !is ScenarioOver) s.choose(s.events[0].choices.lastIndex)
        s.choose(1) // call it a day
        assertNotNull(s.ending)
        assertNull(s.runRecord()!!.medal)
        assertEquals(Scenario.FRONTIER_LAB, s.runRecord()!!.scenario)
    }

    @Test
    fun bestMedalsPerScenario() {
        val base = RunRecord("A", "B", Lane.GAMES, EndingKind.INDIE, "t", 10, 1.0, 1.0, false, Difficulty.NORMAL, 0)
        val hall = listOf(
            base.copy(scenario = Scenario.GIANT_SLAYER, medal = Medal.BRONZE),
            base.copy(scenario = Scenario.GIANT_SLAYER, medal = Medal.GOLD),
            base.copy(scenario = Scenario.FRONTIER_LAB),
            base,
        )
        assertEquals(mapOf(Scenario.GIANT_SLAYER to Medal.GOLD), hall.bestMedals())
    }

    @Test
    fun sandboxHasMoneyAndNoBankruptcy() {
        val s = GameState.create("Sand", "Ada", Lane.AI, seed = 4, sandbox = true)
        assertTrue(s.sandbox)
        assertEquals(GameState.SANDBOX_CASH, s.cash, 1e-6)
        assertNull(s.venture.board) // no seed round needed
        s.cash = -1_000_000.0
        repeat(20) { if (!s.needsDecision) s.advance() else s.choose(s.events[0].choices.lastIndex) }
        assertNull(s.ending)
        assertTrue(s.achievements.isEmpty())
    }

    @Test
    fun theAutopilotCanWinScenarios() {
        var medals = 0
        val lines = mutableListOf<String>()
        for (sc in Scenario.entries) {
            for (seed in 1L..3L) {
                val s = GameState.scenario(sc, "Bot", "Bot", seed = seed)
                val pilot = Autopilot(staysPublic = true)
                while (!s.isOver && s.scenario?.decided != true && s.week <= sc.years * 52 + 2) {
                    if (s.pendingPivot != null) s.stayFocused()
                    pilot.playWeek(s)
                }
                val run = s.scenario!!
                if (run.medal != null) medals += 1
                lines += "${sc.name} seed $seed: ${run.medal ?: if (run.failed) "failed" else "ended ${s.ending?.kind}"} at ${s.dateLabel} (${Format.percent(s.scenarioProgress())})"
            }
        }
        println(lines.joinToString("\n"))
        assertTrue(medals > 0)
    }
}
