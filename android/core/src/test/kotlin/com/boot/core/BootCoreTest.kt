package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun newGame(lane: Lane, seed: Long) = GameState.create("T", "F", lane, seed)

private fun smallGame(s: GameState, topic: Topic = Topic.FANTASY, genre: Genre = Genre.RPG) =
    GameConcept("Test Quest", topic, genre, platforms = listOf(Platform.PC), size = GameSize.SMALL)

/** Make a game start to finish with the given focus, returning its average score. */
private fun makeGame(s: GameState, concept: GameConcept, focus: List<MutableList<Double>>, polishWeeks: Int = 4): Double {
    s.startGame(concept, focus)
    while (s.games!!.project?.isReady != true) {
        s.advance()
        while (s.needsDecision) s.choose(s.events[0].choices.size - 1)
    }
    repeat(polishWeeks) {
        s.advance()
        while (s.needsDecision) s.choose(s.events[0].choices.size - 1)
    }
    s.launchGame()
    return s.games!!.releases[0].score
}

class SetupTest {
    @Test
    fun cheapLanesStartSoloAndUnfunded() {
        for (lane in listOf(Lane.GAMES, Lane.WEBSITES, Lane.APPS, Lane.SOCIAL)) {
            val s = newGame(lane, 1)
            assertEquals(listOf(lane), s.lanes)
            assertEquals(GameState.STARTING_CASH, s.cash)
            assertTrue(s.staff.isEmpty())
            assertNull(s.venture.board, "$lane should not start with a board")
            assertEquals("Cramped apartment", s.headquarters.name)
        }
    }

    @Test
    fun capitalIntensiveLanesStartWithVentureAndBoard() {
        for (lane in listOf(Lane.HARDWARE, Lane.AI)) {
            val s = newGame(lane, 1)
            assertNotNull(s.venture.board, "$lane is hard mode: it starts with a board")
            assertTrue(s.cash > 1_000_000)
            assertTrue(s.venture.founderEquity < 1)
        }
    }

    @Test
    fun everyPairOfLanesHasExactlyOneCombo() {
        assertEquals(15, Combo.entries.size)
        for (a in Lane.entries) {
            for (b in Lane.entries) {
                if (a >= b) continue
                val matches = Combo.entries.filter { setOf(it.first, it.second) == setOf(a, b) }
                assertEquals(1, matches.size, "$a+$b")
                assertEquals(Combo.between(a, b), Combo.between(b, a))
            }
        }
    }
}

class SimulationTest {
    @Test
    fun sameSeedSameGame() {
        val a = newGame(Lane.GAMES, 42)
        val b = newGame(Lane.GAMES, 42)
        Autopilot().playToEnd(a)
        Autopilot().playToEnd(b)
        assertEquals(a.week, b.week)
        assertEquals(a.cash, b.cash)
        assertEquals(a.ending?.kind, b.ending?.kind)
    }

    @Test
    fun autopilotFinishesEveryLaneWithSaneNumbers() {
        for (lane in Lane.entries) {
            for (seed in 1L..4L) {
                val s = newGame(lane, seed)
                Autopilot().playToEnd(s)
                assertNotNull(s.ending, "$lane/$seed never ended")
                assertTrue(s.week <= GameState.LENGTH_WEEKS)
                assertTrue(s.cash.isFinite())
                assertTrue(s.valuation.isFinite())
                assertTrue(s.reputation.score in 0.0..100.0)
                assertFalse(s.ending!!.story.isEmpty())
            }
        }
    }

    @Test
    fun advanceBlocksOnDecisions() {
        val s = newGame(Lane.GAMES, 3)
        s.enqueue(EventKind.Convention)
        val e = assertFailsWith<GameException> { s.advance() }
        assertEquals(GameException.Kind.DECISION_PENDING, e.kind)
        s.choose(2)
        s.advance()
    }

    @Test
    fun saveAndLoadMidGameContinuesIdentically() {
        val s = newGame(Lane.APPS, 9)
        val pilot = Autopilot()
        repeat(120) { pilot.playWeek(s) }
        val restored = GameState.decode(s.encode())
        repeat(60) {
            pilot.playWeek(s)
            pilot.playWeek(restored)
        }
        assertEquals(s.week, restored.week)
        assertEquals(s.cash, restored.cash, 0.001)
        assertEquals(s.lanes, restored.lanes)
        assertEquals(s.encode(), restored.encode())
    }

    @Test
    fun copyIsIndependent() {
        val s = newGame(Lane.GAMES, 12)
        val copy = s.copy()
        copy.startGame(smallGame(copy))
        copy.advance()
        assertNull(s.games?.project)
        assertEquals(0, s.week)
        assertEquals(1, copy.week)
    }

    @Test
    fun bankruptAfterEightWeeksInTheRed() {
        val s = newGame(Lane.WEBSITES, 5)
        s.cash = -1_000_000.0
        repeat(8) { if (!s.isOver) s.advance() }
        assertEquals(EndingKind.BANKRUPT, s.ending?.kind)
    }

    @Test
    fun savesFromBeforeEnergyWasRemovedStillLoad() {
        val s = newGame(Lane.GAMES, 6)
        repeat(3) { s.advance() }
        val old = s.encode()
            .replaceFirst("\"founder\":{", "\"founder\":{\"energy\":42.0,\"vacationWeeksLeft\":1,")
            .replaceFirst("\"stats\":{", "\"stats\":{\"vacations\":2,\"warnedBurnout\":true,")
        assertTrue("energy" in old && "warnedBurnout" in old)
        val loaded = GameState.decode(old)
        assertEquals(s.week, loaded.week)
        assertEquals(s.founder.name, loaded.founder.name)
    }

    @Test
    fun crunchNeedsATeamAndOnlyStretchesIt() {
        val s = newGame(Lane.GAMES, 5)
        assertFailsWith<GameException> { s.setCrunch(true) }
        s.hireDirect(s.makeCandidate(Role.ENGINEER), Lane.GAMES)
        val normal = s.context(Lane.GAMES).workOutput
        s.setCrunch(true)
        val crunching = s.context(Lane.GAMES).workOutput
        val team = s.staffing(Lane.GAMES).teamOutput
        assertTrue(team > 0)
        assertEquals(normal + 0.4 * team * s.context(Lane.GAMES).mods.output, crunching, 1e-9)
    }
}

class LaneTest {
    @Test
    fun gameReadinessAndLaunch() {
        val s = newGame(Lane.GAMES, 11)
        s.startGame(smallGame(s))
        assertFailsWith<GameException> { s.launchGame() }
        while (s.games?.project?.isReady != true) {
            s.advance()
            while (s.needsDecision) s.choose(0)
        }
        val p = s.games!!.project!!
        assertTrue(p.design > 0 && p.tech > 0 && p.bugs > 0)
        s.launchGame()
        val g = s.games!!
        assertNull(g.project)
        assertEquals(1, g.releases.size)
        assertEquals(1, s.stats.releases)
        val r = g.releases[0]
        assertEquals(Outlet.entries.count { MODERN_YEAR in it.years }, r.reviews.size, "six outlets review a PC game")
        assertTrue(r.reviews.all { it.score in 1..10 && it.comment.isNotBlank() })
        assertTrue(r.report.isNotEmpty())
        assertEquals(r.id, g.unseenReview)
        // The game report teaches you the combo you used.
        assertEquals(r.concept.topic.fit(r.concept.genre), g.knownFit(r.concept.topic, r.concept.genre))
    }

    @Test
    fun acceptingAContractPaysUpfront() {
        val s = newGame(Lane.WEBSITES, 2)
        while (s.web!!.offers.isEmpty()) s.advance()
        val offer = s.web!!.offers[0]
        val before = s.cash
        s.acceptContract(offer.id)
        assertEquals(before + offer.value * 0.4, s.cash, 0.01)
        assertEquals(1, s.web?.active?.size)
    }

    @Test
    fun hardwareStockArrivesAfterLeadTime() {
        val s = newGame(Lane.HARDWARE, 4)
        s.startDevice(DeviceCategory.HEADPHONES, DeviceTier.BUDGET)
        s.orderUnits(5_000)
        assertEquals(1, s.hardware?.orders?.size)
        repeat(HardwareLab.LEAD_TIME_WEEKS) {
            s.advance()
            while (s.needsDecision) s.choose(1)
        }
        val stocked = (s.hardware?.design?.preorderedStock ?: 0) + (s.hardware?.inventory ?: 0)
        assertEquals(5_000, stocked)
    }

    @Test
    fun orderYouCannotAffordChangesNothing() {
        val s = newGame(Lane.HARDWARE, 4)
        s.startDevice(DeviceCategory.LAPTOP, DeviceTier.FLAGSHIP)
        val cash = s.cash
        assertFailsWith<GameException> { s.orderUnits(10_000_000) }
        assertEquals(cash, s.cash)
        assertTrue(s.hardware!!.orders.isEmpty())
    }

    @Test
    fun trainingRunEventuallyFinishes() {
        val s = newGame(Lane.AI, 8)
        s.startTraining(TrainingSize.SMALL)
        repeat(TrainingSize.SMALL.weeks) {
            s.advance()
            while (s.needsDecision) s.choose(0)
        }
        assertNull(s.ai?.training)
        assertTrue((s.ai?.capability ?: 0.0) > 0 || s.ai?.failedRuns == 1)
    }
}

class PivotTest {
    /** Advance until the Pivot Point arrives, settling any other decisions along the way. */
    private fun reachPivot(lane: Lane, seed: Long): GameState {
        val s = newGame(lane, seed)
        s.cash = 5_000_000.0
        while (s.pendingPivot == null && !s.isOver) {
            if (s.events.isNotEmpty()) {
                s.choose(s.events[0].choices.size - 1)
                continue
            }
            s.advance()
        }
        assertNotNull(s.pendingPivot)
        assertTrue(s.week in PIVOT_WINDOW)
        return s
    }

    @Test
    fun pivotSellsTheFirstBusiness() {
        val s = reachPivot(Lane.WEBSITES, 21)
        val sale = s.pendingPivot!!.salePrice
        val cash = s.cash
        s.resolvePivot(Lane.GAMES, PivotMode.PIVOT)
        assertEquals(listOf(Lane.GAMES), s.lanes)
        assertNull(s.web)
        assertEquals(cash + sale, s.cash, 0.01)
        assertEquals(PivotMode.PIVOT, s.pivot?.mode)
    }

    @Test
    fun expandRunsBothLanesAndUnlocksTheirCombo() {
        val s = reachPivot(Lane.GAMES, 22)
        s.resolvePivot(Lane.SOCIAL, PivotMode.EXPAND)
        assertEquals(setOf(Lane.GAMES, Lane.SOCIAL), s.lanes.toSet())
        assertNotNull(s.crossover(Combo.GAMES_SOCIAL))
        assertNull(s.pendingPivot)
    }

    @Test
    fun capitalIntensiveLaneNeedsMoneyOrVenture() {
        val s = reachPivot(Lane.WEBSITES, 23)
        s.cash = 10_000.0
        assertNotNull(s.pivotBlocker(Lane.AI, PivotMode.EXPAND, fundWithVenture = false))
    }

    @Test
    fun expansionWaitsForThePivotAndProfit() {
        val s = newGame(Lane.GAMES, 24)
        assertNotNull(s.expansionBlocker)
        assertFailsWith<GameException> { s.expand(Lane.APPS) }
        s.pivot = PivotRecord(PivotTrigger.VIRAL_ACCIDENT, 0, Lane.GAMES, Lane.APPS, PivotMode.EXPAND)
        s.stats.profitableStreak = EXPANSION_STREAK_NEEDED
        assertNull(s.expansionBlocker)
    }

    @Test
    fun ecosystemNeedsHardwareSoftwareAndServices() {
        val s = newGame(Lane.HARDWARE, 25)
        assertFalse(s.isEcosystem)
        s.openLane(Lane.APPS)
        assertFalse(s.isEcosystem)
        s.openLane(Lane.AI)
        assertTrue(s.isEcosystem)
        assertEquals(3, s.crossovers.size)
    }
}

class ReputationTest {
    @Test
    fun predatoryScarFollowsYouIntoSocial() {
        val r = Reputation()
        val before = r.launchReception(Lane.SOCIAL)
        r.scar(ScarKind.PREDATORY_MONETIZATION, 0.6, 0, "")
        assertTrue(r.launchReception(Lane.SOCIAL) < before * 0.8)
        assertTrue(r.score < 50)
    }

    @Test
    fun scarsHeal() {
        val r = Reputation()
        r.scar(ScarKind.SCANDAL, 0.3, 0, "")
        repeat(200) { r.weeklyDrift() }
        assertTrue(r.scars.isEmpty())
    }

    @Test
    fun ridingOutMonetizationBacklashLeavesAPredatoryScar() {
        val s = newGame(Lane.APPS, 30)
        s.openLane(Lane.GAMES)
        s.enqueue(EventKind.ComboProblem(Combo.APPS_GAMES))
        s.choose(1)
        assertTrue(s.reputation.has(ScarKind.PREDATORY_MONETIZATION))
    }
}

class StudioTest {
    private fun ideal(c: GameConcept) = List(3) { st -> FocusArea.forStage(st).map { c.idealShare(it) }.toMutableList() }
    private fun worst(c: GameConcept) = List(3) { st ->
        val areas = FocusArea.forStage(st)
        val low = areas.minBy { c.idealShare(it) }
        areas.map { if (it == low) 1.0 else 0.0 }.toMutableList()
    }

    @Test
    fun greatCombosAndRightFocusReviewBetter() {
        var good = 0.0
        var bad = 0.0
        for (seed in 1L..6L) {
            val a = newGame(Lane.GAMES, seed)
            a.cash = 1_000_000.0
            val greatConcept = GameConcept("A", Topic.FANTASY, Genre.RPG, platforms = listOf(Platform.PC), size = GameSize.SMALL)
            good += makeGame(a, greatConcept, ideal(greatConcept))
            val b = newGame(Lane.GAMES, seed)
            b.cash = 1_000_000.0
            val badConcept = GameConcept("B", Topic.FANTASY, Genre.SIMULATION, platforms = listOf(Platform.PC), size = GameSize.SMALL)
            bad += makeGame(b, badConcept, worst(badConcept))
        }
        assertTrue(good / 6 > bad / 6 + 1.5, "great combo ${good / 6} vs bad ${bad / 6}")
    }

    @Test
    fun shippingBuggyGamesHurts() {
        var polished = 0.0
        var rushed = 0.0
        for (seed in 1L..6L) {
            val c = GameConcept("C", Topic.SPACE, Genre.STRATEGY, platforms = listOf(Platform.PC), size = GameSize.SMALL)
            val a = newGame(Lane.GAMES, seed).also { it.cash = 1_000_000.0 }
            polished += makeGame(a, c, ideal(c), polishWeeks = 6)
            val b = newGame(Lane.GAMES, seed).also { it.cash = 1_000_000.0 }
            rushed += makeGame(b, c, ideal(c), polishWeeks = 0)
        }
        assertTrue(polished > rushed, "polished $polished vs rushed $rushed")
    }

    @Test
    fun researchSpendsPointsAndUnlocks() {
        val s = newGame(Lane.GAMES, 3)
        assertNotNull(s.researchBlocker(Research.MEDIUM_GAMES))
        assertFailsWith<GameException> { s.startGame(smallGame(s).copy(size = GameSize.MEDIUM)) }
        s.researchPoints = 25.0
        s.research(Research.MEDIUM_GAMES)
        assertEquals(5.0, s.researchPoints, 0.001)
        assertTrue(GameSize.MEDIUM in s.unlockedSizes)
        assertNotNull(s.researchBlocker(Research.LARGE_GAMES))
    }

    @Test
    fun researchPointsAccrueWhileYouWork() {
        val s = newGame(Lane.GAMES, 4)
        s.startGame(smallGame(s))
        repeat(10) {
            s.advance()
            while (s.needsDecision) s.choose(s.events[0].choices.size - 1)
        }
        assertTrue(s.researchPoints > 5, "rp ${s.researchPoints}")
    }

    @Test
    fun topicsMustBeResearched() {
        val s = newGame(Lane.GAMES, 5)
        assertNotNull(s.conceptBlocker(smallGame(s, Topic.ZOMBIES, Genre.ACTION)))
        s.researchPoints = 50.0
        s.researchTopic(Topic.ZOMBIES)
        assertNull(s.conceptBlocker(smallGame(s, Topic.ZOMBIES, Genre.ACTION)))
    }

    @Test
    fun customEnginesNeedResearchAndRaiseTheLevel() {
        val s = newGame(Lane.GAMES, 6)
        s.cash = 5_000_000.0
        assertFailsWith<GameException> { s.buildEngine() }
        s.researched = setOf(Research.CUSTOM_ENGINES, Research.GRAPHICS_2D_V2, Research.SOUND_STEREO, Research.BASIC_AI)
        s.buildEngine()
        repeat(10) { if (!s.isOver) { s.advance(); while (s.needsDecision) s.choose(s.events[0].choices.size - 1) } }
        assertEquals(3, s.games!!.engines.maxOf { it.level })
    }

    @Test
    fun platformsComeAndGo() {
        assertTrue(Platform.PLAYSYSTEM_5.isAvailable(0))
        assertFalse(Platform.PLAYSYSTEM_5.isAvailable(400))
        assertFalse(Platform.PLAYSYSTEM_6.isAvailable(0))
        assertTrue(Platform.PLAYSYSTEM_6.isAvailable(400))
        assertTrue(Platform.PC.isAvailable(700))
    }

    @Test
    fun officesLimitHiring() {
        val s = newGame(Lane.GAMES, 7)
        s.cash = 1_000_000.0
        repeat(Office.APARTMENT.desks) { s.hire(s.candidates.first().id, Lane.GAMES) }
        assertFailsWith<GameException> { s.hire(s.candidates.first().id, Lane.GAMES) }
        s.moveOffice()
        assertEquals(Office.COWORKING, s.office)
        s.hire(s.candidates.first().id, Lane.GAMES)
    }

    @Test
    fun publisherDealsPayUpFront() {
        val s = newGame(Lane.GAMES, 8)
        s.cash = 1_000_000.0
        val g = s.games!!
        val deal = PublisherDeal(99, "Megapublish", Topic.SPACE, Genre.STRATEGY, Platform.PC, GameSize.SMALL, 5, 40_000.0, 0.35, 999)
        g.dealOffers += deal
        val before = s.cash
        s.acceptDeal(deal.id)
        assertEquals(before + 20_000.0, s.cash, 0.01)
        assertEquals(deal, g.project?.deal)
    }

    @Test
    fun contractWorkPays() {
        val s = newGame(Lane.GAMES, 9)
        val g = s.games!!
        g.contractOffers += GameContract(77, "Test job", work = 10.0, pay = 9_000.0, weeksAllowed = 20, rp = 2.0, expiresWeek = 99)
        s.takeGameContract(77)
        var paid = false
        repeat(12) {
            if (g.contract == null) return@repeat
            s.advance()
            while (s.needsDecision) s.choose(s.events[0].choices.size - 1)
        }
        paid = g.contract == null && s.log.any { it.text.startsWith("Contract delivered") }
        assertTrue(paid)
    }

    @Test
    fun conventionComesOncAYear() {
        val s = newGame(Lane.GAMES, 10)
        s.cash = 1_000_000.0
        var seen = 0
        while (s.week < 60 && !s.isOver) {
            s.advance()
            while (s.needsDecision) {
                if (s.events.firstOrNull()?.kind == EventKind.Convention) seen++
                s.choose(0)
            }
        }
        assertEquals(1, seen)
    }

    @Test
    fun trainingRaisesSkill() {
        val s = newGame(Lane.GAMES, 12)
        s.cash = 1_000_000.0
        s.hire(s.candidates.first().id, Lane.GAMES)
        val e = s.staff.first()
        val skill = e.skill
        assertNotNull(s.trainingBlocker(e))
        s.researched = setOf(Research.TRAINING)
        s.researchPoints = 100.0
        if (skill < 10) {
            s.train(e.id)
            assertEquals(skill + 1, s.staff.first().skill)
        }
    }
}
