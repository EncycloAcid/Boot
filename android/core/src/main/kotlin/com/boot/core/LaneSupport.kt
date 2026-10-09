package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.exp

/** Multipliers applied to a lane this week by eras, combos and the ecosystem bonus. */
data class LaneModifiers(
    var demand: Double = 1.0,
    /** Additive quality points on anything shipped. */
    var quality: Double = 0.0,
    var output: Double = 1.0,
    var cost: Double = 1.0,
    /** Multiplier on churn. Below 1 is better. */
    var churn: Double = 1.0,
    var growth: Double = 1.0,
    var computeCost: Double = 1.0,
    /** Additive weekly toxicity on a social platform. */
    var toxicity: Double = 0.0,
    /** Multiplier on bad luck: recalls, regulator attention. */
    var risk: Double = 1.0,
    /** Multiplier on capability gained per training run. */
    var gain: Double = 1.0,
)

/** Everything a lane needs to simulate one week. */
data class LaneContext(
    val week: Int,
    val staffing: Staffing,
    val mods: LaneModifiers,
    val reception: Double,
    val era: Era,
    val competition: Double,
    val crunch: Boolean,
    val lane: Lane,
) {
    /** Era, rivals and modifiers rolled into one demand number. */
    val marketDemand: Double get() = era.demand(lane) * competition * mods.demand

    /** Crunch stretches the team, not the founder, who already gives everything. */
    val workOutput: Double get() = (staffing.output + (if (crunch) 0.4 * staffing.teamOutput else 0.0)) * mods.output

    /** How much of this week's work turns into polish. */
    val polishEfficiency: Double get() = if (crunch) 0.6 else 1.0

    val marketingBoost: Double get() = 1 + 0.03 * staffing.marketing
}

@Serializable
enum class LogTone { NEUTRAL, GOOD, BAD, MILESTONE }

@Serializable
data class LogEntry(val id: Int, val week: Int, val text: String, val tone: LogTone, val lane: Lane? = null)

/** Side effects a lane reports back to the game. */
sealed interface Incident {
    data class Log(val text: String, val tone: LogTone) : Incident
    data class Reputation(val delta: Double) : Incident
    data class Scar(val kind: ScarKind, val severity: Double, val note: String) : Incident
    data class Event(val kind: EventKind) : Incident
    data class Stat(val key: StatKey) : Incident
    data class Research(val points: Double) : Incident
}

enum class StatKey { HIT, FLOP, RECALL, SCANDAL, RELEASE, SOLD_OUT, OVERSTOCK }

class LaneReport {
    var revenue = 0.0
    var expenses = 0.0
    val incidents = mutableListOf<Incident>()

    fun log(text: String, tone: LogTone = LogTone.NEUTRAL) {
        incidents += Incident.Log(text, tone)
    }

    fun add(incident: Incident) {
        incidents += incident
    }
}

internal fun shippedQuality(polish: Double, work: Double, averageSkill: Double, bonus: Double): Double {
    val craft = 45 * (1 - exp(-1.2 * polish / maxOf(work, 1.0)))
    return (35 + craft + (averageSkill - 5) * 3 + bonus).clamped(5.0, 99.0)
}
