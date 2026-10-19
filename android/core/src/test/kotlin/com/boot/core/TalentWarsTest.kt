package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
class TalentWarsTest {

    private fun game(lane: Lane = Lane.GAMES, seed: Long = 42): GameState =
        GameState.create("Arc Studio", "Founder", lane, seed).also { it.cash = 5_000_000.0 }

    @Test
    fun starCandidatesCarryACareerArc() {
        val s = game()
        val star = s.makeCandidate(star = true)
        assertTrue(star.notable || star.skill >= 8)
        assertTrue(star.reputation > 0.0)
        assertTrue(star.chapter == CareerChapter.PEAK || star.chapter == CareerChapter.LEGEND || star.chapter == CareerChapter.RISING)
        val e = s.hireDirect(star, Lane.GAMES)
        assertEquals(star.chapter, e.chapter)
        assertEquals(star.pastHits, e.pastHits)
        assertTrue(e.isStar)
    }

    @Test
    fun oldSaveDefaultsKeepEveryoneRisingAndLoyalEnough() {
        val s = game(Lane.WEBSITES)
        val plain = Employee(1, "Pat Old", Role.ENGINEER, 5, 2_000.0, 0.8, Lane.WEBSITES, 0)
        assertEquals(CareerChapter.RISING, plain.chapter)
        assertEquals(0.55, plain.loyalty, 0.001)
        assertEquals(0.0, plain.reputation, 0.001)
        assertFalse(plain.notable)
        assertTrue(plain.pastHits.isEmpty())
        s.staff = listOf(plain)
        val encoded = s.encode()
        val loaded = GameState.decode(encoded)
        val e = loaded.staff.single()
        assertEquals(CareerChapter.RISING, e.chapter)
        assertEquals(0.55, e.loyalty, 0.001)
    }

    @Test
    fun counterOfferRaisesLoyaltyAndClearsWantedBy() {
        val s = game(Lane.WEBSITES)
        val c = s.makeCandidate(Role.ENGINEER).copy(skill = 9, notable = true, chapter = CareerChapter.PEAK, loyalty = 0.3)
        val e = s.hireDirect(c, Lane.WEBSITES)
        e.wantedBy = listOf("RivalCo")
        e.offerPressure = 0.7
        s.enqueue(EventKind.Poach(e.id, "RivalCo"))
        s.choose(0)
        val kept = s.staff.single()
        assertTrue(kept.loyalty > 0.3)
        assertTrue(kept.wantedBy.isEmpty())
        assertEquals(0.0, kept.offerPressure, 0.001)
    }

    @Test
    fun lettingAStarGoStrengthensTheRival() {
        val s = game(Lane.WEBSITES)
        val r = s.world.rival(Lane.WEBSITES)!!
        val before = r.strength
        val c = s.makeCandidate(Role.ENGINEER).copy(skill = 9, notable = true, reputation = 0.6, chapter = CareerChapter.PEAK)
        val e = s.hireDirect(c, Lane.WEBSITES)
        s.enqueue(EventKind.Poach(e.id, r.name))
        s.choose(1)
        assertTrue(s.staff.isEmpty())
        assertTrue(r.strength > before)
    }

    @Test
    fun poachingARivalStarHiresSomeoneWithAnArc() {
        val s = game(Lane.WEBSITES)
        val r = s.world.rival(Lane.WEBSITES)!!
        s.counter(r.name, CounterMove.POACH_STAR)
        val hired = s.staff.single()
        assertTrue(hired.skill >= 8)
        assertTrue(hired.notable || hired.isStar)
        assertTrue(hired.pastTitles.any { it.contains(r.name) } || hired.chapter != CareerChapter.RISING || hired.reputation > 0)
        assertEquals(Lane.WEBSITES, hired.lane)
    }

    @Test
    fun poachingAMarketStarCostsASigningBonus() {
        val s = game(Lane.WEBSITES)
        s.office = Office.STUDIO
        while (s.candidates.none { it.notable || it.skill >= 8 }) {
            s.candidates = s.candidates + s.makeCandidate(star = true)
        }
        val star = s.candidates.first { it.notable || it.skill >= 8 }
        val before = s.cash
        s.poachMarketStar(star.id, Lane.WEBSITES)
        assertTrue(s.cash < before - star.askingSalary * 2)
        assertEquals(1, s.staff.size)
        assertTrue(s.staff.single().isStar)
    }

    @Test
    fun loyalTraitBlocksSoftPoachTargeting() {
        val s = game(Lane.WEBSITES)
        val loyal = s.hireDirect(
            s.makeCandidate(Role.ENGINEER).copy(skill = 9, trait = Trait.LOYAL, notable = true, loyalty = 0.2),
            Lane.WEBSITES,
        )
        assertEquals(1.0, loyal.effectiveLoyalty, 0.001)
        val r = s.world.rival(Lane.WEBSITES)!!
        // Force many rolls — Loyal should never be enqueued as a soft poach.
        repeat(120) { s.tryRivalPoach(r) }
        assertTrue(s.events.none { (it.kind as? EventKind.Poach)?.employeeId == loyal.id })
    }

    @Test
    fun shippingAHitCreditsPastHitsAndCanAdvanceChapter() {
        val s = game(Lane.GAMES)
        val e = s.hireDirect(
            s.makeCandidate(Role.DESIGNER).copy(
                skill = 9, notable = true, chapter = CareerChapter.RISING, reputation = 0.35, loyalty = 0.5,
            ),
            Lane.GAMES,
        )
        s.creditCareerOnShip("Neon Courier", 9.0)
        assertTrue("Neon Courier" in e.pastHits)
        assertTrue(e.reputation > 0.35)
        assertTrue(e.chapter == CareerChapter.PEAK || e.chapter == CareerChapter.LEGEND || e.chapter == CareerChapter.RISING)
    }

    @Test
    fun specialisingAddsACareerTitle() {
        val s = game(Lane.GAMES)
        s.researchPoints = 20.0
        val e = s.hireDirect(s.makeCandidate(Role.DESIGNER).copy(specialty = null, skill = 6), Lane.GAMES)
        s.specialise(e.id, Specialty.WRITER)
        assertEquals(Specialty.WRITER, e.specialty)
        assertTrue(e.pastTitles.any { it == Specialty.WRITER.person })
    }

    @Test
    fun careerFieldsSurviveEncodeDecode() {
        val s = game()
        val e = s.hireDirect(
            s.makeCandidate(star = true).copy(
                chapter = CareerChapter.PEAK,
                pastTitles = listOf("Lead designer"),
                pastHits = listOf("Moss Knight"),
            ),
            Lane.GAMES,
        )
        e.loyalty = 0.42
        e.reputation = 0.61
        e.wantedBy = listOf("Nintendo")
        e.offerPressure = 0.4
        val loaded = GameState.decode(s.encode())
        val again = loaded.staff.single { it.id == e.id }
        assertEquals(CareerChapter.PEAK, again.chapter)
        assertEquals(listOf("Lead designer"), again.pastTitles)
        assertEquals(listOf("Moss Knight"), again.pastHits)
        assertEquals(listOf("Nintendo"), again.wantedBy)
        assertEquals(0.4, again.offerPressure, 0.001)
        assertEquals(0.42, again.loyalty, 0.001)
        assertEquals(0.61, again.reputation, 0.001)
    }
}
