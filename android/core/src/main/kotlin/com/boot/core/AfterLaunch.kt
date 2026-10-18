package com.boot.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/*
 * The long tail: what you can do with a game once it's out.
 *
 * - Pricing: drop the price for good (old games sell more cheap; a young game's buyers feel cheated),
 *   or run a sale at 25, 50 or 75% off. A sale sells more now, but pulls forward buyers who'd have
 *   paid full price later (many of them, for a young game) and teaches players to wait.
 * - Content updates: free updates that win players back, a few times over a game's life.
 * - Mod tools: let players make more of it. Sales tail off more slowly, and now and then a mod
 *   takes off: hire its maker, make it official, or let it be.
 * - More stores: put a released PC game on stores it wasn't on.
 * Ports and expansions also take more decisions (see Development.kt and StudioActions.kt).
 */

/** What players pay for a released game now, before any sale. */
val GameRelease.price: Double get() = concept.unitPrice * (1 - priceCut)

/** Is the game on sale this week? */
fun GameRelease.onSale(week: Int): Boolean = week <= saleUntil

/** How much price matters to a game's buyers: fans buy a new game at any price; later buyers wait for a deal. */
private fun elasticity(age: Int): Double = when {
    age < 26 -> 1.1
    age < 52 -> 1.3
    else -> 1.5
}

/** The permanent cuts on offer. */
val PriceCuts = listOf(0.25, 0.5, 0.75)

/** Why a game's price can't go down to [cut] off, or null if it can. */
fun GameState.priceCutBlocker(r: GameRelease, cut: Double): String? = when {
    r.publisher != null -> "${r.publisher} sets the price."
    r.developer != null -> "${r.developer} sets the price."
    r.live != null -> "Live games make their money in the store."
    r.weeklySales <= 0 -> "It isn't selling any more."
    cut <= r.priceCut + 1e-9 -> "Prices only go down."
    week - r.week < 4 -> "Let it sell at full price for a month first."
    else -> null
}

/** Lowers a released game's price for good. */
fun GameState.cutPrice(id: Int, cut: Double) {
    requireLive()
    val g = games ?: throw notAvailable("You need a game studio.")
    val r = g.release(id) ?: throw invalid("No such game.")
    if (PriceCuts.none { kotlin.math.abs(it - cut) < 1e-9 }) throw invalid("Pick one of the usual cuts.")
    priceCutBlocker(r, cut)?.let { throw notAvailable(it) }
    val age = week - r.week
    val ratio = (1 - cut) / (1 - r.priceCut)
    // Revenue moves by ratio^(1 - elasticity): more copies, each for less.
    r.weeklySales *= ratio.pow(1 - elasticity(age))
    r.priceCut = cut
    if (age < 12) {
        g.fanTrust = max(0.5, g.fanTrust - 0.02)
        if (r.userScore > 0) r.userScore = max(1.0, r.userScore - 0.2)
        addLog("${r.title} is now ${money(r.price)}. People who paid full price a few weeks ago aren't happy.", LogTone.NEUTRAL, Lane.GAMES)
    } else {
        addLog("${r.title} is now ${money(r.price)}.", LogTone.NEUTRAL, Lane.GAMES)
    }
}

/** Weeks between sales of the same game. */
const val SALE_GAP = 13

/** How long a sale runs. */
const val SALE_WEEKS = 2

/** The sale discounts on offer. */
val SaleDepths = listOf(0.25, 0.5, 0.75)

/** Why a game can't go on sale, or null if it can. */
fun GameState.saleBlocker(r: GameRelease): String? = when {
    r.publisher != null -> "${r.publisher} runs its sales."
    r.developer != null -> "${r.developer} runs its sales."
    r.live != null -> "Live games make their money in the store, not on sale."
    week - r.week < 8 -> "It's too new to discount."
    r.weeklySales <= 0 -> "It isn't selling any more."
    r.onSale(week) -> "It's on sale now."
    week - r.lastSaleWeek < SALE_GAP -> "It was on sale recently. Again from ${GameClock.label(r.lastSaleWeek + SALE_GAP)}."
    else -> null
}

/** What a sale at [depth] off would do: revenue while it runs, and the full-price buyers it pulls forward. */
data class SaleOutlook(val lift: Double, val pull: Double)

fun GameState.saleOutlook(r: GameRelease, depth: Double): SaleOutlook {
    val age = week - r.week
    val raw = (1 - depth).pow(1 - 1.6)
    val lift = 1 + (raw - 1) / (1 + r.saleFatigue)
    val pull = depth * when {
        age < 26 -> 0.35
        age < 52 -> 0.18
        else -> 0.06
    }
    return SaleOutlook(lift, pull)
}

/** A sale for two weeks at [depth] off. */
fun GameState.runSale(releaseId: Int, depth: Double = 0.5) {
    requireLive()
    val g = games ?: throw notAvailable("You need a game studio.")
    val r = g.release(releaseId) ?: throw invalid("No such game.")
    if (SaleDepths.none { kotlin.math.abs(it - depth) < 1e-9 }) throw invalid("Pick one of the usual discounts.")
    saleBlocker(r)?.let { throw notAvailable(it) }
    val o = saleOutlook(r, depth)
    r.lastSaleWeek = week
    r.saleUntil = week + SALE_WEEKS
    r.saleDepth = depth
    r.saleLift = o.lift
    r.salePull = o.pull
    r.saleFatigue += depth * 0.6
    val buyers = r.weeklySales * o.lift * SALE_WEEKS / (r.price * (1 - depth))
    g.fans += buyers * 0.04
    stats.sales += 1
    addLog("${r.title} is ${Format.percent(depth)} off for ${Format.weeks(SALE_WEEKS)}.", LogTone.GOOD, Lane.GAMES)
}

// Content updates

/** Free updates a game can get over its life, and how far apart. */
const val MAX_UPDATES = 3
const val UPDATE_GAP = 26

fun GameRelease.updateCost(): Double = max(5_000.0, concept.size.weeklyBudget * 8)

fun GameState.updateBlocker(r: GameRelease): String? = when {
    r.publisher != null -> "${r.publisher} handles updates for that one."
    r.developer != null -> "${r.developer} handles updates for that one."
    r.isCollection -> "Collections don't get updates."
    r.updates >= MAX_UPDATES -> "It's had all the updates it needs. Time for a sequel."
    week - r.week > 156 -> "Players have moved on."
    r.weeklySales <= 0 && (r.live == null || r.live!!.closed) -> "Nobody's playing it any more."
    week - r.lastUpdateWeek < UPDATE_GAP -> "It had an update recently. Again from ${GameClock.label(r.lastUpdateWeek + UPDATE_GAP)}."
    cash < r.updateCost() -> "An update costs ${money(r.updateCost())}."
    else -> null
}

/** A free content update: new things to do, players come back, and the score creeps up. */
fun GameState.releaseUpdate(id: Int) {
    requireLive()
    val g = games ?: throw notAvailable("You need a game studio.")
    val r = g.release(id) ?: throw invalid("No such game.")
    updateBlocker(r)?.let { throw notAvailable(it) }
    spend(r.updateCost(), Lane.GAMES)
    r.updates += 1
    r.lastUpdateWeek = week
    val before = r.userScore
    if (r.userScore > 0) r.userScore = min(min(10.0, r.score + 1.5), r.userScore + 0.3)
    if (r.weeklySales > 0) r.weeklySales *= 1.25
    r.live?.takeIf { !it.closed }?.let { it.players *= 1.1; it.peakPlayers = max(it.peakPlayers, it.players) }
    g.fans += r.unitsSold * 0.01
    g.fanTrust = min(1.2, g.fanTrust + 0.02)
    addLog("Update ${r.updates} for ${r.title} is out. Players are coming back.", LogTone.GOOD, Lane.GAMES)
    if (before > 0 && r.userScore - r.firstUserScore >= 1.5 && r.userScore - before > 0) {
        publish(NewsDesk.GAMES, "PC Gamer", "${r.title} is one of the most improved games around", "Players who gave up on it are coming back.", LogTone.GOOD, Lane.GAMES)
    }
}

// Mod tools

/** Games that suit modding. */
fun GameRelease.moddable(): Boolean =
    concept.hook == Hook.MODS || concept.genre in setOf(Genre.SIMULATION, Genre.STRATEGY, Genre.SANDBOX, Genre.RPG, Genre.SHOOTER)

fun GameState.modToolsCost(r: GameRelease): Double = scaled(20_000.0) * when (r.concept.size) {
    GameSize.SMALL -> 0.5
    GameSize.MEDIUM -> 1.0
    GameSize.LARGE -> 2.0
    GameSize.AAA -> 3.0
}

fun GameState.modToolsBlocker(r: GameRelease): String? = when {
    r.publisher != null || r.developer != null -> "It isn't yours to open up."
    r.isCollection -> "Collections don't get mod tools."
    r.modTools -> "Modders already have the tools."
    Platform.PC !in r.concept.platforms && Platform.PC !in r.extraPlatforms -> "Modding happens on PC."
    !r.moddable() -> "Not the kind of game people mod."
    calendarYear < 1993 -> "Nobody mods games yet."
    cash < modToolsCost(r) -> "Mod tools cost ${money(modToolsCost(r))}."
    else -> null
}

/** Release the tools players need to mod a game. */
fun GameState.releaseModTools(id: Int) {
    requireLive()
    val g = games ?: throw notAvailable("You need a game studio.")
    val r = g.release(id) ?: throw invalid("No such game.")
    modToolsBlocker(r)?.let { throw notAvailable(it) }
    spend(modToolsCost(r), Lane.GAMES)
    r.modTools = true
    r.modToolsWeek = week
    g.fans += r.unitsSold * 0.005
    addLog("Mod tools for ${r.title} are out. Players are already making things.", LogTone.GOOD, Lane.GAMES)
}

/** A mod that took off. */
@Serializable @SerialName("modHit")
data class ModHit(val releaseId: Int, val mod: String, val modder: String) : EventKind

private val ModNames = listOf(
    "Total Overhaul", "Reforged", "Extended Cut", "Hardcore Mode", "Expanded Worlds", "Unofficial Patch", "Better Cities",
    "Lost Chapters", "Realism Pack", "Co-op Everywhere", "Rebalanced", "The Long Road",
)

/** Each week: now and then a mod for one of your modded games takes off. */
internal fun GameState.modWeek(g: GamesStudio) {
    val dice = flavour("mods")
    for (r in g.releases.filter { it.modTools && week - it.modToolsWeek >= 8 }) {
        if (week - r.week > 260 || !dice.chance(0.006)) continue
        enqueue(ModHit(r.id, "${r.title}: ${dice.pick(ModNames)}", "${dice.pick(Names.first)} ${dice.pick(Names.last)}"))
        return
    }
}

internal fun GameState.describeModHit(k: ModHit): Description? {
    val r = games?.release(k.releaseId) ?: return null
    return Description(
        "A ${r.title} mod is taking off",
        "${k.mod}, by a modder called ${k.modder.substringBefore(' ')}, is the most downloaded thing around. People are buying ${r.title} just to play it.",
        listOf(
            choice("Hire ${k.modder.substringBefore(' ')}", "A talented designer joins you, and the mod's fans love it."),
            choice("Make it official", "${money(officialModCost(r))}. It ships as a free update: +30% sales and a better player score."),
            choice("Let it be", "Free. The scene stays healthy, and the modder might go off and make their own game."),
        ),
        Lane.GAMES,
    )
}

private fun GameState.officialModCost(r: GameRelease): Double = max(5_000.0, r.concept.size.weeklyBudget * 3)

internal fun GameState.resolveModHit(k: ModHit, choice: Int) {
    val g = games ?: return
    val r = g.release(k.releaseId) ?: return
    when (choice) {
        0 -> {
            val c = makeCandidate(Role.DESIGNER).copy(name = k.modder, skill = rng.int(6, 8), specialty = Specialty.GAME_DESIGNER, contestedBy = null)
            spend(c.askingSalary * 2, Lane.GAMES)
            hireDirect(c, Lane.GAMES)
            g.fans += r.unitsSold * 0.01
            addLog("${k.modder}, who made ${k.mod}, works for you now.", LogTone.GOOD, Lane.GAMES)
        }
        1 -> {
            spend(officialModCost(r), Lane.GAMES)
            r.weeklySales *= 1.3
            if (r.userScore > 0) r.userScore = min(10.0, r.userScore + 0.2)
            g.fans += r.unitsSold * 0.02
            addLog("${k.mod} is now part of ${r.title}.", LogTone.GOOD, Lane.GAMES)
        }
        else -> {
            r.weeklySales *= 1.1
            g.fans += r.unitsSold * 0.005
            if (flavour("mod ${k.mod}").chance(0.3)) {
                publish(NewsDesk.GAMES, "PC Gamer", "The team behind ${k.mod} is making its own game", "A modding scene can grow up fast.", LogTone.NEUTRAL, Lane.GAMES)
            }
        }
    }
}

// More stores after launch

/** The stores a released PC game is on now. */
fun GameState.storesOf(r: GameRelease): Set<Storefront> = r.stores.toSet().ifEmpty { defaultStores() }

fun GameState.addStoreBlocker(r: GameRelease, s: Storefront): String? = when {
    r.publisher != null || r.developer != null -> "It isn't yours to sell."
    Platform.PC !in r.concept.platforms -> "Only PC games pick a store."
    r.weeklySales <= 0 -> "It isn't selling any more."
    s in storesOf(r) -> "It's already there."
    s !in storesOpen() -> "${storeName(s)} isn't open to you."
    s == Storefront.GOG && r.protection != Protection.NONE -> "GOG only sells games without copy protection."
    else -> null
}

/** Puts a released PC game on another store. */
fun GameState.addStore(id: Int, s: Storefront) {
    requireLive()
    val g = games ?: throw notAvailable("You need a game studio.")
    val r = g.release(id) ?: throw invalid("No such game.")
    addStoreBlocker(r, s)?.let { throw notAvailable(it) }
    val remaster = r.concept.remasterOf != null
    val before = storeTerms(storesOf(r), remaster)
    val after = storeTerms(storesOf(r) + s, remaster)
    val pcShare = 1.0 / r.concept.platforms.size
    r.weeklySales *= 1 - pcShare + pcShare * (after.reach / before.reach.coerceAtLeast(0.01))
    r.netShare += pcShare * (after.keep - before.keep)
    r.stores = (storesOf(r) + s).toList()
    addLog("${r.title} is on ${storeName(s)} now.", LogTone.GOOD, Lane.GAMES)
}
