package com.boot.core

import kotlinx.serialization.Serializable

/** How hard the world is on you. Chosen once, at the start of a run. */
@Serializable
enum class Difficulty(
    val title: String,
    val blurb: String,
    val startingCash: Double,
    /** Multiplier on demand and sales in every lane. */
    val sales: Double,
    /** Multiplier on running costs. */
    val costs: Double,
    /** Added to every rival's starting strength. */
    val rivalStrength: Double,
    /** Weeks in the red before the bank closes you down. */
    val redWeeks: Int,
    /** Annual interest on bank loans. */
    val loanRate: Double,
) {
    EASY("Easy", "More savings, kinder markets, a patient bank.", 60_000.0, 1.2, 0.9, -0.1, 12, 0.06),
    NORMAL("Normal", "The game as designed.", 25_000.0, 1.0, 1.0, 0.0, 8, 0.10),
    HARD("Hard", "Thin savings, tough rivals, pricier everything.", 12_000.0, 0.85, 1.12, 0.12, 6, 0.16),
}
