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

// Mod / community scene
//
// After launch on PC (esp. later decades), a workshop culture can form around a title: cosmetics,
// fix packs, total conversions. Support it and you get long-tail sales and sticky fans; ignore
// broken tools or clamp down too hard and the scene turns toxic, review-bomby, or brand-risky.

/** How you treat the modders around a game. */
@Serializable
enum class ModPolicy(val title: String, val blurb: String) {
    IGNORE("Leave it alone", "The scene grows or withers on its own."),
    SUPPORT("Support tools & docs", "Docs, samples and a forum: healthier, stickier."),
    OFFICIAL("Official mod kit", "A proper SDK. Buzz climbs; sales hold longer."),
    CLAMP("Crack down", "Ban packs, pull broken mods. Buzz dies; brand risk if you go hard."),
    PARTNER("Partner a big mod team", "Pick a star team. Sticky fans, goodwill, a sales bump."),
}

/** Games that suit modding. */
fun GameRelease.moddable(): Boolean =
    concept.hook == Hook.MODS || concept.genre in setOf(Genre.SIMULATION, Genre.STRATEGY, Genre.SANDBOX, Genre.RPG, Genre.SHOOTER)

/** True once a community scene has formed. */
val GameRelease.hasModScene: Boolean get() = modSceneWeek >= 0

/** PC is where workshop culture lives. */
fun GameRelease.onPc(): Boolean =
    Platform.PC in concept.platforms || Platform.PC in extraPlatforms

/**
 * How much the era wants modding: absent early, rising through the nineties, full workshop culture
 * once digital storefronts own PC. Console-only titles never get a scene.
 */
fun GameState.modEraStrength(): Double = when {
    calendarYear < 1993 -> 0.0
    calendarYear < 2000 -> 0.28
    calendarYear < 2006 -> 0.55
    calendarYear < 2012 -> 0.85
    else -> 1.0
}

/** Extra weekly sales retention from the mod scene (0 when none). */
fun GameRelease.modSalesLift(): Double {
    if (hasModScene) return (0.003 + modBuzz * modHealth * 0.018).coerceIn(0.0, 0.03)
    return if (modTools) 0.008 else 0.0
}

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
    !r.onPc() -> "Modding happens on PC."
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
    if (!r.hasModScene && modEraStrength() > 0) formModScene(r, seed = 0.35)
}

fun GameState.modDocsCost(r: GameRelease): Double = scaled(8_000.0) * when (r.concept.size) {
    GameSize.SMALL -> 0.5; GameSize.MEDIUM -> 1.0; GameSize.LARGE -> 1.6; GameSize.AAA -> 2.2
}

fun GameState.modDocsBlocker(r: GameRelease): String? = when {
    r.publisher != null || r.developer != null -> "It isn't yours."
    !r.hasModScene && !r.modTools -> "There's no scene to support yet."
    !r.onPc() -> "Modding happens on PC."
    week - r.modDocsWeek < 26 -> "You put docs out recently. Again from ${GameClock.label(r.modDocsWeek + 26)}."
    cash < modDocsCost(r) -> "Support costs ${money(modDocsCost(r))}."
    else -> null
}

/** Docs, samples and a forum: healthier scene, sticky fans. */
fun GameState.supportModDocs(id: Int) {
    requireLive()
    val g = games ?: throw notAvailable("You need a game studio.")
    val r = g.release(id) ?: throw invalid("No such game.")
    modDocsBlocker(r)?.let { throw notAvailable(it) }
    spend(modDocsCost(r), Lane.GAMES)
    r.modDocsWeek = week
    r.modPolicy = ModPolicy.SUPPORT
    if (!r.hasModScene) formModScene(r, seed = 0.25)
    r.modHealth = min(1.0, r.modHealth + 0.12)
    r.modBuzz = min(1.0, r.modBuzz + 0.08)
    g.fanTrust = min(1.2, g.fanTrust + 0.015)
    g.fans += r.unitsSold * 0.004
    addLog("You put out tools and docs for ${r.title}'s modders. The scene looks healthier.", LogTone.GOOD, Lane.GAMES)
    feedOnMod(r.title, "support")
}

fun GameState.modKitCost(r: GameRelease): Double = scaled(45_000.0) * when (r.concept.size) {
    GameSize.SMALL -> 0.6; GameSize.MEDIUM -> 1.0; GameSize.LARGE -> 2.0; GameSize.AAA -> 3.5
}

fun GameState.modKitBlocker(r: GameRelease): String? = when {
    r.publisher != null || r.developer != null -> "It isn't yours."
    r.isCollection -> "Collections don't get a mod kit."
    !r.modTools -> "Release mod tools first."
    r.modKit -> "The official kit is already out."
    !r.onPc() -> "Modding happens on PC."
    calendarYear < 1998 -> "Nobody expects an official kit yet."
    cash < modKitCost(r) -> "An official kit costs ${money(modKitCost(r))}."
    else -> null
}

/** A proper SDK: big buzz spike, long-tail sales. */
fun GameState.releaseOfficialModKit(id: Int) {
    requireLive()
    val g = games ?: throw notAvailable("You need a game studio.")
    val r = g.release(id) ?: throw invalid("No such game.")
    modKitBlocker(r)?.let { throw notAvailable(it) }
    spend(modKitCost(r), Lane.GAMES)
    r.modKit = true
    r.modPolicy = ModPolicy.OFFICIAL
    if (!r.hasModScene) formModScene(r, seed = 0.45)
    r.modBuzz = min(1.0, r.modBuzz + 0.28)
    r.modHealth = min(1.0, r.modHealth + 0.1)
    g.fans += r.unitsSold * 0.01
    g.fanTrust = min(1.2, g.fanTrust + 0.02)
    addLog("The official mod kit for ${r.title} is out. Workshops are filling up.", LogTone.GOOD, Lane.GAMES)
    publish(NewsDesk.GAMES, "PC Gamer", "$companyName opens ${r.title} to modders properly", "An official kit, not just a dump of tools.", LogTone.GOOD, Lane.GAMES)
    feedOnMod(r.title, "kit")
}

fun GameState.clampModsBlocker(r: GameRelease): String? = when {
    r.publisher != null || r.developer != null -> "It isn't yours."
    !r.hasModScene -> "There's no scene to clamp down on."
    r.modPolicy == ModPolicy.CLAMP && week - r.modClampWeek < 13 -> "You're already cracking down."
    else -> null
}

/** Ban packs / pull mods: buzz dies, brand risk if you go hard. */
fun GameState.clampMods(id: Int) {
    requireLive()
    val g = games ?: throw notAvailable("You need a game studio.")
    val r = g.release(id) ?: throw invalid("No such game.")
    clampModsBlocker(r)?.let { throw notAvailable(it) }
    r.modPolicy = ModPolicy.CLAMP
    r.modClampWeek = week
    r.modBuzz = max(0.0, r.modBuzz * 0.35)
    r.modHealth = max(0.15, r.modHealth - 0.25)
    r.brokenMods = false
    g.fanTrust = max(0.3, g.fanTrust - 0.04)
    if (r.userScore > 0) r.userScore = max(1.0, r.userScore - 0.15)
    addLog("You cracked down on ${r.title}'s mod scene. Workshops emptied; some fans are furious.", LogTone.BAD, Lane.GAMES)
    publish(NewsDesk.GAMES, "Kotaku", "$companyName bans mods for ${r.title}", "Modders call it a purge. The storefront looks cleaner.", LogTone.BAD, Lane.GAMES, tag = NewsTag.NEWS)
    feedOnMod(r.title, "clamp")
    if (flavour("clamp scar ${r.id}").chance(0.35 + (1 - r.modHealth) * 0.3)) {
        reputation.scar(ScarKind.SCANDAL, 0.08, week, "Clamped down hard on ${r.title} modders")
    }
}

fun GameState.partnerModCost(r: GameRelease): Double = max(12_000.0, r.concept.size.weeklyBudget * 4)

fun GameState.partnerModBlocker(r: GameRelease): String? = when {
    r.publisher != null || r.developer != null -> "It isn't yours."
    !r.hasModScene -> "There's no scene to partner with."
    r.modPartner != null -> "You're already partnered with ${r.modPartner}."
    r.modBuzz < 0.25 -> "The scene isn't big enough for a flagship partner yet."
    cash < partnerModCost(r) -> "A partnership costs ${money(partnerModCost(r))}."
    else -> null
}

private val ModTeamNames = listOf(
    "Skyforge Collective", "Patchwork Guild", "Neon Cartographers", "Third Shift Labs",
    "Unofficial Works", "Cascade Modding", "Greybox Assembly", "Aftermarket Atlas",
)

/** Partner a big mod team: sticky fans, goodwill, sales. */
fun GameState.partnerModTeam(id: Int) {
    requireLive()
    val g = games ?: throw notAvailable("You need a game studio.")
    val r = g.release(id) ?: throw invalid("No such game.")
    partnerModBlocker(r)?.let { throw notAvailable(it) }
    spend(partnerModCost(r), Lane.GAMES)
    val team = flavour("partner ${r.id}").pick(ModTeamNames)
    r.modPartner = team
    r.modPolicy = ModPolicy.PARTNER
    r.modBuzz = min(1.0, r.modBuzz + 0.15)
    r.modHealth = min(1.0, r.modHealth + 0.18)
    r.weeklySales *= 1.12
    g.fans += r.unitsSold * 0.015
    g.fanTrust = min(1.2, g.fanTrust + 0.025)
    addLog("You partnered with $team on ${r.title}. Their fans are yours now too.", LogTone.GOOD, Lane.GAMES)
    publish(NewsDesk.GAMES, "PC Gamer", "$companyName teams up with $team", "The biggest ${r.title} modders are going semi-official.", LogTone.GOOD, Lane.GAMES)
    feedOnMod(r.title, "partner")
}

fun GameState.ignoreModSceneBlocker(r: GameRelease): String? = when {
    r.publisher != null || r.developer != null -> "It isn't yours."
    !r.hasModScene -> "There's no scene yet."
    r.modPolicy == ModPolicy.IGNORE -> "You're already leaving it alone."
    else -> null
}

/** Step back: no more support spend, no clamp — the scene runs itself. */
fun GameState.ignoreModScene(id: Int) {
    requireLive()
    val g = games ?: throw notAvailable("You need a game studio.")
    val r = g.release(id) ?: throw invalid("No such game.")
    ignoreModSceneBlocker(r)?.let { throw notAvailable(it) }
    r.modPolicy = ModPolicy.IGNORE
    addLog("You're leaving ${r.title}'s mod scene alone.", LogTone.NEUTRAL, Lane.GAMES)
}

fun GameState.fixBrokenModsBlocker(r: GameRelease): String? = when {
    r.publisher != null || r.developer != null -> "It isn't yours."
    !r.brokenMods -> "Nothing broken on the shelf."
    cash < scaled(5_000.0) -> "Cleaning up costs ${money(scaled(5_000.0))}."
    else -> null
}

/** Pull or fix broken community content on the storefront. */
fun GameState.fixBrokenMods(id: Int) {
    requireLive()
    val g = games ?: throw notAvailable("You need a game studio.")
    val r = g.release(id) ?: throw invalid("No such game.")
    fixBrokenModsBlocker(r)?.let { throw notAvailable(it) }
    spend(scaled(5_000.0), Lane.GAMES)
    r.brokenMods = false
    r.modHealth = min(1.0, r.modHealth + 0.06)
    addLog("Broken ${r.title} mods are off the storefront.", LogTone.GOOD, Lane.GAMES)
}

/** A mod that took off. */
@Serializable @SerialName("modHit")
data class ModHit(val releaseId: Int, val mod: String, val modder: String) : EventKind

/** Something went wrong in a title's mod scene. */
@Serializable
enum class ModCrisisKind(val title: String) {
    TOXIC("a toxic mod community"),
    ABANDONED("abandoned mod tools"),
    BROKEN_STORE("broken mods on the storefront"),
}

@Serializable @SerialName("modCrisis")
data class ModCrisis(val releaseId: Int, val kind: ModCrisisKind) : EventKind

private val ModNames = listOf(
    "Total Overhaul", "Reforged", "Extended Cut", "Hardcore Mode", "Expanded Worlds", "Unofficial Patch", "Better Cities",
    "Lost Chapters", "Realism Pack", "Co-op Everywhere", "Rebalanced", "The Long Road", "Cosmetic Vault", "Fix Pack",
)

/** Kick off a community scene around [r]. */
internal fun GameState.formModScene(r: GameRelease, seed: Double) {
    if (r.hasModScene || !r.onPc() || modEraStrength() <= 0) return
    r.modSceneWeek = week
    r.modBuzz = (seed * modEraStrength()).coerceIn(0.12, 0.55)
    r.modHealth = (0.55 + seed * 0.25).coerceIn(0.4, 0.85)
    stats.modScenes += 1
    addLog("A mod community is forming around ${r.title}.", LogTone.GOOD, Lane.GAMES)
    feedOnMod(r.title, "form")
}

/** Each week: form scenes, drift buzz/health, hit mods, crises, Mod of the Year. */
internal fun GameState.modWeek(g: GamesStudio) {
    val era = modEraStrength()
    if (era <= 0) return
    val dice = flavour("mods")
    for (r in g.releases) {
        if (!r.onPc() || !r.moddable()) continue
        if (r.publisher != null || r.developer != null || r.isCollection) continue
        val age = week - r.week
        if (age < 8 || age > 312) continue
        // Form a scene: faster with tools, weaker organic fan reverse-engineering in later eras.
        if (!r.hasModScene) {
            val chance = when {
                r.modTools && week - r.modToolsWeek >= 4 -> 0.04 * era
                r.score >= 7.5 && age >= 26 && calendarYear >= 1998 -> 0.008 * era
                else -> 0.0
            }
            if (chance > 0 && dice.chance(chance)) formModScene(r, seed = if (r.modTools) 0.4 else 0.22)
        }
        if (!r.hasModScene) continue
        driftModScene(r, g, dice)
    }
    // One hit-mod event a week at most.
    if (events.none { it.kind is ModHit }) {
        for (r in g.releases.filter { it.hasModScene && it.modBuzz >= 0.2 && week - it.modSceneWeek >= 6 }) {
            val odds = 0.004 + r.modBuzz * 0.012 * era * (if (r.modTools || r.modKit) 1.0 else 0.55)
            if (week - r.week > 260 || !dice.chance(odds)) continue
            enqueue(ModHit(r.id, "${r.title}: ${dice.pick(ModNames)}", "${dice.pick(Names.first)} ${dice.pick(Names.last)}"))
            break
        }
    }
    if (events.none { it.kind is ModCrisis }) maybeModCrisis(g, dice)
    maybeModOfYear(g, dice)
    // Own storefront: ignored broken mods slow growth a little.
    g.store?.takeIf { it.isOpen(week) }?.let { store ->
        val broken = g.releases.count { it.brokenMods && Storefront.OWN in storesOf(it) }
        if (broken > 0) store.users = max(0.0, store.users - broken * 0.0004)
    }
}

private fun GameState.driftModScene(r: GameRelease, g: GamesStudio, dice: SeededRNG) {
    val era = modEraStrength()
    var dBuzz = when (r.modPolicy) {
        ModPolicy.IGNORE -> -0.004
        ModPolicy.SUPPORT -> 0.006
        ModPolicy.OFFICIAL -> 0.01
        ModPolicy.CLAMP -> -0.03
        ModPolicy.PARTNER -> 0.008
    }
    var dHealth = when (r.modPolicy) {
        ModPolicy.IGNORE -> -0.002
        ModPolicy.SUPPORT -> 0.008
        ModPolicy.OFFICIAL -> 0.006
        ModPolicy.CLAMP -> -0.01
        ModPolicy.PARTNER -> 0.01
    }
    if (r.modTools) dBuzz += 0.003 else dBuzz -= 0.006
    if (r.modKit) dBuzz += 0.004
    if (r.modPartner != null) dHealth += 0.003
    // Abandoned tools: no docs, no kit, tools out a long time under ignore.
    if (r.modTools && r.modPolicy == ModPolicy.IGNORE && !r.modKit && week - r.modToolsWeek > 52 && week - r.modDocsWeek > 52) {
        dBuzz -= 0.008
        dHealth -= 0.01
    }
    if (r.brokenMods) dHealth -= 0.012
    r.modBuzz = (r.modBuzz + dBuzz * era + dice.range(-0.01, 0.01)).coerceIn(0.0, 1.0)
    r.modHealth = (r.modHealth + dHealth * era + dice.range(-0.008, 0.008)).coerceIn(0.05, 1.0)
    // Sticky fans from a healthy scene.
    if (r.modBuzz > 0.35 && r.modHealth > 0.55 && dice.chance(0.15)) {
        g.fans += r.unitsSold * 0.0004 * r.modBuzz * r.modHealth
    }
    // Low health + buzz can nibble the player score (review-bomb adjacent noise).
    if (r.modHealth < 0.35 && r.modBuzz > 0.25 && r.userScore > 0 && dice.chance(0.08)) {
        r.userScore = max(1.0, r.userScore - 0.05)
    }
}

private fun GameState.maybeModCrisis(g: GamesStudio, dice: SeededRNG) {
    for (r in g.releases.filter { it.hasModScene }) {
        val kind = when {
            r.modHealth < 0.32 && r.modBuzz > 0.2 && dice.chance(0.025) -> ModCrisisKind.TOXIC
            r.modTools && !r.modKit && r.modPolicy == ModPolicy.IGNORE && week - r.modToolsWeek > 40 &&
                dice.chance(0.02) -> ModCrisisKind.ABANDONED
            r.modBuzz > 0.4 && !r.brokenMods && r.modPolicy != ModPolicy.CLAMP && dice.chance(0.015 * modEraStrength()) ->
                ModCrisisKind.BROKEN_STORE
            else -> null
        } ?: continue
        if (kind == ModCrisisKind.BROKEN_STORE) r.brokenMods = true
        enqueue(ModCrisis(r.id, kind))
        return
    }
}

private fun GameState.maybeModOfYear(g: GamesStudio, dice: SeededRNG) {
    // Once a year in December, a healthy scene can make Mod of the Year news.
    if (GameClock.weekOfYear(week) !in 48..51) return
    if (stats.modOfYear > 0 && flavour("moty once").chance(0.7)) return
    val r = g.releases
        .filter { it.hasModScene && it.modBuzz >= 0.55 && it.modHealth >= 0.55 && it.modPolicy != ModPolicy.CLAMP }
        .maxByOrNull { it.modBuzz * it.modHealth } ?: return
    if (!dice.chance(0.08 * modEraStrength())) return
    stats.modOfYear += 1
    r.weeklySales *= 1.15
    g.fans += r.unitsSold * 0.02
    g.fanTrust = min(1.2, g.fanTrust + 0.03)
    publish(
        NewsDesk.GAMES, "PC Gamer", "${r.title} wins Mod of the Year",
        "The workshop around ${r.title} is the liveliest in games. People are still buying it just to play the mods.",
        LogTone.GOOD, Lane.GAMES, lead = true,
    )
    feedOnMod(r.title, "moty")
    addLog("${r.title} took Mod of the Year. The long tail just got longer.", LogTone.MILESTONE, Lane.GAMES)
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
    r.modBuzz = min(1.0, r.modBuzz + 0.1)
    when (choice) {
        0 -> {
            val c = makeCandidate(Role.DESIGNER).copy(name = k.modder, skill = rng.int(6, 8), specialty = Specialty.GAME_DESIGNER, contestedBy = null)
            spend(c.askingSalary * 2, Lane.GAMES)
            hireDirect(c, Lane.GAMES)
            g.fans += r.unitsSold * 0.01
            r.modHealth = min(1.0, r.modHealth + 0.05)
            addLog("${k.modder}, who made ${k.mod}, works for you now.", LogTone.GOOD, Lane.GAMES)
        }
        1 -> {
            spend(officialModCost(r), Lane.GAMES)
            r.weeklySales *= 1.3
            if (r.userScore > 0) r.userScore = min(10.0, r.userScore + 0.2)
            g.fans += r.unitsSold * 0.02
            r.modHealth = min(1.0, r.modHealth + 0.08)
            addLog("${k.mod} is now part of ${r.title}.", LogTone.GOOD, Lane.GAMES)
        }
        else -> {
            r.weeklySales *= 1.1
            g.fans += r.unitsSold * 0.005
            r.modHealth = min(1.0, r.modHealth + 0.04)
            if (flavour("mod ${k.mod}").chance(0.3)) {
                publish(NewsDesk.GAMES, "PC Gamer", "The team behind ${k.mod} is making its own game", "A modding scene can grow up fast.", LogTone.NEUTRAL, Lane.GAMES)
            }
        }
    }
}

internal fun GameState.describeModCrisis(k: ModCrisis): Description? {
    val r = games?.release(k.releaseId) ?: return null
    return when (k.kind) {
        ModCrisisKind.TOXIC -> Description(
            "${r.title}'s mod scene is turning toxic",
            "Flame wars, stolen assets, review-bomb adjacent noise. The workshop is loud and ugly.",
            listOf(
                choice("Support the good ones", "${money(modDocsCost(r))}. Docs and moderation: health climbs."),
                choice("Crack down", "Ban the worst. Buzz drops; some fans call it censorship."),
                choice("Ignore it", "It might blow over. Or it might leak into the user score."),
            ),
            Lane.GAMES,
        )
        ModCrisisKind.ABANDONED -> Description(
            "${r.title}'s mod tools look abandoned",
            "The tools shipped, then silence. Modders are forking broken builds and ranting on the feed.",
            listOf(
                choice("Ship an official kit", "${money(modKitCost(r))}. A real SDK wins them back."),
                choice("Put out docs", "${money(modDocsCost(r))}. Cheaper; helps a bit."),
                choice("Walk away", "The scene withers. Brand takes a small hit."),
            ),
            Lane.GAMES,
        )
        ModCrisisKind.BROKEN_STORE -> Description(
            "Broken ${r.title} mods are on the storefront",
            "Players install workshop junk that crashes the game. Ignoring it looks careless; clamping looks heavy-handed.",
            listOf(
                choice("Clean them up", "${money(scaled(5_000.0))}. Pull the broken ones."),
                choice("Partner a trusted team", "${money(partnerModCost(r))}. They curate; you get goodwill."),
                choice("Leave them", "Noise in reviews. Your own store grows a little slower."),
            ),
            Lane.GAMES,
        )
    }
}

internal fun GameState.resolveModCrisis(k: ModCrisis, choice: Int) {
    val g = games ?: return
    val r = g.release(k.releaseId) ?: return
    when (k.kind) {
        ModCrisisKind.TOXIC -> when (choice) {
            0 -> {
                if (cash >= modDocsCost(r)) spend(modDocsCost(r), Lane.GAMES)
                r.modPolicy = ModPolicy.SUPPORT
                r.modDocsWeek = week
                r.modHealth = min(1.0, r.modHealth + 0.2)
                g.fanTrust = min(1.2, g.fanTrust + 0.01)
                addLog("You backed the better ${r.title} modders. The shouting died down.", LogTone.GOOD, Lane.GAMES)
            }
            1 -> {
                r.modPolicy = ModPolicy.CLAMP
                r.modClampWeek = week
                r.modBuzz = max(0.0, r.modBuzz * 0.5)
                r.modHealth = max(0.2, r.modHealth - 0.1)
                g.fanTrust = max(0.3, g.fanTrust - 0.02)
                addLog("You banned the worst of ${r.title}'s scene. Quieter — and colder.", LogTone.NEUTRAL, Lane.GAMES)
            }
            else -> {
                r.modHealth = max(0.1, r.modHealth - 0.08)
                if (r.userScore > 0) r.userScore = max(1.0, r.userScore - 0.1)
                g.fanTrust = max(0.3, g.fanTrust - 0.02)
                addLog("You ignored the toxicity around ${r.title}. It leaked into reviews.", LogTone.BAD, Lane.GAMES)
            }
        }
        ModCrisisKind.ABANDONED -> when (choice) {
            0 -> {
                if (!r.modTools) {
                    r.modTools = true
                    r.modToolsWeek = week
                }
                if (cash >= modKitCost(r) && !r.modKit) {
                    spend(modKitCost(r), Lane.GAMES)
                    r.modKit = true
                }
                r.modPolicy = ModPolicy.OFFICIAL
                r.modBuzz = min(1.0, r.modBuzz + 0.2)
                r.modHealth = min(1.0, r.modHealth + 0.15)
                addLog("An official ${r.title} kit is out. Modders came back.", LogTone.GOOD, Lane.GAMES)
            }
            1 -> {
                if (cash >= modDocsCost(r)) spend(modDocsCost(r), Lane.GAMES)
                r.modDocsWeek = week
                r.modPolicy = ModPolicy.SUPPORT
                r.modHealth = min(1.0, r.modHealth + 0.1)
                addLog("Fresh docs for ${r.title}. Not a kit, but something.", LogTone.NEUTRAL, Lane.GAMES)
            }
            else -> {
                r.modBuzz = max(0.0, r.modBuzz * 0.6)
                r.modHealth = max(0.15, r.modHealth - 0.12)
                reputation.scar(ScarKind.SCANDAL, 0.04, week, "Abandoned ${r.title} mod tools")
                addLog("You walked away from ${r.title}'s tools. The scene withered.", LogTone.BAD, Lane.GAMES)
            }
        }
        ModCrisisKind.BROKEN_STORE -> when (choice) {
            0 -> {
                if (cash >= scaled(5_000.0)) spend(scaled(5_000.0), Lane.GAMES)
                r.brokenMods = false
                r.modHealth = min(1.0, r.modHealth + 0.08)
                addLog("Broken ${r.title} mods are gone from the shelf.", LogTone.GOOD, Lane.GAMES)
            }
            1 -> {
                if (r.modPartner == null && cash >= partnerModCost(r)) {
                    spend(partnerModCost(r), Lane.GAMES)
                    r.modPartner = flavour("crisis partner ${r.id}").pick(ModTeamNames)
                    r.modPolicy = ModPolicy.PARTNER
                }
                r.brokenMods = false
                r.modHealth = min(1.0, r.modHealth + 0.12)
                r.modBuzz = min(1.0, r.modBuzz + 0.05)
                addLog("${r.modPartner ?: "A mod team"} is curating ${r.title}'s workshop now.", LogTone.GOOD, Lane.GAMES)
            }
            else -> {
                r.brokenMods = true
                if (r.userScore > 0) r.userScore = max(1.0, r.userScore - 0.08)
                g.fanTrust = max(0.3, g.fanTrust - 0.015)
                addLog("Broken ${r.title} mods stayed up. Players noticed.", LogTone.BAD, Lane.GAMES)
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
