package com.boot.game

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.boot.core.Lane
import com.boot.core.acknowledgeReview
import com.boot.core.choose
import com.boot.game.ui.BootApp
import com.boot.game.ui.BootTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Screenshots of the Windows-style floating office shell on Android. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w393dp-h852dp-xxhdpi")
class WindowsLayoutShotTest {
    @get:Rule(order = 0)
    val display = DisplayRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private lateinit var store: GameStore

    @Before
    fun boot() {
        store = GameStore(RuntimeEnvironment.getApplication())
        store.clearHall()
        compose.setContent { BootTheme { BootApp(store) } }
        compose.waitForIdle()
    }

    private fun save(name: String) {
        compose.waitForIdle()
        val view = (compose.onRoot().fetchSemanticsNode().root as ViewRootForTest).view
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val media = File("/cursor/stores/bc-fea3e25b-5445-431d-9480-6019e889e493/media", "$name.png")
        media.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        File(DisplayRule.screenshots, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun clearBlocking() {
        repeat(8) {
            val s = store.state ?: return
            when {
                s.events.isNotEmpty() -> store.perform { choose(events[0].choices.lastIndex) }
                s.games?.unseenReview != null -> store.perform { acknowledgeReview() }
                else -> return
            }
            compose.waitForIdle()
        }
    }

    @Test
    fun officeStageWithHud() {
        compose.runOnIdle { store.newGame("Garage Labs", "Rae", Lane.GAMES, seed = 7, tutorial = false) }
        compose.waitForIdle()
        clearBlocking()
        compose.runOnIdle {
            store.tab = Tab.OFFICE
            // Dismiss the hire tip so Today does not auto-open over the stage.
            store.dismissedStep = "first-hire"
        }
        compose.waitForIdle()
        // Close Today if it already opened this frame.
        runCatching { compose.onNodeWithText("Close").performClick() }
        compose.waitForIdle()
        save("android-windows-layout-office")
    }

    @Test
    fun lanesFloatingPanel() {
        compose.runOnIdle { store.newGame("Garage Labs", "Rae", Lane.GAMES, seed = 7, tutorial = false) }
        compose.waitForIdle()
        clearBlocking()
        compose.runOnIdle { store.tab = Tab.LANES }
        compose.waitForIdle()
        save("android-windows-layout-lane")
    }

    @Test
    fun todayBriefingPanel() {
        compose.runOnIdle { store.newGame("Garage Labs", "Rae", Lane.GAMES, seed = 7, tutorial = true) }
        compose.waitForIdle()
        clearBlocking()
        compose.runOnIdle { store.tab = Tab.OFFICE }
        compose.waitForIdle()
        // Tutorial forces the Today briefing open over the office.
        compose.onNodeWithText("Today").assertExists()
        save("android-windows-layout-today")
    }
}

/** Tablet-width shot so the centered floating window is obvious over the office. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w800dp-h1280dp-xhdpi")
class WindowsLayoutTabletShotTest {
    @get:Rule(order = 0)
    val display = DisplayRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @Test
    fun lanesCenteredWindow() {
        val store = GameStore(RuntimeEnvironment.getApplication())
        store.clearHall()
        compose.setContent { BootTheme { BootApp(store) } }
        compose.waitForIdle()
        compose.runOnIdle { store.newGame("Garage Labs", "Rae", Lane.GAMES, seed = 7, tutorial = false) }
        compose.waitForIdle()
        compose.runOnIdle { store.tab = Tab.LANES }
        compose.waitForIdle()
        val view = (compose.onRoot().fetchSemanticsNode().root as ViewRootForTest).view
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File("/cursor/stores/bc-fea3e25b-5445-431d-9480-6019e889e493/media/android-windows-layout-lane-tablet.png")
            .outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
