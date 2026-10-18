package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GenresAndTopicsTest {
    private fun studio() = GameState.create("Pixel Forge", "F", Lane.GAMES, 3).apply { cash = 1_000_000.0 }

    @Test
    fun newGenresAreResearched() {
        val s = studio()
        val g = s.games!!
        assertEquals(6, g.genres.size)
        assertTrue(Genre.SHOOTER !in g.genres)
        // You can't make one you haven't researched.
        val concept = GameConcept("Neon Ops", Topic.MILITARY, Genre.SHOOTER, platforms = listOf(Platform.PC), size = GameSize.SMALL)
        assertNotNull(s.conceptBlocker(concept))
        assertFailsWith<GameException> { s.startGame(concept) }
        s.researchPoints = 100.0
        assertNull(s.genreBlocker(Genre.SHOOTER))
        s.researchGenre(Genre.SHOOTER)
        assertTrue(Genre.SHOOTER in g.genres)
        assertEquals(100.0 - Genre.SHOOTER.researchCost, s.researchPoints)
        assertNull(s.conceptBlocker(concept))
        assertNotNull(s.genreBlocker(Genre.SHOOTER))
    }

    @Test
    fun aGenreWaitsForItsTime() {
        val s = GameState.create("Pixel Forge", "F", Lane.GAMES, 3, start = StartPoint.HOME_COMPUTERS)
        s.researchPoints = 100.0
        assertNotNull(s.genreBlocker(Genre.SANDBOX))
        assertNull(s.genreBlocker(Genre.PUZZLE))
    }

    @Test
    fun everyGenreHasItsShape() {
        for (g in Genre.entries) {
            assertTrue(g.idealDesignShare in 0.2..0.8)
            assertTrue(FocusArea.entries.any { g.weight(it) == 2 }, "$g")
            // Every genre gets a title and spin-off names.
            assertTrue(gameTitle(Topic.FANTASY, g, 1).isNotBlank())
            assertTrue(spinOffTitle("Moss Knight", g).startsWith("Moss Knight "))
        }
        // The codes stay unique, so topic and platform fits read right.
        assertEquals(Genre.entries.size, Genre.entries.map { it.code }.toSet().size)
    }

    @Test
    fun newTopicsFitTheNewGenres() {
        assertEquals(Fit.GREAT, Topic.MILITARY.fit(Genre.SHOOTER))
        assertEquals(Fit.GREAT, Topic.MUSIC.fit(Genre.RHYTHM))
        assertEquals(Fit.GREAT, Topic.SAMURAI.fit(Genre.FIGHTING))
        assertEquals(Fit.BAD, Topic.BALLET.fit(Genre.SHOOTER))
        assertEquals("Desert island", Topic.DESERT_ISLAND.title)
        assertTrue(Topic.entries.size >= 59)
        // Every topic has a theme's worth of title words.
        for (t in Topic.entries) assertTrue(gameTitle(t, Genre.ACTION, 2).isNotBlank())
        assertEquals(Fit.GOOD, Platform.PC.fit(Genre.SHOOTER))
    }

    @Test
    fun oldSavesKeepTheirSixGenres() {
        val back = GameState.decode(studio().encode())
        assertEquals(Genre.entries.filter { it.starter }, back.games!!.genres)
    }
}
