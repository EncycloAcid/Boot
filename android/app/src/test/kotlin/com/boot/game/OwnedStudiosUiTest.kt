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
import com.boot.core.Plan
import com.boot.core.Sandbox
import com.boot.core.StudioBudget
import com.boot.core.StudioUnrest
import com.boot.game.ui.BootApp
import com.boot.game.ui.BootTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** A studio you own: its morale, its pitch, a greenlit game, and trouble when it's run for cash. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class OwnedStudiosUiTest {
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

    private fun scrollTo(text: String) {
        val n = compose.onAllNodes(hasScrollAction()).fetchSemanticsNodes().size
        val i = (n - 1 downTo 0).firstOrNull { i ->
            runCatching { compose.onAllNodes(hasScrollAction())[i].performScrollToNode(hasText(text, substring = true)) }.isSuccess
        }
        check(i != null) { "No list has $text" }
        compose.waitForIdle()
    }

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-xxhdpi")
    fun runAStudio() {
        val store = GameStore(RuntimeEnvironment.getApplication())
        store.clearHall()
        compose.setContent { BootTheme { BootApp(store) } }
        compose.runOnIdle {
            store.newGame("Pixel Forge", "You", Lane.GAMES, seed = 9)
            store.perform {
                Sandbox.setCash(this, 2_000_000_000.0)
                Sandbox.own(this, "FromSoftware")
            }
            store.todayOpen = false
            store.dealStudio = "FromSoftware"
        }
        compose.waitForIdle()
        scrollTo("Their pitch")
        compose.onAllNodesWithText("Generous").onFirst().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        scrollTo("Greenlight it")
        save("99al-studio-pitch", top = true)
        compose.onAllNodesWithText("Greenlight it", substring = true).onFirst().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        val sub = store.state!!.games!!.subsidiaries.single()
        assertEquals(StudioBudget.GENEROUS, sub.project!!.budget)
        compose.runOnIdle { store.advance(4) }
        compose.waitForIdle()
        scrollTo("Cancel it")
        save("99am-studio-making", top = true)

        // Run it for cash long enough and they say so.
        compose.runOnIdle {
            store.dealStudio = null
            store.perform {
                val s = games!!.subsidiaries.single()
                s.plan = Plan.SQUEEZED
                s.morale = 0.25
                weeks(1)
            }
        }
        compose.waitForIdle()
        assertNotNull(store.state!!.events.firstOrNull { it.kind is StudioUnrest })
        compose.onAllNodesWithText("Trouble at FromSoftware").onFirst().fetchSemanticsNode()
        save("99an-studio-unrest", top = true)
        compose.onAllNodesWithText("Pay retention bonuses").onFirst().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
    }
}
