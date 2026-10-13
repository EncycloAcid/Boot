package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

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

/** Chassis / positioning chosen in the Device Designer. */
@Serializable
enum class FormFactor {
    COMPACT, STANDARD, PRO, ULTRAPORTABLE, CREATOR, BUDS, OVER_EAR;

    val title: String
        get() = when (this) {
            COMPACT -> "Compact"
            STANDARD -> "Standard"
            PRO -> "Pro"
            ULTRAPORTABLE -> "Ultraportable"
            CREATOR -> "Creator"
            BUDS -> "Buds"
            OVER_EAR -> "Over-ear"
        }

    companion object {
        fun options(category: DeviceCategory): List<FormFactor> = when (category) {
            DeviceCategory.PHONE -> listOf(COMPACT, STANDARD, PRO)
            DeviceCategory.LAPTOP -> listOf(ULTRAPORTABLE, CREATOR)
            DeviceCategory.HEADPHONES -> listOf(BUDS, OVER_EAR)
        }

        fun defaultFor(category: DeviceCategory): FormFactor = when (category) {
            DeviceCategory.PHONE -> STANDARD
            DeviceCategory.LAPTOP -> ULTRAPORTABLE
            DeviceCategory.HEADPHONES -> BUDS
        }
    }
}

/** Six effort areas the designer allocates across (sum to 1.0 at scoring time). */
@Serializable
enum class DeviceFocus {
    DISPLAY, BATTERY, SPECIALTY, BUILD, THERMALS, INDUSTRIAL;

    fun title(category: DeviceCategory): String = when (this) {
        DISPLAY -> "Display"
        BATTERY -> "Battery"
        SPECIALTY -> when (category) {
            DeviceCategory.PHONE -> "Camera"
            DeviceCategory.LAPTOP -> "Performance"
            DeviceCategory.HEADPHONES -> "Sound"
        }
        BUILD -> "Build & materials"
        THERMALS -> "Thermals / silicon"
        INDUSTRIAL -> "Industrial design"
    }

    companion object {
        val ALL: List<DeviceFocus> = entries
        fun evenFocus(): List<Double> = List(ALL.size) { 1.0 / ALL.size }
    }
}

/** Review / score axes derived from focus (specialty maps by category). */
@Serializable
enum class DeviceAspect {
    DISPLAY, BATTERY, CAMERA, BUILD, PERF, SOUND, THERMALS, INDUSTRIAL;

    val title: String
        get() = when (this) {
            DISPLAY -> "Display"
            BATTERY -> "Battery"
            CAMERA -> "Camera"
            BUILD -> "Build"
            PERF -> "Performance"
            SOUND -> "Sound"
            THERMALS -> "Thermals"
            INDUSTRIAL -> "Design"
        }

    companion object {
        fun forFocus(focus: DeviceFocus, category: DeviceCategory): DeviceAspect = when (focus) {
            DeviceFocus.DISPLAY -> DISPLAY
            DeviceFocus.BATTERY -> BATTERY
            DeviceFocus.SPECIALTY -> when (category) {
                DeviceCategory.PHONE -> CAMERA
                DeviceCategory.LAPTOP -> PERF
                DeviceCategory.HEADPHONES -> SOUND
            }
            DeviceFocus.BUILD -> BUILD
            DeviceFocus.THERMALS -> THERMALS
            DeviceFocus.INDUSTRIAL -> INDUSTRIAL
        }
    }
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
    /** Relative effort across [DeviceFocus] (normalized at scoring). */
    val focus: List<Double> = DeviceFocus.evenFocus(),
    val formFactor: FormFactor = FormFactor.STANDARD,
    var design: Double = 0.0,
    var tech: Double = 0.0,
    /** Bundle your OS if Platform Lab shipped recently. */
    var bundleOs: Boolean = false,
    /** Optional first-party app pack hype when Apps is open. */
    var appPack: Boolean = false,
) {
    val work: Double get() = spec.category.designWork
    val isReady: Boolean get() = progress >= work
    val completion: Double get() = min(1.0, progress / work)

    val normalizedFocus: List<Double>
        get() {
            val raw = if (focus.size == DeviceFocus.ALL.size) focus else DeviceFocus.evenFocus()
            val total = raw.sum().takeIf { it > 0 } ?: 1.0
            return raw.map { it / total }
        }

    /** BOM multiplier vs the tier's base unit cost (component ambition). */
    val bomMultiplier: Double get() = bomMultiplier(normalizedFocus)

    val effectiveUnitCost: Double get() = spec.unitCost * bomMultiplier

    /** Soft launch price band around list (ambition can nudge MSRP slightly). */
    val priceBand: ClosedFloatingPointRange<Double>
        get() {
            val mid = spec.price * (0.96 + 0.08 * (bomMultiplier - 0.85).coerceIn(0.0, 1.0))
            return (mid * 0.92)..(mid * 1.08)
        }
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
    /** Effective BOM / unit cost at launch. */
    val unitCost: Double = 0.0,
    val aspects: Map<DeviceAspect, Double> = emptyMap(),
    val pros: List<String> = emptyList(),
    val cons: List<String> = emptyList(),
    val formFactor: FormFactor = FormFactor.STANDARD,
    val bundleOs: Boolean = false,
    val appPack: Boolean = false,
) {
    val bomCost: Double get() = if (unitCost > 0) unitCost else spec.unitCost
}

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

    internal fun startDesign(
        spec: DeviceSpec,
        rng: SeededRNG,
        formFactor: FormFactor = FormFactor.defaultFor(spec.category),
        focus: List<Double> = DeviceFocus.evenFocus(),
        bundleOs: Boolean = false,
        appPack: Boolean = false,
    ) {
        if (design != null) throw busy("A device is already in design.")
        val fresh = Names.devices.filter { it !in usedNames }
        val base = if (fresh.isEmpty()) rng.pick(Names.devices) else rng.pick(fresh)
        usedNames += base
        val suffix = when {
            formFactor == FormFactor.PRO || formFactor == FormFactor.CREATOR || formFactor == FormFactor.OVER_EAR ->
                if (spec.tier == DeviceTier.FLAGSHIP) " Pro" else " Plus"
            spec.tier == DeviceTier.FLAGSHIP -> " Pro"
            else -> ""
        }
        val ff = if (formFactor in FormFactor.options(spec.category)) formFactor else FormFactor.defaultFor(spec.category)
        val weights = normalizeFocus(focus)
        design = DeviceDesign(
            name = "$base ${spec.category.title}$suffix",
            spec = spec,
            focus = weights,
            formFactor = ff,
            bundleOs = bundleOs,
            appPack = appPack,
        )
    }

    /** What an order would make and cost, without booking it. */
    internal fun quote(units: Int): Pair<String, Double> {
        if (units <= 0) throw invalid("Order at least one unit.")
        val d = design
        val m = onMarket
        val (model, unit) = when {
            d != null -> d.name to d.effectiveUnitCost
            m != null -> m.name to m.bomCost
            else -> throw invalid("Nothing to manufacture yet.")
        }
        return model to units * unit
    }

    /** Book factory capacity. Paid on order, delivered after the lead time. */
    internal fun book(model: String, units: Int, week: Int, leadTime: Int = LEAD_TIME_WEEKS) {
        val arrival = max(week, productionHaltedUntil) + leadTime
        orders += ProductionOrder(nextId, model, units, arrival)
        nextId += 1
    }

    internal fun launch(ctx: LaneContext, researched: Set<Research> = emptySet()): LaneReport {
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
        val scored = scoreDevice(d, ctx, researched)
        val event = when (keynote) {
            1 -> 0.6
            2 -> 1.4
            else -> 0.0
        }
        var hype = 2 + 0.05 * ctx.staffing.marketing + event
        if (d.appPack) hype += 0.35
        if (d.bundleOs) hype += 0.2
        onMarket = Device(
            name = d.name,
            spec = d.spec,
            quality = scored.quality,
            launchWeek = ctx.week,
            hype = hype,
            unitCost = d.effectiveUnitCost,
            aspects = scored.aspects,
            pros = scored.pros,
            cons = scored.cons,
            formFactor = d.formFactor,
            bundleOs = d.bundleOs,
            appPack = d.appPack,
        )
        if (keynote > 0) report.log(if (keynote == 2) "The keynote was a hit. Everyone's talking about the ${d.name}." else "The launch livestream drew a crowd.", LogTone.GOOD)
        if (d.bundleOs) report.log("Ships with your OS. Reviewers notice the integration.", LogTone.GOOD)
        if (d.appPack) report.log("First-party apps launch with the device. A little extra buzz.", LogTone.GOOD)
        for (note in scored.dingNotes) report.log(note, LogTone.BAD)
        keynote = 0
        priceShift = 0.0
        inventory = d.preorderedStock
        design = null
        modelsShipped += 1
        report.add(Incident.Stat(StatKey.RELEASE))
        val margin = d.spec.price - d.effectiveUnitCost
        report.log(
            "The ${d.name} launches at ${money(d.spec.price)}. Reviewers score it ${scored.quality.toInt()}. " +
                "BOM ${money(d.effectiveUnitCost)} (margin ${money(margin)}). $inventory units on hand.",
            LogTone.MILESTONE,
        )
        return report
    }

    internal fun tick(ctx: LaneContext, rng: SeededRNG): LaneReport {
        val report = LaneReport()

        design?.let { d ->
            val work = ctx.workOutput
            val wasReady = d.isReady
            d.progress += work
            d.polish += work * ctx.polishEfficiency
            // Craft axes: industrial + specialty lean design; thermals + build lean tech.
            val w = d.normalizedFocus
            val designShare = (0.35 + 0.4 * w[DeviceFocus.INDUSTRIAL.ordinal] + 0.25 * w[DeviceFocus.SPECIALTY.ordinal]
                + 0.15 * w[DeviceFocus.DISPLAY.ordinal]).coerceIn(0.25, 0.75)
            d.design += work * 1.5 * designShare * (ctx.staffing.avgDesign / 5)
            d.tech += work * 1.5 * (1 - designShare) * (ctx.staffing.avgTech / 5)
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
        val warehousingUnit = m.bomCost
        report.expenses += inventory * warehousingUnit * 0.002

        val age = (ctx.week - m.launchWeek).toDouble()
        val fit = m.aspects.values.takeIf { it.isNotEmpty() }?.average()?.div(70.0)?.coerceIn(0.55, 1.25) ?: (m.quality / 70)
        val demand = m.spec.category.baseDemand * m.spec.tier.demandMultiplier * (m.quality / 70).pow(2) * fit * brandAwareness *
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
        get() = inventory * (onMarket?.bomCost ?: 0.0) * 0.5 + modelsShipped * 250_000.0

    val statusLine: String
        get() {
            val parts = mutableListOf<String>()
            design?.let { parts += "Designing ${it.name} ${(it.completion * 100).toInt()}%" }
            onMarket?.let { parts += "${it.name} on sale, $inventory in stock" }
            val incoming = orders.sumOf { it.units }
            if (incoming > 0) parts += "$incoming units inbound"
            return if (parts.isEmpty()) "No device in design" else parts.joinToString("; ")
        }

    /** Expected review + preorder units for the current design (or a draft). */
    fun forecast(d: DeviceDesign, awareness: Double = brandAwareness): DeviceForecast {
        val expected = 55.0 + d.design * 0.04 + d.tech * 0.04 + d.polish * 0.08
        val score = expected.coerceIn(40.0, 92.0)
        val units = (d.spec.category.baseDemand * d.spec.tier.demandMultiplier * (score / 70).pow(2) * awareness * 14).toInt()
        return DeviceForecast(score.roundToInt(), d.effectiveUnitCost, d.spec.price, units.coerceAtLeast(100))
    }
}

@Serializable
data class DeviceForecast(
    val expectedScore: Int,
    val unitCost: Double,
    val price: Double,
    val preorderUnits: Int,
)

// --- Scoring helpers (pure; tested) ---

internal fun normalizeFocus(focus: List<Double>): List<Double> {
    val raw = if (focus.size == DeviceFocus.ALL.size) focus else DeviceFocus.evenFocus()
    val total = raw.sum().takeIf { it > 0 } ?: 1.0
    return raw.map { (it / total).coerceIn(0.0, 1.0) }
}

/** Ambition vs an even split. Even ≈ 1.0; maxing expensive parts raises BOM. */
fun bomMultiplier(weights: List<Double>): Double {
    val w = normalizeFocus(weights)
    val costWeight = doubleArrayOf(1.35, 1.25, 1.45, 1.15, 1.2, 0.85) // display…industrial
    val ambition = w.indices.sumOf { w[it] * costWeight[it] }
    val even = costWeight.average()
    return (ambition / even).coerceIn(0.72, 1.65)
}

/** Soft cap for an aspect from researched parts (+ base). */
internal fun deviceAspectCap(aspect: DeviceAspect, researched: Set<Research>): Double {
    var cap = 78.0
    when (aspect) {
        DeviceAspect.DISPLAY -> {
            if (Research.DISPLAY_IPS in researched) cap = max(cap, 82.0)
            if (Research.OLED_DISPLAYS in researched) cap = max(cap, 88.0)
            if (Research.DISPLAY_LTPO in researched) cap = max(cap, 94.0)
        }
        DeviceAspect.BATTERY -> {
            if (Research.BATTERY_SAFETY in researched) cap = max(cap, 82.0)
            if (Research.BATTERY_DENSE in researched) cap = max(cap, 88.0)
            if (Research.BATTERY_SI_ANODE in researched) cap = max(cap, 94.0)
            if (Research.FAST_CHARGING in researched) cap = max(cap, cap + 1.0)
        }
        DeviceAspect.CAMERA, DeviceAspect.PERF, DeviceAspect.SOUND -> {
            when (aspect) {
                DeviceAspect.CAMERA -> {
                    if (Research.CAMERA_DUAL in researched) cap = max(cap, 84.0)
                    if (Research.CAMERA_TRIPLE in researched) cap = max(cap, 90.0)
                    if (Research.CAMERA_COMPUTATIONAL in researched) cap = max(cap, 95.0)
                }
                DeviceAspect.PERF, DeviceAspect.SOUND -> {
                    if (Research.SOC_MIDRANGE in researched) cap = max(cap, 84.0)
                    if (Research.NPU_BLOCK in researched) cap = max(cap, 92.0)
                }
                else -> {}
            }
        }
        DeviceAspect.BUILD, DeviceAspect.INDUSTRIAL -> {
            if (Research.MATERIALS_ALUMINUM in researched) cap = max(cap, 84.0)
            if (Research.MATERIALS_TITANIUM in researched) cap = max(cap, 92.0)
            if (Research.MODULAR_DESIGN in researched) cap = max(cap, cap + 2.0)
        }
        DeviceAspect.THERMALS -> {
            if (Research.SOC_MIDRANGE in researched) cap = max(cap, 84.0)
            if (Research.NPU_BLOCK in researched) cap = max(cap, 90.0)
            if (Research.MODULAR_DESIGN in researched) cap = max(cap, cap + 1.0)
        }
    }
    return cap.coerceAtMost(97.0)
}

/** Category-weighted review mix (sums to 1). */
internal fun deviceReviewWeights(category: DeviceCategory): Map<DeviceAspect, Double> = when (category) {
    DeviceCategory.PHONE -> mapOf(
        DeviceAspect.DISPLAY to 0.15,
        DeviceAspect.BATTERY to 0.22,
        DeviceAspect.CAMERA to 0.25,
        DeviceAspect.BUILD to 0.15,
        DeviceAspect.THERMALS to 0.10,
        DeviceAspect.INDUSTRIAL to 0.13,
    )
    DeviceCategory.LAPTOP -> mapOf(
        DeviceAspect.DISPLAY to 0.12,
        DeviceAspect.BATTERY to 0.15,
        DeviceAspect.PERF to 0.28,
        DeviceAspect.BUILD to 0.12,
        DeviceAspect.THERMALS to 0.22,
        DeviceAspect.INDUSTRIAL to 0.11,
    )
    DeviceCategory.HEADPHONES -> mapOf(
        DeviceAspect.BATTERY to 0.18,
        DeviceAspect.SOUND to 0.35,
        DeviceAspect.BUILD to 0.25,
        DeviceAspect.THERMALS to 0.07,
        DeviceAspect.INDUSTRIAL to 0.15,
    )
}

/** Budget phone with flagship camera ambition (etc.) softens demand. */
internal fun deviceTierFit(weights: List<Double>, tier: DeviceTier, category: DeviceCategory): Double {
    val w = normalizeFocus(weights)
    val specialty = w[DeviceFocus.SPECIALTY.ordinal]
    val build = w[DeviceFocus.BUILD.ordinal]
    return when (tier) {
        DeviceTier.BUDGET -> {
            var fit = 1.0
            if (specialty > 0.28) fit *= 0.82 // overspecced camera/perf on a bargain chassis
            if (build > 0.28) fit *= 0.92
            if (category == DeviceCategory.PHONE && specialty > 0.32) fit *= 0.9
            fit
        }
        DeviceTier.FLAGSHIP -> {
            var fit = 1.0
            if (specialty < 0.12) fit *= 0.88
            if (w[DeviceFocus.DISPLAY.ordinal] < 0.08 && category != DeviceCategory.HEADPHONES) fit *= 0.94
            fit
        }
    }.coerceIn(0.65, 1.1)
}

internal data class DeviceScore(
    val quality: Double,
    val aspects: Map<DeviceAspect, Double>,
    val pros: List<String>,
    val cons: List<String>,
    val dingNotes: List<String>,
)

internal fun scoreDevice(d: DeviceDesign, ctx: LaneContext, researched: Set<Research>): DeviceScore {
    val w = d.normalizedFocus
    val craft = 45 * (1 - exp(-1.2 * d.polish / maxOf(d.work, 1.0)))
    val skill = (ctx.staffing.averageSkill - 5) * 3
    val designCraft = (d.design / maxOf(d.work, 1.0) * 18).coerceIn(0.0, 22.0)
    val techCraft = (d.tech / maxOf(d.work, 1.0) * 18).coerceIn(0.0, 22.0)
    val base = 32.0 + craft * 0.55 + skill + ctx.mods.quality + (designCraft + techCraft) * 0.35

    val aspects = linkedMapOf<DeviceAspect, Double>()
    for (f in DeviceFocus.ALL) {
        val aspect = DeviceAspect.forFocus(f, d.spec.category)
        val share = w[f.ordinal]
        // Focus share vs even (1/6): underspending hurts, overspending helps with diminishing returns.
        val focusBoost = 1.0 + (share - 1.0 / 6) * 2.4
        val formBoost = formFactorBoost(d.formFactor, aspect)
        val researchBoost = partBoost(aspect, researched)
        val raw = (base * focusBoost * formBoost + researchBoost).coerceIn(5.0, 99.0)
        val cap = deviceAspectCap(aspect, researched)
        aspects[aspect] = min(raw, cap)
    }

    if (d.bundleOs) {
        // OEM-style integration bump across the board, slightly more on design/industrial.
        for (k in aspects.keys.toList()) {
            val bump = if (k == DeviceAspect.INDUSTRIAL) 3.5 else 2.0
            aspects[k] = min((aspects[k] ?: 0.0) + bump, deviceAspectCap(k, researched) + 2)
        }
    }

    val weights = deviceReviewWeights(d.spec.category)
    var quality = 0.0
    var weightSum = 0.0
    for ((aspect, weight) in weights) {
        val v = aspects[aspect] ?: continue
        quality += v * weight
        weightSum += weight
    }
    if (weightSum > 0) quality /= weightSum
    quality *= deviceTierFit(w, d.spec.tier, d.spec.category)
    quality = quality.coerceIn(5.0, 99.0)

    val ranked = aspects.entries.sortedByDescending { it.value }
    val pros = ranked.take(2).map { (a, v) ->
        when {
            v >= 85 -> "Excellent ${a.title.lowercase()}"
            v >= 75 -> "Strong ${a.title.lowercase()}"
            else -> "Solid ${a.title.lowercase()}"
        }
    }
    val cons = ranked.reversed().take(2).filter { it.value < 72 }.map { (a, v) ->
        when {
            v < 55 -> "Weak ${a.title.lowercase()}"
            else -> "Mediocre ${a.title.lowercase()}"
        }
    }
    val dingNotes = ranked.reversed().filter { it.value < 60 }.take(1).map { (a, _) ->
        "Reviewers dinged the ${a.title.lowercase()}."
    }

    return DeviceScore(quality, aspects, pros, cons, dingNotes)
}

private fun formFactorBoost(ff: FormFactor, aspect: DeviceAspect): Double = when (ff) {
    FormFactor.COMPACT -> when (aspect) {
        DeviceAspect.BATTERY -> 0.92
        DeviceAspect.INDUSTRIAL -> 1.06
        else -> 1.0
    }
    FormFactor.PRO, FormFactor.CREATOR -> when (aspect) {
        DeviceAspect.CAMERA, DeviceAspect.PERF -> 1.08
        DeviceAspect.THERMALS -> 1.04
        DeviceAspect.BATTERY -> 0.96
        else -> 1.0
    }
    FormFactor.ULTRAPORTABLE -> when (aspect) {
        DeviceAspect.BATTERY -> 1.05
        DeviceAspect.PERF -> 0.94
        DeviceAspect.INDUSTRIAL -> 1.04
        else -> 1.0
    }
    FormFactor.BUDS -> when (aspect) {
        DeviceAspect.SOUND -> 0.97
        DeviceAspect.BATTERY -> 0.95
        DeviceAspect.INDUSTRIAL -> 1.05
        else -> 1.0
    }
    FormFactor.OVER_EAR -> when (aspect) {
        DeviceAspect.SOUND -> 1.08
        DeviceAspect.BUILD -> 1.04
        else -> 1.0
    }
    FormFactor.STANDARD -> 1.0
}

private fun partBoost(aspect: DeviceAspect, researched: Set<Research>): Double {
    var bump = 0.0
    when (aspect) {
        DeviceAspect.DISPLAY -> {
            if (Research.DISPLAY_IPS in researched) bump += 1.5
            if (Research.OLED_DISPLAYS in researched) bump += 3.0
            if (Research.DISPLAY_LTPO in researched) bump += 2.0
        }
        DeviceAspect.BATTERY -> {
            if (Research.BATTERY_DENSE in researched) bump += 2.5
            if (Research.BATTERY_SI_ANODE in researched) bump += 2.0
            if (Research.FAST_CHARGING in researched) bump += 1.0
        }
        DeviceAspect.CAMERA -> {
            if (Research.CAMERA_DUAL in researched) bump += 2.0
            if (Research.CAMERA_TRIPLE in researched) bump += 2.5
            if (Research.CAMERA_COMPUTATIONAL in researched) bump += 3.0
        }
        DeviceAspect.PERF, DeviceAspect.SOUND, DeviceAspect.THERMALS -> {
            if (Research.SOC_MIDRANGE in researched) bump += 2.0
            if (Research.NPU_BLOCK in researched) bump += 3.0
        }
        DeviceAspect.BUILD, DeviceAspect.INDUSTRIAL -> {
            if (Research.MATERIALS_ALUMINUM in researched) bump += 2.0
            if (Research.MATERIALS_TITANIUM in researched) bump += 2.5
            if (Research.MODULAR_DESIGN in researched) bump += 1.5
        }
    }
    return bump
}

/** Migrate pre-depth DeviceDesign saves (empty / short focus → even). */
internal fun DeviceDesign.migrated(): DeviceDesign {
    if (focus.size == DeviceFocus.ALL.size && focus.sum() > 0) {
        val ff = if (formFactor in FormFactor.options(spec.category)) formFactor else FormFactor.defaultFor(spec.category)
        return if (ff == formFactor) this else copy(formFactor = ff)
    }
    return copy(
        focus = DeviceFocus.evenFocus(),
        formFactor = FormFactor.defaultFor(spec.category),
    )
}

/** Whether a bundled-OS toggle is available: OS lane open and a version shipped in the last ~2 years. */
fun GameState.canBundleOs(): Boolean {
    if (Lane.OS !in lanes) return false
    val lab = os ?: return false
    val latest = lab.releases.maxByOrNull { it.week } ?: return false
    return week - latest.week <= 104
}

/** First-party app pack is available when Apps is open (live or still building). */
fun GameState.canOfferAppPack(): Boolean = Lane.APPS in lanes && apps != null
