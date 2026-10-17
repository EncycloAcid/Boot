package com.boot.game

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import com.boot.core.FounderSkill
import com.boot.core.FounderTrait
import com.boot.core.Lane
import com.boot.core.Sandbox
import com.boot.core.StoryKind
import com.boot.core.activeStories
import com.boot.core.founderXpToNext
import com.boot.core.hire
import com.boot.core.skillPoints
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

/** Growing as a founder (levels, the skill tree, a second trait) and a staff story from start to Team tab. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GrowthAndStoriesTest {
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
            store.newGame("Pixel Forge", "You", Lane.GAMES, seed = 9, trait = FounderTrait.CODER)
            store.todayOpen = false
        }
        compose.waitForIdle()
    }

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-xxhdpi")
    fun levelUpLearnASkillAndPickASecondTrait() {
        launch()
        compose.runOnIdle {
            store.perform { repeat(4) { Sandbox.founderXp(this, founderXpToNext(founder.level)) } }
            store.founderOpen = true
        }
        compose.waitForIdle()
        assertEquals(5, store.state!!.founder.level)
        assertTrue(store.holdsClock)
        compose.onNodeWithText("Level 5 · 4 skill points and a second trait to spend").fetchSemanticsNode()
        tap("Hands-on")
        compose.waitForIdle()
        tap("Learn hands-on")
        compose.waitForIdle()
        assertTrue(FounderSkill.HANDS_ON in store.state!!.founder.skills)
        tap("Pitch")
        compose.waitForIdle()
        save("99n-founder-skills", top = true)
        tap("Learn pitch")
        compose.waitForIdle()
        tap("Showrunner")
        compose.waitForIdle()
        save("99q-second-trait", top = true)
        tap("Become a showrunner")
        compose.waitForIdle()
        assertEquals(FounderTrait.SHOWRUNNER, store.state!!.founder.trait2)
        assertEquals(2, store.state!!.founder.skillPoints)
    }

    @Test
    @Config(qualifiers = "w852dp-h393dp-land-xxhdpi")
    fun aDreamTeamStory() {
        launch()
        compose.runOnIdle {
            store.perform {
                Sandbox.setCash(this, 500_000.0)
                val roles = candidates.distinctBy { it.role }.take(2)
                for (c in roles) hire(c.id, Lane.GAMES)
                Sandbox.startStory(this, StoryKind.DREAM_TEAM, staff.map { it.id }.take(2))
            }
        }
        compose.waitForIdle()
        val a = store.state!!.staff[0]
        compose.onNodeWithText("${a.name} and ${store.state!!.staff[1].name} are on a roll").fetchSemanticsNode()
        save("99o-story-dream-team", top = true)
        compose.onNodeWithText("Pair them up").performClick()
        compose.waitForIdle()
        assertEquals(1, store.state!!.activeStories.size)
        compose.runOnIdle { store.tab = Tab.TEAM }
        compose.waitForIdle()
        compose.onAllNodes(hasScrollAction())[0].performScrollToNode(hasText("Now and then", substring = true))
        compose.onNodeWithText("Underway").fetchSemanticsNode()
        save("99p-team-stories")
    }
}
