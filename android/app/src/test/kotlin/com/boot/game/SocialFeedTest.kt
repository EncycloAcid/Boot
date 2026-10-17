package com.boot.game

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import com.boot.core.GameConcept
import com.boot.core.GameSize
import com.boot.core.Genre
import com.boot.core.Lane
import com.boot.core.Platform
import com.boot.core.PlatformInvite
import com.boot.core.Sandbox
import com.boot.core.Topic
import com.boot.core.advance
import com.boot.core.choose
import com.boot.core.startGame
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

/** The feed: what people say after a trailer, on the front page and on its own page. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SocialFeedTest {
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
    @Config(qualifiers = "w852dp-h393dp-land-xxhdpi")
    fun whatPeopleAreSaying() {
        val store = GameStore(RuntimeEnvironment.getApplication())
        store.clearHall()
        compose.setContent { BootTheme { BootApp(store) } }
        compose.runOnIdle {
            store.newGame("Pixel Forge", "You", Lane.GAMES, seed = 9)
            store.perform {
                Sandbox.setCash(this, 2_000_000.0)
                startGame(GameConcept("Moss Knight", Topic.FANTASY, Genre.RPG, platforms = listOf(Platform.PLAYSYSTEM_5), size = GameSize.SMALL))
                weeks(8)
                Sandbox.enqueue(this, PlatformInvite("Sony", "a PlayStation State of Play", 1))
                choose(0)
                while (needsDecision) choose(0)
                repeat(6) {
                    while (needsDecision) choose(events.first().choices.lastIndex)
                    advance()
                }
                while (needsDecision) choose(events.first().choices.lastIndex)
            }
            store.todayOpen = false
            store.tab = Tab.NEWS
        }
        compose.waitForIdle()
        assertTrue(store.state!!.feed.isNotEmpty())
        compose.onAllNodes(hasScrollAction())[0].performScrollToNode(hasText("On Chirp"))
        save("99ad-front-page-feed")
        compose.onAllNodes(hasScrollAction())[0].performScrollToNode(hasText("Front page"))
        compose.onAllNodesWithText("Chirp")[0].performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        compose.onNodeWithText("Trending").fetchSemanticsNode()
        compose.onAllNodesWithText("#MossKnight", substring = true)[0].fetchSemanticsNode()
        compose.onAllNodes(hasScrollAction())[0].performScrollToNode(hasText("Trending"))
        compose.onAllNodes(hasScrollAction())[0].performScrollToNode(hasText(store.state!!.feed.last().text))
        save("99ae-social-feed")
    }
}
