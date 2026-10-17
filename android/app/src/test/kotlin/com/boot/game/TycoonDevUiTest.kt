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
import com.boot.core.Engine
import com.boot.core.FocusArea
import com.boot.core.Lane
import com.boot.core.MilestoneReview
import com.boot.core.Research
import com.boot.core.Sandbox
import com.boot.core.advance
import com.boot.core.choose
import com.boot.core.hire
import com.boot.game.ui.BootApp
import com.boot.game.ui.BootTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Game Dev Tycoon-style development: features and leads in the designer, then a stage gate. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TycoonDevUiTest {
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

    private val kit = listOf(
        Research.SAVE_SYSTEM, Research.PHYSICS, Research.BRANCHING_STORY, Research.ACHIEVEMENTS,
        Research.DIALOGUE_TREES, Research.VOICE_ACTING, Research.BASIC_AI, Research.ADVANCED_AI, Research.LEVEL_EDITOR,
        Research.GRAPHICS_2D_V2, Research.GRAPHICS_3D_V1, Research.STYLE_STYLIZED, Research.SOUND_STEREO, Research.ORCHESTRAL_SCORE, Research.DAY_NIGHT,
    )

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-xxhdpi")
    fun planAGameStageByStage() {
        val store = GameStore(RuntimeEnvironment.getApplication())
        store.clearHall()
        compose.setContent { BootTheme { BootApp(store) } }
        compose.runOnIdle {
            store.newGame("Pixel Forge", "You", Lane.GAMES, seed = 9)
            store.perform {
                Sandbox.setCash(this, 3_000_000.0)
                Sandbox.research(this, Research.LEAD_ROLES)
                games!!.engines += Engine(1, "Forge", kit, 0)
                repeat(2) { candidates.firstOrNull()?.let { c -> hire(c.id, Lane.GAMES) } }
            }
            store.todayOpen = false
            store.designing = true
        }
        compose.waitForIdle()
        repeat(2) {
            compose.onNodeWithText("Next").performClick()
            compose.waitForIdle()
        }
        compose.onNodeWithTag("features-0").fetchSemanticsNode()
        scrollTo("Leads")
        save("99ao-designer-features", top = true)
        compose.onNodeWithText("Next").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Start development").performClick()
        compose.waitForIdle()
        val p = store.state!!.games!!.project!!
        assertTrue(p.features!!.isNotEmpty())

        // Build to the vertical slice: the gate to the next stage.
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
        compose.onNodeWithTag("stage-plan").fetchSemanticsNode()
        scrollTo("Voice acting ·")
        save("99ap-stage-gate", top = true)
        // Put someone in charge of dialogues, then carry on.
        compose.onAllNodesWithText("No lead").onFirst().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        compose.onAllNodesWithText("fit", substring = true).onFirst().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        scrollTo("Keep to the plan")
        save("99aq-stage-gate-plan", top = true)
        compose.onAllNodesWithText("Keep to the plan").onFirst().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        val after = store.state!!.games!!.project!!
        assertTrue(after.leads.keys.any { it.stage == 1 })
        assertTrue(FocusArea.DIALOGUES in after.leads)
    }
}
