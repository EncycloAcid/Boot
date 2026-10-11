package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

@Serializable
enum class AppMonetization {
    ADS, SUBSCRIPTION, PAID;

    val title: String
        get() = when (this) {
            ADS -> "Free with ads"
            SUBSCRIPTION -> "Subscription"
            PAID -> "Paid upfront"
        }

    internal val baseChurn: Double
        get() = when (this) {
            ADS -> 0.05
            SUBSCRIPTION -> 0.07
            PAID -> 0.03
        }

    /** Weekly revenue per active user. */
    internal val arpu: Double
        get() = when (this) {
            ADS -> 0.03
            SUBSCRIPTION -> 0.08
            PAID -> 0.0
        }

    internal val installAppeal: Double
        get() = when (this) {
            ADS -> 1.0
            SUBSCRIPTION -> 0.8
            PAID -> 0.35
        }
}

@Serializable
enum class UpdateStyle {
    /** Ship little and often. Keeps the app fresh. */
    SMALL,

    /** Big quarterly drops. More quality per update, longer stale stretches. */
    BIG;

    val title: String get() = if (this == SMALL) "Small, frequent updates" else "Big, rare updates"
    val work: Double get() = if (this == SMALL) 15.0 else 45.0
    internal val qualityGain: Double get() = if (this == SMALL) 1.5 else 5.5
}

/** How much the app charges, whatever the model: cheap grows, premium earns. */
@Serializable
enum class AppPricing(val title: String, val detail: String, internal val arpu: Double, internal val appeal: Double, internal val churn: Double) {
    LOW("Cheap", "More installs and fewer leave, less money from each.", 0.7, 1.25, 0.95),
    STANDARD("Standard", "The going rate.", 1.0, 1.0, 1.0),
    HIGH("Premium", "More from each user, fewer installs, and more of them leave.", 1.4, 0.75, 1.08),
}

/** What an experiment moves. */
@Serializable
enum class Metric(val title: String) { INSTALLS("installs"), CHURN("churn"), ARPU("revenue per user") }

/** An A/B test you can run on the app. Nobody knows how it'll go until it's run. */
@Serializable
enum class ExperimentKind(val title: String, val detail: String, val metric: Metric, internal val mean: Double, internal val spread: Double) {
    ONBOARDING("New onboarding", "A shorter first-run flow. Fewer people give up on day one.", Metric.CHURN, 0.95, 0.04),
    NUDGES("Push nudges", "Remind lapsed users to come back. Some find it annoying.", Metric.CHURN, 0.99, 0.07),
    SCREENSHOTS("New store page", "Fresh screenshots and a video for the store listing.", Metric.INSTALLS, 1.06, 0.05),
    REFERRAL_BONUS("Invite bonus", "Reward people who bring friends.", Metric.INSTALLS, 1.04, 0.06),
    EARLY_PAYWALL("Earlier paywall", "Ask for money sooner. More pay; some leave.", Metric.ARPU, 1.07, 0.08),
    BUNDLE("Annual bundle", "A discounted yearly plan.", Metric.ARPU, 1.04, 0.04),
}

/** An experiment running on part of the user base. */
@Serializable
data class Experiment(val kind: ExperimentKind, var weeksLeft: Int, val lift: Double)

@Serializable
class AppStudio(val name: String, var monetization: AppMonetization) {
    var isLive = false
    var buildProgress = 0.0
    val buildWork: Double get() = 50.0
    var users = 0.0
    var peakUsers = 0.0

    /** 0...100 */
    var quality = 50.0
    var rank = 450.0
    var weeksSinceUpdate = 0
    var updateProgress = 0.0
    var updatesShipped = 0
    var updateStyle = UpdateStyle.SMALL
    var lastChurn = 0.0
    var lastInstalls = 0.0

    var pricing = AppPricing.STANDARD
    var experiment: Experiment? = null
    val experimentsRun = mutableListOf<ExperimentKind>()

    /** What kept experiments have done, multiplied together. */
    var installLift = 1.0
    var churnLift = 1.0
    var arpuLift = 1.0

    /** Last 26 weeks of churn, for the chart everyone stares at. */
    val churnHistory = mutableListOf<Double>()
    val userHistory = mutableListOf<Double>()

    val rankLabel: String get() = if (isLive) "#${rank.roundToInt()}" else "Unreleased"

    internal fun tick(ctx: LaneContext, rng: SeededRNG): LaneReport {
        val report = LaneReport()
        val work = ctx.workOutput

        if (!isLive) {
            buildProgress += work
            report.expenses += 120 * ctx.mods.cost
            if (buildProgress >= buildWork) {
                isLive = true
                quality = shippedQuality(buildProgress * ctx.polishEfficiency, buildWork, ctx.staffing.averageSkill, ctx.mods.quality)
                users = 1_500 * ctx.reception * ctx.marketDemand
                report.log("$name is live on the App Store and Google Play. ${count(users)} installs in launch week.", LogTone.MILESTONE)
                report.add(Incident.Stat(StatKey.RELEASE))
            }
            return report
        }

        updateProgress += work
        weeksSinceUpdate += 1
        if (updateProgress >= updateStyle.work) {
            updateProgress -= updateStyle.work
            weeksSinceUpdate = 0
            updatesShipped += 1
            quality = min(97.0, quality + updateStyle.qualityGain * (1 - quality / 110) + ctx.mods.quality * 0.05)
            if (updateStyle == UpdateStyle.BIG) {
                report.log("$name ${updatesShipped + 1}.0 ships. Reviews perk up.", LogTone.GOOD)
            }
        }

        experiment?.let { x ->
            x.weeksLeft -= 1
            if (x.weeksLeft <= 0) {
                experiment = null
                report.add(Incident.Inbox(ExperimentResult(x.kind, x.lift), 4))
            }
        }

        val stale = 0.003 * max(0, weeksSinceUpdate - 4)
        val churn = ((monetization.baseChurn * (1.6 - quality / 100) + stale) * ctx.mods.churn * churnLift * pricing.churn).clamped(0.006, 0.3)
        val organic = 200_000 / max(rank, 1.0)
        val appeal = (quality / 60).pow(2) * monetization.installAppeal * installLift * pricing.appeal
        val installs = (organic + ctx.staffing.marketing * 80) * appeal * ctx.reception * ctx.marketDemand

        users = max(0.0, users * (1 - churn) + installs)
        peakUsers = max(peakUsers, users)
        lastChurn = churn
        lastInstalls = installs

        // Store rank chases install velocity, tempered by churn.
        val score = installs * max(0.2, 1.2 - churn * 4)
        val target = (500 / (1 + score / 400).pow(0.6)).clamped(1.0, 500.0)
        rank += (target - rank) * 0.35

        churnHistory += churn
        userHistory += users
        if (churnHistory.size > 26) churnHistory.removeAt(0)
        if (userHistory.size > 52) userHistory.removeAt(0)

        report.revenue += users * monetization.arpu * arpuLift * pricing.arpu
        if (monetization == AppMonetization.PAID) report.revenue += installs * 2.99 * 0.7 * arpuLift * pricing.arpu
        report.expenses += (users * 0.004 + 150) * ctx.mods.cost

        if (rank < 10 && rng.chance(0.05)) {
            report.log("$name is sitting at #${rank.toInt()} on the App Store.", LogTone.GOOD)
        }
        return report
    }

    internal val assetValue: Double get() = users * 1.5

    val statusLine: String
        get() {
            if (!isLive) return "Building $name: ${(min(1.0, buildProgress / buildWork) * 100).toInt()}%"
            return "$name: ${count(users)} users, $rankLabel, churn ${Format.percent(lastChurn, 1)}/wk"
        }
}
