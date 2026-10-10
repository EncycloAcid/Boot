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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import com.boot.core.Autopilot
import com.boot.core.Combo
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
    @get:Rule
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
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun screen(name: String) = compose.onRoot().save(name)

    /** The topmost window: a sheet or full-screen page above the game. */
    private fun overlay(name: String) {
        val roots = compose.onAllNodes(isRoot())
        roots[roots.fetchSemanticsNodes().size - 1].save(name)
    }

    /** Scroll a button into view, then tap it. */
    private fun tapInList(text: String) {
        compose.onNodeWithText(text).performScrollTo().performClick()
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
        compose.onNodeWithText("Hardware").performClick()
        compose.onNodeWithText("Hard mode", substring = true).assertExists()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Hard"))
        compose.onNodeWithText("Hard").performClick()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Games"))
        compose.onNodeWithText("Games").performClick()
        compose.onNodeWithText("Start your game studio").performClick()
        assertEquals(Difficulty.HARD, store.state!!.difficulty)

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
            store.openLane(Lane.GAMES)
        }
        screen("20-ready-to-ship")
        tapInList("Release Moss Knight")
        compose.waitForIdle()
        val release = store.state!!.games!!.releases.single()
        assertEquals(release.id, store.state!!.games!!.unseenReview)
        compose.onNodeWithText("THE REVIEWS ARE IN").assertExists()
        compose.onNodeWithText("Metascore").assertExists()
        compose.onNodeWithText("IGN").assertExists()
        overlay("21-reviews")
        compose.onNodeWithText("Game report").performScrollTo().performClick()
        compose.waitForIdle()
        assertNull(store.state!!.games!!.unseenReview)
        assertEquals(release.id, store.openRelease)
        overlay("22-game-report")
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
        compose.onNodeWithTag("pivot-lane-SOCIAL").performClick()
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
        tab("Office")
        screen("08-office-two-lanes")
    }

    @Test
    fun goalsChartsLoansAndLevels() {
        launch()
        compose.runOnIdle {
            store.newGame("Long Haul", "Pia", Lane.GAMES, seed = 31)
            val pilot = Autopilot()
            store.perform {
                while (week < 130 && !isOver) pilot.playWeek(this)
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
            store.newGame("Series Co", "Rae", Lane.GAMES, seed = 21)
            val pilot = Autopilot()
            store.perform {
                while (week < 300 && !isOver && (games!!.franchises.isEmpty() || games!!.busy)) pilot.playWeek(this)
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
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Coming soon", ignoreCase = true))
        compose.waitForIdle()
        compose.onNodeWithText(g.market.upcoming.first().title).assertExists()
        screen("40-coming-soon")
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText(f.name))
        compose.waitForIdle()
        screen("41-franchises")

        compose.onAllNodesWithText("Next").onFirst().performClick()
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
        compose.onNodeWithText("Start design").performClick()
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
