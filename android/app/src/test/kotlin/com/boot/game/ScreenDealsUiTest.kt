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
import com.boot.core.GameConcept
import com.boot.core.GameRelease
import com.boot.core.GameSize
import com.boot.core.Genre
import com.boot.core.Lane
import com.boot.core.Platform
import com.boot.core.Sandbox
import com.boot.core.ScreenDeal
import com.boot.core.ScreenKind
import com.boot.core.ScreenOffer
import com.boot.core.Topic
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

/** A studio's offer to film one of your series, and the Film and TV section after. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenDealsUiTest {
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

    private fun release(id: Int, title: String, score: Double, week: Int, sequelOf: Int? = null) = GameRelease(
        id = id, title = title,
        concept = GameConcept(title, Topic.FANTASY, Genre.RPG, platforms = listOf(Platform.PC), size = GameSize.MEDIUM, sequelOf = sequelOf),
        week = week, reviews = emptyList(), score = score, weeklySales = 40_000.0, netShare = 0.7, design = 10.0, tech = 10.0,
        bugs = 0.0, hype = 0.0, devCost = 0.0, report = emptyList(), lifetimeSales = 24_000_000.0, unitsSold = 600_000.0,
    )

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-xxhdpi")
    fun aFilmDeal() {
        val store = GameStore(RuntimeEnvironment.getApplication())
        store.clearHall()
        compose.setContent { BootTheme { BootApp(store) } }
        compose.runOnIdle {
            store.newGame("Pixel Forge", "You", Lane.GAMES, seed = 9)
            store.perform {
                Sandbox.setCash(this, 20_000_000.0)
                val g = games!!
                g.releases.add(0, release(9001, "Moss Knight", 8.6, week))
                g.releases.add(0, release(9002, "Moss Knight 2: Ember Crown", 8.1, week, sequelOf = 9001))
                g.releases.add(0, release(9003, "Star Colony", 8.4, week))
                Sandbox.enqueue(this, ScreenOffer(9001, "Universal", ScreenKind.FILM, 1_800_000.0, 2_900_000.0))
            }
            store.todayOpen = false
        }
        compose.waitForIdle()
        compose.onAllNodesWithText("Universal wants to make a Moss Knight film").onFirst().fetchSemanticsNode()
        save("99al-screen-offer", top = true)
        compose.onAllNodesWithText("Co-produce").onFirst().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        val a = store.state!!.games!!.adaptations.single()
        assertEquals(ScreenDeal.COPRODUCE, a.deal)
        compose.runOnIdle { store.openStudio(StudioPage.GAMES) }
        compose.waitForIdle()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasTestTag("screen-deals"))
        compose.waitForIdle()
        compose.onAllNodesWithText("Moss Knight · Film").onFirst().fetchSemanticsNode()
        compose.onAllNodesWithText("Shop rights").onFirst().fetchSemanticsNode()
        save("99am-screen-section")
    }
}
