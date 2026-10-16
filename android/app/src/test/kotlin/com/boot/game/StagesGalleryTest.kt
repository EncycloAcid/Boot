package com.boot.game

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.boot.game.ui.BootTheme
import com.boot.game.ui.StageBanner
import com.boot.game.ui.StageLook
import com.boot.game.ui.stageFor
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Every show's stage, side by side, and which show gets which. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StagesGalleryTest {
    @get:Rule(order = 0)
    val display = DisplayRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private fun save(name: String) {
        compose.waitForIdle()
        val view = (compose.onAllNodes(androidx.compose.ui.test.isRoot())[0].fetchSemanticsNode().root as ViewRootForTest).view
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File(DisplayRule.screenshots, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun showsGetTheirOwnStages() {
        assertEquals(StageLook.GAME_AWARDS, stageFor("The Game Awards"))
        assertEquals(StageLook.SPIKE, stageFor("the Spike Video Game Awards"))
        assertEquals(StageLook.BAFTA, stageFor("The BAFTA Games Awards"))
        assertEquals(StageLook.GOLDEN_JOYSTICKS, stageFor("the Golden Joystick Awards"))
        assertEquals(StageLook.DICE, stageFor("The D.I.C.E. Awards"))
        assertEquals(StageLook.GDC, stageFor("the Game Developers Choice Awards"))
        assertEquals(StageLook.NINTENDO, stageFor("A Nintendo Direct"))
        assertEquals(StageLook.PLAYSTATION, stageFor("a PlayStation State of Play"))
        assertEquals(StageLook.XBOX, stageFor("the Xbox Games Showcase"))
        assertEquals(StageLook.FESTIVAL, stageFor("Summer Game Fest"))
        assertEquals(StageLook.EXPO, stageFor("E3"))
    }

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-xxhdpi")
    fun theCovers() {
        val games = listOf(
            Triple("Moss Knight", com.boot.core.Topic.FANTASY, com.boot.core.Genre.RPG),
            Triple("Iron Armada 2", com.boot.core.Topic.MILITARY, com.boot.core.Genre.ACTION),
            Triple("Neon Drift", com.boot.core.Topic.CYBERPUNK, com.boot.core.Genre.ACTION),
            Triple("Hollow Manor", com.boot.core.Topic.HORROR, com.boot.core.Genre.ADVENTURE),
            Triple("Puppy Party", com.boot.core.Topic.PETS, com.boot.core.Genre.CASUAL),
            Triple("Star Colony", com.boot.core.Topic.SPACE, com.boot.core.Genre.STRATEGY),
            Triple("Tide Keeper", com.boot.core.Topic.PIRATES, com.boot.core.Genre.SIMULATION),
        )
        val platforms = listOf(com.boot.core.Platform.PLAYSYSTEM_5, com.boot.core.Platform.PC, com.boot.core.Platform.SWITCHEROO_2)
        compose.setContent {
            BootTheme {
                Row(Modifier.fillMaxSize().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    games.forEachIndexed { i, (t, topic, genre) ->
                        com.boot.game.ui.GameCover(
                            com.boot.core.GameConcept(t, topic, genre, platforms = listOf(platforms[i % 3]), size = com.boot.core.GameSize.LARGE),
                            112.dp, studio = "Pixel Forge",
                        )
                    }
                }
            }
        }
        save("99z-covers")
    }

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-xxhdpi")
    fun theGallery() {
        val names = listOf(
            "The Game Awards", "Spike Video Game Awards", "BAFTA Games Awards", "Golden Joystick Awards", "D.I.C.E. Awards",
            "Game Developers Choice", "Arkie Awards", "E3", "Summer Game Fest", "A Nintendo Direct", "State of Play", "Xbox Games Showcase",
            "Gamescom Opening Night Live", "Sega press conference", "Your showcase",
        )
        compose.setContent {
            BootTheme {
                Column(Modifier.fillMaxSize().padding(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (row in names.chunked(3)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (n in row) Column(Modifier.weight(1f)) { StageBanner(n, height = 72.dp) }
                        }
                    }
                }
            }
        }
        save("99y-stages")
    }
}
