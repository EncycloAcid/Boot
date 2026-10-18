package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Stakes, earn-outs, what owned studios make, giants buying studios, and what a purchase teaches you. */
class BuyingTest {
    private fun rich(seed: Long = 50) = GameState.create("Pixel Forge", "Sam", Lane.GAMES, seed).apply { cash = 2e11 }

    private val indie = studio("Team Cherry")!!

    private fun GameState.waitForAnswer() = repeat(6) {
        val dealt = { k: EventKind -> k is IntegrationChoice || k is DealCounter || k is BiddingWar || k is RegulatorReview }
        while (needsDecision && !dealt(events.first().kind)) choose(events.first().choices.lastIndex)
        if (needsDecision && events.first().kind is DealCounter) choose(0)
        if (games!!.deal == null && !needsDecision) return
        if (!needsDecision) advance()
    }

    @Test
    fun aStakeEarnsAndWarmsTheOwners() {
        val s = rich()
        val before = s.acceptOdds(indie, 0.1)
        val cash = s.cash
        s.buyStake(indie.name)
        assertEquals(STAKE_STEP, s.stakeIn(indie.name), 1e-9)
        assertTrue(s.cash < cash)
        assertTrue(s.acceptOdds(indie, 0.1) > before)
        s.buyStake(indie.name)
        assertEquals(MAX_STUDIO_STAKE, s.stakeIn(indie.name), 1e-9)
        assertNotNull(s.stakeBlocker(indie))
        assertFailsWith<GameException> { s.buyStake(indie.name) }
        // A share of the profit comes home every week, and the stake counts in what you're worth.
        val money = s.cash
        s.stakesWeek(s.games!!)
        assertTrue(s.cash > money)
        assertTrue(s.stakesValue > 0)
        // What you hold comes off the price of the rest.
        assertEquals(s.offerPrice(indie, 0.2) * (1 - MAX_STUDIO_STAKE), s.offerCash(indie, 0.2, false), 1.0)
        s.sellStake(indie.name)
        assertEquals(0.0, s.stakeIn(indie.name), 1e-9)
    }

    @Test
    fun buyingTheRestFoldsTheStakeIn() {
        val s = rich()
        s.buyStake(indie.name)
        s.acquire(indie.name)
        val sub = s.games!!.subsidiaries.single()
        assertEquals(0.0, s.stakeIn(indie.name), 1e-9)
        assertTrue(sub.paid > s.acquisitionPrice(indie))
    }

    @Test
    fun anEarnOutPaysLaterIfTheStudioDoesWell() {
        var tried = 0
        for (seed in 1L..30L) {
            val s = rich(seed)
            val cash = s.offerCash(indie, 0.6, false, earnOut = true)
            assertEquals(s.offerPrice(indie, 0.6) * EARN_OUT_NOW, cash, 1.0)
            assertNotNull(s.offerBlocker(indie, 0.6, inShares = true, earnOut = true))
            assertTrue(s.acceptOdds(indie, 0.0, earnOut = true) < s.acceptOdds(indie, 0.0))
            s.makeOffer(indie.name, 0.6, earnOut = true)
            s.waitForAnswer()
            val sub = s.games!!.subsidiaries.firstOrNull() ?: continue
            tried += 1
            assertTrue(sub.earnOutDue > 0)
            // Two years on: a happy studio that kept its people gets paid.
            sub.morale = 0.9
            sub.earnOutWeek = s.week
            val due = sub.earnOutDue
            val money = s.cash
            s.earnOutWeek(s.games!!)
            assertEquals(0.0, sub.earnOutDue, 1e-9)
            assertTrue(s.cash <= money - due + 1)
            // An unhappy one doesn't, and the sellers are bitter.
            sub.earnOutDue = 1_000_000.0
            sub.morale = 0.2
            val money2 = s.cash
            s.earnOutWeek(s.games!!)
            assertEquals(money2, s.cash, 1.0)
            assertTrue(sub.morale < 0.2)
            break
        }
        assertTrue(tried > 0)
    }

    @Test
    fun giantsBuyStudiosAndBuyOutYourStake() {
        val s = rich()
        s.buyStake(indie.name)
        s.enqueue(BiddingWar(indie.name, "Tencent", 1e9, false))
        val cash = s.cash
        s.choose(1)
        assertEquals("Tencent", s.games!!.rivalOwned[indie.name])
        assertEquals(0.0, s.stakeIn(indie.name), 1e-9)
        assertTrue(s.cash > cash)
        assertTrue(indie !in s.studiosForSale())
        assertNotNull(s.offerBlocker(indie, 0.2, false))
        assertNotNull(s.stakeBlocker(indie))
        assertEquals(0.0, s.acceptOdds(indie, 0.5))
    }

    @Test
    fun youChooseWhatYourStudiosMakeNext() {
        val s = rich()
        s.games!!.subsidiaries += Subsidiary("FromSoftware", s.week, 1e9)
        val sub = s.games!!.subsidiaries.single()
        s.setNextSeries("FromSoftware", "Armored Core")
        assertEquals("Armored Core", s.studioPitch(sub).franchise)
        s.setNextSeries("FromSoftware", null)
        assertNull(s.studioPitch(sub).franchise)
        assertFailsWith<GameException> { s.setNextSeries("FromSoftware", "Halo") }
        s.greenlight("FromSoftware", StudioBudget.NORMAL)
        assertTrue(!sub.pitchChosen)
    }

    @Test
    fun aPurchaseTeachesYouAndItsPeopleComeAsSpecialists() {
        val s = rich()
        val big = studio("Capcom")!!
        val known = s.games!!.knownFits.size
        s.week = 52 * 4
        s.acquire(big.name)
        if (s.games!!.market.games.any { canonicalStudio(it.studio) == big.name }) assertTrue(s.games!!.knownFits.size > known)
        s.enqueue(IntegrationChoice(big.name))
        s.choose(1)
        val joined = s.staff.takeLast(3)
        assertTrue(joined.any { it.specialty == Specialty.ENGINE_PROGRAMMER })
        assertTrue(joined.any { it.role == Role.DESIGNER && it.specialty != null })
        // What you've put in, what it's worth and what it's earned, at a glance.
        assertTrue(s.studioHoldings().paid > 0 && s.studioHoldings().worth > 0)
    }

    @Test
    fun oldSavesLoad() {
        val s = rich()
        s.buyStake(indie.name)
        val back = GameState.decode(s.encode())
        assertEquals(STAKE_STEP, back.stakeIn(indie.name), 1e-9)
    }
}
