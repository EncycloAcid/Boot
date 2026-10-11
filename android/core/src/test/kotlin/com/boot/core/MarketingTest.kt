package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MarketingTest {
    @Test
    fun channelsArriveInTheirYear() {
        val past = GameState.create("Co", "Ada", Lane.GAMES, 1, start = StartPoint.CONSOLE_WARS)
        val now = past.channels()
        assertTrue(Channel.TV in now && Channel.PRINT in now)
        assertTrue(Channel.SEARCH !in now && Channel.INFLUENCERS !in now)
        val today = GameState.create("Co", "Ada", Lane.GAMES, 1)
        assertEquals(Channel.entries.toSet(), today.channels().toSet())
        assertNotNull(past.campaignBlocker(Lane.GAMES, Channel.INFLUENCERS, AdBudget.SMALL, 4))
    }

    @Test
    fun aCampaignLiftsAwarenessAndThenFades() {
        val s = GameState.create("Co", "Ada", Lane.WEBSITES, 2)
        s.cash = 2e6
        val before = s.awareness(Lane.WEBSITES)
        assertEquals(1.0, s.brandLift(Lane.WEBSITES), 1e-9)
        val forecast = s.campaignForecast(Lane.WEBSITES, Channel.SEARCH, AdBudget.MEDIUM, 8)
        s.startCampaign(Lane.WEBSITES, Channel.SEARCH, AdBudget.MEDIUM, 8)
        assertNotNull(s.campaignBlocker(Lane.WEBSITES, Channel.TV, AdBudget.SMALL, 4))
        repeat(8) { s.advance() }
        assertNull(s.campaignFor(Lane.WEBSITES))
        val r = s.marketing.results.single()
        assertEquals(8 * s.campaignCost(Channel.SEARCH, AdBudget.MEDIUM), r.spent, 1.0)
        assertTrue(s.awareness(Lane.WEBSITES) > before + 0.1)
        assertEquals(forecast, s.awareness(Lane.WEBSITES), 0.05)
        val lift = s.brandLift(Lane.WEBSITES)
        assertTrue(lift > 1.05)
        repeat(26) { if (s.events.isEmpty() && s.pendingPivot == null) s.advance() }
        assertTrue(s.brandLift(Lane.WEBSITES) < lift)
    }

    @Test
    fun noCampaignNoLift() {
        val s = GameState.create("Co", "Ada", Lane.APPS, 3)
        repeat(30) { if (s.events.isEmpty()) s.advance() }
        for (l in s.lanes) assertEquals(1.0, s.brandLift(l), 1e-9)
    }

    @Test
    fun fitsAndBudgets() {
        assertTrue(Channel.TV.fit(Lane.HARDWARE) > Channel.TV.fit(Lane.WEBSITES))
        assertTrue(Channel.EVENTS.fit(Lane.AI) > Channel.EVENTS.fit(Lane.SOCIAL))
        assertEquals("Great fit", Channel.INFLUENCERS.fitLabel(Lane.GAMES))
        assertTrue(AdBudget.LARGE.reach < AdBudget.LARGE.scale)
        val s = GameState.create("Co", "Ada", Lane.HARDWARE, 4)
        assertTrue(
            s.campaignReach(Lane.HARDWARE, Channel.TV, AdBudget.LARGE) > s.campaignReach(Lane.HARDWARE, Channel.TV, AdBudget.SMALL),
        )
        s.cash = 1_000.0
        assertNotNull(s.campaignBlocker(Lane.HARDWARE, Channel.TV, AdBudget.LARGE, 13))
    }

    @Test
    fun marketingSurvivesASave() {
        val s = GameState.create("Co", "Ada", Lane.GAMES, 5)
        s.cash = 1e6
        s.startCampaign(Lane.GAMES, Channel.PRINT, AdBudget.SMALL, 4)
        s.advance()
        val back = GameState.decode(s.encode())
        assertEquals(s.marketing.campaigns, back.marketing.campaigns)
        assertEquals(s.awareness(Lane.GAMES), back.awareness(Lane.GAMES), 1e-9)
    }
}
