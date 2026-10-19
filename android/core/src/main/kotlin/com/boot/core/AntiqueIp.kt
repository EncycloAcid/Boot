package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong

/*
 * Antique IP archaeology: buy dormant brands and revive them as remakes or reboots.
 * Extends Buying / OwnedStudios / remasters — not a second acquisition lane. Era-aware
 * catalogue; acquisition cost; remake (faithful) or reboot (new take); critical hit or meme flop.
 */

/** A dead brand / defunct IP sitting in the vaults, waiting for someone to revive it. */
@Serializable
data class DormantIp(
    val id: String,
    val brand: String,
    val blurb: String,
    /** Last year it was a going concern — available to buy after this. */
    val diedYear: Int,
    /** When the brand was born, for the catalogue era filter. */
    val bornYear: Int,
    val topic: Topic,
    val genre: Genre,
    /** How fondly it's remembered, 0…1 — lifts remake reception. */
    val qualityMemory: Double,
    /** Chance the revival becomes a meme flop instead of a hit, 0…1. */
    val memeRisk: Double,
    /** Base acquisition cost before era / demand scaling. */
    val baseCost: Double,
)

/** An IP you've bought. Remakes and reboots ship under your studio. */
@Serializable
data class OwnedIp(
    val id: String,
    val boughtWeek: Int,
    val paid: Double,
    var remakes: Int = 0,
    var reboots: Int = 0,
    var lastShipWeek: Int = -1,
    var lastScore: Double = -1.0,
    /** Last revival landed as a meme flop. */
    var memeFlop: Boolean = false,
)

/** Remake = faithful rebuild (half-ish work). Reboot = new take (near full work). */
@Serializable
enum class IpPath(val title: String, val blurb: String, val work: Double, val hype: Double) {
    REMAKE("Remake", "Faithful rebuild for today. Safer with fans; critics yawn if it's too safe.", 0.55, 12.0),
    REBOOT("Reboot", "New take on the brand. Bigger upside — and meme risk.", 0.9, 18.0),
}

/** Catalogue of dormant brands. Filtered by calendar year so old saves and decades stay era-aware. */
val DormantIps: List<DormantIp> = listOf(
    DormantIp("zork", "Zork", "Infocom's text labyrinth. The cave still echoes.", 1989, 1980, Topic.FANTASY, Genre.ADVENTURE, 0.88, 0.35, 4_000_000.0),
    DormantIp("kings-quest", "King's Quest", "Sierra's fairy-tale quests. Pixels and parsers.", 2004, 1984, Topic.FANTASY, Genre.ADVENTURE, 0.72, 0.4, 8_000_000.0),
    DormantIp("space-quest", "Space Quest", "Roger Wilco's slapstick sci-fi. Death by dumbness.", 2006, 1986, Topic.SCI_FI, Genre.ADVENTURE, 0.65, 0.55, 5_000_000.0),
    DormantIp("system-shock", "System Shock", "Looking Glass cyber-horror. Hack the deck.", 2000, 1994, Topic.CYBERPUNK, Genre.ACTION, 0.92, 0.25, 12_000_000.0),
    DormantIp("thief", "Thief", "Looking Glass stealth. Stay in the shadows.", 2004, 1998, Topic.MEDIEVAL, Genre.ACTION, 0.9, 0.3, 14_000_000.0),
    DormantIp("ultima", "Ultima", "Origin's open-world ethics. Avatar's path.", 1999, 1981, Topic.FANTASY, Genre.RPG, 0.85, 0.35, 18_000_000.0),
    DormantIp("wing-commander", "Wing Commander", "Space opera dogfights. Mark Hamill era.", 2007, 1990, Topic.SCI_FI, Genre.ACTION, 0.7, 0.45, 10_000_000.0),
    DormantIp("syndicate", "Syndicate", "Bullfrog corporate dystopia. Mind-control agents.", 2001, 1993, Topic.CYBERPUNK, Genre.STRATEGY, 0.78, 0.4, 9_000_000.0),
    DormantIp("dungeon-keeper", "Dungeon Keeper", "Bullfrog's evil landlord sim. Slap the imps.", 1999, 1997, Topic.FANTASY, Genre.STRATEGY, 0.82, 0.5, 11_000_000.0),
    DormantIp("theme-park", "Theme Park", "Bullfrog rollercoasters and vomit paths.", 2001, 1994, Topic.CITY, Genre.SIMULATION, 0.68, 0.45, 7_000_000.0),
    DormantIp("lemmings", "Lemmings", "DMA Design's suicidal puzzle march.", 2006, 1991, Topic.CITY, Genre.PUZZLE, 0.8, 0.55, 6_000_000.0),
    DormantIp("banjo", "Banjo-Kazooie", "Rare's bear-and-bird platformer. Mumbo's waiting.", 2008, 1998, Topic.FANTASY, Genre.PLATFORMER, 0.88, 0.35, 22_000_000.0),
    DormantIp("conker", "Conker", "Rare's foul-mouthed squirrel. Adult cartoon chaos.", 2005, 2001, Topic.FANTASY, Genre.ACTION, 0.7, 0.65, 9_000_000.0),
    DormantIp("perfect-dark", "Perfect Dark", "Rare spy shooter. Agents and aliens.", 2005, 2000, Topic.SCI_FI, Genre.SHOOTER, 0.8, 0.4, 15_000_000.0),
    DormantIp("mortal-kombat", "Mortal Kombat", "Midway fatalities. Finish him — carefully.", 2009, 1992, Topic.NINJA, Genre.FIGHTING, 0.75, 0.5, 25_000_000.0),
    DormantIp("spyro", "Spyro", "Purple dragon platformer. After the Activision years.", 2012, 1998, Topic.FANTASY, Genre.PLATFORMER, 0.78, 0.35, 20_000_000.0),
    DormantIp("crash", "Crash Bandicoot", "Marsupial mayhem. Spin attack nostalgia.", 2012, 1996, Topic.FANTASY, Genre.PLATFORMER, 0.8, 0.4, 24_000_000.0),
    DormantIp("destroy-all-humans", "Destroy All Humans!", "THQ alien invasion satire.", 2011, 2005, Topic.SCI_FI, Genre.ACTION, 0.62, 0.55, 8_000_000.0),
    DormantIp("saints-row", "Saints Row", "THQ open-world absurdity. Before the reboot wars.", 2013, 2006, Topic.CITY, Genre.ACTION, 0.7, 0.6, 16_000_000.0),
    DormantIp("psychonauts", "Psychonauts", "Double Fine mind-diving. Once orphaned, now legendary.", 2005, 2005, Topic.SCI_FI, Genre.PLATFORMER, 0.9, 0.25, 5_000_000.0),
    DormantIp("beyond-good-evil", "Beyond Good & Evil", "Ubisoft's photojournalist conspiracy. Long silence.", 2003, 2003, Topic.SCI_FI, Genre.ADVENTURE, 0.85, 0.3, 12_000_000.0),
    DormantIp("jet-set-radio", "Jet Set Radio", "Sega graffiti skating. Funky to the end.", 2002, 2000, Topic.CITY, Genre.ACTION, 0.82, 0.45, 7_000_000.0),
    DormantIp("shenmue", "Shenmue", "Sega's forklift odyssey. Capsule toys forever.", 2001, 1999, Topic.CITY, Genre.ADVENTURE, 0.88, 0.5, 14_000_000.0),
    DormantIp("chrono-trigger", "Chrono Trigger", "Square time-travel classic. Handle with care.", 1995, 1995, Topic.SCI_FI, Genre.RPG, 0.98, 0.2, 30_000_000.0),
    DormantIp("phantasy-star", "Phantasy Star", "Sega's space RPG lineage, long fallow.", 2000, 1987, Topic.SCI_FI, Genre.RPG, 0.75, 0.4, 11_000_000.0),
)

fun dormantIp(id: String): DormantIp? = DormantIps.firstOrNull { it.id == id }

/** IPs on the market this year that you don't already own. */
fun GameState.ipsForSale(): List<DormantIp> {
    val g = games ?: return emptyList()
    val owned = g.ownedIps.map { it.id }.toSet()
    return DormantIps.filter { calendarYear > it.diedYear && it.id !in owned && calendarYear >= it.bornYear }
        .sortedBy { acquisitionIpCost(it) }
}

/** What buying [ip] costs today. */
fun GameState.acquisitionIpCost(ip: DormantIp): Double {
    val age = (calendarYear - ip.diedYear).coerceAtLeast(1)
    // Older brands get cheaper until nostalgia spikes after ~15 years.
    val nostalgia = if (age in 10..25) 1.15 else if (age > 30) 0.85 else 1.0
    val demand = world.demand(Lane.GAMES).pow(1.2)
    val price = ip.baseCost * nostalgia * demand * (0.7 + ip.qualityMemory * 0.6)
    return (max(scaled(250_000.0), price) / 1_000).roundToLong() * 1_000.0
}

fun GameState.buyIpBlocker(id: String): String? {
    val g = games ?: return "You need a game studio to buy a brand."
    val ip = dormantIp(id) ?: return "No such brand."
    if (g.ownedIps.any { it.id == id }) return "You already hold ${ip.brand}."
    if (calendarYear <= ip.diedYear) return "${ip.brand} isn't on the market until after ${ip.diedYear}."
    if (listing == null && week < 52 * 2) return "Rights holders want a company with two years behind it."
    val price = acquisitionIpCost(ip)
    if (cash < price) return "Costs ${money(price)}. You have ${money(cash)}."
    return null
}

/** Buys a dormant brand / IP for the vault. */
fun GameState.buyIp(id: String) {
    requireLive()
    buyIpBlocker(id)?.let { throw notAvailable(it) }
    val ip = dormantIp(id)!!
    val price = acquisitionIpCost(ip)
    spend(price, Lane.GAMES)
    games!!.ownedIps += OwnedIp(id, week, price)
    stats.antiqueIps += 1
    reputation.adjust(1.0 + ip.qualityMemory)
    addLog("You bought the ${ip.brand} IP for ${money(price)}. Remake or reboot when you're ready.", LogTone.MILESTONE, Lane.GAMES)
    publish(
        NewsDesk.GAMES, rng.pick(listOf("Kotaku", "Bloomberg", "The Verge")),
        "$companyName acquires ${ip.brand} rights for ${money(price)}",
        "${ip.blurb} Can they revive it?",
        LogTone.GOOD, Lane.GAMES, lead = ip.qualityMemory >= 0.85, tag = NewsTag.MONEY,
    )
    feedOnAntique(ip.brand, "buy")
    museumBeat(LegacyBeatKind.MILESTONE, "Bought ${ip.brand}", "Antique IP for ${money(price)}.")
}

fun GameState.ownedIp(id: String): OwnedIp? = games?.ownedIps?.firstOrNull { it.id == id }

/** Why you can't start a remake/reboot of [id], or null if you can. */
fun GameState.reviveIpBlocker(id: String, path: IpPath, team: Int = 1): String? {
    val g = games ?: return "You don't run a game studio."
    val owned = ownedIp(id) ?: return "You don't own that brand."
    val ip = dormantIp(id) ?: return "No such brand."
    if (ip.topic !in g.topics) return "Research ${ip.topic.title} first."
    if (ip.genre !in g.genres) return "Research the ${ip.genre.lower} genre first."
    if (owned.lastShipWeek >= 0 && week - owned.lastShipWeek < 78) {
        return "Give ${ip.brand} eighteen months before another revival."
    }
    val concept = reviveConcept(id, path)
    return conceptBlocker(concept, team = team)
}

/** A remake or reboot concept for an owned dormant IP. */
fun GameState.reviveConcept(id: String, path: IpPath): GameConcept {
    val ip = dormantIp(id) ?: throw invalid("No such brand.")
    val g = games ?: throw notAvailable("You don't run a game studio.")
    val platforms = platformsOnSale.filter { it != Platform.OWN_CONSOLE }.sortedByDescending { platformPlayers(it) }.take(1)
        .ifEmpty { listOf(Platform.PC).filter { availablePlatform(it) } }
    val size = when {
        ip.qualityMemory >= 0.9 -> GameSize.LARGE
        ip.qualityMemory >= 0.75 -> GameSize.MEDIUM
        else -> GameSize.MEDIUM
    }.let { minOf(it, unlockedSizes.maxOrNull() ?: GameSize.SMALL) }
    val suffix = if (path == IpPath.REMAKE) "Remake" else "Reboot"
    return GameConcept(
        title = "${ip.brand} $suffix",
        topic = ip.topic,
        genre = ip.genre,
        platforms = platforms,
        size = size,
        engineId = g.engines.maxBy { it.level }.id,
        antiqueIp = id,
        ipPath = path,
    )
}

/** Starts a remake or reboot of an owned IP. */
fun GameState.startIpRevival(id: String, path: IpPath, team: Int = 1) {
    requireLive()
    reviveIpBlocker(id, path, team)?.let { throw notAvailable(it) }
    val concept = reviveConcept(id, path)
    startGame(concept, team = team)
    val p = games!!.projectOf(team)!!
    val ip = dormantIp(id)!!
    p.hype = min(100.0, p.hype + path.hype * (0.6 + ip.qualityMemory * 0.8))
    addLog(
        "You greenlit a ${path.title.lowercase()} of ${ip.brand}. " +
            if (ip.memeRisk >= 0.5) "Fans are already meme-ing the announcement." else "Nostalgia is on your side.",
        LogTone.NEUTRAL, Lane.GAMES,
    )
    feedOnAntique(ip.brand, if (path == IpPath.REMAKE) "remake" else "reboot")
}

/** Work multiplier for antique IP projects (already folded into GameConcept.work via ipPath). */
internal fun antiqueWork(path: IpPath?): Double = path?.work ?: 1.0

/** After shipping an antique revival: hit, solid, or meme flop. */
internal fun GameState.resolveAntiqueShip(r: GameRelease) {
    val id = r.concept.antiqueIp ?: return
    val owned = ownedIp(id) ?: return
    val ip = dormantIp(id) ?: return
    val g = games ?: return
    val path = r.concept.ipPath ?: IpPath.REMAKE
    when (path) {
        IpPath.REMAKE -> owned.remakes += 1
        IpPath.REBOOT -> owned.reboots += 1
    }
    owned.lastShipWeek = week
    owned.lastScore = r.score
    val scoreLabel = String.format(java.util.Locale.US, "%.1f", r.score)
    val memeRoll = rng.chance(ip.memeRisk * (if (path == IpPath.REBOOT) 1.2 else 0.7) * (if (r.score < 6.5) 1.4 else 0.5))
    if (memeRoll && r.score < 7.5) {
        owned.memeFlop = true
        g.fanTrust = max(0.3, g.fanTrust - 0.04)
        stats.memeFlops += 1
        addLog("${r.title} is a meme flop. The internet will never let ${ip.brand} forget.", LogTone.BAD, Lane.GAMES)
        publish(
            NewsDesk.GAMES, "Kotaku",
            "${r.title} becomes a meme overnight",
            "Critical shrugs, endless clips. ${ip.brand}'s revival is a punchline.",
            LogTone.BAD, Lane.GAMES, tag = NewsTag.REVIEW,
        )
        feedOnAntique(ip.brand, "meme")
        museumBeat(LegacyBeatKind.FLOP, "Meme flop: ${r.title}", "${ip.brand} revival scored $scoreLabel.")
    } else if (r.score >= 8.0) {
        owned.memeFlop = false
        g.fanTrust = min(1.2, g.fanTrust + 0.03 + ip.qualityMemory * 0.02)
        stats.antiqueHits += 1
        addLog("${r.title} lands. ${ip.brand} feels alive again.", LogTone.GOOD, Lane.GAMES)
        feedOnAntique(ip.brand, "hit")
        museumBeat(LegacyBeatKind.HIT, "Revived ${ip.brand}", "${r.title} scored $scoreLabel.")
    } else {
        owned.memeFlop = false
        museumBeat(LegacyBeatKind.MILESTONE, "Shipped ${r.title}", "Antique ${ip.brand} revival · $scoreLabel.")
    }
}

/** Soft sales lift from nostalgia when an antique revival is selling. */
internal fun GameState.antiqueLift(r: GameRelease): Double {
    val id = r.concept.antiqueIp ?: return 1.0
    val ip = dormantIp(id) ?: return 1.0
    val owned = ownedIp(id)
    val path = r.concept.ipPath ?: IpPath.REMAKE
    val base = 1.0 + ip.qualityMemory * 0.12 + if (path == IpPath.REMAKE) 0.05 else 0.0
    return if (owned?.memeFlop == true) base * 0.85 else base
}
