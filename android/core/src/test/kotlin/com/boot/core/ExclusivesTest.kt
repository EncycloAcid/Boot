package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExclusivesTest {
    private val ps = Platform.PLAYSYSTEM_5
    private val xbox = Platform.MBOX_Z

    private fun studio(seed: Long = 3) = GameState.create("Pixel Forge", "F", Lane.GAMES, seed).apply {
        cash = 5_000_000.0
        researched += Research.MULTI_PLATFORM
    }

    private fun GameState.make(platforms: List<Platform> = listOf(Platform.PC, ps, xbox)) =
        startGame(GameConcept("Moss Knight", Topic.FANTASY, Genre.RPG, platforms = platforms, size = GameSize.SMALL))

    /** Runs a week at a time, taking the last choice on anything but an exclusive offer. */
    private fun GameState.weeks(n: Int) = repeat(n) {
        while (needsDecision && events.first().kind !is ExclusiveOffer) choose(events.first().choices.lastIndex)
        if (!needsDecision) advance()
    }

    private fun GameState.finish() {
        repeat(400) {
            val p = games!!.project ?: return
            if (p.isReady && 2 in p.milestones && !needsDecision) return
            while (needsDecision) choose(events.first().choices.lastIndex)
            advance()
        }
    }

    private fun GameState.offer(maker: String = "Sony"): ExclusiveOffer {
        val p = games!!.project!!
        val (full, timed) = exclusiveTerms(p, maker)
        val k = ExclusiveOffer(1, maker, full, timed)
        Sandbox.enqueue(this, k)
        return k
    }

    private fun GameState.take(label: String) {
        val e = events.first { it.kind is ExclusiveOffer }
        choose(e.choices.indexOfFirst { it.label == label })
    }

    @Test
    fun theOfferPaysMoreWhenItCostsMorePlayers() {
        val s = studio().apply { make() }
        val p = s.games!!.project!!
        assertEquals(listOf("Sony", "Microsoft"), exclusiveMakers(p.concept))
        // Going Xbox-only gives up more players than going PlayStation-only, so Microsoft pays more.
        assertTrue(s.exclusiveLoss(p.concept, "Microsoft") > s.exclusiveLoss(p.concept, "Sony"))
        assertTrue(s.exclusiveTerms(p, "Microsoft").first > s.exclusiveTerms(p, "Sony").first)
        val (full, timed) = s.exclusiveTerms(p, "Sony")
        assertEquals(full * 0.45, timed, 1e-6)
        // A PC-only game has no console maker to sign with.
        val pc = studio().apply { make(listOf(Platform.PC)) }
        assertTrue(exclusiveMakers(pc.games!!.project!!.concept).isEmpty())
    }

    @Test
    fun aFullExclusivePaysNowAndShipsOnlyOnTheirConsole() {
        val s = studio().apply { make() }
        s.weeks(2)
        val trust = s.games!!.fanTrust
        val k = s.offer()
        val cash = s.cash
        s.take("Full exclusive")
        assertEquals(k.full, s.cash - cash, 1.0)
        val p = s.games!!.project!!
        assertEquals("Sony", p.exclusive?.maker)
        assertTrue(s.games!!.fanTrust < trust)
        assertEquals(1, s.stats.exclusives)
        s.finish()
        s.launchGame()
        val r = s.games!!.releases.first()
        assertEquals(listOf(ps), r.concept.platforms)
        assertEquals("Sony", r.exclusiveTo)
        // It can never go to Xbox or PC.
        assertTrue(s.portTargets(r).none { it.maker != "Sony" })
    }

    @Test
    fun aTimedExclusiveBringsTheOtherVersionsLater() {
        val s = studio().apply { make() }
        s.weeks(2)
        s.offer()
        s.take("Timed exclusive")
        assertTrue(s.games!!.project!!.exclusive!!.timed)
        s.finish()
        s.launchGame()
        val r = s.games!!.releases.first()
        assertEquals(listOf(ps), r.concept.platforms)
        // The others are lined up for the end of the window, and can't be ported to before then.
        assertEquals(2, s.games!!.ports.count { it.releaseId == r.id })
        assertTrue(s.games!!.ports.all { it.doneWeek == s.week + TIMED_WINDOW })
        repeat(TIMED_WINDOW + 1) {
            while (s.needsDecision) s.choose(s.events.first().choices.lastIndex)
            s.advance()
        }
        assertTrue(Platform.PC in r.extraPlatforms && xbox in r.extraPlatforms, "${r.extraPlatforms}")
    }

    @Test
    fun turningItDownKeepsItEverywhere() {
        val s = studio().apply { make() }
        s.weeks(2)
        s.offer()
        s.take("Turn it down")
        assertNull(s.games!!.project!!.exclusive)
        assertTrue("Sony" in s.games!!.project!!.pitched)
        s.finish()
        s.launchGame()
        assertEquals(3, s.games!!.releases.first().concept.platforms.size)
    }

    @Test
    fun youCanPitchAGameOncePerMaker() {
        val s = studio(seed = 7).apply { make() }
        s.weeks(2)
        assertNull(s.exclusiveBlocker(1, "Sony"))
        assertNotNull(s.exclusiveBlocker(1, "Nintendo"))
        assertTrue(s.pitchOdds(1, "Sony") in 0.05..0.9)
        s.pitchExclusive(1, "Sony")
        assertNotNull(s.exclusiveBlocker(1, "Sony"))
        assertFailsWith<GameException> { s.pitchExclusive(1, "Sony") }
        // A yes comes back as an offer, a little under what they'd offer unasked.
        s.events.firstOrNull { it.kind is ExclusiveOffer }?.let {
            val k = it.kind as ExclusiveOffer
            assertTrue(k.pitched)
            assertEquals(s.exclusiveTerms(s.games!!.project!!, "Sony").first * 0.85, k.full, k.full * 0.05)
        }
    }

    @Test
    fun promisingGamesGetOffersUnasked() {
        var offers = 0
        for (seed in 1L..12L) {
            val s = studio(seed).apply { make() }
            s.games!!.project!!.hype = 60.0
            repeat(40) {
                if (s.events.any { it.kind is ExclusiveOffer }) {
                    offers++
                    return@repeat
                }
                s.weeks(1)
            }
        }
        assertTrue(offers > 0)
    }

    @Test
    fun oldSavesLoadWithoutExclusives() {
        val s = studio().apply { make() }
        val back = GameState.decode(s.encode())
        assertNull(back.games!!.project!!.exclusive)
        assertTrue(back.games!!.project!!.pitched.isEmpty())
    }
}
