package com.boot.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.max

/*
 * The five businesses beyond games, deeper. Each gets new decisions:
 * - Agency: happy clients offer retainers (steady money, a steady slice of the team), and big brands
 *   invite pitches for major projects (pay to pitch, win or lose a few weeks later).
 * - Apps: a price level, and A/B experiments whose results nobody knows until they're run.
 * - Social: a creator fund, a premium tier and the house rules.
 * - Hardware: discount or mark up the device on sale, and book a launch event for the next one.
 * - AI: a trained model waits for your call (ship it, safety-test it, or release the weights), and
 *   GPUs can be reserved for a year at a discount.
 */

@Serializable
sealed interface LaneDecision : EventKind

@Serializable @SerialName("retainerOffer")
data class RetainerOffer(val client: String, val weekly: Double, val load: Double) : LaneDecision

@Serializable @SerialName("bigPitch")
data class BigPitch(val brand: String, val value: Double, val cost: Double, val chance: Double) : LaneDecision

@Serializable @SerialName("experimentResult")
data class ExperimentResult(val kind: ExperimentKind, val lift: Double) : LaneDecision

@Serializable @SerialName("modelReady")
data class ModelReady(val codename: String) : LaneDecision

/** How an experiment's result reads: "+6% installs", "churn down 4%". */
fun ExperimentResult.summary(): String {
    val pct = Format.percent(abs(lift - 1), 0)
    return when (kind.metric) {
        Metric.CHURN -> if (lift <= 1) "churn down $pct" else "churn up $pct"
        else -> if (lift >= 1) "${kind.metric.title} up $pct" else "${kind.metric.title} down $pct"
    }
}

/** Is this result an improvement? */
val ExperimentResult.better: Boolean get() = if (kind.metric == Metric.CHURN) lift < 1 else lift > 1

internal fun GameState.describeLane(k: LaneDecision): Description? = when (k) {
    is RetainerOffer -> Description(
        "${k.client} wants a retainer",
        "They loved the work and want you on call: ${money(k.weekly)} a week for as long as it lasts. " +
            "It takes about ${String.format(java.util.Locale.US, "%.1f", k.load)} points of the team's week before any project gets a look in.",
        listOf(
            choice("Sign them up", "Steady money. Less room for new projects."),
            choice("Decline", "Keep the team free for projects."),
        ),
        Lane.WEBSITES,
    )
    is BigPitch -> Description(
        "${k.brand} is inviting pitches",
        "A ${money(k.value)} project, if you win it. Pitching costs ${money(k.cost)} in spec work, and you'd have about a " +
            "${Format.percent(k.chance)} chance against the big agencies.",
        listOf(
            choice("Pitch for it", "${money(k.cost)} now. The decision comes in three weeks."),
            choice("Sit this one out", "No cost, no chance."),
        ),
        Lane.WEBSITES,
    )
    is ExperimentResult -> Description(
        "Experiment results: ${k.kind.title.lowercase()}",
        "The test group saw ${k.summary()}. " + if (k.better) "Roll it out to everyone?" else "That's worse. Better roll it back.",
        listOf(
            choice("Roll it out", "Everyone gets it: ${k.summary()}."),
            choice("Roll it back", "Back to how it was."),
        ),
        Lane.APPS,
    )
    is ModelReady -> ai?.pendingModel?.let { p ->
        Description(
            "${p.codename} is ready",
            "Training's done and the early numbers look " + when {
                p.roll > 1.3 -> "remarkable."
                p.roll < 0.6 -> "modest."
                else -> "good."
            } + " How do you want to release it?",
            listOf(
                choice("Ship it now", "Live this week."),
                choice("Safety testing first", "Six weeks of evals. Regulators relax, and the fixes make it a little better."),
                choice("Release the weights", "Give the model away. Developers and researchers love you; the API earns less."),
            ),
            Lane.AI,
        )
    }
}

internal fun GameState.resolveLane(k: LaneDecision, choice: Int) {
    when (k) {
        is RetainerOffer -> {
            val w = web ?: return
            if (choice == 0) {
                w.retainers += Retainer(k.client, k.weekly, k.load, week)
                addLog("${k.client} signed a ${money(k.weekly)}/week retainer.", LogTone.GOOD, Lane.WEBSITES)
            }
        }
        is BigPitch -> {
            if (choice == 0) {
                spend(k.cost, Lane.WEBSITES)
                later(3, Consequence.PitchResult(k.brand, k.value, k.chance))
                addLog("You pitched for the ${k.brand} project. Fingers crossed.", LogTone.NEUTRAL, Lane.WEBSITES)
            }
        }
        is ExperimentResult -> {
            val a = apps ?: return
            if (choice == 0) {
                when (k.kind.metric) {
                    Metric.INSTALLS -> a.installLift *= k.lift
                    Metric.CHURN -> a.churnLift *= k.lift
                    Metric.ARPU -> a.arpuLift *= k.lift
                }
                addLog("Rolled out ${k.kind.title.lowercase()}: ${k.summary()}.", if (k.better) LogTone.GOOD else LogTone.BAD, Lane.APPS)
            } else {
                addLog("Rolled back ${k.kind.title.lowercase()}.", LogTone.NEUTRAL, Lane.APPS)
            }
        }
        is ModelReady -> {
            val lab = ai ?: return
            val p = lab.pendingModel ?: return
            when (choice) {
                0 -> {
                    lab.pendingModel = null
                    addLog(lab.release(p), LogTone.MILESTONE, Lane.AI)
                    stats.releases += 1
                    stats.releasesThisQuarter += 1
                }
                1 -> {
                    p.releaseWeek = week + 6
                    lab.regulatoryHeat = max(0.0, lab.regulatoryHeat - 0.25)
                    reputation.adjust(2.0)
                    addLog("${p.codename} goes through six weeks of safety testing first.", LogTone.NEUTRAL, Lane.AI)
                }
                else -> {
                    lab.pendingModel = null
                    lab.openWeights = true
                    addLog(lab.release(p), LogTone.MILESTONE, Lane.AI)
                    stats.releases += 1
                    stats.releasesThisQuarter += 1
                    lab.regulatoryHeat = max(0.0, lab.regulatoryHeat - 0.1)
                    reputation.adjust(4.0)
                    researchPoints += 20.0
                    addLog("You released ${p.codename}'s weights. Developers are building on it everywhere.", LogTone.GOOD, Lane.AI)
                    publish(NewsDesk.INDUSTRY, techOutlet(Lane.AI, rng), "$companyName gives away ${p.codename}", "Open weights, free for anyone.", LogTone.GOOD, Lane.AI)
                }
            }
        }
    }
}

/** A pitch is decided. */
internal fun GameState.landPitch(c: Consequence.PitchResult) {
    val w = web ?: return
    if (rng.chance(c.chance)) {
        w.addBigProject(c.brand, c.value, week, staffing(Lane.WEBSITES).output)
        w.standing = (w.standing + 0.05).clamped(0.0, 1.0)
        addLog("You won the ${c.brand} pitch: a ${money(c.value)} project.", LogTone.MILESTONE, Lane.WEBSITES)
        publish(NewsDesk.YOU, techOutlet(Lane.WEBSITES, rng), "$companyName wins ${c.brand}'s big project", lane = Lane.WEBSITES)
    } else {
        addLog("${c.brand} went with a bigger agency.", LogTone.NEUTRAL, Lane.WEBSITES)
    }
}

// Agency

fun GameState.dropRetainer(client: String) {
    require(Lane.WEBSITES)
    val w = web!!
    if (w.retainers.removeAll { it.client == client }) addLog("You ended ${client}'s retainer.", LogTone.NEUTRAL, Lane.WEBSITES)
}

// Apps

fun GameState.setAppPricing(p: AppPricing) {
    require(Lane.APPS)
    apps!!.pricing = p
}

/** What an experiment costs to run. */
val GameState.experimentCost: Double get() = 10_000.0 + (apps?.users ?: 0.0) * 0.02

fun GameState.experimentBlocker(kind: ExperimentKind): String? {
    val a = apps ?: return "You don't run an app studio."
    if (!a.isLive) return "Launch the app first."
    if (a.experiment != null) return "An experiment is already running."
    if (kind in a.experimentsRun) return "You've already tried that."
    if (cash < experimentCost) return "Costs ${money(experimentCost)}."
    return null
}

fun GameState.startExperiment(kind: ExperimentKind) {
    require(Lane.APPS)
    experimentBlocker(kind)?.let { throw notAvailable(it) }
    val a = apps!!
    spend(experimentCost, Lane.APPS)
    val lift = rng.normal(kind.mean, kind.spread).clamped(0.8, 1.25)
    a.experiment = Experiment(kind, 4, lift)
    a.experimentsRun += kind
    addLog("Testing ${kind.title.lowercase()} on a slice of ${a.name}'s users. Results in four weeks.", LogTone.NEUTRAL, Lane.APPS)
}

// Social

fun GameState.setCreatorFund(level: Int) {
    require(Lane.SOCIAL)
    social!!.creatorFund = level.coerceIn(0, 2)
}

fun GameState.setPremium(on: Boolean) {
    require(Lane.SOCIAL)
    val s = social!!
    if (s.premium == on) return
    s.premium = on
    if (on && s.isLive) {
        s.users *= 0.98
        addLog("${s.name} Premium is live. A few people left in protest; plenty signed up.", LogTone.NEUTRAL, Lane.SOCIAL)
    }
}

fun GameState.setContentPolicy(p: ContentPolicy) {
    require(Lane.SOCIAL)
    social!!.policy = p
}

// Hardware

fun GameState.setDevicePrice(shift: Double) {
    require(Lane.HARDWARE)
    val h = hardware!!
    if (h.onMarket == null) throw notAvailable("Nothing on sale to reprice.")
    h.priceShift = shift.clamped(-0.2, 0.2)
}

/** What a launch event costs: a livestream or a full keynote. */
fun keynoteCost(level: Int): Double = if (level == 2) 400_000.0 else 50_000.0

fun GameState.bookKeynote(level: Int) {
    require(Lane.HARDWARE)
    val h = hardware!!
    if (h.design == null) throw notAvailable("Book a launch event once there's a device in design.")
    if (h.keynote > 0) throw notAvailable("A launch event is already booked.")
    val cost = keynoteCost(level)
    if (cash < cost) throw insufficientFunds("Costs ${money(cost)}.")
    spend(cost, Lane.HARDWARE)
    h.keynote = level.coerceIn(1, 2)
    addLog(if (level == 2) "Booked a keynote for the ${h.design!!.name} launch." else "Booked a launch livestream for the ${h.design!!.name}.", LogTone.NEUTRAL, Lane.HARDWARE)
}

// AI

/** The weekly price of reserving GPUs for a year: 40% off today's rent. */
val GameState.reservedRate: Double get() = AILab.RENT_PER_UNIT * world.gpuPrice * 0.6

fun GameState.reserveCompute(units: Int) {
    require(Lane.AI)
    val lab = ai!!
    if (lab.reservedUnits > 0 && week < lab.reservedUntil) throw notAvailable("You already have a contract until ${GameClock.label(lab.reservedUntil)}.")
    if (cash < reservedRate * units * 4) throw insufficientFunds("Needs a month up front: ${money(reservedRate * units * 4)}.")
    lab.reservedUnits = units
    lab.reservedRate = reservedRate
    lab.reservedUntil = week + 52
    addLog("Reserved $units GPU units for a year at ${money(reservedRate)} each a week.", LogTone.NEUTRAL, Lane.AI)
}
