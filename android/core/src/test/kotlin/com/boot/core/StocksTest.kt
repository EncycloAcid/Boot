package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StocksTest {
    /** Play [n] weeks, saying no to whatever comes up. */
    private fun GameState.pass(n: Int) = repeat(n) {
        if (pendingPivot != null) stayFocused()
        while (events.isNotEmpty() && events[0].kind !is MarketEvent) choose(events[0].choices.size - 1)
        advance()
    }

    private fun rich(lane: Lane = Lane.GAMES, seed: Long = 7): GameState =
        GameState.create("Co", "Ada", lane, seed).also { it.cash = 5e11 }

    @Test
    fun pricesMoveAndRemember() {
        val s = rich()
        repeat(10) { s.advance() }
        val stocks = s.stocks()
        assertTrue(stocks.size > 20)
        val ea = s.stock("Electronic Arts")!!
        assertTrue(ea.listed)
        assertEquals("EA", ea.ticker)
        assertEquals(10, s.portfolio.history["Electronic Arts"]!!.size)
        assertTrue(stocks.any { it.change != 0.0 })
        assertTrue(stocks.all { it.price > 0 })
    }

    @Test
    fun buyAndSell() {
        val s = rich()
        s.cash = 10e6
        s.buyShares("Ubisoft", 1e6)
        assertEquals(10e6 - 1e6 * (1 + TRADING_FEE), s.cash, 1.0)
        assertEquals(1e6, s.portfolioValue, 1e6 * 0.01)
        val before = s.valuation
        assertTrue(before > s.cash + 0.9e6)
        repeat(8) { s.advance() }
        val worth = s.portfolioValue
        val cash = s.cash
        s.sellShares("Ubisoft")
        assertTrue(s.portfolio.holdings.isEmpty())
        assertEquals(cash + worth * (1 - TRADING_FEE), s.cash, worth * 0.01)
        assertEquals(worth * (1 - TRADING_FEE) - 1e6 * (1 + TRADING_FEE), s.portfolio.realized, worth * 0.01)
    }

    @Test
    fun privateCompaniesAndBigStakesAreOffLimits() {
        val s = rich()
        assertNotNull(s.buyBlocker("OpenAI", 1e6))
        val ubi = s.stock("Ubisoft")!!
        assertNotNull(s.buyBlocker("Ubisoft", ubi.value * 0.35))
        s.buyShares("Ubisoft", s.maxBuy("Ubisoft"))
        assertEquals(MAX_STAKE, s.stock("Ubisoft")!!.stake, 0.01)
        assertNotNull(s.buyBlocker("Ubisoft", 1e6))
        // Nothing traded before it listed: no Reddit in 2000.
        val past = GameState.create("Co", "Ada", Lane.WEBSITES, 1, start = StartPoint.THE_WEB)
        past.cash = 1e9
        assertTrue(past.stocks().none { it.key == "Reddit" && it.listed })
    }

    @Test
    fun aStakeInYourRivalMakesThemAngry() {
        val s = rich()
        val ea = s.world.rivals.first { it.lane == Lane.GAMES }
        assertEquals("Electronic Arts", ea.name)
        val heat = ea.heat
        s.buyShares("Electronic Arts", s.stock("Electronic Arts")!!.value * 0.06)
        assertTrue(ea.heat >= heat + 0.2)
        assertTrue(s.news.any { "stake in Electronic Arts" in it.headline })
    }

    @Test
    fun takingOverYourRivalEndsTheFight() {
        val s = rich()
        s.week = 52 * 3
        s.bidFor("Electronic Arts", 0.6)
        val bid = s.portfolio.bid!!
        assertTrue(s.cash < 5e11 - bid.cost + 1)
        s.portfolio.bid = bid.copy(odds = 1.0)
        s.pass(4)
        assertNull(s.portfolio.bid)
        assertTrue(s.games!!.owns("Electronic Arts"))
        val ea = s.world.rivals.first { it.lane == Lane.GAMES }
        assertTrue(ea.quietUntil > s.week + 1000)
        assertEquals(1, s.stats.rivalsBeaten)
        assertTrue(s.rankings().none { it.key == "Electronic Arts" })
    }

    @Test
    fun aRejectedBidRefundsLessFees() {
        val s = rich()
        s.week = 52 * 3
        s.bidFor("Ubisoft", 0.25)
        val bid = s.portfolio.bid!!
        s.portfolio.bid = bid.copy(odds = 0.0)
        val held = s.cash
        s.pass(4)
        assertTrue(s.cash > held + bid.cost * 0.97)
        assertTrue(s.cash < held + bid.cost)
        assertFalse(s.games!!.owns("Ubisoft"))
    }

    @Test
    fun companiesYouOwnPayAndCount() {
        val s = rich(Lane.WEBSITES)
        s.week = 52 * 3
        s.bidFor("Squarespace", 0.4)
        s.portfolio.bid = s.portfolio.bid!!.copy(odds = 1.0)
        s.pass(4)
        val o = s.portfolio.owned.single()
        assertEquals("Squarespace", o.key)
        assertTrue(o.lastProfit > 0)
        assertTrue(s.enterpriseValue >= s.ownedValue(o))
        assertTrue(s.world.rivals.first { it.lane == Lane.WEBSITES }.quietUntil > s.week + 1000)
        val cash = s.cash
        s.sellCompany("Squarespace")
        assertTrue(s.cash > cash)
        assertTrue(s.portfolio.owned.isEmpty())
        assertTrue(s.rankings().any { it.key == "Squarespace" })
    }

    @Test
    fun raidersCanTakeYourCompany() {
        val s = rich()
        s.cash = 5e8
        s.list()
        s.venture.founderEquity = 0.3
        s.portfolio.raid = Raid("Tencent", 0.08)
        s.enqueue(RaiderStake("Tencent", 0.08))
        s.choose(2)
        repeat(7) { if (s.events.isEmpty()) s.advance() }
        val offer = s.events.first().kind as TakeoverOffer
        assertFalse(offer.hostile)
        s.choose(0)
        assertEquals(EndingKind.ACQUIRED, s.ending!!.kind)
    }

    @Test
    fun aPoisonPillKeepsThemOut() {
        val s = rich()
        s.list()
        s.venture.founderEquity = 0.3
        s.portfolio.raid = Raid("Tencent", 0.08)
        s.enqueue(RaiderStake("Tencent", 0.08))
        val price = s.listing!!.sentiment
        s.choose(0)
        assertNull(s.portfolio.raid)
        assertTrue(s.portfolio.pillUntil > s.week + 100)
        assertTrue(s.listing!!.sentiment < price)
    }

    @Test
    fun portfoliosSurviveASave() {
        val s = rich()
        s.buyShares("Nintendo", 1e7)
        repeat(3) { s.advance() }
        val back = GameState.decode(s.encode())
        assertEquals(s.portfolio.holdings, back.portfolio.holdings)
        assertEquals(s.portfolioValue, back.portfolioValue, 1.0)
    }
}
