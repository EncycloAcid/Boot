package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FounderGrowthTest {
    private fun game(trait: FounderTrait? = null, seed: Long = 4) =
        GameState.create("T", "Sam", Lane.GAMES, seed).apply { introduceFounder(trait, null) }

    @Test
    fun runningTheCompanyEarnsLevels() {
        val s = game()
        assertEquals(1, s.founder.level)
        assertEquals(0, s.founder.skillPoints)
        s.founderXp(founderXpToNext(1) - 1)
        assertEquals(1, s.founder.level)
        s.founderXp(1.0)
        assertEquals(2, s.founder.level)
        assertEquals(1, s.founder.skillPoints)
        assertTrue(s.log.any { "You reached level 2" in it.text })
        // Plenty at once can be several levels.
        s.founderXp(10_000.0)
        assertEquals(FOUNDER_MAX_LEVEL, s.founder.level)
        assertEquals(FOUNDER_MAX_LEVEL - 1, s.founder.skillPoints)
        // Every week of work counts a little.
        val fresh = game()
        repeat(10) {
            while (fresh.needsDecision) fresh.choose(fresh.events.first().choices.lastIndex)
            fresh.advance()
        }
        assertTrue(fresh.founder.xp >= 10.0)
    }

    @Test
    fun skillsAreLearnedInOrder() {
        val s = game().apply { founderXp(founderXpToNext(1) + founderXpToNext(2)) }
        assertEquals(2, s.founder.skillPoints)
        assertNotNull(s.skillBlocker(FounderSkill.DESIGN_EYE))
        assertFailsWith<GameException> { s.learnSkill(FounderSkill.CRAFTSMANSHIP) }
        s.learnSkill(FounderSkill.HANDS_ON)
        assertNull(s.skillBlocker(FounderSkill.DESIGN_EYE))
        s.learnSkill(FounderSkill.DESIGN_EYE)
        assertEquals(0, s.founder.skillPoints)
        assertEquals("You need a skill point: they come with each level.", s.skillBlocker(FounderSkill.MENTOR))
        val back = GameState.decode(s.encode())
        assertEquals(setOf(FounderSkill.HANDS_ON, FounderSkill.DESIGN_EYE), back.founder.skills)
        assertEquals(3, back.founder.level)
    }

    @Test
    fun skillsDoWhatTheySay() {
        val plain = game()
        val skilled = game().apply {
            founderXp(10_000.0)
            for (skill in FounderSkill.entries) learnSkill(skill)
        }
        val a = plain.staffing(Lane.GAMES)
        val b = skilled.staffing(Lane.GAMES)
        assertEquals(a.output * 1.2, b.output, 0.001)
        assertTrue(b.avgDesign > a.avgDesign + 0.5)
        val ca = plain.context(Lane.GAMES)
        val cb = skilled.context(Lane.GAMES)
        assertEquals(ca.mods.quality + 1.0, cb.mods.quality, 0.001)
        assertEquals(ca.mods.demand * 1.04, cb.mods.demand, 0.001)
        assertEquals(1.15, skilled.founderHype, 0.001)
        assertEquals(1.15, skilled.reputation.lift, 0.001)
    }

    @Test
    fun aSecondTraitAtLevelFive() {
        val s = game(FounderTrait.CODER)
        assertFailsWith<GameException> { s.pickSecondTrait(FounderTrait.SHOWRUNNER) }
        repeat(SECOND_TRAIT_LEVEL - 1) { s.founderXp(founderXpToNext(s.founder.level)) }
        assertEquals(SECOND_TRAIT_LEVEL, s.founder.level)
        assertTrue(FounderTrait.CODER !in s.secondTraitChoices)
        assertTrue(FounderTrait.BOOTSTRAPPER !in s.secondTraitChoices)
        s.pickSecondTrait(FounderTrait.MEDIA_DARLING)
        assertEquals(1.3, s.reputation.lift, 0.001)
        assertFailsWith<GameException> { s.pickSecondTrait(FounderTrait.SHOWRUNNER) }
        // Both traits work.
        assertTrue(s.founderIs(FounderTrait.CODER) && s.founderIs(FounderTrait.MEDIA_DARLING))
        val back = GameState.decode(s.encode())
        assertEquals(FounderTrait.MEDIA_DARLING, back.founder.trait2)
    }

    @Test
    fun aWholeCareerGrowsTheFounder() {
        val s = game(seed = 11)
        val pilot = Autopilot()
        while (!s.isOver && s.week < 52 * 8) pilot.playWeek(s)
        assertTrue(s.founder.level >= SECOND_TRAIT_LEVEL, "level ${s.founder.level}")
        assertNotNull(s.founder.trait2)
        assertEquals(0, s.founder.skillPoints)
    }
}
