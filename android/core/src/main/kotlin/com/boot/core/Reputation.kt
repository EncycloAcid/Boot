package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

/**
 * A lasting mark on your name. Scars follow you into every lane: ship a predatory mobile
 * game and your social app launches to suspicion.
 */
@Serializable
enum class ScarKind {
    PREDATORY_MONETIZATION, FLOP, PRIVACY, SCANDAL, RECALL, ANTITRUST, LAYOFFS;

    val title: String
        get() = when (this) {
            PREDATORY_MONETIZATION -> "Predatory monetization"
            FLOP -> "Shipped a flop"
            PRIVACY -> "Privacy breach"
            SCANDAL -> "Press scandal"
            RECALL -> "Product recall"
            ANTITRUST -> "Antitrust case"
            LAYOFFS -> "Mass layoffs"
        }

    /** Weekly decay of severity. Players never quite forget some things. */
    internal val decay: Double
        get() = when (this) {
            PREDATORY_MONETIZATION -> 0.002
            FLOP -> 0.006
            PRIVACY -> 0.004
            SCANDAL -> 0.008
            RECALL -> 0.005
            ANTITRUST -> 0.003
            LAYOFFS -> 0.006
        }

    /** How strongly this scar makes a lane's audience suspicious, 0...1. */
    internal fun suspicion(lane: Lane): Double = when (this) {
        PREDATORY_MONETIZATION -> when (lane) {
            Lane.GAMES, Lane.APPS -> 0.7; Lane.SOCIAL -> 0.6; else -> 0.25
        }
        FLOP -> if (lane == Lane.GAMES) 0.6 else 0.1
        PRIVACY -> when (lane) {
            Lane.SOCIAL, Lane.AI, Lane.HARDWARE -> 0.7; else -> 0.3
        }
        SCANDAL -> when (lane) {
            Lane.SOCIAL, Lane.AI -> 0.5; else -> 0.25
        }
        RECALL -> if (lane == Lane.HARDWARE || lane == Lane.OS) 0.8 else 0.15
        ANTITRUST -> 0.2
        LAYOFFS -> 0.15
    }
}

@Serializable
data class Scar(
    val id: Int,
    val kind: ScarKind,
    /** 0...1 */
    var severity: Double,
    var week: Int,
    var note: String,
)

@Serializable
class Reputation {
    /** 0...100, shared across every lane. */
    var score = 50.0
        private set
    val scars = mutableListOf<Scar>()

    /** How much good news counts, and how little bad news does: 1, or more for a media darling. */
    var lift = 1.0
    private var nextScarId = 1

    /** 0.6...1.4 general demand multiplier. */
    val factor: Double get() = 0.6 + 0.8 * score / 100

    /** Multiplier on a launch in [lane], after old scars are taken into account. */
    fun launchReception(lane: Lane): Double {
        val drag = scars.sumOf { it.severity * it.kind.suspicion(lane) }
        return factor * max(0.35, 1 - drag * 0.6)
    }

    fun worstScar(lane: Lane): Scar? = scars.maxByOrNull { it.severity * it.kind.suspicion(lane) }

    fun has(kind: ScarKind, atLeast: Double = 0.2): Boolean = scars.any { it.kind == kind && it.severity >= atLeast }

    internal fun adjust(delta: Double) {
        score = (score + if (delta > 0) delta * lift else delta / lift).clamped(0.0, 100.0)
    }

    internal fun scar(kind: ScarKind, severity: Double, week: Int, note: String) {
        val existing = scars.firstOrNull { it.kind == kind }
        if (existing != null) {
            existing.severity = min(1.0, existing.severity + severity)
            existing.week = week
            existing.note = note
        } else {
            scars += Scar(nextScarId, kind, min(1.0, severity), week, note)
            nextScarId += 1
        }
        adjust(-severity * 18)
    }

    internal fun weeklyDrift() {
        // Reputation slowly regresses toward 50; scars heal at their own pace.
        score += (50 - score) * 0.004
        for (s in scars) s.severity -= s.kind.decay
        scars.removeAll { it.severity <= 0.02 }
    }
}
