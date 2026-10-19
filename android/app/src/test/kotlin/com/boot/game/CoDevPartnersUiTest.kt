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
import com.boot.core.CoDevKind
import com.boot.core.CoDevTerms
import com.boot.core.GameConcept
import com.boot.core.GameSize
import com.boot.core.Genre
import com.boot.core.Lane
import com.boot.core.Platform
import com.boot.core.Research
import com.boot.core.Role
import com.boot.core.Sandbox
import com.boot.core.Topic
import com.boot.core.freeDesks
import com.boot.core.moveOffice
import com.boot.core.startGame
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

/** Studio Make tab shows co-dev partner contribution / propose UI. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CoDevPartnersUiTest {
    @get:Rule(order = 0)
    val display = DisplayRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private fun scrollTo(text: String) {
        val n = compose.onAllNodes(hasScrollAction()).fetchSemanticsNodes().size
        val i = (n - 1 downTo 0).firstOrNull { i ->
            runCatching { compose.onAllNodes(hasScrollAction())[i].performScrollToNode(hasText(text, substring = true)) }.isSuccess
        }
        check(i != null) { "No list has $text" }
        compose.waitForIdle()
    }

    private fun tap(text: String) =
        compose.onAllNodesWithText(text, substring = true).onFirst().performSemanticsAction(SemanticsActions.OnClick)

    private fun save(name: String) {
        compose.waitForIdle()
        val roots = compose.onAllNodes(androidx.compose.ui.test.isRoot())
        val view = (roots[roots.fetchSemanticsNodes().size - 1].fetchSemanticsNode().root as ViewRootForTest).view
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File(DisplayRule.screenshots, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-xxhdpi")
    fun coDevPartnerVisibleOnMakeTab() {
        val store = GameStore(RuntimeEnvironment.getApplication())
        store.clearHall()
        compose.setContent { BootTheme { BootApp(store) } }
        compose.runOnIdle {
            store.newGame("CoDev Co", "You", Lane.GAMES, seed = 17)
            store.perform {
                Sandbox.setCash(this, 200_000_000.0)
                Sandbox.research(this, Research.MEDIUM_GAMES)
                Sandbox.research(this, Research.LARGE_GAMES)
                while (freeDesks < 6 && office.next != null) moveOffice()
                repeat(4) { i ->
                    Sandbox.addStaff(this, "Dev $i", if (i % 2 == 0) Role.DESIGNER else Role.ENGINEER, Lane.GAMES)
                }
                val focus = listOf(
                    mutableListOf(0.4, 0.3, 0.3),
                    mutableListOf(0.4, 0.3, 0.3),
                    mutableListOf(0.4, 0.3, 0.3),
                )
                val concept = GameConcept(
                    "Shared Crown", Topic.FANTASY, Genre.ACTION,
                    platforms = listOf(Platform.PC), size = GameSize.LARGE,
                    engineId = games!!.engines.first().id,
                )
                games!!.topics += Topic.FANTASY
                startGame(concept, focus)
                games!!.project!!.coDev = CoDevTerms(
                    "Valve", CoDevKind.MARKET, 0.4, 0.35, 0.4, 0.3,
                    contribution = 120.0, costPaid = 50_000.0, weeksActive = 4,
                )
            }
            store.todayOpen = false
        }
        compose.waitForIdle()
        compose.runOnIdle { store.openStudio(StudioPage.MAKE) }
        compose.waitForIdle()
        scrollTo("Co-dev")
        assertTrue(compose.onAllNodesWithText("Co-dev · Valve", substring = true).fetchSemanticsNodes().isNotEmpty())
        assertTrue(compose.onAllNodesWithText("Partner contribution", substring = true).fetchSemanticsNodes().isNotEmpty())
        save("co-dev-partners-make")

        // Rivals tab: propose affordance when eligible (project already partnered — locked text still shows).
        compose.runOnIdle { store.openStudio(StudioPage.RIVALS) }
        compose.waitForIdle()
        scrollTo("Propose co-dev")
        assertTrue(compose.onAllNodesWithText("Propose co-dev", substring = true).fetchSemanticsNodes().isNotEmpty())
        save("co-dev-partners-rivals")
    }
}
