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
import com.boot.core.GameSize
import com.boot.core.GameState
import com.boot.core.Genre
import com.boot.core.Lane
import com.boot.core.Platform
import com.boot.core.Research
import com.boot.core.Sandbox
import com.boot.core.Storefront
import com.boot.core.Topic
import com.boot.core.advance
import com.boot.core.choose
import com.boot.core.openStore
import com.boot.core.setGiveaways
import com.boot.core.startGame
import com.boot.core.stayFocused
import com.boot.core.storesFor
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

/** Picking the stores a game is sold on, and running a store of your own. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StoresUiTest {
    @get:Rule(order = 0)
    val display = DisplayRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private lateinit var store: GameStore

    private fun save(name: String) {
        compose.waitForIdle()
        val view = (compose.onAllNodes(androidx.compose.ui.test.isRoot())[0].fetchSemanticsNode().root as ViewRootForTest).view
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File(DisplayRule.screenshots, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun launch(setup: GameState.() -> Unit) {
        store = GameStore(RuntimeEnvironment.getApplication())
        store.clearHall()
        compose.setContent { BootTheme { BootApp(store) } }
        compose.runOnIdle {
            store.newGame("Pixel Forge", "You", Lane.GAMES, seed = 9)
            store.perform {
                Sandbox.setCash(this, 50_000_000.0)
                setup()
            }
            store.todayOpen = false
        }
        compose.waitForIdle()
    }

    private fun GameState.play(weeks: Int) = repeat(weeks) {
        while (needsDecision) if (pendingPivot != null) stayFocused() else choose(events.first().choices.lastIndex)
        advance()
    }

    private fun tap(text: String) = compose.onAllNodesWithText(text, substring = true).onFirst().performSemanticsAction(SemanticsActions.OnClick)

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-xxhdpi")
    fun pickTheStores() {
        launch {
            startGame(GameConcept("Moss Knight", Topic.FANTASY, Genre.RPG, platforms = listOf(Platform.PC), size = GameSize.SMALL))
        }
        compose.runOnIdle { store.tab = Tab.OFFICE }
        compose.waitForIdle()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasTestTag("stores-1"))
        compose.waitForIdle()
        tap("GOG · 30%")
        compose.waitForIdle()
        tap("Epic Games Store · 12%")
        compose.waitForIdle()
        val s = store.state!!
        assertEquals(setOf(Storefront.STEAM, Storefront.GOG, Storefront.EPIC), s.storesFor(s.games!!.project!!))
        compose.onAllNodesWithText("of the usual", substring = true).onFirst().fetchSemanticsNode()
        save("99bf-store-picker")
    }

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-xxhdpi")
    fun runAStore() {
        launch {
            Sandbox.research(this, Research.DIGITAL_STORES)
            openStore()
            play(30)
            setGiveaways(true)
            play(40)
        }
        compose.runOnIdle { store.openStudio(StudioPage.MARKET) }
        compose.waitForIdle()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasTestTag("own-store"))
        compose.waitForIdle()
        tap("12%")
        compose.waitForIdle()
        assertEquals(0.12, store.state!!.games!!.store!!.cut, 1e-9)
        assertTrue(store.state!!.games!!.store!!.users > 0.5)
        save("99bg-own-store")
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasTestTag("store-exclusives"))
        compose.waitForIdle()
        save("99bh-store-exclusives")
    }
}
