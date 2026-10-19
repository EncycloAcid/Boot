package com.boot.game

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performScrollToNode
import com.boot.core.CareerChapter
import com.boot.core.Lane
import com.boot.core.Role
import com.boot.core.Sandbox
import com.boot.core.Specialty
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

/** Career arcs, loyalty and wanted-by pressure on the Team screen. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TalentWarsUiTest {
    @get:Rule(order = 0)
    val display = DisplayRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private fun save(name: String) {
        compose.waitForIdle()
        val roots = compose.onAllNodes(androidx.compose.ui.test.isRoot())
        val view = (roots[0].fetchSemanticsNode().root as ViewRootForTest).view
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File(DisplayRule.screenshots, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-xxhdpi")
    fun teamShowsCareerArcAndWantedBy() {
        val store = GameStore(RuntimeEnvironment.getApplication())
        store.clearHall()
        compose.setContent { BootTheme { BootApp(store) } }
        compose.runOnIdle {
            store.newGame("Star Forge", "You", Lane.GAMES, seed = 11)
            store.perform {
                Sandbox.setCash(this, 2_000_000.0)
                val star = Sandbox.addStaff(this, "Nova Quinn", Role.DESIGNER, Lane.GAMES, Specialty.GAME_DESIGNER, skill = 9)
                star.chapter = CareerChapter.PEAK
                star.reputation = 0.72
                star.loyalty = 0.38
                star.pastTitles = listOf("Lead designer", "Game designer")
                star.pastHits = listOf("Moss Knight")
                star.wantedBy = listOf("Nintendo")
                star.offerPressure = 0.6
            }
            store.todayOpen = false
            store.tab = Tab.TEAM
        }
        compose.waitForIdle()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Nova Quinn"))
        compose.waitForIdle()
        assertTrue(compose.onAllNodes(hasText("Peak")).fetchSemanticsNodes().isNotEmpty())
        assertTrue(compose.onAllNodes(hasText("Wanted by Nintendo", substring = true)).fetchSemanticsNodes().isNotEmpty())
        assertTrue(compose.onAllNodes(hasText("Moss Knight", substring = true)).fetchSemanticsNodes().isNotEmpty())
        save("99av-talent-wars-team")
    }
}
