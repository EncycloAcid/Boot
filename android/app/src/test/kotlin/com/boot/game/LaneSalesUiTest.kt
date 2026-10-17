package com.boot.game

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import com.boot.core.Lane
import com.boot.core.Sandbox
import com.boot.core.advance
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

/** Selling a business from its page in the Lanes tab. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LaneSalesUiTest {
    @get:Rule(order = 0)
    val display = DisplayRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private fun save(name: String, top: Boolean = false) {
        compose.waitForIdle()
        val roots = compose.onAllNodes(androidx.compose.ui.test.isRoot())
        val index = if (top) roots.fetchSemanticsNodes().size - 1 else 0
        val view = (roots[index].fetchSemanticsNode().root as ViewRootForTest).view
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File(DisplayRule.screenshots, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-xxhdpi")
    fun sellTheAgency() {
        val store = GameStore(RuntimeEnvironment.getApplication())
        store.clearHall()
        compose.setContent { BootTheme { BootApp(store) } }
        compose.runOnIdle {
            store.newGame("Pixel Forge", "You", Lane.GAMES, seed = 9)
            store.perform {
                Sandbox.setCash(this, 500_000.0)
                Sandbox.openLane(this, Lane.WEBSITES)
                weeks(4)
            }
            store.todayOpen = false
            store.lane = Lane.WEBSITES
            store.tab = Tab.LANES
        }
        compose.waitForIdle()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Keep only this one"))
        compose.waitForIdle()
        save("99ar-sell-or-focus")
        val cash = store.state!!.cash
        compose.onAllNodesWithText("Sell").onFirst().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        save("99as-sell-confirm", top = true)
        compose.onAllNodesWithText("Sell for", substring = true).onFirst().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertEquals(listOf(Lane.GAMES), store.state!!.lanes)
        assertTrue(store.state!!.cash > cash)
    }
}
