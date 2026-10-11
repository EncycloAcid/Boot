package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

@Serializable
enum class DeviceCategory {
    PHONE, LAPTOP, HEADPHONES;

    val title: String
        get() = when (this) {
            PHONE -> "Phone"
            LAPTOP -> "Laptop"
            HEADPHONES -> "Headphones"
        }

    internal val designWork: Double
        get() = when (this) {
            PHONE -> 260.0
            LAPTOP -> 300.0
            HEADPHONES -> 110.0
        }

    /** Units a week at launch for a well-reviewed flagship. */
    internal val baseDemand: Double
        get() = when (this) {
            PHONE -> 4_000.0
            LAPTOP -> 1_600.0
            HEADPHONES -> 6_000.0
        }
}

@Serializable
enum class DeviceTier {
    BUDGET, FLAGSHIP;

    val title: String get() = if (this == BUDGET) "Budget" else "Flagship"
    internal val demandMultiplier: Double get() = if (this == BUDGET) 1.6 else 1.0
    val weeklyRnD: Double get() = if (this == BUDGET) 25_000.0 else 60_000.0
}

@Serializable
data class DeviceSpec(val category: DeviceCategory, val tier: DeviceTier) {
    val price: Double
        get() = when (category) {
            DeviceCategory.PHONE -> if (tier == DeviceTier.BUDGET) 349.0 else 999.0
            DeviceCategory.LAPTOP -> if (tier == DeviceTier.BUDGET) 699.0 else 1_999.0
            DeviceCategory.HEADPHONES -> if (tier == DeviceTier.BUDGET) 79.0 else 299.0
        }

    val unitCost: Double
        get() = when (category) {
            DeviceCategory.PHONE -> if (tier == DeviceTier.BUDGET) 210.0 else 420.0
            DeviceCategory.LAPTOP -> if (tier == DeviceTier.BUDGET) 480.0 else 900.0
            DeviceCategory.HEADPHONES -> if (tier == DeviceTier.BUDGET) 32.0 else 90.0
        }
}

@Serializable
data class DeviceDesign(
    val name: String,
    val spec: DeviceSpec,
    var progress: Double = 0.0,
    var polish: Double = 0.0,
    /** Units booked at the factory for this model before launch. */
    var preorderedStock: Int = 0,
) {
    val work: Double get() = spec.category.designWork
    val isReady: Boolean get() = progress >= work
    val completion: Double get() = min(1.0, progress / work)
}

@Serializable
data class Device(
    val name: String,
    val spec: DeviceSpec,
    val quality: Double,
    val launchWeek: Int,
    var hype: Double,
    var unitsSold: Int = 0,
    var soldOutWeeks: Int = 0,
)

@Serializable
data class ProductionOrder(val id: Int, val model: String, val units: Int, var arrivalWeek: Int)

@Serializable
class HardwareLab {
    var design: DeviceDesign? = null
    var onMarket: Device? = null
    var inventory = 0
    val orders = mutableListOf<ProductionOrder>()
    var modelsShipped = 0
    var lifetimeUnits = 0
    var lastDemand = 0.0
    var productionHaltedUntil = 0

    /** -0.2...0.2: discount or markup on the model on sale. */
    var priceShift = 0.0

    /** The launch event booked for the next device: 0 none, 1 a livestream, 2 a big keynote. */
    var keynote = 0
    private var nextId = 1
    private val usedNames = mutableListOf<String>()

    /** 0.2...1. Nobody has heard of your first device. Every unit sold makes the next launch easier. */
    val brandAwareness: Double get() = 0.2 + 0.8 * (1 - exp(-lifetimeUnits / 400_000.0))

    companion object {
        /** Factory lead time. You book capacity months ahead. */
        const val LEAD_TIME_WEEKS = 12
    }

    internal fun startDesign(spec: DeviceSpec, rng: SeededRNG) {
        if (design != null) throw busy("A device is already in design.")
        val fresh = Names.devices.filter { it !in usedNames }
        val base = if (fresh.isEmpty()) rng.pick(Names.devices) else rng.pick(fresh)
        usedNames += base
        val suffix = if (spec.tier == DeviceTier.FLAGSHIP) " Pro" else ""
        design = DeviceDesign("$base ${spec.category.title}$suffix", spec)
    }

    /** What an order would make and cost, without booking it. */
    internal fun quote(units: Int): Pair<String, Double> {
        if (units <= 0) throw invalid("Order at least one unit.")
        val d = design
        val m = onMarket
        val (model, spec) = when {
            d != null -> d.name to d.spec
            m != null -> m.name to m.spec
            else -> throw invalid("Nothing to manufacture yet.")
        }
        return model to units * spec.unitCost
    }

    /** Book factory capacity. Paid on order, delivered after the lead time. */
    internal fun book(model: String, units: Int, week: Int, leadTime: Int = LEAD_TIME_WEEKS) {
        val arrival = max(week, productionHaltedUntil) + leadTime
        orders += ProductionOrder(nextId, model, units, arrival)
        nextId += 1
    }

    internal fun launch(ctx: LaneContext): LaneReport {
        val d = design
        if (d == null || !d.isReady) throw invalid("No finished design to launch.")
        val report = LaneReport()
        val old = onMarket
        if (old != null && inventory > 0) {
            val clearance = inventory * old.spec.price * 0.3
            report.revenue += clearance
            report.add(Incident.Stat(StatKey.OVERSTOCK))
            report.log("Stuck with $inventory units of the ${old.name}. Cleared at 70% off for ${money(clearance)}.", LogTone.BAD)
        }
        val quality = shippedQuality(d.polish, d.work, ctx.staffing.averageSkill, ctx.mods.quality)
        val event = when (keynote) {
            1 -> 0.6
            2 -> 1.4
            else -> 0.0
        }
        onMarket = Device(d.name, d.spec, quality, ctx.week, hype = 2 + 0.05 * ctx.staffing.marketing + event)
        if (keynote > 0) report.log(if (keynote == 2) "The keynote was a hit. Everyone's talking about the ${d.name}." else "The launch livestream drew a crowd.", LogTone.GOOD)
        keynote = 0
        priceShift = 0.0
        inventory = d.preorderedStock
        design = null
        modelsShipped += 1
        report.add(Incident.Stat(StatKey.RELEASE))
        report.log("The ${d.name} launches at ${money(d.spec.price)}. Reviewers score it ${quality.toInt()}. $inventory units on hand.", LogTone.MILESTONE)
        return report
    }

    internal fun tick(ctx: LaneContext, rng: SeededRNG): LaneReport {
        val report = LaneReport()

        design?.let { d ->
            val work = ctx.workOutput
            val wasReady = d.isReady
            d.progress += work
            d.polish += work * ctx.polishEfficiency
            report.expenses += d.spec.tier.weeklyRnD * ctx.mods.cost
            if (d.isReady && !wasReady) {
                report.log("The ${d.name} design is locked. Make sure stock is booked before you launch.", LogTone.GOOD)
            }
        }

        // Deliveries.
        if (ctx.week >= productionHaltedUntil) {
            val arrived = orders.filter { it.arrivalWeek <= ctx.week }
            for (o in arrived) {
                if (o.model == onMarket?.name) {
                    inventory += o.units
                } else if (o.model == design?.name) {
                    design!!.preorderedStock += o.units
                }
            }
            orders.removeAll { it in arrived }
        }

        // Warehousing.
        val m = onMarket ?: return report
        report.expenses += inventory * m.spec.unitCost * 0.002

        val age = (ctx.week - m.launchWeek).toDouble()
        val demand = m.spec.category.baseDemand * m.spec.tier.demandMultiplier * (m.quality / 70).pow(2) * brandAwareness *
            ctx.reception * ctx.marketDemand * m.hype * 0.97.pow(age) * ctx.marketingBoost * (1 + priceShift).pow(-2.2)
        val sold = min(demand.toInt(), inventory)
        inventory -= sold
        m.unitsSold += sold
        lifetimeUnits += sold
        m.hype = 1 + (m.hype - 1) * 0.85
        lastDemand = demand
        report.revenue += sold * m.spec.price * (1 + priceShift)

        if (sold < demand * 0.8 && age < 30) {
            m.soldOutWeeks += 1
            if (m.soldOutWeeks == 1) {
                report.add(Incident.Stat(StatKey.SOLD_OUT))
                report.log("The ${m.name} is sold out. Scalpers are listing it at double.", LogTone.BAD)
                m.hype += 0.1
            }
        }

        // Recalls: the battery issue that costs you a quarter.
        val defectRisk = (1 - m.quality / 100).pow(2) * 0.006 * ctx.mods.risk
        if (m.unitsSold > 1_000 && ctx.week >= productionHaltedUntil && rng.chance(defectRisk)) {
            report.add(Incident.Event(EventKind.Recall(m.name, m.unitsSold, m.spec.price)))
        }
        if (orders.isNotEmpty() && rng.chance(0.012)) {
            report.add(Incident.Event(EventKind.ComponentShortage))
        }
        return report
    }

    internal val assetValue: Double
        get() = inventory * (onMarket?.spec?.unitCost ?: 0.0) * 0.5 + modelsShipped * 250_000.0

    val statusLine: String
        get() {
            val parts = mutableListOf<String>()
            design?.let { parts += "Designing ${it.name} ${(it.completion * 100).toInt()}%" }
            onMarket?.let { parts += "${it.name} on sale, $inventory in stock" }
            val incoming = orders.sumOf { it.units }
            if (incoming > 0) parts += "$incoming units inbound"
            return if (parts.isEmpty()) "No device in design" else parts.joinToString("; ")
        }
}
