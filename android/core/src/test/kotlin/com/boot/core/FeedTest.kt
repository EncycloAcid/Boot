package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FeedTest {
    private fun studio(seed: Long = 3) = GameState.create("Pixel Forge", "Sam", Lane.GAMES, seed).apply { cash = 5_000_000.0 }

    @Test
    fun anAnnouncementGetsPeopleTalking() {
        val s = studio()
        s.startGame(GameConcept("Moss Knight", Topic.FANTASY, Genre.RPG, platforms = listOf(Platform.PLAYSYSTEM_5), size = GameSize.SMALL))
        s.enqueue(PlatformInvite("Sony", "a PlayStation State of Play", 1))
        s.choose(0)
        val about = s.feed.filter { it.about == "Moss Knight" }
        assertTrue(about.size >= 2, "${s.feed}")
        assertTrue(about.any { it.kind == Poster.YOU && "#MossKnight" in it.text })
        assertTrue(about.all { it.likes > 0 && it.handle.startsWith("@") })
        assertEquals("#MossKnight", s.trending().first())
        // Ids are the feed's own.
        assertEquals((1..s.feed.size).toList(), s.feed.map { it.id })
        assertEquals(s.feed, GameState.decode(s.encode()).feed)
    }

    @Test
    fun theFeedIsTheForumsBeforeTheWebAndChirpAfter() {
        assertEquals("Chirp", studio().feedName)
        val start = StartPoint.entries.first { (it.year ?: 9999) < 1990 }
        assertEquals("The forums", GameState.create("T", "F", Lane.GAMES, 3, start = start).feedName)
    }

    @Test
    fun aCareerFillsTheFeedWithVariety() {
        val s = studio(seed = 11)
        val pilot = Autopilot()
        while (!s.isOver && s.week < 52 * 4) pilot.playWeek(s)
        assertTrue(s.feed.size >= 40, "${s.feed.size}")
        assertTrue(s.feed.map { it.kind }.toSet().size >= 4)
        // Plenty of different things said.
        assertTrue(s.feed.map { it.text }.toSet().size > s.feed.size * 0.6)
    }
}
