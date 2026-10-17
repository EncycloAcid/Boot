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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import com.boot.core.Hook
import com.boot.core.Lane
import com.boot.core.MilestoneReview
import com.boot.core.Production
import com.boot.core.Sandbox
import com.boot.core.acknowledgeReview
import com.boot.core.advance
import com.boot.core.choose
import com.boot.core.launchGame
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

/** Making a game by hand: production and a hook, a milestone decision, the build timeline, and a port. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DevelopmentUiTest {
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

    private fun tap(text: String) = compose.onAllNodesWithText(text).onFirst().performSemanticsAction(SemanticsActions.OnClick)

    private fun scrollTo(text: String, substring: Boolean = false) {
        val n = compose.onAllNodes(hasScrollAction()).fetchSemanticsNodes().size
        val target = hasText(text, substring = substring)
        // The topmost list that has it: the designer and sheets sit over the page.
        val i = (n - 1 downTo 0).firstOrNull { i ->
            runCatching { compose.onAllNodes(hasScrollAction())[i].performScrollToNode(target) }.isSuccess
        }
        check(i != null) { "No list has $text" }
        compose.waitForIdle()
    }

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-xxhdpi")
    fun makeAGameByHand() {
        val store = GameStore(RuntimeEnvironment.getApplication())
        store.clearHall()
        compose.setContent { BootTheme { BootApp(store) } }
        compose.runOnIdle {
            store.newGame("Pixel Forge", "You", Lane.GAMES, seed = 9)
            store.perform { Sandbox.setCash(this, 3_000_000.0) }
            store.todayOpen = false
            store.designing = true
        }
        compose.waitForIdle()
        repeat(3) {
            compose.onNodeWithText("Next").performClick()
            compose.waitForIdle()
        }
        scrollTo("Lavish")
        tap("Lavish")
        compose.waitForIdle()
        compose.onNodeWithTag("hooks").fetchSemanticsNode()
        scrollTo("Open world")
        tap("Open world")
        compose.waitForIdle()
        scrollTo("The hook")
        save("99af-designer-hook", top = true)
        compose.onNodeWithText("Start development").performClick()
        compose.waitForIdle()
        val c = store.state!!.games!!.project!!.concept
        assertEquals(Production.LAVISH, c.production)
        assertEquals(Hook.OPEN_WORLD, c.hook)

        // Build until the vertical slice.
        compose.runOnIdle {
            store.perform {
                repeat(200) {
                    if (needsDecision && events.first().kind is MilestoneReview) return@perform
                    while (needsDecision && events.first().kind !is MilestoneReview) choose(events.first().choices.lastIndex)
                    if (!needsDecision) advance()
                }
            }
        }
        compose.waitForIdle()
        assertTrue(store.state!!.events.first().kind is MilestoneReview)
        compose.onAllNodesWithText("the vertical slice", substring = true).onFirst().fetchSemanticsNode()
        save("99ag-milestone", top = true)
        tap("Add a feature")
        compose.waitForIdle()
        assertTrue(store.state!!.games!!.project!!.workScale > 1.0)

        compose.runOnIdle {
            store.perform {
                repeat(3) {
                    while (needsDecision) choose(events.first().choices.lastIndex)
                    advance()
                }
                while (needsDecision) choose(events.first().choices.lastIndex)
            }
            store.tab = Tab.OFFICE
        }
        compose.waitForIdle()
        scrollTo("Decided", substring = true)
        compose.onNodeWithTag("timeline").fetchSemanticsNode()
        compose.onNodeWithTag("forecast").fetchSemanticsNode()
        save("99ah-build-timeline")

        // Finish it, ship it, and look at porting it.
        compose.runOnIdle {
            store.perform {
                repeat(400) {
                    val p = games!!.project ?: return@repeat
                    if (p.isReady && 2 in p.milestones && !needsDecision) return@repeat
                    while (needsDecision) choose(events.first().choices.lastIndex)
                    advance()
                }
                while (needsDecision) choose(events.first().choices.lastIndex)
                launchGame()
                acknowledgeReview()
                while (needsDecision) choose(events.first().choices.lastIndex)
            }
            store.openRelease = store.state!!.games!!.releases.first().id
        }
        compose.waitForIdle()
        scrollTo("Put it on sale")
        compose.onNodeWithText("Port to…").fetchSemanticsNode()
        save("99ai-port-and-sale", top = true)
    }
}
