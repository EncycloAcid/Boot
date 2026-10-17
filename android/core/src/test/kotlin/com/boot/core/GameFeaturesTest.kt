package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameFeaturesTest {
    private val kit = listOf(
        Research.SAVE_SYSTEM, Research.PHYSICS, Research.SOUND_STEREO, Research.SOUND_SURROUND,
        Research.BASIC_AI, Research.ADVANCED_AI, Research.VOICE_ACTING, Research.DIALOGUE_TREES,
        Research.GRAPHICS_2D_V2, Research.GRAPHICS_3D_V1, Research.STYLE_PIXEL, Research.BRANCHING_STORY,
    )

    private fun studio(seed: Long = 3) = GameState.create("Pixel Forge", "F", Lane.GAMES, seed).apply {
        cash = 5_000_000.0
        games!!.engines += Engine(1, "Forge", kit, 0)
        repeat(2) { candidates.firstOrNull()?.let { c -> hire(c.id, Lane.GAMES) } }
    }

    private fun concept(genre: Genre = Genre.RPG, size: GameSize = GameSize.SMALL) =
        GameConcept("Moss Knight", Topic.FANTASY, genre, platforms = listOf(Platform.PC), size = size, engineId = 1)

    private fun GameState.weeks(n: Int) = repeat(n) {
        if (pendingPivot != null) stayFocused()
        while (events.isNotEmpty()) choose(events.first().choices.lastIndex)
        advance()
    }

    @Test
    fun featuresSortIntoGroupsAndLadders() {
        val engine = Engine(1, "Forge", kit, 0)
        val stage3 = engineGroups(engine, 2).toMap()
        assertEquals(listOf(Research.SOUND_STEREO, Research.SOUND_SURROUND), stage3[FeatureGroup.SOUND])
        // Up a ladder, the rungs below count too.
        assertEquals(Research.SOUND_STEREO.engineLevel + Research.SOUND_SURROUND.engineLevel, FeatureGroup.SOUND.points(Research.SOUND_SURROUND))
        // The designer starts with the top of every ladder and one art style.
        val picked = defaultFeatures(engine)
        assertTrue(Research.SOUND_SURROUND in picked && Research.SOUND_STEREO !in picked)
        assertTrue(Research.GRAPHICS_3D_V1 in picked && Research.STYLE_PIXEL in picked)
        // Every engine feature belongs somewhere.
        val homeless = Research.entries.filter { it.kind == Research.Kind.ENGINE && FeatureGroup.of(it) == null }
        assertTrue(homeless.isEmpty(), "$homeless")
    }

    @Test
    fun featuresCountForMoreWhereTheGenreCares() {
        // Voice acting is worth more in an RPG than in a puzzle-ish casual game.
        val voice = setOf(Research.VOICE_ACTING)
        assertTrue(featureBoost(concept(Genre.RPG), voice, 1) > featureBoost(concept(Genre.CASUAL), voice, 1))
        assertEquals(1.0, featureBoost(concept(), null, 1))
        assertEquals(1.0, featureBoost(concept(), voice, 0))
        assertTrue(featureCost(Research.PHOTOREAL, GameSize.AAA) > featureCost(Research.PHOTOREAL, GameSize.SMALL) * 50)
    }

    @Test
    fun aStagesFeaturesArePaidForWhenItStarts() {
        val s = studio()
        val features = defaultFeatures(s.games!!.engine(1))
        s.startGame(concept(), features = features)
        val p = s.games!!.project!!
        assertTrue(p.paidStages.isEmpty())
        val spent = p.spent
        s.weeks(1)
        assertTrue(0 in p.paidStages)
        assertTrue(p.spent - spent >= stageFeatureCost(features, 0, GameSize.SMALL) * 0.5)
        // Once paid, that stage's features are set; the next stage's can still change.
        assertNotNull(s.stagePlanBlocker(1, 0, emptySet(), emptyMap()))
        s.planStage(1, 2, features = emptySet())
        assertTrue(p.features!!.none { FeatureGroup.of(it)?.area?.stage == 2 })
        assertFailsWith<GameException> { s.planStage(1, 2, features = setOf(Research.PHOTOREAL)) }
    }

    @Test
    fun moreFeaturesMakeABetterGame() {
        fun craft(features: Set<Research>?): Double {
            val s = studio(seed = 11)
            s.startGame(concept(), features = features)
            repeat(300) {
                val p = s.games!!.project!!
                if (p.isReady && !s.needsDecision) return@repeat
                s.weeks(1)
            }
            val p = s.games!!.project!!
            return (p.design + p.tech) / p.work
        }
        val engine = Engine(1, "Forge", kit, 0)
        assertTrue(craft(defaultFeatures(engine)) > craft(emptySet()) * 1.05)
    }

    @Test
    fun leadsNeedTheResearchAndMakeTheirPartBetter() {
        val s = studio()
        s.startGame(concept())
        val e = s.leadCandidates().maxByOrNull { it.design }!!
        assertNotNull(s.stagePlanBlocker(1, 0, null, mapOf(FocusArea.STORY to e.id)))
        Sandbox.research(s, Research.LEAD_ROLES)
        assertNull(s.stagePlanBlocker(1, 0, null, mapOf(FocusArea.STORY to e.id)))
        // Nobody leads two areas in a stage.
        assertNotNull(s.stagePlanBlocker(1, 0, null, mapOf(FocusArea.STORY to e.id, FocusArea.GAMEPLAY to e.id)))
        s.planStage(1, 0, leads = mapOf(FocusArea.STORY to e.id))
        s.weeks(1)
        val p = s.games!!.project!!
        if (p.stage == 0) assertTrue(p.leadFactor > 1.0)
        assertTrue(leadFit(e, FocusArea.STORY) > 0)
    }

    @Test
    fun skippingWhatTheGenreNeedsShowsInTheReviews() {
        val engine = Engine(1, "Forge", kit, 0)
        // RPGs care about story; a game with the engine's story features left out is bare there.
        val bare = bareAreas(concept(Genre.RPG), emptySet(), engine)
        assertTrue(bare.isNotEmpty())
        assertTrue(bareAreas(concept(Genre.RPG), defaultFeatures(engine), engine).isEmpty())
        assertTrue(bareAreas(concept(Genre.RPG), null, engine).isEmpty())
    }

    @Test
    fun theNewStudioResearchDoesSomething() {
        val s = studio()
        Sandbox.research(s, Research.QA_DEPARTMENT)
        Sandbox.research(s, Research.FOCUS_GROUPS)
        Sandbox.research(s, Research.DESIGN_DOCS)
        val m = s.context(Lane.GAMES).mods
        assertEquals(0.8, m.bugs, 1e-9)
        assertEquals(2, m.insights)
        assertEquals(0.05, m.focus, 1e-9)
    }

    @Test
    fun oldSavesBuildTheOldWay() {
        val s = studio().apply { startGame(concept()) }
        val back = GameState.decode(s.encode())
        assertNull(back.games!!.project!!.features)
        assertTrue(back.games!!.project!!.leads.isEmpty())
    }
}
