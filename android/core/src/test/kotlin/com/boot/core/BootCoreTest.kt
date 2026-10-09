package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun newGame(lane: Lane, seed: Long) = GameState.create("T", "F", lane, seed)

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
        val s = newGame(Lane.WEBSITES, 3)
        s.enqueue(EventKind.BurnoutWarning)
        val e = assertFailsWith<GameException> { s.advance() }
        assertEquals(GameException.Kind.DECISION_PENDING, e.kind)
        s.choose(1)
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
        copy.startGame(GameScope.SMALL)
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
    fun crunchingOnEmptyBurnsOut() {
        val s = newGame(Lane.GAMES, 5)
        s.founder.energy = 2.0
        s.setCrunch(true)
        s.advance()
        assertEquals(EndingKind.BURNOUT, s.ending?.kind)
    }
}

class LaneTest {
    @Test
    fun gameReadinessAndLaunch() {
        val s = newGame(Lane.GAMES, 11)
        s.startGame(GameScope.SMALL)
        assertFailsWith<GameException> { s.launchGame() }
        while (s.games?.project?.isReady != true) {
            s.advance()
            while (s.needsDecision) s.choose(0)
        }
        s.launchGame()
        assertNull(s.games?.project)
        assertEquals(1, s.games?.releases?.size)
        assertEquals(1, s.stats.releases)
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
