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
