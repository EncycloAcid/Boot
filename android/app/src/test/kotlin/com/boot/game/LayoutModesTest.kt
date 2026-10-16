package com.boot.game

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import com.boot.core.Lane
import com.boot.core.advance
import com.boot.core.hire
import com.boot.core.boothCost
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

/** Sideways (the default) and upright with the classic layout. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LayoutModesTest {
    @get:Rule(order = 0)
    val display = DisplayRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private lateinit var store: GameStore

    private fun launch() {
        store = GameStore(RuntimeEnvironment.getApplication())
        store.clearHall()
        compose.setContent { BootTheme { BootApp(store) } }
        compose.waitForIdle()
    }

    /** Saves the screen, or with [top] the topmost window (a dialog or sheet). */
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
    fun sidewaysIsTheDefault() {
        launch()
        assertEquals(ScreenLayout.SIDEWAYS, store.layout)
        save("90-sideways-new-game")
        compose.runOnIdle { store.newGame("Garage Labs", "You", Lane.GAMES, seed = 3) }
        compose.waitForIdle()
        compose.runOnIdle { store.todayOpen = false }
        save("91-sideways-office")
        compose.runOnIdle { store.todayOpen = true }
        save("92-sideways-today")
        compose.runOnIdle {
            store.todayOpen = false
            store.tab = Tab.LANES
        }
        save("93-sideways-lanes")
        // Play stays reachable with a window open.
        compose.onNodeWithContentDescription("Play").performClick()
        compose.waitForIdle()
        assertTrue(store.state!!.week >= 1)
        compose.runOnIdle { store.tab = Tab.COMPANY }
        save("94-sideways-company")

        // Mid-game: people at their desks and a game in development.
        compose.runOnIdle {
            store.newGame("Pixel Forge", "You", Lane.GAMES, seed = 9)
            store.perform {
                com.boot.core.Sandbox.setCash(this, 400_000.0)
                repeat(2) { candidates.firstOrNull()?.let { c -> hire(c.id, Lane.GAMES) } }
                startGame(com.boot.core.GameConcept("Moss Knight", com.boot.core.Topic.FANTASY, com.boot.core.Genre.RPG, platforms = listOf(com.boot.core.Platform.PC), size = com.boot.core.GameSize.SMALL))
                repeat(3) { advance() }
            }
            store.tab = Tab.OFFICE
        }
        compose.waitForIdle()
        compose.runOnIdle { store.todayOpen = false }
        save("97-sideways-developing")
        compose.runOnIdle { store.tab = Tab.TEAM }
        save("98-sideways-team")
        compose.runOnIdle { store.designGame() }
        save("99-sideways-designer", top = true)
        compose.runOnIdle {
            store.designing = false
            store.perform { com.boot.core.Sandbox.enqueue(this, com.boot.core.RivalMove("Electronic Arts", Lane.GAMES, com.boot.core.RivalMoveKind.PRICE_WAR)) }
        }
        save("99b-sideways-event", top = true)
    }

    @Test
    @Config(qualifiers = "w393dp-h852dp-xxhdpi")
    fun uprightUsesTheClassicLayout() {
        launch()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Upright"))
        compose.onNodeWithText("Upright").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertEquals(ScreenLayout.VERTICAL, store.layout)
        compose.runOnIdle { store.newGame("Garage Labs", "You", Lane.GAMES, seed = 3) }
        compose.waitForIdle()
        // The office is on the home page, and the tabs run along the bottom.
        compose.onAllNodesWithText("Cramped apartment")[0].assertExistsCompat()
        save("95-upright-office")
        compose.onNodeWithText("Lanes").performClick()
        compose.waitForIdle()
        assertEquals(Tab.LANES, store.tab)
        save("96-upright-lanes")

        // The choice is remembered, and the menu switches back.
        assertEquals(ScreenLayout.VERTICAL, GameStore(RuntimeEnvironment.getApplication()).layout)
        compose.onNodeWithContentDescription("Menu", substring = true).performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Play sideways").performClick()
        compose.waitForIdle()
        assertEquals(ScreenLayout.SIDEWAYS, store.layout)
    }

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-xxhdpi")
    fun soundSettingsFromTheMenu() {
        launch()
        compose.runOnIdle {
            store.newGame("Garage Labs", "You", Lane.GAMES, seed = 3)
            store.todayOpen = false
        }
        compose.waitForIdle()
        // Today opens by itself for a new company; the menu sits behind it.
        compose.runOnIdle { store.todayOpen = false }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Menu", substring = true).performClick()
        compose.onNodeWithText("Sound and music").performClick()
        compose.waitForIdle()
        save("99c-sideways-sound", top = true)
        compose.onNodeWithContentDescription("Music volume").performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
        compose.onNodeWithContentDescription("Effects volume").performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }
        compose.waitForIdle()
        assertEquals(0f, store.sound.musicVolume)
        assertEquals(0.5f, store.sound.effectsVolume, 0.01f)
        compose.onNodeWithText("Off").assertExistsCompat()
        compose.onNodeWithText("Done").performClick()
        compose.waitForIdle()
        assertTrue(!store.soundOpen)
        // Remembered for next time.
        val again = GameStore(RuntimeEnvironment.getApplication())
        assertEquals(0f, again.sound.musicVolume)
        assertEquals(0.5f, again.sound.effectsVolume, 0.01f)
    }

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-xxhdpi")
    fun theTradeShow() {
        launch()
        compose.runOnIdle {
            store.newGame("Pixel Forge", "You", Lane.GAMES, seed = 9)
            store.perform {
                com.boot.core.Sandbox.setCash(this, 400_000.0)
                repeat(2) { candidates.firstOrNull()?.let { c -> hire(c.id, Lane.GAMES) } }
                startGame(com.boot.core.GameConcept("Moss Knight", com.boot.core.Topic.FANTASY, com.boot.core.Genre.RPG, platforms = listOf(com.boot.core.Platform.PC), size = com.boot.core.GameSize.SMALL))
                repeat(8) { advance() }
                com.boot.core.Sandbox.enqueue(this, com.boot.core.EventKind.Convention)
            }
            store.todayOpen = false
        }
        save("99d-sideways-trade-show", top = true)
        val cash = store.state!!.cash
        compose.onNodeWithText("Medium booth").performClick()
        compose.waitForIdle()
        assertEquals(cash - store.state!!.boothCost(com.boot.core.Booth.MEDIUM), store.state!!.cash, 1.0)
        val report = (store.state!!.events.first().kind as com.boot.core.ShowResults).report
        assertEquals("Moss Knight", report.game)
        compose.onNodeWithText("Visitors").assertExistsCompat()
        save("99e-sideways-show-report", top = true)
        compose.onNodeWithText("Back to work").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertTrue(store.state!!.events.map { it.title }.toString(), !store.state!!.needsDecision)
    }

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-xxhdpi")
    fun theShowSeason() {
        launch()
        compose.runOnIdle {
            store.newGame("Pixel Forge", "You", Lane.GAMES, seed = 9)
            store.perform {
                com.boot.core.Sandbox.setCash(this, 400_000.0)
                startGame(com.boot.core.GameConcept("Moss Knight", com.boot.core.Topic.FANTASY, com.boot.core.Genre.RPG, platforms = listOf(com.boot.core.Platform.SWITCHEROO_2), size = com.boot.core.GameSize.SMALL))
                repeat(6) { advance() }
                com.boot.core.Sandbox.enqueue(this, com.boot.core.PlatformInvite("Nintendo", "a Nintendo Direct", 1))
            }
            store.todayOpen = false
        }
        save("99f-sideways-nintendo-direct", top = true)
        compose.onNodeWithText("Show a trailer").performClick()
        compose.waitForIdle()
        assertEquals(1, store.state!!.games!!.project!!.reveals)
        // The stage report comes first.
        save("99f2-sideways-stage-report", top = true)
        compose.onNodeWithText("Back to work").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertTrue(store.state!!.news.any { it.headline == "Pixel Forge announces Moss Knight" })
        compose.runOnIdle { store.perform { com.boot.core.Sandbox.enqueue(this, com.boot.core.PremiereSlot("The Game Awards", 250_000.0, 1)) } }
        save("99g-sideways-premiere", top = true)
        compose.onNodeWithText("Skip it").performClick()
        compose.waitForIdle()
        assertTrue(!store.state!!.needsDecision)
    }

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-xxhdpi")
    fun tappingSomeoneShowsTheirCard() {
        launch()
        compose.runOnIdle {
            store.newGame("Pixel Forge", "Sam Rivera", Lane.GAMES, seed = 9)
            store.perform {
                com.boot.core.Sandbox.setCash(this, 400_000.0)
                repeat(2) { candidates.firstOrNull()?.let { c -> hire(c.id, Lane.GAMES) } }
            }
        }
        compose.waitForIdle()
        compose.runOnIdle { store.todayOpen = false }
        compose.waitForIdle()
        val hire = store.state!!.staff.first()
        compose.onNodeWithContentDescription(hire.name).performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        compose.onNodeWithText("Level ${hire.level} ${hire.role.title.lowercase()}").assertExistsCompat()
        compose.onNodeWithText("Morale").assertExistsCompat()
        save("99h-sideways-person", top = true)
        compose.onNodeWithText("Manage in Team").performClick()
        compose.waitForIdle()
        assertEquals(Tab.TEAM, store.tab)
        // The founder has a card too.
        compose.runOnIdle { store.tab = Tab.OFFICE }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Sam Rivera").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        compose.onNodeWithText("Founder · that's you").assertExistsCompat()
        save("99i-sideways-founder", top = true)
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertExistsCompat() = fetchSemanticsNode()
}
