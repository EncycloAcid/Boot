package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun game(lane: Lane, seed: Long = 3) = GameState.create("T", "F", lane, seed)

/** Advance, answering every blocking decision with its last option. */
private fun GameState.run(weeks: Int) {
    repeat(weeks) {
        if (isOver) return
        while (needsDecision) {
            if (pendingPivot != null) stayFocused() else choose(events[0].choices.size - 1)
        }
        advance()
    }
}

class RivalsTest {
    @Test
    fun pressuresSqueezeTheLane() {
        val s = game(Lane.WEBSITES)
        val before = s.context(Lane.WEBSITES).mods.demand
        s.world.pressures += Pressure(Lane.WEBSITES, "Price war", 0.8, 5, rival = "Squarespace")
        assertEquals(before * 0.8, s.context(Lane.WEBSITES).mods.demand, 1e-9)
        assertEquals(1, s.pressuresOn(Lane.WEBSITES).size)
        // Other lanes don't feel it.
        assertTrue(s.pressuresOn(Lane.GAMES).isEmpty())
    }

    @Test
    fun pressuresRunOut() {
        val s = game(Lane.WEBSITES)
        s.world.pressures += Pressure(Lane.WEBSITES, "Ad blitz", 0.9, 2)
        s.run(3)
        assertTrue(s.world.pressures.none { it.title == "Ad blitz" })
    }

    @Test
    fun rivalsWarmUpAsYouEarn() {
        val s = game(Lane.WEBSITES)
        val r = s.world.rival(Lane.WEBSITES)!!
        assertEquals(RivalStance.CALM, r.stance)
        r.heat = 0.7
        assertEquals(RivalStance.HOSTILE, r.stance)
    }

    @Test
    fun aRivalMoveIsADecisionWithConsequences() {
        val s = game(Lane.WEBSITES)
        s.enqueue(RivalMove("Squarespace", Lane.WEBSITES, RivalMoveKind.PRICE_WAR))
        val e = s.events.single()
        assertEquals(3, e.choices.size)
        s.choose(1) // hold prices
        val p = s.pressuresOn(Lane.WEBSITES).single()
        assertEquals(0.8, p.demand, 1e-9)
        assertFalse(p.mine)
        assertTrue(s.news.any { it.headline.contains("slashes prices") })
    }

    @Test
    fun everyRivalMoveCanBeDescribedAndAnswered() {
        for (move in RivalMoveKind.entries) {
            for (choice in 0..2) {
                val s = game(Lane.GAMES, seed = 9)
                val id = s.makeCandidate().let { s.hireDirect(it.copy(skill = 8), Lane.GAMES).id }
                s.cash = 10_000_000.0
                s.enqueue(RivalMove("Electronic Arts", Lane.GAMES, move, listOf(id)))
                val e = s.events.singleOrNull() ?: error("$move wasn't described")
                if (choice < e.choices.size) s.choose(choice)
            }
        }
    }

    @Test
    fun aLawsuitYouFightIsDecidedLater() {
        val s = game(Lane.HARDWARE)
        s.enqueue(RivalMove("Apple", Lane.HARDWARE, RivalMoveKind.LAWSUIT))
        s.choose(1)
        assertTrue(s.followUps.any { it.consequence is Consequence.Verdict })
        s.run(30)
        assertTrue(s.followUps.none { it.consequence is Consequence.Verdict })
        assertTrue(s.log.any { it.text.contains("lawsuit against Apple") })
    }

    @Test
    fun counterMovesHitTheRivalAndHaveACooldown() {
        val s = game(Lane.WEBSITES)
        s.cash = 1_000_000.0
        val r = s.world.rival(Lane.WEBSITES)!!
        val strength = r.strength
        s.counter(r.name, CounterMove.UNDERCUT)
        assertTrue(r.strength < strength)
        assertTrue(r.heat > 0)
        assertTrue(s.pressuresOn(Lane.WEBSITES).single().mine)
        assertNotNull(s.counterBlocker(r, CounterMove.ATTACK_ADS))
        assertFailsWith<GameException> { s.counter(r.name, CounterMove.ATTACK_ADS) }
        // Not in a lane you don't run.
        assertNotNull(s.counterBlocker(s.world.rival(Lane.AI)!!, CounterMove.UNDERCUT))
    }

    @Test
    fun poachingAStarHiresThem() {
        val s = game(Lane.WEBSITES)
        s.cash = 1_000_000.0
        val r = s.world.rival(Lane.WEBSITES)!!
        s.counter(r.name, CounterMove.POACH_STAR)
        val hired = s.staff.single()
        assertTrue(hired.skill >= 8)
        assertEquals(Lane.WEBSITES, hired.lane)
    }

    @Test
    fun aBeatenRivalRetreats() {
        val s = game(Lane.WEBSITES)
        s.world.rival(Lane.WEBSITES)!!.strength = 0.05
        s.run(1)
        assertEquals(1, s.stats.rivalsBeaten)
        assertTrue(s.world.rival(Lane.WEBSITES)!!.quietUntil > s.week)
    }

    @Test
    fun rivalsMakeMovesOverARun() {
        var moves = 0
        for (seed in 1L..6L) {
            val s = game(Lane.WEBSITES, seed)
            Autopilot().playToEnd(s)
            moves += s.stats.rivalMoves
        }
        assertTrue(moves > 0, "rivals should make moves across six runs")
    }
}

class InboxTest {
    @Test
    fun theInboxWaitsWithoutStoppingTheClock() {
        val s = game(Lane.WEBSITES)
        s.offer(Interview("Wired"), 3)
        assertEquals(1, s.inbox.size)
        assertFalse(s.needsDecision)
        s.run(1)
        assertEquals(1, s.inbox.size)
    }

    @Test
    fun answeringTakesTheChoice() {
        val s = game(Lane.WEBSITES)
        s.offer(AngelCheck("Raj Patel", 200_000.0, 0.05), 4)
        val cash = s.cash
        s.answer(s.inbox.single().id, 0)
        assertTrue(s.inbox.isEmpty())
        assertEquals(cash + 200_000.0, s.cash, 1e-6)
        assertEquals(0.95, s.venture.founderEquity, 1e-9)
        assertEquals(1, s.stats.inboxAnswered)
    }

    @Test
    fun ignoredItemsLapse() {
        val s = game(Lane.WEBSITES)
        s.offer(AngelCheck("Raj Patel", 200_000.0, 0.05), 2)
        s.run(4)
        assertTrue(s.inbox.none { it.kind is AngelCheck })
        assertEquals(1.0, s.venture.founderEquity, 1e-9)
        assertTrue(s.log.any { it.text.startsWith("You let it lapse") })
    }

    @Test
    fun aQuietPatchCanComeBackToBiteYou() {
        val s = game(Lane.APPS)
        s.enqueue(DataBreach(Lane.APPS))
        s.choose(1)
        val f = s.followUps.single()
        assertTrue(f.consequence is Consequence.BreachExposed)
        assertEquals(0.45, f.chance, 1e-9)
    }

    @Test
    fun peopleDramaResolves() {
        val s = game(Lane.WEBSITES)
        val a = s.hireDirect(s.makeCandidate(), Lane.WEBSITES)
        val b = s.hireDirect(s.makeCandidate(), Lane.WEBSITES)
        s.enqueue(Burnout(a.id))
        s.choose(0)
        assertEquals(s.week + 4, a.leaveUntil)
        // On leave means no output from them.
        assertEquals(1, s.staffing(Lane.WEBSITES).headcount)
        s.enqueue(StaffClash(a.id, b.id))
        s.choose(0)
        assertTrue(b.morale < 0.6)
        s.enqueue(RemoteWork)
        s.choose(0)
        assertTrue(s.hybrid)
    }

    @Test
    fun partnershipsPayTwiceAndSlowTheTeam() {
        val s = game(Lane.WEBSITES)
        s.offer(Partnership(Lane.WEBSITES, "Nike", 40_000.0, 6), 4)
        val cash = s.cash
        s.answer(s.inbox.single().id, 0)
        assertEquals(cash + 20_000.0, s.cash, 1e-6)
        assertEquals(0.8, s.context(Lane.WEBSITES).mods.output, 1e-9)
        assertTrue(s.followUps.any { it.consequence is Consequence.PartnerPays })
    }

    @Test
    fun newDecisionsTurnUpOverARun() {
        val s = game(Lane.APPS, seed = 5)
        var seen = 0
        Autopilot().let { a ->
            while (!s.isOver && s.week < 400) {
                seen += s.inbox.size + s.events.count { it.kind is DramaEvent || it.kind is LaneDecision }
                a.playWeek(s)
            }
        }
        assertTrue(seen > 0)
    }

    @Test
    fun inboxAndFollowUpsSurviveASave() {
        val s = game(Lane.WEBSITES)
        s.offer(Interview("Wired"), 3)
        s.later(5, Consequence.ClientPays("Acme", 10_000.0))
        s.world.pressures += Pressure(Lane.WEBSITES, "Price war", 0.9, 4, rival = "Squarespace")
        s.enqueue(RivalMove("Squarespace", Lane.WEBSITES, RivalMoveKind.SMEAR))
        val back = GameState.decode(s.encode())
        assertEquals(s.inbox.single().kind, back.inbox.single().kind)
        assertEquals(s.followUps, back.followUps)
        assertEquals(s.world.pressures, back.world.pressures)
        assertEquals(s.events.single().kind, back.events.single().kind)
    }
}

class DeeperBusinessesTest {
    @Test
    fun retainersPayEveryWeekAndTakeTheTeamsTime() {
        val s = game(Lane.WEBSITES)
        s.offer(RetainerOffer("Kettle Cafe", 500.0, 1.5), 4)
        s.answer(s.inbox.single().id, 0)
        val w = s.web!!
        assertEquals(1, w.retainers.size)
        assertEquals(1.5, w.retainerLoad, 1e-9)
        s.run(1)
        assertTrue(s.ledger.last().revenue[Lane.WEBSITES]!! >= 500.0 * 0.9)
        s.dropRetainer("Kettle Cafe")
        assertTrue(w.retainers.isEmpty())
    }

    @Test
    fun aWonPitchLandsABigProject() {
        val s = game(Lane.WEBSITES)
        s.offer(BigPitch("Nike", 300_000.0, 18_000.0, 1.0), 4)
        s.answer(s.inbox.single().id, 0)
        s.run(4)
        assertTrue(s.web!!.active.any { it.client == "Nike" && it.value == 300_000.0 } || s.log.any { it.text.contains("won the Nike pitch") })
    }

    @Test
    fun appExperimentsReportBackAndCanBeKept() {
        val s = game(Lane.APPS)
        val a = s.apps!!
        a.isLive = true
        a.users = 10_000.0
        s.startExperiment(ExperimentKind.SCREENSHOTS)
        assertNotNull(s.experimentBlocker(ExperimentKind.ONBOARDING)) // one at a time
        s.run(5)
        val result = s.inbox.first { it.kind is ExperimentResult }
        val lift = (result.kind as ExperimentResult).lift
        s.answer(result.id, 0)
        assertEquals(lift, a.installLift, 1e-9)
        assertNotNull(s.experimentBlocker(ExperimentKind.SCREENSHOTS)) // already tried
    }

    @Test
    fun appPricingTradesInstallsForRevenue() {
        val cheap = game(Lane.APPS, seed = 4).apply { apps!!.isLive = true; apps!!.users = 50_000.0; setAppPricing(AppPricing.LOW) }
        val dear = game(Lane.APPS, seed = 4).apply { apps!!.isLive = true; apps!!.users = 50_000.0; setAppPricing(AppPricing.HIGH) }
        cheap.run(1)
        dear.run(1)
        assertTrue(cheap.apps!!.lastInstalls > dear.apps!!.lastInstalls)
    }

    @Test
    fun socialLevers() {
        val s = game(Lane.SOCIAL)
        val p = s.social!!
        s.setCreatorFund(2)
        assertTrue(p.creatorFundCost > 0)
        s.setContentPolicy(ContentPolicy.STRICT)
        s.setPremium(true)
        assertTrue(p.premium)
        assertEquals(ContentPolicy.STRICT, p.policy)
    }

    @Test
    fun hardwarePricingAndKeynotes() {
        val s = game(Lane.HARDWARE)
        s.cash = 5_000_000.0
        assertFailsWith<GameException> { s.setDevicePrice(-0.1) } // nothing on sale
        s.startDevice(DeviceCategory.HEADPHONES, DeviceTier.BUDGET)
        s.bookKeynote(1)
        assertEquals(1, s.hardware!!.keynote)
        assertFailsWith<GameException> { s.bookKeynote(2) }
    }

    @Test
    fun aTrainedModelWaitsForYourCall() {
        for (choice in 0..2) {
            val s = game(Lane.AI, seed = 21)
            s.cash = 50_000_000.0
            s.startTraining(TrainingSize.SMALL)
            var guard = 0
            while (s.events.none { it.kind is ModelReady } && guard++ < 200) {
                if (s.needsDecision) {
                    if (s.pendingPivot != null) s.stayFocused() else s.choose(s.events[0].choices.lastIndex)
                    continue
                }
                if (s.ai!!.training == null && s.ai!!.pendingModel == null) s.startTraining(TrainingSize.SMALL)
                s.advance()
            }
            val lab = s.ai!!
            assertNotNull(lab.pendingModel)
            assertEquals(0.0, lab.capability)
            while (s.events.first().kind !is ModelReady) s.choose(s.events[0].choices.lastIndex)
            s.choose(choice)
            when (choice) {
                0 -> assertTrue(lab.capability > 0)
                1 -> {
                    assertEquals(0.0, lab.capability)
                    s.run(7)
                    assertTrue(lab.capability > 0)
                    assertNull(lab.pendingModel)
                }
                else -> {
                    assertTrue(lab.capability > 0)
                    assertTrue(lab.openWeights)
                }
            }
        }
    }

    @Test
    fun reservedComputeIsCheaperThanRenting() {
        val s = game(Lane.AI)
        s.cash = 50_000_000.0
        val spot = s.trainingWeeklyCost(TrainingSize.MEDIUM)
        s.reserveCompute(4)
        assertTrue(s.trainingWeeklyCost(TrainingSize.MEDIUM) < spot)
        assertFailsWith<GameException> { s.reserveCompute(4) }
    }
}
