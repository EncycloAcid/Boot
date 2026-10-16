package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShowNightTest {
    private fun studio(seed: Long = 3) = GameState.create("Pixel Forge", "Sam", Lane.GAMES, seed).apply { cash = 5_000_000.0 }

    private fun GameState.make() {
        startGame(GameConcept("Moss Knight", Topic.FANTASY, Genre.RPG, platforms = listOf(Platform.PLAYSYSTEM_5), size = GameSize.SMALL))
    }

    /** A game of ours, out this year, good enough to be up for prizes. */
    private fun GameState.shipped(title: String = "Little Lantern", score: Double = 9.4, size: GameSize = GameSize.SMALL): GameRelease {
        val r = GameRelease(
            id = nextId(), title = title, concept = GameConcept(title, Topic.FANTASY, Genre.ADVENTURE, platforms = listOf(Platform.PC), size = size),
            week = week, reviews = emptyList(), score = score, weeklySales = 50_000.0, netShare = 0.7, design = 10.0, tech = 10.0, bugs = 0.0,
            hype = 0.0, devCost = 0.0, report = emptyList(),
        )
        games!!.releases.add(0, r)
        return r
    }

    private fun GameState.night(): PendingEvent = events.first { it.kind is AwardNight }

    @Test
    fun aNominationMeansAnAwardNight() {
        val s = studio()
        s.shipped()
        s.holdAwardShow(AwardShow.BAFTA)
        val night = (s.night().kind as AwardNight).ceremony
        assertEquals("The BAFTA Games Awards", night.show)
        // Both prizes we're up for, each with its nominees and a winner among them.
        assertTrue(night.categories.isNotEmpty())
        for (c in night.categories) {
            assertTrue(c.nominated)
            assertTrue(c.winner in c.nominees.indices)
            assertEquals(Topic.FANTASY, c.nominees.first { it.mine }.topic)
        }
        // A rival-only show is just news.
        val other = studio()
        other.holdAwardShow(AwardShow.BAFTA)
        assertTrue(other.events.none { it.kind is AwardNight })
    }

    @Test
    fun aWinnerGetsToMakeASpeech() {
        val s = studio().apply { make() }
        repeat(3) { s.hireDirect(s.makeCandidate(Role.DESIGNER), Lane.GAMES) }
        val win = Ceremony("The BAFTA Games Awards", listOf(Category("Best Game", listOf(Nominee("Little Lantern", "Pixel Forge", 9.4, true)), 0)))
        s.enqueue(AwardNight(win))
        assertEquals("Little Lantern wins Best Game!", s.night().title)
        assertEquals(listOf("Thank the team", "Tease what's next", "Thank the fans"), s.night().choices.map { it.label })
        val morale = s.staff.map { it.morale }
        s.choose(0)
        assertTrue(s.staff.zip(morale).all { (e, m) -> e.morale > m || e.morale == 1.0 })
        assertEquals(1, s.stats.ceremonies)
        // Teasing the next game is its announcement if it hasn't had one.
        s.enqueue(AwardNight(win))
        s.choose(1)
        assertEquals(1, s.games!!.project!!.reveals)
        assertTrue(s.games!!.project!!.hype > 0)
        // Losing is a quieter night.
        val loss = Ceremony("The D.I.C.E. Awards", listOf(Category("Best Game", listOf(Nominee("Big Game", "Rival", 9.6, false), Nominee("Little Lantern", "Pixel Forge", 9.0, true)), 0)))
        s.enqueue(AwardNight(loss))
        assertEquals(listOf("Back to work"), s.night().choices.map { it.label })
    }

    @Test
    fun gameOfTheYearIsReadOutAfterYouDecideToGo() {
        val s = studio()
        s.shipped("Little Lantern", 9.6)
        s.holdAwards(s.games!!)
        assertEquals(EventKind.GameAwards, s.events.first().kind)
        s.choose(1)
        val night = (s.night().kind as AwardNight).ceremony
        assertEquals("Game of the Year", night.categories.last().prize)
        assertTrue(s.pendingCategories.isEmpty())
    }

    @Test
    fun everyStageReportsBack() {
        val s = studio().apply { make() }
        s.enqueue(PlatformInvite("Sony", "a PlayStation State of Play", 1))
        s.choose(0)
        val report = (s.events.first { it.kind is ShowResults }.kind as ShowResults).report
        assertNull(report.booth)
        assertEquals("Moss Knight", report.game)
        assertTrue(report.stream)
        assertTrue(report.hype > 0 && report.wishlists > 0)
        assertEquals(2, report.quotes.size)
        assertEquals(4, report.chat.size)
        assertTrue(report.reaction in 0.0..1.0)
        assertTrue(s.events.first { it.kind is ShowResults }.title.endsWith("Moss Knight at a PlayStation State of Play") ||
            s.events.first { it.kind is ShowResults }.title.endsWith("at a PlayStation State of Play"))
    }

    @Test
    fun aStreamerPreviewAndAFanVote() {
        val s = studio().apply { make() }
        val p = s.games!!.project!!
        p.reveals = 1
        p.bugs = 0.0
        p.wishlists = 10_000.0
        s.enqueue(StreamerPreview(1, "Shroud"))
        s.choose(0)
        assertTrue(p.wishlists > 11_500.0)
        assertEquals(1, s.stats.previews)

        // Rallying fans lifts our game in the public vote.
        val v = studio()
        val mine = v.shipped("Little Lantern", 8.0, GameSize.MEDIUM)
        v.enqueue(FanCampaign(AwardShow.GOLDEN_JOYSTICKS.title))
        val cash = v.cash
        v.choose(0)
        assertEquals(cash - v.fanCampaignCost(), v.cash, 1.0)
        assertEquals(AwardShow.GOLDEN_JOYSTICKS.title, v.voteDrive)
        v.holdAwardShow(AwardShow.GOLDEN_JOYSTICKS)
        assertNull(v.voteDrive)
        assertNotNull(mine)
    }

    @Test
    fun presentingAnAward() {
        val s = studio()
        val rep = s.reputation.score
        s.enqueue(PresenterInvite("The Game Awards"))
        s.choose(0)
        assertTrue(s.reputation.score > rep)
        assertEquals(1, s.stats.presented)
    }

    @Test
    fun aWholeCareerOfShowNights() {
        val s = studio(seed = 11)
        val pilot = Autopilot()
        while (!s.isOver && s.week < 52 * 8) pilot.playWeek(s)
        assertTrue(s.stats.ceremonies >= 1, "${s.stats.ceremonies}")
    }
}
