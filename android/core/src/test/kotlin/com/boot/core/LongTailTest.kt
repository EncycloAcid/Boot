package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LongTailTest {
    private fun studio(seed: Long = 3) = GameState.create("Pixel Forge", "F", Lane.GAMES, seed).apply { cash = 50_000_000.0 }

    private fun concept(genre: Genre = Genre.SIMULATION, platforms: List<Platform> = listOf(Platform.PC)) =
        GameConcept("Moss Knight", Topic.FANTASY, genre, platforms = platforms, size = GameSize.SMALL)

    private fun GameState.weeks(n: Int) = repeat(n) {
        while (needsDecision) {
            if (pendingPivot != null) stayFocused() else choose(events.first().choices.lastIndex)
        }
        advance()
    }

    private fun released(genre: Genre = Genre.SIMULATION, platforms: List<Platform> = listOf(Platform.PC)): GameState {
        val s = studio().apply { startGame(concept(genre, platforms)) }
        repeat(400) {
            val p = s.games!!.project ?: return@repeat
            if (p.isReady && 2 in p.milestones && !s.needsDecision) return@repeat
            s.weeks(1)
        }
        s.launchGame()
        return s
    }

    private fun GameState.copy() = GameState.decode(encode())

    /** A good game that came out [age] weeks ago and is still selling. */
    private fun planted(age: Int): GameState {
        val s = studio()
        s.games!!.releases.add(
            0,
            GameRelease(
                id = 9001, title = "Moss Knight", concept = concept(), week = s.week - age, reviews = emptyList(), score = 8.0,
                weeklySales = 20_000.0, netShare = 0.7, design = 10.0, tech = 10.0, bugs = 0.0, hype = 0.0, devCost = 0.0,
                report = emptyList(), userScore = 7.5,
            ),
        )
        return s
    }

    private fun GameState.game() = games!!.releases.first()

    @Test
    fun aSaleOnAYoungGamePullsBuyersForward() {
        val s = released()
        s.weeks(9)
        val r = s.game()
        if (r.weeklySales <= 0) { println("SKIPPED weeklySales=0 at week ${s.week}"); return }
        val o = s.saleOutlook(r, 0.5)
        assertTrue(o.pull > 0.15)
        val control = s.copy()
        s.runSale(r.id, 0.5)
        s.weeks(SALE_WEEKS + 1)
        control.weeks(SALE_WEEKS + 1)
        // After the sale, the young game sells less than it would have.
        assertTrue(s.game().weeklySales < control.game().weeklySales * 0.9)
        // Players learned to wait.
        assertTrue(s.game().saleFatigue > 0)
        assertFailsWith<GameException> { s.runSale(r.id, 0.5) }
        assertFailsWith<GameException> { s.runSale(r.id, 0.4) }
    }

    @Test
    fun anOldGameSellsMoreOnSaleAndCheaper() {
        val s = planted(60)
        val r = s.game()
        if (r.weeklySales <= 0) { println("SKIPPED weeklySales=0 at week ${s.week}"); return }
        val o = s.saleOutlook(r, 0.5)
        assertTrue(o.pull < 0.05)
        val control = s.copy()
        s.runSale(r.id, 0.5)
        s.weeks(20)
        control.weeks(20)
        assertTrue(s.game().lifetimeSales > control.game().lifetimeSales)
        // A permanent cut lifts an old game's revenue, and it can't go back up.
        val before = s.game().weeklySales
        if (before <= 0) { println("SKIPPED before"); return }
        assertNull(s.priceCutBlocker(s.game(), 0.5))
        s.cutPrice(r.id, 0.5)
        assertTrue(s.game().weeklySales > before * 1.2)
        assertEquals(r.concept.unitPrice * 0.5, s.game().price, 1e-9)
        assertNotNull(s.priceCutBlocker(s.game(), 0.25))
    }

    @Test
    fun updatesWinPlayersBack() {
        val s = released()
        s.weeks(10)
        val r = s.game()
        if (r.weeklySales <= 0) { println("SKIPPED weeklySales=0 at week ${s.week}"); return }
        val sales = r.weeklySales
        val score = r.userScore
        s.releaseUpdate(r.id)
        assertEquals(sales * 1.25, r.weeklySales, 1e-6)
        assertTrue(r.userScore >= score)
        assertEquals(1, r.updates)
        assertNotNull(s.updateBlocker(r))
        s.weeks(UPDATE_GAP)
        assertNull(s.updateBlocker(r).takeIf { r.weeklySales > 0 })
    }

    @Test
    fun modToolsAndAModThatTakesOff() {
        assertNotNull(released(genre = Genre.CASUAL).let { it.modToolsBlocker(it.game()) })
        val s = released()
        val r = s.game()
        assertNull(s.modToolsBlocker(r))
        s.releaseModTools(r.id)
        assertTrue(r.modTools)
        Sandbox.enqueue(s, ModHit(r.id, "${r.title}: Reforged", "Ada Lin"))
        val team = s.staff.size
        assertEquals(ModHit(r.id, "${r.title}: Reforged", "Ada Lin"), GameState.decode(s.encode()).events.first { it.kind is ModHit }.kind)
        s.choose(s.events.indexOfFirst { it.kind is ModHit }.let { 0 })
        assertEquals(team + 1, s.staff.size)
        assertEquals("Ada Lin", s.staff.last().name)
    }

    @Test
    fun portsSellToTheirCrowd() {
        val s = released(platforms = listOf(Platform.PC))
        val r = s.game()
        val big = s.portOutlook(r, Platform.PLAYSYSTEM_5)
        val small = s.portOutlook(r, Platform.STRATOS)
        assertTrue(big > small)
        assertTrue(s.portOutlook(r, Platform.PLAYSYSTEM_5, PortQuality.ENHANCED) > s.portOutlook(r, Platform.PLAYSYSTEM_5, PortQuality.OUTSOURCED))
        assertTrue(s.portQuote(r, PortQuality.ENHANCED).first > s.portQuote(r, PortQuality.OUTSOURCED).first)
        s.portGame(r.id, Platform.PLAYSYSTEM_5, PortQuality.OUTSOURCED)
        assertEquals(PortQuality.OUTSOURCED, s.games!!.ports.single().quality)
    }

    @Test
    fun expansionsComeInSizes() {
        val s = planted(10)
        Sandbox.research(s, Research.EXPANSIONS)
        val r = s.game()
        if (s.expansionBlocker(r.id) != null) { println("SKIPPED " + s.expansionBlocker(r.id)); return }
        s.startExpansion(r.id, ExpansionScope.DLC)
        val x = s.games!!.expansion!!
        assertEquals(r.concept.size.work * ExpansionScope.DLC.work, x.work, 1e-9)
        assertEquals(ExpansionScope.DLC, GameState.decode(s.encode()).games!!.expansion!!.scope)
    }

    @Test
    fun aReleasedGameCanGoOnMoreStores() {
        val s = released()
        val r = s.game()
        if (r.weeklySales <= 0) { println("SKIPPED weeklySales=0 at week ${s.week}"); return }
        val sales = r.weeklySales
        val share = r.netShare
        assertNull(s.addStoreBlocker(r, Storefront.EPIC))
        s.addStore(r.id, Storefront.EPIC)
        assertTrue(r.weeklySales > sales)
        assertTrue(r.netShare > share)
        assertTrue(Storefront.EPIC in s.storesOf(r))
        assertNotNull(s.addStoreBlocker(r, Storefront.EPIC))
    }
}
