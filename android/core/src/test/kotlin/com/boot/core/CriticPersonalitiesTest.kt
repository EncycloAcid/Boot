package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private fun game(seed: Long = 11, year: Int? = 2010) =
    GameState.create("CriticCo", "Founder", Lane.GAMES, seed, year = year)

private fun GameState.ship(genre: Genre, title: String, score: Double = 8.0): GameRelease {
    val g = games!!
    val reviews = Outlet.entries.filter { calendarYear in it.years }.map { outlet ->
        val profile = criticProfile(outlet)
        val lean = when {
            profile.likes(genre) -> 0.6
            profile.hates(genre) -> -1.2
            else -> 0.0
        }
        val exact = (score + lean).coerceIn(1.0, 10.0)
        Review(outlet, exact.toInt().coerceIn(1, 10), "Test take on $title.", exact)
    }
    val r = GameRelease(
        id = g.nextId(), title = title,
        concept = GameConcept(title, Topic.CITY, genre, platforms = listOf(Platform.PC), size = GameSize.MEDIUM),
        week = week, reviews = reviews, score = score, weeklySales = 10_000.0, netShare = 0.7,
        design = 40.0, tech = 40.0, bugs = 0.0, hype = 0.0, devCost = 1_000.0, report = emptyList(),
        unitsSold = 100_000.0,
    )
    g.releases.add(0, r)
    applyCriticRelations(r)
    return r
}

class CriticPersonalitiesTest {
    @Test
    fun profilesCoverScoringOutletsAndReuseMarks() {
        for (outlet in Outlet.entries) {
            val p = criticProfile(outlet)
            assertEquals(outlet, p.outlet)
            assertTrue(p.blurb.isNotEmpty(), "${outlet.title} needs a blurb")
            companyMark(outlet.title)
        }
        assertEquals(LogoId.IGN, companyMark("IGN").logo)
        assertEquals(LogoId.GAMESPOT, companyMark("GameSpot").logo)
        assertEquals(LogoId.EUROGAMER, companyMark("Eurogamer").logo)
        assertEquals(LogoId.EDGE, companyMark("Edge").logo)
        assertEquals(LogoId.PC_GAMER, companyMark("PC Gamer").logo)
        assertEquals(LogoId.ARS, companyMark("Ars Technica").logo)
        assertTrue(criticsIn(2010).any { it.outlet == Outlet.IGN })
        assertFalse(criticsIn(1985).any { it.outlet == Outlet.IGN })
        assertTrue(criticsIn(1985).any { it.outlet == Outlet.COMPUTER_GAMING_WORLD })
    }

    @Test
    fun oldSavesDefaultEmptyStanding() {
        val s = game()
        assertTrue(s.games!!.criticStanding.isEmpty())
        val roundTrip = GameState.decode(s.encode())
        assertNotNull(roundTrip.games)
        assertTrue(roundTrip.games!!.criticStanding.isEmpty())
        // Field absent in a stripped save still loads.
        val stripped = s.encode().replace("\"criticStanding\":[],", "")
        val loaded = GameState.decode(stripped)
        assertTrue(loaded.games!!.criticStanding.isEmpty())
    }

    @Test
    fun wheelhouseHitsFormAPet() {
        val s = game(seed = 3)
        repeat(4) { n -> s.ship(Genre.STRATEGY, "Strat $n", score = 8.2) }
        val pcg = s.games!!.standingOf(Outlet.PC_GAMER)
        assertTrue(pcg.favor > 0.3, "favor=${pcg.favor}")
        assertTrue(pcg.hitsInLane >= 3)
        assertEquals("pet", pcg.status)
        assertTrue(s.news.any { "can't get enough" in it.headline || "rooting" in it.body.lowercase() })
        assertTrue(s.feed.any { "rooting" in it.text.lowercase() || "loves" in it.text.lowercase() || "PC Gamer" in it.text })
    }

    @Test
    fun hatedGenresFormAGrudge() {
        val s = game(seed = 5)
        // Edge dislikes casual / simulation — ship hated genres with a cold Edge take.
        repeat(4) { n -> s.ship(Genre.CASUAL, "Casual $n", score = 7.0) }
        val edge = s.games!!.standingOf(Outlet.EDGE)
        assertTrue(edge.favor < -0.3, "favor=${edge.favor}")
        assertTrue(edge.snubs >= 3)
        assertEquals("grudge", edge.status)
        assertTrue(s.news.any { "has it in for" in it.headline || "grudge" in it.body.lowercase() })
    }

    @Test
    fun standingBiasesFutureScores() {
        val base = ReviewInputs(
            title = "Bias Check",
            concept = GameConcept("Bias Check", Topic.CITY, Genre.STRATEGY, platforms = listOf(Platform.PC), size = GameSize.MEDIUM),
            focus = List(3) { MutableList(3) { 1.0 } },
            score = 7.0, designRatio = 0.5, focusMatch = 0.7, bugPenalty = 0.95, polish = 0.5,
            engineLevel = 5, engineStandard = 5.0, hype = 20.0, fanTrust = 1.0,
            onTrend = false, repeated = false, rushedSequel = false, worthySequel = false,
            stepBack = false, record = false, wrongCrowd = false, awkwardPlatform = null,
            year = 2010, company = "CriticCo",
        )
        val cold = writeReviews(base.copy(criticFavor = mapOf(Outlet.PC_GAMER to -0.9)), SeededRNG(42))
        val warm = writeReviews(base.copy(criticFavor = mapOf(Outlet.PC_GAMER to 0.9)), SeededRNG(42))
        val coldScore = cold.reviews.first { it.outlet == Outlet.PC_GAMER }.exact
        val warmScore = warm.reviews.first { it.outlet == Outlet.PC_GAMER }.exact
        assertTrue(warmScore > coldScore + 0.5, "warm=$warmScore cold=$coldScore")
    }

    @Test
    fun publisherBiasMovesTheNeedle() {
        val base = ReviewInputs(
            title = "Pub Bias",
            concept = GameConcept("Pub Bias", Topic.FANTASY, Genre.RPG, platforms = listOf(Platform.PC), size = GameSize.MEDIUM),
            focus = List(3) { MutableList(3) { 1.0 } },
            score = 7.0, designRatio = 0.5, focusMatch = 0.7, bugPenalty = 0.95, polish = 0.5,
            engineLevel = 5, engineStandard = 5.0, hype = 10.0, fanTrust = 1.0,
            onTrend = false, repeated = false, rushedSequel = false, worthySequel = false,
            stepBack = false, record = false, wrongCrowd = false, awkwardPlatform = null,
            year = 2010,
        )
        val ea = writeReviews(base.copy(publisher = "Electronic Arts"), SeededRNG(7))
        val indie = writeReviews(base.copy(publisher = "Tiny Hut"), SeededRNG(7))
        val eaEuro = ea.reviews.first { it.outlet == Outlet.EUROGAMER }.exact
        val indieEuro = indie.reviews.first { it.outlet == Outlet.EUROGAMER }.exact
        assertTrue(indieEuro > eaEuro, "Eurogamer should soft-pedal less for EA: ea=$eaEuro indie=$indieEuro")
    }

    @Test
    fun publicSpatMakesNews() {
        val s = game(seed = 9)
        val g = s.games!!
        val reviews = listOf(
            Review(Outlet.IGN, 8, "Fine.", 8.2),
            Review(Outlet.GAMESPOT, 8, "Solid.", 8.0),
            Review(Outlet.EDGE, 4, "A muddle.", 4.0),
            Review(Outlet.EUROGAMER, 8, "Good.", 7.8),
            Review(Outlet.PC_GAMER, 8, "OK.", 7.9),
            Review(Outlet.GAME_INFORMER, 8, "Alright.", 8.1),
        )
        val r = GameRelease(
            id = g.nextId(), title = "Spat Game",
            concept = GameConcept("Spat Game", Topic.CITY, Genre.ACTION, platforms = listOf(Platform.PC), size = GameSize.LARGE),
            week = s.week, reviews = reviews, score = 7.5, weeklySales = 5_000.0, netShare = 0.7,
            design = 30.0, tech = 30.0, bugs = 0.0, hype = 40.0, devCost = 1_000.0, report = emptyList(),
        )
        g.releases += r
        s.applyCriticRelations(r)
        assertTrue(s.news.any { "pans" in it.headline.lowercase() || "harder than anyone" in it.headline })
        assertTrue(g.standingOf(Outlet.EDGE).snubs >= 1)
    }
}
