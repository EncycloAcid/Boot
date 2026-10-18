package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LaneSalesTest {
    /** A games studio that also runs a web agency, with people in both. */
    private fun twoLanes(): GameState = GameState.create("Pixel Forge", "F", Lane.GAMES, 3).apply {
        cash = 1_000_000.0
        openLane(Lane.WEBSITES)
        repeat(4) { advance() }
        hireDirect(makeCandidate(Role.DESIGNER), Lane.WEBSITES)
        hireDirect(makeCandidate(Role.ENGINEER), Lane.WEBSITES)
        hireDirect(makeCandidate(Role.OPERATOR), Lane.WEBSITES)
        hireDirect(makeCandidate(Role.DESIGNER), Lane.GAMES)
    }

    @Test
    fun youCantSellYourLastBusiness() {
        val s = GameState.create("Pixel Forge", "F", Lane.GAMES, 3)
        assertNotNull(s.sellLaneBlocker(Lane.GAMES))
        assertFailsWith<GameException> { s.sellLane(Lane.GAMES) }
        assertNotNull(s.sellLaneBlocker(Lane.WEBSITES))
    }

    @Test
    fun sellingABusinessPaysAndTakesItsPeople() {
        val s = twoLanes()
        assertNull(s.sellLaneBlocker(Lane.WEBSITES))
        val price = s.laneSalePrice(Lane.WEBSITES)
        assertTrue(price > 0)
        val followers = s.followersOf(Lane.WEBSITES).map { it.id }.toSet()
        val cash = s.cash
        val before = s.staff.size
        s.sellLane(Lane.WEBSITES)
        assertEquals(price, s.cash - cash, 1.0)
        assertEquals(listOf(Lane.GAMES), s.lanes)
        assertNull(s.web)
        assertEquals(1, s.stats.lanesSold)
        // Those whose craft fits games came along; the rest went with the buyer.
        assertTrue(s.staff.none { it.lane == Lane.WEBSITES })
        assertTrue(s.staff.filter { it.id in followers }.all { it.lane == Lane.GAMES })
        assertEquals(before - (3 - followers.size), s.staff.size)
        assertTrue(s.news.any { "buys" in it.headline })
    }

    @Test
    fun downToOneBusinessTheCompanyPullsOneWay() {
        val s = twoLanes()
        val before = s.context(Lane.GAMES).mods.output
        s.sellLane(Lane.WEBSITES)
        assertTrue(s.focused)
        assertEquals(before * FOCUS_OUTPUT, s.context(Lane.GAMES).mods.output, 1e-9)
        s.focusedUntil = s.week
        assertFalse(s.focused)
    }

    @Test
    fun focusSellsEverythingElse() {
        val s = twoLanes()
        s.openLane(Lane.APPS)
        assertEquals(3, s.lanes.size)
        val total = s.focusPrice(Lane.GAMES)
        val cash = s.cash
        assertNull(s.focusBlocker(Lane.GAMES))
        s.focusOn(Lane.GAMES)
        assertEquals(listOf(Lane.GAMES), s.lanes)
        assertEquals(total, s.cash - cash, 2.0)
        assertEquals(2, s.stats.lanesSold)
        assertNotNull(s.focusBlocker(Lane.GAMES))
    }

    @Test
    fun aGamesBusinessTakesItsStudiosWithIt() {
        val s = GameState.create("Pixel Forge", "F", Lane.WEBSITES, 3).apply {
            cash = 1_000_000.0
            openLane(Lane.GAMES)
        }
        val bare = s.laneSalePrice(Lane.GAMES)
        Sandbox.own(s, "Team Cherry")
        assertTrue(s.laneSalePrice(Lane.GAMES) > bare)
        s.sellLane(Lane.GAMES)
        assertNull(s.games)
    }

    @Test
    fun oldSavesLoadUnfocused() {
        val back = GameState.decode(twoLanes().encode())
        assertEquals(-1, back.focusedUntil)
        assertFalse(back.focused)
    }
}
