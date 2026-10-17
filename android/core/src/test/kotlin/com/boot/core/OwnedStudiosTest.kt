package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OwnedStudiosTest {
    private fun studio(seed: Long = 3) = GameState.create("Pixel Forge", "F", Lane.GAMES, seed).apply { cash = 2_000_000_000.0 }

    private fun GameState.owning(name: String) = apply { Sandbox.own(this, name) }

    private fun GameState.sub(name: String) = games!!.subsidiaries.first { it.name == name }

    /** A week at a time, taking the last choice on anything but studio trouble. */
    private fun GameState.weeks(n: Int) = repeat(n) {
        if (pendingPivot != null) stayFocused()
        while (events.isNotEmpty() && events.first().kind !is StudioUnrest) choose(events.first().choices.lastIndex)
        if (!needsDecision) advance()
    }

    @Test
    fun aStudioPitchesTheSameGameUntilItsMade() {
        val s = studio().owning("FromSoftware")
        val sub = s.sub("FromSoftware")
        val pitch = s.studioPitch(sub)
        assertEquals(pitch.title, s.studioPitch(sub).title)
        assertTrue(pitch.size >= GameSize.LARGE)
        // FromSoftware's pitches lean on its own series.
        val seeded = (1L..10L).map { seed -> studio(seed).owning("FromSoftware").let { it.studioPitch(it.sub("FromSoftware")) } }
        assertTrue(seeded.any { it.franchise != null })
        // An indie pitches something smaller.
        val indie = studio().owning("Team Cherry")
        assertTrue(indie.studioPitch(indie.sub("Team Cherry")).size <= GameSize.MEDIUM)
    }

    @Test
    fun greenlitGamesComeOutUnderTheirNameAndPayYou() {
        val s = studio().owning("Team Cherry")
        val sub = s.sub("Team Cherry")
        val pitch = s.studioPitch(sub)
        val cash = s.cash
        s.greenlight("Team Cherry", StudioBudget.GENEROUS)
        assertEquals(pitch.cost * StudioBudget.GENEROUS.cost, cash - s.cash, 1.0)
        val p = assertNotNull(sub.project)
        assertNotNull(s.greenlightBlocker("Team Cherry", StudioBudget.NORMAL))
        assertFailsWith<GameException> { s.greenlight("Team Cherry", StudioBudget.NORMAL) }
        s.weeks(p.doneWeek - s.week + 1)
        assertNull(sub.project)
        assertEquals(1, sub.shipped)
        assertEquals(1, s.stats.studioGames)
        val game = s.games!!.market.games.first { it.title == p.title }
        assertTrue(game.funded)
        // While it sells, most of it comes to you.
        if (game.weeklySales > 0) assertTrue(s.fundedSales(sub) > 0)
        // And there's a new pitch.
        assertTrue(s.studioPitch(sub).title != p.title)
    }

    @Test
    fun aBiggerBudgetAndHappierTeamMakeBetterGames() {
        fun average(budget: StudioBudget, morale: Double): Double = (1L..8L).map { seed ->
            val s = studio(seed).owning("Ubisoft")
            val sub = s.sub("Ubisoft")
            s.greenlight("Ubisoft", budget)
            val p = sub.project!!
            repeat(p.doneWeek - s.week + 1) {
                sub.morale = morale
                s.weeks(1)
            }
            s.games!!.market.games.first { it.title == p.title }.score
        }.average()
        assertTrue(average(StudioBudget.GENEROUS, 0.9) > average(StudioBudget.TIGHT, 0.3) + 0.8)
    }

    @Test
    fun cancellingCostsMorale() {
        val s = studio().owning("Capcom")
        val sub = s.sub("Capcom")
        s.greenlight("Capcom", StudioBudget.NORMAL)
        val morale = sub.morale
        s.cancelStudioGame("Capcom")
        assertNull(sub.project)
        assertTrue(sub.morale < morale - 0.1)
        assertFailsWith<GameException> { s.cancelStudioGame("Capcom") }
    }

    @Test
    fun squeezingAStudioWearsItDownUntilItSpeaksUp() {
        val s = studio().owning("Ubisoft")
        val sub = s.sub("Ubisoft")
        sub.plan = Plan.SQUEEZED
        repeat(120) {
            if (s.events.any { it.kind is StudioUnrest }) return@repeat
            s.weeks(1)
        }
        val e = s.events.first { it.kind is StudioUnrest }
        assertTrue(sub.morale < 0.3)
        val labels = e.choices.map { it.label }
        assertTrue("Let them run themselves" in labels)
        s.choose(labels.indexOf("Let them run themselves"))
        assertEquals(Plan.INDEPENDENT, sub.plan)
        assertTrue(sub.morale > 0.3)
    }

    @Test
    fun lettingThemWalkCostsValue() {
        val s = studio().owning("Ubisoft")
        val sub = s.sub("Ubisoft")
        val st = studio("Ubisoft")!!
        val before = s.divestPrice(st)
        sub.morale = 0.1
        s.weeks(1)
        val e = s.events.first { it.kind is StudioUnrest }
        s.choose(e.choices.indexOfFirst { it.label == "Let them walk" })
        assertEquals(1, sub.talentLost)
        assertTrue(s.divestPrice(st) < before)
    }

    @Test
    fun oldSavesLoadWithCalmStudios() {
        val s = studio().owning("Capcom")
        val back = GameState.decode(s.encode())
        val sub = back.games!!.subsidiaries.single()
        assertEquals(0.7, sub.morale)
        assertNull(sub.project)
    }
}
