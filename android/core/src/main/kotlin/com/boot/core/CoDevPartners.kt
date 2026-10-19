package com.boot.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
/*
 * Co-dev partners (3.34.9): split a large / AAA project with a rival, owned subsidiary,
 * or market studio. Shared cost, workload, credit and royalties — shared glory on hits,
 * shared blame on delays, bugs, flops and scandals. Extends Development / Projects /
 * OwnedStudios / Rivals / Deals / News / Feed — not a parallel project system.
 */

// ─── Book & terms (old saves default empty / null) ───────────────────────────

/** Lasting co-dev relationships. Defaults keep old saves loadable. */
@Serializable
class CoDevBook {
    val standing: MutableList<CoDevStanding> = mutableListOf()
    /** Lifetime co-dev ships — for balance flavour. */
    var ships: Int = 0
    var hits: Int = 0
    var flops: Int = 0
    var disputes: Int = 0
    var nextOfferId: Int = 1
}

@Serializable
data class CoDevStanding(
    val partner: String,
    /** −1…1 how they feel about co-developing with you. */
    var favor: Double = 0.0,
    var projects: Int = 0,
    var hits: Int = 0,
    var flops: Int = 0,
    var lastWeek: Int = -1_000,
)

/** Who the partner is, relative to your studio. */
@Serializable
enum class CoDevKind(val title: String, val blurb: String) {
    RIVAL("Rival", "A competing studio. Shared glory cools heat; shared blame reignites it."),
    SUBSIDIARY("Owned studio", "One of yours. Cost stays in the family; credit still splits in the press."),
    MARKET("Market co-dev", "An independent or publisher studio available for hire-on."),
}

@Serializable
enum class CoDevPhase {
    /** Offer on the table, not yet signed onto a project. */
    OFFER,
    /** Active on a live GameProject. */
    ACTIVE,
    /** Partner walked mid-project. */
    QUIT,
    /** Partner sued after a blow-up. */
    SUED,
    /** Project shipped (or cancelled) with the deal closed. */
    DONE,
}

/**
 * Terms on a [GameProject] (or a pending [CoDevOffer]).
 * Shares are the partner's cut: cost they pay, work they contribute, credit they claim, royalties they take.
 */
@Serializable
data class CoDevTerms(
    val partner: String,
    val kind: CoDevKind,
    /** 0.2…0.55 — partner pays this fraction of weekly burn. */
    val costShare: Double = 0.4,
    /** 0.2…0.5 — extra progress as a fraction of your weekly work. */
    val workShare: Double = 0.35,
    /** 0.2…0.5 — how much brand glory / blame they share. Mutable when credit is clawed back. */
    var creditShare: Double = 0.4,
    /** 0.15…0.45 — cut of net sales they take after launch. */
    val royaltyShare: Double = 0.3,
    var phase: CoDevPhase = CoDevPhase.ACTIVE,
    /** Work points the partner has contributed. */
    var contribution: Double = 0.0,
    /** Cash the partner has covered. */
    var costPaid: Double = 0.0,
    var weeksActive: Int = 0,
    /** Soft-poached credit attempts. */
    var creditGrabs: Int = 0,
)

/** A proposal waiting for an answer — yours to them, or theirs to you. */
@Serializable
data class CoDevOffer(
    val id: Int,
    val partner: String,
    val kind: CoDevKind,
    val team: Int,
    val costShare: Double,
    val workShare: Double,
    val creditShare: Double,
    val royaltyShare: Double,
    val expiresWeek: Int,
    /** True when they approached you. */
    val incoming: Boolean = false,
)

@Serializable @SerialName("coDevReply")
data class CoDevReply(val offerId: Int, val partner: String, val accept: Boolean) : EventKind

@Serializable @SerialName("coDevIncoming")
data class CoDevIncoming(val offerId: Int) : EventKind

@Serializable @SerialName("coDevDispute")
data class CoDevDispute(val team: Int, val reason: CoDevDisputeReason) : EventKind

@Serializable
enum class CoDevDisputeReason(val title: String) {
    DELAY("Delay fallout"),
    BUGS("Bug blame"),
    FLOP("Shared flop"),
    CREDIT("Credit grab"),
    SCOPE("Scope fight"),
}

// ─── Lookups ─────────────────────────────────────────────────────────────────

fun GameState.coDevOf(partner: String): CoDevStanding? =
    coDev.standing.firstOrNull { it.partner == partner }

fun GameState.ensureCoDevStanding(partner: String): CoDevStanding =
    coDevOf(partner) ?: CoDevStanding(partner).also { coDev.standing += it }

fun GameState.coDevFavor(partner: String): Double = coDevOf(partner)?.favor ?: 0.0

/** LARGE / AAA only — co-dev is for mega-projects. */
fun GameProject.isCoDevEligible(): Boolean =
    concept.size == GameSize.LARGE || concept.size == GameSize.AAA

fun GameProject.activeCoDev(): CoDevTerms? =
    coDev?.takeIf { it.phase == CoDevPhase.ACTIVE }

// ─── Candidates & blockers ───────────────────────────────────────────────────

data class CoDevCandidate(val name: String, val kind: CoDevKind, val favor: Double, val note: String)

/** Studios you can invite onto [team]'s project. */
fun GameState.coDevCandidates(team: Int = 1): List<CoDevCandidate> {
    val g = games ?: return emptyList()
    val p = g.projectOf(team) ?: return emptyList()
    if (!p.isCoDevEligible() || p.activeCoDev() != null) return emptyList()
    if (g.coDevOffers.any { it.team == team }) return emptyList()
    val out = mutableListOf<CoDevCandidate>()
    val taken = mutableSetOf<String>()
    // Owned subsidiaries first.
    for (sub in g.subsidiaries) {
        if (sub.name in taken) continue
        taken += sub.name
        val favor = coDevFavor(sub.name)
        out += CoDevCandidate(
            sub.name, CoDevKind.SUBSIDIARY, favor,
            "Morale ${Format.percent(sub.morale)}. ${if (sub.project != null) "Busy on ${sub.project!!.title}." else "Available."}",
        )
    }
    // Active games rivals.
    for (r in gameRivals.filter { it.quietUntil <= week && !it.absorbed }) {
        if (r.name in taken) continue
        taken += r.name
        val favor = coDevFavor(r.name)
        out += CoDevCandidate(
            r.name, CoDevKind.RIVAL, favor,
            "Heat ${Format.percent(r.heat)}. ${r.blurb.ifEmpty { "Competing studio." }}",
        )
    }
    // Market studios not already owned / rivalled this run.
    for (s in Studios) {
        if (s.name in taken) continue
        if (g.owns(s.name)) continue
        if (g.rivalOwned.containsKey(s.name)) continue
        if (!studioActive(s.name, calendarYear)) continue
        taken += s.name
        val favor = coDevFavor(s.name)
        out += CoDevCandidate(
            s.name, CoDevKind.MARKET, favor,
            if (s.indie) "Indie co-dev hire." else "Publisher / mid-size co-dev.",
        )
    }
    return out.sortedWith(compareBy({ it.kind.ordinal }, { -it.favor }, { it.name }))
}

fun GameState.proposeCoDevBlocker(
    team: Int,
    partner: String,
    costShare: Double = 0.4,
    workShare: Double = 0.35,
    creditShare: Double = 0.4,
    royaltyShare: Double = 0.3,
): String? {
    val g = games ?: return "You need a game studio."
    val p = g.projectOf(team) ?: return "Nothing in development."
    if (!p.isCoDevEligible()) return "Co-dev is for large and AAA projects."
    if (p.activeCoDev() != null) return "Already partnered with ${p.coDev!!.partner}."
    if (p.deal != null) return "Publisher deals and co-dev don't stack."
    if (g.coDevOffers.any { it.team == team }) return "Waiting on a co-dev reply."
    if (costShare !in 0.2..0.55) return "Cost share must be 20–55%."
    if (workShare !in 0.2..0.5) return "Workload share must be 20–50%."
    if (creditShare !in 0.2..0.5) return "Credit share must be 20–50%."
    if (royaltyShare !in 0.15..0.45) return "Royalty share must be 15–45%."
    val cand = coDevCandidates(team).firstOrNull { it.name == partner }
        ?: return "$partner isn't available for co-dev."
    if (cand.kind == CoDevKind.SUBSIDIARY) {
        val sub = g.subsidiaries.firstOrNull { it.name == partner }
        if (sub?.project != null) return "${partner} is already making ${sub.project!!.title}."
    }
    if (cand.favor < -0.55) return "$partner won't take your calls after the last blow-up."
    return null
}

/** Default balanced split for UI / autopilot. */
fun defaultCoDevSplit(kind: CoDevKind): Quadruple {
    return when (kind) {
        CoDevKind.SUBSIDIARY -> Quadruple(0.45, 0.4, 0.35, 0.25)
        CoDevKind.RIVAL -> Quadruple(0.4, 0.35, 0.4, 0.32)
        CoDevKind.MARKET -> Quadruple(0.35, 0.35, 0.4, 0.35)
    }
}

/** Tiny named tuple so call sites stay readable. */
data class Quadruple(val cost: Double, val work: Double, val credit: Double, val royalty: Double)

/**
 * Propose co-dev on [team]'s project. Partner answers in a week or two ([CoDevReply]),
 * or you accept an incoming [CoDevIncoming] offer.
 */
fun GameState.proposeCoDev(
    team: Int,
    partner: String,
    costShare: Double = 0.4,
    workShare: Double = 0.35,
    creditShare: Double = 0.4,
    royaltyShare: Double = 0.3,
) {
    requireLive()
    require(Lane.GAMES)
    proposeCoDevBlocker(team, partner, costShare, workShare, creditShare, royaltyShare)?.let {
        throw notAvailable(it)
    }
    val g = games!!
    val cand = coDevCandidates(team).first { it.name == partner }
    val id = coDev.nextOfferId++
    val offer = CoDevOffer(
        id = id,
        partner = partner,
        kind = cand.kind,
        team = team,
        costShare = costShare,
        workShare = workShare,
        creditShare = creditShare,
        royaltyShare = royaltyShare,
        expiresWeek = week + rng.int(2, 4),
        incoming = false,
    )
    g.coDevOffers += offer
    addLog(
        "Proposed co-dev with $partner on ${g.projectOf(team)!!.title}: " +
            "${Format.percent(costShare)} cost · ${Format.percent(workShare)} work · " +
            "${Format.percent(creditShare)} credit · ${Format.percent(royaltyShare)} royalties.",
        LogTone.NEUTRAL, Lane.GAMES,
    )
    feedOnCoDev("propose", partner, g.projectOf(team)!!.title)
    // Schedule their answer as a blocking event soon — soft delay via later enqueue in week tick.
}

fun GameState.acceptCoDevOfferBlocker(offerId: Int): String? {
    val g = games ?: return "No studio."
    val offer = g.coDevOffers.firstOrNull { it.id == offerId } ?: return "That offer is gone."
    if (!offer.incoming) return "You're waiting on their reply."
    val p = g.projectOf(offer.team) ?: return "The project is gone."
    if (!p.isCoDevEligible()) return "Only large / AAA projects."
    if (p.activeCoDev() != null) return "Already partnered."
    if (p.deal != null) return "Publisher deals and co-dev don't stack."
    return null
}

/** Accept an incoming co-dev offer from another studio. */
fun GameState.acceptCoDevOffer(offerId: Int) {
    requireLive()
    acceptCoDevOfferBlocker(offerId)?.let { throw notAvailable(it) }
    val g = games!!
    val offer = g.coDevOffers.first { it.id == offerId }
    g.coDevOffers.removeAll { it.id == offerId || it.team == offer.team }
    attachCoDev(offer.team, offer.toTerms())
}

fun GameState.declineCoDevOffer(offerId: Int) {
    requireLive()
    val g = games ?: return
    val offer = g.coDevOffers.firstOrNull { it.id == offerId } ?: throw invalid("That offer is gone.")
    g.coDevOffers.remove(offer)
    val stand = ensureCoDevStanding(offer.partner)
    stand.favor = (stand.favor - 0.05).clamped(-1.0, 1.0)
    addLog("Turned down co-dev with ${offer.partner}.", LogTone.NEUTRAL, Lane.GAMES)
    feedOnCoDev("decline", offer.partner, null)
}

fun GameState.withdrawCoDevOffer(offerId: Int) {
    requireLive()
    val g = games ?: throw notAvailable("No studio.")
    val offer = g.coDevOffers.firstOrNull { it.id == offerId } ?: throw invalid("That offer is gone.")
    if (offer.incoming) throw invalid("That's their offer — decline it instead.")
    g.coDevOffers.remove(offer)
    addLog("Withdrew the co-dev pitch to ${offer.partner}.", LogTone.NEUTRAL, Lane.GAMES)
}

private fun CoDevOffer.toTerms() = CoDevTerms(
    partner = partner,
    kind = kind,
    costShare = costShare,
    workShare = workShare,
    creditShare = creditShare,
    royaltyShare = royaltyShare,
    phase = CoDevPhase.ACTIVE,
)

internal fun GameState.attachCoDev(team: Int, terms: CoDevTerms) {
    val g = games ?: return
    val p = g.projectOf(team) ?: return
    p.coDev = terms
    val stand = ensureCoDevStanding(terms.partner)
    stand.projects += 1
    stand.lastWeek = week
    stand.favor = (stand.favor + 0.08).clamped(-1.0, 1.0)
    addLog(
        "Co-dev locked with ${terms.partner} on ${p.title}. " +
            "They cover ${Format.percent(terms.costShare)} of burn and ${Format.percent(terms.workShare)} of the workload; " +
            "they take ${Format.percent(terms.royaltyShare)} of net and ${Format.percent(terms.creditShare)} of the credit.",
        LogTone.GOOD, Lane.GAMES,
    )
    publish(
        NewsDesk.INDUSTRY, techOutlet(Lane.GAMES, rng),
        "$companyName × ${terms.partner}: co-dev on ${p.title}",
        "A ${p.concept.size.lower} project split — shared cost, shared credit, shared risk.",
        LogTone.GOOD, Lane.GAMES, tag = NewsTag.NEWS, cover = p.newsCover(),
    )
    feedOnCoDev("accept", terms.partner, p.title)
}

// ─── Sim hooks ───────────────────────────────────────────────────────────────

/**
 * Apply partner cost/work inside [GamesStudio.develop].
 * Returns the multiplier on *your* weekly budget (1 − costShare) and extra progress points.
 */
internal fun GameProject.coDevBudgetFactor(): Double =
    activeCoDev()?.let { (1.0 - it.costShare).coerceIn(0.45, 1.0) } ?: 1.0

internal fun GameProject.coDevWorkBoost(paced: Double): Double {
    val cd = activeCoDev() ?: return 0.0
    val boost = paced * cd.workShare
    cd.contribution += boost
    return boost
}

internal fun GameProject.coDevRecordCost(fullBudget: Double) {
    val cd = activeCoDev() ?: return
    val theirs = fullBudget * cd.costShare
    cd.costPaid += theirs
}

/** Royalty keep after platform/publisher cuts — multiply into launch netShare. */
internal fun GameProject.coDevRoyaltyKeep(): Double =
    activeCoDev()?.let { (1.0 - it.royaltyShare).coerceIn(0.55, 1.0) } ?: 1.0

/** Weekly: offer replies, inbound pitches, dispute rolls, standing drift. */
internal fun GameState.coDevWeek() {
    val g = games ?: return
    // Expire stale offers.
    val expired = g.coDevOffers.filter { it.expiresWeek < week }
    for (o in expired) {
        g.coDevOffers.remove(o)
        if (!o.incoming) {
            addLog("${o.partner} let the co-dev pitch on ${g.projectOf(o.team)?.title ?: "the project"} go cold.", LogTone.NEUTRAL, Lane.GAMES)
            ensureCoDevStanding(o.partner).favor = (coDevFavor(o.partner) - 0.03).clamped(-1.0, 1.0)
        }
    }
    // Outgoing offers: roll accept/decline as CoDevReply events.
    for (o in g.coDevOffers.filter { !it.incoming }.toList()) {
        if (events.any { it.kind is CoDevReply && (it.kind as CoDevReply).offerId == o.id }) continue
        if (o.expiresWeek - week > 2 && rng.chance(0.55)) continue // sit a bit
        val favor = coDevFavor(o.partner)
        val base = when (o.kind) {
            CoDevKind.SUBSIDIARY -> 0.85
            CoDevKind.MARKET -> 0.55
            CoDevKind.RIVAL -> 0.4 - (gameRivals.firstOrNull { it.name == o.partner }?.heat ?: 0.3) * 0.25
        }
        val sweet = (if (o.royaltyShare >= 0.35) 0.08 else 0.0) - (if (o.costShare >= 0.5) 0.1 else 0.0)
        val odds = (base + favor * 0.35 + (reputation.score - 50) / 200 + sweet).clamped(0.08, 0.92)
        enqueue(CoDevReply(o.id, o.partner, accept = rng.chance(odds)))
    }
    // Rare inbound pitch on a lonely large/AAA project.
    if (g.coDevOffers.isEmpty() && rng.chance(0.012)) {
        for (team in listOf(1, 2)) {
            val p = g.projectOf(team) ?: continue
            if (!p.isCoDevEligible() || p.activeCoDev() != null || p.deal != null) continue
            val pool = coDevCandidates(team).filter { it.favor >= -0.2 }
            if (pool.isEmpty()) continue
            val cand = rng.pick(pool)
            val split = defaultCoDevSplit(cand.kind)
            // Slightly partner-favoring inbound terms.
            val id = coDev.nextOfferId++
            val offer = CoDevOffer(
                id = id, partner = cand.name, kind = cand.kind, team = team,
                costShare = (split.cost - 0.05).coerceIn(0.2, 0.55),
                workShare = split.work,
                creditShare = (split.credit + 0.05).coerceIn(0.2, 0.5),
                royaltyShare = (split.royalty + 0.05).coerceIn(0.15, 0.45),
                expiresWeek = week + 3,
                incoming = true,
            )
            g.coDevOffers += offer
            enqueue(CoDevIncoming(id))
            break
        }
    }
    // Active partnerships: tick + dispute risk.
    for (team in listOf(1, 2)) {
        val p = g.projectOf(team) ?: continue
        val cd = p.activeCoDev() ?: continue
        cd.weeksActive += 1
        // Soft relation drift from contribution balance.
        val expected = p.progress * cd.workShare / (1.0 + cd.workShare).coerceAtLeast(0.01)
        val ratio = if (expected > 1) cd.contribution / expected else 1.0
        val stand = ensureCoDevStanding(cd.partner)
        if (ratio < 0.7 && cd.weeksActive > 8) {
            stand.favor = (stand.favor - 0.01).clamped(-1.0, 1.0)
        } else if (ratio > 0.95) {
            stand.favor = (stand.favor + 0.004).clamped(-1.0, 1.0)
        }
        // Dispute: delays, nasty bugs, low favor, or mid-project scope fight.
        if (events.any { it.kind is CoDevDispute && (it.kind as CoDevDispute).team == team }) continue
        val bugHeavy = p.bugs > p.work * 0.12
        val delayed = p.delays > 0
        val tense = stand.favor < -0.15
        val chance = when {
            delayed && bugHeavy -> 0.08
            delayed || bugHeavy -> 0.04
            tense -> 0.03
            cd.weeksActive > 20 && rng.chance(0.5) -> 0.015
            else -> 0.006
        }
        if (rng.chance(chance)) {
            val reason = when {
                delayed -> CoDevDisputeReason.DELAY
                bugHeavy -> CoDevDisputeReason.BUGS
                stand.favor < 0 -> CoDevDisputeReason.CREDIT
                else -> CoDevDisputeReason.SCOPE
            }
            enqueue(CoDevDispute(team, reason))
        }
    }
}

/** After a release ships — glory or blame. Call from [launchGame]. */
internal fun GameState.coDevOnLaunch(p: GameProject, r: GameRelease) {
    val cd = p.coDev?.takeIf { it.phase == CoDevPhase.ACTIVE || it.phase == CoDevPhase.QUIT } ?: return
    r.coDevPartner = cd.partner
    r.coDevRoyalty = cd.royaltyShare
    cd.phase = CoDevPhase.DONE
    coDev.ships += 1
    val stand = ensureCoDevStanding(cd.partner)
    stand.lastWeek = week
    val hit = r.score >= 8.0
    val flop = r.score < 5.0
    if (hit) {
        coDev.hits += 1
        stand.hits += 1
        stand.favor = (stand.favor + 0.12 + cd.creditShare * 0.1).clamped(-1.0, 1.0)
        reputation.adjust(1.5 + cd.creditShare * 2.0)
        games!!.fanTrust = (games!!.fanTrust + 0.02).clamped(0.3, 1.2)
        // Rival heat cools; subsidiary morale rises.
        when (cd.kind) {
            CoDevKind.RIVAL -> gameRivals.firstOrNull { it.name == cd.partner }?.let {
                it.heat = (it.heat - 0.12).clamped(0.0, 1.0)
                it.reputation = (it.reputation + 0.06).clamped(0.0, 1.0)
            }
            CoDevKind.SUBSIDIARY -> games!!.subsidiaries.firstOrNull { it.name == cd.partner }?.let {
                it.morale = (it.morale + 0.08).clamped(0.0, 1.0)
            }
            CoDevKind.MARKET -> { /* favor already bumped */ }
        }
        publish(
            NewsDesk.GAMES, techOutlet(Lane.GAMES, rng),
            "${r.title}: shared glory for $companyName and ${cd.partner}",
            "Critics toast the co-dev. Both brands ride the wave — ${Format.percent(cd.creditShare)} of the credit goes their way.",
            LogTone.GOOD, Lane.GAMES, tag = NewsTag.REVIEW, cover = r.newsCover(),
        )
        feedOnCoDev("glory", cd.partner, r.title)
    } else if (flop) {
        coDev.flops += 1
        stand.flops += 1
        stand.favor = (stand.favor - 0.15 - cd.creditShare * 0.1).clamped(-1.0, 1.0)
        reputation.scar(ScarKind.FLOP, 0.08 + cd.creditShare * 0.06, week, "Co-dev flop with ${cd.partner}")
        games!!.fanTrust = (games!!.fanTrust - 0.03).clamped(0.3, 1.2)
        when (cd.kind) {
            CoDevKind.RIVAL -> gameRivals.firstOrNull { it.name == cd.partner }?.let {
                it.heat = (it.heat + 0.1).clamped(0.0, 1.0)
                it.reputation = (it.reputation - 0.05).clamped(0.0, 1.0)
            }
            CoDevKind.SUBSIDIARY -> games!!.subsidiaries.firstOrNull { it.name == cd.partner }?.let {
                it.morale = (it.morale - 0.1).clamped(0.0, 1.0)
            }
            CoDevKind.MARKET -> {}
        }
        publish(
            NewsDesk.GAMES, techOutlet(Lane.GAMES, rng),
            "${r.title} flop stains $companyName and ${cd.partner}",
            "Shared blame on a co-dev miss. Lawyers and PR teams are already drafting statements.",
            LogTone.BAD, Lane.GAMES, tag = NewsTag.REVIEW, cover = r.newsCover(),
        )
        feedOnCoDev("blame", cd.partner, r.title)
        // Mid-dev disputes only — post-ship blame is the news beat above.
    } else {
        stand.favor = (stand.favor + 0.03).clamped(-1.0, 1.0)
        publish(
            NewsDesk.INDUSTRY, techOutlet(Lane.GAMES, rng),
            "$companyName and ${cd.partner} ship ${r.title}",
            "A workmanlike co-dev release. Royalties split ${Format.percent(cd.royaltyShare)} their way.",
            LogTone.NEUTRAL, Lane.GAMES, tag = NewsTag.NEWS, cover = r.newsCover(),
        )
        feedOnCoDev("ship", cd.partner, r.title)
    }
}

/** Shared blame when a dated release slips — called from [resolveSlip]. */
internal fun GameState.coDevOnDelay(p: GameProject) {
    val cd = p.activeCoDev() ?: return
    val stand = ensureCoDevStanding(cd.partner)
    stand.favor = (stand.favor - 0.06).clamped(-1.0, 1.0)
    when (cd.kind) {
        CoDevKind.RIVAL -> gameRivals.firstOrNull { it.name == cd.partner }?.let {
            it.heat = (it.heat + 0.05).clamped(0.0, 1.0)
        }
        CoDevKind.SUBSIDIARY -> games?.subsidiaries?.firstOrNull { it.name == cd.partner }?.let {
            it.morale = (it.morale - 0.04).clamped(0.0, 1.0)
        }
        CoDevKind.MARKET -> {}
    }
    publish(
        NewsDesk.INDUSTRY, "GamesIndustry.biz",
        "${p.title} delay hits $companyName and ${cd.partner}",
        "Co-dev calendars slip together. Shared blame in the trade press.",
        LogTone.BAD, Lane.GAMES, tag = NewsTag.DELAY, cover = p.newsCover(),
    )
    feedOnCoDev("delay", cd.partner, p.title)
    if (rng.chance(0.35 + (0.2 - stand.favor).coerceAtLeast(0.0))) {
        enqueue(CoDevDispute(if (games?.project2 === p) 2 else 1, CoDevDisputeReason.DELAY))
    }
}

/** Cancel clears the partnership without a lawsuit by default. */
internal fun GameState.coDevOnCancel(p: GameProject) {
    val cd = p.coDev ?: return
    if (cd.phase == CoDevPhase.ACTIVE) {
        cd.phase = CoDevPhase.DONE
        ensureCoDevStanding(cd.partner).favor = (coDevFavor(cd.partner) - 0.08).clamped(-1.0, 1.0)
        addLog("Co-dev with ${cd.partner} ended when ${p.title} was cancelled.", LogTone.BAD, Lane.GAMES)
        feedOnCoDev("cancel", cd.partner, p.title)
    }
}

// ─── Events ──────────────────────────────────────────────────────────────────

internal fun GameState.describeCoDevReply(k: CoDevReply): Description? {
    val g = games ?: return null
    val offer = g.coDevOffers.firstOrNull { it.id == k.offerId } ?: return null
    val title = g.projectOf(offer.team)?.title ?: "the project"
    return if (k.accept) {
        Description(
            "${k.partner} accepts the co-dev",
            "They're in on $title: ${Format.percent(offer.costShare)} of cost, ${Format.percent(offer.workShare)} of work, " +
                "${Format.percent(offer.royaltyShare)} royalties, ${Format.percent(offer.creditShare)} credit.",
            listOf(choice("Lock it in", "Sign the split and get to work.")),
            Lane.GAMES,
        )
    } else {
        Description(
            "${k.partner} passes on co-dev",
            "They passed on $title. Favor cools a little; you can pitch someone else.",
            listOf(choice("Move on", "Withdraw the pitch and keep developing alone.")),
            Lane.GAMES,
        )
    }
}

internal fun GameState.resolveCoDevReply(k: CoDevReply, choice: Int) {
    val g = games ?: return
    val offer = g.coDevOffers.firstOrNull { it.id == k.offerId }
    if (offer == null) return
    g.coDevOffers.remove(offer)
    if (k.accept) {
        attachCoDev(offer.team, offer.toTerms())
    } else {
        ensureCoDevStanding(k.partner).favor = (coDevFavor(k.partner) - 0.04).clamped(-1.0, 1.0)
        addLog("${k.partner} declined co-dev.", LogTone.NEUTRAL, Lane.GAMES)
        feedOnCoDev("decline", k.partner, g.projectOf(offer.team)?.title)
    }
}

internal fun GameState.describeCoDevIncoming(k: CoDevIncoming): Description? {
    val g = games ?: return null
    val offer = g.coDevOffers.firstOrNull { it.id == k.offerId } ?: return null
    val title = g.projectOf(offer.team)?.title ?: "your project"
    return Description(
        "${offer.partner} wants to co-dev $title",
        "${offer.kind.blurb} Terms: they cover ${Format.percent(offer.costShare)} of burn and ${Format.percent(offer.workShare)} of workload; " +
            "they take ${Format.percent(offer.royaltyShare)} of net and ${Format.percent(offer.creditShare)} of credit.",
        listOf(
            choice("Accept", "Sign the split."),
            choice("Decline", "Stay solo. Slight favor hit."),
        ),
        Lane.GAMES,
    )
}

internal fun GameState.resolveCoDevIncoming(k: CoDevIncoming, choice: Int) {
    if (choice == 0) {
        runCatching { acceptCoDevOffer(k.offerId) }
            .onFailure {
                games?.coDevOffers?.removeAll { it.id == k.offerId }
                addLog("Co-dev fell through.", LogTone.NEUTRAL, Lane.GAMES)
            }
    } else {
        runCatching { declineCoDevOffer(k.offerId) }
    }
}

internal fun GameState.describeCoDevDispute(k: CoDevDispute): Description? {
    val g = games ?: return null
    val project = g.projectOf(k.team)
    val cd = project?.coDev ?: return null
    if (cd.phase != CoDevPhase.ACTIVE && k.reason != CoDevDisputeReason.FLOP) return null
    val title = project.title
    val body = when (k.reason) {
        CoDevDisputeReason.DELAY -> "${cd.partner} is furious about the slip on $title. They want out — or a sweeter cut."
        CoDevDisputeReason.BUGS -> "${cd.partner} says the bug debt on $title is your mess. They're threatening to walk."
        CoDevDisputeReason.FLOP -> "${cd.partner} wants distance from $title's reception. Lawyers are circling."
        CoDevDisputeReason.CREDIT -> "${cd.partner} is soft-poaching credit in the press — \"we carried $title.\""
        CoDevDisputeReason.SCOPE -> "${cd.partner} wants to cut scope on $title or rewrite the split."
    }
    return Description(
        "Co-dev dispute with ${cd.partner}",
        body,
        listOf(
            choice("Smooth it over", "${money(coDevSmoothCost(cd))}. Keep them; favor recovers a little."),
            choice("Let them quit", "Lose their remaining work and cost share. Favor drops."),
            choice("Fight / counter-sue", "Hardball. Risk a suit, or claw credit back."),
        ),
        Lane.GAMES,
    )
}

fun GameState.coDevSmoothCost(cd: CoDevTerms): Double =
    scaled(25_000.0) * (0.8 + cd.royaltyShare) * if (cd.kind == CoDevKind.RIVAL) 1.3 else 1.0

internal fun GameState.resolveCoDevDispute(k: CoDevDispute, choice: Int) {
    val g = games ?: return
    val p = g.projectOf(k.team)
    val cd = p?.coDev ?: return
    coDev.disputes += 1
    val stand = ensureCoDevStanding(cd.partner)
    when (choice) {
        0 -> {
            val cost = coDevSmoothCost(cd)
            if (cash >= cost) spend(cost, Lane.GAMES)
            stand.favor = (stand.favor + 0.1).clamped(-1.0, 1.0)
            addLog("Smoothed the co-dev dispute with ${cd.partner}.", LogTone.GOOD, Lane.GAMES)
            feedOnCoDev("smooth", cd.partner, p.title)
        }
        1 -> {
            // Partner quits — strip remaining help.
            cd.phase = CoDevPhase.QUIT
            stand.favor = (stand.favor - 0.18).clamped(-1.0, 1.0)
            p.workScale = (p.workScale * 1.08).coerceAtMost(1.35)
            addLog("${cd.partner} quit the co-dev on ${p.title}. Workload falls back on you.", LogTone.BAD, Lane.GAMES)
            publish(
                NewsDesk.INDUSTRY, techOutlet(Lane.GAMES, rng),
                "${cd.partner} walks from $companyName co-dev",
                "${p.title} loses a partner mid-flight. Shared blame, separate futures.",
                LogTone.BAD, Lane.GAMES, tag = NewsTag.NEWS, cover = p.newsCover(),
            )
            feedOnCoDev("quit", cd.partner, p.title)
            when (cd.kind) {
                CoDevKind.RIVAL -> gameRivals.firstOrNull { it.name == cd.partner }?.let {
                    it.heat = (it.heat + 0.12).clamped(0.0, 1.0)
                }
                else -> {}
            }
        }
        else -> {
            // Fight: either claw credit or get sued.
            if (k.reason == CoDevDisputeReason.CREDIT || rng.chance(0.55)) {
                cd.creditGrabs += 1
                cd.creditShare = (cd.creditShare - 0.08).coerceAtLeast(0.15)
                stand.favor = (stand.favor - 0.1).clamped(-1.0, 1.0)
                reputation.adjust(0.4)
                addLog("You fought ${cd.partner}'s credit grab on ${p.title}. Press mostly sides with you.", LogTone.NEUTRAL, Lane.GAMES)
                feedOnCoDev("credit_fight", cd.partner, p.title)
            } else {
                cd.phase = CoDevPhase.SUED
                val damages = scaled(80_000.0) * (1.0 + cd.royaltyShare)
                spend(damages, Lane.GAMES)
                stand.favor = (stand.favor - 0.25).clamped(-1.0, 1.0)
                reputation.scar(ScarKind.SCANDAL, 0.1, week, "Co-dev lawsuit with ${cd.partner}")
                p.workScale = (p.workScale * 1.1).coerceAtMost(1.4)
                addLog("${cd.partner} sued over ${p.title}. ${money(damages)} and a press scar.", LogTone.BAD, Lane.GAMES)
                publish(
                    NewsDesk.INDUSTRY, "Polygon",
                    "$companyName sued by co-dev partner ${cd.partner}",
                    "The ${p.title} partnership is in court. Shared blame, expensive lawyers.",
                    LogTone.BAD, Lane.GAMES, tag = NewsTag.NEWS, cover = p.newsCover(),
                )
                feedOnCoDev("sue", cd.partner, p.title)
            }
        }
    }
}
