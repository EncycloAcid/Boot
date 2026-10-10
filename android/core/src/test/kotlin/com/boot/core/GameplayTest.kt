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
private fun GameState.fakeRelease(
    score: Double, bugs: Double = 10.0, units: Double = 5_000.0, title: String = "Fake Quest", sequelOf: Int? = null, genre: Genre = Genre.RPG,
): GameRelease {
    val g = games!!
    val r = GameRelease(
        id = g.nextId(), title = title,
        concept = GameConcept(title, Topic.FANTASY, genre, platforms = listOf(Platform.PC), size = GameSize.SMALL, sequelOf = sequelOf),
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
        assertEquals("Dragon Thing", g.clashFor(concept, s.week)?.game?.title)
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

class TycoonTest {
    @Test
    fun priceMattersMoreForWeakGames() {
        assertTrue(PriceTier.PREMIUM.revenueFactor(9.0) > 1.0, "great games can charge more")
        assertTrue(PriceTier.PREMIUM.revenueFactor(5.0) < 1.0, "weak games can't")
        assertTrue(PriceTier.BUDGET.revenueFactor(4.0) > 1.0, "cheap helps a weak game")
        assertEquals(1.0, PriceTier.STANDARD.revenueFactor(6.0))
        val c = GameConcept("X", Topic.FANTASY, Genre.RPG, platforms = listOf(Platform.PC), size = GameSize.MEDIUM, price = PriceTier.PREMIUM)
        assertEquals(GameSize.MEDIUM.price * 1.4, c.unitPrice, 1e-9)
    }

    @Test
    fun agencyRatesTradeVolumeForValue() {
        val s = game(Lane.WEBSITES, 30)
        s.setAgencyRate(AgencyRate.PREMIUM)
        assertEquals(AgencyRate.PREMIUM, s.web!!.rate)
        assertEquals(AgencyRate.PREMIUM, GameState.decode(s.encode()).web!!.rate)
    }

    @Test
    fun perksNeedRoomAndPayOff() {
        val s = game(Lane.WEBSITES, 31)
        s.cash = 5_000_000.0
        assertNotNull(s.perkBlocker(Perk.GYM), "a gym won't fit in the apartment")
        s.buyPerk(Perk.COFFEE)
        assertNotNull(s.perkBlocker(Perk.PLANTS), "the apartment fits one perk")
        s.moveOffice()
        s.buyPerk(Perk.PLANTS)
        s.buyPerk(Perk.ARCADE)
        assertEquals(3, s.perks.size)
        assertTrue(s.perkUpkeep > 0)
        val e = s.hireDirect(s.makeCandidate(Role.ENGINEER), Lane.WEBSITES)
        e.morale = 0.7
        s.play(30)
        assertTrue(s.staff.first { it.id == e.id }.morale > 0.72, "morale settles higher with perks")
        s.removePerk(Perk.ARCADE)
        assertTrue(Perk.ARCADE !in s.perks)
    }

    @Test
    fun goingPublicCanKeepTheGameGoing() {
        val s = game(Lane.WEBSITES, 32)
        s.cash = 1_000_000.0
        s.week = IPO_EARLIEST_WEEK + 1
        s.list()
        val l = s.listing!!
        assertNull(s.ending)
        assertTrue(s.venture.founderEquity < 1.0)
        assertNotNull(s.venture.board)
        l.lastQuarterRevenue = 1_000.0
        repeat(30) {
            if (s.pendingPivot != null) s.stayFocused()
            s.play(1)
        }
        assertTrue(l.history.size >= 20)
        assertTrue(s.log.any { "Quarterly earnings" in it.text })
        assertTrue(s.founderNetWorth > 0)
        val equity = s.venture.founderEquity
        s.cash = 1e12
        s.buyBack()
        assertTrue(s.venture.founderEquity > equity)
        s.issueShares()
        assertNotNull(s.offeringBlocker())
        s.retire()
        assertEquals(EndingKind.IPO, s.ending?.kind)
        assertTrue("public company" in s.ending!!.summary)
    }

    @Test
    fun youCanPlayPastYearFifteen() {
        val s = game(Lane.WEBSITES, 33)
        s.cash = 1e9
        s.week = GameState.LENGTH_WEEKS - 1
        if (s.pendingPivot == null) s.pendingPivot = PivotOffer(PivotTrigger.VIRAL_ACCIDENT, s.week, "h", "b", 0.0)
        s.stayFocused()
        s.play(2)
        assertEquals(EndingKind.INDIE, s.ending?.kind)
        s.keepPlaying()
        assertNull(s.ending)
        assertTrue(s.freeplay)
        repeat(10) {
            if (s.pendingPivot != null) s.stayFocused()
            s.play(1)
        }
        assertNull(s.ending)
        assertTrue(s.week > GameState.LENGTH_WEEKS)
        assertFailsWith<GameException> { game(Lane.WEBSITES, 34).keepPlaying() }
    }

    @Test
    fun theIndustryTableRanksYou() {
        val s = game(Lane.GAMES, 35)
        val table = s.rankings()
        assertEquals(table.sortedByDescending { it.value }, table)
        assertEquals(table.size, s.rank)
        assertEquals("Ubisoft", s.nextToBeat?.name)
        s.cash = 5e12
        assertEquals(1, s.rank)
    }

    @Test
    fun licensingAndMerchPay() {
        val s = game(Lane.GAMES, 36)
        s.cash = 1_000_000.0
        val g = s.games!!
        g.engines += Engine(99, "Ember", listOf(Research.GRAPHICS_2D_V2, Research.SOUND_STEREO), 0)
        assertFailsWith<GameException> { s.setEngineLicensed(g.engines.first().id, true) }
        s.setEngineLicensed(99, true)
        s.researched = s.researched + Research.MARKETING + Research.MERCHANDISE
        s.fakeRelease(8.6, units = 200_000.0)
        s.play(1)
        assertTrue(g.lastRoyalties > 0)
        assertTrue(g.lastMerch > 0)
    }
}

class FranchiseTest {
    @Test
    fun titlesSoundLikeGames() {
        assertEquals(gameTitle(Topic.RACING, Genre.SIMULATION, 1), gameTitle(Topic.RACING, Genre.SIMULATION, 1))
        val many = (1L..20L).map { gameTitle(Topic.FANTASY, Genre.RPG, it) }.toSet()
        assertTrue(many.size >= 8, "$many")
        val first = gameTitle(Topic.SPACE, Genre.STRATEGY, 7)
        assertTrue(gameTitle(Topic.SPACE, Genre.STRATEGY, 7, setOf(first)) != first)
        for (topic in Topic.entries) for (genre in Genre.entries) assertTrue(gameTitle(topic, genre, 3).isNotBlank())

        assertEquals("III", roman(3))
        assertEquals("Moss Knight", seriesName("Moss Knight II: Reborn"))
        assertEquals("Moss Knight", seriesName("Moss Knight 3"))
        for (salt in 1L..12L) {
            val t = sequelTitle("Moss Knight", 3, Topic.FANTASY, salt)
            assertEquals("Moss Knight", seriesName(t), t)
            assertTrue("3" in t || "III" in t, t)
        }
        assertEquals("Moss Knight Tactics", spinOffTitle("Moss Knight", Genre.STRATEGY))
        assertEquals("Moss Knight Command", spinOffTitle("Moss Knight", Genre.STRATEGY, 1))
    }

    @Test
    fun realSeriesAnnounceAndShipOnSchedule() {
        val s = game(Lane.GAMES, 40)
        s.cash = 10_000_000.0
        val m = s.games!!.market
        var announced = false
        repeat(60) {
            s.play(1)
            if (m.upcoming.any { it.franchise == "Call of Duty" }) announced = true
            assertEquals(m.upcoming.sortedBy { it.week }, m.upcoming)
            assertTrue(m.upcoming.all { it.week >= s.week })
        }
        assertTrue(announced)
        val fc = m.games.filter { it.franchise == "EA Sports FC" }
        assertEquals(listOf("EA Sports FC 28"), fc.map { it.title })
        assertEquals(37, fc.single().week)
        assertTrue(m.games.any { it.franchise == "Call of Duty" && it.title.startsWith("Call of Duty: ") })
        assertTrue(m.games.count { it.franchise != null } >= 4)
        assertEquals(m.franchiseNext, GameState.decode(s.encode()).games!!.market.franchiseNext)
    }

    @Test
    fun launchingNextToAGiantCostsPlayers() {
        val s = game(Lane.GAMES, 41)
        val g = s.games!!
        val rpg = GameConcept("Mine", Topic.FANTASY, Genre.RPG, platforms = listOf(Platform.PC), size = GameSize.SMALL)
        assertNull(g.clashFor(rpg, s.week))
        g.market.games += RivalGame(900, "Some Strategy Hit", "Ubisoft", Topic.MEDIEVAL, Genre.STRATEGY, GameSize.LARGE, 8.4, s.week, 1e5)
        assertNull(g.clashFor(rpg, s.week))
        g.market.games += RivalGame(901, "Some RPG Hit", "Bandai Namco", Topic.FANTASY, Genre.RPG, GameSize.LARGE, 8.1, s.week - 2, 1e5)
        assertEquals(0.85, g.clashFor(rpg, s.week)!!.penalty)
        g.market.games += RivalGame(902, "Grand Theft Auto VII", "Rockstar Games", Topic.CITY, Genre.ACTION, GameSize.AAA, 9.7, s.week - 1, 5e6, franchise = "Grand Theft Auto")
        val clash = g.clashFor(rpg, s.week)!!
        assertTrue(clash.mega)
        assertEquals("Grand Theft Auto VII", clash.game.title)
        assertNull(g.clashFor(rpg, s.week + 10))
    }

    @Test
    fun sequelsAndSpinOffsBuildASeries() {
        val s = game(Lane.GAMES, 42)
        s.researched = s.researched + Research.SEQUELS
        val g = s.games!!
        val first = s.fakeRelease(8.6)
        assertTrue(g.franchises.isEmpty())

        val next = s.sequelConcept(first.id)
        assertEquals(first.id, next.sequelOf)
        assertEquals("Fake Quest", seriesName(next.title))
        assertEquals(Topic.FANTASY, next.topic)
        val spinOff = s.sequelConcept(first.id, Genre.STRATEGY)
        assertEquals("Fake Quest Tactics", spinOff.title)
        assertEquals(Genre.STRATEGY, spinOff.genre)
        assertEquals("A sequel or spin-off keeps the topic.", s.conceptBlocker(next.copy(topic = Topic.SPACE)))

        val second = s.fakeRelease(8.2, title = "Fake Quest II", sequelOf = first.id)
        s.fakeRelease(7.0, title = "Fake Quest Tactics", sequelOf = second.id, genre = Genre.STRATEGY)
        s.fakeRelease(6.0, title = "Unrelated")
        val f = g.franchises.single()
        assertEquals("Fake Quest", f.name)
        assertEquals(3, f.entries.size)
        assertEquals("Fake Quest Tactics", f.latest.title)
        assertEquals((8.6 + 8.2 + 7.0) / 3, f.averageScore, 1e-9)
        assertEquals(f.latest.id, s.sequelConcept(first.id).sequelOf)
        assertTrue(seriesName(g.nextTitle(second.id)) == "Fake Quest")
    }

    @Test
    fun aGoodSeriesSellsMoreThanItsFirstGame() {
        fun launchWeek(withSeries: Boolean): Double {
            val s = game(Lane.GAMES, 43)
            s.cash = 1_000_000.0
            s.researched = s.researched + Research.SEQUELS
            val concept = if (withSeries) {
                val a = s.fakeRelease(8.8, title = "Moss Knight")
                val b = s.fakeRelease(8.5, title = "Moss Knight 2", sequelOf = a.id)
                s.sequelConcept(b.id)
            } else {
                GameConcept("Moss Knight 3", Topic.FANTASY, Genre.RPG, platforms = listOf(Platform.PC), size = GameSize.SMALL)
            }
            s.startGame(concept, List(3) { mutableListOf(1.0, 1.0, 1.0) })
            while (s.games!!.project?.isReady != true) s.play(1)
            s.launchGame()
            val r = s.games!!.releases[0]
            assertEquals(withSeries, r.report.any { "series" in it }, r.report.joinToString("\n"))
            if (withSeries) assertTrue(r.report.any { "brought its fans: +20% at launch." in it }, r.report.joinToString("\n"))
            return r.weeklySales / r.score
        }
        launchWeek(true)
        launchWeek(false)
    }
}

class AcquisitionTest {
    private fun rich(seed: Long): GameState {
        val s = game(Lane.GAMES, seed)
        s.cash = 2e11
        return s
    }

    @Test
    fun buyAnIndieAndItPaysYou() {
        val s = rich(50)
        val g = s.games!!
        val indie = studio("ConcernedApe")!!
        val price = s.acquisitionPrice(indie)
        val before = s.cash
        val valueBefore = s.enterpriseValue
        s.acquire(indie.name)
        assertEquals(before - price, s.cash, 1.0)
        assertTrue(g.owns(indie.name))
        assertTrue(s.enterpriseValue - valueBefore > price * 0.8)
        assertTrue(indie !in s.studiosForSale())
        assertEquals("It's already yours.", s.acquisitionBlocker(indie))
        s.play(1)
        assertTrue(g.subsidiaries.single().lastProfit > 0)
        assertTrue(Achievement.FIRST_STUDIO in s.achievements)
        val cash = s.cash
        s.divest(indie.name)
        assertTrue(s.cash > cash + s.studioValue(indie) * 0.85)
        assertTrue(g.subsidiaries.isEmpty())
        assertEquals(GameState.decode(s.encode()).games!!.subsidiaries, g.subsidiaries)
    }

    @Test
    fun bigPublishersWaitAndOwnedGamesStopCompeting() {
        val s = rich(51)
        val g = s.games!!
        val nintendo = studio("Nintendo")!!
        assertEquals("The big publishers won't talk to a company this young.", s.acquisitionBlocker(nintendo))
        val old = rich(57)
        old.week = 200
        old.acquire("Nintendo")
        val og = old.games!!
        assertTrue(old.rankings().none { it.name == "Nintendo" })
        og.market.games += RivalGame(950, "Mario Kart 10", "Nintendo", Topic.RACING, Genre.CASUAL, GameSize.AAA, 9.6, old.week, 9e8, franchise = "Mario Kart")
        val concept = GameConcept("Mine", Topic.RACING, Genre.CASUAL, platforms = listOf(Platform.PC), size = GameSize.SMALL)
        assertNull(og.clashFor(concept, old.week))
        assertTrue(g.subsidiaries.isEmpty())
        old.play(1)
        assertTrue(old.games!!.market.chart.any { it.owned }, old.games!!.market.chart.toString())
        assertTrue(Achievement.ITS_A_ME in old.achievements)
    }
}

class LiveServiceTest {
    private fun liveGame(s: GameState, players: Double = 200_000.0, score: Double = 8.0): GameRelease {
        val r = s.fakeRelease(score)
        r.live = LiveService(players, s.week)
        return r
    }

    @Test
    fun liveGamesNeedResearchAndSize() {
        val s = game(Lane.GAMES, 52)
        s.cash = 1e7
        val c = GameConcept("Live", Topic.FANTASY, Genre.RPG, platforms = listOf(Platform.PC), size = GameSize.SMALL, live = true)
        assertEquals("Research live service games first.", s.conceptBlocker(c))
        s.researched = s.researched + Research.LIVE_SERVICE
        assertEquals("Live games need to be medium or bigger.", s.conceptBlocker(c))
    }

    @Test
    fun playersSpendLeaveAndComeBackForSeasons() {
        val s = game(Lane.GAMES, 53)
        s.cash = 1e8
        val r = liveGame(s)
        val l = r.live!!
        s.play(1)
        assertTrue(l.lastRevenue > 0)
        assertTrue(l.players < 200_000)
        assertNotNull(s.seasonBlocker(r.id))
        repeat(SEASON_GAP) { s.play(1) }
        assertNull(s.seasonBlocker(r.id))
        val before = l.players
        s.launchSeason(r.id)
        assertTrue(l.players > before * 1.15)
        assertEquals(1, l.seasons)
        assertNotNull(s.seasonBlocker(r.id))
    }

    @Test
    fun staleGamesBleedAndAggressiveStoresBleedFaster() {
        fun after(m: Monetization, weeks: Int): Double {
            val s = game(Lane.GAMES, 54)
            s.cash = 1e8
            val r = liveGame(s)
            s.setMonetization(r.id, m)
            s.play(weeks)
            return r.live!!.players
        }
        assertTrue(after(Monetization.AGGRESSIVE, 10) < after(Monetization.COSMETIC, 10))
        // Past the stale point, players leave faster.
        val p10 = after(Monetization.FAIR, 10)
        val p30 = after(Monetization.FAIR, 30)
        val p40 = after(Monetization.FAIR, 40)
        assertTrue(p40 / p30 < p10 / 200_000)
    }

    @Test
    fun shuttingDownStopsTheBills() {
        val s = game(Lane.GAMES, 55)
        s.cash = 1e8
        val r = liveGame(s, players = 50_000.0)
        val trust = s.games!!.fanTrust
        s.endService(r.id)
        assertTrue(r.live!!.closed)
        assertTrue(s.games!!.fanTrust < trust)
        assertTrue(s.games!!.liveGames.isEmpty())
        s.play(1)
        assertEquals(0.0, r.live!!.players)
        assertEquals(r.live, GameState.decode(s.encode()).games!!.release(r.id)!!.live)
    }
}

class AchievementTest {
    @Test
    fun achievementsUnlockOnceAndSurviveSaves() {
        val s = game(Lane.GAMES, 56)
        assertTrue(s.achievements.isEmpty())
        s.fakeRelease(9.6)
        s.play(1)
        val got = s.achievements
        assertTrue(Achievement.HELLO_WORLD in got)
        assertTrue(Achievement.BREAKOUT in got)
        assertTrue(Achievement.MASTERPIECE in got)
        assertTrue(Achievement.GOTY !in got)
        val week = got.getValue(Achievement.HELLO_WORLD)
        s.play(1)
        assertEquals(week, s.achievements.getValue(Achievement.HELLO_WORLD))
        assertEquals(s.achievements, GameState.decode(s.encode()).achievements)
        assertEquals(s.log.count { it.text.startsWith("Achievement unlocked: Hello, World") }, 1)
    }
}
