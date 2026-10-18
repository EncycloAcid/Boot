package com.boot.core

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StorefrontsTest {
    @AfterTest
    fun resetClock() {
        GameClock.startYear = null
    }

    private fun studio(start: StartPoint = StartPoint.TODAY, seed: Long = 7) =
        GameState.create("Pixel Forge", "Ada", Lane.GAMES, seed = seed, start = start).also { Sandbox.setCash(it, 100_000_000.0) }

    private fun GameState.play(weeks: Int) = repeat(weeks) {
        while (needsDecision) if (pendingPivot != null) stayFocused() else choose(0)
        advance()
    }

    private fun GameState.project(platforms: List<Platform>) {
        val c = GameConcept("Moon Base", Topic.SPACE, Genre.ACTION, platforms = platforms, size = GameSize.SMALL)
        games!!.setProject(1, GameProject(c, focus = List(3) { mutableListOf(1.0, 1.0, 1.0) }, startedWeek = week))
    }

    @Test
    fun theUsualStoresChangeNothing() {
        val s = studio()
        assertEquals(setOf(Storefront.STEAM), s.defaultStores())
        val t = s.storeTerms(s.defaultStores())
        assertEquals(1.0, t.lift, 1e-9)
        assertEquals(0.7, t.keep, 1e-9)
        assertEquals(setOf(Storefront.RETAIL), studio(StartPoint.THE_WEB).defaultStores())
        assertEquals(setOf(Storefront.RETAIL, Storefront.STEAM), studio(StartPoint.SMARTPHONES).defaultStores())
    }

    @Test
    fun leavingSteamCostsBuyersButKeepsMore() {
        val s = studio()
        val epic = s.storeTerms(setOf(Storefront.EPIC))
        assertTrue(epic.lift < 0.5)
        assertEquals(0.88, epic.keep, 1e-9)
        val wide = s.storeTerms(setOf(Storefront.STEAM, Storefront.EPIC, Storefront.GOG, Storefront.ITCH))
        assertTrue(wide.lift > 1.05)
        // Only the PC share of a game's sales depends on its stores.
        s.project(listOf(Platform.PC, Platform.PLAYSYSTEM_5))
        s.setStore(1, Storefront.EPIC, true)
        s.setStore(1, Storefront.STEAM, false)
        val p = s.games!!.project!!
        assertEquals(setOf(Storefront.EPIC), s.storesFor(p))
        assertEquals(0.5 + 0.5 * epic.lift, s.storeLaunchTerms(p).first, 1e-9)
        assertNotNull(s.epicGuarantee(p))
        assertFailsWith<GameException> { s.setStore(1, Storefront.EPIC, false) }
    }

    @Test
    fun gogWantsNoDrm() {
        val s = studio()
        s.project(listOf(Platform.PC))
        val p = s.games!!.project!!
        p.protection = Protection.ANTI_TAMPER
        assertNotNull(s.storeBlocker(p, Storefront.GOG))
        p.protection = Protection.NONE
        assertNull(s.storeBlocker(p, Storefront.GOG))
        s.setStore(1, Storefront.GOG, true)
        p.protection = Protection.ANTI_TAMPER
        assertTrue(Storefront.GOG !in s.storesFor(p))
        // Console-only games don't pick a store.
        s.project(listOf(Platform.PLAYSYSTEM_5))
        assertNotNull(s.storeBlocker(s.games!!.project!!, Storefront.STEAM))
    }

    @Test
    fun yourOwnStoreGrowsAndEarns() {
        val s = studio()
        assertEquals("Research digital storefronts first.", s.openStoreBlocker())
        Sandbox.research(s, Research.DIGITAL_STORES)
        s.openStore()
        val store = s.games!!.store!!
        assertTrue(Storefront.OWN !in s.storesOpen())
        s.play(STORE_BUILD_WEEKS + 1)
        assertTrue(Storefront.OWN in s.storesOpen())
        assertTrue(store.users > 0)
        s.setStoreCut(0.12)
        assertFailsWith<GameException> { s.setStoreCut(0.5) }
        val calm = store.users
        s.play(26)
        val withoutGiveaways = store.users - calm
        s.setGiveaways(true)
        val before = store.users
        s.play(26)
        assertTrue(store.users - before > withoutGiveaways)
        assertTrue(store.earned > 0)
        assertTrue(s.ownStoreReach() > 0)
        assertEquals(store, GameState.decode(s.encode()).games!!.store)
        val cash = s.cash
        val value = s.storeValue
        assertTrue(value > 0)
        s.sellStore()
        assertEquals(cash + value, s.cash, 1.0)
        assertNull(s.games!!.store)
    }

    @Test
    fun anExclusiveBringsPlayers() {
        val s = studio()
        Sandbox.research(s, Research.DIGITAL_STORES)
        s.openStore()
        s.play(STORE_BUILD_WEEKS + 1)
        var target = s.exclusiveTargets().firstOrNull { !it.mega }
        var guard = 0
        while (target == null && guard++ < 60) {
            s.play(1)
            target = s.exclusiveTargets().firstOrNull { !it.mega }
        }
        assertNotNull(target)
        s.signStoreExclusive(target.title)
        val users = s.games!!.store!!.users
        Sandbox.jumpTo(s, target.week)
        s.play(1)
        val e = s.games!!.store!!.exclusives.single()
        assertTrue(e.landed)
        assertTrue(s.games!!.store!!.users > users * 0.99)
    }

    @Test
    fun noStoresBeforeDownloads() {
        val s = studio(StartPoint.THE_WEB)
        Sandbox.research(s, Research.DIGITAL_STORES)
        assertEquals("Nobody sells games as downloads yet. Wait for 2004.", s.openStoreBlocker())
    }
}
