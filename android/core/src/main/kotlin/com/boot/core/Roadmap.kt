package com.boot.core

import kotlinx.serialization.Serializable

/*
 * Product roadmaps. Every business but the game studio (which has research, engines and its own
 * catalogue) has a short list of things it can build: features for the app, tools for the agency,
 * a feed for the social platform, a chip for the devices, an API for the models. Each costs money
 * up front and a few weeks of the team's time, during which it gets a little less done, and then
 * helps for good. Some come with a catch. One build per business at a time.
 */

@Serializable
enum class Feature(
    val lane: Lane,
    val title: String,
    val detail: String,
    val cost: Double,
    val weeks: Int,
    internal val effect: LaneModifiers.() -> Unit,
) {
    // Web agency
    CASE_STUDIES(Lane.WEBSITES, "Case studies", "Write up your best work. Better clients come looking: 5% more demand.", 15_000.0, 4, { demand *= 1.05 }),
    DESIGN_SYSTEM(Lane.WEBSITES, "Design system", "Reusable components. Every build goes 6% faster.", 40_000.0, 6, { output *= 1.06 }),
    RETAINERS(Lane.WEBSITES, "Retainer packages", "Monthly plans keep clients coming back: a little more demand, 5% lower costs.", 30_000.0, 5, { demand *= 1.03; cost *= 0.95 }),
    OFFSHORE(Lane.WEBSITES, "Offshore partners", "A partner shop takes the overflow: 12% more output, but some clients notice (4% less demand).", 60_000.0, 6, { output *= 1.12; demand *= 0.96 }),

    // Apps
    ONBOARDING(Lane.APPS, "Guided onboarding", "New users learn the app in a minute. 4% less churn.", 20_000.0, 4, { churn *= 0.96 }),
    DARK_MODE(Lane.APPS, "Dark mode and polish", "The thing every review asks for. Better ratings.", 15_000.0, 3, { quality += 3 }),
    REFERRALS(Lane.APPS, "Referral program", "Users bring their friends: 6% more installs, at a small cost per reward.", 35_000.0, 5, { demand *= 1.06; cost *= 1.03 }),
    OFFLINE_MODE(Lane.APPS, "Offline mode", "Works on the subway. 5% less churn, a little more to run.", 50_000.0, 6, { churn *= 0.95; cost *= 1.05 }),

    // Social
    STORIES(Lane.SOCIAL, "Stories", "Posts that vanish in a day. 3% faster growth, and a little more mess to moderate.", 60_000.0, 6, { growth *= 1.03; toxicity += 0.0005 }),
    ALGO_FEED(Lane.SOCIAL, "Algorithmic feed", "Show people what keeps them scrolling: 8% more ad demand, but toxicity creeps up.", 120_000.0, 8, { demand *= 1.08; toxicity += 0.001 }),
    COMMUNITY_NOTES(Lane.SOCIAL, "Community notes", "Users add context to misleading posts. Toxicity falls faster.", 80_000.0, 6, { toxicity -= 0.0015 }),
    LIVE_VIDEO(Lane.SOCIAL, "Live video", "Go live to your followers. 4% faster growth; streaming isn't cheap.", 150_000.0, 8, { growth *= 1.04; cost *= 1.06 }),

    // Hardware
    DROP_TEST_LAB(Lane.HARDWARE, "Drop-test lab", "Break them before customers do. 30% fewer recalls.", 80_000.0, 6, { risk *= 0.7 }),
    IN_HOUSE_CHIP(Lane.HARDWARE, "In-house chip", "Your own silicon: better devices and slightly cheaper parts.", 600_000.0, 16, { quality += 4; cost *= 0.97 }),
    RETAIL_PARTNERS(Lane.HARDWARE, "Retail partners", "On the shelf at every big store. 6% more demand.", 250_000.0, 8, { demand *= 1.06 }),
    REPAIR_PROGRAM(Lane.HARDWARE, "Repair program", "Fix it instead of replacing it. Better reviews and fewer recalls.", 120_000.0, 6, { quality += 2; risk *= 0.85 }),

    // AI
    INFERENCE_OPT(Lane.AI, "Inference optimization", "Squeeze more out of every GPU. Compute bills drop 5%.", 200_000.0, 6, { computeCost *= 0.95 }),
    DATA_DEALS(Lane.AI, "Data deals", "License archives and forums. Training runs gain 5% more, and regulators take an interest.", 500_000.0, 8, { gain *= 1.05; risk *= 1.1 }),
    RED_TEAM(Lane.AI, "Red team", "People paid to break your models first. 15% less trouble with regulators.", 150_000.0, 6, { risk *= 0.85 }),
    ENTERPRISE_API(Lane.AI, "Enterprise API", "Contracts, SLAs and a sales team. 4% more demand.", 300_000.0, 8, { demand *= 1.04 }),

    // Platform Lab / OS
    DRIVER_CERT(Lane.OS, "Driver certification", "OEMs trust your board support: 5% more demand, fewer recalls.", 120_000.0, 6, { demand *= 1.05; risk *= 0.9 }),
    UPDATE_PIPELINE(Lane.OS, "Update pipeline", "Seamless OTA keeps devices current: better quality, 4% less risk.", 180_000.0, 7, { quality += 3; risk *= 0.96 }),
    APP_STORE_KIT(Lane.OS, "App store kit", "Hosted storefront for third-party apps: 6% more demand.", 250_000.0, 8, { demand *= 1.06 }),
    ENTERPRISE_MDM(Lane.OS, "Enterprise MDM", "Manage fleets for companies: 5% more demand, a little more to run.", 200_000.0, 6, { demand *= 1.05; cost *= 1.03 }),
    ;

    companion object {
        fun forLane(lane: Lane): List<Feature> = entries.filter { it.lane == lane }
    }
}

/** A feature being built. */
@Serializable
data class FeatureBuild(val feature: Feature, var weeksLeft: Int)

/** While a team builds a feature, it gets this much of its usual work done. */
const val BUILD_DRAG = 0.85

internal fun GameState.applyRoadmap(lane: Lane, mods: LaneModifiers) {
    for (f in features) if (f.lane == lane) f.effect(mods)
    if (building.any { it.feature.lane == lane }) mods.output *= BUILD_DRAG
}

/** Why [f] can't be built now, or null if it can. */
fun GameState.featureBlocker(f: Feature): String? {
    if (f.lane !in lanes) return "You don't run ${Format.a(f.lane.businessName.lowercase())}."
    if (f in features) return "Already built."
    building.firstOrNull { it.feature.lane == f.lane }?.let { return "The team is building ${it.feature.title.lowercase()}." }
    if (staff(f.lane).isEmpty()) return "Needs at least one person on the ${f.lane.businessName.lowercase()}."
    if (cash < f.cost) return "Costs ${money(f.cost)}."
    return null
}

fun GameState.buildFeature(f: Feature) {
    requireLive()
    featureBlocker(f)?.let { throw notAvailable(it) }
    spend(f.cost, f.lane)
    building = building + FeatureBuild(f, f.weeks)
    addLog("The ${f.lane.businessName.lowercase()} team started on ${f.title.lowercase()}. Ready in ${Format.weeks(f.weeks)}.", LogTone.NEUTRAL, f.lane)
}

internal fun GameState.roadmapWeek() {
    if (building.isEmpty()) return
    val done = mutableListOf<FeatureBuild>()
    for (b in building) {
        if (b.feature.lane !in lanes) {
            done += b
            continue
        }
        b.weeksLeft -= 1
        if (b.weeksLeft <= 0) {
            done += b
            features = features + b.feature
            addLog("${b.feature.title} is live. ${b.feature.detail}", LogTone.GOOD, b.feature.lane)
        }
    }
    building = building - done.toSet()
}
