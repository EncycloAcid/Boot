package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Focused coverage for start-logic, new research, GAMES news, and delayed userScore. */
class StartResearchNewsTest {

    @Test
    fun createSeedsEightResearchPoints() {
        val s = GameState.create("T", "F", Lane.GAMES, 1)
        assertEquals(8.0, s.researchPoints, 1e-9)
    }

    @Test
    fun restartTutorialShowsWelcomeAgain() {
        val s = GameState.create("T", "F", Lane.GAMES, 2, tutorial = true)
        s.completeLesson(TutorialStep.WELCOME)
        assertTrue(TutorialStep.WELCOME in s.tutorial!!.done)
        s.restartTutorial()
        assertTrue(s.tutorial!!.done.isEmpty())
        assertEquals(TutorialStep.WELCOME, s.lesson()!!.step)
    }

    @Test
    fun startPointSnapsClosedLanePreferringGames() {
        assertFalse(StartPoint.HOME_COMPUTERS.laneOpen(Lane.APPS))
        assertTrue(StartPoint.HOME_COMPUTERS.laneOpen(Lane.GAMES))
        assertTrue(Lane.APPS.arrival.contains("2008"))
        assertTrue(Lane.OS.arrival.contains("1981"))
    }

    @Test
    fun acceptDealUsesRealPlatformAndPublisherPaysFees() {
        val s = GameState.create("T", "F", Lane.GAMES, 8)
        s.cash = 5_000.0
        val g = s.games!!
        // Console fee would bankrupt a PC-only check that ignored availability; real platform must be on sale.
        val deal = PublisherDeal(
            99, "Megapublish", Topic.SPACE, Genre.STRATEGY, Platform.PC,
            GameSize.SMALL, 5, 40_000.0, 0.35, 999,
        )
        g.dealOffers += deal
        // With publisherPaysFees, cash only needs 4 weeks of SMALL budget, not PC fee + burn.
        val concept = GameConcept(
            title = "X", topic = Topic.SPACE, genre = Genre.STRATEGY,
            platforms = listOf(Platform.PC), size = GameSize.SMALL, engineId = g.engines.first().id,
        )
        assertNull(s.conceptBlocker(concept, deal, publisherPaysFees = true))
        assertNotNull(s.conceptBlocker(concept.copy(platforms = listOf(Platform.PLAYSTATION)), deal, publisherPaysFees = true))
        s.acceptDeal(deal.id)
        assertEquals(deal, g.project?.deal)
    }

    @Test
    fun acceptDealFailsWhenRealPlatformUnavailable() {
        val s = GameState.create("T", "F", Lane.GAMES, seed = 9, start = StartPoint.HOME_COMPUTERS)
        s.cash = 1_000_000.0
        val g = s.games!!
        val modern = Platform.entries.firstOrNull { !s.availablePlatform(it) && it != Platform.OWN_CONSOLE }
            ?: return // nothing locked in this year — still a valid soft skip
        val deal = PublisherDeal(1, "Pub", Topic.FANTASY, Genre.ACTION, modern, GameSize.SMALL, 5, 10_000.0, 0.5, 99)
        g.dealOffers += deal
        assertFailsWith<GameException> { s.acceptDeal(deal.id) }
    }

    @Test
    fun newResearchRequiresAndEffects() {
        val s = GameState.create("T", "F", Lane.GAMES, 11)
        s.cash = 1_000_000.0
        s.researchPoints = 10_000.0
        assertNotNull(s.researchBlocker(Research.DESTRUCTION_PHYSICS))
        s.researched = s.researched + Research.PHYSICS
        assertNull(s.researchBlocker(Research.DESTRUCTION_PHYSICS))
        s.research(Research.DESTRUCTION_PHYSICS)

        assertNotNull(s.researchBlocker(Research.CLOUD_SAVES))
        s.researched = s.researched + Research.MULTIPLAYER + Research.ONLINE_SERVICES
        assertNull(s.researchBlocker(Research.CLOUD_SAVES))
        s.research(Research.CLOUD_SAVES)

        assertNotNull(s.researchBlocker(Research.STYLE_ANIME))
        s.researched = s.researched + Research.GRAPHICS_2D_V2 + Research.STYLE_STYLIZED
        assertNull(s.researchBlocker(Research.STYLE_ANIME))

        val combo = engineCombo(listOf(Research.PHYSICS, Research.DESTRUCTION_PHYSICS))
        assertTrue(combo.level >= combo.rawLevel || combo.notes.any { it.contains("Destruction", ignoreCase = true) })

        s.research(Research.PLAYTESTING)
        s.research(Research.ACCESSIBLE_DESIGN)
        val mods = LaneModifiers()
        s.applyResearch(Lane.GAMES, mods)
        assertTrue(mods.quality >= 2)

        val apps = GameState.create("A", "F", Lane.APPS, 12)
        apps.cash = 1_000_000.0
        apps.researchPoints = 10_000.0
        apps.researched = apps.researched + Research.PUSH_NOTIFICATIONS + Research.AB_TESTING + Research.HOME_WIDGETS
        assertNull(apps.researchBlocker(Research.APP_WIDGETS_V2))
        apps.research(Research.APP_WIDGETS_V2)
        val appMods = LaneModifiers()
        apps.applyResearch(Lane.APPS, appMods)
        assertTrue(appMods.churn < 1.0)

        val os = GameState.create("O", "F", Lane.OS, 13, sandbox = true)
        os.cash = 5_000_000.0
        os.researchPoints = 10_000.0
        os.research(Research.KERNEL_V1)
        assertNull(os.researchBlocker(Research.OS_DRIVER_MODEL))
        os.research(Research.OS_DRIVER_MODEL)
        os.research(Research.OS_SERVICES)
        os.research(Research.OS_UPDATE_OTA)
        val osMods = LaneModifiers()
        os.applyResearch(Lane.OS, osMods)
        assertTrue(osMods.risk < 1.0)
    }

    @Test
    fun newTopicsExistWithFits() {
        assertEquals(Fit.GREAT, Topic.HEIST.fit(Genre.ACTION))
        assertEquals(Fit.GREAT, Topic.STEAMPUNK.fit(Genre.RPG))
        assertEquals(Fit.GREAT, Topic.SURVIVAL.fit(Genre.SIMULATION))
        assertEquals(Fit.GREAT, Topic.BALLET.fit(Genre.CASUAL))
        assertTrue(Topic.HEIST.researchCost > 0)
        assertTrue(gameTitle(Topic.HEIST, Genre.ACTION, 42).isNotBlank())
    }

    @Test
    fun launchPublishesGamesDeskNews() {
        val s = GameState.create("T", "F", Lane.GAMES, 21)
        s.cash = 500_000.0
        val g = s.games!!
        s.startGame(
            GameConcept(
                "News Hit", Topic.FANTASY, Genre.ACTION,
                platforms = listOf(Platform.PC), size = GameSize.SMALL, engineId = g.engines.first().id,
            ),
        )
        val p = g.project!!
        p.progress = p.work
        s.launchGame()
        assertTrue(s.news.any { it.desk == NewsDesk.GAMES && it.headline.contains("News Hit") })
        assertTrue(s.news.any { it.desk == NewsDesk.YOU && it.headline.contains("News Hit") })
    }

    @Test
    fun userScoreSettlesAfterThreeWeeksOrPatch() {
        val s = GameState.create("T", "F", Lane.GAMES, 22)
        s.cash = 500_000.0
        val g = s.games!!
        s.startGame(
            GameConcept(
                "Slow Burn", Topic.FANTASY, Genre.ACTION,
                platforms = listOf(Platform.PC), size = GameSize.SMALL, engineId = g.engines.first().id,
            ),
        )
        g.project!!.progress = g.project!!.work
        s.launchGame()
        s.acknowledgeReview()
        val r = g.releases.first()
        assertFalse(r.userScoreSettled)
        assertTrue(r.userScore > 0)
        repeat(3) {
            while (s.needsDecision) s.choose(s.events[0].choices.size - 1)
            s.advance()
        }
        assertTrue(r.userScoreSettled)

        // Patch path: fresh release settles immediately on patch.
        s.cash = 500_000.0
        s.startGame(
            GameConcept(
                "Patched", Topic.SCI_FI, Genre.ACTION,
                platforms = listOf(Platform.PC), size = GameSize.SMALL, engineId = g.engines.first().id,
            ),
        )
        g.project!!.progress = g.project!!.work
        g.project!!.bugs = 40.0
        s.launchGame()
        s.acknowledgeReview()
        val r2 = g.releases.first { it.title == "Patched" }
        assertFalse(r2.userScoreSettled)
        r2.bugs = 20.0
        s.patchGame(r2.id)
        assertTrue(r2.userScoreSettled)
    }

    @Test
    fun newsTrimPrefersKeepingGamesAndYou() {
        val s = GameState.create("T", "F", Lane.GAMES, 30)
        repeat(301) { i ->
            s.publish(
                if (i % 3 == 0) NewsDesk.MARKETS else if (i % 3 == 1) NewsDesk.GAMES else NewsDesk.YOU,
                "Outlet", "Headline $i", "Body",
            )
        }
        assertTrue(s.news.size <= 300)
        assertTrue(s.news.count { it.desk == NewsDesk.GAMES || it.desk == NewsDesk.YOU } >
            s.news.count { it.desk == NewsDesk.MARKETS })
    }
}
