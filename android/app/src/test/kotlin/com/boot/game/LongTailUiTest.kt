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
import com.boot.core.Topic
import com.boot.core.onSale
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

/** A released game's long tail: price, sales, updates, ports and mods. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LongTailUiTest {
    @get:Rule(order = 0)
    val display = DisplayRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private fun save(name: String) {
        compose.waitForIdle()
        val roots = compose.onAllNodes(androidx.compose.ui.test.isRoot())
        val view = (roots[roots.fetchSemanticsNodes().size - 1].fetchSemanticsNode().root as ViewRootForTest).view
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

    private fun tap(text: String) = compose.onAllNodesWithText(text, substring = true).onFirst().performSemanticsAction(SemanticsActions.OnClick)

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-xxhdpi")
    fun theLongTail() {
        val store = GameStore(RuntimeEnvironment.getApplication())
        store.clearHall()
        compose.setContent { BootTheme { BootApp(store) } }
        compose.runOnIdle {
            store.newGame("Pixel Forge", "You", Lane.GAMES, seed = 9)
            store.perform {
                Sandbox.setCash(this, 20_000_000.0)
                games!!.releases.add(
                    0,
                    GameRelease(
                        id = 9001, title = "Moss Kingdom",
                        concept = GameConcept("Moss Kingdom", Topic.CITY, Genre.SIMULATION, platforms = listOf(Platform.PC), size = GameSize.MEDIUM),
                        week = week - 60, reviews = emptyList(), score = 8.2, weeklySales = 30_000.0, netShare = 0.7, design = 10.0, tech = 10.0,
                        bugs = 0.0, hype = 0.0, devCost = 0.0, report = emptyList(), userScore = 7.8, unitsSold = 400_000.0,
                    ),
                )
            }
            store.todayOpen = false
            store.openRelease = 9001
        }
        compose.waitForIdle()
        scrollTo("Port it")
        save("99bi-after-launch")
        tap("Sale…")
        compose.waitForIdle()
        save("99bj-sale-menu")
        tap("50% off")
        compose.waitForIdle()
        val r = store.state!!.games!!.release(9001)!!
        assertTrue(r.onSale(store.state!!.week))
        scrollTo("Release mod tools")
        tap("Enhanced")
        compose.waitForIdle()
        save("99bk-port-and-mods")
    }
}
