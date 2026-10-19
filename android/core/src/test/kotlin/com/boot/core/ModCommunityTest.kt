package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Late-game PC mod / community scene: formation, levers, crises, era gates, save compat. */
class ModCommunityTest {
    private fun studio(seed: Long = 11, start: StartPoint = StartPoint.TODAY) =
        GameState.create("Pixel Forge", "F", Lane.GAMES, seed, start = start).apply { cash = 50_000_000.0 }

    private fun concept(genre: Genre = Genre.SIMULATION, platforms: List<Platform> = listOf(Platform.PC)) =
        GameConcept("Moss Knight", Topic.FANTASY, genre, platforms = platforms, size = GameSize.MEDIUM)

    private fun GameState.plant(age: Int = 40, score: Double = 8.0): GameRelease {
        val r = GameRelease(
            id = 9001, title = "Moss Knight", concept = concept(), week = week - age, reviews = emptyList(),
            score = score, weeklySales = 25_000.0, netShare = 0.7, design = 10.0, tech = 10.0, bugs = 0.0,
            hype = 0.0, devCost = 0.0, report = emptyList(), userScore = score - 0.3, unitsSold = 500_000.0,
        )
        games!!.releases.add(0, r)
        return r
    }

    @Test
    fun eraGatesModCulture() {
        val early = studio(start = StartPoint.CONSOLE_WARS)
        assertEquals(0.0, early.modEraStrength(), 1e-9)
        val mid = studio(start = StartPoint.THE_WEB)
        assertTrue(mid.modEraStrength() in 0.2..0.6)
        val now = studio()
        assertEquals(1.0, now.modEraStrength(), 1e-9)
    }

    @Test
    fun consoleOnlyNeverGetsAScene() {
        val s = studio()
        val console = GameRelease(
            id = 9002, title = "Pad Only", concept = concept(platforms = listOf(Platform.PLAYSYSTEM_5)),
            week = s.week - 40, reviews = emptyList(), score = 8.5, weeklySales = 20_000.0, netShare = 0.7,
            design = 10.0, tech = 10.0, bugs = 0.0, hype = 0.0, devCost = 0.0, report = emptyList(), userScore = 8.0,
        )
        s.games!!.releases.add(console)
        assertNotNull(s.modToolsBlocker(console))
        assertFalse(console.onPc())
        assertEquals(0.0, console.modSalesLift(), 1e-9)
    }

    @Test
    fun toolsFormASceneAndLeversWork() {
        val s = studio()
        val r = s.plant()
        assertNull(s.modToolsBlocker(r))
        s.releaseModTools(r.id)
        assertTrue(r.modTools)
        assertTrue(r.hasModScene)
        assertTrue(r.modBuzz > 0)
        val encoded = s.encode()
        val loaded = GameState.decode(encoded)
        val lr = loaded.games!!.release(r.id)!!
        assertTrue(lr.hasModScene)
        assertEquals(r.modPolicy, lr.modPolicy)

        s.supportModDocs(r.id)
        assertEquals(ModPolicy.SUPPORT, r.modPolicy)
        assertTrue(r.modHealth > 0.7)

        s.releaseOfficialModKit(r.id)
        assertTrue(r.modKit)
        assertEquals(ModPolicy.OFFICIAL, r.modPolicy)

        r.modBuzz = 0.5
        s.partnerModTeam(r.id)
        assertNotNull(r.modPartner)
        assertEquals(ModPolicy.PARTNER, r.modPolicy)

        s.clampMods(r.id)
        assertEquals(ModPolicy.CLAMP, r.modPolicy)
        assertTrue(r.modBuzz < 0.3)

        s.ignoreModScene(r.id)
        assertEquals(ModPolicy.IGNORE, r.modPolicy)
    }

    @Test
    fun oldSavesLoadWithoutModFields() {
        val s = studio()
        val r = s.plant()
        // Encode then decode — new defaults must apply.
        val again = GameState.decode(s.encode())
        val r2 = again.games!!.release(r.id)!!
        assertEquals(-1, r2.modSceneWeek)
        assertEquals(ModPolicy.IGNORE, r2.modPolicy)
        assertFalse(r2.modKit)
        assertNull(r2.modPartner)
        assertFalse(r2.brokenMods)
        assertEquals(0.0, r2.modSalesLift(), 1e-9)
    }

    @Test
    fun healthySceneLiftsSalesDecay() {
        val s = studio()
        val r = s.plant()
        assertEquals(0.0, r.modSalesLift(), 1e-9)
        r.modTools = true
        assertEquals(0.008, r.modSalesLift(), 1e-9)
        r.modSceneWeek = maxOf(0, s.week - 10)
        r.modBuzz = 0.8
        r.modHealth = 0.9
        assertTrue(r.modSalesLift() > 0.01)
        assertTrue(r.modSalesLift() <= 0.03)
    }

    @Test
    fun modHitAndCrisisRoundTrip() {
        val s = studio()
        val r = s.plant()
        r.modTools = true
        r.modToolsWeek = s.week - 10
        r.modSceneWeek = s.week - 10
        r.modBuzz = 0.6
        Sandbox.enqueue(s, ModHit(r.id, "${r.title}: Reforged", "Ada Lin"))
        assertEquals(ModHit(r.id, "${r.title}: Reforged", "Ada Lin"), GameState.decode(s.encode()).events.first { it.kind is ModHit }.kind)
        s.choose(2)

        Sandbox.enqueue(s, ModCrisis(r.id, ModCrisisKind.TOXIC))
        assertTrue(GameState.decode(s.encode()).events.any { it.kind is ModCrisis })
        s.choose(0)
        assertEquals(ModPolicy.SUPPORT, r.modPolicy)

        r.brokenMods = true
        assertNull(s.fixBrokenModsBlocker(r))
        s.fixBrokenMods(r.id)
        assertFalse(r.brokenMods)
    }

    @Test
    fun casualGenreBlocksTools() {
        val s = studio()
        val r = GameRelease(
            id = 7, title = "Candy", concept = concept(genre = Genre.CASUAL), week = s.week - 20,
            reviews = emptyList(), score = 7.0, weeklySales = 10_000.0, netShare = 0.7, design = 8.0, tech = 8.0,
            bugs = 0.0, hype = 0.0, devCost = 0.0, report = emptyList(), userScore = 7.0,
        )
        s.games!!.releases.add(r)
        assertNotNull(s.modToolsBlocker(r))
        assertFailsWith<GameException> { s.releaseModTools(r.id) }
    }
}
