package com.boot.core

import kotlinx.serialization.Serializable

/** Where the company works. Each office has a set number of desks; you move to grow. */
@Serializable
enum class Office(val title: String, val desks: Int, val rent: Double, val moveCost: Double) {
    APARTMENT("Cramped apartment", 2, 600.0, 0.0),
    COWORKING("Coworking desks", 6, 2_500.0, 25_000.0),
    STUDIO("Studio floor", 16, 9_000.0, 150_000.0),
    DOWNTOWN("Downtown office", 45, 35_000.0, 900_000.0),
    CAMPUS("Campus", 1_000, 120_000.0, 6_000_000.0);

    val next: Office? get() = entries.getOrNull(ordinal + 1)
}

/** Move to the next office up. */
fun GameState.moveOffice() {
    if (ending != null) throw gameOver()
    val next = office.next ?: throw notAvailable("You're already on a campus.")
    if (cash < next.moveCost) throw insufficientFunds("Moving to a ${next.title.lowercase()} costs ${money(next.moveCost)}.")
    spend(next.moveCost, null)
    office = next
    addLog("You moved into a ${next.title.lowercase()}. ${next.desks} desks, ${money(next.rent)}/week rent.", LogTone.MILESTONE)
}

val GameState.freeDesks: Int get() = office.desks - staff.size

/** Things you can put in the office. Each costs money up front and every week, and earns its keep in people. */
@Serializable
enum class Perk(
    val title: String,
    val blurb: String,
    val cost: Double,
    /** Weekly upkeep. Free lunch is per head instead. */
    private val upkeep: Double,
    val minOffice: Office,
    /** Lifts where morale settles. */
    internal val morale: Double,
) {
    COFFEE("Espresso machine", "Better mornings. Morale up a little.", 2_500.0, 40.0, Office.APARTMENT, 0.02),
    PLANTS("Plants everywhere", "Calmer days. Morale up a little.", 1_500.0, 25.0, Office.APARTMENT, 0.015),
    ARCADE("Arcade cabinets", "Morale up, and game developers learn 20% faster.", 18_000.0, 120.0, Office.COWORKING, 0.03),
    CHEF("Free lunch", "Morale up, and better people apply. Costs \$30 a head a week.", 5_000.0, 0.0, Office.COWORKING, 0.04),
    NAP_PODS("Nap pods", "Crunch hurts morale half as much.", 40_000.0, 250.0, Office.STUDIO, 0.01),
    LIBRARY("Research library", "10% more research points.", 70_000.0, 400.0, Office.STUDIO, 0.01),
    GYM("Gym", "Morale up, and everyone levels up 10% faster.", 120_000.0, 900.0, Office.STUDIO, 0.04),
    ROOFTOP("Rooftop bar", "Morale up, and star candidates come looking for you.", 300_000.0, 2_500.0, Office.DOWNTOWN, 0.05),
    DAYCARE("Daycare", "Morale up, and half as many people quit.", 500_000.0, 4_000.0, Office.CAMPUS, 0.05);

    fun weeklyCost(headcount: Int): Double = if (this == CHEF) 30.0 * (headcount + 1) else upkeep
}

/** How many perks fit. */
val Office.perkSlots: Int
    get() = when (this) {
        Office.APARTMENT -> 1
        Office.COWORKING -> 3
        Office.STUDIO -> 5
        Office.DOWNTOWN -> 7
        Office.CAMPUS -> 9
    }

val GameState.perkUpkeep: Double get() = perks.sumOf { it.weeklyCost(staff.size) }

fun GameState.perkBlocker(perk: Perk): String? {
    if (perk in perks) return "You already have it."
    if (office < perk.minOffice) return "Needs a ${perk.minOffice.title.lowercase()} or bigger."
    if (perks.size >= office.perkSlots) return "No room. A ${office.title.lowercase()} fits ${office.perkSlots}."
    if (cash < perk.cost) return "Costs ${money(perk.cost)}."
    return null
}

fun GameState.buyPerk(perk: Perk) {
    requireLive()
    perkBlocker(perk)?.let { throw notAvailable(it) }
    spend(perk.cost, null)
    perks = perks + perk
    addLog("New in the office: ${perk.title.lowercase()}. ${perk.blurb}", LogTone.GOOD)
}

fun GameState.removePerk(perk: Perk) {
    requireLive()
    if (perk !in perks) throw notAvailable("You don't have that.")
    perks = perks - perk
    for (e in staff) e.morale = maxOf(0.0, e.morale - 0.05)
    addLog("The ${perk.title.lowercase()} is gone. People noticed.", LogTone.BAD)
}

internal fun GameState.has(perk: Perk): Boolean = perk in perks
