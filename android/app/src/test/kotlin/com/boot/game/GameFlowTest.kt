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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.boot.core.Autopilot
import com.boot.core.Combo
import com.boot.core.EndingKind
import com.boot.core.Lane
import com.boot.core.choose
import com.boot.game.ui.BootApp
import com.boot.game.ui.BootTheme
import org.junit.Assert.assertEquals
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

    private fun tab(label: String) {
        compose.onNodeWithText(label).performClick()
        compose.waitForIdle()
    }

    @Test
    fun startASoloStudioAndPlayAWeek() {
        launch()
        screen("01-new-game")

        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Hardware"))
        compose.onNodeWithText("Hardware").performClick()
        compose.onNodeWithText("Hard mode", substring = true).assertExists()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Games"))
        compose.onNodeWithText("Games").performClick()
        compose.onNodeWithText("Start your game studio").performClick()

        compose.onNodeWithText("Your lanes", ignoreCase = true).assertExists()
        assertEquals(listOf(Lane.GAMES), store.state!!.lanes)
        screen("02-dashboard")

        tab("Lanes")
        compose.onAllNodesWithText("Start")[0].performClick()
        assertNotNull(store.state!!.games!!.project)
        compose.onNodeWithText("Next week").performClick()
        compose.waitForIdle()
        assertEquals(1, store.state!!.week)
        screen("03-games-lane")
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

        compose.onNodeWithTag("pivot-lane-SOCIAL").performClick()
        compose.onNodeWithText("Expand into Social Media").performClick()
        compose.waitForIdle()

        val state = store.state!!
        assertNull(state.pendingPivot)
        assertEquals(setOf(Lane.GAMES, Lane.SOCIAL), state.lanes.toSet())
        assertNotNull(state.crossover(Combo.GAMES_SOCIAL))

        // Settle whatever else is queued so the tabs are visible.
        compose.runOnIdle { store.perform { while (events.isNotEmpty()) choose(0) } }
        tab("Combos")
        compose.onAllNodesWithText("Trending Hub").onFirst().assertExists()
        screen("05-combos")
        tab("People")
        screen("06-people")
        tab("Money")
        screen("07-money")
        tab("Home")
        screen("08-dashboard-two-lanes")
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
        compose.onNodeWithText("Your lanes", ignoreCase = true).assertExists()
        screen("13-dashboard-dark")
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
