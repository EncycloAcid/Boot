package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShowsTest {
    private fun studio(seed: Long = 3) = GameState.create("Pixel Forge", "F", Lane.GAMES, seed).apply { cash = 5_000_000.0 }

    private fun GameState.make(platform: Platform = Platform.PLAYSYSTEM_5, size: GameSize = GameSize.MEDIUM) {
        researched = researched + Research.MEDIUM_GAMES
        startGame(GameConcept("Moss Knight", Topic.FANTASY, Genre.RPG, platforms = listOf(platform), size = size))
    }

    private fun GameState.pick(label: String) {
        val e = events.first()
        val i = e.choices.indexOfFirst { it.label == label }
        assertTrue(i >= 0, "no \"$label\" in ${e.choices.map { it.label }}")
        choose(i)
    }

    /** Moves the calendar to a week of the year without simulating the weeks between. */
    private fun GameState.jumpTo(weekOfYear: Int) {
        week = (week / GameClock.WEEKS_PER_YEAR) * GameClock.WEEKS_PER_YEAR + weekOfYear - 1
    }

    @Test
    fun showsAreNamedForTheirTime() {
        assertEquals("the Arkie Awards", yearEndShow(1981))
        assertEquals("the Golden Joystick Awards", yearEndShow(1995))
        assertEquals("the Spike Video Game Awards", yearEndShow(2010))
        assertEquals("The Game Awards", yearEndShow(2026))
        assertEquals("Nintendo Space World" to 34, PlatformShow.NINTENDO.inYear(1995))
        assertNull(PlatformShow.NINTENDO.inYear(2005))
        assertEquals("a Nintendo Direct", PlatformShow.NINTENDO.inYear(2026)!!.first)
        assertEquals("Sony's E3 press conference", PlatformShow.SONY.inYear(2000)!!.first)
        assertEquals("a PlayStation State of Play", PlatformShow.SONY.inYear(2026)!!.first)
        assertNull(PlatformShow.MICROSOFT.inYear(1999))
        assertEquals("the Xbox Games Showcase", PlatformShow.MICROSOFT.inYear(2026)!!.first)
        assertNotNull(PlatformShow.SEGA.inYear(1997))
        assertNull(PlatformShow.SEGA.inYear(2005))
        assertTrue(!AwardShow.BAFTA.heldIn(2000) && AwardShow.BAFTA.heldIn(2026))
    }

    @Test
    fun consoleMakersInviteGamesOnTheirConsoles() {
        // A PC game isn't Sony's business.
        val pc = studio().apply { make(Platform.PC) }
        pc.jumpTo(PlatformShow.SONY.inYear(pc.calendarYear)!!.second)
        pc.showSeasonWeek()
        assertTrue(pc.events.none { it.kind is PlatformInvite })

        // A PlayStation game is, if it looks good enough.
        val ps = studio().apply { make(Platform.PLAYSYSTEM_5) }
        ps.jumpTo(PlatformShow.SONY.inYear(ps.calendarYear)!!.second)
        ps.showSeasonWeek()
        val invited = ps.events.any { it.kind is PlatformInvite }
        assertEquals(ps.appealOf(1) >= SHOWCASE_BAR, invited)
        if (!invited) assertTrue(ps.log.any { "passed on Moss Knight" in it.text })
    }

    @Test
    fun theFirstTrailerIsTheAnnouncement() {
        val s = studio().apply { make() }
        val p = s.games!!.project!!
        s.enqueue(PlatformInvite("Sony", "a PlayStation State of Play", 1))
        val cash = s.cash
        s.pick("Show a trailer")
        assertEquals(cash - s.trailerCost(p), s.cash, 1.0)
        assertEquals(1, p.reveals)
        assertTrue(s.news.any { it.headline == "Pixel Forge announces Moss Knight" })
        val first = p.hype
        // A second trailer adds less, and isn't news in the same way.
        s.enqueue(PlatformInvite("Sony", "a PlayStation State of Play", 1))
        s.pick("Show a trailer")
        assertEquals(first / 1.5, p.hype - first, 0.01)
        assertEquals(1, s.news.count { "announces" in it.headline })
        assertEquals(1, s.stats.announcements)
    }

    @Test
    fun aMarketingDealPaysButOtherFansGrumble() {
        val s = studio().apply { make(size = GameSize.MEDIUM) }
        val p = s.games!!.project!!
        val trust = s.games!!.fanTrust
        val cash = s.cash
        s.enqueue(PlatformInvite("Microsoft", "the Xbox Games Showcase", 1))
        s.pick("Take their marketing deal")
        assertEquals(cash + s.marketingDeal(p), s.cash, 1.0)
        assertEquals("Microsoft", p.exclusiveWith)
        assertTrue(s.games!!.fanTrust < trust)
        assertTrue(p.hype > 0)
        // One deal per game.
        s.enqueue(PlatformInvite("Microsoft", "the Xbox Games Showcase", 1))
        assertTrue(s.events.first().choices.none { it.label == "Take their marketing deal" })
    }

    @Test
    fun aFinishedGameCanBeShadowDroppedOnStage() {
        val s = studio().apply { make() }
        val p = s.games!!.project!!
        p.progress = p.work
        s.enqueue(PremiereSlot("The Game Awards", 250_000.0, 1))
        val released = s.games!!.releases.size
        s.pick("Shadow-drop it")
        assertEquals(released + 1, s.games!!.releases.size)
        assertNull(s.games!!.project)
        assertEquals(1, s.stats.shadowDrops)
        assertTrue(s.news.any { it.headline == "Moss Knight is out now" })
    }

    @Test
    fun worldPremieresCostASlotAndBuildHype() {
        val s = studio().apply { make() }
        val p = s.games!!.project!!
        val cash = s.cash
        s.enqueue(PremiereSlot("The Game Awards", 250_000.0, 1))
        // Not finished, so no shadow drop on offer.
        assertTrue(s.events.first().choices.none { it.label == "Shadow-drop it" })
        s.pick("World premiere")
        assertEquals(cash - 250_000.0 - s.trailerCost(p), s.cash, 1.0)
        assertTrue(p.hype > 0)
        assertEquals(1, s.stats.premieres)
        assertNotNull(s.achievements[Achievement.WORLD_PREMIERE] ?: run { s.advance(); s.achievements[Achievement.WORLD_PREMIERE] })
    }

    @Test
    fun awardJuriesHaveTheirOwnTastes() {
        val s = studio()
        val m = s.games!!.market
        // A small, artful game against a big, slick one with the same score.
        m.games += RivalGame(m.nextId++, "Little Lantern", "Tiny Studio", Topic.FANTASY, Genre.ADVENTURE, GameSize.SMALL, 8.6, s.week, 20_000.0, lifetime = 300_000.0)
        m.games += RivalGame(m.nextId++, "Iron Armada", "Big Studio", Topic.MILITARY, Genre.ACTION, GameSize.AAA, 8.6, s.week, 4_000_000.0, lifetime = 90_000_000.0)
        val field = s.contenders()
        val small = field.first { it.title == "Little Lantern" }
        val big = field.first { it.title == "Iron Armada" }
        assertTrue(AwardShow.BAFTA.rate(small) > AwardShow.BAFTA.rate(big))
        assertTrue(AwardShow.GDC.rate(big) > AwardShow.GDC.rate(small))
        assertTrue(AwardShow.GOLDEN_JOYSTICKS.rate(big) > AwardShow.GOLDEN_JOYSTICKS.rate(small))
        s.holdAwardShow(AwardShow.BAFTA)
        val award = m.awards.last()
        assertEquals(AwardShow.BAFTA.prizes.last().title, award.prize)
        assertEquals("the BAFTA Games Awards", award.show)
        assertTrue(award.title in setOf("Little Lantern", "Iron Armada"))
        // Only Game of the Year counts as a Game of the Year win.
        m.awards += Award(1, "Moss Knight", "Pixel Forge", 9.0, mine = true, prize = "Best Technology")
        assertEquals(0, m.wins)
    }

    @Test
    fun theBiggestStudiosGetKeynotesAndShowcases() {
        val s = studio().apply { make() }
        assertTrue(Booth.KEYNOTE !in s.boothsOpen())
        s.games!!.fans = KEYNOTE_FANS
        assertTrue(Booth.KEYNOTE in s.boothsOpen())
        assertEquals(1_200_000.0, s.boothCost(Booth.KEYNOTE))
        s.jumpTo(OWN_SHOWCASE_WEEK)
        s.showSeasonWeek()
        assertTrue(s.events.any { it.kind == OwnShowcase })
        val hype = s.games!!.project!!.hype
        while (s.events.first().kind != OwnShowcase) s.choose(s.events.first().choices.lastIndex)
        s.pick("Go live")
        assertEquals(1, s.stats.ownShowcases)
        assertTrue(s.games!!.project!!.hype > hype)
    }

    @Test
    fun todayKnowsWhatsComingUp() {
        val s = studio().apply { make(Platform.PLAYSYSTEM_5) }
        s.jumpTo(SHOW_WEEK - 5)
        val ahead = s.showsAhead(8).map { it.name }
        assertTrue("Summer Game Fest" in ahead, "$ahead")
        // Sony's showcase is on the list because the game is on PlayStation; Nintendo's isn't.
        val sony = PlatformShow.SONY.inYear(s.calendarYear)!!
        if (sony.second in (SHOW_WEEK - 5)..(SHOW_WEEK + 3)) assertTrue(ahead.any { "State of Play" in it }, "$ahead")
        assertTrue(ahead.none { "Nintendo" in it })
    }

    @Test
    fun aWholeCareerOfShows() {
        var awards = 0
        var announced = 0
        var showsLogged = 0
        for (seed in 1L..5L) {
            val s = GameState.create("T", "F", Lane.GAMES, seed)
            val pilot = Autopilot()
            while (!s.isOver && s.week < 52 * 15) pilot.playWeek(s)
            awards += s.games!!.market.awards.count { it.prize.isNotEmpty() }
            announced += s.stats.announcements
            showsLogged += s.stats.premieres + s.stats.tradeShows + s.stats.platformDeals
            val mine = s.games!!.releases.map { it.score }.sortedDescending().take(5)
            val rivals = s.games!!.market.games.map { it.score }.sortedDescending().take(5)
            println("seed $seed: mine $mine rivals $rivals nominations ${s.log.count { "nominated" in it.text }}")
            println("seed $seed: honours ${s.stats.honours}, GOTY ${s.stats.awards}, announced ${s.stats.announcements}, premieres ${s.stats.premieres}, deals ${s.stats.platformDeals}, drops ${s.stats.shadowDrops}, keynotes ${s.stats.keynotes}, own ${s.stats.ownShowcases}, trade ${s.stats.tradeShows}, fans ${s.games!!.fans.toInt()}, cash ${s.cash.toInt()}")
        }
        assertTrue(awards >= 20, "award shows hand out prizes every year: $awards")
        assertTrue(announced >= 5, "games get announced: $announced")
        assertTrue(showsLogged >= 10, "the autopilot uses the stages: $showsLogged")
    }
}
