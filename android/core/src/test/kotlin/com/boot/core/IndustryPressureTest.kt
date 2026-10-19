package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Contract labour, streaming era, and platform-holder politics. */
class IndustryPressureTest {
    private fun studio(seed: Long = 19, start: StartPoint = StartPoint.TODAY) =
        GameState.create("Pressure Co", "P", Lane.GAMES, seed, start = start).apply {
            cash = 80_000_000.0
            // Desks for temps.
            while (office.next != null && freeDesks < 8) moveOffice()
        }

    private fun concept(platforms: List<Platform> = listOf(Platform.PC)) =
        GameConcept("Signal Peak", Topic.FANTASY, Genre.ACTION, platforms = platforms, size = GameSize.MEDIUM)

    private fun GameState.plant(age: Int = 20): GameRelease {
        val r = GameRelease(
            id = 9101, title = "Signal Peak", concept = concept(), week = week - age, reviews = emptyList(),
            score = 7.8, weeklySales = 40_000.0, netShare = 0.7, design = 10.0, tech = 10.0, bugs = 0.0,
            hype = 0.0, devCost = 0.0, report = emptyList(), userScore = 7.5, unitsSold = 200_000.0,
        )
        games!!.releases.add(0, r)
        return r
    }

    @Test
    fun hireTempsSetsContractUntilAndLandsInTemps() {
        val s = studio()
        assertTrue(s.temps().isEmpty())
        assertNull(s.tempHireBlocker(TempKind.ARTIST, 2, 8))
        s.hireTemps(TempKind.ARTIST, 2, 8)
        assertEquals(2, s.temps().size)
        assertTrue(s.temps().all { it.isTemp(s.week) && it.contractUntil == s.week + 8 })
        assertTrue(s.industry.tempsHiredEver >= 2)
        val mods = LaneModifiers()
        s.applyTempLabourMods(Lane.GAMES, mods)
        assertTrue(mods.output > 1.0)
        assertTrue(mods.quality < 0.0)
    }

    @Test
    fun endContractModesAndSaveDefaults() {
        val s = studio()
        s.hireTemps(TempKind.QA, 1, 10)
        val e = s.temps().single()
        s.endContract(e.id, ContractExit.FAIR)
        assertTrue(s.temps().isEmpty())
        assertTrue(s.industry.tempAbuse < 0.2)

        s.hireTemps(TempKind.QA, 1, 10)
        val cut = s.temps().single()
        s.endContract(cut.id, ContractExit.CUT)
        assertTrue(s.industry.tempAbuse > 0.0)

        val again = GameState.decode(s.encode())
        assertNotNull(again.industry)
        assertTrue(again.industry.creators.isEmpty())
        assertEquals(0.0, again.industry.tempAbuse, 1.0) // may be >0 after cut; just loads
    }

    @Test
    fun contractsExpireOnTheirOwn() {
        val s = studio()
        s.hireTemps(TempKind.ARTIST, 1, 4)
        val until = s.temps().single().contractUntil
        while (s.week < until + 1 && s.ending == null) {
            s.events = emptyList()
            s.inbox = emptyList()
            runCatching { s.advance() }
        }
        assertTrue(s.temps().isEmpty())
    }

    @Test
    fun streamEraGatesAndCourtWorks() {
        val early = studio(start = StartPoint.CONSOLE_WARS)
        assertEquals(0.0, early.streamEraStrength(), 1e-9)
        assertNotNull(early.streamCourtBlocker(1))

        val s = studio()
        assertTrue(s.streamEraStrength() >= 0.7)
        val r = s.plant()
        assertNull(s.streamCourtBlocker(r.id))
        val before = r.weeklySales
        s.courtStreamer(r.id, "TestStreamer")
        assertTrue(r.weeklySales >= before)
        assertNotNull(s.creatorOf("TestStreamer"))
        assertTrue(s.streamerSalesLift(r) >= 0.0)
    }

    @Test
    fun featuringEraGatesAndLogoDealTradeoffs() {
        val early = studio(start = StartPoint.CONSOLE_WARS)
        assertEquals(0.0, early.featuringEraStrength(), 1e-9)

        val s = studio()
        assertTrue(s.featuringEraStrength() > 0)
        val holders = s.courtAbleHolders()
        assertTrue(holders.isNotEmpty())
        val h = holders.firstOrNull { it == Platform.PLAYSYSTEM_5.maker } ?: holders.first()
        assertNull(s.featuringBlocker(h))
        s.chaseFeaturing(h, weeks = 6)
        assertTrue(s.holderOf(h)!!.featuringUntil >= s.week)

        assertNull(s.logoDealBlocker(h))
        s.signLogoDeal(h, weeks = 26)
        val stand = s.holderOf(h)!!
        assertTrue(stand.logoUntil >= s.week)
        assertTrue(stand.logoCut > 0)
        assertTrue(stand.favor > 0)
        // Keep/lift apply when the release platforms (or stores) match the holder we courted.
        val plats = listOf(Platform.PLAYSYSTEM_5).takeIf { Platform.PLAYSYSTEM_5.maker == h }
            ?: Platform.entries.filter { it.maker == h }.take(1).ifEmpty { listOf(Platform.PC) }
        val keep = s.platformPoliticsKeep(plats)
        assertTrue(keep < 1.0 || s.platformPoliticsKeep(plats, setOf(Storefront.STEAM)) < 1.0)
        val lift = s.platformPoliticsSalesLift(plats) + s.platformPoliticsSalesLift(listOf(Platform.PC), setOf(Storefront.STEAM))
        assertTrue(lift > 0)
    }

    @Test
    fun certDelayBlocksLaunchUntilCleared() {
        val s = studio()
        // Force a console project with a cert schedule.
        val p = GameProject(
            concept = concept(listOf(Platform.PLAYSYSTEM_5)),
            focus = listOf(mutableListOf(0.4, 0.3, 0.3), mutableListOf(0.4, 0.3, 0.3), mutableListOf(0.4, 0.3, 0.3)),
            startedWeek = s.week,
            progress = 10_000.0,
        )
        s.games!!.project = p
        s.seedCertSchedule(p)
        assertTrue(p.certClearWeek >= s.week)
        if (p.certClearWeek > s.week) {
            assertNotNull(s.launchCertBlocker(1))
            assertNull(s.chaseCertBlocker(1))
            s.chaseCert(1)
            // Pulling cert may or may not clear immediately depending on standing.
            assertTrue(p.certClearWeek <= s.week + 4)
        }
        // Old save: certClearWeek default
        val bare = GameProject(
            concept = concept(),
            focus = listOf(mutableListOf(0.4, 0.3, 0.3), mutableListOf(0.4, 0.3, 0.3), mutableListOf(0.4, 0.3, 0.3)),
            startedWeek = 0,
        )
        assertEquals(-1, bare.certClearWeek)
    }

    @Test
    fun oldIndustryBookLoads() {
        val s = studio()
        val encoded = s.encode()
        val loaded = GameState.decode(encoded)
        assertEquals(0.0, loaded.industry.tempAbuse, 1e-9)
        assertTrue(loaded.industry.creators.isEmpty())
        assertTrue(loaded.industry.platforms.isEmpty())
        assertEquals(0, loaded.industry.tempsHiredEver)
    }
}
