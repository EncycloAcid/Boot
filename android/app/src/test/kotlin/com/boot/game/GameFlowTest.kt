package com.boot.game

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import com.boot.core.Autopilot
import com.boot.core.gameTeam
import com.boot.core.hire
import com.boot.core.moveOffice
import com.boot.core.Office
import com.boot.core.seasonCost
import com.boot.core.research
import com.boot.core.acquire
import com.boot.core.Achievement
import com.boot.core.Format
import com.boot.core.Research
import com.boot.core.LiveService
import com.boot.core.GameRelease
import com.boot.core.GameState
import com.boot.core.Perk
import com.boot.core.Feature
import com.boot.core.encodeHall
import com.boot.core.RunRecord
import com.boot.core.PrizePool
import com.boot.core.Engine
import com.boot.core.stayFocused
import com.boot.core.Combo
import com.boot.core.Sandbox
import com.boot.core.Era
import com.boot.core.calendarYear
import com.boot.core.Difficulty
import com.boot.core.Goal
import com.boot.core.EndingKind
import com.boot.core.GameConcept
import com.boot.core.GameSize
import com.boot.core.Genre
import com.boot.core.Lane
import com.boot.core.Platform
import com.boot.core.Topic
import com.boot.core.acknowledgeReview
import com.boot.core.advance
import com.boot.core.startGame
import com.boot.core.choose
import com.boot.core.spinOffTitle
import com.boot.game.ui.BootApp
import com.boot.game.ui.BootTheme
import org.junit.Assert.assertEquals
import com.boot.core.TutorialStep
import com.boot.core.lesson
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Drives the real Compose UI on the JVM, from the apartment to the ending. Each step also saves a
 * phone-sized screenshot to app/build/screenshots so layout changes are easy to review.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w393dp-h852dp-xxhdpi")
class GameFlowTest {
    @get:Rule(order = 0)
    val display = DisplayRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private lateinit var store: GameStore

    private fun launch(): GameStore {
        store = GameStore(RuntimeEnvironment.getApplication())
        compose.setContent { BootTheme { BootApp(store) } }
        return store
    }

    /** Draws the window this node lives in straight into a bitmap. */
    private fun SemanticsNodeInteraction.save(name: String) {
        compose.waitForIdle()
        val view = (fetchSemanticsNode().root as ViewRootForTest).view
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val dir = DisplayRule.screenshots
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun screen(name: String) = compose.onRoot().save(name)

    /** The topmost window: a sheet or full-screen page above the game. */
    private fun overlay(name: String) {
        val roots = compose.onAllNodes(isRoot())
        roots[roots.fetchSemanticsNodes().size - 1].save(name)
    }

    /**
     * Scroll a button into view, then tap it. The tap goes through the button's click action
     * rather than a touch, since scrolled-to content can sit under the floating tab bar.
     */
    private fun tapInList(text: String) {
        compose.onNodeWithText(text).performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }

    private fun studio(page: StudioPage) {
        compose.runOnIdle { store.openStudio(page) }
        compose.waitForIdle()
    }

    private fun tab(label: String) {
        compose.onNode(hasText(label) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)).performClick()
        compose.waitForIdle()
    }

    private fun scrollToTop() {
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToIndex(0)
        compose.waitForIdle()
    }

    @Test
    fun startASoloStudioAndPlayAWeek() {
        launch()
        screen("01-new-game")

        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Hardware"))
        compose.onNodeWithText("Hardware").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithText("Hard mode", substring = true).assertExists()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Hard"))
        compose.onNodeWithText("Hard").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Games"))
        compose.onNodeWithText("Games").performSemanticsAction(SemanticsActions.OnClick)
        // No tutorial this time.
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Show me the ropes"))
        compose.onNodeWithText("Show me the ropes").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithText("Start your game studio").performClick()
        assertEquals(Difficulty.HARD, store.state!!.difficulty)
        assertNull(store.state!!.tutorial)

        compose.onNodeWithText("The studio is free").assertExists()
        assertEquals(listOf(Lane.GAMES), store.state!!.lanes)
        screen("02-office")

        tab("Lanes")
        screen("03-studio")
        tab("Office")
        tapInList("Develop a new game")
        compose.waitForIdle()
        overlay("15-designer-concept")
        compose.onNodeWithText("Next").performClick()
        compose.waitForIdle()
        overlay("16-designer-platform")
        compose.onNodeWithText("Next").performClick()
        compose.waitForIdle()
        overlay("17-designer-focus")
        compose.onNodeWithText("Next").performClick()
        compose.waitForIdle()
        overlay("18-designer-summary")
        compose.onNodeWithText("Start development").performClick()
        compose.waitForIdle()
        assertNotNull(store.state!!.games!!.project)
        assertFalse(store.designing)
        compose.onNodeWithContentDescription("Next week").performClick()
        compose.waitForIdle()
        assertEquals(1, store.state!!.week)
        scrollToTop()
        screen("19-in-development")
    }

    @Test
    fun playRunsTheClockUntilSomethingNeedsYou() {
        launch()
        compose.runOnIdle {
            store.newGame("Clockwork", "Sol", Lane.WEBSITES, seed = 8)
            store.speed = Speed.FASTEST
        }
        compose.onNodeWithContentDescription("Play").performClick()
        // The test clock runs the game until it stops by itself: an event, the overdraft, or the end.
        compose.waitForIdle()
        val s = store.state!!
        assertFalse(store.playing)
        assertTrue("week ${s.week}", s.week >= 1)
        assertTrue(s.needsDecision || s.isOver || s.cash < 0)
        overlay("24-stopped-for-you")
    }

    @Test
    fun shipAGameReadTheReviewsAndTheReport() {
        launch()
        compose.runOnIdle {
            store.newGame("Lantern Works", "Ivy", Lane.GAMES, seed = 21)
            store.perform {
                startGame(GameConcept("Moss Knight", Topic.FANTASY, Genre.RPG, platforms = listOf(Platform.PC), size = GameSize.SMALL))
                while (games!!.project?.isReady != true || games!!.project!!.bugs > 2) {
                    advance()
                    while (needsDecision) choose(events[0].choices.size - 1)
                }
            }
        }
        // The office card in polish: the meter label and its note must both stay readable.
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Release", ignoreCase = true))
        compose.waitForIdle()
        screen("19b-office-polish")
        compose.runOnIdle { store.openLane(Lane.GAMES) }
        compose.waitForIdle()
        screen("20-ready-to-ship")
        tapInList("Release Moss Knight")
        compose.waitForIdle()
        val release = store.state!!.games!!.releases.single()
        assertEquals(release.id, store.state!!.games!!.unseenReview)
        compose.onNodeWithText("The reviews are in").assertExists()
        compose.onNodeWithText("Metascore").assertExists()
        compose.onNodeWithText("IGN").assertExists()
        overlay("21-reviews")
        // Why it scored what it did, on the verdict screen.
        assertTrue(release.breakdown.isNotEmpty())
        compose.onNodeWithText("Critics", ignoreCase = true).performScrollTo()
        compose.waitForIdle()
        overlay("21b-why-it-scored")
        compose.onNodeWithText("Game report").performScrollTo().performClick()
        compose.waitForIdle()
        assertNull(store.state!!.games!!.unseenReview)
        assertEquals(release.id, store.openRelease)
        overlay("22-game-report")
        compose.onNodeWithText("Total").performScrollTo()
        compose.waitForIdle()
        overlay("22b-score-breakdown")
    }

    @Test
    fun researchTabShowsTheTree() {
        launch()
        compose.runOnIdle { store.newGame("Lab Rats", "Cy", Lane.GAMES, seed = 22) }
        tab("Research")
        compose.onNodeWithText("Research points").assertExists()
        compose.onNodeWithText("Medium games").assertExists()
        screen("23-research")
    }

    @Test
    fun pivotPointExpandsIntoASecondLaneAndUnlocksItsCombo() {
        launch()
        compose.runOnIdle {
            store.newGame("Moss & Co", "Tess", Lane.GAMES, seed = 7)
            val pilot = Autopilot()
            store.perform { while (pendingPivot == null && !isOver) pilot.playWeek(this) }
        }
        assertNotNull(store.state!!.pendingPivot)
        overlay("04-pivot-point")

        compose.onNodeWithText("Stay focused", substring = true).assertExists()
        compose.onNodeWithTag("pivot-lane-SOCIAL").performScrollTo().performClick()
        compose.onNodeWithText("Expand into Social Media").performClick()
        compose.waitForIdle()

        val state = store.state!!
        assertNull(state.pendingPivot)
        assertEquals(setOf(Lane.GAMES, Lane.SOCIAL), state.lanes.toSet())
        assertNotNull(state.crossover(Combo.GAMES_SOCIAL))

        // Settle whatever else is queued so the tabs are visible.
        compose.runOnIdle { store.perform { while (events.isNotEmpty()) choose(0) } }
        tab("Company")
        screen("25-company-overview")
        compose.onNodeWithText("Combos").performClick()
        compose.waitForIdle()
        compose.onAllNodesWithText("Trending Hub").onFirst().assertExists()
        screen("05-combos")
        compose.onNodeWithText("Money").performClick()
        compose.waitForIdle()
        screen("07-money")
        tab("Team")
        screen("06-team")
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Next: ", substring = true))
        compose.waitForIdle()
        screen("44-next-office")
        tab("Office")
        screen("08-office-two-lanes")
    }

    @Test
    fun goalsChartsLoansAndLevels() {
        launch()
        compose.runOnIdle {
            store.newGame("Long Haul", "Pia", Lane.GAMES, seed = 35)
            val pilot = Autopilot()
            store.perform {
                // Stay a game studio whatever the Pivot Point offers.
                while (week < 130 && !isOver) {
                    if (pendingPivot != null) stayFocused()
                    pilot.playWeek(this)
                }
                while (events.isNotEmpty()) choose(events[0].choices.size - 1)
                acknowledgeReview()
            }
        }
        val s = store.state!!
        assertTrue(s.goalsDone.isNotEmpty())
        assertTrue(s.games!!.market.games.isNotEmpty())
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Goals ·", substring = true, ignoreCase = true))
        compose.waitForIdle()
        screen("26-office-goals")
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Sales chart", substring = true, ignoreCase = true))
        compose.waitForIdle()
        screen("27-office-chart")

        tab("Company")
        compose.onNodeWithText("Goals").performClick()
        compose.waitForIdle()
        compose.onNodeWithText(Goal.G_SHIP.title).assertExists()
        screen("28-goals")
        compose.onNodeWithText("Money").performClick()
        compose.waitForIdle()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Bank loan", ignoreCase = true))
        screen("29-loan")

        tab("Team")
        if (s.staff.isNotEmpty()) compose.onAllNodesWithText("Lv", substring = true).onFirst().assertExists()
        screen("30-team-levels")

        val release = s.games!!.releases.first()
        compose.runOnIdle { store.openRelease = release.id }
        compose.waitForIdle()
        compose.onNodeWithText("Make an expansion pack").performScrollTo()
        overlay("31-after-launch")
    }

    @Test
    fun franchisesComingSoonAndSpinOffs() {
        launch()
        compose.runOnIdle {
            store.newGame("Series Co", "Rae", Lane.GAMES, seed = 22)
            val pilot = Autopilot()
            store.perform {
<<<<<<< HEAD
                while (week < 300 && !isOver) {
=======
                // Stay on Games through the Pivot so franchises can form; games!! NPE'd when Autopilot pivoted away.
                while (week < 300 && !isOver) {
                    while (pendingPivot != null) stayFocused()
>>>>>>> acee0ea (Add start/news/reviews depth, Strategy offices/alliances, and Venture Arm)
                    val g = games ?: break
                    if (g.franchises.isNotEmpty() && !g.busy) break
                    pilot.playWeek(this)
                }
                while (events.isNotEmpty()) choose(events[0].choices.size - 1)
                acknowledgeReview()
            }
            store.openLane(Lane.GAMES)
        }
        val g = store.state!!.games!!
        val f = g.franchises.first()
        assertTrue(f.entries.size >= 2)
        assertTrue(g.market.upcoming.isNotEmpty())
        assertTrue(g.market.games.any { it.franchise != null })
        studio(StudioPage.MARKET)
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Coming soon", ignoreCase = true))
        compose.waitForIdle()
        compose.onNodeWithText(g.market.upcoming.first().title).assertExists()
        screen("40-coming-soon")
        studio(StudioPage.GAMES)
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText(f.name))
        compose.waitForIdle()
        screen("41-franchises")

        compose.onAllNodesWithText("Next").onFirst().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        compose.onNodeWithText("Design a sequel").assertExists()
        compose.onNodeWithText(store.draft!!.title).assertExists()
        overlay("42-sequel")
        val other = Genre.entries.first { it != f.latest.concept.genre }
        compose.onAllNodesWithText(other.title).onFirst().performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Design a spin-off").assertExists()
        compose.onNodeWithText(spinOffTitle(f.name, other)).assertExists()
        compose.onNodeWithContentDescription("Another name").performClick()
        compose.waitForIdle()
        compose.onNodeWithText(spinOffTitle(f.name, other, 1)).assertExists()
        overlay("43-spin-off")
    }

    @Test
    fun goPublicKeepPlayingAndSeeTheStock() {
        launch()
        compose.runOnIdle {
            store.newGame("Tally Apps", "Jin", Lane.APPS, seed = 9)
            store.perform { Autopilot().playToEnd(this) }
        }
        val ending = store.state!!.ending!!
        if (ending.kind != EndingKind.IPO) return
        compose.onNodeWithText("Keep playing", substring = true).assertExists()
        overlay("32-ending-keep-playing")
        compose.onNodeWithText("Keep playing", substring = true).performClick()
        compose.waitForIdle()
        val s = store.state!!
        assertNull(s.ending)
        assertNotNull(s.listing)
        compose.runOnIdle { store.perform { while (events.isNotEmpty()) choose(events[0].choices.size - 1) } }
        screen("33-office-public")
        tab("Company")
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Industry rankings", substring = true, ignoreCase = true))
        compose.waitForIdle()
        screen("34-rankings")
        scrollToTop()
        compose.onNodeWithText("Money").performClick()
        compose.waitForIdle()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Stock", substring = true, ignoreCase = true))
        compose.waitForIdle()
        screen("35-stock")
        tab("Team")
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Perks", substring = true, ignoreCase = true))
        compose.waitForIdle()
        screen("36-perks")
    }

    @Test
    fun hardwareLaneDesignsADevice() {
        launch()
        compose.runOnIdle { store.newGame("Pebble Works", "Ari", Lane.HARDWARE, seed = 3) }
        assertNotNull(store.state!!.venture.board)
        tab("Lanes")
        screen("09-hardware")
        tapInList("Start design")
        compose.waitForIdle()
        assertNotNull(store.state!!.hardware!!.design)
        compose.onNodeWithText("Launch the", substring = true).assertExists()
    }

    @Test
    fun aiLaneStartsATrainingRun() {
        launch()
        compose.runOnIdle { store.newGame("Lumen Labs", "Kai", Lane.AI, seed = 5) }
        tab("Lanes")
        screen("10-ai")
        compose.onAllNodesWithText("Train")[0].performClick()
        compose.waitForIdle()
        assertNotNull(store.state!!.ai!!.training)
    }

    @Test
    fun eventsBlockTheCalendarUntilAnswered() {
        launch()
        compose.runOnIdle {
            store.newGame("Chirp Inc", "Bo", Lane.SOCIAL, seed = 11)
            val pilot = Autopilot()
            store.perform { while (events.isEmpty() && !isOver) pilot.playWeek(this) }
        }
        val event = store.state!!.events.first()
        assertTrue(!store.canAdvance)
        overlay("11-event")
        compose.onNodeWithText(event.choices.first().label).performClick()
        compose.waitForIdle()
        assertTrue(store.state!!.events.none { it.id == event.id })
    }

    @Test
    fun theEndingTellsTheStoryAndStartsOver() {
        launch()
        compose.runOnIdle {
            store.newGame("Tally Apps", "Jin", Lane.APPS, seed = 9)
            store.perform { Autopilot().playToEnd(this) }
        }
        val ending = store.state!!.ending!!
        assertTrue(ending.kind in EndingKind.entries)
        compose.onNodeWithText(ending.title).assertExists()
        overlay("12-ending")
        compose.onNodeWithText("New game").performClick()
        compose.waitForIdle()
        assertNull(store.state)
        compose.onNodeWithText("Start your", substring = true).assertExists()
    }

    @Test
    @Config(qualifiers = "+night")
    fun darkModeUsesTheDarkPalette() {
        launch()
        compose.runOnIdle {
            store.newGame("Night Shift", "Umi", Lane.HARDWARE, seed = 4)
            val pilot = Autopilot()
            store.perform { repeat(60) { if (!isOver && pendingPivot == null) pilot.playWeek(this) } }
            store.perform { while (events.isNotEmpty()) choose(0) }
        }
        compose.onNodeWithText("Your business", ignoreCase = true).assertExists()
        screen("13-office-dark")
        tab("Lanes")
        screen("14-hardware-dark")
    }

    @Test
    fun studiosLiveGamesAndAchievements() {
        // A rich studio with a live game, written as the save the app opens with.
        val base = GameState.create("Empire Co", "Rae", Lane.GAMES, seed = 61)
        val s = GameState.decode(
            base.encode()
                .replace(Regex("\"cash\":[-0-9.Ee+]+"), "\"cash\":5.0E9")
                .replace(Regex("\"researchPoints\":[-0-9.Ee+]+"), "\"researchPoints\":100000.0"),
        )
        listOf(Research.MULTIPLAYER, Research.ONLINE_SERVICES, Research.LIVE_SERVICE, Research.MEDIUM_GAMES).forEach { s.research(it) }
        s.acquire("Team Cherry")
        repeat(10) {
            s.advance()
            while (s.needsDecision) s.choose(s.events[0].choices.size - 1)
        }
        val g = s.games!!
        val concept = GameConcept("Skyforge Online", Topic.FANTASY, Genre.RPG, platforms = listOf(Platform.PC), size = GameSize.MEDIUM, live = true)
        g.releases.add(
            0,
            GameRelease(
                id = 900, title = concept.title, concept = concept, week = s.week - 12, reviews = emptyList(), score = 8.2, weeklySales = 40_000.0,
                netShare = 0.7, design = 300.0, tech = 200.0, bugs = 10.0, hype = 20.0, devCost = 200_000.0, report = emptyList(),
                live = LiveService(240_000.0, s.week - 12),
            ),
        )
        File(RuntimeEnvironment.getApplication().filesDir, "autosave.json").writeText(s.encode())
        launch()
        assertEquals("Empire Co", store.state?.companyName)
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Achievements", substring = true, ignoreCase = true))
        compose.waitForIdle()
        screen("49-office-empire")

        compose.runOnIdle { store.openLane(Lane.GAMES) }
        compose.waitForIdle()
        studio(StudioPage.MARKET)
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Buy a studio", ignoreCase = true))
        compose.waitForIdle()
        compose.onNodeWithText("Team Cherry").assertExists()
        screen("45-studios")
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Buy"))
        compose.waitForIdle()
        compose.onAllNodesWithText("Buy")[0].performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        val dialogButtons = compose.onAllNodesWithText("Buy")
        dialogButtons[dialogButtons.fetchSemanticsNodes().size - 1].performClick()
        compose.waitForIdle()
        assertEquals(2, store.state!!.games!!.subsidiaries.size)

        studio(StudioPage.GAMES)
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Live games", ignoreCase = true))
        compose.waitForIdle()
        screen("46-live-games")
        compose.runOnIdle { store.openRelease = 900 }
        compose.waitForIdle()
        val cost = store.state!!.games!!.release(900)!!.seasonCost()
        compose.onNodeWithText(Format.money(cost)).performScrollTo()
        compose.waitForIdle()
        overlay("47-live-service")
        compose.onNodeWithText(Format.money(cost)).performClick()
        compose.waitForIdle()
        assertEquals(1, store.state!!.games!!.release(900)!!.live!!.seasons)
        compose.runOnIdle { store.openRelease = null }
        compose.waitForIdle()

        tab("Company")
        compose.onNodeWithText("Goals").performClick()
        compose.waitForIdle()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Achievements ·", substring = true, ignoreCase = true))
        compose.waitForIdle()
        assertTrue(Achievement.FIRST_STUDIO in store.state!!.achievements)
        screen("48-achievements")
    }

    @Test
    fun twoTeamsMakeTwoGames() {
        val base = GameState.create("Twin Studio", "Ada", Lane.GAMES, seed = 62)
        val s = GameState.decode(
            base.encode()
                .replace(Regex("\"cash\":[-0-9.Ee+]+"), "\"cash\":5.0E7")
                .replace(Regex("\"researchPoints\":[-0-9.Ee+]+"), "\"researchPoints\":100000.0"),
        )
        while (s.office < Office.STUDIO) s.moveOffice()
        listOf(Research.MEDIUM_GAMES, Research.LARGE_GAMES, Research.SECOND_TEAM).forEach { s.research(it) }
        var guard = 0
        while (s.gameTeam < 8 && guard++ < 60) {
            s.candidates.toList().forEach { if (s.gameTeam < 8) runCatching { s.hire(it.id, Lane.GAMES) } }
            s.advance()
            while (s.needsDecision) s.choose(s.events[0].choices.size - 1)
        }
        s.startGame(GameConcept("Moss Knight", Topic.FANTASY, Genre.RPG, platforms = listOf(Platform.PC), size = GameSize.MEDIUM))
        File(RuntimeEnvironment.getApplication().filesDir, "autosave.json").writeText(s.encode())
        launch()
        compose.runOnIdle { store.openLane(Lane.GAMES) }
        compose.waitForIdle()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Design a game for Team 2"))
        compose.waitForIdle()
        screen("57-team-two-free")
        tapInList("Design a game for Team 2")
        assertTrue(store.designing)
        assertEquals(2, store.designTeam)
        repeat(3) {
            compose.onNodeWithText("Next").performClick()
            compose.waitForIdle()
        }
        compose.onNodeWithText("Start development").performClick()
        compose.waitForIdle()
        val g = store.state!!.games!!
        assertNotNull(g.project2)
        assertEquals("Moss Knight", g.project!!.title)
        compose.runOnIdle { store.advance(2) }
        compose.waitForIdle()
        assertTrue(store.state!!.games!!.project2!!.progress > 0)
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Team 2 · In development", substring = true, ignoreCase = true))
        compose.waitForIdle()
        screen("55-team-two")
        tab("Office")
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Game studio", ignoreCase = true))
        compose.waitForIdle()
        screen("56-office-two-games")
    }

    @Test
    fun yourLabelPublishesAndOldHitsGetRemastered() {
        val base = GameState.create("Lantern Label", "Ivy", Lane.GAMES, seed = 64)
        val s = GameState.decode(
            base.encode()
                .replace(Regex("\"cash\":[-0-9.Ee+]+"), "\"cash\":5.0E7")
                .replace(Regex("\"researchPoints\":[-0-9.Ee+]+"), "\"researchPoints\":100000.0"),
        )
        listOf(Research.MARKETING, Research.MEDIUM_GAMES, Research.SEQUELS, Research.REMASTERS, Research.PUBLISHING).forEach { s.research(it) }
        val classic = GameConcept("Moss Knight", Topic.FANTASY, Genre.RPG, platforms = listOf(Platform.PC), size = GameSize.SMALL)
        s.games!!.releases.add(
            0,
            GameRelease(
                id = 901, title = classic.title, concept = classic, week = s.week - 200, reviews = emptyList(), score = 8.7, weeklySales = 0.0,
                netShare = 0.7, design = 60.0, tech = 40.0, bugs = 2.0, hype = 20.0, devCost = 9_000.0, report = emptyList(),
                lifetimeSales = 400_000.0, unitsSold = 40_000.0,
            ),
        )
        var guard = 0
        while (s.games!!.pitches.size < 2 && guard++ < 60) {
            s.advance()
            while (s.needsDecision) if (s.pendingPivot != null) s.stayFocused() else s.choose(s.events[0].choices.size - 1)
        }
        File(RuntimeEnvironment.getApplication().filesDir, "autosave.json").writeText(s.encode())
        launch()
        compose.runOnIdle { store.openLane(Lane.GAMES) }
        compose.waitForIdle()
        studio(StudioPage.MARKET)
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Pitches", ignoreCase = true))
        compose.waitForIdle()
        screen("58-label-pitches")

        val pitch = store.state!!.games!!.pitches.first()
        tapInList(pitch.title)
        overlay("59-pitch")
        compose.onNodeWithText("Sign").performClick()
        compose.waitForIdle()
        val g = store.state!!.games!!
        assertEquals(pitch.id, g.signed.single().pitch.id)

        // Fast-forward to its release.
        compose.runOnIdle {
            store.perform { games!!.signed.single().weeksLeft = 1 }
            store.advance(1)
        }
        compose.waitForIdle()
        assertEquals(pitch.title, store.state!!.games!!.published.single().title)
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Published", substring = true, ignoreCase = true))
        compose.waitForIdle()
        screen("60-label-published")

        // An eight-year-old hit, ready for a remaster.
        studio(StudioPage.MAKE)
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Ready to remaster", ignoreCase = true))
        compose.waitForIdle()
        compose.onAllNodesWithText("Remaster")[0].performClick()
        compose.waitForIdle()
        assertTrue(store.designing)
        assertEquals(901, store.draft?.remasterOf)
        compose.onNodeWithText("Design a remaster").assertExists()
        overlay("61-remaster-designer")
        repeat(3) {
            compose.onNodeWithText("Next").performClick()
            compose.waitForIdle()
        }
        compose.onNodeWithText("Start development").performClick()
        compose.waitForIdle()
        assertEquals("Moss Knight Remastered", store.state!!.games!!.project!!.title)
    }

    @Test
    fun esportsLeagueForACompetitiveHit() {
        val base = GameState.create("Arena Works", "Kim", Lane.GAMES, seed = 66)
        val s = GameState.decode(
            base.encode()
                .replace(Regex("\"cash\":[-0-9.Ee+]+"), "\"cash\":5.0E7")
                .replace(Regex("\"researchPoints\":[-0-9.Ee+]+"), "\"researchPoints\":100000.0"),
        )
        listOf(Research.MULTIPLAYER, Research.ONLINE_SERVICES, Research.ESPORTS).forEach { s.research(it) }
        val g0 = s.games!!
        g0.engines += Engine(500, "Netcode", listOf(Research.MULTIPLAYER), s.week)
        val arena = GameConcept("Arena Kings", Topic.SPORTS, Genre.ACTION, platforms = listOf(Platform.PC), size = GameSize.LARGE, engineId = 500)
        g0.releases.add(
            0,
            GameRelease(
                id = 902, title = arena.title, concept = arena, week = s.week - 20, reviews = emptyList(), score = 8.6, weeklySales = 200_000.0,
                netShare = 0.7, design = 900.0, tech = 800.0, bugs = 10.0, hype = 40.0, devCost = 2e6, report = emptyList(),
                lifetimeSales = 3e7, unitsSold = 2e6,
            ),
        )
        File(RuntimeEnvironment.getApplication().filesDir, "autosave.json").writeText(s.encode())
        launch()
        compose.runOnIdle { store.openLane(Lane.GAMES) }
        compose.waitForIdle()
        studio(StudioPage.GAMES)
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Start a league", ignoreCase = true))
        compose.waitForIdle()
        screen("62-start-a-league")
        tapInList("Start")
        val league = store.state!!.games!!.release(902)!!.league
        assertNotNull(league)
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText(league!!.name))
        compose.waitForIdle()
        tapInList("Major")
        assertEquals(PrizePool.MAJOR, store.state!!.games!!.release(902)!!.league!!.prize)
        // Skip ahead to the championship.
        compose.runOnIdle {
            store.perform { games!!.release(902)!!.league!!.nextFinal = week + 1 }
            store.advance(2)
        }
        compose.waitForIdle()
        val after = store.state!!.games!!.release(902)!!.league!!
        assertEquals(1, after.finals)
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText(after.name))
        compose.waitForIdle()
        screen("63-esports")
    }

    @Test
    fun hallOfFameTraitsAndRoadmaps() {
        // Three earlier runs: one went public, one went bust.
        val earlier = listOf(
            RunRecord("Moss & Co", "Ivy", Lane.GAMES, EndingKind.IPO, "The Moss IPO", 300, 2.4e9, 6e8, false, Difficulty.NORMAL, 22, seed = 1),
            RunRecord("Pebble Works", "Ivy", Lane.HARDWARE, EndingKind.BANKRUPT, "Out of Stock", 140, 3e6, 0.0, false, Difficulty.HARD, 6, seed = 2),
            RunRecord("Tally Apps", "Ivy", Lane.APPS, EndingKind.INDIE, "Small and Mighty", 780, 9e7, 9e7, false, Difficulty.EASY, 15, seed = 3),
        )
        File(RuntimeEnvironment.getApplication().filesDir, "hall.json").writeText(encodeHall(earlier))
        launch()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Hall of Fame ·", substring = true))
        compose.waitForIdle()
        compose.onNodeWithText("Moss & Co").assertExists()
        screen("64-hall-of-fame")

        compose.runOnIdle { store.newGame("Lantern Web", "Ivy", Lane.WEBSITES, seed = 5) }
        compose.waitForIdle()
        val s = store.state!!
        // Been here before, IPO veteran, survivor and billion-dollar founder.
        assertEquals(Difficulty.NORMAL.startingCash + 10_000, s.cash, 1.0)
        assertTrue(s.candidates.any { it.skill >= 8 })
        assertTrue(Perk.COFFEE in s.perks)

        compose.runOnIdle { store.perform { hire(candidates.first { it.trait != null }.id, Lane.WEBSITES) } }
        compose.waitForIdle()
        val hired = store.state!!.staff.single()
        tab("Team")
        compose.onAllNodesWithText(hired.trait!!.title, substring = true).onFirst().assertExists()
        screen("65-traits")

        tab("Lanes")
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Roadmap", substring = true, ignoreCase = true))
        compose.waitForIdle()
        compose.onAllNodesWithText("Build")[0].performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertEquals(Feature.CASE_STUDIES, store.state!!.building.single().feature)
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Roadmap", substring = true, ignoreCase = true))
        compose.waitForIdle()
        screen("66-roadmap")
    }

    @Test
    fun theDailyByteAndTheEra() {
        launch()
        compose.runOnIdle {
            store.newGame("Night Desk", "Ivy", Lane.GAMES, seed = 12)
            val pilot = Autopilot()
            store.perform {
                repeat(140) { if (pendingPivot != null) stayFocused(); pilot.playWeek(this) }
                if (pendingPivot != null) stayFocused()
                while (events.isNotEmpty()) choose(events[0].choices.size - 1)
                acknowledgeReview()
            }
        }
        compose.waitForIdle()
        assertTrue(store.state!!.news.isNotEmpty())
        compose.runOnIdle { store.tab = Tab.NEWS }
        compose.waitForIdle()
        compose.onNodeWithText("The Daily Byte").assertExists()
        screen("67-news-front")
        assertEquals(store.state!!.news.last().id, store.newsSeen)
        compose.onNodeWithText("Industry").performClick()
        compose.waitForIdle()
        screen("68-news-industry")
        // The desks scroll sideways; the last ones start off-screen.
        val chips = compose.onNodeWithTag("desks")
        chips.performScrollToNode(hasText("Markets"))
        compose.onNodeWithText("Markets").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("The biggest companies", ignoreCase = true).assertExists()
        screen("68b-news-markets")
        chips.performScrollToNode(hasText("Front page"))
        compose.onNodeWithText("Front page").performClick()
        compose.waitForIdle()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToIndex(4)
        compose.waitForIdle()
        screen("68c-news-latest")
        compose.runOnIdle {
            store.companyPage = 0
            store.tab = Tab.COMPANY
        }
        compose.waitForIdle()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Demand this era", ignoreCase = true))
        compose.waitForIdle()
        screen("69-era")
    }

    @Test
    fun decadesStartsInThePastAndPlaysThroughHistory() {
        launch()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("1985: The console wars"))
        compose.onNodeWithText("1985: The console wars").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        // The web hasn't been invented yet.
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Websites"))
        compose.onNodeWithText("From 1994").assertExists()
        compose.onNodeWithText("Websites").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("When", ignoreCase = true))
        screen("70-decades-new-game")
        compose.onNodeWithText("Start your game studio").performClick()
        compose.waitForIdle()
        val s = store.state!!
        assertEquals(1985, s.startYear)
        assertEquals(Lane.GAMES, s.lanes.single())
        assertEquals("Jan 1985", s.dateLabel)
        assertEquals(Era.CONSOLE_WARS, s.world.era)
        compose.runOnIdle {
            val pilot = Autopilot()
            store.perform {
                repeat(52 * 3) { if (pendingPivot != null) stayFocused(); pilot.playWeek(this) }
                if (pendingPivot != null) stayFocused()
                while (events.isNotEmpty()) choose(events[0].choices.size - 1)
                acknowledgeReview()
            }
        }
        compose.waitForIdle()
        assertEquals(1988, store.state!!.calendarYear)
        assertEquals(Era.CONSOLE_WARS, store.state!!.world.era)
        assertTrue(store.state!!.games!!.market.games.any { it.title == "Super Mario Bros." || it.title == "The Legend of Zelda" })
        compose.runOnIdle { store.tab = Tab.NEWS }
        compose.waitForIdle()
        screen("71-decades-news")
        studio(StudioPage.TECH)
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Platforms", ignoreCase = true))
        compose.onNodeWithText("NES").assertExists()
        screen("72-decades-platforms")
        studio(StudioPage.MARKET)
        scrollToTop()
        screen("73-decades-market")
        compose.runOnIdle {
            store.companyPage = 0
            store.tab = Tab.COMPANY
        }
        compose.waitForIdle()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("From 2023"))
        compose.waitForIdle()
        screen("74-decades-timeline")
        tab("Research")
        scrollToTop()
        compose.onNodeWithText("Only in the 8-bit console wars", substring = true, ignoreCase = true).assertExists()
        screen("74b-decades-research")
        compose.onNodeWithText("Engine").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Not invented until", substring = true))
        compose.waitForIdle()
        screen("74c-decades-engine-research")
    }

    @Test
    fun rivalsTheInboxAndDeeperBusinesses() {
        launch()
        compose.runOnIdle {
            store.newGame("Rival Co", "Ada", Lane.WEBSITES, seed = 31)
            store.perform {
                Sandbox.setCash(this, 2_000_000.0)
                Sandbox.offer(this, com.boot.core.Interview("Wired"), 3)
                Sandbox.offer(this, com.boot.core.RetainerOffer("Kettle Cafe", 600.0, 1.5), 4)
            }
        }
        compose.waitForIdle()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Decline"))
        compose.waitForIdle()
        screen("75-inbox")
        compose.onAllNodesWithText("Choose")[0].performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertEquals(1, store.state!!.inbox.size)

        // A rival strikes, as a decision.
        compose.runOnIdle {
            store.perform { Sandbox.enqueue(this, com.boot.core.RivalMove("Squarespace", Lane.WEBSITES, com.boot.core.RivalMoveKind.PRICE_WAR)) }
        }
        compose.waitForIdle()
        overlay("76-rival-move")
        compose.onNodeWithText("Hold your prices").performClick()
        compose.waitForIdle()
        assertTrue(store.state!!.events.isEmpty())

        tab("Lanes")
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Attack ads"))
        compose.waitForIdle()
        screen("77-rival-card")
        compose.onAllNodesWithText("Go")[0].performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertTrue(store.state!!.world.pressures.any { it.mine })

        // The app studio's new levers.
        compose.runOnIdle {
            store.newGame("App Co", "Ada", Lane.APPS, seed = 32)
            store.perform {
                Sandbox.setCash(this, 2_000_000.0)
                apps!!.isLive = true
                apps!!.users = 40_000.0
            }
            store.tab = Tab.LANES
        }
        compose.waitForIdle()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Experiments", ignoreCase = true))
        compose.waitForIdle()
        screen("78-app-experiments")
        compose.onAllNodesWithText("Run")[0].performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertNotNull(store.state!!.apps!!.experiment)
    }

    @Test
    fun scenariosWinMedalsAndSandboxHasNoLimits() {
        launch()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Scenarios"))
        compose.onNodeWithText("Scenarios").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Survive the Bust"))
        compose.onNodeWithText("Survive the Bust").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        screen("79-scenarios")
        compose.onNodeWithText("Play Survive the Bust").performClick()
        compose.waitForIdle()
        val run = store.state!!.scenario!!
        assertEquals(com.boot.core.Scenario.SURVIVE_THE_BUST, run.scenario)
        assertEquals(2000, store.state!!.startYear)
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Gold by", substring = true))
        compose.waitForIdle()
        screen("80-scenario-card")

        // Win it, and decide whether to keep going.
        compose.runOnIdle {
            store.perform {
                scenario!!.medal = com.boot.core.Medal.GOLD
                Sandbox.enqueue(this, com.boot.core.ScenarioOver(won = true))
            }
        }
        compose.waitForIdle()
        overlay("81-scenario-won")
        compose.onNodeWithText("Keep playing").performClick()
        compose.waitForIdle()
        assertFalse(store.state!!.isOver)

        // Sandbox: a billion dollars, and you can't go bust.
        compose.runOnIdle { store.newGame("Toy Co", "Ada", Lane.HARDWARE, sandbox = true) }
        compose.waitForIdle()
        assertTrue(store.state!!.sandbox)
        assertTrue(store.state!!.cash >= 900_000_000.0)
        scrollToTop()
        screen("82-sandbox")
    }

    @Test
    fun rosaShowsTheRopesAndTheGuideExplains() {
        launch()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Show me the ropes"))
        compose.waitForIdle()
        screen("83-new-game-tutorial")
        compose.onNodeWithText("Start your game studio").performClick()
        compose.waitForIdle()
        assertNotNull(store.state!!.tutorial)

        val list = compose.onAllNodes(hasScrollAction()).onFirst()
        list.performScrollToNode(hasText("Let's go"))
        compose.onNodeWithText("Welcome to Garage Labs").assertExists()
        screen("84-lesson-welcome")
        compose.onNodeWithText("Let's go").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()

        // Make a game: the lesson's button opens the designer.
        list.performScrollToNode(hasText("Develop a game"))
        compose.onNodeWithText("Make your first game").assertExists()
        compose.onNodeWithText("Develop a game").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertTrue(store.designing)
        repeat(3) {
            compose.onNodeWithText("Next").performClick()
            compose.waitForIdle()
        }
        compose.onNodeWithText("Start development").performClick()
        compose.waitForIdle()
        assertEquals(TutorialStep.PLAY, store.state!!.lesson()!!.step)
        list.performScrollToNode(hasText("Play a week"))
        compose.waitForIdle()
        screen("85-lesson-play")
        compose.onNodeWithText("Play a week").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertEquals(1, store.state!!.week)
        assertEquals(TutorialStep.HIRE, store.state!!.lesson()!!.step)

        // How to play, from the menu.
        compose.onNodeWithContentDescription("More", substring = true).performClick()
        compose.waitForIdle()
        compose.onNodeWithText("How to play").performClick()
        compose.waitForIdle()
        assertTrue(store.guideOpen)
        overlay("86-guide")
        compose.onNodeWithText("A studio in the Game Dev Tycoon mould", substring = true).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        compose.onNodeWithText("Design a game", substring = true).assertExists()
        overlay("87-guide-games")
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithContentDescription("Close").performClick()
        compose.waitForIdle()
        assertFalse(store.guideOpen)

        // Skipping ends the lessons.
        list.performScrollToNode(hasText("Skip tutorial"))
        compose.onNodeWithText("Skip tutorial").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertNull(store.state!!.tutorial)
    }

    @Test
    fun autosaveSurvivesARestart() {
        launch()
        compose.runOnIdle {
            store.newGame("Saved Co", "Wren", Lane.WEBSITES, seed = 2)
            store.advance(4)
        }
        val week = store.state!!.week
        // The save is written off the main thread; give it a moment.
        val file = File(RuntimeEnvironment.getApplication().filesDir, "autosave.json")
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline && !(file.exists() && file.readText().contains("\"week\":$week"))) {
            Thread.sleep(20)
        }
        val reloaded = GameStore(RuntimeEnvironment.getApplication())
        assertEquals("Saved Co", reloaded.state?.companyName)
        assertEquals(week, reloaded.state?.week)
    }
}
