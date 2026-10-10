package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun game(lane: Lane, seed: Long, difficulty: Difficulty = Difficulty.NORMAL) = GameState.create("T", "F", lane, seed, difficulty)

/** Advance, answering every decision with its last (usually cheapest) option. */
private fun GameState.play(weeks: Int) {
    repeat(weeks) {
        if (isOver) return
        while (needsDecision) {
            if (pendingPivot != null) return
            choose(events[0].choices.size - 1)
        }
        advance()
    }
    while (needsDecision && pendingPivot == null) choose(events[0].choices.size - 1)
}

/** A finished game to test what happens after launch. */
private fun GameState.fakeRelease(score: Double, bugs: Double = 10.0, units: Double = 5_000.0): GameRelease {
    val g = games!!
    val r = GameRelease(
        id = g.nextId(), title = "Fake Quest", concept = GameConcept("Fake Quest", Topic.FANTASY, Genre.RPG, platforms = listOf(Platform.PC), size = GameSize.SMALL),
        week = week, reviews = emptyList(), score = score, weeklySales = 5_000.0, netShare = 0.7, design = 50.0, tech = 50.0,
        bugs = bugs, hype = 0.0, devCost = 5_000.0, report = emptyList(), unitsSold = units,
    )
    g.releases.add(0, r)
    return r
}

class DifficultyTest {
    @Test
    fun difficultySetsTheStartingConditions() {
        val easy = game(Lane.GAMES, 1, Difficulty.EASY)
        val normal = game(Lane.GAMES, 1)
        val hard = game(Lane.GAMES, 1, Difficulty.HARD)
        assertEquals(60_000.0, easy.cash)
        assertEquals(GameState.STARTING_CASH, normal.cash)
        assertEquals(12_000.0, hard.cash)
        assertTrue(hard.world.rival(Lane.GAMES)!!.strength > easy.world.rival(Lane.GAMES)!!.strength)
        assertTrue(easy.context(Lane.GAMES).mods.demand > hard.context(Lane.GAMES).mods.demand)
        assertEquals(Difficulty.HARD, GameState.decode(hard.encode()).difficulty)
    }

    @Test
    fun hardModeGoesBankruptSooner() {
        val s = game(Lane.WEBSITES, 2, Difficulty.HARD)
        s.cash = -1_000_000.0
        repeat(Difficulty.HARD.redWeeks) { s.play(1) }
        assertEquals(EndingKind.BANKRUPT, s.ending?.kind)
    }
}

class GoalTest {
    @Test
    fun goalsPayOutOnceInOrder() {
        val s = game(Lane.GAMES, 3)
        assertEquals(Goal.G_SHIP, s.currentGoals.first())
        assertTrue(s.currentGoals.none { it.lane != null && it.lane != Lane.GAMES })
        s.hireDirect(s.makeCandidate(Role.DESIGNER), Lane.GAMES)
        val rp = s.researchPoints
        s.play(1)
        assertTrue(Goal.HIRE_FIRST in s.goalsDone)
        assertTrue(s.researchPoints >= rp + Goal.HIRE_FIRST.reward.rp)
        s.play(3)
        assertEquals(1, s.goalsDone.count { it == Goal.HIRE_FIRST })
        assertTrue(s.log.any { "Goal reached: Hire your first employee" in it.text })
        assertEquals(s.goalsDone, GameState.decode(s.encode()).goalsDone)
    }

    @Test
    fun laneGoalsOnlyCountForYourLanes() {
        val s = game(Lane.WEBSITES, 4)
        assertTrue(Goal.G_SHIP !in s.openGoals)
        assertTrue(Goal.W_FIRST in s.openGoals)
        assertEquals(0.0, Goal.W_TEN.progress(s))
    }
}

class MarketTest {
    @Test
    fun rivalsShipGamesAndTheChartRanksEveryone() {
        val s = game(Lane.GAMES, 5)
        s.cash = 1_000_000.0
        s.play(60)
        val m = s.games!!.market
        assertTrue(m.games.isNotEmpty(), "rivals should have released something in a year")
        assertEquals(m.chart.sortedByDescending { it.weeklySales }, m.chart)
        assertTrue(m.chart.size <= 10)

        val r = s.fakeRelease(9.0)
        r.weeklySales = 10_000_000.0
        s.play(1)
        assertEquals(1, r.chartRank)
        assertEquals(r.id, m.chart.first().releaseId)
        assertEquals(1, s.stats.chartToppers)
    }

    @Test
    fun gameOfTheYearIsAwardedEveryDecember() {
        val s = game(Lane.GAMES, 6)
        s.cash = 1_000_000.0
        s.play(AWARDS_WEEK - 2)
        s.fakeRelease(9.6)
        // Week 50: either you're nominated and asked, or the winner is announced.
        repeat(3) {
            s.advance()
            while (s.needsDecision) {
                val awards = s.events[0].kind == EventKind.GameAwards
                s.choose(if (awards) 0 else s.events[0].choices.size - 1)
            }
        }
        val awards = s.games!!.market.awards
        assertEquals(1, awards.size)
        assertEquals(1, awards[0].year)
    }

    @Test
    fun aStrongRivalInYourGenreTakesBuyers() {
        val s = game(Lane.GAMES, 7)
        val g = s.games!!
        val concept = GameConcept("Mine", Topic.FANTASY, Genre.RPG, platforms = listOf(Platform.PC), size = GameSize.SMALL)
        assertNull(g.clashFor(concept, s.week))
        g.market.games += RivalGame(99, "Dragon Thing", "Nebula Forge", Topic.FANTASY, Genre.RPG, GameSize.MEDIUM, 8.8, s.week, 100_000.0)
        assertEquals("Dragon Thing", g.clashFor(concept, s.week)?.title)
        assertNull(g.clashFor(concept.copy(genre = Genre.CASUAL), s.week))
        assertNull(g.clashFor(concept, s.week + 10))
    }
}

class AfterLaunchTest {
    @Test
    fun patchingFixesBugsOnce() {
        val s = game(Lane.GAMES, 8)
        s.cash = 100_000.0
        val r = s.fakeRelease(7.0, bugs = 20.0)
        assertNull(s.patchBlocker(r.id))
        val cash = s.cash
        val sales = r.weeklySales
        s.patchGame(r.id)
        assertEquals(0.0, r.bugs)
        assertTrue(r.patched)
        assertTrue(r.weeklySales > sales)
        assertTrue(s.cash < cash)
        assertNotNull(s.patchBlocker(r.id))
        assertFailsWith<GameException> { s.patchGame(r.id) }
    }

    @Test
    fun expansionsNeedResearchAndBringPlayersBack() {
        val s = game(Lane.GAMES, 9)
        s.cash = 1_000_000.0
        val r = s.fakeRelease(8.5, units = 20_000.0)
        assertEquals("Research expansion packs first.", s.expansionBlocker(r.id))
        s.researched = s.researched + Research.SEQUELS + Research.EXPANSIONS
        val weak = s.fakeRelease(4.0)
        assertNotNull(s.expansionBlocker(weak.id))
        s.startExpansion(r.id)
        val x = s.games!!.expansion!!
        assertNotNull(s.conceptBlocker(GameConcept("Next", Topic.FANTASY, Genre.RPG, platforms = listOf(Platform.PC), size = GameSize.SMALL)))
        val fans = s.games!!.fans
        var weeks = 0
        while (s.games!!.expansion != null && weeks < 60) {
            s.play(1)
            weeks++
        }
        assertNull(s.games!!.expansion)
        assertEquals(listOf(x.title), r.expansions)
        assertTrue(s.games!!.fans > fans)
        assertTrue(Goal.G_EXPANSION in s.goalsDone || Goal.G_EXPANSION !in s.openGoals)
    }
}

class StaffGrowthTest {
    @Test
    fun peopleLevelUpByWorking() {
        val s = game(Lane.WEBSITES, 10)
        s.cash = 500_000.0
        val e = s.hireDirect(s.makeCandidate(Role.ENGINEER), Lane.WEBSITES)
        val salary = e.salary
        val tech = e.tech
        e.xp = e.xpToNext - 0.1
        s.play(1)
        val after = s.staff.first { it.id == e.id }
        assertEquals(2, after.level)
        assertTrue(after.salary > salary)
        assertEquals(minOf(10, tech + 1), after.tech)
        assertEquals(listOf(e.id), s.levelUps)
        s.play(1)
        assertTrue(s.levelUps.isEmpty())
    }
}

class LoanTest {
    @Test
    fun aLoanIsRepaidOverAYear() {
        val s = game(Lane.WEBSITES, 11)
        s.cash = 200_000.0
        val limit = s.loanLimit
        assertTrue(limit >= 15_000)
        val worth = s.valuation
        s.takeLoan()
        assertEquals(200_000.0 + limit, s.cash)
        val loan = s.loan!!
        assertTrue(s.valuation <= worth + 1, "borrowing shouldn't make you richer")
        assertNotNull(s.loanBlocker)
        assertFailsWith<GameException> { s.takeLoan() }
        s.play(LOAN_TERM_WEEKS + 1)
        assertNull(s.loan)
        assertTrue(loan.weeklyPayment * LOAN_TERM_WEEKS > limit, "interest is charged")
    }

    @Test
    fun repayingEarlyClearsIt() {
        val s = game(Lane.WEBSITES, 12)
        s.cash = 500_000.0
        s.takeLoan(10_000.0)
        val payoff = s.loanPayoff!!
        assertTrue(payoff < s.loan!!.balance)
        s.repayLoan()
        assertNull(s.loan)
    }
}

class NewEventsTest {
    @Test
    fun everyNewEventResolvesBothWays() {
        for (choice in 0..1) {
            val s = game(Lane.GAMES, 13)
            s.cash = 2_000_000.0
            s.openLane(Lane.APPS)
            s.apps!!.isLive = true
            s.apps!!.users = 10_000.0
            val e = s.hireDirect(s.makeCandidate(Role.DESIGNER), Lane.GAMES)
            val r = s.fakeRelease(8.5)
            s.startGame(GameConcept("Wip", Topic.SCI_FI, Genre.ACTION, platforms = listOf(Platform.PC), size = GameSize.SMALL), evenFocus())
            s.researched = s.researched + Research.SEQUELS
            val kinds = listOf(
                EventKind.RaiseRequest(e.id), EventKind.Streamer(r.id, "Cozy Kat"), EventKind.LeakedBuild, EventKind.GameJam,
                EventKind.TaxCredit(10_000.0), EventKind.ServerOutage(Lane.APPS), EventKind.FanPetition(r.id), EventKind.Headhunter,
            )
            for (k in kinds) s.enqueue(k)
            assertEquals(kinds.size, s.events.size)
            while (s.events.isNotEmpty()) s.choose(choice)
            if (choice == 0) {
                assertEquals(r.id, s.games!!.promisedSequel)
                assertTrue(s.candidates.any { it.skill >= 8 })
            } else {
                assertTrue(s.apps!!.users < 10_000.0)
            }
            s.play(1)
        }
    }
}

class ReviewTest {
    @Test
    fun outletsScoreInTheirOwnStyle() {
        assertEquals("★★★★☆", Outlet.EUROGAMER.format(8.2))
        assertEquals("84%", Outlet.PC_GAMER.format(8.4))
        assertEquals("8.25", Outlet.GAME_INFORMER.format(8.2))
        assertEquals("8.5", Outlet.GAME_INFORMER.format(8.45))
        assertEquals("8", Outlet.IGN.format(8.2))
    }

    @Test
    fun launchesGetFullReviews() {
        val samples = mutableListOf<String>()
        for (seed in 1L..4L) {
            val s = game(Lane.GAMES, seed)
            s.cash = 1_000_000.0
            s.startGame(
                GameConcept("Review Me $seed", Topic.FANTASY, if (seed % 2 == 0L) Genre.RPG else Genre.SIMULATION, platforms = listOf(Platform.PC), size = GameSize.SMALL),
                evenFocus(),
            )
            while (s.games!!.project?.isReady != true) s.play(1)
            s.play(6)
            s.launchGame()
            val r = s.games!!.releases.first()
            assertEquals(Outlet.entries.size, r.reviews.size, "PC games get PC Gamer too")
            assertTrue(r.reviews.all { it.comment.length > 10 && it.comment.endsWith(".") })
            assertEquals(ReviewAspect.entries.toSet(), r.aspects.keys)
            assertTrue(r.userScore in 1.0..10.0)
            assertTrue(r.metascore in 1..100)
            assertTrue(r.pros.size + r.cons.size > 0)
            samples += "${r.title}: meta ${r.metascore}, players ${r.userScore}, aspects ${r.aspects}, pros ${r.pros}, cons ${r.cons}"
            r.reviews.forEach { samples += "  ${it.outlet.title} ${it.formatted}${it.outlet.scale}: ${it.comment}" }
        }
        println(samples.joinToString("\n"))
    }

    @Test
    fun consoleOnlyGamesSkipPcGamer() {
        val s = game(Lane.GAMES, 20)
        s.cash = 1_000_000.0
        val console = s.platformsOnSale.first { it != Platform.PC }
        s.startGame(GameConcept("Couch", Topic.SPORTS, Genre.CASUAL, platforms = listOf(console), size = GameSize.SMALL), evenFocus())
        while (s.games!!.project?.isReady != true) s.play(1)
        s.launchGame()
        assertTrue(s.games!!.releases.first().reviews.none { it.outlet == Outlet.PC_GAMER })
    }
}

class StayFocusedTest {
    @Test
    fun youCanTurnDownTheSecondLane() {
        val s = game(Lane.GAMES, 21)
        s.cash = 2_000_000.0
        while (s.pendingPivot == null) s.play(1)
        s.stayFocused()
        assertNull(s.pendingPivot)
        assertEquals(listOf(Lane.GAMES), s.lanes)
        assertEquals(PivotMode.STAY, s.pivot?.mode)
        s.play(20)
        assertEquals(listOf(Lane.GAMES), s.lanes)
        assertNull(s.pendingPivot, "the Pivot Point doesn't come back")
        assertEquals(1, s.lanes.size)
        // A second lane can still come later, once the business is profitable.
        assertTrue(s.expansionBlocker == null || "profitable" in s.expansionBlocker!!)
        val story = GameState.decode(s.encode()).pivot
        assertEquals(PivotMode.STAY, story?.mode)
    }
}

class RealCompaniesTest {
    @Test
    fun theWorldIsReal() {
        val s = game(Lane.GAMES, 22)
        assertEquals("Electronic Arts", s.world.rival(Lane.GAMES)?.name)
        assertEquals("OpenAI", s.world.rival(Lane.AI)?.name)
        assertEquals("PlayStation 5", Platform.PLAYSYSTEM_5.title)
        assertEquals("Nintendo", Platform.SWITCHEROO_2.maker)
        assertTrue(Outlet.entries.map { it.title }.containsAll(listOf("IGN", "GameSpot", "Eurogamer")))
    }

    @Test
    fun oldSavesGetRealRivalsAndOutlets() {
        val s = game(Lane.GAMES, 23)
        val old = s.encode().replace("\"Electronic Arts\"", "\"Gigaplay Interactive\"").replace("\"OpenAI\"", "\"Hyperion Labs\"")
        assertTrue("Gigaplay Interactive" in old)
        val loaded = GameState.decode(old)
        assertEquals("Electronic Arts", loaded.world.rival(Lane.GAMES)?.name)
        assertEquals("OpenAI", loaded.world.rival(Lane.AI)?.name)
        // Reviews from parody outlets map onto real ones.
        val review = kotlinx.serialization.json.Json.decodeFromString(Review.serializer(), """{"outlet":"PIXEL_GAZETTE","score":7,"comment":"Fun."}""")
        assertEquals(Outlet.IGN, review.outlet)
        assertEquals(7.0, review.exact)
    }

    @Test
    fun roundsHaveRealLeads() {
        val s = game(Lane.AI, 24)
        assertTrue(s.venture.rounds.first().lead.isNotEmpty())
        assertTrue(s.log.any { "led by" in it.text })
    }
}
