package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

/*
 * Game stores.
 *
 * Where a PC game is sold: in shops, on Steam, GOG, itch.io, the Epic Games Store, or your own
 * store. Each opened when it did, takes its own cut and reaches its own share of PC players. The
 * default is what most studios did that year (shops, then Steam too), so leaving it alone changes
 * nothing; leaving Steam out costs most of your PC buyers, adding the smaller stores finds a few
 * more. Epic pays a guarantee for a game sold only there. GOG only sells games without DRM.
 *
 * Your own store, a Steam rival: build it, set its cut, give games away to pull players in, and
 * pay studios for timed exclusives on their upcoming games. Its players buy from other studios and
 * you take the cut; your own PC games sold there keep every penny, and the bigger the store, the
 * more PC players it reaches. Steam doesn't sit still once you get big.
 */

@Serializable
enum class Storefront(val title: String, val owner: String, val opens: Int, val cut: Double, val blurb: String) {
    RETAIL("Shops", "retailers", 0, 0.30, "Boxes on shelves. Every game was sold this way, once."),
    STEAM("Steam", "Valve", 2005, 0.30, "Where most PC players buy their games."),
    GOG("GOG", "CD Projekt", 2008, 0.30, "DRM-free only, and fond of older games."),
    ITCH("itch.io", "itch.io", 2013, 0.10, "Small and indie, and it takes very little."),
    EPIC("Epic Games Store", "Epic Games", 2018, 0.12, "Smaller than Steam and takes far less. Pays for exclusives."),
    OWN("Your store", "you", 0, 0.0, "You keep everything your players spend."),
}

/** A store's name: your own store goes by the name you gave it. */
fun GameState.storeName(s: Storefront): String = if (s == Storefront.OWN) games?.store?.name ?: s.title else s.title

/** The share of PC buyers [s] reaches in [year]. */
fun GameState.storeReach(s: Storefront, year: Int = calendarYear, remaster: Boolean = false): Double = when (s) {
    Storefront.RETAIL -> physicalShare(year)
    Storefront.STEAM -> if (year < 2005) 0.0 else if (year < 2010) 0.45 else 0.75
    Storefront.GOG -> if (year < 2008) 0.0 else if (remaster) 0.15 else 0.06
    Storefront.ITCH -> if (year < 2013) 0.0 else 0.03
    Storefront.EPIC -> if (year < 2018) 0.0 else if (year < 2021) 0.15 else 0.2
    Storefront.OWN -> ownStoreReach()
}

/** The stores a game can be sold on now. */
fun GameState.storesOpen(): List<Storefront> = Storefront.entries.filter {
    if (it == Storefront.OWN) games?.store?.isOpen(week) == true else calendarYear >= it.opens && storeReach(it) > 0
}

/** What most studios sold through this year. */
fun GameState.defaultStores(): Set<Storefront> = buildSet {
    if (calendarYear < 2013) add(Storefront.RETAIL)
    if (calendarYear >= 2005) add(Storefront.STEAM)
}

/** The stores [p] will be sold on. */
fun GameState.storesFor(p: GameProject): Set<Storefront> {
    val open = storesOpen()
    return (p.stores ?: defaultStores()).filter { it in open && (it != Storefront.GOG || p.protection == Protection.NONE) }.toSet().ifEmpty { defaultStores() }
}

/** How a set of stores does on PC: reach against the usual stores, and the share you keep. */
data class StoreTerms(val reach: Double, val lift: Double, val keep: Double)

fun GameState.storeTerms(stores: Set<Storefront>, remaster: Boolean = false): StoreTerms {
    fun combined(set: Set<Storefront>) = 1 - set.fold(1.0) { acc, s -> acc * (1 - storeReach(s, remaster = remaster)) }
    val reach = combined(stores)
    val usual = combined(defaultStores()).coerceAtLeast(0.05)
    val weight = stores.sumOf { storeReach(it, remaster = remaster) }
    val keep = if (weight <= 0) 0.7 else stores.sumOf { storeReach(it, remaster = remaster) * (1 - it.cut) } / weight
    return StoreTerms(reach, (reach / usual).clamped(0.05, 1.4), keep)
}

/** Why [p] can't be sold on [s], or null if it can. */
fun GameState.storeBlocker(p: GameProject, s: Storefront): String? = when {
    p.deal != null -> "${p.deal.publisher} decides where it's sold."
    Platform.PC !in p.concept.platforms -> "Only PC games pick a store."
    s !in storesOpen() -> if (s == Storefront.OWN) "You don't have a store of your own." else "${s.title} opens in ${s.opens}."
    s == Storefront.GOG && p.protection != Protection.NONE -> "GOG only sells games without copy protection."
    else -> null
}

/** Sells [team]'s game on [s] or takes it off. */
fun GameState.setStore(team: Int, s: Storefront, on: Boolean) {
    requireLive()
    val p = games?.projectOf(team) ?: throw invalid("No game in development.")
    if (on) storeBlocker(p, s)?.let { throw notAvailable(it) }
    val now = storesFor(p).toMutableSet()
    if (on) now += s else now -= s
    if (now.isEmpty()) throw notAvailable("It has to be sold somewhere.")
    p.stores = now
}

/** Epic pays a guarantee for a game sold only on its store. */
fun GameState.epicGuarantee(p: GameProject): Double? {
    if (calendarYear < 2019 || p.deal != null || Platform.PC !in p.concept.platforms) return null
    if (storesFor(p) != setOf(Storefront.EPIC)) return null
    return p.concept.size.launchSales * 1.2
}

/** At launch: what the stores mean for this game. Returns (sales lift, share kept on PC). */
internal fun GameState.storeLaunchTerms(p: GameProject): Pair<Double, Double> {
    if (p.deal != null || Platform.PC !in p.concept.platforms) return 1.0 to 0.7
    val t = storeTerms(storesFor(p), remaster = p.concept.remasterOf != null)
    val pcShare = 1.0 / p.concept.platforms.size
    return (1 - pcShare + pcShare * t.lift) to t.keep
}

/** After launch: Epic's guarantee, and the players your own store gains. */
internal fun GameState.afterStores(p: GameProject, r: GameRelease) {
    if (p.deal != null || Platform.PC !in r.concept.platforms) return
    val stores = storesFor(p)
    r.stores = stores.toList()
    epicGuarantee(p)?.let { pay ->
        cash += pay
        recordRevenue(pay, Lane.GAMES)
        reputation.adjust(-1.0)
        addLog("Epic paid ${money(pay)} to have ${r.title} on its store alone. Some PC players are annoyed.", LogTone.NEUTRAL, Lane.GAMES)
        publish(NewsDesk.GAMES, "PC Gamer", "${r.title} is an Epic Games Store exclusive", "Steam players will have to wait, or switch.", LogTone.NEUTRAL, Lane.GAMES)
    }
    if (stores != defaultStores()) {
        val t = storeTerms(stores, remaster = p.concept.remasterOf != null)
        addLog(
            "${r.title} is on ${stores.joinToString(" and ") { storeName(it) }}: ${Format.percent(t.reach)} of PC players, and you keep ${Format.percent(t.keep)} of each sale there.",
            LogTone.NEUTRAL, Lane.GAMES,
        )
    }
    val own = games?.store ?: return
    if (Storefront.OWN !in stores) return
    val buyers = r.weeklySales / r.concept.unitPrice * 5 / r.concept.platforms.size
    // Sold only on your store, everyone who wants it signs up; alongside Steam, some do.
    val joined = buyers / 1_000_000 * (if (Storefront.STEAM in stores) 0.15 else 0.7)
    own.users += joined
    if (joined >= 0.01) addLog("${Format.count(joined * 1_000_000)} people signed up to ${own.name} for ${r.title}.", LogTone.GOOD, Lane.GAMES)
}

// Your own store

/** Your game store. Players in millions. */
@Serializable
data class OwnStore(
    var name: String,
    val startedWeek: Int,
    val openWeek: Int,
    var users: Double = 0.0,
    var catalogue: Int = 25,
    var cut: Double = 0.30,
    var giveaways: Boolean = false,
    var lastRevenue: Double = 0.0,
    var lastCost: Double = 0.0,
    var earned: Double = 0.0,
    var spent: Double = 0.0,
    var peakUsers: Double = 0.0,
    val exclusives: MutableList<StoreExclusive> = mutableListOf(),
    var steamAnswered: Boolean = false,
    var squeezedUntil: Int = 0,
) {
    fun isOpen(week: Int) = week >= openWeek
}

/** A rival's game you've paid to sell only on your store for a while. */
@Serializable
data class StoreExclusive(val title: String, val studio: String, val week: Int, val size: GameSize, val mega: Boolean, val paid: Double, var landed: Boolean = false)

/** The cuts your store can take. */
val StoreCuts = listOf(0.30, 0.20, 0.12, 0.08)

/** What it costs to build a store, and how long it takes. */
const val STORE_BUILD_COST = 3_000_000.0
const val STORE_BUILD_WEEKS = 26

/** What a week of free games costs: more as the store grows. */
fun giveawayCost(s: OwnStore): Double = 120_000.0 + s.users * 1_000_000 * 0.012

/** PC players today, in millions. */
fun GameState.pcPlayers(): Double = max(1.0, platformPlayers(Platform.PC))

/** The share of PC players with an account on your store. */
fun GameState.ownStoreReach(): Double {
    val s = games?.store ?: return 0.0
    if (!s.isOpen(week)) return 0.0
    return (s.users / pcPlayers()).coerceIn(0.0, 0.9)
}

/** Why you can't open a store, or null if you can. */
fun GameState.openStoreBlocker(): String? {
    val g = games ?: return "You need a game studio first."
    if (g.store != null) return "You already have one."
    if (calendarYear < 2004) return "Nobody sells games as downloads yet. Wait for 2004."
    if (Research.DIGITAL_STORES !in researched) return "Research digital storefronts first."
    if (cash < STORE_BUILD_COST) return "It costs ${money(STORE_BUILD_COST)} to build."
    return null
}

/** Starts building your own store. */
fun GameState.openStore(name: String = "$companyName Store") {
    requireLive()
    openStoreBlocker()?.let { throw notAvailable(it) }
    val g = games!!
    spend(STORE_BUILD_COST, Lane.GAMES)
    g.store = OwnStore(name.trim().ifEmpty { "$companyName Store" }, week, week + STORE_BUILD_WEEKS, spent = STORE_BUILD_COST)
    addLog("You're building ${g.store!!.name}, a store of your own. It opens ${GameClock.label(week + STORE_BUILD_WEEKS)}.", LogTone.MILESTONE, Lane.GAMES)
}

fun GameState.setStoreCut(cut: Double) {
    requireLive()
    val s = games?.store ?: throw notAvailable("You don't have a store.")
    if (StoreCuts.none { kotlin.math.abs(it - cut) < 1e-9 }) throw invalid("Pick one of the usual cuts.")
    s.cut = cut
}

fun GameState.setGiveaways(on: Boolean) {
    requireLive()
    val s = games?.store ?: throw notAvailable("You don't have a store.")
    s.giveaways = on
}

/** Upcoming games from other studios you could pay to sell only on your store. */
fun GameState.exclusiveTargets(): List<Announcement> {
    val g = games ?: return emptyList()
    val s = g.store ?: return emptyList()
    return g.market.upcoming.filter { a ->
        val studio = canonicalStudio(a.studio)
        a.week - week >= 4 && studio !in setOf("Nintendo", "Valve", "PlayStation Studios", "Xbox Game Studios") && !g.owns(studio) &&
            s.exclusives.none { it.title == a.title }
    }
}

/** What a studio wants to keep [a] off Steam for a year. */
fun GameState.exclusiveFee(a: Announcement): Double = a.size.launchSales * (if (a.mega) 2.5 else 1.0)

fun GameState.exclusiveBlocker(a: Announcement): String? {
    val s = games?.store ?: return "You don't have a store."
    if (a.mega && s.users < pcPlayers() * 0.05) return "They won't leave Steam for a store this small."
    if (cash < exclusiveFee(a)) return "They want ${money(exclusiveFee(a))}."
    return null
}

/** Pays to sell [title] only on your store. */
fun GameState.signStoreExclusive(title: String) {
    requireLive()
    val s = games?.store ?: throw notAvailable("You don't have a store.")
    val a = exclusiveTargets().firstOrNull { it.title == title } ?: throw notAvailable("$title isn't on offer.")
    exclusiveBlocker(a)?.let { throw notAvailable(it) }
    val fee = exclusiveFee(a)
    spend(fee, Lane.GAMES)
    s.spent += fee
    s.exclusives += StoreExclusive(a.title, a.studio, a.week, a.size, a.mega, fee)
    addLog("${a.title} will be sold only on ${s.name} for its first year: ${money(fee)}.", LogTone.GOOD, Lane.GAMES)
    publish(NewsDesk.GAMES, "PC Gamer", "${a.title} skips Steam for ${s.name}", "${a.studio}'s game will be a ${s.name} exclusive for a year.", LogTone.NEUTRAL, Lane.GAMES)
}

/** What your store would sell for. */
val GameState.storeValue: Double
    get() {
        val s = games?.store ?: return 0.0
        if (!s.isOpen(week)) return s.spent * 0.5
        return max(0.0, (s.lastRevenue - s.lastCost) * 52 * 6) + s.users * 1_000_000 * 1.5
    }

/** Sells your store. */
fun GameState.sellStore() {
    requireLive()
    val g = games ?: throw notAvailable("You don't make games.")
    val s = g.store ?: throw notAvailable("You don't have a store.")
    val price = storeValue
    g.store = null
    cash += price
    addLog("You sold ${s.name} for ${money(price)}.", LogTone.NEUTRAL, Lane.GAMES)
    publish(NewsDesk.GAMES, "Bloomberg", "$companyName sells ${s.name}", "${Format.count(s.users * 1_000_000)} players have a new landlord.", LogTone.NEUTRAL, Lane.GAMES, tag = NewsTag.MONEY)
}

/** How much other studios want to be on your store: more for a lower cut. */
private fun cutAppeal(cut: Double): Double = 1 + (0.30 - cut) * 6

/** Each week: build, grow, earn. */
internal fun GameState.storeWeek(g: GamesStudio) {
    val s = g.store ?: return
    if (week < s.openWeek) return
    if (week == s.openWeek) {
        s.users = 0.3 + g.fans / 1_000_000 * 0.5
        addLog("${s.name} is open, with ${Format.count(s.users * 1_000_000)} players signed up.", LogTone.MILESTONE, Lane.GAMES)
        publish(NewsDesk.GAMES, "PC Gamer", "$companyName opens ${s.name}", "A new place to buy PC games, taking a ${Format.percent(s.cut)} cut.", LogTone.NEUTRAL, Lane.GAMES, lead = true)
    }
    val pc = pcPlayers()
    val share = (s.users / pc).coerceIn(0.0, 1.0)
    val room = max(0.0, 0.9 - share)
    val appeal = cutAppeal(s.cut)
    val shelf = s.catalogue / (s.catalogue + 400.0)
    var growth = pc * 0.0006 * shelf * appeal * room
    if (week < s.squeezedUntil) growth *= 0.5
    var cost = s.users * 1_000_000 * 0.02
    if (s.giveaways) {
        growth += pc * 0.0012 * room
        cost += giveawayCost(s)
    }
    s.users = max(0.0, s.users * 0.996 + growth)
    // Studios come to where the players are, and to a lower cut.
    s.catalogue += 2 + (60 * share * appeal * appeal).toInt()
    // Exclusives: when one comes out, its players sign up.
    for (e in s.exclusives.filter { !it.landed && week >= it.week }) {
        e.landed = true
        val gain = pc * room * when {
            e.mega -> 0.06
            e.size == GameSize.AAA -> 0.03
            e.size == GameSize.LARGE -> 0.015
            else -> 0.005
        }
        s.users += gain
        s.catalogue += 1
        addLog("${e.title} is out, only on ${s.name}: ${Format.count(gain * 1_000_000)} new players.", LogTone.GOOD, Lane.GAMES)
    }
    s.peakUsers = max(s.peakUsers, s.users)
    stats.storePeakUsers = max(stats.storePeakUsers, s.users)
    val sales = s.users * 1_000_000 * 0.5 * (s.catalogue / (s.catalogue + 1_000.0)) * world.demand(Lane.GAMES)
    val revenue = sales * s.cut
    s.lastRevenue = revenue
    s.lastCost = cost
    s.earned += revenue
    s.spent += cost
    cash += revenue - cost
    recordRevenue(revenue, Lane.GAMES)
    recordCost(cost, Lane.GAMES)
    // Get big enough and Valve answers.
    if (!s.steamAnswered && share >= 0.12) {
        s.steamAnswered = true
        s.squeezedUntil = week + 26
        addLog("Valve has noticed ${s.name}: a huge Steam sale and a lower cut for big games. Your growth slows for six months.", LogTone.BAD, Lane.GAMES)
        publish(NewsDesk.GAMES, "PC Gamer", "Valve answers ${s.name}", "Steam cuts its fee for the biggest games and throws its biggest sale yet.", LogTone.NEUTRAL, Lane.GAMES, lead = true)
    }
}
