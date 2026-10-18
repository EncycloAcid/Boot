package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TitlesTest {
    private fun many(topic: Topic, genre: Genre, year: Int?, n: Int = 60) = (1L..n).map { gameTitle(topic, genre, it, year = year) }

    @Test
    fun titlesReadCleanly() {
        for (topic in Topic.entries) for (genre in Genre.entries) for (year in listOf(null, 1981, 1994, 2005, 2022)) {
            for (t in many(topic, genre, year, 8)) {
                assertTrue('@' !in t && '#' !in t, t)
                assertTrue(t.length <= 36, t)
                // Every word starts with a capital (or is a little joining word), so no "Desert island Tycoon".
                val bad = t.split(' ').filter { it.isNotEmpty() && it[0].isLowerCase() && it !in setOf("of", "the", "to", "in", "from", "and") }
                assertTrue(bad.isEmpty(), t)
                val w = t.lowercase().split(' ', ':', '-', '&').filter { it.length > 2 }
                assertEquals(w.size, w.toSet().size, t)
            }
        }
    }

    @Test
    fun titlesSoundLikeTheirYear() {
        val arcade = many(Topic.SPACE, Genre.ACTION, 1981, 120)
        assertTrue(arcade.any { listOf("Attack", "Mega", "Blaster", "Invaders", "tron", "Command").any { w -> w in it } }, "$arcade")
        val nineties = many(Topic.FANTASY, Genre.ADVENTURE, 1994, 120)
        assertTrue(nineties.any { listOf("Quest", "Deluxe", "Turbo", "Gold", "Ultimate", "Xtreme").any { w -> w in it } }, "$nineties")
        val indie = many(Topic.FANTASY, Genre.PLATFORMER, 2022, 120)
        assertTrue(indie.any { "Survivors" in it || it.split(' ').size == 1 }, "$indie")
        // Arcade names stay out of modern games and modern ones out of the arcade.
        assertTrue(arcade.none { "Survivors" in it || "Untitled" in it }, "$arcade")
        assertTrue(many(Topic.SPACE, Genre.ACTION, 2022, 120).none { "Invaders" in it })
        // The same salt still gives the same name.
        assertEquals(gameTitle(Topic.PIRATES, Genre.RPG, 5, year = 1990), gameTitle(Topic.PIRATES, Genre.RPG, 5, year = 1990))
        assertTrue(many(Topic.COOKING, Genre.SIMULATION, null, 40).toSet().size >= 15)
    }

    @Test
    fun sequelsAndExpansionsSpeakTheTopic() {
        // Sports series name the season.
        val seasons = (1L..20L).map { sequelTitle("Kickoff", 3, Topic.SPORTS, it, 1997) }
        assertTrue("Kickoff 98" in seasons, "$seasons")
        assertTrue(seasons.all { seriesName(it) == "Kickoff" }, "$seasons")
        assertTrue((1L..20L).map { sequelTitle("Kickoff", 4, Topic.SPORTS, it, 2005) }.any { it == "Kickoff 06" })
        // Fantasy series never do, and get fantasy subtitles.
        val fantasy = (1L..40L).map { sequelTitle("Moss Knight", 3, Topic.FANTASY, it, 1997) }
        assertTrue(fantasy.all { seriesName(it) == "Moss Knight" && ("3" in it || "III" in it) }, "$fantasy")
        val sci = (1L..60L).map { sequelTitle("Void Runner", 2, Topic.SPACE, it) }.joinToString()
        assertTrue(listOf("Zero Hour", "Deep Signal", "Event Horizon", "Long Dark", "First Contact", "Dead Orbit", "Escape from", "Beyond").any { it in sci }, sci)
        val pirates = expansionTitles(Topic.PIRATES, 3)
        assertTrue("The Frozen Throne" in pirates && pirates.none { '@' in it })
        assertTrue(expansionTitles(Topic.SPORTS, 1).first() == "World Tour")
        // A place in a title never ends in a number a sequel could be confused with.
        for (topic in Topic.entries) for (t in many(topic, Genre.ADVENTURE, null, 10)) assertTrue(!t.last().isDigit(), t)
    }
}
