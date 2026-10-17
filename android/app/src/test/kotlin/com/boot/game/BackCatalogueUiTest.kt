package com.boot.game

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import com.boot.core.Audience
import com.boot.core.BombReason
import com.boot.core.GameConcept
import com.boot.core.GameRelease
import com.boot.core.GameSize
import com.boot.core.Genre
import com.boot.core.Lane
import com.boot.core.Platform
import com.boot.core.Protection
import com.boot.core.RatingDispute
import com.boot.core.Research
import com.boot.core.ReviewBomb
import com.boot.core.Sandbox
import com.boot.core.Topic
import com.boot.core.isCollection
import com.boot.core.makeCollection
import com.boot.core.setProtection
import com.boot.core.startGame
import com.boot.core.stayFocused
import com.boot.core.advance
import com.boot.core.choose
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

/** Copy protection and ratings on the project card, collections, and the review bomb and ratings decisions. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BackCatalogueUiTest {
    @get:Rule(order = 0)
    val display = DisplayRule()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private lateinit var store: GameStore

    private fun save(name: String, top: Boolean = false) {
        compose.waitForIdle()
        val roots = compose.onAllNodes(androidx.compose.ui.test.isRoot())
        val index = if (top) roots.fetchSemanticsNodes().size - 1 else 0
        val view = (roots[index].fetchSemanticsNode().root as ViewRootForTest).view
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        File(DisplayRule.screenshots, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun release(id: Int, title: String, score: Double, week: Int, sequelOf: Int? = null, topic: Topic = Topic.FANTASY) = GameRelease(
        id = id, title = title,
        concept = GameConcept(title, topic, Genre.RPG, platforms = listOf(Platform.PC), size = GameSize.MEDIUM, sequelOf = sequelOf),
        week = week, reviews = emptyList(), score = score, weeklySales = 40_000.0, netShare = 0.7, design = 10.0, tech = 10.0,
        bugs = 0.0, hype = 0.0, devCost = 0.0, report = emptyList(), lifetimeSales = 24_000_000.0, unitsSold = 600_000.0, userScore = score,
    )

    private fun launch(seed: Long = 9) {
        store = GameStore(RuntimeEnvironment.getApplication())
        store.clearHall()
        compose.setContent { BootTheme { BootApp(store) } }
        compose.runOnIdle {
            store.newGame("Pixel Forge", "You", Lane.GAMES, seed = seed)
            store.todayOpen = false
        }
        compose.waitForIdle()
    }

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-xxhdpi")
    fun protectionAndRatingOnTheProjectCard() {
        launch()
        compose.runOnIdle {
            store.perform {
                Sandbox.setCash(this, 5_000_000.0)
                Sandbox.research(this, Research.TARGET_AUDIENCE)
                games!!.topics += Topic.HORROR
                startGame(GameConcept("Night Ward", Topic.HORROR, Genre.ADVENTURE, audience = Audience.MATURE, platforms = listOf(Platform.PC), size = GameSize.SMALL))
                setProtection(1, Protection.ACTIVATION)
                weeks(2)
            }
            store.tab = Tab.OFFICE
        }
        compose.waitForIdle()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasTestTag("shelf-1"))
        compose.waitForIdle()
        compose.onAllNodesWithText("Rated Mature 17+").onFirst().fetchSemanticsNode()
        save("99an-shelf-controls")
        compose.onAllNodesWithText("Go uncut").onFirst().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertTrue(store.state!!.games!!.project!!.uncut)
    }

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-xxhdpi")
    fun collectionsAndAReviewBomb() {
        launch()
        compose.runOnIdle {
            store.perform {
                Sandbox.setCash(this, 20_000_000.0)
                val g = games!!
                g.releases.add(0, release(9001, "Moss Knight", 8.6, week - 400))
                g.releases.add(0, release(9002, "Moss Knight 2", 8.1, week - 300, sequelOf = 9001))
                g.releases.add(0, release(9003, "Moss Knight 3", 8.4, week - 200, sequelOf = 9002))
                g.releases.add(0, release(9011, "Star Colony", 8.0, week - 300, topic = Topic.SPACE))
                g.releases.add(0, release(9012, "Star Colony 2", 7.6, week - 150, sequelOf = 9011, topic = Topic.SPACE))
                makeCollection(9001)
                repeat(14) {
                    while (events.isNotEmpty()) choose(events.first().choices.lastIndex)
                    if (pendingPivot != null) stayFocused()
                    advance()
                }
                while (events.isNotEmpty()) choose(events.first().choices.lastIndex)
                val bombed = release(9020, "Grey Days", 6.4, week - 2).apply {
                    protection = Protection.ALWAYS_ONLINE
                    userScore = 4.6
                }
                g.releases.add(0, bombed)
                Sandbox.enqueue(this, ReviewBomb(9020, BombReason.DRM, 1.8))
            }
        }
        compose.waitForIdle()
        assertTrue(store.state!!.games!!.releases.any { it.isCollection && it.title == "Moss Knight Trilogy" })
        compose.onAllNodesWithText("Grey Days is being review bombed").onFirst().fetchSemanticsNode()
        save("99ap-review-bomb", top = true)
        compose.onAllNodesWithText("Take the copy protection out").onFirst().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertEquals(Protection.NONE, store.state!!.games!!.release(9020)!!.protection)
        compose.runOnIdle { store.openStudio(StudioPage.GAMES) }
        compose.waitForIdle()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasTestTag("collections"))
        compose.waitForIdle()
        compose.onAllNodesWithText("Star Colony Double Pack").onFirst().fetchSemanticsNode()
        save("99ao-collections")
        compose.runOnIdle { store.openRelease = store.state!!.games!!.releases.first { it.isCollection }.id }
        compose.waitForIdle()
        save("99ar-collection-report", top = true)
    }

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-xxhdpi")
    fun theRatingsBoardSaysAdultsOnly() {
        launch()
        compose.runOnIdle {
            store.perform {
                Sandbox.setCash(this, 5_000_000.0)
                Sandbox.research(this, Research.TARGET_AUDIENCE)
                Sandbox.research(this, Research.MULTI_PLATFORM)
                games!!.topics += Topic.HORROR
                startGame(GameConcept("Night Ward", Topic.HORROR, Genre.ADVENTURE, audience = Audience.MATURE, platforms = listOf(Platform.PC, Platform.PLAYSYSTEM_5), size = GameSize.SMALL))
                Sandbox.enqueue(this, RatingDispute(1))
            }
        }
        compose.waitForIdle()
        compose.onAllNodesWithText("The ratings board says Adults Only").onFirst().fetchSemanticsNode()
        save("99aq-rating-dispute", top = true)
    }
}
