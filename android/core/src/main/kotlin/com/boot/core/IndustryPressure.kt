package com.boot.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/*
 * Industry pressure (3.34.6): contract labour for crunch, streaming / Let's Play era,
 * and platform-holder politics. Extends Team / crunch / AfterLaunch / Release /
 * Storefronts / Platform / News / Feed / Events — not parallel sims.
 *
 * Temp hooks from talent wars (`contractUntil`, `temps()` / `permanents()`) are filled here.
 */

// ─── Shared book (old saves default empty) ───────────────────────────────────

/** Lasting industry-pressure state on the company. Defaults keep old saves loadable. */
@Serializable
class IndustryBook {
    /** 0…1 — overuse / mistreatment of temps; feeds scandal risk. */
    var tempAbuse: Double = 0.0
    /** Temps hired this calendar year (scandal scales when this climbs). */
    var tempsHiredYear: Int = 0
    var tempsYearMark: Int = -1
    /** Lifetime temp hires — for balance / endings flavour. */
    var tempsHiredEver: Int = 0
    /** Creator relationships (name → favor −1…1). Empty before the streaming era. */
    val creators: MutableList<CreatorStanding> = mutableListOf()
    /** Standing with platform holders (Sony, Nintendo, …). */
    val platforms: MutableList<HolderStanding> = mutableListOf()
}

@Serializable
data class CreatorStanding(
    val name: String,
    /** −1…1 how they feel about you. */
    var favor: Double = 0.0,
    var lastCourtWeek: Int = -1_000,
    var plays: Int = 0,
)

@Serializable
data class HolderStanding(
    /** Maker string matching [Platform.maker] / store owners (Sony, Nintendo, Microsoft, Valve…). */
    val holder: String,
    /** −1…1 relationship. */
    var favor: Double = 0.0,
    /** Weeks of cert backlog still owed on the next console ship. */
    var certBacklog: Int = 0,
    /** Active featuring weeks left (modern digital stores). */
    var featuringUntil: Int = -1,
    /** Logo-on-the-box / first-party-friendly deal active until this week. */
    var logoUntil: Int = -1,
    /** Margin you gave up for the logo deal (0…0.2). */
    var logoCut: Double = 0.0,
)

// ─── Era gates ───────────────────────────────────────────────────────────────

/** Contract labour exists early; press / scandal bite harder later. */
fun GameState.contractPressScale(): Double = when {
    calendarYear < 1995 -> 0.35
    calendarYear < 2005 -> 0.55
    calendarYear < 2015 -> 0.8
    else -> 1.0
}

/** Streaming / Let's Play culture — weak before YouTube LPs, full from Twitch era. */
fun GameState.streamEraStrength(): Double = when {
    calendarYear < 2007 -> 0.0
    calendarYear < 2011 -> 0.35
    calendarYear < 2015 -> 0.7
    else -> 1.0
}

/** Modern digital store featuring (Steam / console stores as discovery engines). */
fun GameState.featuringEraStrength(): Double = when {
    calendarYear < 2008 -> 0.0
    calendarYear < 2012 -> 0.45
    else -> 1.0
}

// ─── 1. Contract labour boom ─────────────────────────────────────────────────

@Serializable
enum class TempKind(val title: String, val role: Role, val blurb: String) {
    ARTIST("Temp artists", Role.DESIGNER, "Artists and animators on short contracts — ship art faster, quality can slip."),
    QA("Temp QA", Role.ENGINEER, "Contract testers — more coverage and bug finds, shallow product sense."),
}

@Serializable
enum class ContractExit(val title: String, val blurb: String) {
    FAIR("Pay out fairly", "Two weeks' pay, clean exit. Permanents notice you treat people well."),
    QUIET("Quiet end", "Contract ends, no severance. Cheap; a little resentment."),
    CUT("Cut loose", "Walk them same day. Saves money — scandal risk if overused."),
}

/** Cost to bring in [count] temps for [weeks] (agency fee + salaries prepaid for half the term). */
fun GameState.tempHireCost(kind: TempKind, count: Int, weeks: Int): Double {
    val weekly = kind.role.baseSalary * 0.85 * world.salaries
    val agency = weekly * 2.5 * count
    val prepaid = weekly * count * (weeks / 2.0).coerceAtLeast(2.0)
    return agency + prepaid
}

fun GameState.tempHireBlocker(kind: TempKind, count: Int, weeks: Int): String? = when {
    ending != null -> "The run is over."
    Lane.GAMES !in lanes && kind == TempKind.ARTIST -> "Temp artists are for a games studio."
    count !in 1..6 -> "Hire between 1 and 6 at a time."
    weeks !in 4..26 -> "Contracts run 4–26 weeks."
    freeDesks < count -> "Need $count free desk${if (count == 1) "" else "s"}."
    cash < tempHireCost(kind, count, weeks) -> "Costs ${money(tempHireCost(kind, count, weeks))}."
    else -> null
}

/**
 * Hire fixed-term artists or QA. Sets [Employee.contractUntil] so they land in Team → Temps.
 * Speeds crunch / coverage; quality risk and permanent morale scale with how many you stack.
 */
fun GameState.hireTemps(kind: TempKind, count: Int = 2, weeks: Int = 8) {
    requireLive()
    tempHireBlocker(kind, count, weeks)?.let { throw notAvailable(it) }
    rollTempYear()
    val cost = tempHireCost(kind, count, weeks)
    spend(cost, Lane.GAMES.takeIf { Lane.GAMES in lanes })
    val until = week + weeks
    val hired = mutableListOf<String>()
    repeat(count) {
        val skill = rng.int(3, 6)
        val c = makeCandidate(kind.role).copy(
            skill = skill,
            askingSalary = kind.role.baseSalary * 0.85 * world.salaries * (0.5 + skill * 0.1),
            contestedBy = null,
            notable = false,
            reputation = 0.05,
            loyalty = rng.range(0.2, 0.45),
            chapter = CareerChapter.RISING,
            pastTitles = listOf("Contractor"),
            pastHits = emptyList(),
        )
        val e = hireDirect(c, if (Lane.GAMES in lanes) Lane.GAMES else lanes.firstOrNull())
        e.contractUntil = until
        e.morale = 0.65
        hired += e.name
    }
    industry.tempsHiredYear += count
    industry.tempsHiredEver += count
    // Stacking temps while crunching burns goodwill.
    val stack = temps().size
    val abuseBump = (0.04 + 0.02 * count) * (if (crunch) 1.4 else 1.0) * (1 + stack * 0.08)
    industry.tempAbuse = (industry.tempAbuse + abuseBump * 0.35).clamped(0.0, 1.0)
    for (p in permanents()) {
        if (p.lane == Lane.GAMES || kind == TempKind.QA) p.morale = (p.morale - 0.02 * count * (if (crunch) 1.3 else 1.0)).clamped(0.0, 1.0)
    }
    addLog(
        "Hired ${hired.size} ${kind.title.lowercase()} through ${Format.weeks(weeks)}: ${hired.take(3).joinToString()}" +
            if (hired.size > 3) "…" else "",
        LogTone.NEUTRAL, Lane.GAMES.takeIf { Lane.GAMES in lanes },
    )
    feedOnIndustry("temp_hire", kind.title)
    if (industry.tempsHiredYear >= 8 && contractPressScale() >= 0.55) {
        maybeTempScandal("A trade rag clocks how many contractors ${companyName} burned through this year.")
    }
}

fun GameState.endContractBlocker(employeeId: Int): String? {
    val e = staff.firstOrNull { it.id == employeeId } ?: return "They're gone."
    if (!e.isTemp(week)) return "Not on a temp contract."
    return null
}

fun GameState.endContractCost(employeeId: Int, mode: ContractExit): Double {
    val e = staff.firstOrNull { it.id == employeeId } ?: return 0.0
    return when (mode) {
        ContractExit.FAIR -> e.salary * 2
        ContractExit.QUIET -> 0.0
        ContractExit.CUT -> 0.0
    }
}

/** End a temp early — fair / quiet / cut-loose. Scandal risk scales with era + abuse. */
fun GameState.endContract(employeeId: Int, mode: ContractExit) {
    requireLive()
    endContractBlocker(employeeId)?.let { throw notAvailable(it) }
    val e = staff.first { it.id == employeeId }
    val cost = endContractCost(employeeId, mode)
    if (cost > 0) {
        if (cash < cost) throw insufficientFunds("Needs ${money(cost)}.")
        spend(cost, e.lane)
    }
    staff = staff - e
    when (mode) {
        ContractExit.FAIR -> {
            industry.tempAbuse = (industry.tempAbuse - 0.04).clamped(0.0, 1.0)
            for (p in permanents().filter { it.lane == e.lane }) p.morale = min(1.0, p.morale + 0.02)
            addLog("Ended ${e.name}'s contract fairly.", LogTone.NEUTRAL, e.lane)
            feedOnIndustry("temp_fair", e.name)
        }
        ContractExit.QUIET -> {
            industry.tempAbuse = (industry.tempAbuse + 0.03 * contractPressScale()).clamped(0.0, 1.0)
            for (p in permanents().filter { it.lane == e.lane }) p.morale = (p.morale - 0.025).clamped(0.0, 1.0)
            addLog("Quietly ended ${e.name}'s contract.", LogTone.NEUTRAL, e.lane)
            feedOnIndustry("temp_quiet", e.name)
        }
        ContractExit.CUT -> {
            industry.tempAbuse = (industry.tempAbuse + 0.08 * contractPressScale()).clamped(0.0, 1.0)
            for (p in permanents()) p.morale = (p.morale - 0.04).clamped(0.0, 1.0)
            addLog("Cut ${e.name} loose mid-contract.", LogTone.BAD, e.lane)
            feedOnIndustry("temp_cut", e.name)
            if (industry.tempAbuse >= 0.35 || rng.chance(0.15 * contractPressScale())) {
                maybeTempScandal("${companyName} dumped contractors mid-crunch. The forums noticed.")
            }
        }
    }
}

private fun GameState.rollTempYear() {
    if (industry.tempsYearMark != calendarYear) {
        industry.tempsYearMark = calendarYear
        industry.tempsHiredYear = 0
    }
}

private fun GameState.maybeTempScandal(note: String) {
    if (contractPressScale() < 0.4) return
    if (rng.chance(0.35 * contractPressScale() * (0.4 + industry.tempAbuse))) {
        reputation.scar(ScarKind.SCANDAL, 0.12 + 0.15 * industry.tempAbuse, week, note)
        stats.scandals += 1
        publish(
            NewsDesk.INDUSTRY, techOutlet(Lane.GAMES, rng),
            "Contract labour storm around $companyName",
            note, LogTone.BAD, Lane.GAMES, tag = NewsTag.NEWS,
        )
        feedOnIndustry("temp_scandal", null)
        industry.tempAbuse = (industry.tempAbuse * 0.7).clamped(0.0, 1.0)
    }
}

/** Weekly: expire contracts, morale under crunch+temps, soft scandal roll. */
internal fun GameState.contractLabourWeek() {
    rollTempYear()
    val expired = staff.filter { it.contractUntil >= 0 && it.contractUntil < week }
    for (e in expired) {
        staff = staff - e
        addLog("${e.name}'s contract ended.", LogTone.NEUTRAL, e.lane)
        feedOnIndustry("temp_expire", e.name)
    }
    val active = temps()
    if (active.isEmpty()) {
        industry.tempAbuse = (industry.tempAbuse - 0.008).clamped(0.0, 1.0)
        return
    }
    // Permanents resent a contractor pile-on during crunch.
    if (crunch) {
        for (p in permanents()) {
            p.morale = (p.morale - 0.008 * active.size.coerceAtMost(6)).clamped(0.0, 1.0)
        }
        industry.tempAbuse = (industry.tempAbuse + 0.006 * active.size * contractPressScale()).clamped(0.0, 1.0)
    }
    if (industry.tempAbuse >= 0.55 && rng.chance(0.02 * contractPressScale())) {
        maybeTempScandal("Ex-contractors talk: crunch culture at $companyName.")
    }
}

/** Output / quality / bug modifiers while temps are on the Games team. */
internal fun GameState.applyTempLabourMods(lane: Lane, mods: LaneModifiers) {
    if (lane != Lane.GAMES) return
    val t = temps(Lane.GAMES)
    if (t.isEmpty()) return
    val artists = t.count { it.role == Role.DESIGNER }
    val qa = t.count { it.role == Role.ENGINEER }
    val n = t.size.toDouble()
    // Faster ship / coverage.
    mods.output *= 1.0 + 0.04 * n.coerceAtMost(8.0) + if (crunch) 0.03 else 0.0
    // Quality risk — artists pad design points poorly; QA finds bugs but shallow sense.
    mods.quality -= 0.35 * artists + 0.15 * qa
    mods.bugs *= 1.0 + 0.06 * artists - 0.04 * qa.coerceAtMost(4)
    mods.focus -= 0.02 * n.coerceAtMost(6.0)
}

// ─── 2. Streaming / Let's Play era ───────────────────────────────────────────

fun GameState.creatorOf(name: String): CreatorStanding? =
    industry.creators.firstOrNull { it.name.equals(name, ignoreCase = true) }

fun GameState.ensureCreator(name: String): CreatorStanding =
    creatorOf(name) ?: CreatorStanding(name).also { industry.creators += it }

fun GameState.streamCourtCost(releaseId: Int): Double = scaled(12_000.0) * (0.7 + 0.3 * streamEraStrength())

fun GameState.streamCourtBlocker(releaseId: Int): String? {
    val r = games?.release(releaseId) ?: return "No such game."
    if (streamEraStrength() <= 0) return "Creators aren't a launch force yet — try again in the late 2000s."
    if (!r.onSale) return "${r.title} isn't selling."
    if (cash < streamCourtCost(releaseId)) return "Costs ${money(streamCourtCost(releaseId))}."
    return null
}

private fun GameState.streamerNames(): List<String> = when {
    calendarYear < 2011 -> listOf("AngryJoe", "TotalBiscuit", "ProtonJon", "a Let's Play channel")
    calendarYear < 2016 -> listOf("PewDiePie", "Markiplier", "Jacksepticeye", "GameGrumps", "a Twitch partner")
    else -> listOf("xQc", "Pokimane", "Shroud", "Ludwig", "a huge Twitch streamer", "a TikTok clipper")
}

/**
 * Court a creator for a live title — hype and sales if they like you; review-bomb-adjacent
 * noise or embargo drama if the relationship sours.
 */
fun GameState.courtStreamer(releaseId: Int, name: String? = null) {
    requireLive()
    require(Lane.GAMES)
    streamCourtBlocker(releaseId)?.let { throw notAvailable(it) }
    val r = games!!.release(releaseId)!!
    val who = name ?: rng.pick(streamerNames())
    spend(streamCourtCost(releaseId), Lane.GAMES)
    val c = ensureCreator(who)
    c.lastCourtWeek = week
    val warm = c.favor >= -0.1
    if (warm) {
        c.favor = (c.favor + 0.18).clamped(-1.0, 1.0)
        c.plays += 1
        r.weeklySales = max(r.weeklySales * (1.25 + 0.2 * streamEraStrength()), r.weeklySales + 2_000)
        games!!.fans += max(200.0, games!!.fans * 0.02 * streamEraStrength())
        if (r.userScore > 0) r.userScore = (r.userScore + 0.08 * streamEraStrength()).clamped(1.0, 10.0)
        addLog("Courted $who for ${r.title}. Chat is watching.", LogTone.GOOD, Lane.GAMES)
        publish(
            NewsDesk.GAMES, "Twitch",
            "$who queues up ${r.title}",
            "$companyName sent keys and talking points. Viewership follows.",
            LogTone.GOOD, Lane.GAMES, tag = NewsTag.NEWS,
        )
        feedOnIndustry("stream_court", who, r.title)
        // Soft chance of embargo drama even on a warm court.
        if (rng.chance(0.08 * streamEraStrength())) enqueueStreamerDrama(r, who, "embargo")
    } else {
        c.favor = (c.favor - 0.05).clamped(-1.0, 1.0)
        r.weeklySales *= 0.95
        addLog("$who iced the ${r.title} pitch on stream.", LogTone.BAD, Lane.GAMES)
        feedOnIndustry("stream_snub", who, r.title)
        if (rng.chance(0.25 * streamEraStrength())) enqueueStreamerDrama(r, who, "bomb")
    }
}

@Serializable @SerialName("streamerEmbargo")
data class StreamerEmbargo(val releaseId: Int, val name: String) : EventKind

@Serializable @SerialName("streamerNoise")
data class StreamerNoise(val releaseId: Int, val name: String) : EventKind

internal fun GameState.enqueueStreamerDrama(r: GameRelease, who: String, kind: String) {
    when (kind) {
        "embargo" -> enqueue(StreamerEmbargo(r.id, who))
        else -> enqueue(StreamerNoise(r.id, who))
    }
}

/** Weekly creator chatter once the era is on. */
internal fun GameState.streamingWeek() {
    val era = streamEraStrength()
    if (era <= 0) return
    val g = games ?: return
    // Decay favor gently.
    for (c in industry.creators) c.favor *= 0.995
    val hot = g.releases.filter { it.onSale && it.score >= 5.5 }
    if (hot.isEmpty()) return
    // Organic streamer pickup — denser than the old 1% roll once Twitch exists.
    if (rng.chance(0.012 * era)) {
        val r = rng.pick(hot)
        val who = rng.pick(streamerNames())
        val c = ensureCreator(who)
        enqueue(EventKind.Streamer(r.id, who))
        if (c.favor < -0.35 && rng.chance(0.3)) enqueueStreamerDrama(r, who, "bomb")
    }
    // Review-bomb-adjacent clip drama on bombed or low-trust titles.
    val fragile = hot.filter { it.bombed || it.userScore in 1.0..5.5 || g.fanTrust < 0.55 }
    if (fragile.isNotEmpty() && rng.chance(0.006 * era)) {
        val r = rng.pick(fragile)
        enqueueStreamerDrama(r, rng.pick(streamerNames()), "bomb")
    }
}

/** Sales lift from warm creators who've played this title recently. */
fun GameState.streamerSalesLift(r: GameRelease): Double {
    val era = streamEraStrength()
    if (era <= 0) return 0.0
    val warm = industry.creators.filter { it.favor > 0.2 && week - it.lastCourtWeek < 26 }
    if (warm.isEmpty()) return 0.0
    return (0.01 * warm.size * era * (1 + warm.sumOf { it.favor } / warm.size)).coerceAtMost(0.06)
}

// ─── 3. Platform holder politics ─────────────────────────────────────────────

fun GameState.holderOf(name: String): HolderStanding? =
    industry.platforms.firstOrNull { it.holder.equals(name, ignoreCase = true) }

fun GameState.ensureHolder(name: String): HolderStanding =
    holderOf(name) ?: HolderStanding(name).also { industry.platforms += it }

/** Active console / store holders you can court this year. */
fun GameState.courtAbleHolders(): List<String> {
    val fromPlatforms = platformsOnSale.map { it.makerIn(calendarYear) }.distinct()
        .filter { it !in listOf("You", "Apple and Google", "The open web") }
    val stores = listOfNotNull(
        "Valve".takeIf { calendarYear >= 2004 },
        "Epic Games".takeIf { calendarYear >= 2018 },
    )
    return (fromPlatforms + stores + platformHoldersIn(calendarYear)).distinct()
}

fun GameState.holderFavor(holder: String): Double = holderOf(holder)?.favor ?: 0.0

fun GameState.certDelayWeeks(platforms: List<Platform>): Int {
    val consoles = platforms.filter { it != Platform.PC && it.fee > 0 }
    if (consoles.isEmpty()) return 0
    var delay = 0
    for (p in consoles) {
        val h = ensureHolder(p.maker)
        val base = when {
            calendarYear < 1995 -> 2
            calendarYear < 2005 -> 3
            else -> 4
        }
        delay = max(delay, (base - (h.favor * 4).roundToInt() + h.certBacklog).coerceAtLeast(0))
    }
    return delay
}

fun GameState.chaseCertCost(team: Int = 1): Double = scaled(25_000.0)

fun GameState.chaseCertBlocker(team: Int = 1): String? {
    val p = games?.projectOf(team) ?: return "No game in development."
    if (p.concept.platforms.none { it != Platform.PC && it.fee > 0 }) return "No console cert to chase — PC ships when ready."
    if (p.certClearWeek >= 0 && p.certClearWeek <= week) return "Cert is already clear."
    if (cash < chaseCertCost(team)) return "Costs ${money(chaseCertCost(team))}."
    return null
}

/** Pay to pull console certification forward — burns favor less than missing a slot. */
fun GameState.chaseCert(team: Int = 1) {
    requireLive()
    require(Lane.GAMES)
    chaseCertBlocker(team)?.let { throw notAvailable(it) }
    val p = games!!.projectOf(team)!!
    spend(chaseCertCost(team), Lane.GAMES)
    if (p.certClearWeek < 0) {
        p.certClearWeek = week + certDelayWeeks(p.concept.platforms).coerceAtLeast(1)
    }
    val pull = 3 + (holderFavor(p.concept.platforms.first { it != Platform.PC }.maker) * 2).roundToInt().coerceAtLeast(0)
    p.certClearWeek = max(week, p.certClearWeek - pull)
    for (pl in p.concept.platforms.filter { it != Platform.PC }) {
        val h = ensureHolder(pl.maker)
        h.favor = (h.favor + 0.04).clamped(-1.0, 1.0)
        h.certBacklog = (h.certBacklog - 1).coerceAtLeast(0)
    }
    addLog("Chased cert for ${p.title}: clear ${if (p.certClearWeek <= week) "now" else GameClock.label(p.certClearWeek)}.", LogTone.NEUTRAL, Lane.GAMES)
    feedOnIndustry("cert_chase", p.title)
}

fun GameState.featuringCost(holder: String): Double = scaled(40_000.0) * (1.1 - holderFavor(holder) * 0.2)

fun GameState.featuringBlocker(holder: String): String? = when {
    featuringEraStrength() <= 0 -> "Store featuring isn't a lever yet — digital storefronts come later."
    holderOf(holder)?.featuringUntil?.let { it >= week } == true -> "Already featured with $holder."
    cash < featuringCost(holder) -> "Costs ${money(featuringCost(holder))}."
    else -> null
}

/** Chase Store featuring — visibility now, margin and a favor debt. Era-gated. */
fun GameState.chaseFeaturing(holder: String, weeks: Int = 8) {
    requireLive()
    require(Lane.GAMES)
    featuringBlocker(holder)?.let { throw notAvailable(it) }
    spend(featuringCost(holder), Lane.GAMES)
    val h = ensureHolder(holder)
    h.featuringUntil = week + weeks
    h.favor = (h.favor + 0.06).clamped(-1.0, 1.0)
    // Soft margin hit via a temporary pressure on Games demand swap — applied in lift helpers.
    addLog("Featured with $holder for ${Format.weeks(weeks)}. Front-page visibility, thinner margin.", LogTone.GOOD, Lane.GAMES)
    publish(
        NewsDesk.GAMES, holder,
        "$companyName lands $holder featuring",
        "A front-of-store slot for $weeks weeks — discovery up, cut up.",
        LogTone.GOOD, Lane.GAMES, tag = NewsTag.NEWS,
    )
    feedOnIndustry("featuring", holder)
}

fun GameState.logoDealCost(holder: String): Double = scaled(60_000.0)

fun GameState.logoDealBlocker(holder: String): String? = when {
    holderOf(holder)?.logoUntil?.let { it >= week } == true -> "Logo deal with $holder still runs."
    cash < logoDealCost(holder) -> "Costs ${money(logoDealCost(holder))}."
    else -> null
}

/**
 * "Get the logo on the box" / first-party-friendly deal — big visibility and favor,
 * lasting margin cut and a cert backlog bump (they own more of the schedule).
 */
fun GameState.signLogoDeal(holder: String, weeks: Int = 52) {
    requireLive()
    require(Lane.GAMES)
    logoDealBlocker(holder)?.let { throw notAvailable(it) }
    spend(logoDealCost(holder), Lane.GAMES)
    val h = ensureHolder(holder)
    h.logoUntil = week + weeks
    h.logoCut = 0.08
    h.favor = (h.favor + 0.2).clamped(-1.0, 1.0)
    h.certBacklog += 1
    addLog(
        "Signed a $holder logo deal through ${GameClock.label(h.logoUntil)}. Box presence and favor — thinner cut, they set more of the clock.",
        LogTone.MILESTONE, Lane.GAMES,
    )
    publish(
        NewsDesk.INDUSTRY, holder,
        "$companyName goes $holder-friendly",
        "Logo on the box, marketing muscle, and a tighter leash on cert.",
        LogTone.GOOD, Lane.GAMES, tag = NewsTag.NEWS,
    )
    feedOnIndustry("logo_deal", holder)
}

/** Net-share / sales modifiers from active featuring + logo deals for platforms on a title. */
fun GameState.platformPoliticsKeep(platforms: List<Platform>, stores: Set<Storefront>? = null): Double {
    var keep = 1.0
    for (p in platforms) {
        val h = holderOf(p.maker) ?: continue
        if (h.logoUntil >= week) keep *= (1.0 - h.logoCut)
    }
    // Featuring costs a store cut bump while active.
    for (h in industry.platforms) {
        if (h.featuringUntil < week) continue
        if (featuringEraStrength() <= 0) continue
        keep *= 0.94
    }
    stores?.forEach { s ->
        val owner = when (s) {
            Storefront.STEAM -> "Valve"
            Storefront.EPIC -> "Epic Games"
            else -> null
        } ?: return@forEach
        val h = holderOf(owner) ?: return@forEach
        if (h.featuringUntil >= week) keep *= 0.96
        if (h.logoUntil >= week) keep *= (1.0 - h.logoCut * 0.5)
    }
    return keep.clamped(0.55, 1.0)
}

fun GameState.platformPoliticsSalesLift(platforms: List<Platform>, stores: Set<Storefront>? = null): Double {
    var lift = 0.0
    for (p in platforms) {
        val h = holderOf(p.maker) ?: continue
        if (h.logoUntil >= week) lift += 0.08 + 0.04 * h.favor
        if (h.featuringUntil >= week) lift += 0.05 * featuringEraStrength()
        lift += h.favor * 0.02
    }
    stores?.forEach { s ->
        val owner = when (s) {
            Storefront.STEAM -> "Valve"
            Storefront.EPIC -> "Epic Games"
            else -> null
        } ?: return@forEach
        val h = holderOf(owner) ?: return@forEach
        if (h.featuringUntil >= week) lift += 0.06 * featuringEraStrength()
    }
    return lift.coerceAtMost(0.2)
}

/** Seed cert clear week when a project starts (consoles only). */
internal fun GameState.seedCertSchedule(p: GameProject) {
    val delay = certDelayWeeks(p.concept.platforms)
    p.certClearWeek = if (delay <= 0) week else week + delay
}

fun GameState.launchCertBlocker(team: Int = 1): String? {
    val p = games?.projectOf(team) ?: return null
    if (p.certClearWeek < 0) return null
    if (p.certClearWeek > week) {
        return "Platform cert clears ${GameClock.label(p.certClearWeek)}. Chase cert or wait."
    }
    return null
}

/** Weekly favor drift and featuring expiry notes. */
internal fun GameState.platformPoliticsWeek() {
    for (h in industry.platforms) {
        h.favor = (h.favor * 0.997).clamped(-1.0, 1.0)
        if (h.featuringUntil == week - 1) {
            addLog("Featuring with ${h.holder} ended.", LogTone.NEUTRAL, Lane.GAMES)
        }
        if (h.logoUntil == week - 1) {
            h.logoCut = 0.0
            addLog("The ${h.holder} logo deal expired.", LogTone.NEUTRAL, Lane.GAMES)
        }
    }
}

// ─── Feed ────────────────────────────────────────────────────────────────────

/** Refresh per-release industry sales lift before the Games tick. */
internal fun GameState.refreshIndustryLifts() {
    val g = games ?: return
    for (r in g.releases) {
        r.industryLift = (streamerSalesLift(r) + platformPoliticsSalesLift(r.concept.platforms, r.stores.toSet().ifEmpty { null }))
            .coerceAtMost(0.12)
    }
}

// ─── Streamer drama events (extend Events, not a parallel sim) ───────────────

internal fun GameState.describeStreamerEmbargo(k: StreamerEmbargo): Description? {
    val r = games?.release(k.releaseId) ?: return null
    return Description(
        "${k.name} broke the ${r.title} embargo",
        "They went live a day early. The store page isn't ready and rival creators are furious.",
        listOf(
            choice("Lean in", "Ride the chaos: more eyes, bruised relationships."),
            choice("Apologize & delay keys", "${money(scaled(8_000.0))}. Cool the room; smaller bump."),
        ),
        Lane.GAMES,
    )
}

internal fun GameState.resolveStreamerEmbargo(k: StreamerEmbargo, choice: Int) {
    val r = games?.release(k.releaseId) ?: return
    val c = ensureCreator(k.name)
    if (choice == 0) {
        r.weeklySales = max(r.weeklySales * 1.35, r.weeklySales + 3_000)
        c.favor = (c.favor + 0.05).clamped(-1.0, 1.0)
        for (other in industry.creators.filter { it.name != k.name }) other.favor = (other.favor - 0.08).clamped(-1.0, 1.0)
        games!!.fanTrust = (games!!.fanTrust - 0.02).clamped(0.3, 1.2)
        addLog("${k.name} broke the ${r.title} embargo. Sales spiked; other creators are mad.", LogTone.NEUTRAL, Lane.GAMES)
        feedOnIndustry("stream_court", k.name, r.title)
    } else {
        spend(scaled(8_000.0), Lane.GAMES)
        r.weeklySales *= 1.1
        c.favor = (c.favor - 0.04).clamped(-1.0, 1.0)
        addLog("You apologized for the ${r.title} embargo mess and slowed key drops.", LogTone.NEUTRAL, Lane.GAMES)
    }
}

internal fun GameState.describeStreamerNoise(k: StreamerNoise): Description? {
    val r = games?.release(k.releaseId) ?: return null
    return Description(
        "${k.name} is dunking on ${r.title}",
        "Clips of bugs and bad takes are spreading. It isn't a full review bomb — yet — but chat is loud.",
        listOf(
            choice("Send a patch note & gift", "${money(scaled(6_000.0))}. Own it on stream."),
            choice("Ignore the noise", "Hope it blows over. Risk of a real bomb."),
        ),
        Lane.GAMES,
    )
}

internal fun GameState.resolveStreamerNoise(k: StreamerNoise, choice: Int) {
    val g = games ?: return
    val r = g.release(k.releaseId) ?: return
    val c = ensureCreator(k.name)
    if (choice == 0) {
        spend(scaled(6_000.0), Lane.GAMES)
        c.favor = (c.favor + 0.1).clamped(-1.0, 1.0)
        if (r.userScore > 0) r.userScore = (r.userScore + 0.15).clamped(1.0, 10.0)
        addLog("You answered ${k.name} on ${r.title}. Chat cooled off.", LogTone.GOOD, Lane.GAMES)
    } else {
        c.favor = (c.favor - 0.12).clamped(-1.0, 1.0)
        r.weeklySales *= 0.92
        if (rng.chance(0.35 * streamEraStrength())) {
            r.bombed = true
            if (r.userScore > 0) r.userScore = (r.userScore - 0.6).clamped(1.0, 10.0)
            g.fanTrust = (g.fanTrust - 0.04).clamped(0.3, 1.2)
            addLog("${k.name}'s clips tipped ${r.title} into review-bomb-adjacent noise.", LogTone.BAD, Lane.GAMES)
            feedOnIndustry("stream_snub", k.name, r.title)
        } else {
            addLog("You ignored ${k.name}'s dunks on ${r.title}. It mostly blew over.", LogTone.NEUTRAL, Lane.GAMES)
        }
    }
}

