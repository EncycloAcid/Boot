package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NewsAndQuotesTest {
    private fun studio(seed: Long = 3) = GameState.create("Pixel Forge", "Sam", Lane.GAMES, seed).apply { cash = 5_000_000.0 }

    /** Makes and ships a small game, finished the honest way. */
    private fun GameState.shipOne(): GameRelease {
        startGame(GameConcept("Moss Knight", Topic.FANTASY, Genre.RPG, platforms = listOf(Platform.PC), size = GameSize.SMALL))
        repeat(300) {
            if (games!!.project?.isReady != false) return@repeat
            while (needsDecision) choose(events.first().choices.lastIndex)
            advance()
        }
        launchGame()
        return games!!.releases.first()
    }

    @Test
    fun everyCriticHasAPullQuoteAndPlayersHaveTheirSay() {
        val s = studio()
        val r = s.shipOne()
        assertTrue(r.reviews.isNotEmpty())
        assertTrue(r.reviews.all { it.headline.isNotBlank() })
        assertEquals(3, r.playerQuotes.size)
        assertTrue(r.playerQuotes.all { it.text.isNotBlank() && it.hours > 0 })
        // The thumbs follow the players' score.
        val ups = r.playerQuotes.count { it.recommended }
        if (r.userScore >= 8.5) assertEquals(3, ups)
        if (r.userScore < 5) assertEquals(0, ups)
        // Survives a save.
        val back = GameState.decode(s.encode()).games!!.releases.first()
        assertEquals(r.playerQuotes, back.playerQuotes)
        assertEquals(r.reviews.map { it.headline }, back.reviews.map { it.headline })
    }

    @Test
    fun outletsHaveTheirOwnSeals() {
        val s = studio()
        val inputs = { score: Double ->
            ReviewInputs(
                "Moss Knight", GameConcept("Moss Knight", Topic.FANTASY, Genre.RPG, platforms = listOf(Platform.PC), size = GameSize.SMALL),
                List(3) { listOf(1.0, 1.0, 1.0) }, score, 0.5, 0.9, 1.0, 0.5, 5, 5.0, 30.0, 1.0,
                onTrend = false, repeated = false, rushedSequel = false, worthySequel = false, stepBack = false, record = false,
                wrongCrowd = false, awkwardPlatform = null,
            )
        }
        val great = writeReviews(inputs(9.8), SeededRNG(1))
        assertTrue(great.reviews.any { it.badge != null }, "${great.reviews.map { it.outlet to it.badge }}")
        assertTrue(great.reviews.first { it.outlet == Outlet.EDGE }.headline in setOf("Exceptional", "Accomplished"))
        val poor = writeReviews(inputs(3.0), SeededRNG(1))
        assertTrue(poor.reviews.none { it.badge != null })
        assertTrue(poor.players.count { it.recommended } <= 1)
        assertNotNull(s)
    }

    @Test
    fun wordsNeverChangeWhatHappens() {
        // Two identical games, one of which reads its news: the outcomes match exactly.
        val a = studio(seed = 21)
        val b = studio(seed = 21)
        val pilotA = Autopilot()
        val pilotB = Autopilot()
        repeat(120) {
            pilotA.playWeek(a)
            pilotB.playWeek(b)
            b.news.lastOrNull()?.topComment
        }
        assertEquals(a.cash, b.cash, 0.001)
        assertEquals(a.games!!.releases.map { it.score }, b.games!!.releases.map { it.score })
    }

    @Test
    fun aLaunchMakesTheNewsWithARoundupAndAQuote() {
        val s = studio()
        val r = s.shipOne()
        val story = s.news.last { it.desk == NewsDesk.YOU && it.tag == NewsTag.REVIEW }
        assertEquals(r.title, story.cover?.title)
        assertTrue(r.reviews.first().outlet.title in story.body, story.body)
        assertNotNull(story.quote)
        assertTrue(story.quoteBy in r.reviews.map { it.outlet.title })
        // Modern readers comment.
        assertTrue(story.comments > 0)
        assertNotNull(story.topComment)
    }

    @Test
    fun beforeTheWebNobodyComments() {
        val start = StartPoint.entries.first { (it.year ?: 9999) < 1990 }
        val s = GameState.create("Pixel Forge", "Sam", Lane.GAMES, 3, start = start).apply { cash = 5_000_000.0 }
        s.shipOne()
        assertTrue(s.news.filter { it.tag == NewsTag.REVIEW }.all { it.comments == 0 && it.topComment == null })
    }

    @Test
    fun theChartsEveryMonth() {
        val s = studio()
        val pilot = Autopilot()
        while (s.week < 60) pilot.playWeek(s)
        val charts = s.news.filter { it.tag == NewsTag.CHARTS }
        assertTrue(charts.isNotEmpty())
        assertTrue(charts.last().body.startsWith("1. "), charts.last().body)
    }

    @Test
    fun rumoursBeforeTheShows() {
        val s = studio()
        s.startGame(GameConcept("Moss Knight", Topic.FANTASY, Genre.RPG, platforms = listOf(Platform.PC), size = GameSize.SMALL))
        s.week = (s.week / GameClock.WEEKS_PER_YEAR) * GameClock.WEEKS_PER_YEAR + SHOW_WEEK - 2
        s.showMomentsWeek(s.games!!)
        assertTrue(s.news.any { it.tag == NewsTag.RUMOUR && it.headline.startsWith("What to expect at") }, "${s.news.map { it.headline }}")
    }
}
