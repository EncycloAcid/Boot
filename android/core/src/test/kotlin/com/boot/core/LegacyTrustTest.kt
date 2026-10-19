package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Legacy / trust pack: antique IP, transparency, activist board, access/local, tech demo, museum. */
class LegacyTrustTest {

    private fun studio(seed: Long = 21, start: StartPoint = StartPoint.TODAY) =
        GameState.create("Legacy Soft", "F", Lane.GAMES, seed, start = start).apply {
            cash = 80_000_000.0
            week = 110 // antique IP sellers want two years behind you
            repeat(6) { i ->
                Sandbox.addStaff(this, "Dev $i", if (i % 2 == 0) Role.DESIGNER else Role.ENGINEER, Lane.GAMES, skill = 7)
            }
        }

    private fun GameState.beginToy(title: String = "Toy", genre: Genre = Genre.ACTION, size: GameSize = GameSize.SMALL) {
        startGame(
            GameConcept(title, Topic.FANTASY, genre, platforms = listOf(Platform.PC), size = size, engineId = 0),
        )
    }

    @Test
    fun oldSavesDefaultTransparencyAndEmptyVault() {
        val s = studio()
        val g = s.games!!
        assertEquals(0.5, g.transparency, 1e-9)
        assertTrue(g.ownedIps.isEmpty())
        assertTrue(s.museum.isEmpty())
        val loaded = GameState.decode(s.encode())
        assertEquals(0.5, loaded.games!!.transparency, 1e-9)
        assertTrue(loaded.games!!.ownedIps.isEmpty())
        assertTrue(loaded.museum.isEmpty())
        assertNull(loaded.games!!.project?.techDemoPromise)
        assertEquals(0.0, loaded.games!!.project?.let { 0.0 } ?: 0.0, 1e-9)
    }

    @Test
    fun antiqueCatalogueIsEraAware() {
        val early = studio(start = StartPoint.CONSOLE_WARS)
        // King's Quest died 2004 — not for sale in the console wars.
        assertTrue(early.ipsForSale().none { it.id == "kings-quest" })
        val now = studio()
        assertTrue(now.ipsForSale().any { it.id == "zork" || it.id == "system-shock" })
    }

    @Test
    fun buyingAndRevivingAntiqueIp() {
        val s = studio()
        val ip = s.ipsForSale().first { it.genre.starter && it.topic.starter }
        s.buyIp(ip.id)
        assertTrue(s.games!!.ownedIps.any { it.id == ip.id })
        assertEquals(1, s.stats.antiqueIps)
        val brand = dormantIp(ip.id)!!
        s.startIpRevival(ip.id, IpPath.REMAKE)
        val p = s.games!!.project!!
        assertEquals(ip.id, p.concept.antiqueIp)
        assertEquals(IpPath.REMAKE, p.concept.ipPath)
        val full = GameConcept("x", brand.topic, brand.genre, platforms = listOf(Platform.PC), size = p.concept.size, engineId = 0)
        assertTrue(p.concept.work < full.work)
    }

    @Test
    fun cannotBuyIpTwice() {
        val s = studio()
        val id = s.ipsForSale().first { it.genre.starter && it.topic.starter }.id
        s.buyIp(id)
        assertNotNull(s.buyIpBlocker(id))
        assertFailsWith<GameException> { s.buyIp(id) }
    }

    @Test
    fun devDiaryRaisesTransparencyAndWishlists() {
        val s = studio()
        s.beginToy("Moss")
        val p = s.games!!.project!!
        p.reveals = 1
        p.hype = 10.0
        val beforeT = s.games!!.transparency
        val beforeW = p.wishlists
        s.publishDevDiary()
        assertTrue(s.games!!.transparency > beforeT)
        assertTrue(p.wishlists > beforeW)
        assertEquals(1, s.stats.devDiaries)
    }

    @Test
    fun silenceDuringCrisisHurtsMore() {
        val s = studio()
        s.reputation.scar(ScarKind.SCANDAL, 0.4, s.week, "Press mess")
        val before = s.games!!.transparency
        s.goSilent()
        assertTrue(s.games!!.transparency <= before - 0.1)
    }

    @Test
    fun overshareRoadmapSetsPromise() {
        val s = studio()
        s.beginToy("Neon")
        val p = s.games!!.project!!
        p.reveals = 1
        s.overshareRoadmap()
        assertTrue(p.roadmapOvershared)
        assertTrue(p.techDemoPromise > 0)
    }

    @Test
    fun activistDemandsAppearAfterIpo() {
        val s = studio()
        // Force a listing + board.
        s.week = IPO_EARLIEST_WEEK + 10
        s.cash = 600_000_000.0
        // Give enough enterprise value via cash/history
        repeat(60) {
            s.recordRevenue(2_000_000.0, Lane.GAMES)
            s.ledger += WeekLedger(s.week).also { it.revenue[Lane.GAMES] = 2_000_000.0; it.cash = s.cash }
        }
        // Directly list
        s.listing = Listing("LEGY", s.week, 10.0, 100_000_000.0)
        s.venture.board = Board(lastReviewWeek = s.week - 20).also { it.confidence = 0.3 }
        // Enqueue activist demand
        s.enqueue(EventKind.BoardDemandEvent(BoardDemand.LIVE_OPS))
        assertTrue(s.events.first().kind is EventKind.BoardDemandEvent)
        s.choose(1) // fight
        assertTrue(s.venture.board!!.demandsDefied >= 1)
        assertTrue(s.listing!!.sentiment < 1.0 || s.venture.board!!.confidence < 0.3)
    }

    @Test
    fun accessibilityBetRaisesWorkAndFlagsRelease() {
        val s = studio()
        s.beginToy("Access Me")
        val p = s.games!!.project!!
        p.progress = p.work * 0.3
        val workBefore = p.work
        s.investAccessibility()
        assertTrue(p.accessibilityBet)
        assertTrue(p.work > workBefore)
        assertEquals(1, s.stats.accessibilityBets)
    }

    @Test
    fun localisationBetTracksRegions() {
        val s = studio()
        s.beginToy("World Tour", Genre.SIMULATION)
        val p = s.games!!.project!!
        p.progress = p.work * 0.2
        s.investLocalisation(region = Region.EUROPE)
        assertTrue(Region.EUROPE in p.localisationRegions)
        assertEquals(1, s.stats.localisationBets)
    }

    @Test
    fun techDemoFlexRaisesPromise() {
        val s = studio()
        s.beginToy("Flex")
        val p = s.games!!.project!!
        val hype = s.techDemoFlex(p, "E3", 0.8)
        assertTrue(hype > 0)
        assertTrue(p.techDemoPromise > 15)
        assertEquals(1, s.stats.techDemos)
        assertTrue(p.techDemos >= 1)
    }

    @Test
    fun museumRecordsBeatsAndRoundTrips() {
        val s = studio()
        s.museumBeat(LegacyBeatKind.HIT, "Hit: Test", "9.0")
        s.museumBeat(LegacyBeatKind.SCAR, "Scandal", "oops")
        assertEquals(2, s.museum.size)
        val loaded = GameState.decode(s.encode())
        assertEquals(2, loaded.museum.size)
        assertEquals("Hit: Test", loaded.museumTimeline().first().title)
    }

    @Test
    fun boardDemandEnumIncludesActivist() {
        assertTrue(BoardDemand.LIVE_OPS.activist)
        assertTrue(BoardDemand.SELL_LANE.activist)
        assertFalse(BoardDemand.LAYOFFS.activist)
    }

    @Test
    fun accessLocalLiftOnRelease() {
        val r = GameRelease(
            id = 1, title = "T", concept = GameConcept("T", Topic.FANTASY, Genre.ACTION, platforms = listOf(Platform.PC), size = GameSize.SMALL),
            week = 0, reviews = emptyList(), score = 8.0, weeklySales = 1000.0, netShare = 0.7,
            design = 1.0, tech = 1.0, bugs = 0.0, hype = 0.0, devCost = 0.0, report = emptyList(),
            accessibility = true, localisedRegions = listOf(Region.EUROPE, Region.ASIA_PACIFIC),
        )
        assertTrue(r.accessLocalLift() > 1.05)
    }
}
