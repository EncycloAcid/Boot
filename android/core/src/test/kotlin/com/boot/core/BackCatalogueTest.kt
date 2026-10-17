package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Piracy and copy protection, age ratings, review bombs and collections. */
class BackCatalogueTest {
    private fun studio(seed: Long = 5, start: StartPoint = StartPoint.TODAY) = GameState.create("Pixel Forge", "F", Lane.GAMES, seed, start = start).apply {
        cash = 50_000_000.0
        if (Topic.HORROR !in games!!.topics) games!!.topics += Topic.HORROR
        repeat(2) { candidates.firstOrNull()?.let { c -> hire(c.id, Lane.GAMES) } }
    }

    private fun GameState.weeks(n: Int) = repeat(n) {
        if (pendingPivot != null) stayFocused()
        while (events.isNotEmpty()) choose(events.first().choices.lastIndex)
        advance()
    }

    private fun GameState.fake(title: String, score: Double, week: Int, sequelOf: Int? = null): GameRelease {
        val g = games!!
        val r = GameRelease(
            id = g.nextId(), title = title,
            concept = GameConcept(title, Topic.FANTASY, Genre.RPG, platforms = listOf(Platform.PC), size = GameSize.MEDIUM, sequelOf = sequelOf),
            week = week, reviews = emptyList(), score = score, weeklySales = 50_000.0, netShare = 0.7, design = 10.0, tech = 10.0,
            bugs = 0.0, hype = 0.0, devCost = 0.0, report = emptyList(), lifetimeSales = 20_000_000.0, unitsSold = 800_000.0, userScore = score,
        )
        g.releases.add(0, r)
        return r
    }

    /** Build and launch [concept] with [setup] applied to the project; returns the release. */
    private fun GameState.ship(concept: GameConcept, setup: GameState.() -> Unit = {}): GameRelease {
        startGame(concept)
        setup()
        repeat(400) {
            val p = games!!.project ?: error("no project")
            if (p.isReady) {
                weeks(1)
                while (events.isNotEmpty()) choose(0)
                launchGame()
                return games!!.releases.first { it.title == concept.title }
            }
            weeks(1)
        }
        error("never shipped")
    }

    private fun concept(title: String = "Moss Knight", audience: Audience = Audience.EVERYONE, topic: Topic = Topic.FANTASY, platforms: List<Platform> = listOf(Platform.PC)) =
        GameConcept(title, topic, Genre.RPG, audience = audience, platforms = platforms, size = GameSize.SMALL)

    @Test
    fun protectionAndPiracyChangeWithTheTimes() {
        assertTrue(Protection.DISK_LOCK in protections(1985) && Protection.ACTIVATION !in protections(1985))
        assertTrue(Protection.ALWAYS_ONLINE in protections(2026) && Protection.DISK_LOCK !in protections(2026))
        // Floppies were easy to copy; cartridges weren't; disc consoles got modded, then locked down.
        assertTrue(piracyRate(Platform.PC, 1985) > piracyRate(Platform.NES, 1988) * 5)
        assertTrue(piracyRate(Platform.PLAYSTATION, 1998) > piracyRate(Platform.PLAYSYSTEM_5, 2026))
        assertEquals("Tape-to-tape and floppy copying", piracyEra(1984))
        val s = studio()
        val c = concept()
        assertTrue(s.piracyLoss(c, Protection.ALWAYS_ONLINE) < s.piracyLoss(c, Protection.NONE) * 0.3)
        assertTrue(s.piracyLoss(c, Protection.NONE, 1.5) > s.piracyLoss(c, Protection.NONE, 0.6))
        assertTrue(s.protectionCost(Protection.ANTI_TAMPER, GameSize.LARGE) > s.protectionCost(Protection.ANTI_TAMPER, GameSize.SMALL))
    }

    @Test
    fun protectionSavesSalesAndAnnoysPlayers() {
        val open = studio(11).ship(concept())
        var cost = 0.0
        val locked = studio(11).let { s ->
            s.ship(concept()) {
                s.setProtection(1, Protection.ALWAYS_ONLINE)
                cost = s.protectionCost(Protection.ALWAYS_ONLINE, GameSize.SMALL)
            }
        }
        assertTrue(cost > 0)
        assertEquals(Protection.ALWAYS_ONLINE, locked.protection)
        assertTrue(locked.weeklySales > open.weeklySales, "${locked.weeklySales} vs ${open.weeklySales}")
        assertTrue(locked.pirated < open.pirated)
        assertTrue(locked.userScore < open.userScore)
    }

    @Test
    fun noProtectionThatDoesNotExistYet() {
        val s = studio(start = StartPoint.CONSOLE_WARS)
        s.startGame(concept(platforms = listOf(Platform.PC)))
        assertNotNull(s.protectionBlocker(1, Protection.ALWAYS_ONLINE))
        assertNull(s.protectionBlocker(1, Protection.DISK_LOCK))
    }

    @Test
    fun ratingsComeFromWhoItIsFor() {
        assertEquals(AgeRating.EVERYONE, ratingFor(Audience.YOUNG))
        assertEquals(AgeRating.MATURE, ratingFor(Audience.MATURE))
        assertEquals(AgeRating.ADULTS_ONLY, ratingFor(Audience.MATURE, uncut = true))
        // No ratings board in 1985.
        val old = studio(start = StartPoint.CONSOLE_WARS)
        Sandbox.research(old, Research.TARGET_AUDIENCE)
        old.startGame(concept(audience = Audience.MATURE, topic = Topic.HORROR))
        assertNull(old.projectRating(1))
        assertNotNull(old.uncutBlocker(1, true))
    }

    @Test
    fun adultsOnlyShipsOnFewerMachines() {
        val s = studio(7)
        Sandbox.research(s, Research.TARGET_AUDIENCE)
        Sandbox.research(s, Research.MULTI_PLATFORM)
        val r = s.ship(concept(audience = Audience.MATURE, topic = Topic.HORROR, platforms = listOf(Platform.PC, Platform.PLAYSYSTEM_5))) {
            // A game for everyone can't go uncut.
            s.setUncut(1, true)
            assertEquals(AgeRating.ADULTS_ONLY, s.projectRating(1))
        }
        assertEquals(AgeRating.ADULTS_ONLY, r.rating)
        assertEquals(listOf(Platform.PC), r.concept.platforms)
        assertTrue(r.breakdown.any { it.label == "Uncut" && it.points > 0 })
    }

    @Test
    fun theRatingsBoardCanSayAdultsOnly() {
        val s = studio(3)
        Sandbox.research(s, Research.TARGET_AUDIENCE)
        s.startGame(concept(audience = Audience.MATURE, topic = Topic.HORROR))
        val p = s.games!!.project!!
        s.enqueue(RatingDispute(1))
        val before = p.qualityBonus
        s.choose(0)
        assertTrue(p.qualityBonus < before)
        assertFalse(p.uncut)
        s.enqueue(RatingDispute(1))
        s.choose(2)
        assertTrue(p.uncut)
    }

    @Test
    fun angryPlayersReviewBombAndYouAnswer() {
        val s = studio()
        val r = s.fake("Moss Knight", 7.0, s.week)
        r.protection = Protection.ALWAYS_ONLINE
        assertEquals(BombReason.DRM, s.bombRisk(r)?.second)
        s.enqueue(ReviewBomb(r.id, BombReason.DRM, 1.5))
        r.userScore -= 1.5
        val low = r.userScore
        s.choose(0)
        assertEquals(Protection.NONE, r.protection)
        assertTrue(r.userScore > low)
        // Left alone, risky games get bombed sooner or later.
        val bombed = (1L..20L).count { seed ->
            val t = studio(seed)
            val x = t.fake("Grey Days", 6.0, t.week)
            x.protection = Protection.ALWAYS_ONLINE
            t.weeks(13)
            x.bombed
        }
        assertTrue(bombed >= 5, "$bombed")
        // Before user reviews took off, nobody bombs anything.
        val old = studio(start = StartPoint.CONSOLE_WARS)
        val y = old.fake("Grey Days", 6.0, old.week).apply { protection = Protection.DISK_LOCK }
        assertNull(old.bombRisk(y))
    }

    @Test
    fun collectionsPackageTheOldGames() {
        val s = studio()
        val one = s.fake("Moss Knight", 8.4, s.week - 300)
        val two = s.fake("Moss Knight 2", 8.0, s.week - 200, sequelOf = one.id)
        s.fake("Moss Knight 3", 8.2, s.week - 10, sequelOf = two.id)
        assertEquals(listOf(one.id, two.id), s.collectable(one.id).map { it.id })
        assertNull(s.collectionBlocker(one.id))
        val cash = s.cash
        s.makeCollection(one.id)
        assertTrue(s.cash < cash)
        assertNotNull(s.collectionBlocker(one.id))
        s.weeks(s.collectionQuote(listOf(one.id, two.id)).second + 1)
        val c = s.games!!.releases.first { it.isCollection }
        assertEquals("Moss Knight Double Pack", c.title)
        assertEquals(listOf("Moss Knight", "Moss Knight 2"), c.concept.collects)
        assertEquals(PriceTier.BUDGET, c.concept.price)
        assertTrue(c.weeklySales > 0)
        assertEquals(1, s.stats.collections)
        // The collected games don't go in another one, and the collection isn't a series of its own.
        assertTrue(s.collectable(one.id).isEmpty())
        assertTrue(s.adaptableSeries().none { it.root.isCollection })
        val back = GameState.decode(s.encode())
        assertTrue(back.games!!.releases.first().isCollection)
    }
}
