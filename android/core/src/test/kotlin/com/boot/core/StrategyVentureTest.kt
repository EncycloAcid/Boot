package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StrategyRegionalTest {
    private fun richGames(seed: Long = 1): GameState {
        val s = GameState.create("Strat Co", "Ada", Lane.GAMES, seed)
        s.cash = 5_000_000.0
        s.researchPoints = 10_000.0
        s.moveOffice() // apartment → coworking
        return s
    }

    @Test
    fun openAndLocalizeOfficeGrowsTerritory() {
        val s = richGames()
        assertNull(s.openOfficeBlocker(Region.NORTH_AMERICA))
        s.openOffice(Region.NORTH_AMERICA)
        assertEquals(1, s.strategy.offices.size)
        assertNotNull(s.openOfficeBlocker(Region.EUROPE)) // needs research for second
        s.research(Research.REGIONAL_EXPANSION)
        assertNull(s.openOfficeBlocker(Region.EUROPE))
        s.openOffice(Region.EUROPE)
        s.localizeOffice(Region.NORTH_AMERICA)
        assertTrue(s.strategy.office(Region.NORTH_AMERICA)!!.localized)
        repeat(4) {
            while (s.needsDecision) s.choose(s.events[0].choices.size - 1)
            s.advance()
        }
        assertTrue(s.strategy.lastTerritory > 0)
    }

    @Test
    fun allianceSignsGrowsAndExpires() {
        val s = richGames(2)
        s.research(Research.REGIONAL_EXPANSION)
        s.research(Research.GLOBAL_ALLIANCES)
        assertNull(s.signAllianceBlocker(AllianceKind.DISTRIBUTION, Lane.GAMES))
        s.signAlliance(AllianceKind.DISTRIBUTION, Lane.GAMES)
        val a = s.strategy.activeAlliances.single()
        assertEquals(104, a.weeksLeft(s.week))
        val before = a.value
        repeat(8) {
            while (s.needsDecision) s.choose(s.events[0].choices.size - 1)
            s.advance()
        }
        assertTrue(s.strategy.alliances.first().value >= before * 0.99)
        // Fast-forward expiry
        a.expiresWeek = s.week
        while (s.needsDecision) s.choose(s.events[0].choices.size - 1)
        s.advance()
        assertTrue(a.expired)
    }

    @Test
    fun encodeDecodePreservesStrategy() {
        val s = richGames(3)
        s.research(Research.REGIONAL_EXPANSION)
        s.research(Research.GLOBAL_ALLIANCES)
        s.openOffice(Region.ASIA_PACIFIC)
        s.signAlliance(AllianceKind.CLOUD, Lane.GAMES)
        val round = GameState.decode(s.encode())
        assertEquals(1, round.strategy.offices.size)
        assertEquals(Region.ASIA_PACIFIC, round.strategy.offices.first().region)
        assertEquals(1, round.strategy.activeAlliances.size)
    }
}

class VentureArmTest {
    private fun armed(seed: Long = 10): GameState {
        val s = GameState.create("VC Co", "Bo", Lane.GAMES, seed)
        s.cash = 10_000_000.0
        s.researchPoints = 10_000.0
        s.moveOffice()
        s.research(Research.REGIONAL_EXPANSION)
        s.research(Research.GLOBAL_ALLIANCES)
        s.research(Research.VENTURE_ARM)
        return s
    }

    @Test
    fun seedFollowOnAndExit() {
        val s = armed()
        assertNull(s.seedVentureBlocker(Lane.GAMES))
        val previewOwn = s.seedOwnershipPreview(0.7)
        s.seedVenture(Lane.GAMES, conviction = 0.7)
        val h = s.strategy.liveHoldings.single()
        assertEquals(VentureStage.SEED, h.stage)
        assertEquals(previewOwn, h.ownership, 1e-9)
        assertTrue(h.ownership in 0.05..0.2)
        assertTrue(h.basis > 0 && h.valuation > h.basis)
        assertTrue(s.followOnWeeksLeft(h) in 1..26)
        assertTrue(s.exitQuote(h) > 0)
        assertTrue(s.marketCycleLabel().isNotBlank())

        assertNotNull(s.followOnBlocker(h.id)) // too early
        s.week = h.seededWeek + 26
        assertEquals(0, s.followOnWeeksLeft(h))
        assertNull(s.followOnBlocker(h.id))
        val beforeOwn = h.ownership
        s.followOnVenture(h.id)
        assertEquals(VentureStage.SERIES_A, h.stage)
        assertTrue(h.ownership > beforeOwn)

        assertNull(s.exitVentureBlocker(h.id))
        val quote = s.exitQuote(h)
        val cashBefore = s.cash
        s.exitVenture(h.id)
        assertTrue(h.exited)
        assertEquals(quote, h.exitProceeds, 1e-6)
        assertTrue(s.cash > cashBefore)
        assertTrue(s.strategy.liveHoldings.isEmpty())
    }

    @Test
    fun marketCycleMovesMarks() {
        val s = armed(11)
        s.seedVenture(Lane.GAMES)
        val h = s.strategy.liveHoldings.single()
        val start = h.valuation
        repeat(20) {
            while (s.needsDecision) s.choose(s.events[0].choices.size - 1)
            s.advance()
        }
        // Valuation should have drifted (almost never identical after 20 noisy weeks).
        assertTrue(h.valuation != start || s.marketCycle() == 1.0)
        assertTrue(h.valuation > 0)
    }

    @Test
    fun cannotSeedWithoutResearch() {
        val s = GameState.create("X", "Y", Lane.GAMES, 12)
        s.cash = 1_000_000.0
        assertFailsWith<GameException> { s.seedVenture(Lane.GAMES) }
    }

    @Test
    fun ventureArmSurvivesSave() {
        val s = armed(13)
        s.seedVenture(Lane.GAMES)
        s.week = s.strategy.liveHoldings.first().seededWeek + 30
        s.followOnVenture(s.strategy.liveHoldings.first().id)
        val round = GameState.decode(s.encode())
        val h = round.strategy.liveHoldings.single()
        assertEquals(VentureStage.SERIES_A, h.stage)
        assertTrue(h.basis > 0)
    }
}
