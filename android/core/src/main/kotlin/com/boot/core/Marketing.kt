package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/*
 * Marketing for every business. Each one has brand awareness: how many of the people it could sell
 * to have heard of it. Awareness has a natural level set by how big the business is, and campaigns
 * push it higher; every point above that natural level lifts demand. Once a campaign ends, people
 * slowly forget.
 *
 * Campaigns run on a channel (print, TV, search ads, social ads, influencers, trade shows, a PR
 * agency), each around from its real year and suited to some businesses more than others, at a
 * budget and for a length you pick. The report says what each one cost and roughly what it earned.
 */

@Serializable
enum class Channel(
    val title: String,
    val blurb: String,
    /** Weekly cost at the smallest budget. */
    val weekly: Double,
    /** Awareness a week at the smallest budget, for a business it suits. */
    val reach: Double,
    val since: Int,
) {
    PRINT("Print ads", "Magazines and newspapers. Cheap, steady and a little old-fashioned.", 3_000.0, 0.010, 1900),
    TV("TV spots", "Expensive, and nothing reaches more people at once.", 40_000.0, 0.050, 1950),
    PR("PR agency", "Stories about you instead of ads. Builds reputation too.", 5_000.0, 0.011, 1900),
    EVENTS("Trade shows", "Conferences and booths, where the business buyers are.", 18_000.0, 0.022, 1900),
    SEARCH("Search ads", "Show up when people are looking for what you sell.", 8_000.0, 0.026, 1998),
    SOCIAL_ADS("Social ads", "Targeted ads on the big social networks.", 10_000.0, 0.030, 2007),
    INFLUENCERS("Influencers", "Creators and streamers who talk about you to their fans.", 14_000.0, 0.034, 2012),
    ;

    /** How well this channel suits a business, around 1. */
    fun fit(lane: Lane): Double = when (this) {
        PRINT -> when (lane) { Lane.HARDWARE, Lane.GAMES, Lane.OS -> 1.1; Lane.WEBSITES -> 1.0; Lane.AI -> 0.8; else -> 0.7 }
        TV -> when (lane) { Lane.HARDWARE -> 1.3; Lane.GAMES -> 1.15; Lane.APPS, Lane.OS -> 1.0; Lane.AI, Lane.SOCIAL -> 0.8; Lane.WEBSITES -> 0.5 }
        PR -> 1.0
        EVENTS -> when (lane) { Lane.WEBSITES, Lane.AI, Lane.OS -> 1.4; Lane.HARDWARE -> 1.2; Lane.GAMES -> 1.0; else -> 0.6 }
        SEARCH -> when (lane) { Lane.WEBSITES -> 1.3; Lane.APPS -> 1.2; Lane.AI, Lane.SOCIAL, Lane.OS -> 1.0; Lane.GAMES -> 0.9; Lane.HARDWARE -> 0.8 }
        SOCIAL_ADS -> when (lane) { Lane.APPS -> 1.3; Lane.GAMES, Lane.SOCIAL -> 1.2; Lane.HARDWARE, Lane.OS -> 1.0; else -> 0.8 }
        INFLUENCERS -> when (lane) { Lane.GAMES -> 1.4; Lane.APPS, Lane.SOCIAL -> 1.2; Lane.HARDWARE, Lane.OS -> 1.1; Lane.AI -> 0.8; Lane.WEBSITES -> 0.6 }
    }

    /** "Great fit", "Good fit" or "Poor fit", for the picker. */
    fun fitLabel(lane: Lane): String = fit(lane).let { if (it >= 1.2) "Great fit" else if (it >= 0.95) "Good fit" else "Poor fit" }
}

/** How much to spend: each level costs more a week, and does a little less per dollar. */
@Serializable
enum class AdBudget(val title: String, val scale: Double) {
    SMALL("Small", 1.0), MEDIUM("Medium", 3.0), LARGE("Large", 8.0);

    /** Reach grows slower than spend. */
    val reach: Double get() = scale.pow(0.62)
}

/** How long a campaign can run. */
val CampaignLengths = listOf(4, 8, 13)

@Serializable
data class AdCampaign(
    val lane: Lane,
    val channel: Channel,
    val budget: AdBudget,
    val weeks: Int,
    val weeklyCost: Double,
    val startWeek: Int,
    val startAwareness: Double,
    var weeksLeft: Int = weeks,
    var spent: Double = 0.0,
    /** Revenue the extra awareness brought in, roughly. */
    var earned: Double = 0.0,
)

/** A finished campaign, for the report. */
@Serializable
data class CampaignResult(
    val lane: Lane,
    val channel: Channel,
    val budget: AdBudget,
    val weeks: Int,
    val endWeek: Int,
    val spent: Double,
    val from: Double,
    val to: Double,
    var earned: Double,
)

@Serializable
class Marketing {
    /** Awareness campaigns have added on top of the natural level. It fades once they stop. */
    val boost = mutableMapOf<Lane, Double>()
    val campaigns = mutableListOf<AdCampaign>()
    val results = mutableListOf<CampaignResult>()
}

/** How aware people are of a business, 0...1. */
fun GameState.awareness(lane: Lane): Double = naturalAwareness(lane) + (marketing.boost[lane] ?: 0.0)

/** Where awareness settles on its own: higher for a bigger business. */
fun GameState.naturalAwareness(lane: Lane): Double {
    val weekly = max(0.0, trailingRevenue(lane, 4) / 4)
    return (0.08 + 0.07 * log10(1 + weekly / 5_000)).clamped(0.08, 0.5)
}

/** What awareness above its natural level does for demand: half a percent per point. */
fun GameState.brandLift(lane: Lane): Double = 1 + 0.5 * (marketing.boost[lane] ?: 0.0)

fun GameState.campaignFor(lane: Lane): AdCampaign? = marketing.campaigns.firstOrNull { it.lane == lane }

/** The channels around this year. */
fun GameState.channels(): List<Channel> = Channel.entries.filter { it.since <= calendarYear }

/** What a campaign costs a week. */
fun GameState.campaignCost(channel: Channel, budget: AdBudget): Double = channel.weekly * budget.scale

/** Roughly how much awareness a week a campaign adds, before it starts to saturate. */
fun GameState.campaignReach(lane: Lane, channel: Channel, budget: AdBudget): Double {
    var r = channel.reach * channel.fit(lane) * budget.reach * (1 + 0.04 * min(10.0, staffing(lane).marketing))
    if (channel == Channel.INFLUENCERS && world.era == Era.CREATOR_ECONOMY) r *= 1.3
    if (channel == Channel.PR && world.era == Era.PRIVACY_PANIC) r *= 1.2
    return r
}

/** Where awareness would end up after a campaign, if nothing else changed. */
fun GameState.campaignForecast(lane: Lane, channel: Channel, budget: AdBudget, weeks: Int): Double {
    val natural = naturalAwareness(lane)
    var b = marketing.boost[lane] ?: 0.0
    val r = campaignReach(lane, channel, budget)
    repeat(weeks) { b = (b + r * max(0.0, 1 - natural - b)) * 0.995 }
    return natural + b
}

fun GameState.campaignBlocker(lane: Lane, channel: Channel, budget: AdBudget, weeks: Int): String? {
    if (lane !in lanes) return "You don't run that business."
    if (campaignFor(lane) != null) return "A campaign is already running."
    if (channel.since > calendarYear) return "${channel.title} don't exist until ${channel.since}."
    val total = campaignCost(channel, budget) * weeks
    if (cash < total * 0.5) return "It costs ${money(total)} in all. Keep at least half of that in the bank."
    return null
}

fun GameState.startCampaign(lane: Lane, channel: Channel, budget: AdBudget, weeks: Int) {
    requireLive()
    campaignBlocker(lane, channel, budget, weeks)?.let { throw notAvailable(it) }
    if (weeks !in CampaignLengths) throw invalid("Pick 4, 8 or 13 weeks.")
    marketing.campaigns += AdCampaign(lane, channel, budget, weeks, campaignCost(channel, budget), week, awareness(lane))
    addLog(
        "${channel.title} for ${lane.businessName.lowercase()}: ${budget.title.lowercase()} budget, $weeks weeks, ${money(campaignCost(channel, budget))} a week.",
        LogTone.NEUTRAL, lane,
    )
}

/** Stop a campaign early. What's spent is spent. */
fun GameState.stopCampaign(lane: Lane) {
    val c = campaignFor(lane) ?: throw notAvailable("No campaign is running.")
    finish(c)
}

private fun GameState.finish(c: AdCampaign) {
    marketing.campaigns.remove(c)
    val now = awareness(c.lane)
    marketing.results += CampaignResult(c.lane, c.channel, c.budget, c.weeks - c.weeksLeft, week, c.spent, c.startAwareness, now, c.earned)
    if (marketing.results.size > 12) marketing.results.removeAt(0)
    addLog(
        "${c.channel.title} for ${c.lane.businessName.lowercase()} ended: awareness ${Format.percent(c.startAwareness)} → ${Format.percent(now)}. " +
            "Spent ${money(c.spent)}, brought in about ${money(c.earned)} so far.",
        if (c.earned >= c.spent) LogTone.GOOD else LogTone.NEUTRAL, c.lane,
    )
}

/** Each week: campaigns spend and reach, awareness settles, and the extra revenue is counted. */
internal fun GameState.marketingWeek() {
    for (lane in lanes) {
        val natural = naturalAwareness(lane)
        var b = marketing.boost[lane] ?: 0.0
        val c = campaignFor(lane)
        if (c != null) {
            spend(c.weeklyCost, lane)
            c.spent += c.weeklyCost
            b += campaignReach(lane, c.channel, c.budget) * max(0.0, 1 - natural - b)
            if (c.channel == Channel.PR) reputation.adjust(0.08 * c.budget.reach)
        }
        // Without a campaign, people slowly forget.
        b *= if (c == null) 0.975 else 0.995
        if (b < 0.002) marketing.boost.remove(lane) else marketing.boost[lane] = b.clamped(0.0, 0.9)

        // Credit the extra demand with the revenue it brought, roughly.
        val lift = brandLift(lane)
        val extra = max(0.0, ledger.lastOrNull()?.revenue?.get(lane) ?: 0.0) * (1 - 1 / lift)
        if (c != null) c.earned += extra
        else marketing.results.lastOrNull { it.lane == lane && week - it.endWeek <= 26 }?.let { it.earned += extra }
        if (c != null) {
            c.weeksLeft -= 1
            if (c.weeksLeft <= 0) finish(c)
        }
    }
    marketing.boost.keys.retainAll(lanes.toSet())
    marketing.campaigns.removeAll { it.lane !in lanes }
}
