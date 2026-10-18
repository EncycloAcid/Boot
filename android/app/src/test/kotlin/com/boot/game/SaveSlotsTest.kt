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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Three games on the go at once: start, leave, continue, delete and replace. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SaveSlotsTest {
    @get:Rule(order = 0)
    val display = DisplayRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private fun save(name: String) {
        compose.waitForIdle()
        val view = (compose.onAllNodes(androidx.compose.ui.test.isRoot())[0].fetchSemanticsNode().root as ViewRootForTest).view
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File(DisplayRule.screenshots, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun scrollTo(text: String) {
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(text))
    }

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-xxhdpi")
    fun threeGamesOnTheGo() {
        val app = RuntimeEnvironment.getApplication()
        val store = GameStore(app)
        store.clearHall()
        compose.setContent { BootTheme { BootApp(store) } }
        compose.waitForIdle()
        assertEquals(listOf(null, null, null), store.slots)
        assertEquals(1, store.slot)

        // A game in slot 1, a week in, then back to the title screen.
        compose.runOnIdle {
            store.newGame("Garage Labs", "You", Lane.GAMES, seed = 3)
            store.advance(1)
            store.leaveToTitle()
        }
        compose.waitForIdle()
        assertNull(store.state)
        assertEquals("Garage Labs", store.slots[0]?.company)
        // The next new game goes into the empty slot 2, not over slot 1.
        assertEquals(2, store.slot)
        scrollTo("Save slots")
        save("80-save-slots")

        // Start one through the title screen: it lands in slot 2.
        compose.runOnIdle { store.newGame("Pixel Forge", "You", Lane.APPS, seed = 5) }
        compose.waitForIdle()
        assertEquals(2, store.slot)
        compose.runOnIdle { store.leaveToTitle() }
        compose.waitForIdle()
        assertEquals("Pixel Forge", store.slots[1]?.company)
        assertEquals(3, store.slot)

        // Carry on with slot 1 where it was left.
        scrollTo("Garage Labs")
        compose.onAllNodesWithText("Continue")[0].performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertEquals("Garage Labs", store.state?.companyName)
        assertEquals(1, store.state?.week)
        assertEquals(1, store.slot)

        // Coming back to the app opens the same game, and remembers the others.
        val again = GameStore(app)
        assertEquals(1, again.slot)
        assertEquals("Garage Labs", again.state?.companyName)
        assertEquals("Pixel Forge", again.slots[1]?.company)

        // Delete slot 2, with a confirmation.
        compose.runOnIdle { store.leaveToTitle() }
        compose.waitForIdle()
        scrollTo("Pixel Forge")
        compose.onNodeWithContentDescription("Delete slot 2").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithText("Delete").performClick()
        compose.waitForIdle()
        assertNull(store.slots[1])
        assertNotNull(store.slots[0])

        // Starting over a filled slot asks before replacing it.
        compose.runOnIdle { store.chooseSlot(1) }
        compose.waitForIdle()
        // Sideways, the start button sits in the fixed panel on the left, with a note of where it saves.
        compose.onNodeWithText("Saves to slot 1, replacing Garage Labs.").fetchSemanticsNode()
        compose.onNodeWithText("Start your game studio").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        compose.onNodeWithText("Replace").performClick()
        compose.waitForIdle()
        assertEquals(1, store.slot)
        assertEquals(0, store.state?.week)
    }
}
