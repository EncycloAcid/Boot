package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private fun game(lane: Lane, seed: Long) = GameState.create("T", "F", lane, seed)

private fun GameState.play(weeks: Int) {
    repeat(weeks) {
        if (isOver) return
        while (needsDecision) {
            if (pendingPivot != null) return
            choose(events[0].choices.size - 1)
        }
        advance()
    }
    while (needsDecision && pendingPivot == null) choose(events[0].choices.size - 1)
}

class TycoonDepthAppsTest {
    @Test
    fun appsAccumulateDesignAndTechAndHonourUpdateFocus() {
        val s = game(Lane.APPS, 501)
        s.cash = 200_000.0
        s.hireDirect(s.makeCandidate(Role.ENGINEER), Lane.APPS)
        s.hireDirect(s.makeCandidate(Role.DESIGNER), Lane.APPS)
        val a = assertNotNull(s.apps)
        s.setAppCategory(AppCategory.CREATIVE)
        assertEquals(AppCategory.CREATIVE, a.category)
        while (!a.isLive) s.play(1)
        assertTrue(a.design > 0 && a.tech > 0)
        assertTrue(a.quality in 5.0..97.0)
        s.setUpdateFocus(UpdateFocus.STABILITY)
        assertEquals(UpdateFocus.STABILITY, a.updateFocus)
        s.researched = s.researched + Research.APP_SDK
        assertTrue(Research.APP_SDK in a.features || Research.APP_SDK in s.researched)
    }

    @Test
    fun researchingAppPlatformAddsToFeatureStack() {
        val s = game(Lane.APPS, 502)
        s.cash = 100_000.0
        s.researchPoints = 100.0
        s.research(Research.APP_SDK)
        assertTrue(Research.APP_SDK in s.apps!!.features)
    }
}

class TycoonDepthOsTest {
    @Test
    fun osLaneIsCapitalIntensiveAndCanShipWithBeta() {
        assertTrue(Lane.OS.isCapitalIntensive)
        val s = game(Lane.OS, 601)
        s.cash = 5_000_000.0
        s.hireDirect(s.makeCandidate(Role.ENGINEER), Lane.OS)
        s.hireDirect(s.makeCandidate(Role.RESEARCHER), Lane.OS)
        assertNotNull(s.os)
        s.startOsVersion(OsTarget.PC)
        val p = assertNotNull(s.os!!.project)
        while (!p.isReady) s.play(1)
        s.startOsBeta(2)
        while (s.os!!.project!!.betaWeeksLeft > 0) s.play(1)
        s.shipOs()
        assertTrue(s.os!!.releases.isNotEmpty())
        assertTrue(s.os!!.versionsShipped >= 1)
        assertTrue(s.os!!.oemTrust > 0.3)
    }

    @Test
    fun hardwareAndAppsSynergyNotesOnOsShip() {
        val s = game(Lane.OS, 602)
        s.cash = 10_000_000.0
        s.openLane(Lane.HARDWARE)
        s.openLane(Lane.APPS)
        s.hireDirect(s.makeCandidate(Role.ENGINEER), Lane.OS)
        s.hardware!!.onMarket = Device(
            "Bundle Phone", DeviceSpec(DeviceCategory.PHONE, DeviceTier.FLAGSHIP), 80.0, s.week, hype = 1.0,
        )
        s.apps!!.isLive = true
        s.apps!!.users = 50_000.0
        s.startOsVersion(OsTarget.PC)
        val p = s.os!!.project!!
        p.progress = p.work
        p.design = 200.0
        p.tech = 220.0
        p.bugs = 5.0
        s.shipOs()
        val r = s.os!!.releases.first()
        assertTrue(r.score >= 1.0)
        assertTrue(s.os!!.ecosystem > 0.2)
    }

    @Test
    fun rawShipWithHeavyBugsCanRollback() {
        var rolled = false
        repeat(12) { seed ->
            val s = game(Lane.OS, 700L + seed)
            s.cash = 5_000_000.0
            s.startOsVersion(OsTarget.PC)
            val p = s.os!!.project!!
            p.progress = p.work
            p.design = 80.0
            p.tech = 80.0
            p.bugs = p.work * 0.5
            s.shipOs()
            if (s.os!!.releases.first().rolledBack) rolled = true
        }
        assertTrue(rolled, "at least one raw ship with heavy bugs should roll back")
    }

    @Test
    fun oldSavesIgnoreUnknownOsField() {
        val s = game(Lane.GAMES, 1)
        val json = s.encode()
        val round = GameState.decode(json)
        assertEquals(null, round.os)
        assertTrue(Lane.OS !in round.lanes)
    }
}
