package com.boot.game

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import com.boot.core.Lane
import com.boot.game.ui.BootApp
import com.boot.game.ui.BootTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Sideways (the default) and upright with the classic layout. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LayoutModesTest {
    @get:Rule(order = 0)
    val display = DisplayRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private lateinit var store: GameStore

    private fun launch() {
        store = GameStore(RuntimeEnvironment.getApplication())
        store.clearHall()
        compose.setContent { BootTheme { BootApp(store) } }
        compose.waitForIdle()
    }

    private fun save(name: String) {
        compose.waitForIdle()
        val view = (compose.onAllNodes(androidx.compose.ui.test.isRoot())[0].fetchSemanticsNode().root as ViewRootForTest).view
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File(DisplayRule.screenshots, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-xxhdpi")
    fun sidewaysIsTheDefault() {
        launch()
        assertEquals(ScreenLayout.SIDEWAYS, store.layout)
        save("90-sideways-new-game")
        compose.runOnIdle { store.newGame("Garage Labs", "You", Lane.GAMES, seed = 3) }
        compose.waitForIdle()
        compose.runOnIdle { store.todayOpen = false }
        save("91-sideways-office")
        compose.runOnIdle { store.todayOpen = true }
        save("92-sideways-today")
        compose.runOnIdle {
            store.todayOpen = false
            store.tab = Tab.LANES
        }
        save("93-sideways-lanes")
        // Play stays reachable with a window open.
        compose.onNodeWithContentDescription("Play").performClick()
        compose.waitForIdle()
        assertTrue(store.state!!.week >= 1)
        compose.runOnIdle { store.tab = Tab.COMPANY }
        save("94-sideways-company")
    }

    @Test
    @Config(qualifiers = "w393dp-h852dp-xxhdpi")
    fun uprightUsesTheClassicLayout() {
        launch()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Upright"))
        compose.onNodeWithText("Upright").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertEquals(ScreenLayout.VERTICAL, store.layout)
        compose.runOnIdle { store.newGame("Garage Labs", "You", Lane.GAMES, seed = 3) }
        compose.waitForIdle()
        // The office is on the home page, and the tabs run along the bottom.
        compose.onAllNodesWithText("Cramped apartment")[0].assertExistsCompat()
        save("95-upright-office")
        compose.onNodeWithText("Lanes").performClick()
        compose.waitForIdle()
        assertEquals(Tab.LANES, store.tab)
        save("96-upright-lanes")

        // The choice is remembered, and the menu switches back.
        assertEquals(ScreenLayout.VERTICAL, GameStore(RuntimeEnvironment.getApplication()).layout)
        compose.onNodeWithContentDescription("Menu", substring = true).performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Play sideways").performClick()
        compose.waitForIdle()
        assertEquals(ScreenLayout.SIDEWAYS, store.layout)
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertExistsCompat() = fetchSemanticsNode()
}
