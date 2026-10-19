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
import com.boot.core.GameConcept
import com.boot.core.GameRelease
import com.boot.core.GameSize
import com.boot.core.Genre
import com.boot.core.Lane
import com.boot.core.Platform
import com.boot.core.Sandbox
import com.boot.core.TempKind
import com.boot.core.Topic
import com.boot.core.freeDesks
import com.boot.core.hireTemps
import com.boot.core.moveOffice
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

/** Team contract labour + studio platform politics levers are visible. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IndustryPressureUiTest {
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

    private fun tap(text: String) = compose.onAllNodesWithText(text, substring = true).onFirst().performSemanticsAction(SemanticsActions.OnClick)

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
    fun industryPressureLevers() {
        val store = GameStore(RuntimeEnvironment.getApplication())
        store.clearHall()
        compose.setContent { BootTheme { BootApp(store) } }
        compose.runOnIdle {
            store.newGame("Pressure Co", "You", Lane.GAMES, seed = 11)
            store.perform {
                Sandbox.setCash(this, 50_000_000.0)
                while (freeDesks < 6 && office.next != null) moveOffice()
                hireTemps(TempKind.ARTIST, 2, 8)
                games!!.releases.add(
                    0,
                    GameRelease(
                        id = 9201, title = "Clip Bait",
                        concept = GameConcept("Clip Bait", Topic.CITY, Genre.ACTION, platforms = listOf(Platform.PC), size = GameSize.MEDIUM),
                        week = week - 30, reviews = emptyList(), score = 8.0, weeklySales = 25_000.0, netShare = 0.7,
                        design = 10.0, tech = 10.0, bugs = 0.0, hype = 0.0, devCost = 0.0, report = emptyList(),
                        userScore = 7.8, unitsSold = 100_000.0,
                    ),
                )
            }
            store.todayOpen = false
        }
        compose.waitForIdle()
        // Team: contract labour + temps
        tap("Team")
        compose.waitForIdle()
        scrollTo("Contract labour")
        assertTrue(compose.onAllNodesWithText("Hire temp artists", substring = true).fetchSemanticsNodes().isNotEmpty())
        scrollTo("Temps & contractors")
        assertTrue(compose.onAllNodesWithText("Temp", substring = true).fetchSemanticsNodes().isNotEmpty())
        save("industry-pressure-team")

        // Studio Tech page: featuring / logo
        compose.runOnIdle { store.openStudio(StudioPage.TECH) }
        compose.waitForIdle()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Logo on the box", substring = true))
        compose.waitForIdle()
        assertTrue(compose.onAllNodesWithText("Store featuring", substring = true).fetchSemanticsNodes().isNotEmpty())
        assertTrue(compose.onAllNodesWithText("Logo on the box", substring = true).fetchSemanticsNodes().isNotEmpty())
        save("industry-pressure-platforms")
    }
}
