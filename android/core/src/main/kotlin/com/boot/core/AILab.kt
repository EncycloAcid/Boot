package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

@Serializable
enum class TrainingSize {
    SMALL, MEDIUM, FRONTIER;

    val title: String
        get() = when (this) {
            SMALL -> "Small run"
            MEDIUM -> "Medium run"
            FRONTIER -> "Frontier run"
        }

    val weeks: Int
        get() = when (this) {
            SMALL -> 8
            MEDIUM -> 16
            FRONTIER -> 30
        }

    /** GPU cluster units rented per week. */
    val computeUnits: Int
        get() = when (this) {
            SMALL -> 1
            MEDIUM -> 4
            FRONTIER -> 16
        }

    /** A run this size can't push a model much past this capability. Progress means bigger runs. */
    val ceiling: Double
        get() = when (this) {
            SMALL -> 30.0
            MEDIUM -> 65.0
            FRONTIER -> 140.0
        }

    internal val expectedGain: Double
        get() = when (this) {
            SMALL -> 4.0
            MEDIUM -> 9.0
            FRONTIER -> 18.0
        }
}

@Serializable
enum class AIProduct {
    CHATBOT, API, ENTERPRISE;

    val title: String
        get() = when (this) {
            CHATBOT -> "Consumer chatbot"
            API -> "Developer API"
            ENTERPRISE -> "Enterprise tools"
        }
}

@Serializable
data class TrainingRun(val codename: String, val size: TrainingSize, var weeksDone: Int = 0) {
    val completion: Double get() = weeksDone.toDouble() / size.weeks
}

@Serializable
class AILab {
    var training: TrainingRun? = null

    /** Capability of the best shipped model. 0 until the first run lands. */
    var capability = 0.0
    var modelName: String? = null
    var generation = 0
    var focus = AIProduct.CHATBOT
    var subscribers = 0.0

    /** Thousands of API calls a week. */
    var apiVolume = 0.0
    var enterpriseClients = 0
    var gpusOwned = 0

    /** 0...1 regulator attention. */
    var regulatoryHeat = 0.0
    var failedRuns = 0

    companion object {
        const val RENT_PER_UNIT = 15_000.0
        const val GPU_UNIT_PRICE = 400_000.0
    }

    internal fun weeklyComputeCost(units: Int, ctx: LaneContext): Double {
        val rented = max(0, units - gpusOwned)
        val owned = min(units, gpusOwned)
        return (rented * RENT_PER_UNIT * ctx.gpuPrice + owned * 1_000.0) * ctx.mods.computeCost
    }

    internal fun startTraining(size: TrainingSize, rng: SeededRNG) {
        if (training != null) throw busy("A training run is already going.")
        training = TrainingRun("${rng.pick(Names.models)}-${generation + 1}", size)
    }

    internal fun tick(ctx: LaneContext, rng: SeededRNG): LaneReport {
        val report = LaneReport()

        training?.let { run ->
            run.weeksDone += 1
            report.expenses += weeklyComputeCost(run.size.computeUnits, ctx)
            if (run.weeksDone >= run.size.weeks) {
                training = null
                finish(run, ctx, rng, report)
            }
        }

        // Owned GPUs draw power whether or not they're training.
        if (training == null) report.expenses += gpusOwned * 1_000.0

        if (capability <= 0) return report
        val reach = ctx.reception * ctx.marketDemand * ctx.marketingBoost
        val c = capability / 60

        // Every product grows toward its potential; the focused one gets the push.
        fun push(p: AIProduct) = if (p == focus) 1.0 else 0.25
        val subTarget = c.pow(2.5) * 120_000 * reach * push(AIProduct.CHATBOT)
        subscribers += (subTarget - subscribers) * 0.08
        val apiTarget = c.pow(2.7) * 12_000 * reach * push(AIProduct.API)
        apiVolume += (apiTarget - apiVolume) * 0.10
        val entTarget = c.pow(2) * (2 + ctx.staffing.marketing * 0.5) * ctx.marketDemand * push(AIProduct.ENTERPRISE)
        if (enterpriseClients < floor(entTarget) && rng.chance(0.15)) {
            enterpriseClients += 1
            report.log("Signed a new enterprise client.", LogTone.GOOD)
        } else if (enterpriseClients > entTarget + 1 && rng.chance(0.1)) {
            enterpriseClients -= 1
        }

        report.revenue += subscribers * 4.6 + apiVolume * 2.5 + enterpriseClients * 40_000.0
        report.expenses += (subscribers * 2.0 + apiVolume * 1.1) * ctx.mods.computeCost + enterpriseClients * 6_000.0

        val scale = subscribers / 1_000_000 + apiVolume / 100_000 + enterpriseClients / 40.0
        regulatoryHeat = (regulatoryHeat + scale * 0.01 * ctx.mods.risk - 0.004).clamped(0.0, 1.0)
        if (regulatoryHeat > 0.5 && rng.chance(0.02 * regulatoryHeat)) {
            report.add(Incident.Event(EventKind.RegulatorInquiry))
        }
        return report
    }

    private fun finish(run: TrainingRun, ctx: LaneContext, rng: SeededRNG, report: LaneReport) {
        val researchFactor = (0.5 + ctx.staffing.research * 0.025).clamped(0.5, 2.0)
        val headroom = max(0.03, 1 - capability / run.size.ceiling)
        val roll = rng.normal(1.0, 0.55)
        val gain = run.size.expectedGain * researchFactor * headroom * roll * ctx.mods.gain
        if (roll < 0.15 || gain < 0.3) {
            failedRuns += 1
            report.log("${run.codename} finished training and it's... worse. The run was a write-off.", LogTone.BAD)
            return
        }
        capability += gain
        generation += 1
        modelName = run.codename
        report.add(Incident.Stat(StatKey.RELEASE))
        val verdict = when {
            roll > 1.3 -> "It's far better than anyone expected."
            roll < 0.6 -> "It's underwhelming."
            else -> "Solid gains."
        }
        report.log("${run.codename} is done. Capability now ${capability.toInt()}. $verdict", if (roll < 0.6) LogTone.NEUTRAL else LogTone.MILESTONE)
    }

    internal val assetValue: Double get() = capability.pow(2) * 8_000 + gpusOwned * GPU_UNIT_PRICE * 0.6

    val statusLine: String
        get() {
            val parts = mutableListOf<String>()
            training?.let { parts += "Training ${it.codename} ${(it.completion * 100).toInt()}%" }
            modelName?.let { parts += "$it live, capability ${capability.toInt()}" }
            return if (parts.isEmpty()) "No model yet" else parts.joinToString("; ")
        }
}
