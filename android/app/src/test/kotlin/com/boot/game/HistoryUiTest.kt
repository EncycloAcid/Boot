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
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import com.boot.core.GameClock
import com.boot.core.HistoryChoice
import com.boot.core.HistoryEvent
import com.boot.core.Lane
import com.boot.core.Sandbox
import com.boot.core.StartPoint
import com.boot.core.advance
import com.boot.core.choose
import com.boot.core.stayFocused
import com.boot.game.ui.BootApp
import com.boot.game.ui.BootTheme
import org.junit.After
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

/** History arriving in a run set in the past, and the era page's history and decades. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HistoryUiTest {
    @get:Rule(order = 0)
    val display = DisplayRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @After
    fun resetClock() {
        GameClock.startYear = null
    }

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
    fun doomArrives() {
        val store = GameStore(RuntimeEnvironment.getApplication())
        store.clearHall()
        compose.setContent { BootTheme { BootApp(store) } }
        compose.runOnIdle {
            store.newGame("Pixel Forge", "You", Lane.GAMES, seed = 9, start = StartPoint.CONSOLE_WARS)
            store.perform {
                Sandbox.setCash(this, 50_000_000.0)
                while (needsDecision) if (pendingPivot != null) stayFocused() else choose(0)
                advance()
                while (needsDecision) if (pendingPivot != null) stayFocused() else choose(0)
                // The 1980s end, then Doom comes out.
                Sandbox.jumpTo(this, (1990 - 1985) * 52)
                advance()
                while (needsDecision) if (pendingPivot != null) stayFocused() else choose(0)
                Sandbox.jumpTo(this, HistoryEvent.DOOM.weekIn(1985))
                advance()
                while (needsDecision && events.firstOrNull()?.kind !is HistoryChoice) if (pendingPivot != null) stayFocused() else choose(0)
            }
            store.todayOpen = false
        }
        compose.waitForIdle()
        compose.onAllNodesWithText("Give away part of your next game?").onFirst().fetchSemanticsNode()
        save("99bc-history-choice", top = true)
        compose.onAllNodesWithText("Go shareware").onFirst().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertTrue(store.state!!.world.pressures.any { it.title == "Shareware" })
        assertEquals(1, store.state!!.world.decades.size)

        compose.runOnIdle {
            store.tab = Tab.COMPANY
            store.companyPage = 0
        }
        compose.waitForIdle()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasTestTag("history"))
        compose.waitForIdle()
        compose.onAllNodesWithText(HistoryEvent.DOOM.headline).onFirst().fetchSemanticsNode()
        save("99bd-history")
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasTestTag("decades"))
        compose.waitForIdle()
        compose.onAllNodesWithText("The 1980s").onFirst().fetchSemanticsNode()
        save("99be-decades")
    }
}
