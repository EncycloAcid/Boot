package com.boot.game

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import com.boot.core.GameConcept
import com.boot.core.GameSize
import com.boot.core.Genre
import com.boot.core.Lane
import com.boot.core.Platform
import com.boot.core.Role
import com.boot.core.Sandbox
import com.boot.core.Specialty
import com.boot.core.Topic
import com.boot.core.Trait
import com.boot.core.pairKey
import com.boot.core.startGame
import com.boot.core.teamPairs
import com.boot.game.ui.BootApp
import com.boot.game.ui.BootTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Specialists on the stage areas, team chemistry, and making someone a specialist. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TeamFitUiTest {
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
    fun aTeamOfSpecialists() {
        val store = GameStore(RuntimeEnvironment.getApplication())
        store.clearHall()
        compose.setContent { BootTheme { BootApp(store) } }
        compose.runOnIdle {
            store.newGame("Pixel Forge", "You", Lane.GAMES, seed = 9)
            store.perform {
                Sandbox.setCash(this, 2_000_000.0)
                Sandbox.setResearchPoints(this, 100.0)
                fun add(name: String, specialty: Specialty?, role: Role = Role.DESIGNER, trait: Trait? = null) {
                    Sandbox.addStaff(this, name, role, Lane.GAMES, specialty, trait)
                }
                add("Wren Hale", Specialty.WRITER)
                add("Iris Vale", Specialty.ARTIST)
                add("Theo Park", Specialty.ENGINE_PROGRAMMER, Role.ENGINEER)
                add("Dee Moss", null, trait = Trait.DIVA)
                add("Dax Rook", null, trait = Trait.DIVA)
                startGame(GameConcept("Moss Knight", Topic.FANTASY, Genre.RPG, platforms = listOf(Platform.PC), size = GameSize.SMALL))
                // They've worked out who gets on with whom.
                for (pair in teamPairs()) games!!.knownPairs += pairKey(pair.a.id, pair.b.id)
            }
            store.todayOpen = false
            store.openStudio(StudioPage.MAKE)
        }
        compose.waitForIdle()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasTestTag("team-fit"))
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("What rpg games need most", substring = true, ignoreCase = true))
        compose.waitForIdle()
        save("99as-team-fit")
        compose.runOnIdle { store.tab = Tab.TEAM }
        compose.waitForIdle()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Dee Moss"))
        compose.waitForIdle()
        save("99at-team-specialists")
        compose.onAllNodesWithContentDescription("Manage Dee Moss").onFirst().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        compose.onAllNodesWithText("Make a specialist").onFirst().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        save("99au-specialise", top = true)
        compose.onAllNodesWithText("Composer").onFirst().performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        assertEquals(Specialty.COMPOSER, store.state!!.staff.first { it.name == "Dee Moss" }.specialty)
    }
}
