package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DealsTest {
    private fun rich(seed: Long = 50) = GameState.create("Pixel Forge", "Sam", Lane.GAMES, seed).apply { cash = 2e11 }

    private val indie = studio("Team Cherry")!!

    /** Moves time on until the owners answer. */
    private fun GameState.waitForAnswer() = repeat(6) {
        val dealt = { k: EventKind -> k is IntegrationChoice || k is DealCounter || k is BiddingWar || k is RegulatorReview }
        while (needsDecision && !dealt(events.first().kind)) choose(events.first().choices.lastIndex)
        if (games!!.deal == null) return
        if (!needsDecision) advance()
    }

    @Test
    fun ownersHaveAStanceAndAHigherOfferIsLikelierToLand() {
        val s = rich()
        assertEquals(Stance.CLOSED, s.stanceOf(studio("Nintendo")!!))
        assertEquals(0.0, s.acceptOdds(studio("Nintendo")!!, MAX_PREMIUM))
        assertTrue(s.acceptOdds(indie, 0.4) > s.acceptOdds(indie, 0.0))
        // The stance is settled for the year: asking again doesn't change it.
        assertEquals(s.stanceOf(indie), s.stanceOf(indie))
        assertEquals("Nintendo isn't for sale at any price.", s.offerBlocker(studio("Nintendo")!!, 0.5, false))
    }

    @Test
    fun dueDiligenceRevealsWhatsThere() {
        val s = rich()
        val cash = s.cash
        s.dueDiligence(indie.name)
        assertEquals(cash - s.diligenceCost(indie), s.cash, 1.0)
        assertTrue(indie.name in s.games!!.diligence)
        assertTrue(s.log.any { "Due diligence on Team Cherry" in it.text && s.findingAt(indie).title.lowercase() in it.text })
        assertFailsWith<GameException> { s.dueDiligence(indie.name) }
        // What's there comes with the studio either way.
        s.acquire(indie.name)
        assertEquals(s.findingAt(indie), s.games!!.subsidiaries.single().finding)
    }

    @Test
    fun anOfferIsAnsweredInTwoWeeks() {
        // Over a spread of games, a generous offer for an indie mostly lands, and every answer is one of the three.
        var yes = 0
        var counters = 0
        var noes = 0
        for (seed in 1L..40L) {
            val s = rich(seed)
            s.makeOffer(indie.name, 0.5)
            assertNotNull(s.games!!.deal)
            assertNotNull(s.offerBlocker(studio("Mega Crit")!!, 0.2, false))
            s.waitForAnswer()
            assertNull(s.games!!.deal)
            val first = s.events.firstOrNull()
            when {
                first?.kind is IntegrationChoice -> yes++
                first?.kind is DealCounter -> counters++
                else -> {
                    noes++
                    assertTrue(s.games!!.cooloff.getValue(indie.name) > s.week)
                    assertTrue(s.offerBlocker(indie, 0.5, false)!!.startsWith("Team Cherry won't take your calls"))
                }
            }
        }
        assertTrue(yes > noes, "yes $yes counters $counters no $noes")
    }

    @Test
    fun runningAStudioYouBought() {
        val s = rich()
        s.makeOffer(indie.name, MAX_PREMIUM)
        repeat(6) {
            if (s.events.firstOrNull()?.kind is IntegrationChoice) return@repeat
            while (s.needsDecision) {
                val k = s.events.first().kind
                if (k is IntegrationChoice) break
                s.choose(0)
            }
            if (!s.needsDecision) s.advance()
        }
        val pick = s.events.first { it.kind is IntegrationChoice }
        assertEquals(Plan.entries.map { it.title }, pick.choices.map { it.label })
        val team = s.staff.size
        s.choose(Plan.INTEGRATED.ordinal)
        val sub = s.games!!.subsidiaries.single()
        assertEquals(Plan.INTEGRATED, sub.plan)
        assertEquals(team + 3, s.staff.size)
        assertTrue(s.staff.takeLast(3).all { it.skill >= 6 })
        // Once a year.
        assertFailsWith<GameException> { s.runStudio(indie.name, Plan.SQUEEZED) }
        s.week += 52
        s.runStudio(indie.name, Plan.SQUEEZED)
        assertTrue(s.ownedQualityPenalty(indie.name) > 0)
        assertTrue(sub.profitFactor(s.week) > 1.0)
        assertEquals(sub, GameState.decode(s.encode()).games!!.subsidiaries.single())
    }

    @Test
    fun aListedCompanyCanPayInShares() {
        val s = rich()
        assertEquals("Only a listed company can pay in shares.", s.offerBlocker(indie, 0.2, true))
    }

    @Test
    fun aBiddingWarAndTheRegulators() {
        val s = rich()
        s.week = 52 * 4
        s.enqueue(BiddingWar("Capcom", "Tencent", 20e9, false))
        val cash = s.cash
        s.choose(1)
        assertEquals(cash, s.cash, 1.0)
        assertTrue(s.games!!.cooloff.getValue("Capcom") > s.week)
        s.enqueue(RegulatorReview("Capcom", 25e9, false))
        s.choose(0)
        assertTrue(s.games!!.owns("Capcom"))
        assertEquals(cash - 25e9 * 1.03, s.cash, 1e6)
    }
}
