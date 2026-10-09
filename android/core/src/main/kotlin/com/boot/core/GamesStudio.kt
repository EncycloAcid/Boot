package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

@Serializable
enum class GameScope {
    SMALL, MEDIUM, LARGE;

    val title: String
        get() = when (this) {
            SMALL -> "Small indie"
            MEDIUM -> "Mid-size"
            LARGE -> "Blockbuster"
        }

    /** Work points to finish. */
    val work: Double
        get() = when (this) {
            SMALL -> 60.0
            MEDIUM -> 600.0
            LARGE -> 4_000.0
        }

    /** Art, licences and marketing while in development. */
    val weeklyBudget: Double
        get() = when (this) {
            SMALL -> 150.0
            MEDIUM -> 3_000.0
            LARGE -> 25_000.0
        }

    internal val launchSales: Double
        get() = when (this) {
            SMALL -> 25_000.0
            MEDIUM -> 150_000.0
            LARGE -> 1_100_000.0
        }

    internal val price: Double
        get() = when (this) {
            SMALL -> 15.0
            MEDIUM -> 25.0
            LARGE -> 60.0
        }

    internal val fanScale: Double
        get() = when (this) {
            SMALL -> 5_000.0
            MEDIUM -> 40_000.0
            LARGE -> 250_000.0
        }
}

@Serializable
data class GameProject(
    val title: String,
    val scope: GameScope,
    var progress: Double = 0.0,
    var polish: Double = 0.0,
    var weeksInDev: Int = 0,
) {
    val isReady: Boolean get() = progress >= scope.work
    val completion: Double get() = min(1.0, progress / scope.work)
}

@Serializable
data class GameRelease(
    val id: Int,
    val title: String,
    val scope: GameScope,
    val week: Int,
    val review: Int,
    var weeklySales: Double,
    var lifetimeSales: Double,
) {
    val verdict: String
        get() = when {
            review >= 90 -> "Masterpiece"
            review >= 85 -> "Hit"
            review >= 70 -> "Solid"
            review >= 55 -> "Mixed"
            else -> "Flop"
        }
}

@Serializable
class GamesStudio {
    var project: GameProject? = null
    val releases = mutableListOf<GameRelease>()
    var fans = 0.0

    /** 0.3...1.2. Flops knock it down and only hits bring it back. The fanbase never forgets. */
    var fanTrust = 1.0
    var hits = 0
    var flops = 0
    private var nextId = 1
    private val usedTitles = mutableListOf<String>()

    val weeklySales: Double get() = releases.sumOf { it.weeklySales }

    internal fun startProject(scope: GameScope, rng: SeededRNG) {
        if (project != null) throw busy("A game is already in development.")
        val fresh = Names.games.filter { it !in usedTitles }
        val title = if (fresh.isEmpty()) "Untitled ${releases.size + 1}" else rng.pick(fresh)
        usedTitles += title
        project = GameProject(title, scope)
    }

    internal fun cancelProject() {
        project = null
    }

    /** Launches the current project. Returns what happened for the log. */
    internal fun launch(ctx: LaneContext, rng: SeededRNG): LaneReport {
        val p = project ?: throw invalid("No game in development.")
        if (!p.isReady) throw invalid("${p.title} isn't finished yet.")
        val report = LaneReport()
        val quality = shippedQuality(p.polish, p.scope.work, ctx.staffing.averageSkill, ctx.mods.quality)
        val review = rng.normal(quality, 9.0).clamped(5.0, 98.0).roundToInt()
        val fanLift = 1 + 1.5 * fans / (fans + p.scope.fanScale)
        val firstWeek = p.scope.launchSales * (review / 70.0).pow(3) * fanLift * fanTrust *
            ctx.reception * ctx.marketDemand * ctx.marketingBoost

        releases.add(0, GameRelease(nextId, p.title, p.scope, ctx.week, review, firstWeek, 0.0))
        nextId += 1
        project = null
        report.add(Incident.Stat(StatKey.RELEASE))

        val buyers = firstWeek / p.scope.price
        when {
            review >= 85 -> {
                hits += 1
                fanTrust = min(1.2, fanTrust + 0.05)
                fans += buyers * 0.35
                report.add(Incident.Stat(StatKey.HIT))
                report.add(Incident.Reputation(4.0))
                report.log("${p.title} launches to a $review review score. Launch week: ${money(firstWeek)}. It's a hit.", LogTone.MILESTONE)
            }
            review >= 55 -> {
                fans += buyers * 0.15
                report.log("${p.title} launches with a $review. Launch week: ${money(firstWeek)}.", LogTone.GOOD)
            }
            else -> {
                flops += 1
                fanTrust = max(0.3, fanTrust - 0.2)
                fans *= 0.85
                report.add(Incident.Stat(StatKey.FLOP))
                val severity = if (p.scope == GameScope.SMALL) 0.15 else 0.35
                report.add(Incident.Scar(ScarKind.FLOP, severity, "${p.title} flopped with a $review."))
                report.log("${p.title} flops with a $review. The fans will remember this one.", LogTone.BAD)
            }
        }
        return report
    }

    internal fun tick(ctx: LaneContext, rng: SeededRNG): LaneReport {
        val report = LaneReport()
        project?.let { p ->
            val work = ctx.workOutput
            val wasReady = p.isReady
            p.progress += work
            p.polish += work * ctx.polishEfficiency
            p.weeksInDev += 1
            report.expenses += p.scope.weeklyBudget * ctx.mods.cost
            if (p.isReady && !wasReady) {
                report.log("${p.title} is content complete. Launch it now, or keep polishing.", LogTone.GOOD)
            }
        }
        for (r in releases) {
            val age = ctx.week - r.week
            report.revenue += r.weeklySales
            r.lifetimeSales += r.weeklySales
            r.weeklySales *= if (age < 6) 0.75 else 0.95
            if (r.weeklySales < 40) r.weeklySales = 0.0
        }
        return report
    }

    /** Back catalogue and fans count toward valuation. */
    internal val assetValue: Double get() = fans * 4 * fanTrust

    val statusLine: String
        get() {
            val p = project
            if (p != null) {
                return "${p.title} (${p.scope.title}) ${(p.completion * 100).toInt()}%" +
                    if (p.isReady) ", ready to launch" else ""
            }
            return if (releases.isEmpty()) "No game in development" else "Between projects"
        }
}
