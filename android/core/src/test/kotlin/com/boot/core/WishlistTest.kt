package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WishlistTest {
    private fun studio(seed: Long = 3) = GameState.create("Pixel Forge", "F", Lane.GAMES, seed).apply { cash = 5_000_000.0 }

    private fun GameState.make() =
        startGame(GameConcept("Moss Knight", Topic.FANTASY, Genre.RPG, platforms = listOf(Platform.PC), size = GameSize.SMALL))

    /** Runs the clock a week at a time, taking the last choice on anything that comes up but a slip. */
    private fun GameState.weeks(n: Int) = repeat(n) {
        while (needsDecision && events.first().kind !is ReleaseSlip) choose(events.first().choices.lastIndex)
        if (!needsDecision) advance()
    }

    private fun GameState.finish() {
        repeat(300) {
            if (games!!.project?.isReady != false) return
            weeks(1)
        }
    }

    @Test
    fun trailersBringWishlistsAndHypeKeepsThemComing() {
        val s = studio().apply { make() }
        val p = s.games!!.project!!
        s.weeks(1)
        // Nobody can wishlist a game they haven't heard of.
        assertEquals(0.0, p.wishlists)
        val hype = s.reveal(p, "a show", 20.0, 0.6)
        val first = p.wishlists
        assertEquals(hype * (150.0 + s.games!!.fans * 0.03), first, 0.01)
        s.weeks(4)
        val undated = p.wishlists - first
        assertTrue(undated > 0)
        // A date to count down to brings them in faster.
        val dated = studio().apply { make() }
        val q = dated.games!!.project!!
        dated.weeks(1)
        dated.reveal(q, "a show", 20.0, 0.6)
        dated.setReleaseDate(1, dated.week + 30)
        dated.weeks(4)
        assertTrue(q.wishlists - first > undated)
    }

    @Test
    fun theBetterTheReviewsTheMoreWishlistsBuy() {
        assertEquals(0.02, wishlistConversion(3.0))
        assertEquals(0.1, wishlistConversion(7.0), 1e-9)
        assertEquals(0.2, wishlistConversion(10.0))
        // On the day, a big wishlist means a bigger launch.
        fun launch(wishlists: Double): Pair<Double, List<String>> {
            val s = studio(seed = 9).apply { make(); finish() }
            s.games!!.project!!.wishlists = wishlists
            val cash = s.cash
            s.launchGame()
            assertEquals(wishlists, s.games!!.releases.first().wishlists)
            return s.cash - cash to s.games!!.releases.first().report
        }
        val (none, _) = launch(0.0)
        val (lots, report) = launch(50_000.0)
        assertTrue(lots > none + 50_000.0 * 0.02 * 12 * 0.5, "$none vs $lots")
        assertTrue(report.any { "wishlists turned into" in it })
    }

    @Test
    fun theCalendarKnowsTheHolidaysAndTheBigReleases() {
        assertEquals(LaunchWindow.HOLIDAY, LaunchWindow.at(47))
        assertEquals(LaunchWindow.SLUMP, LaunchWindow.at(52 + 1))
        assertNull(LaunchWindow.at(30))
        val s = studio().apply { make() }
        val c = s.games!!.project!!.concept
        val calendar = s.releaseCalendar()
        assertEquals(26, calendar.size)
        assertEquals(s.week + 1, calendar.first().week)
        // A rival everyone's waiting for, just before your date, would bury it.
        val at = s.week + 10
        val quiet = s.launchTiming(c, at).lift
        s.games!!.market.upcoming += Announcement("Big One", "Rival", "Big", at - 2, Topic.SCI_FI, Genre.ACTION, GameSize.AAA, mega = true)
        val busy = s.launchTiming(c, at)
        assertEquals("Big One", busy.rival?.title)
        assertEquals(quiet * 0.8, busy.lift, 1e-9)
        // Released after yours, it doesn't matter.
        assertNull(s.launchTiming(c, at - 3).rival)
    }

    @Test
    fun aDateIsSetAndAnnounced() {
        val s = studio().apply { make() }
        val p = s.games!!.project!!
        assertFailsWith<GameException> { s.setReleaseDate(1, s.week) }
        // Before the announcement, a date is the team's own target.
        s.setReleaseDate(1, s.week + 20)
        assertTrue(s.news.none { "launches" in it.headline })
        s.setReleaseDate(1, null)
        assertNull(p.releaseWeek)
        s.reveal(p, "a show", 20.0, 0.6)
        s.setReleaseDate(1, s.week + 20)
        assertTrue(s.news.any { it.headline.startsWith("Moss Knight launches") })
        val back = GameState.decode(s.encode()).games!!.project!!
        assertEquals(s.week + 20, back.releaseWeek)
        assertEquals(p.wishlists, back.wishlists, 0.01)
    }

    @Test
    fun aFinishedGameLaunchesOnTheDay() {
        val s = studio().apply { make(); finish() }
        s.setReleaseDate(1, s.week + 3)
        val due = s.week + 3
        s.weeks(3)
        assertEquals(due, s.week)
        assertNull(s.games!!.project)
        assertEquals("Moss Knight", s.games!!.releases.first().title)
        assertEquals(due, s.games!!.releases.first().week)
    }

    @Test
    fun anUnfinishedGameHasToBeDelayed() {
        val s = studio().apply { make() }
        val p = s.games!!.project!!
        s.reveal(p, "a show", 20.0, 0.6)
        s.setReleaseDate(1, s.week + 2)
        s.weeks(2)
        val slip = s.events.first()
        assertTrue(slip.kind is ReleaseSlip)
        assertEquals(listOf("Delay four weeks", "Delay until it's done"), slip.choices.map { it.label })
        val trust = s.games!!.fanTrust
        val wishlists = p.wishlists
        s.choose(0)
        assertEquals(s.week + 4, p.releaseWeek)
        assertEquals(1, p.delays)
        assertTrue(s.games!!.fanTrust < trust)
        assertEquals(wishlists * 0.92, p.wishlists, 0.01)
        assertTrue(s.news.any { it.headline == "Moss Knight delayed to ${GameClock.label(s.week + 4)}" })
        // Still not done four weeks later: this time, no date at all.
        s.weeks(4)
        assertTrue(s.events.first().kind is ReleaseSlip)
        s.choose(1)
        assertNull(p.releaseWeek)
        assertEquals(2, p.delays)
        assertTrue(s.news.any { it.headline == "Moss Knight delayed indefinitely" })
    }

    @Test
    fun aWholeCareerStillPlays() {
        // The autopilot never sets dates, but it announces games at shows and sells to the wishlists.
        val s = studio(seed = 11)
        val pilot = Autopilot()
        var most = 0.0
        while (!s.isOver && s.week < 52 * 8) {
            pilot.playWeek(s)
            s.games?.releases?.firstOrNull()?.let { most = maxOf(most, it.wishlists) }
        }
        assertTrue(most > 1_000, "$most")
    }
}
