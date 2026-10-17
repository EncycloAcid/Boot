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
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import com.boot.core.AwardNight
import com.boot.core.Category
import com.boot.core.Ceremony
import com.boot.core.EventKind
import com.boot.core.GameConcept
import com.boot.core.GameSize
import com.boot.core.Genre
import com.boot.core.Lane
import com.boot.core.Nominee
import com.boot.core.Platform
import com.boot.core.PlatformInvite
import com.boot.core.Sandbox
import com.boot.core.Topic
import com.boot.core.advance
import com.boot.core.choose
import com.boot.core.hire
import com.boot.core.launchGame
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

/** An award night, a stage report, a decision with faces on it, the reviews and the news. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ShowNightUiTest {
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

    private fun tap(text: String) = compose.onNodeWithText(text).performSemanticsAction(SemanticsActions.OnClick)

    private lateinit var store: GameStore

    private fun launch() {
        store = GameStore(RuntimeEnvironment.getApplication())
        store.clearHall()
        compose.setContent { BootTheme { BootApp(store) } }
        compose.runOnIdle {
            store.newGame("Pixel Forge", "You", Lane.GAMES, seed = 9)
            store.perform {
                Sandbox.setCash(this, 2_000_000.0)
                repeat(2) { candidates.firstOrNull()?.let { c -> hire(c.id, Lane.GAMES) } }
                startGame(GameConcept("Moss Knight", Topic.FANTASY, Genre.RPG, platforms = listOf(Platform.PLAYSYSTEM_5), size = GameSize.SMALL))
                weeks(6)
            }
            store.todayOpen = false
        }
        compose.waitForIdle()
    }

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-xxhdpi")
    fun anAwardNight() {
        launch()
        val nominees = listOf(
            Nominee("Little Lantern", "Pixel Forge", 9.1, true, Topic.FANTASY, Genre.ADVENTURE, GameSize.SMALL),
            Nominee("Iron Armada", "Big Studio", 9.3, false, Topic.MILITARY, Genre.ACTION, GameSize.AAA),
            Nominee("Neon Drift", "Velocity", 8.8, false, Topic.RACING, Genre.ACTION, GameSize.LARGE),
            Nominee("Tide Keeper", "Harbour", 8.6, false, Topic.PIRATES, Genre.STRATEGY, GameSize.MEDIUM),
        )
        compose.runOnIdle {
            store.perform {
                Sandbox.enqueue(
                    this,
                    AwardNight(
                        Ceremony(
                            "The BAFTA Games Awards",
                            listOf(Category("Artistic Achievement", nominees, 1), Category("Best Game", nominees, 0)),
                        ),
                    ),
                )
            }
        }
        compose.waitForIdle()
        // The envelopes open one by one.
        compose.mainClock.advanceTimeBy(1_700)
        compose.waitForIdle()
        compose.onNodeWithText("Winner: Iron Armada").fetchSemanticsNode()
        compose.mainClock.advanceTimeBy(1_700)
        compose.waitForIdle()
        compose.onNodeWithText("You won!").fetchSemanticsNode()
        compose.onNodeWithContentDescription("Winner: Little Lantern").fetchSemanticsNode()
        save("99r-award-night", top = true)
        tap("Thank the team")
        compose.waitForIdle()
        assertTrue(store.state!!.events.isEmpty())
    }

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-xxhdpi")
    fun aStageReportAndFacesOnADecision() {
        launch()
        compose.runOnIdle { store.perform { Sandbox.enqueue(this, PlatformInvite("Sony", "a PlayStation State of Play", 1)) } }
        compose.waitForIdle()
        save("99s-stage-invite", top = true)
        tap("Show a trailer")
        compose.waitForIdle()
        compose.onNodeWithText("Crowd").fetchSemanticsNode()
        save("99t-stage-report", top = true)
        tap("Back to work")
        compose.waitForIdle()
        val who = store.state!!.staff.first()
        compose.runOnIdle { store.perform { Sandbox.enqueue(this, EventKind.RaiseRequest(who.id)) } }
        compose.waitForIdle()
        compose.onAllNodesWithText(who.name)[0].fetchSemanticsNode()
        save("99u-event-faces", top = true)
    }

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-xxhdpi")
    fun theReviewsAndTheNews() {
        launch()
        compose.runOnIdle {
            store.perform {
                repeat(200) {
                    if (games!!.project?.isReady != false) return@repeat
                    while (needsDecision) choose(events.first().choices.lastIndex)
                    advance()
                }
                settleMilestones()
                launchGame()
            }
        }
        compose.waitForIdle()
        val r = store.state!!.games!!.releases.first()
        compose.onAllNodesWithText(r.reviews.first().headline, substring = true)[0].fetchSemanticsNode()
        compose.onAllNodes(hasScrollAction()).let { it[it.fetchSemanticsNodes().size - 1] }.performScrollToNode(hasText(r.reviews[2].comment, substring = true))
        compose.mainClock.advanceTimeBy(5_000)
        save("99v-reviews", top = true)
        compose.onAllNodes(hasScrollAction()).let { it[it.fetchSemanticsNodes().size - 1] }.performScrollToNode(hasText("Players"))
        save("99w-player-reviews", top = true)
        tap("Continue")
        compose.waitForIdle()
        compose.runOnIdle { store.tab = Tab.NEWS }
        compose.waitForIdle()
        compose.onAllNodes(hasScrollAction())[0].performScrollToNode(hasText("REVIEW"))
        save("99x-news")
        compose.onAllNodesWithText("REVIEW")[0].fetchSemanticsNode()
    }
}
