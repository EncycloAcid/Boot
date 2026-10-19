package com.boot.game

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performScrollToNode
import com.boot.core.Lane
import com.boot.core.LegacyBeatKind
import com.boot.core.Sandbox
import com.boot.core.buyIp
import com.boot.core.ipsForSale
import com.boot.core.museumBeat
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

/** Antique IP vault, transparency, museum wing affordances. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LegacyTrustUiTest {
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
    fun studioShowsTransparencyAndAntiqueVault() {
        val store = GameStore(RuntimeEnvironment.getApplication())
        store.clearHall()
        compose.setContent { BootTheme { BootApp(store) } }
        compose.runOnIdle {
            store.newGame("Legacy Soft", "You", Lane.GAMES, seed = 19)
            store.perform {
                Sandbox.setCash(this, 80_000_000.0)
                Sandbox.jumpTo(this, 120)
                games!!.transparency = 0.72
                games!!.diariesPublished = 3
                val id = ipsForSale().first { it.genre.starter && it.topic.starter }.id
                buyIp(id)
            }
            store.todayOpen = false
            store.openStudio(StudioPage.MARKET)
        }
        compose.waitForIdle()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Transparency", substring = true))
        compose.waitForIdle()
        assertTrue(
            compose.onAllNodes(hasText("Open book", substring = true)).fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodes(hasText("Talkative", substring = true)).fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodes(hasText("Transparency", substring = true)).fetchSemanticsNodes().isNotEmpty(),
        )
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("IP vault", substring = true))
        compose.waitForIdle()
        assertTrue(compose.onAllNodes(hasText("Remake")).fetchSemanticsNodes().isNotEmpty())
        save("99aw-legacy-trust-studio")
    }

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-xxhdpi")
    fun companyMuseumShowsTimeline() {
        val store = GameStore(RuntimeEnvironment.getApplication())
        store.clearHall()
        compose.setContent { BootTheme { BootApp(store) } }
        compose.runOnIdle {
            store.newGame("Museum Co", "You", Lane.GAMES, seed = 7)
            store.perform {
                museumBeat(LegacyBeatKind.HIT, "Hit: Moss Knight", "Scored 8.4.")
                museumBeat(LegacyBeatKind.SCAR, "Press scandal", "A rough week.")
                museumBeat(LegacyBeatKind.TRIUMPH, "IPO: MUSM", "Rang the bell.")
            }
            store.todayOpen = false
            store.companyPage = 6
            store.tab = Tab.COMPANY
        }
        compose.waitForIdle()
        assertTrue(compose.onAllNodes(hasText("Museum", substring = true)).fetchSemanticsNodes().isNotEmpty())
        assertTrue(compose.onAllNodes(hasText("Moss Knight", substring = true)).fetchSemanticsNodes().isNotEmpty())
        assertTrue(compose.onAllNodes(hasText("Press scandal", substring = true)).fetchSemanticsNodes().isNotEmpty())
        save("99ax-legacy-trust-museum")
    }
}
