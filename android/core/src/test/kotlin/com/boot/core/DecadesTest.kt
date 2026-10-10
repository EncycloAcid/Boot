package com.boot.core

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DecadesTest {
    @AfterTest
    fun resetClock() {
        GameClock.startYear = null
    }

    private fun start(point: StartPoint, lane: Lane = Lane.GAMES, seed: Long = 7) =
        GameState.create("Retro", "Ada", lane, seed = seed, start = point)

    @Test
    fun aRunStartedTodayIsUnchanged() {
        val s = GameState.create("Now", "Ada", Lane.GAMES, seed = 7)
        assertNull(s.startYear)
        assertFalse(s.isDecades)
        assertEquals("Y1 W1", s.dateLabel)
        assertEquals(GameState.LENGTH_WEEKS, s.runLength)
        assertEquals(0, s.timelineWeek)
        assertEquals(Era.BOOTSTRAP_YEARS, s.world.era)
    }

    @Test
    fun theCalendarShowsRealDates() {
        val s = start(StartPoint.HOME_COMPUTERS)
        assertEquals(1977, s.startYear)
        assertEquals("Jan 1977", s.dateLabel)
        assertEquals((MODERN_YEAR - 1977) * 52, s.runLength)
        assertEquals(-50 * 52, s.timelineWeek)
        assertEquals("Jul 1978", GameClock.label(52 + 26))
        // Saves remember where the calendar starts.
        GameClock.startYear = null
        val back = GameState.decode(s.encode())
        assertEquals("Jan 1977", back.dateLabel)
    }

    @Test
    fun historyStartsInItsEra() {
        val s = start(StartPoint.THE_WEB)
        assertEquals(Era.DOT_COM_BOOM, s.world.era)
        assertEquals((2000 - 1995) * 52, s.world.eraLength)
        assertTrue(Era.HOME_COMPUTERS in s.world.pastEras)
        assertEquals(Era.MULTIMEDIA, s.world.history.last().era)
        assertTrue(s.world.history.all { it.endWeek <= 0 })
    }

    @Test
    fun businessesArriveWhenTheyBecamePossible() {
        val s = start(StartPoint.HOME_COMPUTERS)
        assertTrue(s.laneOpen(Lane.GAMES))
        assertTrue(s.laneOpen(Lane.HARDWARE))
        for (lane in listOf(Lane.WEBSITES, Lane.SOCIAL, Lane.APPS, Lane.AI)) {
            assertFalse(s.laneOpen(lane))
            assertNotNull(s.laneClosed(lane))
        }
        assertEquals(listOf(Lane.HARDWARE), s.pivotChoices)
        val web = start(StartPoint.THE_WEB)
        assertTrue(web.laneOpen(Lane.WEBSITES))
        assertFalse(web.laneOpen(Lane.SOCIAL))
    }

    @Test
    fun platformsAreTheOnesOfTheDay() {
        val early = start(StartPoint.HOME_COMPUTERS).platformsOnSale
        // January 1977: the Apple II and friends. The Atari 2600 is out that autumn.
        assertEquals(listOf(Platform.PC), early)
        assertTrue(Platform.ATARI_2600.isAvailable(-50 * 52 + 40))
        assertFalse(Platform.PLAYSYSTEM_5 in early)
        assertFalse(Platform.GRPHONE in early)
        val web = start(StartPoint.THE_WEB).platformsOnSale
        assertTrue(Platform.SNES in web)
        assertFalse(Platform.ATARI_2600 in web)
        val phones = start(StartPoint.SMARTPHONES).platformsOnSale
        assertTrue(Platform.WII in phones && Platform.XBOX_360 in phones && Platform.NINTENDO_DS in phones)
        // None of the old consoles are still on sale today.
        val today = GameState.create("Now", "Ada", Lane.GAMES, seed = 7).platformsOnSale
        assertFalse(today.any { it.retire < 0 })
    }

    @Test
    fun rivalsAndPeersAreTheGiantsOfTheDay() {
        val s = start(StartPoint.HOME_COMPUTERS)
        assertEquals("Atari", s.world.rival(Lane.GAMES)!!.name)
        assertEquals("Commodore", s.world.rival(Lane.HARDWARE)!!.name)
        assertFalse("TikTok" in peersIn(Lane.SOCIAL, 2005))
        assertTrue("MySpace" in peersIn(Lane.SOCIAL, 2005))
        assertEquals("FIFA", seriesNameIn("EA Sports FC", 2010))
        assertEquals("Computer Gaming World", outletIn("IGN", 1985).let { if (it == "Electronic Games") "Computer Gaming World" else it })
        assertEquals("IGN", outletIn("IGN", 2010))
    }

    @Test
    fun playingThroughHistoryReachesToday() {
        val s = start(StartPoint.SMARTPHONES, Lane.GAMES, seed = 11)
        val ending = Autopilot().playToEnd(s)
        assertNotNull(ending)
        if (ending.kind == EndingKind.INDIE) {
            assertEquals(s.runLength, ending.week)
            assertEquals(MODERN_YEAR, s.calendarYear)
        }
        // History's eras came round in order.
        val seen = s.world.history.map { it.era } + s.world.era
        val order = History.map { it.second }
        val expected = order.filter { it in seen }
        assertEquals(expected, seen.filter { it in order }.distinct())
        // Real series came out under their real names, and no rival game was AAA before its time.
        val m = s.games!!.market
        assertTrue(m.games.any { it.title == "Call of Duty: Black Ops" || it.title.startsWith("Super Mario Galaxy") || it.title.startsWith("Halo") })
        assertTrue(m.games.none { it.size == GameSize.AAA && s.yearAt(it.week) < 2005 })
    }

    @Test
    fun theEightiesHaveTheirOwnReviewers() {
        val s = start(StartPoint.CONSOLE_WARS, Lane.GAMES, seed = 5)
        Autopilot().let { a -> repeat(52 * 4) { if (!s.isOver) a.playWeek(s) } }
        val outlets = s.games!!.releases.flatMap { r -> r.reviews.map { it.outlet } }.toSet()
        assertTrue(outlets.isNotEmpty())
        assertFalse(Outlet.IGN in outlets)
        assertTrue(outlets.all { s.games!!.releases.any { r -> s.yearAt(r.week) in it.years } })
    }
}
