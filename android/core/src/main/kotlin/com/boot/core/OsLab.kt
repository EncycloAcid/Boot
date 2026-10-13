package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/** Target devices for an OS release. Eras unlock phone and dual targets. */
@Serializable
enum class OsTarget {
    PC, PHONE, BOTH;

    val title: String
        get() = when (this) {
            PC -> "PC"
            PHONE -> "Phone"
            BOTH -> "PC & phone"
        }

    internal val work: Double
        get() = when (this) {
            PC -> 320.0
            PHONE -> 280.0
            BOTH -> 480.0
        }

    internal val weeklyBurn: Double
        get() = when (this) {
            PC -> 45_000.0
            PHONE -> 40_000.0
            BOTH -> 70_000.0
        }

    /** Year this target becomes available. */
    val arrives: Int
        get() = when (this) {
            PC -> 1975
            PHONE -> 2007
            BOTH -> 2012
        }
}

@Serializable
data class OsProject(
    val codename: String,
    val version: Int,
    val target: OsTarget,
    var progress: Double = 0.0,
    var design: Double = 0.0,
    var tech: Double = 0.0,
    var bugs: Double = 0.0,
    var weeksInDev: Int = 0,
    var spent: Double = 0.0,
    /** Optional beta: delays ship, cuts bugs, raises OEM trust. */
    var betaWeeksLeft: Int = 0,
    var inBeta: Boolean = false,
) {
    val work: Double get() = target.work
    val isReady: Boolean get() = progress >= work
    val completion: Double get() = min(1.0, progress / work)
}

@Serializable
data class OsRelease(
    val id: Int,
    val codename: String,
    val version: Int,
    val target: OsTarget,
    val week: Int,
    val score: Double,
    val design: Double,
    val tech: Double,
    var bugs: Double,
    val beta: Boolean,
    var installedBase: Double = 0.0,
    var oemPartners: Int = 0,
    var weeklyLicense: Double = 0.0,
    var rolledBack: Boolean = false,
)

/** Platform Lab: kernel research, version projects, beta, OEM licensing, ecosystem. */
@Serializable
class OsLab {
    var project: OsProject? = null
    val releases = mutableListOf<OsRelease>()
    var oemTrust = 0.4
    var ecosystem = 0.2
    var lifetimeLicenses = 0.0
    var versionsShipped = 0
    var lastLicense = 0.0
    private var nextId = 1
    private val usedNames = mutableListOf<String>()

    companion object {
        val CODENAMES = listOf(
            "Cedar", "Horizon", "Pulse", "Nimbus", "Quill", "Forge", "Atlas", "Mercury",
            "Cascade", "Orchid", "Summit", "Drift", "Harbor", "Lumen", "Vertex", "Echo",
        )
    }

    internal fun nextCodename(rng: SeededRNG): String {
        val fresh = CODENAMES.filter { it !in usedNames }
        val name = if (fresh.isEmpty()) rng.pick(CODENAMES) + " ${versionsShipped + 1}" else rng.pick(fresh)
        usedNames += name
        return name
    }

    fun startProject(target: OsTarget, rng: SeededRNG) {
        if (project != null) throw busy("An OS version is already in development.")
        val version = versionsShipped + 1
        project = OsProject(nextCodename(rng), version, target)
    }

    fun startBeta(weeks: Int = 6) {
        val p = project ?: throw invalid("No OS project.")
        if (!p.isReady) throw invalid("${p.codename} isn't feature-complete yet.")
        if (p.inBeta) throw busy("Already in beta.")
        p.inBeta = true
        p.betaWeeksLeft = weeks
    }

    fun ship(ctx: LaneContext, rng: SeededRNG, hardwareBundle: Boolean, appsBoost: Boolean): LaneReport {
        val p = project ?: throw invalid("No OS project to ship.")
        if (!p.isReady) throw invalid("${p.codename} isn't ready.")
        if (p.inBeta && p.betaWeeksLeft > 0) throw invalid("Beta still has ${p.betaWeeksLeft} weeks left. Wait, or cancel beta first.")
        val report = LaneReport()
        val kernel = ctx.mods.quality
        val craft = (p.design + p.tech) / max(1.0, p.work)
        val bugPenalty = 1 - min(0.4, p.bugs / (p.work * 0.5) * 0.4)
        var score = (5.0 + craft * 2.2 + kernel / 12 + (if (p.inBeta) 0.6 else -0.4)).clamped(1.0, 10.0)
        score = softCap(score * bugPenalty + if (hardwareBundle) 0.3 else 0.0)
        if (appsBoost) score = (score + 0.25).clamped(1.0, 10.0)

        val trustGain = if (p.inBeta) 0.08 else -0.05
        oemTrust = (oemTrust + trustGain + (score - 6) * 0.02).clamped(0.05, 1.0)
        if (appsBoost) ecosystem = (ecosystem + 0.06).clamped(0.0, 1.0)
        if (hardwareBundle) ecosystem = (ecosystem + 0.04).clamped(0.0, 1.0)

        val partners = (oemTrust * 8 * (score / 7)).roundToInt().coerceIn(0, 12)
        val base = 50_000.0 * (score / 7).pow(2) * (1 + partners * 0.15) * ctx.marketDemand *
            (if (p.target == OsTarget.BOTH) 1.4 else 1.0)
        val release = OsRelease(
            id = nextId++, codename = p.codename, version = p.version, target = p.target,
            week = ctx.week, score = (score * 10).roundToInt() / 10.0, design = p.design, tech = p.tech,
            bugs = p.bugs, beta = p.inBeta, installedBase = base, oemPartners = partners,
        )
        // Raw ship risk: recall-like rollback.
        if (!p.inBeta && p.bugs > p.work * 0.25 && rng.chance(0.35 + p.bugs / p.work)) {
            release.rolledBack = true
            release.installedBase *= 0.45
            oemTrust = (oemTrust - 0.12).clamped(0.05, 1.0)
            report.add(Incident.Scar(ScarKind.RECALL, 0.25, "${p.codename} had to be rolled back."))
            report.log(
                "${p.codename} ships at ${release.score}/10 — then a critical bug forces a rollback. OEMs freeze orders.",
                LogTone.BAD,
            )
        } else {
            report.log(
                "${p.codename} (v${p.version}) ships for ${p.target.title.lowercase()} at ${release.score}/10. " +
                    "$partners OEM partner${if (partners == 1) "" else "s"}." +
                    if (p.inBeta) " Beta paid off." else "",
                LogTone.MILESTONE,
            )
        }
        releases.add(0, release)
        versionsShipped += 1
        project = null
        report.add(Incident.Stat(StatKey.RELEASE))
        report.add(Incident.Reputation(if (release.rolledBack) -2.0 else 3.0))
        return report
    }

    internal fun tick(ctx: LaneContext, rng: SeededRNG): LaneReport {
        val report = LaneReport()
        val work = ctx.workOutput

        project?.let { p ->
            p.weeksInDev += 1
            if (p.inBeta && p.betaWeeksLeft > 0) {
                p.betaWeeksLeft -= 1
                val fixed = min(p.bugs, work * 0.9)
                p.bugs -= fixed
                p.tech += work * 0.15
                report.expenses += p.target.weeklyBurn * 0.6 * ctx.mods.cost
                if (p.betaWeeksLeft <= 0) {
                    report.log("${p.codename} beta wraps. Bugs are down; OEMs are watching.", LogTone.GOOD)
                }
            } else if (!p.isReady) {
                val designShare = 0.42
                p.design += work * 1.8 * designShare * (ctx.staffing.avgDesign / 5)
                p.tech += work * 1.8 * (1 - designShare) * (ctx.staffing.avgTech / 5) * (1 + ctx.staffing.research / 40)
                p.bugs += work * 0.28 * (1.2 - ctx.staffing.avgTech / 10)
                p.progress += work
                p.spent += p.target.weeklyBurn * ctx.mods.cost
                report.expenses += p.target.weeklyBurn * ctx.mods.cost
                if (p.isReady) {
                    report.log("${p.codename} is feature-complete. Run a beta or ship raw.", LogTone.GOOD)
                }
            } else {
                // Idle polish while waiting to beta/ship.
                p.bugs = max(0.0, p.bugs - work * 0.2)
                report.expenses += p.target.weeklyBurn * 0.4 * ctx.mods.cost
            }
        }

        var license = 0.0
        for (r in releases) {
            if (r.rolledBack && r.installedBase < 1_000) continue
            val age = ctx.week - r.week
            val fee = r.installedBase * 0.04 * (r.score / 7) * (1 + r.oemPartners * 0.05) *
                (1 + ecosystem * 0.3) * ctx.marketDemand * (0.985.pow(age / 13.0))
            r.weeklyLicense = fee
            r.installedBase *= 1 + 0.008 * oemTrust * (r.score / 8) * (1 - age / 400.0).coerceAtLeast(0.2)
            license += fee
            lifetimeLicenses += fee
        }
        lastLicense = license
        report.revenue += license

        // Slow ecosystem drift toward first-party strength.
        ecosystem = (ecosystem * 0.998 + versionsShipped * 0.0005).clamped(0.0, 1.0)

        if (rng.chance(0.008) && releases.isNotEmpty() && oemTrust > 0.55) {
            val r = releases.first()
            r.oemPartners += 1
            report.log("Another OEM signs on for ${r.codename}.", LogTone.GOOD)
        }
        return report
    }

    internal val assetValue: Double
        get() = releases.sumOf { it.installedBase * 0.8 } + oemTrust * 400_000.0 + ecosystem * 300_000.0

    val statusLine: String
        get() {
            project?.let { p ->
                return if (p.inBeta) {
                    "${p.codename} in beta (${p.betaWeeksLeft}w left)"
                } else {
                    "${p.codename} v${p.version} ${(p.completion * 100).toInt()}%" + if (p.isReady) ", ready to ship" else ""
                }
            }
            val latest = releases.firstOrNull() ?: return "No OS version in development"
            return "${latest.codename}: ${Format.count(latest.installedBase)} devices, ${latest.oemPartners} OEMs"
        }
}

// Player actions

private fun GameState.osLab(): OsLab {
    if (ending != null) throw gameOver()
    return os ?: throw notAvailable("You don't run a platform lab.")
}

fun GameState.osTargetOpen(t: OsTarget): Boolean = calendarYear >= t.arrives

fun GameState.startOsVersion(target: OsTarget) {
    val lab = osLab()
    if (!osTargetOpen(target)) throw notAvailable("${target.title} targets unlock in ${target.arrives}.")
    lab.startProject(target, rng)
    val p = lab.project!!
    addLog("Platform Lab starts ${p.codename} (v${p.version}) for ${target.title.lowercase()}.", LogTone.NEUTRAL, Lane.OS)
}

fun GameState.startOsBeta(weeks: Int = 6) {
    val lab = osLab()
    lab.startBeta(weeks)
    addLog("${lab.project!!.codename} enters a $weeks-week beta. Bugs fall; ship date slips.", LogTone.NEUTRAL, Lane.OS)
}

fun GameState.cancelOsBeta() {
    val p = osLab().project ?: throw invalid("No OS project.")
    if (!p.inBeta) throw invalid("Not in beta.")
    p.inBeta = false
    p.betaWeeksLeft = 0
    addLog("${p.codename} beta cancelled. You can ship raw whenever you're ready.", LogTone.NEUTRAL, Lane.OS)
}

fun GameState.shipOs() {
    val lab = osLab()
    val hardwareBundle = hardware?.onMarket != null && Lane.HARDWARE in lanes
    val appsBoost = apps?.isLive == true && Lane.APPS in lanes
    val report = lab.ship(context(Lane.OS), rng, hardwareBundle, appsBoost)
    if (hardwareBundle) {
        report.log("Shipping alongside your own hardware sweetens the launch.", LogTone.GOOD)
    }
    if (appsBoost) {
        report.log("First-party apps lift the ecosystem score.", LogTone.GOOD)
    }
    cash += report.revenue - report.expenses
    recordRevenue(report.revenue, Lane.OS)
    recordCost(report.expenses, Lane.OS)
    handle(report.incidents, Lane.OS)
}
