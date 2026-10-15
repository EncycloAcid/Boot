package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CompanyMarksTest {
    @Test
    fun publishersAreYearGated() {
        val early = publishersIn(1977)
        assertTrue("Atari" in early)
        assertFalse(early.any { it.contains("Annapurna", ignoreCase = true) })
        assertFalse(early.any { it.contains("Devolver", ignoreCase = true) })
        assertFalse("THQ Nordic" in early)

        val nineties = publishersIn(1995)
        assertTrue("Electronic Arts" in nineties || nineties.any { it.contains("Electronic") })
        assertTrue(nineties.any { it.contains("Nintendo") })
        assertFalse("Annapurna Interactive" in nineties)

        val today = publishersIn(MODERN_YEAR)
        assertTrue("Devolver Digital" in today || "Electronic Arts" in today)
        assertTrue(today.size >= early.size)
    }

    @Test
    fun investorsAreYearGated() {
        val seed77 = investorsIn(1977, 0)
        assertTrue("Sequoia Capital" !in seed77 || companyActive("Sequoia Capital", 1977))
        assertTrue(seed77.none { it == "Y Combinator" })
        assertTrue(seed77.any { it in listOf("NEA", "Venrock", "Mayfield Fund", "Bessemer Venture Partners") })

        val seriesA = investorsIn(2015, 1)
        assertTrue("Sequoia Capital" in seriesA || "Andreessen Horowitz" in seriesA)
        assertTrue(investorsIn(2000, 1).none { it == "Andreessen Horowitz" })
    }

    @Test
    fun brandsAndPlatformHoldersRespectHistory() {
        assertTrue("Nike" in brandsIn(1977))
        assertFalse("Airbnb" in brandsIn(1977))
        assertTrue("Uber" in brandsIn(2015))

        assertTrue("Atari" in platformHoldersIn(1977) || "Nintendo" in platformHoldersIn(1977))
        assertFalse("Google" in platformHoldersIn(1977))
        assertTrue("Valve" in platformHoldersIn(2010) || "Steam" in platformHoldersIn(2010) || "Microsoft" in platformHoldersIn(2010))
    }

    @Test
    fun companyMarkResolvesMajorsAndAliases() {
        val ea = companyMark("Electronic Arts")
        assertEquals("EA", ea.initials)
        assertEquals(LogoId.EA, ea.logo)
        assertEquals(LogoId.APPLE, companyMark("Apple").logo)
        assertEquals(LogoId.NINTENDO, companyMark("Nintendo").logo)
        assertEquals(LogoId.MICROSOFT, companyMark("Microsoft").logo)
        assertEquals(LogoId.STEAM, companyMark("Steam").logo)
        assertEquals(LogoId.ATARI, companyMark("Atari").logo)

        val activision = companyMark("Activision")
        assertEquals(LogoId.ACTIVISION, activision.logo)
        assertEquals(companyMark("Activision Blizzard").logo, activision.logo)

        val unknown = companyMark("Moonlit Otter Games")
        assertTrue(unknown.initials.isNotEmpty())
        assertEquals(LogoId.NONE, unknown.logo)
        assertTrue(companyActive("Moonlit Otter Games", 1977))
    }

    @Test
    fun closedFirmsDropOutAfterTheyLeave() {
        assertTrue(companyActive("Atari", 1982))
        assertFalse(companyActive("Atari", 2015))
        assertTrue(companyActive("Sierra On-Line", 1990))
        assertFalse(companyActive("Sierra On-Line", 2010))
        assertTrue("Atari" !in publishersIn(2015))
    }

    @Test
    fun outletsHaveMarksWhenTheyExist() {
        val eighties = outletsWithMarksIn(1985)
        assertTrue(eighties.any { it.contains("Computer Gaming") || it == "Electronic Games" || it == "Byte" })
        assertFalse("Polygon" in eighties)
        assertTrue("IGN" in outletsWithMarksIn(2010))
    }

    @Test
    fun decadesDealOffersUseEraPublishers() {
        val s = GameState.create("Retro", "Ada", Lane.GAMES, seed = 11, start = StartPoint.HOME_COMPUTERS)
        s.cash = 1e7
        val concept = GameConcept("Boot Quest", Topic.FANTASY, Genre.RPG, platforms = listOf(Platform.PC), size = GameSize.SMALL)
        s.games!!.releases += GameRelease(
            id = 1, title = concept.title, concept = concept, week = 0, reviews = emptyList(),
            score = 7.0, weeklySales = 1_000.0, netShare = 0.7, design = 50.0, tech = 50.0, bugs = 5.0,
            hype = 10.0, devCost = 20_000.0, report = emptyList(),
        )
        // refreshOffers rolls deals on week % 13 == 0 when a release exists (60% chance).
        for (w in listOf(13, 26, 39, 52, 65, 78, 91, 104)) {
            s.games!!.refreshOffers(w, s.calendarYear, 20.0, s.rng, listOf(GameSize.SMALL), listOf(Platform.PC))
        }
        val pubs = s.games!!.dealOffers.map { it.publisher }
        assertTrue(pubs.isNotEmpty(), "expected a publisher deal")
        assertTrue(pubs.all { companyActive(it, s.calendarYear) }, pubs.toString())
        assertTrue(pubs.none { it.contains("Annapurna") || it.contains("Devolver") }, pubs.toString())
    }
}
