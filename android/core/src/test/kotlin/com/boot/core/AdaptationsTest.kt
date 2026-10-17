package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdaptationsTest {
    private fun studio(seed: Long = 5) = GameState.create("Pixel Forge", "F", Lane.GAMES, seed).apply {
        cash = 50_000_000.0
        repeat(2) { candidates.firstOrNull()?.let { c -> hire(c.id, Lane.GAMES) } }
    }

    private fun GameState.release(title: String, score: Double, sequelOf: Int? = null): GameRelease {
        val g = games!!
        val r = GameRelease(
            id = g.nextId(), title = title,
            concept = GameConcept(title, Topic.FANTASY, Genre.RPG, platforms = listOf(Platform.PC), size = GameSize.MEDIUM, sequelOf = sequelOf),
            week = week, reviews = emptyList(), score = score, weeklySales = 50_000.0, netShare = 0.7, design = 10.0, tech = 10.0,
            bugs = 0.0, hype = 0.0, devCost = 0.0, report = emptyList(), lifetimeSales = 20_000_000.0, unitsSold = 500_000.0,
        )
        g.releases.add(0, r)
        return r
    }

    /** A two-game series that averages 8.25. */
    private fun GameState.hitSeries(): GameRelease {
        val root = release("Moss Knight", 8.5)
        release("Moss Knight 2", 8.0, sequelOf = root.id)
        return root
    }

    private fun GameState.weeks(n: Int) = repeat(n) {
        if (pendingPivot != null) stayFocused()
        while (events.isNotEmpty()) choose(events.first().choices.lastIndex)
        advance()
    }

    private fun GameState.offer(root: GameRelease): ScreenOffer {
        val f = adaptableSeries().first { it.root.id == root.id }
        val (fee, stake) = screenTerms(f, ScreenKind.FILM)
        return ScreenOffer(root.id, "Universal", ScreenKind.FILM, fee, stake)
    }

    @Test
    fun onlyGoodSeriesGetCalls() {
        val s = studio()
        val meh = s.release("Grey Days", 6.5)
        val root = s.hitSeries()
        val series = s.adaptableSeries()
        assertEquals(listOf(root.id), series.map { it.root.id })
        assertNotNull(s.screenBlocker(meh.id))
        assertNull(s.screenBlocker(root.id))
        // Co-producing should come out better, on average, than handing it over.
        val f = series.single()
        assertTrue(screenOutlook(f, ScreenDeal.COPRODUCE) > screenOutlook(f, ScreenDeal.LICENSE))
        // Studios only existed for what was around: no streaming in 1995, no game films in 1985.
        assertTrue("Netflix" !in screenStudios(ScreenKind.SERIES, 1995))
        assertEquals(listOf(ScreenKind.ANIMATED), screenKinds(1985))
    }

    @Test
    fun licensingPaysNowAndTheFilmComesOutLater() {
        val s = studio()
        val root = s.hitSeries()
        val k = s.offer(root)
        s.enqueue(k)
        val cash = s.cash
        s.choose(0)
        assertEquals(cash + k.fee, s.cash, 1.0)
        val a = s.games!!.adaptations.single()
        assertEquals(ScreenDeal.LICENSE, a.deal)
        assertTrue(!a.out)
        // One at a time per series.
        assertNotNull(s.screenBlocker(root.id))
        assertFailsWith<GameException> { s.pitchScreen(root.id) }
        s.weeks(ScreenKind.FILM.weeks + 1)
        assertTrue(a.out)
        assertEquals(1, s.stats.adaptations)
        assertTrue(s.news.any { it.headline.contains("Moss Knight") && it.tag == NewsTag.REVIEW })
        assertTrue(a.flop == (s.games!!.screenLift[root.id] == SCREEN_FLOP_LIFT))
        assertTrue(a.hit == (s.games!!.screenLift[root.id] == SCREEN_HIT_LIFT))
    }

    @Test
    fun coproducingCostsMoneyAndDoesBetter() {
        fun critics(deal: Int): List<Int> = (1L..30L).map { seed ->
            val s = studio(seed)
            val root = s.hitSeries()
            s.enqueue(s.offer(root))
            s.choose(deal)
            s.weeks(ScreenKind.FILM.weeks + 1)
            s.games!!.adaptations.single().critics!!
        }
        val licensed = critics(0)
        val coproduced = critics(1)
        assertTrue(coproduced.average() > licensed.average() + 5, "${coproduced.average()} vs ${licensed.average()}")
        // Both ways can go well or badly.
        assertTrue(licensed.any { it < BAD_ADAPTATION } && coproduced.any { it >= GOOD_ADAPTATION })
    }

    @Test
    fun aHitBringsFansAndSalesAndAFlopHurts() {
        var sawHit = false
        var sawFlop = false
        for (seed in 1L..40L) {
            val s = studio(seed)
            val root = s.hitSeries()
            s.enqueue(s.offer(root))
            s.choose(1)
            val g = s.games!!
            s.weeks(ScreenKind.FILM.weeks - 1)
            val fans = g.fans
            val trust = g.fanTrust
            val sales = g.series(root).sumOf { it.weeklySales }
            s.weeks(2)
            val a = g.adaptations.single()
            if (a.hit && !sawHit) {
                sawHit = true
                assertTrue(g.fans > fans)
                assertTrue(g.series(root).sumOf { it.weeklySales } > sales)
                assertTrue(a.takings > a.money, "a co-produced hit pays back more than went in")
                assertEquals(1, s.stats.adaptationHits)
            }
            if (a.flop && !sawFlop) {
                sawFlop = true
                assertTrue(g.fanTrust < trust)
                assertEquals(SCREEN_FLOP_LIFT, g.screenLift[root.id])
            }
            if (sawHit && sawFlop) break
        }
        assertTrue(sawHit && sawFlop)
    }

    @Test
    fun theNextGameInTheSeriesFeelsIt() {
        fun launchSales(lift: Double?): Double {
            val s = studio(9)
            val root = s.hitSeries()
            lift?.let { s.games!!.screenLift[root.id] = it }
            Sandbox.research(s, Research.SEQUELS)
            val latest = s.games!!.series(root).last()
            s.startGame(GameConcept("Moss Knight 3", Topic.FANTASY, Genre.RPG, platforms = listOf(Platform.PC), size = GameSize.SMALL, sequelOf = latest.id))
            repeat(300) {
                val p = s.games!!.project ?: return@repeat
                if (p.isReady) {
                    while (s.events.isNotEmpty()) s.choose(s.events.first().choices.lastIndex)
                    s.launchGame()
                    return s.games!!.releases.first { it.title == "Moss Knight 3" }.weeklySales
                }
                s.weeks(1)
            }
            error("never shipped")
        }
        val plain = launchSales(null)
        val after = launchSales(SCREEN_HIT_LIFT)
        val flop = launchSales(SCREEN_FLOP_LIFT)
        assertTrue(after > plain * 1.1, "$after vs $plain")
        assertTrue(flop < plain * 0.95, "$flop vs $plain")
    }

    @Test
    fun shoppingTheRights() {
        val s = studio()
        val root = s.hitSeries()
        assertTrue(s.screenPitchOdds(root.id) in 0.1..0.85)
        s.pitchScreen(root.id)
        // Either a studio bit, or you wait half a year to try again.
        val offer = s.events.firstOrNull { it.kind is ScreenOffer }
        if (offer == null) assertNotNull(s.screenPitchBlocker(root.id))
        assertFailsWith<GameException> { s.pitchScreen(root.id) }
    }

    @Test
    fun oldSavesLoad() {
        val s = studio().apply { hitSeries() }
        s.enqueue(s.offer(s.adaptableSeries().single().root))
        s.choose(0)
        val back = GameState.decode(s.encode())
        assertEquals(1, back.games!!.adaptations.size)
        assertEquals(s.games!!.adaptations.single().premiereWeek, back.games!!.adaptations.single().premiereWeek)
    }
}
