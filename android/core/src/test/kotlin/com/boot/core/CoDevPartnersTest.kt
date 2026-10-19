package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Co-dev partners: propose / accept, cost & work split, glory / blame, save defaults. */
class CoDevPartnersTest {
    private fun studio(seed: Long = 42) =
        GameState.create("CoDev Co", "You", Lane.GAMES, seed, start = StartPoint.TODAY).apply {
            cash = 200_000_000.0
            researched += Research.LARGE_GAMES
            researched += Research.AAA_GAMES
            researched += Research.MEDIUM_GAMES
            while (freeDesks < 6 && office.next != null) moveOffice()
            refreshTalentPool()
            var guard = 0
            while (gameTeam < 4 && candidates.isNotEmpty() && guard++ < 20) {
                runCatching { hire(candidates.first().id, Lane.GAMES) }
                if (candidates.isEmpty()) refreshTalentPool()
            }
        }

    private fun GameState.plantLarge(team: Int = 1): GameProject {
        val focus = listOf(
            mutableListOf(0.4, 0.3, 0.3),
            mutableListOf(0.4, 0.3, 0.3),
            mutableListOf(0.4, 0.3, 0.3),
        )
        val concept = GameConcept(
            "Horizon Split", Topic.FANTASY, Genre.ACTION,
            platforms = listOf(Platform.PC), size = GameSize.LARGE,
            engineId = games!!.engines.first().id,
        )
        games!!.topics += Topic.FANTASY
        if (Genre.ACTION !in games!!.genres) games!!.genres += Genre.ACTION
        startGame(concept, focus, team = team)
        return games!!.projectOf(team)!!
    }

    @Test
    fun proposeAndAttachCoDevSplitsCostAndWork() {
        val s = studio()
        val p = s.plantLarge()
        assertTrue(p.isCoDevEligible())
        val cand = s.coDevCandidates(1).firstOrNull { it.kind == CoDevKind.MARKET || it.kind == CoDevKind.RIVAL }
        assertNotNull(cand)
        assertNull(s.proposeCoDevBlocker(1, cand.name))
        s.proposeCoDev(1, cand.name, 0.4, 0.35, 0.4, 0.3)
        assertTrue(s.games!!.coDevOffers.any { it.partner == cand.name })

        // Force accept path.
        val offer = s.games!!.coDevOffers.single()
        s.games!!.coDevOffers.clear()
        s.attachCoDev(1, offer.let {
            CoDevTerms(it.partner, it.kind, it.costShare, it.workShare, it.creditShare, it.royaltyShare)
        })
        val cd = p.activeCoDev()
        assertNotNull(cd)
        assertEquals(cand.name, cd.partner)
        assertEquals(0.4, cd.costShare, 1e-9)
        assertTrue(s.coDevFavor(cand.name) > 0)

        val beforeSpent = p.spent
        val beforeProgress = p.progress
        // One develop tick via advance (clear blockers).
        s.events = emptyList()
        s.inbox = emptyList()
        runCatching { s.advance() }
        assertTrue(p.weeksInDev >= 1)
        // Partner should have covered some cost and contributed work.
        assertTrue(cd.costPaid > 0.0 || p.spent > beforeSpent)
        assertTrue(cd.contribution > 0.0 || p.progress > beforeProgress)
        assertTrue(p.coDevBudgetFactor() < 1.0)
        assertTrue(p.coDevRoyaltyKeep() < 1.0)
    }

    @Test
    fun smallGamesCannotCoDev() {
        val s = studio()
        val focus = listOf(
            mutableListOf(0.4, 0.3, 0.3),
            mutableListOf(0.4, 0.3, 0.3),
            mutableListOf(0.4, 0.3, 0.3),
        )
        val concept = GameConcept(
            "Tiny Jam", Topic.FANTASY, Genre.ACTION,
            platforms = listOf(Platform.PC), size = GameSize.SMALL,
            engineId = s.games!!.engines.first().id,
        )
        s.games!!.topics += Topic.FANTASY
        s.startGame(concept, focus)
        assertTrue(s.coDevCandidates(1).isEmpty())
        assertNotNull(s.proposeCoDevBlocker(1, "Valve"))
    }

    @Test
    fun sharedGloryAndBlameOnLaunch() {
        val s = studio()
        val p = s.plantLarge()
        p.coDev = CoDevTerms("Valve", CoDevKind.MARKET, 0.4, 0.35, 0.4, 0.3)
        // Hit path via coDevOnLaunch directly.
        val hit = GameRelease(
            id = 1, title = p.title, concept = p.concept, week = s.week, reviews = emptyList(),
            score = 8.5, weeklySales = 100_000.0, netShare = 0.7, design = p.design, tech = p.tech,
            bugs = 0.0, hype = 20.0, devCost = 1.0, report = emptyList(),
        )
        val favorBefore = s.coDevFavor("Valve")
        s.coDevOnLaunch(p, hit)
        assertEquals("Valve", hit.coDevPartner)
        assertEquals(0.3, hit.coDevRoyalty, 1e-9)
        assertTrue(s.coDev.hits >= 1)
        assertTrue(s.coDevFavor("Valve") > favorBefore)
        assertEquals(CoDevPhase.DONE, p.coDev!!.phase)

        val flopConcept = GameConcept(
            "Mudslide", Topic.FANTASY, Genre.ACTION,
            platforms = listOf(Platform.PC), size = GameSize.LARGE,
            engineId = s.games!!.engines.first().id,
        )
        val flopProject = GameProject(
            flopConcept,
            listOf(mutableListOf(0.4, 0.3, 0.3), mutableListOf(0.4, 0.3, 0.3), mutableListOf(0.4, 0.3, 0.3)),
            s.week,
            coDev = CoDevTerms("Epic Games", CoDevKind.MARKET, 0.4, 0.35, 0.4, 0.3),
        )
        val flop = GameRelease(
            id = 2, title = flopProject.title, concept = flopConcept, week = s.week,
            reviews = emptyList(), score = 4.0, weeklySales = 1_000.0, netShare = 0.7,
            design = 1.0, tech = 1.0, bugs = 10.0, hype = 0.0, devCost = 1.0, report = emptyList(),
        )
        s.coDevOnLaunch(flopProject, flop)
        assertTrue(s.coDev.flops >= 1)
        assertTrue(s.coDevFavor("Epic Games") < 0.1)
    }

    @Test
    fun disputeQuitAndSmooth() {
        val s = studio()
        val p = s.plantLarge()
        p.coDev = CoDevTerms("Ubisoft", CoDevKind.RIVAL, 0.4, 0.35, 0.4, 0.3)
        s.resolveCoDevDispute(CoDevDispute(1, CoDevDisputeReason.DELAY), choice = 1)
        assertEquals(CoDevPhase.QUIT, p.coDev!!.phase)
        assertTrue(s.coDev.disputes >= 1)

        // Smooth on the same project after re-partnering.
        p.coDev = CoDevTerms("Capcom", CoDevKind.MARKET, 0.4, 0.35, 0.4, 0.3)
        val favor = s.coDevFavor("Capcom")
        s.resolveCoDevDispute(CoDevDispute(1, CoDevDisputeReason.SCOPE), choice = 0)
        assertEquals(CoDevPhase.ACTIVE, p.coDev!!.phase)
        assertTrue(s.coDevFavor("Capcom") >= favor)
    }

    @Test
    fun oldSavesLoadCoDevDefaults() {
        val s = studio()
        val encoded = s.encode()
        val loaded = GameState.decode(encoded)
        assertNotNull(loaded.coDev)
        assertTrue(loaded.coDev.standing.isEmpty())
        assertEquals(0, loaded.coDev.ships)
        assertTrue(loaded.games!!.coDevOffers.isEmpty())
        val bare = GameProject(
            concept = GameConcept("X", Topic.FANTASY, Genre.ACTION, platforms = listOf(Platform.PC), size = GameSize.MEDIUM),
            focus = listOf(mutableListOf(0.4, 0.3, 0.3), mutableListOf(0.4, 0.3, 0.3), mutableListOf(0.4, 0.3, 0.3)),
            startedWeek = 0,
        )
        assertNull(bare.coDev)
    }

    @Test
    fun incomingOfferAcceptDecline() {
        val s = studio()
        s.plantLarge()
        val id = s.coDev.nextOfferId++
        s.games!!.coDevOffers += CoDevOffer(
            id = id, partner = "Larian Studios", kind = CoDevKind.MARKET, team = 1,
            costShare = 0.35, workShare = 0.35, creditShare = 0.4, royaltyShare = 0.35,
            expiresWeek = s.week + 3, incoming = true,
        )
        assertNull(s.acceptCoDevOfferBlocker(id))
        s.acceptCoDevOffer(id)
        assertNotNull(s.games!!.project!!.activeCoDev())
        assertEquals("Larian Studios", s.games!!.project!!.coDev!!.partner)
    }
}
