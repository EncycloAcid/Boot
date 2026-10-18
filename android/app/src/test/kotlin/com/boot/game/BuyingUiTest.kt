package com.boot.game

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import com.boot.core.Lane
import com.boot.core.Sandbox
import com.boot.core.advance
import com.boot.core.stakeIn
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

/** Buying a stake in a studio, offering an earn-out for the rest, and the holdings summary. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BuyingUiTest {
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

    private fun tap(text: String, substring: Boolean = false) =
        compose.onAllNodesWithText(text, substring = substring).onFirst().performSemanticsAction(SemanticsActions.OnClick)

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-xxhdpi")
    fun stakeThenEarnOut() {
        val store = GameStore(RuntimeEnvironment.getApplication())
        store.clearHall()
        compose.setContent { BootTheme { BootApp(store) } }
        compose.runOnIdle {
            store.newGame("Pixel Forge", "You", Lane.GAMES, seed = 9)
            store.perform {
                Sandbox.setCash(this, 2_000_000_000.0)
                repeat(4) { advance() }
            }
            store.todayOpen = false
            store.dealStudio = "Team Cherry"
        }
        compose.waitForIdle()
        compose.onNodeWithTag("stake", useUnmergedTree = true).performScrollTo()
        tap("Buy 20%", substring = true)
        compose.waitForIdle()
        assertEquals(0.2, store.state!!.stakeIn("Team Cherry"), 1e-9)
        compose.onNodeWithTag("stake", useUnmergedTree = true).performScrollTo()
        compose.onAllNodesWithText("Sell your stake", substring = true).onFirst().fetchSemanticsNode()
        save("99az-stake", top = true)

        // The stake comes off the price; pay part of the rest on results.
        tap("Earn-out")
        compose.waitForIdle()
        compose.onAllNodesWithText("your 20% stake covers", substring = true).onFirst().performScrollTo()
        save("99ba-earn-out-offer", top = true)
        tap("Offer $", substring = true)
        compose.waitForIdle()
        assertTrue(store.state!!.games!!.deal!!.earnOut)

        // The holdings summary on the market page.
        compose.runOnIdle {
            store.dealStudio = null
            store.perform { advance() }
            store.openStudio(StudioPage.MARKET)
        }
        compose.waitForIdle()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasTestTag("stakes"))
        compose.waitForIdle()
        compose.onAllNodesWithText("Your stakes", substring = true).onFirst().fetchSemanticsNode()
        save("99bb-holdings")
    }
}
