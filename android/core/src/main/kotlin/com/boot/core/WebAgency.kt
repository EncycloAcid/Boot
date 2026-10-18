package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.ceil
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

@Serializable
enum class ScopePolicy {
    /** Eat the extra work. Clients love you; margins don't. */
    ABSORB,

    /** Send a change order. Paid, but clients bristle and some walk. */
    BILL_CHANGES;

    val title: String get() = if (this == ABSORB) "Absorb scope creep" else "Bill for changes"
}

@Serializable
data class Contract(
    val id: Int,
    val client: String,
    val value: Double,
    var work: Double,
    var done: Double = 0.0,
    var weeksAllowed: Int,
    var weeksElapsed: Int = 0,
    /** 0...1 */
    var satisfaction: Double = 0.7,
    var changeOrders: Double = 0.0,
    var creepCount: Int = 0,
    val expiresWeek: Int,
) {
    val completion: Double get() = min(1.0, done / max(work, 1.0))
    val isOverdue: Boolean get() = weeksElapsed > weeksAllowed
}

/** A client on a monthly plan: steady money for a steady slice of the team's week. */
@Serializable
data class Retainer(val client: String, val weekly: Double, val load: Double, val since: Int)

/** What the agency charges. Cheap fills the calendar; premium picks the best clients. */
@Serializable
enum class AgencyRate(val title: String, val volume: Double, val value: Double, val blurb: String) {
    BUDGET("Budget", 1.35, 0.7, "More briefs, smaller budgets."),
    STANDARD("Standard", 1.0, 1.0, "The going rate."),
    PREMIUM("Premium", 0.65, 1.45, "Fewer briefs, bigger budgets."),
}

@Serializable
class WebAgency {
    val offers = mutableListOf<Contract>()
    val active = mutableListOf<Contract>()

    /** 0...1 word of mouth. Bigger, better clients come to agencies with standing. */
    var standing = 0.25
    var policy = ScopePolicy.ABSORB
    var rate = AgencyRate.STANDARD
    var completed = 0

    /** Happy clients on monthly plans. */
    val retainers = mutableListOf<Retainer>()

    /** Work points a week the retainers take before any project gets a look in. */
    val retainerLoad: Double get() = retainers.sumOf { it.load }

    /** Clients pull back once they notice you sell competing software (Web + Apps). */
    var clientWariness = 0.0
    private var nextId = 1

    companion object {
        const val RATE_PER_POINT = 400.0
    }

    /** Moves an offer into active work and returns the 40% paid up front. */
    internal fun accept(id: Int): Double {
        val i = offers.indexOfFirst { it.id == id }
        if (i < 0) throw invalid("That offer is no longer on the table.")
        val c = offers.removeAt(i)
        c.weeksElapsed = 0
        active += c
        return c.value * 0.4
    }

    internal fun decline(id: Int) {
        offers.removeAll { it.id == id }
    }

    internal fun tick(ctx: LaneContext, rng: SeededRNG): LaneReport {
        val report = LaneReport()
        offers.removeAll { it.expiresWeek < ctx.week }

        // New briefs.
        val mean = (0.5 + standing * 1.5 + ctx.staffing.marketing * 0.06) * ctx.marketDemand * (1 - clientWariness * 0.6) * rate.volume
        repeat(min(rng.poisson(mean), 3)) {
            if (offers.size >= 5) return@repeat
            val size = (1 + standing * 2) * (1 + 0.4 * log2(1 + ctx.staffing.output / 6))
            val value = min(rng.range(2_500.0, 8_000.0) * size * ctx.mods.demand, 60_000.0) * rate.value
            val work = value / RATE_PER_POINT * rng.range(0.8, 1.2)
            val pace = max(3.0, ctx.staffing.output * 0.8)
            offers += Contract(
                id = nextId, client = rng.pick(if (value >= 25_000) brandsIn(ctx.year) else Names.clients), value = value.roundToLong().toDouble(), work = work,
                weeksAllowed = ceil(work / pace).toInt() + 2, expiresWeek = ctx.week + 2,
            )
            nextId += 1
        }

        report.expenses += (80 + 40.0 * active.size) * ctx.mods.cost

        // Retainers come first: steady money for a steady slice of the week.
        report.revenue += retainers.sumOf { it.weekly } * ctx.mods.demand.coerceAtMost(1.2)
        val free = max(0.0, ctx.workOutput - retainerLoad)
        if (retainers.isNotEmpty() && ctx.workOutput < retainerLoad * 0.8 && rng.chance(0.06)) {
            val lost = retainers.removeAt(0)
            standing = max(0.0, standing - 0.03)
            report.log("${lost.client} ended their retainer. You couldn't keep up with them.", LogTone.BAD)
        }

        // Now and then a big brand wants pitches for a major project.
        if (standing >= 0.35 && rng.chance(0.006)) {
            val value = Math.round(rng.range(120_000.0, 400_000.0) * (0.5 + standing) / 10_000.0) * 10_000.0
            report.add(Incident.Inbox(BigPitch(rng.pick(brandsIn(ctx.year)), value, Math.round(value * 0.06 / 1_000.0) * 1_000.0, (0.15 + 0.5 * standing).coerceAtMost(0.7)), 4))
        }
        if (active.isEmpty()) return report

        // Work is spread evenly; overcommit and everything slips.
        val share = free / active.size
        val finished = mutableListOf<Contract>()
        val walked = mutableListOf<Contract>()
        for (c in active) {
            c.done += share
            c.weeksElapsed += 1
            if (c.isOverdue) c.satisfaction -= 0.05

            // The 11pm email.
            if (rng.chance(0.10)) {
                val extra = c.work * rng.range(0.15, 0.4)
                c.work += extra
                c.creepCount += 1
                when (policy) {
                    ScopePolicy.ABSORB -> {
                        c.satisfaction += 0.02
                        c.weeksAllowed += 1
                    }
                    ScopePolicy.BILL_CHANGES -> {
                        c.changeOrders += extra * RATE_PER_POINT * 0.8
                        c.satisfaction -= 0.12
                        c.weeksAllowed += ceil(extra / max(share, 1.0)).toInt()
                    }
                }
                if (rng.chance(0.3)) report.log("${c.client} changed the brief at 11pm. Again.")
            }
            c.satisfaction = c.satisfaction.clamped(0.0, 1.0)

            if (policy == ScopePolicy.BILL_CHANGES && c.satisfaction < 0.35 && rng.chance(0.08)) {
                report.log("${c.client} walked away mid-project over the change orders.", LogTone.BAD)
                standing = max(0.0, standing - 0.04)
                walked += c
                continue
            }
            if (c.done >= c.work) finished += c
        }

        for (c in finished) {
            val withheld = if (c.satisfaction < 0.3) 0.7 else 1.0
            report.revenue += (c.value * 0.6 + c.changeOrders) * withheld
            standing = (standing + (c.satisfaction - 0.5) * 0.08).clamped(0.0, 1.0)
            completed += 1
            if (c.satisfaction > 0.8 && rng.chance(0.4)) {
                report.log("${c.client} loved the site and is telling everyone.", LogTone.GOOD)
            }
            // Happy clients sometimes want to keep you on.
            if (c.satisfaction > 0.75 && retainers.size < 4 && retainers.none { it.client == c.client } && rng.chance(0.3)) {
                val weekly = Math.round(c.value * 0.05 * rate.value / 100.0) * 100.0
                report.add(Incident.Inbox(RetainerOffer(c.client, weekly, weekly / RATE_PER_POINT * 1.15), 4))
            }
        }
        val gone = (finished + walked).map { it.id }.toSet()
        active.removeAll { it.id in gone }
        return report
    }

    /** A won pitch: a big project straight onto the books. */
    internal fun addBigProject(client: String, value: Double, week: Int, output: Double) {
        val work = value / RATE_PER_POINT * 0.9
        active += Contract(
            id = nextId, client = client, value = value, work = work,
            weeksAllowed = ceil(work / max(3.0, output * 0.8)).toInt() + 4, expiresWeek = week, satisfaction = 0.75,
        )
        nextId += 1
    }

    internal val assetValue: Double get() = standing * 150_000 + completed * 2_000.0 + retainers.sumOf { it.weekly } * 40

    val statusLine: String get() = "${active.size} active, ${offers.size} offers, standing ${(standing * 100).toInt()}"
}
