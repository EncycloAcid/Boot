package com.boot.core

import kotlinx.serialization.Serializable

/** Parody industry eras that boost or weaken different lanes. */
@Serializable
enum class Era {
    BOOTSTRAP_YEARS,
    AI_GOLD_RUSH,
    VR_HYPE_CYCLE,
    CRYPTO_WINTER,
    REMOTE_WORK_BOOM,
    PRIVACY_PANIC,
    CREATOR_ECONOMY,
    RETRO_REVIVAL,
    METAVERSE_HANGOVER;

    val title: String
        get() = when (this) {
            BOOTSTRAP_YEARS -> "The Bootstrap Years"
            AI_GOLD_RUSH -> "The AI Gold Rush"
            VR_HYPE_CYCLE -> "The VR Hype Cycle"
            CRYPTO_WINTER -> "Crypto Winter"
            REMOTE_WORK_BOOM -> "The Remote Work Boom"
            PRIVACY_PANIC -> "The Privacy Panic"
            CREATOR_ECONOMY -> "The Creator Economy"
            RETRO_REVIVAL -> "The Retro Revival"
            METAVERSE_HANGOVER -> "The Metaverse Hangover"
        }

    val blurb: String
        get() = when (this) {
            BOOTSTRAP_YEARS -> "Nobody's watching yet. Rent is due."
            AI_GOLD_RUSH -> "Every pitch deck says 'agentic'. GPUs cost more than houses. Clients want chatbots on their bakery sites."
            VR_HYPE_CYCLE -> "Headsets are the future, again. Gamers and gadget buyers are throwing money around."
            CRYPTO_WINTER -> "The coins crashed. Investors are skittish, ad budgets shrank, and miners are dumping cheap GPUs."
            REMOTE_WORK_BOOM -> "Everyone needs a website, a productivity app and a better laptop."
            PRIVACY_PANIC -> "Regulators and journalists are hunting for data scandals. Social and AI feel the heat."
            CREATOR_ECONOMY -> "Everyone's a creator. Platforms and storefronts boom."
            RETRO_REVIVAL -> "Pixel art is back. Small games sell; shiny gadgets less so."
            METAVERSE_HANGOVER -> "Remember the metaverse? Investors do, unfortunately."
        }

    /** Demand multiplier per lane. */
    fun demand(lane: Lane): Double = when (this) {
        BOOTSTRAP_YEARS -> 1.0
        AI_GOLD_RUSH -> when (lane) {
            Lane.AI -> 1.7; Lane.APPS -> 1.1; Lane.WEBSITES -> 0.9; else -> 1.0
        }
        VR_HYPE_CYCLE -> when (lane) {
            Lane.HARDWARE -> 1.35; Lane.GAMES -> 1.2; else -> 1.0
        }
        CRYPTO_WINTER -> when (lane) {
            Lane.SOCIAL -> 0.8; Lane.APPS -> 0.85; Lane.AI -> 0.9; else -> 0.95
        }
        REMOTE_WORK_BOOM -> when (lane) {
            Lane.WEBSITES -> 1.3; Lane.APPS -> 1.2; Lane.HARDWARE -> 1.2; else -> 1.0
        }
        PRIVACY_PANIC -> when (lane) {
            Lane.SOCIAL -> 0.75; Lane.AI -> 0.85; Lane.HARDWARE -> 1.1; else -> 1.0
        }
        CREATOR_ECONOMY -> when (lane) {
            Lane.SOCIAL -> 1.35; Lane.WEBSITES -> 1.1; Lane.GAMES -> 1.1; else -> 1.0
        }
        RETRO_REVIVAL -> when (lane) {
            Lane.GAMES -> 1.3; Lane.HARDWARE -> 0.9; else -> 1.0
        }
        METAVERSE_HANGOVER -> when (lane) {
            Lane.HARDWARE -> 0.8; Lane.SOCIAL -> 0.9; Lane.AI -> 1.1; else -> 0.95
        }
    }

    /** Multiplier on what GPUs cost an AI lab. */
    val gpuPrice: Double
        get() = when (this) {
            AI_GOLD_RUSH -> 1.8
            CRYPTO_WINTER -> 0.6
            VR_HYPE_CYCLE -> 1.1
            else -> 1.0
        }

    /** How willing investors are to write cheques. */
    val investorAppetite: Double
        get() = when (this) {
            AI_GOLD_RUSH -> 1.5
            VR_HYPE_CYCLE -> 1.2
            CRYPTO_WINTER -> 0.6
            METAVERSE_HANGOVER -> 0.7
            PRIVACY_PANIC -> 0.9
            else -> 1.0
        }

    /** Lanes investors are especially hot on this era. */
    val hotLanes: List<Lane> get() = Lane.entries.filter { demand(it) >= 1.2 }

    val coldLanes: List<Lane> get() = Lane.entries.filter { demand(it) <= 0.85 }
}

@Serializable
data class Rival(
    val id: Int,
    var name: String,
    val lane: Lane,
    /** 0...1 market muscle in their lane. */
    var strength: Double,
    /** 0...1 how often they poach and bid for talent. */
    val aggression: Double,
    var lastMove: String? = null,
)

@Serializable
class World {
    var era = Era.BOOTSTRAP_YEARS
    var eraStartWeek = 0
    var eraLength = 104
    val pastEras = mutableListOf<Era>()
    val rivals = mutableListOf<Rival>()

    companion object {
        internal fun makeRivals() = listOf(
            Rival(1, "Electronic Arts", Lane.GAMES, strength = 0.5, aggression = 0.5),
            Rival(2, "Squarespace", Lane.WEBSITES, strength = 0.4, aggression = 0.3),
            Rival(3, "Google", Lane.APPS, strength = 0.5, aggression = 0.4),
            Rival(4, "Meta", Lane.SOCIAL, strength = 0.6, aggression = 0.5),
            Rival(5, "Apple", Lane.HARDWARE, strength = 0.7, aggression = 0.4),
            Rival(6, "OpenAI", Lane.AI, strength = 0.6, aggression = 0.8),
        )

        /** The other big names in each lane, for the news. The main rival is one of them. */
        fun peers(lane: Lane): List<String> = when (lane) {
            Lane.GAMES -> listOf("Electronic Arts", "Ubisoft", "Activision Blizzard", "Take-Two", "Nintendo", "Sony Interactive")
            Lane.WEBSITES -> listOf("Squarespace", "Wix", "Webflow", "WordPress.com", "Shopify")
            Lane.APPS -> listOf("Google", "Duolingo", "Spotify", "Calm", "Strava", "Notion")
            Lane.SOCIAL -> listOf("Meta", "TikTok", "X", "Snap", "Reddit", "Discord")
            Lane.HARDWARE -> listOf("Apple", "Samsung", "Google", "Sony", "Nothing", "Microsoft")
            Lane.AI -> listOf("OpenAI", "Google DeepMind", "Anthropic", "Meta AI", "xAI", "Mistral AI")
        }

        /** Earlier versions used made-up rivals; saves from then get the real ones. */
        internal val renamed = mapOf(
            "Gigaplay Interactive" to "Electronic Arts",
            "Pixelsmith Collective" to "Squarespace",
            "Appetite Inc" to "Google",
            "Fritter" to "Meta",
            "Pear Computing" to "Apple",
            "Hyperion Labs" to "OpenAI",
        )
    }

    /** Demand multiplier from rival competition in a lane. */
    fun competition(lane: Lane): Double {
        val s = rivals.filter { it.lane == lane }.maxOfOrNull { it.strength } ?: 0.0
        return 1 - 0.2 * s
    }

    fun rival(lane: Lane): Rival? = rivals.firstOrNull { it.lane == lane }

}
