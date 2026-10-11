package com.boot.core

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EraLifeTest {
    @AfterTest
    fun resetClock() {
        GameClock.startYear = null
    }

    private fun game(lane: Lane = Lane.GAMES, start: StartPoint = StartPoint.TODAY, seed: Long = 5) =
        GameState.create("Era Co", "Ada", lane, seed = seed, start = start)

    /** Answer anything that comes up with its last choice and move on a week. */
    private fun GameState.step() {
        while (needsDecision) if (pendingPivot != null) stayFocused() else choose(events[0].choices.lastIndex)
        advance()
    }

    /** End the current era next week. */
    private fun GameState.endEraNow() {
        world.eraLength = week - world.eraStartWeek + 1
        step()
    }

    @Test
    fun aRunSetInThePastStartsWithItsErasChallenge() {
        val s = game(start = StartPoint.HOME_COMPUTERS)
        val c = assertNotNull(s.world.challenge)
        assertEquals(Era.HOME_COMPUTERS, c.era)
        assertEquals(ChallengeKind.HIT, c.kind)
        // A web agency can't ship hits, so it gets a different goal.
        val web = game(Lane.WEBSITES, StartPoint.THE_WEB)
        assertEquals(ChallengeKind.VALUE_GROWTH, web.world.challenge!!.kind)
    }

    @Test
    fun theBootstrapYearsHaveNoChallengeButTheNextEraDoes() {
        val s = game()
        assertNull(s.world.challenge)
        s.endEraNow()
        assertTrue(s.world.era != Era.BOOTSTRAP_YEARS)
        assertNotNull(s.world.challenge)
    }

    @Test
    fun meetingAChallengePaysOut() {
        val s = game(start = StartPoint.SMARTPHONES)
        s.world.challenge = EraChallenge(Era.SMARTPHONES, ChallengeKind.PROFIT_STREAK, 0.0, "Test", "", 75_000.0)
        val cash = s.cash
        val rp = s.researchPoints
        s.step()
        assertTrue(s.world.challenge!!.done)
        assertEquals(1, s.stats.eraChallengesWon)
        assertTrue(s.cash > cash + 50_000)
        assertTrue(s.researchPoints >= rp + 25)
    }

    @Test
    fun anEndedEraIsReviewed() {
        val s = game(start = StartPoint.CONSOLE_WARS)
        repeat(10) { s.step() }
        s.endEraNow()
        val span = s.world.history.last()
        assertEquals(Era.CONSOLE_WARS, span.era)
        assertTrue(span.lived)
        assertNotNull(span.challengeWon)
        assertTrue(s.news.any { it.headline.contains("in review") })
        // History from before the run isn't "lived".
        assertTrue(s.world.history.first().let { !it.lived })
    }

    @Test
    fun theErasMomentComesAtItsHeight() {
        val s = game(start = StartPoint.THE_WEB)
        s.world.eraStartWeek = s.week - s.world.eraLength / 2
        s.week += 30
        s.advance()
        assertTrue(s.events.any { it.kind is EraMoment } || s.world.momentDone)
        assertTrue(s.world.momentDone)
    }

    @Test
    fun everyErasMomentCanBeAnswered() {
        for (era in Era.entries.filter { it != Era.BOOTSTRAP_YEARS }) {
            for (choice in 0..1) {
                val s = game(seed = era.ordinal.toLong())
                s.cash = 5_000_000.0
                s.world.era = era
                s.enqueue(EraMoment(era))
                val e = s.events.singleOrNull() ?: error("$era has no moment for a game studio")
                s.choose(choice)
                assertTrue(s.events.none { it.kind is EraMoment })
            }
        }
    }

    @Test
    fun trendsFollowTheEra() {
        val s = game(start = StartPoint.SMARTPHONES)
        val trends = (1..40).mapNotNull { s.eraTrend() }
        assertEquals(40, trends.size)
        assertTrue(trends.any { it.genre == Genre.CASUAL })
        assertTrue(Era.entries.filter { it != Era.BOOTSTRAP_YEARS }.all { era -> s.world.era = era; s.eraTrend() != null })
    }

    @Test
    fun eraStateSurvivesASave() {
        val s = game(start = StartPoint.HOME_COMPUTERS)
        repeat(5) { s.step() }
        s.endEraNow()
        val back = GameState.decode(s.encode())
        assertEquals(s.world.challenge, back.world.challenge)
        assertEquals(s.world.history, back.world.history)
        assertEquals(s.world.eraStartValue, back.world.eraStartValue)
    }
}
