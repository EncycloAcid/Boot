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
import com.boot.core.ExclusiveOffer
import com.boot.core.GameConcept
import com.boot.core.GameSize
import com.boot.core.Genre
import com.boot.core.Lane
import com.boot.core.Platform
import com.boot.core.Research
import com.boot.core.Sandbox
import com.boot.core.Topic
import com.boot.core.exclusiveTerms
import com.boot.core.startGame
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

/** A console maker's exclusive offer, and the deal on the project card. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ExclusivesUiTest {
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
    fun aTimedExclusive() {
        val store = GameStore(RuntimeEnvironment.getApplication())
        store.clearHall()
        compose.setContent { BootTheme { BootApp(store) } }
        compose.runOnIdle {
            store.newGame("Pixel Forge", "You", Lane.GAMES, seed = 9)
            store.perform {
                Sandbox.setCash(this, 2_000_000.0)
                Sandbox.research(this, Research.MULTI_PLATFORM)
                startGame(
                    GameConcept(
                        "Moss Knight", Topic.FANTASY, Genre.RPG,
                        platforms = listOf(Platform.PC, Platform.PLAYSYSTEM_5, Platform.MBOX_Z), size = GameSize.SMALL,
                    ),
                )
                weeks(3)
                val (full, timed) = exclusiveTerms(games!!.project!!, "Sony")
                Sandbox.enqueue(this, ExclusiveOffer(1, "Sony", full, timed))
            }
            store.todayOpen = false
        }
        compose.waitForIdle()
        compose.onAllNodesWithText("Sony wants an exclusive").onFirst().fetchSemanticsNode()
        save("99aj-exclusive-offer", top = true)
        compose.onAllNodesWithText("Timed exclusive").onFirst().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        val p = store.state!!.games!!.project!!
        assertEquals("Sony", p.exclusive?.maker)
        assertTrue(p.exclusive!!.timed)
        compose.runOnIdle { store.tab = Tab.OFFICE }
        compose.waitForIdle()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Timed exclusive", substring = true))
        compose.waitForIdle()
        save("99ak-exclusive-card")
    }
}
