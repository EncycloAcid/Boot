package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DevelopmentTest {
    private fun studio(seed: Long = 3) = GameState.create("Pixel Forge", "F", Lane.GAMES, seed).apply { cash = 5_000_000.0 }

    private fun concept(production: Production = Production.STANDARD, hook: Hook? = null, genre: Genre = Genre.RPG) =
        GameConcept("Moss Knight", Topic.FANTASY, genre, platforms = listOf(Platform.PC), size = GameSize.SMALL, production = production, hook = hook)

    /** Runs a week at a time. Milestones get [pick] (by label) if it's there; everything else the last choice. */
    private fun GameState.weeks(n: Int, pick: (List<String>) -> String? = { null }) = repeat(n) {
        while (needsDecision) {
            val e = events.first()
            val labels = e.choices.map { it.label }
            val chosen = if (e.kind is MilestoneReview) pick(labels)?.let { labels.indexOf(it) }?.takeIf { it >= 0 } else null
            choose(chosen ?: labels.lastIndex)
        }
        advance()
    }

    private fun GameState.finish(pick: (List<String>) -> String? = { null }) {
        repeat(400) {
            val p = games!!.project ?: return
            if (p.isReady && 2 in p.milestones && !needsDecision) return
            weeks(1, pick)
        }
    }

    @Test
    fun productionAndAHookChangeTheWorkAndTheBudget() {
        val lean = concept(Production.LEAN).work
        val standard = concept().work
        val lavish = concept(Production.LAVISH).work
        assertTrue(lean < standard && standard < lavish)
        assertEquals(standard * HOOK_WORK, concept(hook = Hook.STORY).work, 1e-9)
        // A lavish game burns more money every week.
        fun weekly(c: GameConcept): Double {
            val s = studio().apply { startGame(c) }
            val before = s.games!!.project!!.spent
            s.weeks(2)
            return s.games!!.project!!.spent - before
        }
        assertTrue(weekly(concept(Production.LAVISH)) > weekly(concept(Production.LEAN)) * 1.8)
    }

    @Test
    fun aHookThatSuitsTheGenreLiftsItAndAForcedOneDragsItDown() {
        assertTrue(Hook.OPEN_WORLD.fits(concept(genre = Genre.RPG)))
        assertTrue(!Hook.COZY.fits(concept(genre = Genre.RPG)))
        fun parts(c: GameConcept): List<ScorePart> {
            val s = studio(seed = 11).apply { startGame(c); finish() }
            s.launchGame()
            return s.games!!.releases.first().breakdown
        }
        val good = parts(concept(hook = Hook.STORY))
        assertTrue(good.any { it.label == "Story-driven" && it.points > 0 }, "$good")
        val forced = parts(concept(hook = Hook.COZY))
        assertTrue(forced.any { it.label == "Cozy" && it.points < 0 }, "$forced")
        val lavish = parts(concept(Production.LAVISH))
        assertTrue(lavish.any { it.label == "Production" && it.points > 0 })
    }

    @Test
    fun theMilestonesComeUpInOrderOnceEach() {
        val s = studio().apply { startGame(concept()) }
        val seen = mutableListOf<Int>()
        repeat(400) {
            val p = s.games!!.project ?: return@repeat
            if (p.isReady && 2 in p.milestones && !s.needsDecision) return@repeat
            while (s.needsDecision) {
                val k = s.events.first().kind
                if (k is MilestoneReview) seen += k.milestone
                s.choose(s.events.first().choices.lastIndex)
            }
            s.advance()
        }
        assertEquals(listOf(0, 1, 2), seen)
        val p = s.games!!.project!!
        assertEquals(3, p.choices.size)
        // The forecast is kept week by week.
        assertTrue(p.forecasts.size > 5)
    }

    @Test
    fun addingAFeatureMeansMoreWorkAndABetterGame() {
        val s = studio().apply { startGame(concept()) }
        val base = s.games!!.project!!.work
        s.finish { if ("Add a feature" in it) "Add a feature" else null }
        val p = s.games!!.project!!
        assertEquals(base * 1.12, p.work, 1e-6)
        assertTrue(p.choices.first().contains("add a feature"))
        s.launchGame()
        val part = s.games!!.releases.first().breakdown.first { it.label == "Milestones" }
        assertTrue(part.points > 0)
    }

    @Test
    fun cuttingScopeAndCrunchingSpeedThingsUp() {
        val s = studio().apply { startGame(concept()) }
        val base = s.games!!.project!!.work
        s.finish { if ("Cut scope" in it) "Cut scope" else null }
        assertEquals(base * 0.85, s.games!!.project!!.work, 1e-6)

        val c = studio().apply { startGame(concept()) }
        c.finish {
            when {
                "Keep to the plan" in it -> "Keep to the plan"
                "Crunch for a month" in it -> "Crunch for a month"
                else -> null
            }
        }
        assertTrue(c.stats.crunchWeeks >= 4)
    }

    @Test
    fun aClosedBetaCostsMoneyAndFindsBugs() {
        val s = studio().apply { startGame(concept()) }
        // Get to beta, then take the closed beta.
        repeat(400) {
            val p = s.games!!.project!!
            if (s.needsDecision) {
                val e = s.events.first()
                val k = e.kind
                if (k is MilestoneReview && k.milestone == 2) {
                    p.bugs = 50.0
                    val cash = s.cash
                    s.choose(e.choices.indexOfFirst { it.label == "Closed beta" })
                    assertEquals(30.0, p.bugs, 1e-9)
                    assertEquals(s.closedBetaCost(p), cash - s.cash, 1.0)
                    return
                }
                s.choose(e.choices.lastIndex)
            } else {
                s.advance()
            }
        }
        error("No beta came up.")
    }

    private fun released(seed: Long = 5): GameState {
        val s = studio(seed).apply { startGame(concept()); finish(); launchGame() }
        return s
    }

    @Test
    fun aPortTakesTimeThenSellsToANewCrowd() {
        val s = released()
        val r = s.games!!.releases.first()
        val targets = s.portTargets(r)
        if (targets.isEmpty()) return
        val to = targets.first()
        assertNull(s.portBlocker(r, to))
        val (cost, weeks) = s.portQuote(r)
        val cash = s.cash
        s.portGame(r.id, to)
        assertEquals(cost, cash - s.cash, 1.0)
        assertNotNull(s.portBlocker(r, to))
        assertFailsWith<GameException> { s.portGame(r.id, to) }
        s.weeks(weeks - 1)
        assertTrue(to !in r.extraPlatforms)
        s.weeks(2)
        assertTrue(to in r.extraPlatforms)
        assertEquals(1, s.stats.ports)
        assertTrue(to !in s.portTargets(r))
    }

    @Test
    fun aSaleSellsMoreButNotTooOften() {
        val s = released()
        val r = s.games!!.releases.first()
        assertNotNull(s.saleBlocker(r))
        s.weeks(9)
        if (r.weeklySales <= 0) return
        assertNull(s.saleBlocker(r))
        val before = r.weeklySales
        val fans = s.games!!.fans
        s.runSale(r.id)
        assertEquals(before * 1.5, r.weeklySales, 1e-6)
        assertTrue(s.games!!.fans > fans)
        assertNotNull(s.saleBlocker(r))
        assertFailsWith<GameException> { s.runSale(r.id) }
    }

    @Test
    fun oldSavesLoadWithTheDefaults() {
        val s = studio().apply { startGame(concept()) }
        val json = s.encode()
        val back = GameState.decode(json)
        assertEquals(Production.STANDARD, back.games!!.project!!.concept.production)
        assertNull(back.games!!.project!!.concept.hook)
        assertEquals(1.0, back.games!!.project!!.workScale)
    }
}
