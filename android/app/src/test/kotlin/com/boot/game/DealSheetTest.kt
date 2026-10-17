package com.boot.game

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import com.boot.core.IntegrationChoice
import com.boot.core.Lane
import com.boot.core.Plan
import com.boot.core.Sandbox
import com.boot.core.advance
import com.boot.core.choose
import com.boot.game.ui.BootApp
import com.boot.game.ui.BootTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Looking a studio over, checking its books, making an offer, and running it once it's yours. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DealSheetTest {
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
        compose.onNodeWithText(text, substring = substring).performSemanticsAction(SemanticsActions.OnClick)

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-xxhdpi")
    fun buyAStudio() {
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
        assertTrue(store.holdsClock)
        compose.onNodeWithText("Due diligence").fetchSemanticsNode()
        tap("Look at the books", substring = true)
        compose.waitForIdle()
        assertTrue("Team Cherry" in store.state!!.games!!.diligence)
        save("99aa-deal-sheet", top = true)
        tap("Offer $", substring = true)
        compose.waitForIdle()
        assertNotNull(store.state!!.games!!.deal)
        compose.onNodeWithText("Withdraw the offer").fetchSemanticsNode()

        // Two weeks on, the owners answer; take whatever they say until it's ours or it's over.
        compose.runOnIdle {
            store.dealStudio = null
            store.perform {
                repeat(4) {
                    while (needsDecision && events.first().kind !is IntegrationChoice) choose(0)
                    if (!needsDecision && games!!.deal != null) advance()
                }
            }
        }
        compose.waitForIdle()
        val g = store.state!!.games!!
        if (g.subsidiaries.isNotEmpty()) {
            save("99ab-integration", top = true)
            tap("Fold them in")
            compose.waitForIdle()
            assertEquals(Plan.INTEGRATED, store.state!!.games!!.subsidiaries.single().plan)
            compose.runOnIdle { store.dealStudio = "Team Cherry" }
            compose.waitForIdle()
            compose.onNodeWithText("How you run it").fetchSemanticsNode()
            save("99ac-running-a-studio", top = true)
        }
    }
}
