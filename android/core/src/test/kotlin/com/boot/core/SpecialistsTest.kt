package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpecialistsTest {
    private fun studio(seed: Long = 5) = GameState.create("Pixel Forge", "F", Lane.GAMES, seed).apply {
        cash = 50_000_000.0
        researchPoints = 500.0
    }

    private fun GameState.person(name: String, specialty: Specialty? = null, role: Role = Role.DESIGNER, trait: Trait? = null, skill: Int = 6): Employee {
        val e = Employee(nextId(), name, role, skill, 2_000.0, 0.8, Lane.GAMES, week, trait = trait, specialty = specialty)
        staff = staff + e
        return e
    }

    private fun GameState.weeks(n: Int) = repeat(n) {
        if (pendingPivot != null) stayFocused()
        while (events.isNotEmpty()) choose(events.first().choices.lastIndex)
        advance()
    }

    private val rpg = GameConcept("Moss Knight", Topic.FANTASY, Genre.RPG, platforms = listOf(Platform.PC), size = GameSize.SMALL)

    @Test
    fun everyAreaHasExactlyOneSpecialty() {
        for (area in FocusArea.entries) assertEquals(1, Specialty.entries.count { area in it.areas }, "$area")
        assertEquals(Specialty.WRITER, Specialty.of(FocusArea.DIALOGUES))
        assertEquals(Specialty.COMPOSER, Specialty.of(FocusArea.SOUND))
        assertEquals(3, specialistsNeeded(GameSize.AAA))
    }

    @Test
    fun specialistsMakeTheirAreasBetter() {
        val s = studio()
        val storyHeavy = listOf(0.1, 0.1, 0.8)
        val before = s.specialistBoost(rpg, 0, storyHeavy)
        assertEquals(1.0, before, 1e-9)
        s.person("Wren", Specialty.WRITER)
        val after = s.specialistBoost(rpg, 0, storyHeavy)
        assertTrue(after > 1.15, "$after")
        // A writer does nothing for the engine.
        assertEquals(1.0, s.specialistBoost(rpg, 0, listOf(1.0, 0.0, 0.0)), 1e-9)
        // A big game needs more of them.
        assertEquals(1.0, s.coverage(FocusArea.STORY, GameSize.SMALL), 1e-9)
        assertTrue(s.coverage(FocusArea.STORY, GameSize.AAA) < 0.5)
        // Someone away on a course doesn't count.
        s.staff.first { it.specialty == Specialty.WRITER }.leaveUntil = s.week + 3
        assertEquals(0.0, s.coverage(FocusArea.STORY, GameSize.SMALL), 1e-9)
    }

    @Test
    fun chemistryIsFixedAndShapedByWhoPeopleAre() {
        val s = studio()
        val a = s.person("Ana")
        val b = s.person("Ben")
        assertEquals(s.chemistry(a, b), s.chemistry(b, a))
        val d1 = s.person("Dee", trait = Trait.DIVA)
        val d2 = s.person("Dax", trait = Trait.DIVA)
        assertEquals(Chemistry.BAD, s.chemistry(d1, d2))
        val tp = s.person("Tia", trait = Trait.TEAM_PLAYER)
        assertTrue((1..40).none { s.chemistry(tp, s.person("P$it")) == Chemistry.BAD })
        // Across many pairs, most just get along.
        val t = studio(9)
        val people = (1..30).map { t.person("Q$it") }
        val all = people.flatMapIndexed { i, x -> people.drop(i + 1).map { y -> t.chemistry(x, y) } }
        val good = all.count { it == Chemistry.GOOD }.toDouble() / all.size
        val bad = all.count { it == Chemistry.BAD }.toDouble() / all.size
        assertTrue(good in 0.07..0.18 && bad in 0.04..0.13, "$good $bad")
    }

    @Test
    fun goodPairsLiftTheTeamAndClashesDragIt() {
        val s = studio()
        s.person("Dee", trait = Trait.DIVA)
        s.person("Dax", trait = Trait.DIVA)
        val pairs = s.teamPairs()
        assertTrue(pairs.any { it.chemistry == Chemistry.BAD })
        assertTrue(s.chemistryFactor(listOf(pairs.first { it.chemistry == Chemistry.BAD })) < 1.0)
        // You find out by working together.
        s.startGame(rpg)
        assertTrue(s.teamPairs().none { it.known })
        s.weeks(60)
        assertTrue(s.games!!.knownPairs.isNotEmpty())
    }

    @Test
    fun becomingASpecialist() {
        val s = studio()
        val marketer = s.person("Max", role = Role.MARKETER)
        assertNotNull(s.specialiseBlocker(marketer, Specialty.WRITER))
        val e = s.person("Eve")
        assertNull(s.specialiseBlocker(e, Specialty.COMPOSER))
        val rp = s.researchPoints
        s.specialise(e.id, Specialty.COMPOSER)
        assertEquals(Specialty.COMPOSER, e.specialty)
        assertTrue(s.researchPoints < rp)
        assertTrue(e.leaveUntil > s.week)
        // Not again this year.
        assertNotNull(s.specialiseBlocker(e, Specialty.WRITER))
        // Specialists lead their own areas better.
        assertTrue(leadFit(e, FocusArea.SOUND) > leadFit(e.copy(specialty = null), FocusArea.SOUND))
    }

    @Test
    fun someNewHiresComeWithASpecialty() {
        val s = studio()
        val specialties = (1..200).map { s.specialtyFor(it, Role.DESIGNER) }
        assertTrue(specialties.count { it != null } in 80..140)
        assertTrue(specialties.filterNotNull().toSet().size >= 4)
        assertNull(s.specialtyFor(5, Role.MARKETER))
    }

    @Test
    fun aGameWithSpecialistsOnItsKeyAreasReviewsBetter() {
        val s = studio(11)
        repeat(2) { s.candidates.firstOrNull()?.let { c -> s.hire(c.id, Lane.GAMES) } }
        for (area in keyAreas(rpg)) s.person("Spec ${area.title}", Specialty.of(area))
        s.startGame(rpg)
        repeat(300) {
            val p = s.games!!.project!!
            if (p.isReady) {
                assertTrue(keyAreas(rpg).all { it in p.covered }, "${p.covered}")
                while (s.events.isNotEmpty()) s.choose(0)
                s.launchGame()
                val r = s.games!!.releases.first()
                assertTrue(r.breakdown.any { it.label == "Specialists" && it.points > 0 })
                return
            }
            s.weeks(1)
        }
        error("never shipped")
    }

    @Test
    fun oldSavesLoad() {
        val s = studio()
        s.person("Wren", Specialty.WRITER)
        val back = GameState.decode(s.encode())
        assertEquals(Specialty.WRITER, back.staff.first { it.name == "Wren" }.specialty)
    }
}
