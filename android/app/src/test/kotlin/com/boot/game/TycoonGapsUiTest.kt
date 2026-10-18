package com.boot.game

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performScrollToNode
import com.boot.core.FocusArea
import com.boot.core.GameConcept
import com.boot.core.GameSize
import com.boot.core.Genre
import com.boot.core.Lane
import com.boot.core.Platform
import com.boot.core.Research
import com.boot.core.Role
import com.boot.core.Sandbox
import com.boot.core.Specialty
import com.boot.core.Topic
import com.boot.core.assignCrew
import com.boot.core.buildEngine
import com.boot.core.orderEditions
import com.boot.core.startGame
import com.boot.game.ui.BootApp
import com.boot.game.ui.BootTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Who works on what, boxed editions, licences and an engine build taking the team. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TycoonGapsUiTest {
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

    private fun launch(setup: com.boot.core.GameState.() -> Unit) {
        store = GameStore(RuntimeEnvironment.getApplication())
        store.clearHall()
        compose.setContent { BootTheme { BootApp(store) } }
        compose.runOnIdle {
            store.newGame("Pixel Forge", "You", Lane.GAMES, seed = 9)
            store.perform {
                Sandbox.setCash(this, 5_000_000.0)
                setup()
            }
            store.todayOpen = false
        }
        compose.waitForIdle()
    }

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-xxhdpi")
    fun whoWorksOnWhat() {
        launch {
            val wren = Sandbox.addStaff(this, "Wren Hale", Role.DESIGNER, Lane.GAMES, Specialty.WRITER, skill = 8)
            val theo = Sandbox.addStaff(this, "Theo Park", Role.ENGINEER, Lane.GAMES, Specialty.ENGINE_PROGRAMMER)
            Sandbox.addStaff(this, "Ada Lin", Role.DESIGNER, Lane.GAMES)
            startGame(GameConcept("Moss Knight", Topic.FANTASY, Genre.RPG, platforms = listOf(Platform.PC), size = GameSize.SMALL))
            assignCrew(1, 0, wren.id, FocusArea.STORY)
            assignCrew(1, 0, theo.id, FocusArea.ENGINE)
        }
        compose.runOnIdle { store.openStudio(StudioPage.MAKE) }
        compose.waitForIdle()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasTestTag("crew"))
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Ada Lin"))
        compose.waitForIdle()
        save("99av-crew")
    }

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-xxhdpi")
    fun boxedEditionsAndLicences() {
        launch {
            Sandbox.research(this, Research.BOXED_EDITIONS)
            games!!.fans = 150_000.0
            startGame(GameConcept("Moss Knight", Topic.FANTASY, Genre.RPG, platforms = listOf(Platform.PC), size = GameSize.SMALL))
            orderEditions(1, 20_000, 1_000)
        }
        compose.runOnIdle { store.tab = Tab.OFFICE }
        compose.waitForIdle()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Collector's editions"))
        compose.waitForIdle()
        save("99aw-editions")
        compose.runOnIdle { store.openStudio(StudioPage.TECH) }
        compose.waitForIdle()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Buy licence"))
        compose.waitForIdle()
        save("99ax-licences")
    }

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-xxhdpi")
    fun anEngineTakesTheTeam() {
        launch {
            Sandbox.research(this, Research.CUSTOM_ENGINES)
            Sandbox.research(this, Research.GRAPHICS_2D_V2)
            Sandbox.research(this, Research.SOUND_STEREO)
            buildEngine()
        }
        compose.runOnIdle { store.openStudio(StudioPage.MAKE) }
        compose.waitForIdle()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("An engine takes the whole main team", substring = true))
        compose.waitForIdle()
        save("99ay-engine-build")
    }
}
