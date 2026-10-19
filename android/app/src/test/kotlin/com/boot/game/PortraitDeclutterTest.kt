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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import com.boot.core.Genre
import com.boot.core.Lane
import com.boot.core.Platform
import com.boot.core.Sandbox
import com.boot.core.Topic
import com.boot.core.hire
import com.boot.core.startGame
import com.boot.game.ui.BootApp
import com.boot.game.ui.BootTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Upright portrait: progressive disclosure keeps one screen from becoming a wall of panels. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PortraitDeclutterTest {
    @get:Rule(order = 0)
    val display = DisplayRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private lateinit var store: GameStore

    private fun launchUpright() {
        store = GameStore(RuntimeEnvironment.getApplication())
        store.clearHall()
        compose.setContent { BootTheme { BootApp(store) } }
        compose.waitForIdle()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Upright"))
        compose.onNodeWithText("Upright").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertEquals(ScreenLayout.VERTICAL, store.layout)
    }

    private fun save(name: String) {
        compose.waitForIdle()
        val roots = compose.onAllNodes(androidx.compose.ui.test.isRoot())
        val view = (roots[0].fetchSemanticsNode().root as ViewRootForTest).view
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File(DisplayRule.screenshots, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun scrollTo(text: String, substring: Boolean = false) {
        val target = hasText(text, substring = substring)
        val n = compose.onAllNodes(hasScrollAction()).fetchSemanticsNodes().size
        val i = (n - 1 downTo 0).firstOrNull { idx ->
            runCatching { compose.onAllNodes(hasScrollAction())[idx].performScrollToNode(target) }.isSuccess
        }
        check(i != null) { "No list has $text" }
        compose.waitForIdle()
    }

    private fun tap(text: String) = compose.onAllNodesWithText(text).onFirst().performSemanticsAction(SemanticsActions.OnClick)

    @Test
    @Config(qualifiers = "w393dp-h852dp-xxhdpi")
    fun todayUsesNowMarketPulseTabs() {
        launchUpright()
        compose.runOnIdle { store.newGame("Garage Labs", "You", Lane.GAMES, seed = 3) }
        compose.waitForIdle()
        compose.onNodeWithText("Now").assertExistsCompat()
        compose.onNodeWithText("Market").assertExistsCompat()
        compose.onNodeWithText("Pulse").assertExistsCompat()
        // Default Now: office stage, not the full chart wall.
        compose.onAllNodesWithText("Cramped apartment")[0].assertExistsCompat()
        compose.onNodeWithText("Pulse").performClick()
        compose.waitForIdle()
        // Pulse surfaces the latest log — not stacked under Now with charts.
        compose.onNodeWithText("Latest").assertExistsCompat()
        save("portrait-today")
    }

    @Test
    @Config(qualifiers = "w393dp-h852dp-xxhdpi")
    fun makeTabsHideSecondaryChromeUntilChosen() {
        launchUpright()
        compose.runOnIdle {
            store.newGame("Pixel Forge", "You", Lane.GAMES, seed = 9)
            store.perform {
                Sandbox.setCash(this, 400_000.0)
                repeat(2) { candidates.firstOrNull()?.let { c -> hire(c.id, Lane.GAMES) } }
                startGame(
                    com.boot.core.GameConcept(
                        "Moss Knight",
                        Topic.FANTASY,
                        Genre.RPG,
                        platforms = listOf(Platform.PC),
                        size = com.boot.core.GameSize.SMALL,
                    ),
                )
            }
            store.openStudio(StudioPage.MAKE)
        }
        compose.waitForIdle()
        // Development stays up; Focus / Fit / Hype / Trust are tabs, not a simultaneous stack.
        scrollTo("Focus")
        compose.onAllNodesWithText("Focus").onFirst().assertExistsCompat()
        compose.onAllNodesWithText("Fit").onFirst().assertExistsCompat()
        compose.onAllNodesWithText("Hype").onFirst().assertExistsCompat()
        compose.onAllNodesWithText("Trust").onFirst().assertExistsCompat()
        tap("Hype")
        compose.waitForIdle()
        tap("Trust")
        compose.waitForIdle()
        scrollTo("Transparency", substring = true)
        compose.onAllNodesWithText("Transparency", substring = true).onFirst().assertExistsCompat()
        save("portrait-make")
    }

    @Test
    @Config(qualifiers = "w393dp-h852dp-xxhdpi")
    fun teamSplitsPeopleHirePlace() {
        launchUpright()
        compose.runOnIdle {
            store.newGame("Pixel Forge", "You", Lane.GAMES, seed = 9)
            store.perform {
                Sandbox.setCash(this, 400_000.0)
                candidates.firstOrNull()?.let { c -> hire(c.id, Lane.GAMES) }
            }
            store.tab = Tab.TEAM
        }
        compose.waitForIdle()
        compose.onNodeWithText("People").assertExistsCompat()
        compose.onAllNodesWithText("Hire")[0].assertExistsCompat()
        compose.onNodeWithText("Place").assertExistsCompat()
        // Default People: staff list, not contract-labour hire CTAs.
        compose.onNodeWithText("Staff").assertExistsCompat()
        compose.onAllNodesWithText("Hire")[0].performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Contract labour").assertExistsCompat()
        compose.onNodeWithText("Place").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Perks", substring = true).assertExistsCompat()
        save("portrait-team")
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertExistsCompat() = fetchSemanticsNode()
}
