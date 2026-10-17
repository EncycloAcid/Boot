package com.boot.game

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.boot.core.GameConcept
import com.boot.core.GameSize
import com.boot.core.Genre
import com.boot.core.Platform
import com.boot.core.Topic
import com.boot.game.ui.BootTheme
import com.boot.game.ui.GameCover
import com.boot.game.ui.RealCovers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Real games show their real covers; made-up ones keep the drawn box art. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RealCoversTest {
    @get:Rule(order = 0)
    val display = DisplayRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val context get() = RuntimeEnvironment.getApplication()

    @Test
    fun titlesMatchTheirCovers() {
        assertTrue(RealCovers.has(context, "Halo 2"))
        assertTrue(RealCovers.has(context, "Pokémon Gold and Silver"))
        assertTrue(RealCovers.has(context, "Assassin's Creed II"))
        assertTrue(RealCovers.has(context, "Call of Duty: World at War"))
        assertFalse(RealCovers.has(context, "Sunken Crown"))
        val cover = RealCovers.load(context, "The Legend of Zelda: Breath of the Wild")!!
        assertEquals(300, cover.width)
        assertEquals(400, cover.height)
        assertEquals(RealCovers.key("Pokemon gold & silver"), RealCovers.key("Pokémon Gold and Silver"))
    }

    @Test
    fun aShelfOfRealAndMadeUpGames() {
        val titles = listOf(
            "Super Mario Bros. 3", "The Legend of Zelda: Ocarina of Time", "Halo: Combat Evolved", "Pokémon Red and Blue",
            "Final Fantasy VII", "Grand Theft Auto V", "Street Fighter 6", "Sunken Crown",
        )
        compose.setContent {
            BootTheme {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    titles.chunked(4).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { GameCover(GameConcept(it, Topic.FANTASY, Genre.ACTION, platforms = listOf(Platform.PC), size = GameSize.AAA), 88.dp, studio = "Pixel Forge") }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        val view = (compose.onAllNodes(androidx.compose.ui.test.isRoot())[0].fetchSemanticsNode().root as ViewRootForTest).view
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File(DisplayRule.screenshots, "54c-real-covers.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
