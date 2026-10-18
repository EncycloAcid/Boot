package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StoriesTest {
    private fun studio(seed: Long = 3) = GameState.create("Pixel Forge", "F", Lane.GAMES, seed).apply { cash = 5_000_000.0 }

    private fun GameState.person(name: String, role: Role, skill: Int = 6, level: Int = 1, morale: Double = 0.8): Employee {
        val e = Employee(nextId(), name, role, skill, 1_000.0, morale, Lane.GAMES, week, level = level)
        staff = staff + e
        return e
    }

    /** Runs the clock, taking the last choice on anything but a story chapter. */
    private fun GameState.weeks(n: Int) = repeat(n) {
        while (needsDecision && events.first().kind !is StoryBeat) choose(events.first().choices.lastIndex)
        if (!needsDecision) advance()
    }

    private fun GameState.chapter(): PendingEvent = events.first { it.kind is StoryBeat }

    @Test
    fun aDreamTeamWorksBetterTogether() {
        val s = studio()
        val a = s.person("Ada Park", Role.DESIGNER)
        val b = s.person("Ben Cole", Role.ENGINEER)
        val before = s.staffing(Lane.GAMES).output
        s.startStory(StoryKind.DREAM_TEAM, listOf(a.id, b.id))
        val first = s.chapter()
        assertEquals("Ada Park and Ben Cole are on a roll", first.title)
        s.choose(0)
        val story = s.storyOf(a.id)!!
        assertEquals(story, s.storyOf(b.id))
        val paired = s.staffing(Lane.GAMES).output
        assertEquals((a.output(Lane.GAMES) + b.output(Lane.GAMES)) * 0.15, paired - before, 0.001)
        // Months later, they want to lead.
        story.nextWeek = s.week + 1
        s.weeks(1)
        assertEquals("Ada and Ben want their own project", s.chapter().title)
        val pay = a.salary
        s.choose(0)
        assertEquals(pay * 1.1, a.salary, 0.01)
        assertTrue(s.storyQuality(Lane.GAMES) >= 1.0)
        assertTrue(s.staffing(Lane.GAMES).output > paired)
        // Split them up and the magic goes.
        b.lane = Lane.WEBSITES
        assertEquals(1.0, s.storyOutput(a))
    }

    @Test
    fun aStarWantsTheirNameOnTheBox() {
        val s = studio()
        val star = s.person("Mia Stone", Role.DESIGNER, skill = 9)
        s.startStory(StoryKind.NAME_ON_THE_BOX, listOf(star.id))
        assertEquals("Mia Stone wants their name on the box", s.chapter().title)
        s.choose(0)
        assertTrue(s.storyStatus(s.storyOf(star.id)!!).contains("a Mia Stone game"))
        // The next game comes out: a hit.
        s.games!!.releases.add(0, fakeRelease(s, 8.4))
        s.weeks(1)
        val famous = s.chapter()
        assertEquals("Mia Stone is famous now", famous.title)
        s.choose(0)
        assertEquals(1.5, s.storyQuality(Lane.GAMES), 0.001)
        assertTrue(s.news.any { it.headline == "Mia Stone named creative director at Pixel Forge" })
    }

    @Test
    fun keepingAFamousStarWhereTheyAreInvitesOffers() {
        val s = studio()
        val star = s.person("Mia Stone", Role.DESIGNER, skill = 9)
        s.startStory(StoryKind.NAME_ON_THE_BOX, listOf(star.id))
        s.choose(0)
        s.games!!.releases.add(0, fakeRelease(s, 8.0, week = s.week + 1))
        s.weeks(1)
        s.choose(1)
        assertTrue(s.events.any { (it.kind as? EventKind.Poach)?.employeeId == star.id })
        assertNull(s.storyOf(star.id))
    }

    @Test
    fun runningOnEmptyEndsInABreakOrAResignation() {
        val s = studio()
        val e = s.person("Sol Reyes", Role.ENGINEER, morale = 0.55)
        s.startStory(StoryKind.RUNNING_ON_EMPTY, listOf(e.id))
        assertEquals("Sol Reyes keeps staying late", s.chapter().title)
        s.choose(1)
        assertEquals(1.15, s.storyOutput(e))
        s.storyOf(e.id)!!.nextWeek = s.week + 1
        s.weeks(1)
        assertEquals("Sol Reyes is running on empty", s.chapter().title)
        s.choose(0)
        assertEquals(s.week + 4, e.leaveUntil)
        s.weeks(5)
        assertNull(s.storyOf(e.id))
        assertTrue(e.morale >= 0.8)
        assertTrue(s.stories.any { it.ending?.contains("rested, and grateful") == true })

        // Sent home on time instead: a quieter few weeks.
        val t = studio()
        val f = t.person("Kai Lund", Role.ENGINEER, morale = 0.55)
        t.startStory(StoryKind.RUNNING_ON_EMPTY, listOf(f.id))
        t.choose(0)
        assertEquals(0.9, t.storyOutput(f))
        t.weeks(7)
        assertNull(t.storyOf(f.id))
    }

    @Test
    fun aMentorHelpsANewcomerGrowUp() {
        val s = studio()
        val vet = s.person("Ola Berg", Role.ENGINEER, skill = 8, level = 6)
        val kid = s.person("Tim Hart", Role.ENGINEER, skill = 3, level = 1)
        s.startStory(StoryKind.MENTOR, listOf(vet.id, kid.id), mark = kid.level)
        assertEquals("Ola has taken Tim under their wing", s.chapter().title)
        s.choose(0)
        assertEquals(2.0, s.storyXp(kid))
        assertEquals(0.9, s.storyOutput(vet))
        s.weeks(60)
        val story = s.stories.first { it.kind == StoryKind.MENTOR }
        assertNotNull(story.ending)
        assertTrue(kid.level >= 3 || "wrapped up" in story.ending!!, story.ending)
    }

    @Test
    fun storiesSurviveASave() {
        val s = studio()
        val a = s.person("Ada Park", Role.DESIGNER)
        val b = s.person("Ben Cole", Role.ENGINEER)
        s.startStory(StoryKind.DREAM_TEAM, listOf(a.id, b.id))
        s.choose(0)
        val back = GameState.decode(s.encode())
        assertEquals(1, back.activeStories.size)
        assertEquals(1.15, back.storyOutput(back.staff.first { it.id == a.id }))
    }

    @Test
    fun storiesComeUpInAWholeCareer() {
        // A run can end early (the autopilot takes companies public), so look across a few.
        val counts = (5L..7L).map { seed ->
            val s = studio(seed = seed)
            val pilot = Autopilot()
            while (!s.isOver && s.week < 52 * 8) pilot.playWeek(s)
            s.stats.stories
        }
        assertTrue(counts.average() >= 3, "$counts stories")
    }

    private fun fakeRelease(s: GameState, score: Double, week: Int = s.week + 1): GameRelease = GameRelease(
        id = s.nextId(), title = "Moss Knight", concept = GameConcept("Moss Knight", Topic.FANTASY, Genre.RPG, platforms = listOf(Platform.PC), size = GameSize.SMALL),
        week = week, reviews = emptyList(), score = score, weeklySales = 1_000.0, netShare = 0.7, design = 10.0, tech = 10.0, bugs = 0.0, hype = 0.0, devCost = 0.0, report = emptyList(),
    )
}
