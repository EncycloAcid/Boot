package com.boot.game

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import com.boot.core.Announcement
import com.boot.core.GameConcept
import com.boot.core.GameSize
import com.boot.core.Genre
import com.boot.core.Lane
import com.boot.core.Platform
import com.boot.core.PlatformInvite
import com.boot.core.ReleaseSlip
import com.boot.core.Sandbox
import com.boot.core.Topic
import com.boot.core.advance
import com.boot.core.startGame
import com.boot.game.ui.BootApp
import com.boot.game.ui.BootTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Announcing a game, picking its date on the calendar, and delaying it when it isn't ready. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReleaseCalendarTest {
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

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-xxhdpi")
    fun pickADateAndDelayIt() {
        val store = GameStore(RuntimeEnvironment.getApplication())
        store.clearHall()
        compose.setContent { BootTheme { BootApp(store) } }
        compose.runOnIdle {
            store.newGame("Pixel Forge", "You", Lane.GAMES, seed = 9)
            store.perform {
                Sandbox.setCash(this, 400_000.0)
                startGame(GameConcept("Moss Knight", Topic.FANTASY, Genre.RPG, platforms = listOf(Platform.SWITCHEROO_2), size = GameSize.SMALL))
                repeat(6) { advance() }
                Sandbox.enqueue(this, PlatformInvite("Nintendo", "a Nintendo Direct", 1))
            }
            store.todayOpen = false
        }
        compose.waitForIdle()
        // The trailer is the announcement, and the first wishlists.
        compose.onNodeWithText("Show a trailer").performClick()
        compose.waitForIdle()
        val p = store.state!!.games!!.project!!
        assertTrue(p.wishlists > 0)
        compose.runOnIdle { store.todayOpen = false }
        save("99k-sideways-wishlists")
        compose.onNodeWithText("${com.boot.core.Format.count(p.wishlists)} wishlists").fetchSemanticsNode()

        // A big rival release, ten weeks out.
        val now = store.state!!.week
        compose.runOnIdle {
            store.perform {
                games!!.market.upcoming += Announcement("Starfall III", "Bungie", "Starfall", now + 10, Topic.SCI_FI, Genre.ACTION, GameSize.AAA, mega = true)
            }
            store.openCalendar(1)
        }
        compose.waitForIdle()
        assertTrue(store.holdsClock)
        compose.onNodeWithText("Release calendar").fetchSemanticsNode()
        compose.onNodeWithContentDescription("Week 12").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        compose.onNodeWithText("Starfall III", substring = true).fetchSemanticsNode()
        save("99l-release-calendar", top = true)
        compose.onNodeWithText("Set as release date").performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertEquals(now + 12, store.state!!.games!!.project!!.releaseWeek)
        assertNull(store.calendarTeam)
        assertTrue(store.state!!.news.any { it.headline.startsWith("Moss Knight launches") })

        // Launch day comes and it isn't done.
        compose.runOnIdle { store.perform { Sandbox.enqueue(this, ReleaseSlip(1)) } }
        compose.waitForIdle()
        save("99m-release-slip", top = true)
        compose.onNodeWithText("Delay four weeks").performClick()
        compose.waitForIdle()
        val q = store.state!!.games!!.project!!
        assertEquals(1, q.delays)
        assertEquals(store.state!!.week + 4, q.releaseWeek)
    }
}
