package com.boot.game

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import com.boot.core.FounderTrait
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

/** Making yourself on the new game screen, and seeing yourself in the game. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FounderCustomizerTest {
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

    private fun tap(text: String) = compose.onNodeWithText(text).performSemanticsAction(SemanticsActions.OnClick)

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-xxhdpi")
    fun makeYourselfAndPickATrait() {
        val store = GameStore(RuntimeEnvironment.getApplication())
        store.clearHall()
        compose.setContent { BootTheme { BootApp(store) } }
        compose.waitForIdle()

        compose.onNode(hasScrollAction()).performScrollToNode(hasText("You, the founder"))
        tap("Bun")
        compose.onNodeWithContentDescription("Clothes colour 5").performSemanticsAction(SemanticsActions.OnClick)
        tap("Glasses")
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Shuffle"))
        save("81-founder-look")
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Showrunner"))
        tap("Showrunner")
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Your special trait"))
        save("82-founder-trait")

        tap("Start your game studio")
        compose.waitForIdle()
        val founder = store.state!!.founder
        assertEquals(FounderTrait.SHOWRUNNER, founder.trait)
        val look = founder.appearance!!
        assertEquals(3, look.style)
        assertEquals(4, look.top)
        // Glasses were toggled from whatever the name gave.
        assertTrue(look.glasses != com.boot.game.ui.appearanceFor("You".hashCode() xor "Garage Labs".hashCode()).glasses)

        // The founder's card in the office shows the trait.
        compose.runOnIdle { store.todayOpen = false }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("You").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        compose.onNodeWithText("Showrunner").fetchSemanticsNode()
        save("83-founder-card", top = true)
    }
}
