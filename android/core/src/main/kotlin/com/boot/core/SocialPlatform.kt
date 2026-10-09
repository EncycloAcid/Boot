package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min

/** [spark] is hidden product-market fit. Feature work nudges it up. */
@Serializable
class SocialPlatform(val name: String, var spark: Double) {
    var isLive = false
    var buildProgress = 0.0
    val buildWork: Double get() = 70.0
    var users = 0.0
    var peakUsers = 0.0

    /** 0...0.6 of accounts that aren't people. */
    var botShare = 0.0

    /** 0...1 */
    var toxicity = 0.05

    /** 0...1 player slider. Costs money, keeps the place livable. */
    var moderation = 0.3

    /** 0...1 player slider. Revenue now versus growth. */
    var adLoad = 0.2
    var weeksLive = 0
    var caughtOn = false
    var lastGrowth = 0.0
    val userHistory = mutableListOf<Double>()

    val realUsers: Double get() = users * (1 - botShare)

    internal fun tick(ctx: LaneContext, rng: SeededRNG): LaneReport {
        val report = LaneReport()
        val work = ctx.workOutput

        if (!isLive) {
            buildProgress += work
            report.expenses += 200 * ctx.mods.cost
            if (buildProgress >= buildWork) {
                isLive = true
                users = 1_000 * ctx.reception * ctx.marketDemand
                report.log("$name opens its doors. ${count(users)} people sign up in week one.", LogTone.MILESTONE)
                report.add(Incident.Stat(StatKey.RELEASE))
            }
            return report
        }

        weeksLive += 1
        spark = min(1.8, spark + work * 0.0004)

        val modPower = moderation * (0.5 + min(ctx.staffing.operations, 20.0) * 0.05)
        val cap = 400_000_000.0
        val network = 1 + 4 * users / (users + 30_000)
        val growth = 0.05 * spark * network * max(0.0, 1 - users / cap) * (1 - adLoad * 0.4) *
            ctx.marketDemand * ctx.reception * ctx.mods.growth * ctx.marketingBoost
        val churn = 0.015 + toxicity * 0.05 + botShare * 0.02
        lastGrowth = growth - churn
        users = max(50.0, users * (1 + lastGrowth) + ctx.staffing.marketing * 25)
        peakUsers = max(peakUsers, users)
        userHistory += users
        if (userHistory.size > 52) userHistory.removeAt(0)

        if (!caughtOn && users > 250_000) {
            caughtOn = true
            report.log("$name is catching on. Growth just went vertical.", LogTone.MILESTONE)
        }

        val scale = log10(users + 1)
        botShare = (botShare + 0.0025 * scale / 7 * ctx.era.demand(Lane.AI) - modPower * 0.01).clamped(0.0, 0.6)
        toxicity = (toxicity + 0.004 * scale / 7 + ctx.mods.toxicity - modPower * 0.012).clamped(0.0, 1.0)

        val maturity = min(1.0, users / 2_000_000)
        val advertiserTrust = 1 - toxicity * 0.5
        report.revenue += realUsers * adLoad * 0.02 * maturity * advertiserTrust * ctx.mods.demand
        report.expenses += (100 + users * 0.002 + users * moderation * 0.002) * ctx.mods.cost

        if (users > 100_000 && rng.chance(toxicity * 0.03)) {
            report.add(Incident.Event(EventKind.ModerationFire))
        }
        if (users > 500_000 && rng.chance(0.004 * scale / 7 * (1.2 - moderation))) {
            report.add(Incident.Event(EventKind.PressScandal))
        }
        if (botShare > 0.25 && rng.chance(0.02)) {
            report.add(Incident.Event(EventKind.BotWave))
        }
        return report
    }

    internal val assetValue: Double get() = realUsers * 2.5

    val statusLine: String
        get() {
            if (!isLive) return "Building $name: ${(min(1.0, buildProgress / buildWork) * 100).toInt()}%"
            return "$name: ${count(users)} users (${Format.percent(botShare)} bots), growth ${Format.percent(lastGrowth, 1)}/wk"
        }
}
