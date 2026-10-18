package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Sequels and engines, one-time platform licences, engine builds, boxed editions, who works on what, and specialist-only features. */
class TycoonGapsTest {
    private fun studio(seed: Long = 5) = GameState.create("Pixel Forge", "F", Lane.GAMES, seed).apply {
        cash = 50_000_000.0
        researchPoints = 500.0
        repeat(3) { Sandbox.addStaff(this, "Dev $it", if (it == 0) Role.ENGINEER else Role.DESIGNER, Lane.GAMES) }
    }

    private fun GameState.weeks(n: Int) = repeat(n) {
        if (pendingPivot != null) stayFocused()
        while (events.isNotEmpty()) choose(events.first().choices.lastIndex)
        advance()
    }

    private fun GameState.ship(concept: GameConcept, setup: GameState.() -> Unit = {}): GameRelease {
        startGame(concept)
        setup()
        repeat(400) {
            val p = games!!.project ?: error("no project")
            if (p.isReady) {
                while (events.isNotEmpty()) choose(0)
                launchGame()
                return games!!.releases.first { it.title == concept.title }
            }
            weeks(1)
        }
        error("never shipped")
    }

    private fun concept(title: String = "Moss Knight", engineId: Int = 0, sequelOf: Int? = null, platforms: List<Platform> = listOf(Platform.PC)) =
        GameConcept(title, Topic.FANTASY, Genre.RPG, platforms = platforms, size = GameSize.SMALL, engineId = engineId, sequelOf = sequelOf)

    @Test
    fun aSequelWantsANewerEngine() {
        val kit = listOf(Research.GRAPHICS_2D_V2, Research.SOUND_STEREO, Research.SAVE_SYSTEM, Research.BASIC_AI)
        fun sequel(newEngine: Boolean): GameRelease {
            val s = studio(11)
            Sandbox.research(s, Research.SEQUELS)
            val first = s.ship(concept())
            s.weeks(60)
            if (newEngine) s.games!!.engines += Engine(7, "Forge", kit, s.week)
            return s.ship(concept("Moss Knight 2", engineId = if (newEngine) 7 else 0, sequelOf = first.id))
        }
        assertTrue(sequel(false).breakdown.any { it.label == "Same engine" && it.points < 0 })
        assertTrue(sequel(true).breakdown.any { it.label == "Newer engine" && it.points > 0 })
    }

    @Test
    fun aPlatformLicenceIsBoughtOnce() {
        val s = studio()
        Sandbox.research(s, Research.MULTI_PLATFORM)
        val ps5 = Platform.PLAYSYSTEM_5
        assertTrue(ps5.fee > 0)
        assertEquals(ps5.fee, s.licenceFees(listOf(ps5, Platform.PC)), 1e-9)
        val cash = s.cash
        s.startGame(concept(platforms = listOf(ps5)))
        assertTrue(ps5 in s.games!!.licences)
        assertTrue(s.cash <= cash - ps5.fee)
        assertEquals(0.0, s.licenceFees(listOf(ps5)), 1e-9)
        assertNotNull(s.licenceBlocker(ps5))
        // Another can be bought ahead of time.
        assertNull(s.licenceBlocker(Platform.MBOX_Z))
        s.buyLicence(Platform.MBOX_Z)
        assertEquals(0.0, s.licenceFees(listOf(Platform.MBOX_Z)), 1e-9)
    }

    @Test
    fun buildingAnEngineTakesTheTeam() {
        val s = studio()
        Sandbox.research(s, Research.CUSTOM_ENGINES)
        Sandbox.research(s, Research.GRAPHICS_2D_V2)
        Sandbox.research(s, Research.SOUND_STEREO)
        s.startGame(concept())
        assertFailsWith<GameException> { s.buildEngine() }
        s.games!!.setProject(1, null)
        s.buildEngine()
        assertTrue(s.games!!.busy)
        assertNotNull(s.conceptBlocker(concept()))
        s.weeks(s.games!!.engineBuild!!.weeksLeft + 1)
        assertNull(s.games!!.engineBuild)
        assertNull(s.conceptBlocker(concept()))
    }

    @Test
    fun boxedEditionsSellWhatYouOrder() {
        fun run(boxes: (Int) -> Int, collectors: Int = 0): GameRelease {
            val s = studio(21)
            Sandbox.research(s, Research.BOXED_EDITIONS)
            s.games!!.fans = 200_000.0
            return s.ship(concept()) {
                assertNull(s.editionsBlocker(1))
                val estimate = s.boxEstimate(1)
                assertTrue(estimate > 0)
                s.orderEditions(1, boxes(estimate), collectors)
            }
        }
        val distributor = run({ 0 })
        assertEquals(0, distributor.boxes)
        val tooMany = run({ it * 20 })
        assertTrue(tooMany.boxesSold < tooMany.boxes)
        val tooFew = run({ maxOf(1, it / 20) })
        assertEquals(tooFew.boxes, tooFew.boxesSold)
        // Buyers who couldn't find a box mostly went without.
        assertTrue(tooFew.weeklySales < distributor.weeklySales)
        val collectors = run({ 0 }, collectors = 2_000)
        assertTrue(collectors.collectorsSold > 0)
        // Before research, or with a publisher, there's nothing to order.
        val t = studio()
        t.startGame(concept())
        assertNotNull(t.editionsBlocker(1))
    }

    @Test
    fun puttingPeopleOnAreas() {
        val s = studio()
        val writer = Sandbox.addStaff(s, "Wren", Role.DESIGNER, Lane.GAMES, skill = 9)
        val coder = Sandbox.addStaff(s, "Theo", Role.ENGINEER, Lane.GAMES, skill = 3)
        s.startGame(concept())
        val p = s.games!!.project!!
        for (i in 0..2) p.focus[0][i] = listOf(0.1, 0.1, 0.8)[i]
        assertEquals(1.0, s.crewBoost(p, 0), 1e-9)
        // The best designer on the story helps; the weakest one doesn't.
        s.assignCrew(1, 0, writer.id, FocusArea.STORY)
        val good = s.crewBoost(p, 0)
        s.assignCrew(1, 0, writer.id, null)
        s.assignCrew(1, 0, coder.id, FocusArea.STORY)
        val bad = s.crewBoost(p, 0)
        assertTrue(good > bad, "$good vs $bad")
        // Everyone on the engine, which gets a tenth of the effort, wastes them.
        for (e in s.staff(Lane.GAMES)) s.assignCrew(1, 0, e.id, FocusArea.ENGINE)
        assertTrue(s.crewBoost(p, 0) < 0.9)
        assertNotNull(s.crewBlocker(1, 0, writer.id, FocusArea.SOUND))
        // Someone on another area doesn't count as a specialist here.
        val artist = Sandbox.addStaff(s, "Iris", Role.DESIGNER, Lane.GAMES, Specialty.WRITER)
        s.assignCrew(1, 0, artist.id, FocusArea.ENGINE)
        assertEquals(0.0, s.coverage(FocusArea.STORY, GameSize.SMALL, s.specialists(), p), 1e-9)
    }

    @Test
    fun theMostAdvancedFeaturesNeedASpecialist() {
        val s = studio()
        assertEquals(Specialty.ARTIST, Research.PHOTOREAL.needs())
        assertNull(Research.SOUND_STEREO.needs())
        assertNotNull(s.featureBlocker(Research.PHOTOREAL))
        val engine = Engine(3, "Forge", listOf(Research.GRAPHICS_3D_V2, Research.GRAPHICS_3D_V4, Research.SPATIAL_AUDIO, Research.SOUND_SURROUND), 0)
        val fitted = s.fitFeatures(setOf(Research.GRAPHICS_3D_V4, Research.SPATIAL_AUDIO), engine)!!
        // Without an artist the graphics step down a rung; without a composer the audio goes.
        assertEquals(setOf(Research.GRAPHICS_3D_V2, Research.SOUND_SURROUND), fitted)
        Sandbox.addStaff(s, "Iris", Role.DESIGNER, Lane.GAMES, Specialty.ARTIST)
        assertNull(s.featureBlocker(Research.PHOTOREAL))
    }
}
