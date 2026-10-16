package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TradeShowTest {
    private fun studio(seed: Long = 3) = GameState.create("T", "F", Lane.GAMES, seed).apply { cash = 2_000_000.0 }

    /** Starts a game and lets the team work on it until it's [done] that far. */
    private fun GameState.develop(title: String = "Moss Knight", done: Double = 0.6) {
        startGame(GameConcept(title, Topic.FANTASY, Genre.RPG, platforms = listOf(Platform.PC), size = GameSize.SMALL))
        repeat(200) {
            if (games!!.project!!.completion >= done) return
            while (needsDecision) choose(events.firstOrNull()?.choices?.lastIndex ?: return)
            advance()
        }
    }

    /** Runs the clock to the show and returns its decision. */
    private fun GameState.toTheShow(): PendingEvent {
        repeat(60) {
            events.firstOrNull { it.kind == EventKind.Convention }?.let { return it }
            // Clear anything else that comes up on the way.
            while (needsDecision && events.first().kind != EventKind.Convention) choose(events.first().choices.lastIndex)
            if (!needsDecision) advance()
        }
        error("No show in a year")
    }

    @Test
    fun boothsAndPrices() {
        val s = studio()
        assertEquals(listOf(Booth.SMALL, Booth.MEDIUM), s.boothsOpen())
        assertEquals(15_000.0, s.boothCost(Booth.SMALL))
        assertEquals(90_000.0, s.boothCost(Booth.MEDIUM))
        // The main hall needs a following.
        s.games!!.fans = MAIN_HALL_FANS
        assertEquals(Booth.entries, s.boothsOpen())
        assertEquals(400_000.0, s.boothCost(Booth.LARGE))
        // In the eighties a stand cost a fraction of that.
        val start = StartPoint.entries.first { (it.year ?: 9999) < 1990 }
        val past = GameState.create("T", "F", Lane.GAMES, 3, start = start)
        assertTrue(past.boothCost(Booth.MEDIUM) < 20_000.0)
    }

    @Test
    fun theShowComesOnceAYearAndReportsBack() {
        val s = studio()
        s.develop(done = 0.3)
        assertEquals(SHOW_WEEK - GameClock.weekOfYear(s.week), s.nextShow()!!.weeks)
        val show = s.toTheShow()
        assertEquals(SHOW_WEEK, GameClock.weekOfYear(s.week))
        assertEquals(0, s.nextShow()!!.weeks)
        assertEquals(listOf("Small booth", "Medium booth", "Skip it"), show.choices.map { it.label })
        val cash = s.cash
        val fans = s.games!!.fans
        val hype = s.games!!.project!!.hype
        s.choose(1)
        assertEquals(cash - 90_000.0, s.cash, 1.0)
        assertTrue(s.games!!.fans > fans)
        assertTrue(s.games!!.project!!.hype > hype)
        assertEquals(1, s.stats.tradeShows)
        // The report comes straight after.
        val report = (s.events.first().kind as ShowResults).report
        assertEquals(Booth.MEDIUM, report.booth)
        assertEquals("Moss Knight", report.game)
        assertTrue(report.visitors > 0)
        assertTrue(report.press.isNotEmpty())
        s.choose(0)
        assertTrue(!s.needsDecision)
        // And next year's is 52 weeks away.
        s.advance()
        assertEquals(GameClock.WEEKS_PER_YEAR - 1, s.nextShow()!!.weeks)
    }

    @Test
    fun skippingCostsNothing() {
        val s = studio()
        s.toTheShow()
        val cash = s.cash
        s.choose(s.events.first().choices.lastIndex)
        assertEquals(cash, s.cash)
        assertTrue(!s.needsDecision)
        assertEquals(0, s.stats.tradeShows)
    }

    @Test
    fun theCrowdJudgesWhatItSees() {
        // A first-week prototype on a proper stand gets panned as too early, and half the hype.
        val early = studio().apply { develop(done = 0.05) }
        val later = studio().apply { develop(done = 0.6) }
        assertTrue(early.showcase().early)
        assertTrue(!later.showcase().early)
        early.holdShow(Booth.MEDIUM)
        later.holdShow(Booth.MEDIUM)
        val a = (early.events.last().kind as ShowResults).report
        val b = (later.events.last().kind as ShowResults).report
        assertTrue(a.press.any { "too early" in it }, "$a")
        assertTrue(b.hype > a.hype * 1.5, "$a / $b")
        // A bigger booth draws a bigger crowd.
        val big = studio().apply {
            develop(done = 0.6)
            games!!.fans = MAIN_HALL_FANS
            holdShow(Booth.LARGE)
        }
        assertTrue((big.events.last().kind as ShowResults).report.visitors > b.visitors * 1.5, "${big.events.last()} / $b")
    }

    @Test
    fun withNothingToShowFansComeButNoHype() {
        val s = studio()
        s.holdShow(Booth.SMALL)
        val r = (s.events.last().kind as ShowResults).report
        assertNull(r.game)
        assertEquals(0.0, r.hype)
        assertTrue(r.fans > 0)
    }

    @Test
    fun aBuggyDemoOnTheMainStageCanCrash() {
        var crashes = 0
        for (seed in 1L..30L) {
            val s = studio(seed).apply {
                develop(done = 0.7)
                games!!.fans = MAIN_HALL_FANS
                games!!.project!!.bugs = 500.0
            }
            assertTrue(s.showcase().buggy)
            s.holdShow(Booth.LARGE)
            val r = (s.events.last().kind as ShowResults).report
            if (r.press.any { "crashed" in it }) {
                crashes++
                assertTrue(!r.bestInShow)
            }
        }
        assertTrue(crashes in 4..22, "crashes: $crashes")
    }

    @Test
    fun bestInShowAndItsAchievement() {
        // Over a run, the autopilot books booths, and a strong studio wins Best in Show now and then.
        var shows = 0
        var best = 0
        for (seed in 1L..6L) {
            val s = GameState.create("T", "F", Lane.GAMES, seed)
            val pilot = Autopilot()
            while (!s.isOver && s.week < 52 * 12) pilot.playWeek(s)
            println("seed $seed: ${s.stats.tradeShows} shows, best ${s.games?.releases?.maxOfOrNull { it.score }}, ${s.games?.releases?.count { it.score >= 8.5 }} games of 8.5+")
            shows += s.stats.tradeShows
            best += s.stats.bestInShow
            if (s.stats.bestInShow > 0) assertNotNull(s.achievements[Achievement.BEST_IN_SHOW])
        }
        println("trade shows: $shows booths, $best Best in Show")
        assertTrue(shows >= 12, "the autopilot goes to most shows: $shows")
        assertTrue(best >= 1, "someone wins Best in Show")
    }
}
