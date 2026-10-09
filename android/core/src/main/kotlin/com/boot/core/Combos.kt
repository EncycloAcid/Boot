package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.min
import kotlin.math.pow

/**
 * Every pairing of two lanes unlocks a crossover product only that combination gets,
 * along with a problem unique to it.
 */
@Serializable
enum class Combo(val first: Lane, val second: Lane) {
    GAMES_SOCIAL(Lane.GAMES, Lane.SOCIAL),
    WEBSITES_APPS(Lane.WEBSITES, Lane.APPS),
    APPS_GAMES(Lane.APPS, Lane.GAMES),
    HARDWARE_APPS(Lane.HARDWARE, Lane.APPS),
    HARDWARE_AI(Lane.HARDWARE, Lane.AI),
    AI_SOCIAL(Lane.AI, Lane.SOCIAL),
    AI_GAMES(Lane.AI, Lane.GAMES),
    GAMES_WEBSITES(Lane.GAMES, Lane.WEBSITES),
    GAMES_HARDWARE(Lane.GAMES, Lane.HARDWARE),
    WEBSITES_SOCIAL(Lane.WEBSITES, Lane.SOCIAL),
    WEBSITES_HARDWARE(Lane.WEBSITES, Lane.HARDWARE),
    WEBSITES_AI(Lane.WEBSITES, Lane.AI),
    APPS_SOCIAL(Lane.APPS, Lane.SOCIAL),
    APPS_AI(Lane.APPS, Lane.AI),
    SOCIAL_HARDWARE(Lane.SOCIAL, Lane.HARDWARE);

    fun involves(lane: Lane) = first == lane || second == lane

    val product: String
        get() = when (this) {
            GAMES_SOCIAL -> "Trending Hub"
            WEBSITES_APPS -> "Client Tools Suite"
            APPS_GAMES -> "Free-to-Play Mobile"
            HARDWARE_APPS -> "Walled Garden"
            HARDWARE_AI -> "Custom Silicon"
            AI_SOCIAL -> "The Algorithm"
            AI_GAMES -> "Living NPCs"
            GAMES_WEBSITES -> "Co-Dev Studio"
            GAMES_HARDWARE -> "Handheld Console"
            WEBSITES_SOCIAL -> "Creator Storefronts"
            WEBSITES_HARDWARE -> "Smart Retail Installs"
            WEBSITES_AI -> "Autopilot Agency"
            APPS_SOCIAL -> "Super App"
            APPS_AI -> "Assistant App"
            SOCIAL_HARDWARE -> "Smart Glasses"
        }

    val pitch: String
        get() = when (this) {
            GAMES_SOCIAL -> "Your platform becomes where your games trend. Launches ride the feed."
            WEBSITES_APPS -> "Turn the tools you built for clients into your own software product."
            APPS_GAMES -> "Mobile money is huge. How hard you squeeze players is up to you."
            HARDWARE_APPS -> "Your apps run best on your devices. Customers get locked in."
            HARDWARE_AI -> "Design your own chips. No more GPU shortage, and your devices get smarter."
            AI_SOCIAL -> "A recommendation engine that makes the feed irresistible."
            AI_GAMES -> "NPCs that actually talk back. A huge selling point."
            GAMES_WEBSITES -> "Your agency takes on game marketing sites and co-development work at premium rates."
            GAMES_HARDWARE -> "Your own handheld. Your games sell devices; your devices sell games."
            WEBSITES_SOCIAL -> "Sell storefront sites to the creators on your platform."
            WEBSITES_HARDWARE -> "Bundle your devices into kiosk and retail installs for agency clients."
            WEBSITES_AI -> "Let the model build the sites. Margins soar."
            APPS_SOCIAL -> "Log in with your social account, share everything, never leave."
            APPS_AI -> "An assistant baked into your app. People stop leaving."
            SOCIAL_HARDWARE -> "Glasses that post what you see. Social, hands-free."
        }

    val problem: String
        get() = when (this) {
            GAMES_SOCIAL -> "Your gamer audience floods the site and clashes with everyone else."
            WEBSITES_APPS -> "Your clients notice you're now competing with them."
            APPS_GAMES -> "Squeeze players too hard and your reputation tanks across every lane."
            HARDWARE_APPS -> "Too much lock-in brings antitrust lawsuits."
            HARDWARE_AI -> "The chip R&D bill is brutal."
            AI_SOCIAL -> "Engagement soars, and so do the congressional hearings."
            AI_GAMES -> "Sooner or later a player gets an NPC to say something that goes viral for the wrong reasons."
            GAMES_WEBSITES -> "Agency deadlines collide with launch crunch."
            GAMES_HARDWARE -> "Fans resent the exclusives."
            WEBSITES_SOCIAL -> "Creators accuse you of rigging the platform for paying customers."
            WEBSITES_HARDWARE -> "Every install is a support ticket waiting to happen."
            WEBSITES_AI -> "You're automating your own staff, and they know it."
            APPS_SOCIAL -> "All that sharing is one leak away from a privacy scandal."
            APPS_AI -> "Inference costs scale with every user."
            SOCIAL_HARDWARE -> "People don't love being recorded by strangers' glasses."
        }

    /** Headline when the problem lands. */
    val headline: String
        get() = when (this) {
            GAMES_SOCIAL -> "Gamers have flooded the platform"
            WEBSITES_APPS -> "Your clients noticed"
            APPS_GAMES -> "Players say you're squeezing them"
            HARDWARE_APPS -> "Antitrust lawsuit filed"
            HARDWARE_AI -> "The chip program is over budget"
            AI_SOCIAL -> "Called to testify about the algorithm"
            AI_GAMES -> "An NPC said what?"
            GAMES_WEBSITES -> "Deadlines collide"
            GAMES_HARDWARE -> "Exclusivity backlash"
            WEBSITES_SOCIAL -> "Creators cry favoritism"
            WEBSITES_HARDWARE -> "Install support is drowning"
            WEBSITES_AI -> "The team knows what Autopilot means"
            APPS_SOCIAL -> "Data-sharing leak"
            APPS_AI -> "Inference bill shock"
            SOCIAL_HARDWARE -> "Your glasses are getting banned"
        }

    /** What the intensity dial controls for this combo. */
    val dial: String
        get() = when (this) {
            GAMES_SOCIAL -> "Cross-promotion"
            WEBSITES_APPS -> "Product push"
            APPS_GAMES -> "Monetization pressure"
            HARDWARE_APPS -> "Lock-in"
            HARDWARE_AI -> "Chip program scale"
            AI_SOCIAL -> "Algorithm aggressiveness"
            AI_GAMES -> "NPC freedom"
            GAMES_WEBSITES -> "Co-dev workload"
            GAMES_HARDWARE -> "Exclusivity"
            WEBSITES_SOCIAL -> "Storefront promotion"
            WEBSITES_HARDWARE -> "Install volume"
            WEBSITES_AI -> "Automation"
            APPS_SOCIAL -> "Data sharing"
            APPS_AI -> "Assistant reach"
            SOCIAL_HARDWARE -> "Always-on capture"
        }

    val buildCost: Double
        get() = when (this) {
            HARDWARE_AI -> 2_000_000.0
            GAMES_HARDWARE, SOCIAL_HARDWARE, HARDWARE_APPS -> 600_000.0
            AI_SOCIAL, AI_GAMES, APPS_AI, WEBSITES_AI -> 250_000.0
            else -> 60_000.0
        }

    val buildWeeks: Int
        get() = when (this) {
            HARDWARE_AI -> 26
            GAMES_HARDWARE, SOCIAL_HARDWARE -> 20
            HARDWARE_APPS, AI_SOCIAL, AI_GAMES, APPS_AI -> 12
            else -> 8
        }

    /** Scar left if you ride out this combo's problem instead of fixing it. */
    internal val scar: ScarKind?
        get() = when (this) {
            APPS_GAMES -> ScarKind.PREDATORY_MONETIZATION
            HARDWARE_APPS -> ScarKind.ANTITRUST
            AI_SOCIAL, AI_GAMES, GAMES_SOCIAL, WEBSITES_SOCIAL -> ScarKind.SCANDAL
            APPS_SOCIAL, SOCIAL_HARDWARE -> ScarKind.PRIVACY
            WEBSITES_AI -> ScarKind.LAYOFFS
            else -> null
        }

    companion object {
        fun between(a: Lane, b: Lane): Combo? =
            entries.firstOrNull { (it.first == a && it.second == b) || (it.first == b && it.second == a) }

        fun available(lanes: List<Lane>): List<Combo> = entries.filter { it.first in lanes && it.second in lanes }
    }
}

@Serializable
class Crossover(
    val combo: Combo,
    var buildWeeksLeft: Int,
    var built: Boolean = false,
    /** 0...1, player controlled. More upside, more trouble. */
    var intensity: Double = 0.5,
    /** Builds toward 1, then the combo's problem fires. */
    var pressure: Double = 0.0,
    var incidents: Int = 0,
) {
    /** Not started: the player hasn't paid to build it yet. */
    val isIdle: Boolean get() = !built && buildWeeksLeft == combo.buildWeeks
    val isBuilding: Boolean get() = !built && buildWeeksLeft < combo.buildWeeks
}

/** What the active combos do to the company this week. */
internal class ComboEffects {
    val mods = mutableMapOf<Lane, LaneModifiers>()
    var revenue = 0.0
    var cost = 0.0
    var moraleDrain = 0.0
    var webWariness = 0.0

    fun mod(lane: Lane, change: LaneModifiers.() -> Unit) {
        mods.getOrPut(lane) { LaneModifiers() }.change()
    }
}

/** Weekly effect of a built crossover at its current intensity. */
internal fun Crossover.applyTo(fx: ComboEffects, state: GameState) {
    if (!built) return
    val i = intensity
    when (combo) {
        Combo.GAMES_SOCIAL -> {
            val reach = min(1.0, (state.social?.realUsers ?: 0.0) / 5_000_000)
            fx.mod(Lane.GAMES) { demand *= 1 + i * (0.2 + 0.8 * reach) }
            fx.mod(Lane.SOCIAL) { growth *= 1 + 0.25 * i; toxicity += 0.004 * i }
        }
        Combo.WEBSITES_APPS -> {
            val clients = (state.web?.completed ?: 0).toDouble()
            fx.revenue += clients * 60 * i
            fx.mod(Lane.APPS) { demand *= 1 + 0.3 * i }
            fx.webWariness = 0.6 * i
        }
        Combo.APPS_GAMES -> {
            fx.mod(Lane.APPS) { demand *= 1 + 0.4 * i; churn *= 1 + 0.4 * i * i }
            fx.mod(Lane.GAMES) { demand *= 1 + 0.6 * i }
            fx.revenue += ((state.apps?.users ?: 0.0) * 0.05 + (state.games?.fans ?: 0.0) * 0.4) * i * (1 + i)
        }
        Combo.HARDWARE_APPS -> {
            fx.mod(Lane.HARDWARE) { demand *= 1 + 0.4 * i }
            fx.mod(Lane.APPS) { churn *= 1 - 0.35 * i }
        }
        Combo.HARDWARE_AI -> {
            fx.mod(Lane.AI) { computeCost *= 1 - 0.6 * i }
            fx.mod(Lane.HARDWARE) { quality += 10 * i; demand *= 1 + 0.15 * i }
            fx.cost += 150_000 * i
        }
        Combo.AI_SOCIAL -> fx.mod(Lane.SOCIAL) { growth *= 1 + 0.8 * i; demand *= 1 + 0.3 * i; toxicity += 0.003 * i }
        Combo.AI_GAMES -> fx.mod(Lane.GAMES) { quality += 8 * i; demand *= 1 + 0.2 * i }
        Combo.GAMES_WEBSITES -> {
            fx.mod(Lane.WEBSITES) { demand *= 1 + 0.5 * i }
            fx.moraleDrain += 0.008 * i
        }
        Combo.GAMES_HARDWARE -> {
            val fans = min(1.0, (state.games?.fans ?: 0.0) / 200_000)
            fx.mod(Lane.HARDWARE) { demand *= 1 + i * (0.1 + 0.4 * fans) }
            fx.mod(Lane.GAMES) { demand *= 1 + 0.3 * i }
        }
        Combo.WEBSITES_SOCIAL -> {
            fx.revenue += (state.social?.realUsers ?: 0.0) * 0.0006 * i
            fx.mod(Lane.WEBSITES) { demand *= 1 + 0.2 * i }
        }
        Combo.WEBSITES_HARDWARE -> {
            fx.mod(Lane.WEBSITES) { demand *= 1 + 0.6 * i }
            fx.mod(Lane.HARDWARE) { demand *= 1 + 0.1 * i }
            fx.cost += 4_000 * i
        }
        Combo.WEBSITES_AI -> {
            fx.mod(Lane.WEBSITES) { output *= 1 + 1.2 * i }
            fx.moraleDrain += 0.012 * i
        }
        Combo.APPS_SOCIAL -> {
            val reach = min(1.0, (state.social?.realUsers ?: 0.0) / 10_000_000)
            fx.mod(Lane.APPS) { demand *= 1 + i * (0.2 + 0.6 * reach) }
            fx.mod(Lane.SOCIAL) { growth *= 1 + 0.15 * i }
        }
        Combo.APPS_AI -> {
            fx.mod(Lane.APPS) { churn *= 1 - 0.35 * i; quality += 4 * i }
            fx.cost += (state.apps?.users ?: 0.0) * 0.01 * i
        }
        Combo.SOCIAL_HARDWARE -> {
            fx.mod(Lane.HARDWARE) { demand *= 1 + 0.3 * i }
            fx.mod(Lane.SOCIAL) { growth *= 1 + 0.2 * i }
        }
    }
}

/** Weekly pressure toward this combo's problem. */
internal fun Crossover.pressureRate(state: GameState): Double {
    val base = 0.004
    // Gamers flood your platform whether or not you built the hub. Other problems need the crossover.
    if (!built) return if (combo == Combo.GAMES_SOCIAL) base else 0.0
    val i = intensity
    return when (combo) {
        Combo.APPS_GAMES -> base + 0.06 * i.pow(3)
        Combo.HARDWARE_APPS -> base + 0.02 * i * i
        Combo.AI_SOCIAL -> base + 0.03 * i * min(1.0, (state.social?.users ?: 0.0) / 20_000_000 + 0.2)
        Combo.HARDWARE_AI -> base + 0.006 * i
        Combo.WEBSITES_AI -> base + 0.03 * i
        else -> base + 0.02 * i * i
    }
}
