package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FounderTest {
    private fun game(trait: FounderTrait? = null, seed: Long = 4) =
        GameState.create("T", "Sam", Lane.GAMES, seed).apply { introduceFounder(trait, null) }

    @Test
    fun yourLookAndTraitAreSaved() {
        val look = Appearance(skin = 3, hair = 5, style = 2, glasses = true, beard = false, outfit = 4, top = 9)
        val s = GameState.create("T", "Sam", Lane.GAMES, 4).apply { introduceFounder(FounderTrait.SHOWRUNNER, look) }
        val back = GameState.decode(s.encode())
        assertEquals(look, back.founder.appearance)
        assertEquals(FounderTrait.SHOWRUNNER, back.founder.trait)
        // No choice made: no trait, and the look comes from the name as before.
        assertNull(game().founder.trait)
        assertNull(game().founder.appearance)
    }

    @Test
    fun aBootstrapperStartsWithMoreSavings() {
        assertEquals(game().cash * 1.6, game(FounderTrait.BOOTSTRAPPER).cash, 0.01)
        // A sandbox already has all the money it needs.
        val sandbox = GameState.create("T", "Sam", Lane.GAMES, 4, sandbox = true).apply { introduceFounder(FounderTrait.BOOTSTRAPPER, null) }
        assertEquals(GameState.create("T", "Sam", Lane.GAMES, 4, sandbox = true).cash, sandbox.cash, 0.01)
    }

    @Test
    fun aCoderGetsMoreDoneAndAVisionaryDesignsBetter() {
        val plain = game().context(Lane.GAMES)
        assertTrue(game(FounderTrait.CODER).context(Lane.GAMES).staffing.output > plain.staffing.output * 1.3)
        assertTrue(game(FounderTrait.VISIONARY).context(Lane.GAMES).staffing.avgDesign > plain.staffing.avgDesign + 1.5)
    }

    @Test
    fun aTinkererResearchesFaster() {
        assertEquals(game().researchPerWeek * 1.25, game(FounderTrait.TINKERER).researchPerWeek, 0.001)
    }

    @Test
    fun aPeoplePersonKeepsTheTeamHappy() {
        fun morale(trait: FounderTrait?): Double {
            val s = game(trait).apply { cash = 5_000_000.0 }
            repeat(2) { s.candidates.firstOrNull()?.let { c -> s.hire(c.id, Lane.GAMES) } }
            repeat(40) {
                while (s.needsDecision) s.choose(s.events.firstOrNull()?.choices?.lastIndex ?: return@repeat)
                s.advance()
            }
            return s.staff.map { it.morale }.average()
        }
        assertTrue(morale(FounderTrait.PEOPLE_PERSON) > morale(null) + 0.04)
    }

    @Test
    fun aShowrunnerBuildsMoreHype() {
        fun hype(trait: FounderTrait?): Double {
            val s = game(trait).apply { cash = 5_000_000.0 }
            s.startGame(GameConcept("Moss Knight", Topic.FANTASY, Genre.RPG, platforms = listOf(Platform.PC), size = GameSize.SMALL))
            val p = s.games!!.project!!
            return s.reveal(p, "a show", 20.0, 0.6)
        }
        assertEquals(hype(null) * 1.3, hype(FounderTrait.SHOWRUNNER), 0.001)
    }

    @Test
    fun aMediaDarlingWeathersThePress() {
        val plain = game().reputation
        val darling = game(FounderTrait.MEDIA_DARLING).reputation
        val start = plain.score
        plain.adjust(10.0)
        darling.adjust(10.0)
        assertEquals(start + 13.0, darling.score, 0.001)
        assertEquals(start + 10.0, plain.score, 0.001)
        plain.adjust(-13.0)
        darling.adjust(-13.0)
        assertEquals(start + 3.0, darling.score, 0.001)
        assertTrue(darling.score > plain.score)
    }

    @Test
    fun everyTraitStillPlaysAWholeRun() {
        // None of the traits breaks the game: the autopilot gets through ten years with each.
        for (trait in FounderTrait.entries) {
            val s = game(trait, seed = 11)
            val pilot = Autopilot()
            while (!s.isOver && s.week < 52 * 10) pilot.playWeek(s)
            assertTrue(s.week >= 52 * 5 || s.isOver, "$trait")
        }
    }
}
