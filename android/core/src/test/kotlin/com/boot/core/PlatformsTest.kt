package com.boot.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlatformsTest {
    private fun weekOf(year: Int, month: Int = 6) = Math.round((year + (month - 1) / 12.0 - MODERN_YEAR) * 52).toInt()

    @Test
    fun eachMachineComesAndGoesInItsTime() {
        assertTrue(Platform.DREAMCAST.isAvailable(weekOf(2000)))
        assertFalse(Platform.DREAMCAST.isAvailable(weekOf(1998)))
        assertFalse(Platform.DREAMCAST.isAvailable(weekOf(2005)))
        assertTrue(Platform.GAME_BOY_ADVANCE.isAvailable(weekOf(2004)))
        assertTrue(Platform.NINTENDO_3DS.isAvailable(weekOf(2015)))
        assertTrue(Platform.MASTER_SYSTEM.isAvailable(weekOf(1988)))
        // Today: the Mac, the web, the Steam Machine and the ROG Xbox Ally; the 3DS is long gone.
        for (p in listOf(Platform.MAC, Platform.WEB, Platform.STEAM_MACHINE, Platform.ROG_ALLY)) assertTrue(p.isAvailable(10), "$p")
        assertFalse(Platform.NINTENDO_3DS.isAvailable(0))
        // Every machine has a launch before its retirement.
        for (p in Platform.entries) assertTrue(p.launch < p.retire, "$p")
        assertEquals(Platform.entries.size, Platform.entries.map { it.title }.toSet().size)
    }

    @Test
    fun genresSuitSomeMachinesMoreThanOthers() {
        assertEquals(Fit.GOOD, Platform.NEO_GEO.fit(Genre.FIGHTING))
        assertEquals(Fit.BAD, Platform.NEO_GEO.fit(Genre.SIMULATION))
        assertEquals(Fit.GOOD, Platform.GAME_BOY_ADVANCE.fit(Genre.PLATFORMER))
        assertEquals(Fit.GOOD, Platform.WEB.fit(Genre.PUZZLE))
    }

    @Test
    fun onlyConsoleMakersSignExclusives() {
        val c = GameConcept(
            "Moss Knight", Topic.FANTASY, Genre.RPG,
            platforms = listOf(Platform.PC, Platform.MAC, Platform.WEB, Platform.STEAM_MACHINE, Platform.PLAYSYSTEM_5), size = GameSize.SMALL,
        )
        assertEquals(listOf("Sony"), exclusiveMakers(c))
    }

    @Test
    fun aRunTodayCanShipOnTheNewMachines() {
        val s = GameState.create("Pixel Forge", "F", Lane.GAMES, 3)
        assertTrue(Platform.MAC in s.platformsOnSale)
        assertTrue(Platform.WEB in s.platformsOnSale)
        assertFalse(Platform.DREAMCAST in s.platformsOnSale)
    }
}
