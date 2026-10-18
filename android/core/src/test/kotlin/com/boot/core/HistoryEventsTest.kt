package com.boot.core

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HistoryEventsTest {
    @AfterTest
    fun resetClock() {
        GameClock.startYear = null
    }

    private fun start(point: StartPoint, lane: Lane = Lane.GAMES, seed: Long = 7) =
        GameState.create("Retro", "Ada", lane, seed = seed, start = point).also { Sandbox.setCash(it, 50_000_000.0) }

    /** Jump to the week before [target] and play into it. */
    private fun GameState.reach(target: Int) {
        if (world.decadeStartWeek < 0) advance()
        while (needsDecision) choose(0)
        week = target - 1
        advance()
    }

    @Test
    fun historyIsDatedAndKnownAhead() {
        val s = start(StartPoint.HOME_COMPUTERS)
        val coming = s.historyComing()
        assertEquals(HistoryEvent.SPACE_INVADERS, coming.first().second)
        assertEquals(52 + 23, coming.first().first)
        assertTrue(s.historySoFar().isEmpty())
        // A run started today has no history ahead of it.
        assertTrue(GameState.create("Now", "Ada", Lane.GAMES, seed = 7).historyEvents().isEmpty())
    }

    @Test
    fun doomOffersShareware() {
        val s = start(StartPoint.CONSOLE_WARS)
        s.reach(HistoryEvent.DOOM.weekIn(1985))
        assertTrue(s.news.any { it.headline == HistoryEvent.DOOM.headline })
        assertEquals(Genre.SHOOTER, s.games!!.trend?.genre)
        val pick = s.events.first { it.kind is HistoryChoice }
        assertEquals(HistoryChoice(HistoryEvent.DOOM), pick.kind)
        // Saves keep the pending choice.
        assertEquals(pick.kind, GameState.decode(s.encode()).events.first { it.kind is HistoryChoice }.kind)
        val fans = s.games!!.fans
        s.choose(0)
        assertTrue(s.world.pressures.any { it.title == "Shareware" && it.mine })
        assertTrue(s.games!!.fans >= fans)
        assertEquals(HistoryEvent.DOOM, s.historySoFar().first().second)
    }

    @Test
    fun theFinancialCrisisHitsEveryLane() {
        val s = start(StartPoint.SMARTPHONES, Lane.WEBSITES)
        s.reach(HistoryEvent.CRISIS.weekIn(2007))
        assertTrue(s.world.pressures.any { it.lane == Lane.WEBSITES && it.title == "The financial crisis" && it.demand < 1 })
    }

    @Test
    fun kickstarterRaisesMoney() {
        val s = start(StartPoint.INDIES)
        s.reach(HistoryEvent.KICKSTARTER.weekIn(2012))
        val cash = s.cash
        val raise = s.crowdfundTarget()
        s.choose(0)
        assertEquals(cash + raise, s.cash, raise * 0.01)
    }

    @Test
    fun aDecadeIsReviewed() {
        val s = start(StartPoint.HOME_COMPUTERS)
        s.reach((1980 - 1977) * 52)
        val d = s.world.decades.single()
        assertEquals(1970, d.decade)
        assertEquals("The 1970s", d.title)
        assertTrue(s.news.any { it.headline == "The 1970s in review" })
        assertEquals(d, GameState.decode(s.encode()).world.decades.single())
        // The next decade starts counting from here.
        assertEquals(s.week, s.world.decadeStartWeek)
    }

    @Test
    fun yourHitCanBeTheGameOfTheDecade() {
        val s = start(StartPoint.HOME_COMPUTERS)
        s.advance()
        val g = s.games!!
        g.market.games.clear()
        g.releases.add(
            0,
            GameRelease(
                id = 9001, title = "Moon Base", concept = GameConcept("Moon Base", Topic.SPACE, Genre.ACTION, platforms = listOf(Platform.PC), size = GameSize.SMALL),
                week = 60, reviews = emptyList(), score = 9.4, weeklySales = 1000.0, netShare = 0.7, design = 10.0, tech = 10.0,
                bugs = 0.0, hype = 0.0, devCost = 0.0, report = emptyList(),
            ),
        )
        s.reach((1980 - 1977) * 52)
        val d = s.world.decades.single()
        assertEquals("Moon Base", d.yourBest)
        assertTrue(d.crowned || (d.topScore > 9.4))
        if (d.crowned) assertEquals(1, s.stats.gamesOfTheDecade)
    }

    @Test
    fun theNewStartPointsPlay() {
        for ((point, era) in listOf(StartPoint.MILLENNIUM to Era.DOT_COM_BUST, StartPoint.INDIES to Era.FREE_TO_PLAY)) {
            val s = start(point)
            assertEquals(era, s.world.era)
            assertTrue(s.platformsOnSale.isNotEmpty())
            repeat(60) {
                while (s.needsDecision) s.choose(0)
                s.advance()
            }
        }
    }
}
