package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/** Paid hype, bought once each per game. */
@Serializable
enum class Campaign(val title: String, val cost: Double, val hype: Double) {
    PRESS_RELEASE("Press release", 5_000.0, 6.0),
    AD_CAMPAIGN("Ad campaign", 40_000.0, 14.0),
    TV_BLITZ("TV blitz", 300_000.0, 28.0),
}

/** A small paid job for another company, to keep the lights on between games. */
@Serializable
data class GameContract(
    val id: Int,
    val title: String,
    val work: Double,
    val pay: Double,
    val weeksAllowed: Int,
    val rp: Double,
    val expiresWeek: Int,
    var done: Double = 0.0,
    var weeksElapsed: Int = 0,
) {
    val completion: Double get() = min(1.0, done / work)
}

/** How big an expansion is: what it takes, who buys it, and for how much. */
@Serializable
enum class ExpansionScope(val title: String, val work: Double, val budget: Double, val attach: Double, val price: Double, val blurb: String) {
    DLC("Small DLC", 0.12, 0.4, 0.8, 0.2, "A few weeks' work: a new area, a new mode. Cheap, and plenty of owners buy it."),
    EXPANSION("Expansion", 0.3, 0.5, 1.0, 0.4, "A proper chunk of new game for the people who loved it."),
    STANDALONE("Standalone", 0.5, 0.7, 1.0, 0.6, "Big enough to stand alone: finds new players as well as old ones."),
}

/** New content for a released game. The whole team works on it, like a small game. */
@Serializable
data class GameExpansion(
    val releaseId: Int,
    val title: String,
    val work: Double,
    val weeklyBudget: Double,
    var done: Double = 0.0,
    var weeks: Int = 0,
    val scope: ExpansionScope = ExpansionScope.EXPANSION,
) {
    val completion: Double get() = min(1.0, done / work)
}

/** A publisher pays you to make the game they want, and takes most of the sales. */
@Serializable
data class PublisherDeal(
    val id: Int,
    val publisher: String,
    val topic: Topic,
    val genre: Genre,
    val platform: Platform,
    val size: GameSize,
    val minScore: Int,
    val advance: Double,
    /** Your share of net sales. */
    val share: Double,
    val expiresWeek: Int,
)

@Serializable
data class GameProject(
    val concept: GameConcept,
    /** Effort split for each of the three stages, three areas each. */
    val focus: List<MutableList<Double>>,
    val startedWeek: Int,
    var progress: Double = 0.0,
    var design: Double = 0.0,
    var tech: Double = 0.0,
    var bugs: Double = 0.0,
    var polish: Double = 0.0,
    var hype: Double = 0.0,
    var weeksInDev: Int = 0,
    var spent: Double = 0.0,
    val campaigns: MutableList<Campaign> = mutableListOf(),
    val deal: PublisherDeal? = null,
    /** Work-weighted focus match so far, so changing sliders only affects work still to come. */
    var matchSum: Double = 0.0,
    var matchWork: Double = 0.0,
    /** Points added last week, for the bubbles. */
    var lastDesign: Double = 0.0,
    var lastTech: Double = 0.0,
    var lastBugs: Double = 0.0,
    /** Development stages that have had a hands-on playtest (0–2 plus final polish stage 3). */
    val playtestedStages: MutableSet<Int> = mutableSetOf(),
    /**
     * Weekly creative direction. Older saves omit this field; kotlinx defaults it to [GameFocus.BALANCED].
     */
    var creativeFocus: GameFocus = GameFocus.BALANCED,
    /** Trailers shown so far. The first one is the announcement. */
    var reveals: Int = 0,
    /** The console maker whose marketing deal this game took, if any. */
    var exclusiveWith: String? = null,
    /** People waiting to buy it. */
    var wishlists: Double = 0.0,
    /** The week it launches, if a date's been set. */
    var releaseWeek: Int? = null,
    /** Times it's slipped. */
    var delays: Int = 0,
    /** Scope changes at milestones: more or less work than planned. */
    var workScale: Double = 1.0,
    /** What milestone decisions added to (or took from) the score. */
    var qualityBonus: Double = 0.0,
    /** Milestones reviewed: 0 the vertical slice, 1 alpha, 2 beta. */
    val milestones: MutableSet<Int> = mutableSetOf(),
    /** What was decided at each milestone, for the review breakdown. */
    val choices: MutableList<String> = mutableListOf(),
    /** The forecast score, week by week. */
    val forecasts: MutableList<Double> = mutableListOf(),
    /** A console maker's exclusive, if one's signed. */
    var exclusive: Exclusive? = null,
    /** Makers who've been pitched or made an offer. */
    val pitched: MutableSet<String> = mutableSetOf(),
    /** The engine features that go into it. Null: built the old way, with the engine's whole kit. */
    var features: MutableSet<Research>? = null,
    /** Who leads each area, by staff id. */
    val leads: MutableMap<FocusArea, Int> = mutableMapOf(),
    /** Stages whose features have been paid for. */
    val paidStages: MutableSet<Int> = mutableSetOf(),
    /** Copy protection it'll ship with, paid for at launch. */
    var protection: Protection = Protection.NONE,
    /** Shipping uncut, rated Adults Only. */
    var uncut: Boolean = false,
    /** The ratings board has looked at it. */
    var rated: Boolean = false,
    /** Areas that had enough specialists on them while their stage was worked. */
    val covered: MutableSet<FocusArea> = mutableSetOf(),
    /** Who's on which area of each stage ("stage:id"); anyone not here goes where they're needed. */
    val crew: MutableMap<String, FocusArea> = mutableMapOf(),
    /** Standard boxes and collector's editions on order; none leaves it to a distributor. */
    var boxes: Int = 0,
    var collectors: Int = 0,
    /** The stores it'll be sold on; null for the usual ones. */
    var stores: Set<Storefront>? = null,
) {
    /** How much the leads add to this week's work. Worked out each week before the team gets going. */
    @kotlinx.serialization.Transient
    var leadFactor: Double = 1.0

    /** The stage it was on when this week began. */
    @kotlinx.serialization.Transient
    var weekStartStage: Int = 0

    val title: String get() = concept.title
    val work: Double get() = concept.work * workScale
    val isReady: Boolean get() = progress >= work
    val completion: Double get() = min(1.0, progress / work)

    /** 0, 1 or 2 while in development. */
    val stage: Int get() = min(2, (progress / work * 3).toInt())

    val focusMatch: Double get() = if (matchWork > 0) matchSum / matchWork else 0.5

    /**
     * Estimated design share from work already done plus the current stage's sliders for work still
     * to come. Moves when you rebalance mid-dev so the lean meter and forecast can react.
     */
    fun projectedDesignShare(): Double {
        val remaining = (work - progress).coerceAtLeast(0.0)
        val accruedShare = if (design + tech > 0.01) design / (design + tech) else null
        if (isReady || remaining <= 0.01) return accruedShare ?: concept.idealDesignShare
        val alloc = focus[stage]
        val areas = FocusArea.forStage(stage)
        val total = alloc.sum().takeIf { it > 0 } ?: 1.0
        val stageShare = areas.indices.sumOf { alloc[it] / total * areas[it].designShare }
        val done = progress.coerceAtLeast(0.0)
        val accrued = accruedShare ?: stageShare
        return (accrued * done + stageShare * remaining) / (done + remaining)
    }

    /** How close the projected design lean is to the genre ideal, without revealing the target. */
    fun designLeanDelta(): Double = projectedDesignShare() - concept.idealDesignShare

    /**
     * 1 when projected lean matches the genre ideal; falls toward 0 as you drift.
     * Used as a meter fill so "design lean" doesn't read as a progress bar toward 100% design.
     */
    fun designLeanFit(): Double = (1.0 - kotlin.math.abs(designLeanDelta()) / 0.35).coerceIn(0.0, 1.0)
}

@Serializable
data class GameRelease(
    val id: Int,
    val title: String,
    val concept: GameConcept,
    val week: Int,
    val reviews: List<Review>,
    /** Average review, 1...10. */
    val score: Double,
    var weeklySales: Double,
    var netShare: Double,
    val design: Double,
    val tech: Double,
    var bugs: Double,
    val hype: Double,
    val devCost: Double,
    val report: List<String>,
    val publisher: String? = null,
    var lifetimeSales: Double = 0.0,
    var unitsSold: Double = 0.0,
    var chartRank: Int = 0,
    var bestRank: Int = 0,
    /** A patch went out: bugs fixed, sales hold up a little longer. */
    var patched: Boolean = false,
    val expansions: MutableList<String> = mutableListOf(),
    /** What players think, 1...10. Word of mouth: it decides how long the game keeps selling. -1 on old saves. Patches can raise it. */
    var userScore: Double = -1.0,
    /**
     * False while [userScore] is still the early/provisional number (settles after 3 weeks or the first patch).
     * Old saves default to true so existing scores stay final.
     */
    var userScoreSettled: Boolean = true,
    val aspects: Map<ReviewAspect, Double> = emptyMap(),
    val pros: List<String> = emptyList(),
    val cons: List<String> = emptyList(),
    val salesHistory: MutableList<Double> = mutableListOf(),
    /** The community of a live game. */
    var live: LiveService? = null,
    /** Why it scored what it did: the baseline plus each part. Empty on old saves and for games you didn't make. */
    val breakdown: List<ScorePart> = emptyList(),
    /** The indie who made it, when your label published it. */
    val developer: String? = null,
    /** Its pro league, if it has one. */
    var league: League? = null,
    /** Wishlists on launch day. */
    val wishlists: Double = 0.0,
    /** What players said in their reviews. */
    val playerQuotes: List<PlayerQuote> = emptyList(),
    /** Platforms it was ported to after launch. */
    val extraPlatforms: MutableList<Platform> = mutableListOf(),
    /** The last week it was on sale. */
    var lastSaleWeek: Int = -1000,
    /** The console maker it's exclusive to, and until when (forever for a full exclusive). */
    var exclusiveTo: String? = null,
    var exclusiveUntil: Int = 0,
    /** The copy protection it shipped with, and about how many copies were pirated. */
    var protection: Protection = Protection.NONE,
    var pirated: Double = 0.0,
    /** Its age rating, once there were ratings. */
    var rating: AgeRating? = null,
    /** A permanent price cut (0.25 = a quarter off), and the sale running now, if any. */
    var priceCut: Double = 0.0,
    var saleUntil: Int = -1,
    var saleDepth: Double = 0.0,
    var saleLift: Double = 1.0,
    var salePull: Double = 0.0,
    /** Players who've learned to wait for the next sale. */
    var saleFatigue: Double = 0.0,
    /** Free content updates so far. */
    var updates: Int = 0,
    var lastUpdateWeek: Int = -1000,
    /** The player score at launch, to tell how far it's come. */
    var firstUserScore: Double = 10.0,
    /** Mod tools are out. */
    var modTools: Boolean = false,
    var modToolsWeek: Int = 0,
    /**
     * The mod / community scene around this title (0 = none yet). Old saves default to zero so
     * they load clean; scenes form after launch on PC in later decades.
     */
    var modBuzz: Double = 0.0,
    var modHealth: Double = 0.7,
    var modPolicy: ModPolicy = ModPolicy.IGNORE,
    /** Week the scene formed, or -1 if none (0 is a valid calendar week). */
    var modSceneWeek: Int = -1,
    /** Official kit beyond bare tools, and any partnered mod team. */
    var modKit: Boolean = false,
    var modPartner: String? = null,
    var modDocsWeek: Int = -1000,
    var modClampWeek: Int = -1000,
    /** Broken community content is showing up on a storefront you control or list on. */
    var brokenMods: Boolean = false,
    /** The stores it was sold on, for a PC game you published. */
    var stores: List<Storefront> = emptyList(),
    /** A wave of angry user reviews has hit it. */
    var bombed: Boolean = false,
    /** Boxes and collector's editions you made, and how many sold. */
    var boxes: Int = 0,
    var boxesSold: Int = 0,
    var collectors: Int = 0,
    var collectorsSold: Int = 0,
) {
    /** 0...100, for the rest of the company's bookkeeping. */
    val review: Int get() = (score * 10).roundToInt()

    /** The critics' average on a 100-point scale, the way aggregators show it. */
    val metascore: Int get() = (score * 10).roundToInt().coerceIn(1, 100)

    /** The score word of mouth runs on: players' if known, otherwise the critics'. */
    val wordOfMouth: Double get() = if (userScore > 0) userScore else score

    val verdict: String
        get() = when {
            score >= 9 -> "Masterpiece"
            score >= 8 -> "Hit"
            score >= 6.5 -> "Solid"
            score >= 5 -> "Mixed"
            else -> "Flop"
        }

    val onSale: Boolean get() = weeklySales > 0
}

/** One part of a game's score: what it was, how many points it added or took off, and why. */
@Serializable
data class ScorePart(val label: String, val points: Double, val note: String = "")

/** Where every score starts before its parts. */
const val SCORE_BASELINE = 5.0

/**
 * What a game in development would score if you shipped it today.
 * Critics' mood (±0.6) is left at zero, so the range is the expected band.
 */
data class LaunchForecast(
    val expectedScore: Double,
    val low: Double,
    val high: Double,
    val bugs: Double,
    /** polish / (work × 0.4), capped at 1 once the polish meter is full. */
    val polishRatio: Double,
    /** Whole weeks of current output to hit content-complete. Null when already ready. */
    val weeksToReady: Int?,
) {
    /** Further polish buys little once the meter is full. */
    val polishPastSweetSpot: Boolean get() = polishRatio >= 0.95
}

/** Design and tech points per unit of work that a decent team makes in a given week: it rises 10% a year. */
internal fun craftBenchmark(week: Int): Double = 2.0 * 1.1.pow(min(week / 52.0, BENCHMARK_YEARS))

/** Expectations stop rising after this many years, so a long run (a Decades run lasts 50) stays playable. */
internal const val BENCHMARK_YEARS = 15.0

/** The engine level reviewers expect in a given week, rising until [BENCHMARK_YEARS]. */
internal fun engineStandard(week: Int): Double = min(week / 20.0, BENCHMARK_YEARS * 52 / 20)

/** Above 8, every extra point counts half: a 10 needs everything to go right. */
internal fun softCap(raw: Double): Double = if (raw > 8) 8 + (raw - 8) * 0.5 else raw

/** Points for how well a topic fits a genre. */
internal fun fitPoints(f: Fit): Double = when (f) {
    Fit.GREAT -> 0.6
    Fit.GOOD -> 0.2
    Fit.OKAY -> -0.3
    Fit.BAD -> -1.5
}

/** One of your series: the first game and everything that followed it. */
data class Franchise(val root: GameRelease, val entries: List<GameRelease>) {
    val name: String get() = seriesName(root.title)
    val latest: GameRelease get() = entries.last()
    val averageScore: Double get() = entries.map { it.score }.average()
    val lifetime: Double get() = entries.sumOf { it.lifetimeSales }
    val copies: Double get() = entries.sumOf { it.unitsSold }
}

@Serializable
class GamesStudio {
    var project: GameProject? = null

    /** The second team's game, once researched. */
    var project2: GameProject? = null

    /** Share of the game staff on the second team while both teams have a game. */
    var team2Share = 0.4

    fun projectOf(team: Int): GameProject? = if (team == 2) project2 else project

    internal fun setProject(team: Int, p: GameProject?) {
        if (team == 2) project2 = p else project = p
    }

    /** Both teams have a game on. */
    val twoGames: Boolean get() = project != null && project2 != null
    var contract: GameContract? = null
    var expansion: GameExpansion? = null

    /** Something is keeping the team busy. */
    val busy: Boolean get() = project != null || contract != null || expansion != null || engineBuild != null
    val contractOffers = mutableListOf<GameContract>()
    val dealOffers = mutableListOf<PublisherDeal>()
    val releases = mutableListOf<GameRelease>()
    var fans = 0.0

    /** 0.3...1.2. Flops knock it down and only hits bring it back. The fanbase never forgets. */
    var fanTrust = 1.0
    var hits = 0
    var flops = 0
    val topics: MutableList<Topic> = Topic.entries.filter { it.starter }.toMutableList()

    /** Genres you can make. The first six are known from the start; the rest are researched. */
    val genres: MutableList<Genre> = Genre.entries.filter { it.starter }.toMutableList()
    val engines = mutableListOf(Engine(0, "Starter kit", emptyList(), 0))

    /** Ports under way. */
    val ports = mutableListOf<Port>()
    var engineBuild: EngineBuild? = null

    /** What game reports have taught you: fits keyed "TG:topic:genre", "TA:topic:audience", "PG:platform:genre". */
    val knownFits: MutableMap<String, Fit> = mutableMapOf()

    /** Genre focus lessons, keyed "GENRE:AREA". */
    val insights: MutableSet<String> = mutableSetOf()

    /**
     * Engine feature tech levels you've measured in a build or a shipped game.
     * Until then the UI shows the feature name only.
     */
    val revealedEngineTech: MutableSet<String> = mutableSetOf()
    var trend: Trend? = null
    var topQuality = 0.0
    var topScore = 0.0

    /** A review the player hasn't looked at yet. */
    var unseenReview: Int? = null

    /** Work done on games or contracts last week, for research. */
    var lastWork = 0.0

    /** Rival games, the sales chart and the awards. */
    val market = GameMarket()

    /** Side income last week, for the studio screen. */
    var lastRoyalties = 0.0
    var lastMerch = 0.0

    /** Studios you've bought. */
    val subsidiaries = mutableListOf<Subsidiary>()

    /** Your own game store, if you've built one. */
    var store: OwnStore? = null

    /** An offer for a studio, waiting on an answer. */
    var deal: Deal? = null

    /** Studios whose books you've been through. */
    val diligence = mutableSetOf<String>()

    /** Stakes you hold in studios you don't own (share of each), what you paid, and what they've earned. */
    val stakes: MutableMap<String, Double> = mutableMapOf()
    val stakePaid: MutableMap<String, Double> = mutableMapOf()
    var stakeEarned = 0.0
    var lastStakeProfit = 0.0

    /** Studios one of the giants bought, and when. */
    val rivalOwned: MutableMap<String, String> = mutableMapOf()
    val rivalOwnedWeek: MutableMap<String, Int> = mutableMapOf()

    /** Studios that won't hear another offer until a given week. */
    val cooloff = mutableMapOf<String, Int>()

    /** Your label: pitches waiting for an answer, games you've funded, and the ones you've put out. */
    val pitches = mutableListOf<IndiePitch>()
    val signed = mutableListOf<SignedGame>()
    val published = mutableListOf<GameRelease>()

    /** Releases whose fans have petitioned for a sequel, and the one you promised. */
    val petitions: MutableSet<Int> = mutableSetOf()
    var promisedSequel: Int? = null

    /** Films and shows of your series. */
    val adaptations = mutableListOf<Adaptation>()

    /** How a film or show changes the next launch in a series, keyed by the series' first game. */
    val screenLift: MutableMap<Int, Double> = mutableMapOf()

    /** When you last shopped each series' rights around. */
    val screenPitched: MutableMap<Int, Int> = mutableMapOf()

    /** Collections being put together, and the games already in one. */
    val collectionJobs = mutableListOf<CollectionJob>()
    val collected: MutableSet<Int> = mutableSetOf()

    /** Platforms you hold a development licence for: paid once, kept for good. */
    val licences: MutableSet<Platform> = mutableSetOf()

    /** Pairs of people who've found out how they get on ("id:id"). */
    val knownPairs: MutableSet<String> = mutableSetOf()
    private var nextId = 1

    internal fun nextId(): Int = nextId++

    val weeklySales: Double get() = releases.sumOf { it.weeklySales * it.netShare } + published.sumOf { it.weeklySales * it.netShare }
    val bestGame: GameRelease? get() = releases.maxByOrNull { it.score }

    fun engine(id: Int): Engine = engines.firstOrNull { it.id == id } ?: engines.first()

    fun knownFit(topic: Topic, genre: Genre): Fit? = knownFits["TG:${topic.name}:${genre.name}"]
    fun knownFit(topic: Topic, audience: Audience): Fit? = knownFits["TA:${topic.name}:${audience.name}"]
    fun knownFit(platform: Platform, genre: Genre): Fit? = knownFits["PG:${platform.name}:${genre.name}"]
    fun knowsWeight(genre: Genre, area: FocusArea): Boolean = "${genre.name}:${area.name}" in insights
    fun knowsEngineLevel(r: Research): Boolean = r.name in revealedEngineTech || r.engineLevel == 0
    fun revealEngineTech(features: Iterable<Research>) {
        for (f in features) if (f.kind == Research.Kind.ENGINE) revealedEngineTech += f.name
    }

    /** One of your games, or one your label published. */
    fun release(id: Int): GameRelease? = releases.firstOrNull { it.id == id } ?: published.firstOrNull { it.id == id }

    /** The first game of the series a release belongs to. */
    fun seriesRoot(r: GameRelease): GameRelease {
        var current = r
        repeat(64) { current = current.concept.sequelOf?.let { release(it) } ?: return current }
        return current
    }

    /** Every game in a series, oldest first. */
    fun series(root: GameRelease): List<GameRelease> = releases.filter { seriesRoot(it).id == root.id }.sortedBy { it.id }

    /**
     * What to call the next game in [releaseId]'s series: a numbered sequel, or a spin-off when the
     * genre changes. Another [reroll] gives another way of numbering and subtitling it.
     */
    fun nextTitle(releaseId: Int, genre: Genre? = null, reroll: Int = 0, year: Int? = null): String {
        val r = release(releaseId) ?: return "Untitled"
        val entries = series(seriesRoot(r))
        val latest = entries.last()
        val name = seriesName(seriesRoot(r).title)
        val n = entries.size + 1
        val used = releases.map { it.title }.toSet()
        val title = if (genre != null && genre != latest.concept.genre) {
            spinOffTitle(name, genre, reroll)
        } else {
            sequelTitle(name, n, latest.concept.topic, latest.id.toLong() + reroll * 101L, year)
        }
        return if (title in used) "$name $n" else title
    }

    /** Your franchises: every first game that got at least one sequel or spin-off. */
    val franchises: List<Franchise>
        get() = releases.filter { it.concept.sequelOf == null }
            .map { Franchise(it, series(it)) }
            .filter { it.entries.size >= 2 }
            .sortedByDescending { it.lifetime }

    /** Suggest a name nobody has used, in the voice of the topic and genre. */
    fun suggestTitle(rng: SeededRNG, topic: Topic = Topic.FANTASY, genre: Genre = Genre.ACTION, year: Int? = null): String =
        gameTitle(topic, genre, rng.int(0, 1_000_000).toLong(), releases.map { it.title }.toSet() + listOfNotNull(project?.title, project2?.title), year)

    // Weekly simulation

    internal fun tick(ctx: LaneContext, rng: SeededRNG): LaneReport {
        val report = LaneReport()
        val work = ctx.workOutput
        lastWork = 0.0

        engineBuild?.let { b ->
            // Building an engine is the main team's work while it lasts.
            if (project == null) lastWork = work
            val left = b.weeksLeft - 1
            if (left <= 0) {
                val engine = Engine(nextId(), b.name, b.features, ctx.week)
                engines += engine
                engineBuild = null
                revealEngineTech(b.features)
                val comboNote = engine.combo.notes.takeIf { it.isNotEmpty() }?.joinToString(" ") ?: ""
                report.log(
                    "${engine.name} is finished: tech level ${engine.level}" +
                        (if (engine.rawLevel != engine.level) " (raw ${engine.rawLevel})" else "") +
                        ". New games can use it." + if (comboNote.isNotEmpty()) " $comboNote" else "",
                    LogTone.MILESTONE,
                )
            } else {
                engineBuild = b.copy(weeksLeft = left)
            }
        }

        // With both teams busy, each gets its share of the staff; a team on its own gets everyone.
        val team1Busy = project != null || contract != null || expansion != null
        val share2 = if (project2 == null) 0.0 else if (team1Busy) team2Share else 1.0
        val work1 = work * (1 - share2)
        project?.let { develop(it, work1, ctx, report) }
        project2?.let { develop(it, work * share2, ctx, report) }
        if (project != null || project2 != null) lastWork = work

        contract?.let { c ->
            lastWork = work
            c.done += work1
            c.weeksElapsed += 1
            if (c.done >= c.work) {
                report.revenue += c.pay
                report.log("Contract delivered: ${c.title}. Paid ${money(c.pay)}.", LogTone.GOOD)
                contract = null
                report.add(Incident.Research(c.rp))
            } else if (c.weeksElapsed > c.weeksAllowed) {
                report.expenses += c.pay * 0.25
                report.log("You blew the deadline on ${c.title}. The client wants a refund.", LogTone.BAD)
                contract = null
            }
        }

        // Other studios building on your engines.
        val royalties = engines.filter { it.licensed }.sumOf { it.royalty(ctx.week) } * ctx.marketDemand
        if (royalties > 0) {
            report.revenue += royalties
            lastRoyalties = royalties
        } else {
            lastRoyalties = 0.0
        }

        expansion?.let { x ->
            lastWork = work
            x.done += work1
            x.weeks += 1
            val budget = x.weeklyBudget * ctx.mods.cost
            report.expenses += budget
            if (x.done >= x.work) {
                expansion = null
                release(x.releaseId)?.let { r ->
                    r.expansions += x.title
                    val attach = (0.3 * (r.score / 8).pow(2) * x.scope.attach).clamped(0.03, 0.6)
                    val buyers = r.unitsSold * attach
                    r.weeklySales += buyers * r.concept.size.price * x.scope.price * 0.3 * ctx.marketDemand
                    fans += buyers * 0.1
                    // A standalone expansion also finds players who never bought the original.
                    if (x.scope == ExpansionScope.STANDALONE) {
                        val fresh = r.concept.size.launchSales * (r.score / 7).pow(3) * 0.25 * ctx.marketDemand
                        r.weeklySales += fresh
                        fans += fresh / r.concept.unitPrice * 0.1
                    }
                    report.log("${x.title} is out. About ${Format.count(buyers)} players came back for more.", LogTone.GOOD)
                }
            }
        }

        contractOffers.removeAll { it.expiresWeek < ctx.week }
        dealOffers.removeAll { it.expiresWeek < ctx.week }

        for (r in releases + published) {
            val l = r.live
            if (l != null && !l.closed) {
                val (rev, cost) = l.week(ctx.week, r.wordOfMouth, ctx.marketDemand)
                report.revenue += rev
                report.expenses += cost
                r.lifetimeSales += rev - cost
                if (l.players < 1_500) {
                    l.closed = true
                    l.players = 0.0
                    report.log("${r.title} had too few players left to keep the servers on. It's shut down.", LogTone.NEUTRAL)
                }
            }
            if (r.weeklySales <= 0) continue
            val age = ctx.week - r.week
            val onSale = r.onSale(ctx.week)
            val gross = r.weeklySales * (if (onSale) r.saleLift else 1.0)
            report.revenue += gross * r.netShare
            r.lifetimeSales += gross * r.netShare
            r.unitsSold += gross / (r.price * (if (onSale) 1 - r.saleDepth else 1.0))
            r.salesHistory += gross
            // A sale ends: some of the people who'd have paid full price later already bought it.
            if (ctx.week == r.saleUntil) r.weeklySales *= 1 - r.salePull
            r.saleFatigue *= 0.97
            if (r.salesHistory.size > 52) r.salesHistory.removeAt(0)
            r.weeklySales *= salesDecay(age, if (age < 6) r.score else r.wordOfMouth) * (if (r.patched) 1.01 else 1.0) * (1.0 + r.modSalesLift())
            if (r.weeklySales < 40) {
                r.weeklySales = 0.0
                r.chartRank = 0
            }
        }
        return report
    }

    private fun develop(p: GameProject, work: Double, ctx: LaneContext, report: LaneReport) {
        p.weeksInDev += 1
        val engineMult = engine(p.concept.engineId).multiplier
        val directive = p.creativeFocus
        val paced = work * directive.workMultiplier
        if (!p.isReady) {
            val stage = p.stage
            // A stage's features are paid for, and bring their bugs, when it starts.
            val chosen = p.features
            if (chosen != null && stage !in p.paidStages) {
                p.paidStages += stage
                val cost = stageFeatureCost(chosen, stage, p.concept.size, ctx.year) * ctx.mods.cost
                p.spent += cost
                report.expenses += cost
                p.bugs += featureBugs(chosen, stage)
            }
            val alloc = p.focus[stage]
            val total = alloc.sum().takeIf { it > 0 } ?: 1.0
            val areas = FocusArea.forStage(stage)
            val designShare = areas.indices.sumOf { alloc[it] / total * areas[it].designShare }
            val boost = featureBoost(p.concept, chosen, stage) * p.leadFactor
            val d = paced * 2 * designShare * (ctx.staffing.avgDesign / 5) * engineMult * boost
            val t = paced * 2 * (1 - designShare) * (ctx.staffing.avgTech / 5) * engineMult * boost
            val engineBugs = engine(p.concept.engineId).combo.bugRisk
            val b = paced * 0.3 * (1.3 - ctx.staffing.avgTech / 10) * (if (ctx.crunch) 1.6 else 1.0) *
                (1 + engineBugs) * directive.bugMultiplier * ctx.mods.bugs
            p.design += d
            p.tech += t
            p.bugs += b
            p.lastDesign = d
            p.lastTech = t
            p.lastBugs = b
            p.matchSum += min(1.0, focusMatch(p.concept, stage, alloc) + ctx.mods.focus) * paced
            p.matchWork += paced
            val wasStage = stage
            p.progress += paced
            if (p.isReady) {
                repeat(ctx.mods.insights) { grantStageInsight(p, wasStage, report) }
                report.log("${p.title} is content complete. Fix bugs and polish, or ship it now.", LogTone.GOOD)
            } else if (p.stage != wasStage) {
                repeat(ctx.mods.insights) { grantStageInsight(p, wasStage, report) }
                report.log("${p.title} moves on to ${FocusArea.STAGES[p.stage].lowercase()}.")
            }
        } else {
            // Bug fixing and polish once the content is in.
            val fixed = min(p.bugs, paced * 0.6 / directive.bugMultiplier.coerceAtLeast(0.5))
            p.bugs -= fixed
            p.polish = min(p.work * 0.4, p.polish + paced * 0.5 * ctx.polishEfficiency * directive.polishMultiplier)
            p.lastDesign = 0.0
            p.lastTech = 0.0
            p.lastBugs = -fixed
        }
        p.hype *= 0.99
        val budget = p.concept.size.weeklyBudget * ctx.mods.cost * (if (p.deal != null) 0.5 else 1.0) * (if (p.concept.live) 1.25 else 1.0) * p.concept.production.cost
        p.spent += budget
        report.expenses += budget
    }

    // Launch, reviews and the game report

    /** One sharper genre-weight insight when a development stage finishes. */
    private fun grantStageInsight(p: GameProject, completedStage: Int, report: LaneReport) {
        val genres = listOf(p.concept.genre) + listOfNotNull(p.concept.genre2)
        val areas = FocusArea.forStage(completedStage)
        for (genre in genres) {
            val pick = areas
                .filter { !knowsWeight(genre, it) }
                .maxByOrNull { p.concept.idealShare(it) }
                ?: continue
            insights += "${genre.name}:${pick.name}"
            val how = when (genre.weight(pick)) {
                2 -> "matters a lot"
                1 -> "matters somewhat"
                else -> "barely matters"
            }
            report.log(
                "Insight from ${FocusArea.STAGES[completedStage]}: ${pick.title} $how in ${genre.lower} games.",
                LogTone.GOOD,
            )
            break
        }
    }

    /**
     * Expected critic score if [p] shipped today. [mood] is the critics' mood roll (±0.6);
     * pass 0 for a forecast. [projectRemaining] folds current stage sliders into craft/balance so
     * mid-dev forecasts move when you rebalance.
     */
    private fun draftScore(
        p: GameProject,
        ctx: LaneContext,
        audienceSuited: Boolean,
        mood: Double,
        projectRemaining: Boolean = false,
    ): ScoreDraft {
        val c = p.concept
        val week = ctx.week
        val engine = engine(c.engineId)
        val parts = mutableListOf<ScorePart>()
        fun part(label: String, points: Double, note: String) {
            if (abs(points) >= 0.05) parts += ScorePart(label, (points * 10).roundToInt() / 10.0, note)
        }
        val remaining = if (projectRemaining && !p.isReady) (p.work - p.progress).coerceAtLeast(0.0) else 0.0
        val projectedShare = if (projectRemaining) p.projectedDesignShare() else null
        val engineMult = engine.multiplier * (if (remaining > 0) featureBoost(c, p.features, p.stage) * p.leadFactor else 1.0)
        val projectedDesign = if (remaining > 0) {
            val share = projectedShare ?: c.idealDesignShare
            p.design + remaining * 2 * share * (ctx.staffing.avgDesign / 5) * engineMult
        } else {
            p.design
        }
        val projectedTech = if (remaining > 0) {
            val share = projectedShare ?: c.idealDesignShare
            p.tech + remaining * 2 * (1 - share) * (ctx.staffing.avgTech / 5) * engineMult
        } else {
            p.tech
        }
        val craft = (projectedDesign + projectedTech) / max(1.0, p.work) / craftBenchmark(week)
        part("Craft", (2.2 * ln(max(0.05, craft)) / ln(1.5)).clamped(-3.0, 2.0),
            "${Format.percent(craft)} of what a decent team makes today. Better people and engines raise it.")
        val designRatio = projectedShare ?: (projectedDesign / max(1.0, projectedDesign + projectedTech))
        val focusForScore = if (projectRemaining && !p.isReady) {
            val stage = p.stage
            val doneMatch = p.focusMatch
            val remainingMatch = focusMatch(c, stage, p.focus[stage])
            val done = p.progress.coerceAtLeast(0.0)
            val left = remaining
            if (done + left <= 0.01) doneMatch else (doneMatch * done + remainingMatch * left) / (done + left)
        } else {
            p.focusMatch
        }
        val combo = (listOf(c.topic.fit(c.genre)) + listOfNotNull(c.genre2?.let { c.topic.fit(it) })).map { fitPoints(it) }.average()
        part("${c.topic.title} + ${c.genreTitle}", combo, "How well the topic and genre fit together.")
        val audienceFit = c.topic.fit(c.audience)
        part("Audience", (if (audienceFit == Fit.BAD) -0.8 else if (audienceFit == Fit.OKAY) 0.0 else 0.2) + (if (audienceSuited) 0.0 else -0.4),
            if (audienceSuited) "${c.topic.title} for ${c.audience.phrase}." else "Your platforms' players aren't ${c.audience.phrase}.")
        part("Platforms", c.platforms.map {
            when (it.fit(c.genre)) {
                Fit.GREAT, Fit.GOOD -> 0.2
                Fit.OKAY -> 0.0
                Fit.BAD -> -0.7
            }
        }.average() - 0.1 * (c.platforms.size - 1), "How ${c.genre.lower} games play on ${c.platforms.joinToString { it.title }}.")
        part("Focus", ((focusForScore - 0.6) * 2.5).clamped(-1.5, 0.8), "How close the effort split was to what ${c.genreLower} games need.")
        val balanceOff = abs(designRatio - c.idealDesignShare)
        part("Design and tech balance", if (balanceOff < 0.05) 0.2 else -(balanceOff * 4).clamped(0.0, 1.0),
            "${Format.percent(designRatio)} design; ${c.genreLower} games want about ${Format.percent(c.idealDesignShare)}.")
        val bugPenalty = 1 - min(0.35, p.bugs / (p.work * 0.6) * 0.35)
        part("Bugs", -(1 - bugPenalty) * 7, "${p.bugs.roundToInt()} bug${if (p.bugs.roundToInt() == 1) "" else "s"} shipped.")
        part("Polish", 0.4 * min(1.0, p.polish / (p.work * 0.4)), "Weeks spent polishing after content was done.")
        val standard = engineStandard(week)
        val dated = engine.level < standard - 5
        part("Engine", when {
            dated -> -(0.12 * (standard - 5 - engine.level)).clamped(0.0, 1.2)
            engine.level >= standard + 3 -> 0.2
            else -> 0.0
        }, if (dated) "${engine.name} is behind the times." else "${engine.name}, level ${engine.level}.")
        val onTrend = trend?.matches(c) == true
        if (onTrend) part("On trend", 0.4, "Everyone wants ${trend?.title?.lowercase()} games right now.")
        val last = releases.firstOrNull { !it.isCollection }
        val repeated = c.sequelOf == null && c.remasterOf == null && last != null && last.concept.topic == c.topic && last.concept.genre == c.genre
        if (repeated) part("Retread", -0.8, "The same topic and genre as your last game, without calling it a sequel.")
        val original = c.sequelOf?.let { release(it) }
        val rushedSequel = original != null && week - original.week < 52
        if (rushedSequel) part("Rushed sequel", -0.6, "Less than a year after ${original!!.title}.")
        // Fans expect a sequel to look and play better than the last one.
        if (original != null && c.remasterOf == null) {
            val before = engine(original.concept.engineId)
            when {
                original.concept.engineId == c.engineId -> part("Same engine", -0.4, "Built on ${engine.name}, like ${original.title}. Fans expected a step up.")
                engine.level > before.level -> part("Newer engine", 0.3, "${engine.name} is a step up from ${before.name}, which ${original.title} used.")
            }
        }
        part("Studio craft", ctx.mods.quality / 15, "Perfectionists, playtesting and the like.")
        val directionBonus = p.creativeFocus.qualityBonus / 15
        if (abs(directionBonus) >= 0.05) {
            part(
                "Creative direction",
                directionBonus,
                "${p.creativeFocus.title}: ${p.creativeFocus.blurb}",
            )
        }
        if (c.production != Production.STANDARD) part("Production", c.production.score, "${c.production.title}: ${c.production.blurb}")
        val specialist = specialistPoints(c, p.covered)
        if (specialist > 0) {
            val areas = keyAreas(c).filter { it in p.covered }.joinToString { it.title.lowercase() }
            part("Specialists", specialist, "Specialists on what ${c.genreLower} games need most: $areas.")
        }
        if (p.uncut) {
            val uncut = uncutPoints(c)
            part("Uncut", uncut, if (uncut > 0) "Adults Only, and ${c.topic.title.lowercase()} is better for it." else "Adults Only, and it feels gratuitous.")
        }
        c.hook?.let { h ->
            part(h.title, h.points(c), if (h.fits(c)) "${h.title} suits ${c.genreLower} games." else "${h.title} doesn't suit ${c.genreLower} games.")
        }
        part("Milestones", p.qualityBonus, p.choices.joinToString("; ").ifEmpty { "Decisions made along the way." })
        val recent = releases.filter { it.developer == null && !it.isCollection }.take(3).map { it.score }
        val before = SCORE_BASELINE + parts.sumOf { it.points }
        val stepBack = recent.isNotEmpty() && before < recent.average() - 1.5
        val record = releases.isNotEmpty() && before > topScore
        if (stepBack) part("A step back", -0.5, "Well below your last games (${String.format(java.util.Locale.US, "%.1f", recent.average())} on average).")
        if (record) part("Your best yet", 0.2, "Better than anything you've made.")
        if (abs(mood) >= 0.05) part("Critics' mood", mood, "Reviewers are people.")
        val raw = SCORE_BASELINE + parts.sumOf { it.points }
        if (raw > 8) part("Diminishing returns", softCap(raw) - raw, "Above 8, every extra point counts half.")
        val score = softCap(raw).clamped(1.0, 10.0)
        return ScoreDraft(
            parts = parts, score = score, craft = craft, designRatio = designRatio, bugPenalty = bugPenalty,
            audienceFit = audienceFit, onTrend = onTrend, repeated = repeated, rushedSequel = rushedSequel,
            stepBack = stepBack, record = record, original = original, dated = dated, standard = standard, engine = engine,
        )
    }

    /** Expected score if you shipped [team]'s project today, with a ±0.6 critic-mood band. */
    fun forecast(ctx: LaneContext, audienceSuited: Boolean, workOutput: Double, team: Int = 1): LaunchForecast? {
        val p = projectOf(team) ?: return null
        val draft = draftScore(p, ctx, audienceSuited, mood = 0.0, projectRemaining = true)
        val weeks = when {
            p.isReady -> null
            workOutput <= 0.01 -> null
            else -> ceil((p.work - p.progress) / workOutput).toInt()
        }
        return LaunchForecast(
            expectedScore = (draft.score * 10).roundToInt() / 10.0,
            low = ((draft.score - 0.6).clamped(1.0, 10.0) * 10).roundToInt() / 10.0,
            high = ((draft.score + 0.6).clamped(1.0, 10.0) * 10).roundToInt() / 10.0,
            bugs = p.bugs,
            polishRatio = min(1.0, p.polish / (p.work * 0.4)),
            weeksToReady = weeks,
        )
    }

    internal fun launch(ctx: LaneContext, rng: SeededRNG, reach: Double, audienceSuited: Boolean, clash: Clash? = null, team: Int = 1, kept: Double = 1.0, pcKeep: Double = 0.7): LaneReport {
        val p = projectOf(team) ?: throw invalid("No game in development.")
        if (!p.isReady) throw invalid("${p.title} isn't content complete yet.")
        val c = p.concept
        val report = LaneReport()
        val week = ctx.week
        val mood = rng.normal(sd = 0.3).clamped(-0.6, 0.6)
        val draft = draftScore(p, ctx, audienceSuited, mood)
        val parts = draft.parts
        val score = draft.score
        val q = draft.craft
        val designRatio = draft.designRatio
        val bugPenalty = draft.bugPenalty
        val onTrend = draft.onTrend
        val repeated = draft.repeated
        val rushedSequel = draft.rushedSequel
        val stepBack = draft.stepBack
        val record = draft.record
        val original = draft.original
        val dated = draft.dated
        val standard = draft.standard
        val engine = draft.engine
        val audienceFit = draft.audienceFit

        // The critics, and then the players.
        val areas = FocusArea.entries
        val weakest = areas.maxByOrNull { a -> c.idealShare(a) - p.focus[a.stage][a.ordinal % 3] / max(0.01, p.focus[a.stage].sum()) }!!
        val verdict = writeReviews(
            ReviewInputs(
                title = p.title, concept = c, focus = p.focus, score = score, designRatio = designRatio, focusMatch = p.focusMatch,
                bugPenalty = bugPenalty, polish = p.polish / p.work, engineLevel = engine.level, engineStandard = standard,
                hype = p.hype, fanTrust = fanTrust, onTrend = onTrend, repeated = repeated, rushedSequel = rushedSequel,
                worthySequel = original != null && original.score >= 7 && !rushedSequel, stepBack = stepBack, record = record,
                wrongCrowd = audienceFit == Fit.BAD || !audienceSuited, awkwardPlatform = c.platforms.firstOrNull { it.fit(c.genre) == Fit.BAD }, price = c.price,
                year = ctx.year, production = c.production, bare = bareAreas(c, p.features, engine),
            ),
            rng,
        )
        val reviews = verdict.reviews
        val avg = verdict.average

        // Launch week.
        val fanLift = 1 + 1.5 * fans / (fans + c.size.fanScale)
        val reachMult = reach.pow(0.75).clamped(0.25, 2.0)
        // A series brings its fans along, and wears them out if it slips.
        val previous = original?.let { series(seriesRoot(it)) } ?: emptyList()
        val spinOff = original != null && original.concept.genre != c.genre
        val seriesAvg = if (previous.isEmpty()) 0.0 else previous.map { it.score }.average()
        var sequelLift = when {
            previous.isEmpty() -> 1.0
            seriesAvg >= 7 -> 1 + min(0.4, 0.1 * previous.size)
            else -> 1 - min(0.2, 0.05 * previous.size)
        }
        val fatigued = previous.size >= 3 && previous.last().score < previous[previous.size - 2].score - 0.5
        if (fatigued) sequelLift *= 0.95
        if (spinOff) sequelLift = 1 + (sequelLift - 1) / 2
        // A film or show of the series since the last game: a hit brings people in, a flop puts them off.
        val screen = previous.firstOrNull()?.let { screenLift.remove(it.id) } ?: 1.0
        // A remaster sells to the people who remember the original, and fewer of them than a new game finds.
        val remastered = c.remasterOf?.let { release(it) }
        val nostalgia = remastered?.let { remasterLift(it.score) } ?: 1.0
        val season = GameSeason.forWeek(week)
        val window = LaunchWindow.at(week)
        val seasonalLift = season.launchMultiplier(c.genre) * (window?.lift ?: 1.0)
        // Wishlists turn into sales on the day, more of them the better the reviews.
        val wishlistGross = p.wishlists * wishlistConversion(avg) * c.size.price * c.price.revenueFactor(avg) * pastScale(ctx.year.toDouble())
        val gross = c.size.launchSales * (avg / 7).pow(3) * fanLift * fanTrust * ctx.reception * ctx.marketDemand *
            ctx.marketingBoost * (1 + p.hype / 80) * reachMult * (if (onTrend) 1.12 else 1.0) * sequelLift *
            (clash?.penalty ?: 1.0) * c.price.revenueFactor(avg) * (if (c.live) 0.85 else 1.0) * nostalgia * seasonalLift * screen * kept
        val netShare = c.platforms.sumOf {
            when (it) {
                Platform.OWN_CONSOLE -> 1.0
                Platform.PC -> pcKeep
                else -> 0.7
            }
        } / c.platforms.size * (p.deal?.share ?: 1.0)

        // The game report: what this release taught you.
        val lines = mutableListOf<String>()
        fun learn(key: String, fit: Fit) {
            knownFits[key] = fit
        }
        learn("TG:${c.topic.name}:${c.genre.name}", c.topic.fit(c.genre))
        lines += "${c.topic.title} + ${c.genre.title}: ${c.topic.fit(c.genre).title.lowercase()} combination."
        val seasonPct = ((seasonalLift - 1) * 100).roundToInt()
        lines += "${season.title} launch timing: ${season.marketTip} Your ${c.genre.lower} game received ${if (seasonPct >= 0) "+" else ""}$seasonPct% seasonal demand." +
            (window?.let { " ${it.title}: ${it.note.replaceFirstChar { ch -> ch.lowercase() }}" } ?: "")
        if (screen > 1) lines += "The ${seriesName(previous.first().title)} adaptation brought new players: launch sales were up ${Format.percent(screen - 1)}."
        if (screen < 1) lines += "The ${seriesName(previous.first().title)} adaptation put people off: launch sales were down ${Format.percent(1 - screen)}."
        if (p.wishlists >= 100) {
            lines += "${Format.count(p.wishlists)} wishlists turned into ${Format.money(wishlistGross)} of launch-week sales."
        }
        c.genre2?.let {
            learn("TG:${c.topic.name}:${it.name}", c.topic.fit(it))
            lines += "${c.topic.title} + ${it.title}: ${c.topic.fit(it).title.lowercase()} combination."
        }
        learn("TA:${c.topic.name}:${c.audience.name}", c.topic.fit(c.audience))
        lines += "${c.topic.title} for ${c.audience.phrase}: ${c.topic.fit(c.audience).title.lowercase()}."
        for (pl in c.platforms) {
            learn("PG:${pl.name}:${c.genre.name}", pl.fit(c.genre))
            lines += "${c.genre.title} on ${pl.title}: ${pl.fit(c.genre).title.lowercase()}."
        }
        lines += when {
            designRatio < c.idealDesignShare - 0.08 -> when (rng.int(0, 2)) {
                0 -> "Lean harder into design next ${c.genre.lower}."
                1 -> "${c.genre.title} wanted more design craft than this shipped with."
                else -> "Tech-heavy for a ${c.genre.lower} — dial design up next time."
            }
            designRatio > c.idealDesignShare + 0.08 -> when (rng.int(0, 2)) {
                0 -> "${c.genre.title} needed more tech under the hood."
                1 -> "Pretty ideas, thin tech — ${c.genre.lower} players noticed."
                else -> "Push the engineering side on the next ${c.genre.lower}."
            }
            else -> when (rng.int(0, 1)) {
                0 -> "Design and tech landed in the sweet spot for ${c.genre.lower}."
                else -> "Balance felt right for a ${c.genre.lower} release."
            }
        }
        val lessons = (listOf(weakest) + FocusArea.entries.shuffledWith(rng)).distinct().take(2)
        for (a in lessons) {
            insights += "${c.genre.name}:${a.name}"
            val w = c.genre.weight(a)
            lines += when (w) {
                2 -> when (rng.int(0, 2)) {
                    0 -> "${a.title} carries ${c.genre.lower} games — keep investing."
                    1 -> "Lesson: nail ${a.title.lowercase()} or ${c.genre.lower} falls flat."
                    else -> "${c.genre.title} lives or dies on ${a.title.lowercase()}."
                }
                1 -> when (rng.int(0, 1)) {
                    0 -> "${a.title} helps ${c.genre.lower}, but it's not the whole game."
                    else -> "Worth a look: ${a.title.lowercase()} in ${c.genre.lower}."
                }
                else -> when (rng.int(0, 1)) {
                    0 -> "Don't overspend on ${a.title.lowercase()} for ${c.genre.lower}."
                    else -> "${a.title} is optional in ${c.genre.lower} — save the hours."
                }
            }
        }
        if (bugPenalty < 0.95) lines += when (rng.int(0, 1)) {
            0 -> "Bugs hurt the score. Bake in more polish time."
            else -> "Ship cleaner next time — the crashes showed."
        }
        if (dated) lines += when (rng.int(0, 1)) {
            0 -> "Engine tech is lagging. Research and rebuild."
            else -> "Players clocked the dated tech. New engine features will help."
        }
        revealEngineTech(engine.features)
        if (engine.combo.notes.isNotEmpty()) lines += engine.combo.notes.first()
        if (onTrend) lines += when (rng.int(0, 1)) {
            0 -> "Catching the ${trend?.title?.lowercase()} wave helped sales."
            else -> "Right place, right time: ${trend?.title?.lowercase()}."
        }
        if (previous.isNotEmpty()) {
            val name = seriesName(seriesRoot(original!!).title)
            val pct = ((sequelLift - 1) * 100).roundToInt()
            lines += "The $name series (${previous.size} game${if (previous.size > 1) "s" else ""} before this, averaging " +
                String.format(java.util.Locale.US, "%.1f", seriesAvg) + ") " +
                (if (pct >= 0) "brought its fans: +$pct% at launch." else "has worn thin: $pct% at launch.") +
                (if (spinOff) " As a spin-off, it got half the series effect." else "") +
                (if (fatigued) " Players are tiring of it." else "")
        }
        if (remastered != null) {
            lines += "A remaster of ${remastered.title} (${String.format(java.util.Locale.US, "%.1f", remastered.score)}): " +
                "it took half the work, and launch sales came in at ${(nostalgia * 100).roundToInt()}% of a new game's. " +
                "The better the original, the more players come back."
        }
        val priceEffect = c.price.revenueFactor(avg)
        if (c.price != PriceTier.STANDARD) {
            lines += "At ${money(c.unitPrice)} (${c.price.title.lowercase()}), launch revenue came out " +
                (if (priceEffect >= 1) "${((priceEffect - 1) * 100).roundToInt()}% higher" else "${((1 - priceEffect) * 100).roundToInt()}% lower") +
                " than at the standard price."
        }
        if (clash != null) {
            val rival = clash.game
            lines += if (clash.mega) {
                "You launched days after ${rival.title}. Everyone was busy playing it, and your launch week suffered."
            } else {
                "${rival.title} by ${rival.studio} came out weeks earlier in the same genre and took some of your buyers."
            }
        }

        val release = GameRelease(
            id = nextId(), title = p.title, concept = c, week = week, reviews = reviews, score = avg, weeklySales = gross,
            netShare = netShare, design = p.design, tech = p.tech, bugs = p.bugs, hype = p.hype, devCost = p.spent,
            report = lines, publisher = p.deal?.publisher,
            userScore = verdict.userScore, userScoreSettled = false, firstUserScore = verdict.userScore,
            aspects = verdict.aspects, pros = verdict.pros, cons = verdict.cons, playerQuotes = verdict.players,
            live = if (c.live) LiveService(livePlayers(gross / c.unitPrice), week) else null,
            breakdown = listOf(ScorePart("Baseline", SCORE_BASELINE, "Where every game starts.")) + parts,
            wishlists = p.wishlists,
        )
        // The wishlist buyers come once, on the day; the weekly sales that tail off come from everyone else.
        report.revenue += wishlistGross * netShare
        if (c.live) {
            lines += "It launched as a live game with ${Format.count(release.live!!.players)} players. Keep them with a new season every few months."
        }
        releases.add(0, release)
        setProject(team, null)
        unseenReview = release.id
        topQuality = max(topQuality, q)
        topScore = max(topScore, avg)
        report.add(Incident.Stat(StatKey.RELEASE))

        val buyers = (gross + wishlistGross) / c.unitPrice
        val scoreText = String.format(java.util.Locale.US, "%.1f", avg)
        when {
            avg >= 8 -> {
                hits += 1
                fanTrust = min(1.2, fanTrust + 0.05)
                fans += buyers * 0.35
                report.add(Incident.Stat(StatKey.HIT))
                report.add(Incident.Reputation(4.0))
                report.log("${p.title} reviews at $scoreText/10. Launch week: ${money(gross + wishlistGross)}. It's a hit.", LogTone.MILESTONE)
            }
            avg >= 5 -> {
                fans += buyers * 0.15
                report.log("${p.title} reviews at $scoreText/10. Launch week: ${money(gross + wishlistGross)}.", LogTone.GOOD)
            }
            else -> {
                flops += 1
                fanTrust = max(0.3, fanTrust - 0.2)
                fans *= 0.85
                report.add(Incident.Stat(StatKey.FLOP))
                val severity = if (c.size == GameSize.SMALL) 0.15 else 0.35
                report.add(Incident.Scar(ScarKind.FLOP, severity, "${p.title} flopped with $scoreText/10."))
                report.log("${p.title} flops at $scoreText/10. The fans will remember this one.", LogTone.BAD)
            }
        }
        if (p.hype > 60 && avg < 6) {
            fanTrust = max(0.3, fanTrust - 0.1)
            report.log("All that hype, and this is it? Fans feel misled.", LogTone.BAD)
        }
        p.deal?.let { d ->
            if (avg >= d.minScore) {
                report.revenue += d.advance * 0.5
                report.log("${d.publisher} is happy with ${p.title} and pays the rest of the advance: ${money(d.advance * 0.5)}.", LogTone.GOOD)
            } else {
                report.expenses += d.advance * 0.25
                report.log("${p.title} missed ${d.publisher}'s ${d.minScore}/10 target. They want ${money(d.advance * 0.25)} back.", LogTone.BAD)
            }
        }
        return report
    }

    // Offers

    internal fun refreshOffers(
        week: Int, year: Int, output: Double, rng: SeededRNG, unlockedSizes: List<GameSize>, platforms: List<Platform>,
    ) {
        if (week % 4 == 0 && contractOffers.size < 2) {
            val work = rng.range(15.0, 45.0) * (1 + output / 40)
            val clients = brandsIn(year) + GameNames.clients.filter { companyActive(it, year) }
            val title = rng.pick(
                listOf(
                    "Port a puzzle game to ${if (year < 2007) "a handheld" else "the iPhone"}",
                    "An advergame for ${rng.pick(clients)}",
                    "Bug-fix sprint for ${rng.pick(publishersIn(year).ifEmpty { listOf("a publisher") })}",
                    "A level pack for ${rng.pick(publishersIn(year).ifEmpty { listOf("Electronic Arts") })}",
                    "A tech demo for ${rng.pick(clients)}",
                    "An educational game for ${rng.pick(clients)}",
                ),
            )
            contractOffers += GameContract(
                id = nextId(), title = title, work = work, pay = (work * 330 * rng.range(0.9, 1.3)).roundToInt().toDouble(),
                weeksAllowed = ceil(work / max(3.0, output * 0.8)).toInt() + 3, rp = work * 0.15, expiresWeek = week + 6,
            )
        }
        if (week % 13 == 0 && releases.isNotEmpty() && dealOffers.size < 2 && rng.chance(0.6)) {
            val size = unlockedSizes.filter { it <= GameSize.LARGE }.last()
            val minScore = rng.int(5, 8)
            val genre = rng.pick(Genre.entries)
            val topic = topics.filter { it.fit(genre) != Fit.BAD }.ifEmpty { topics }.let { rng.pick(it) }
            val platform = rng.pick(platforms.ifEmpty { listOf(Platform.PC) })
            val pubs = publishersIn(year).ifEmpty { GameNames.publishers }
            dealOffers += PublisherDeal(
                id = nextId(), publisher = rng.pick(pubs), topic = topic, genre = genre, platform = platform,
                size = size, minScore = minScore, advance = (size.launchSales * rng.range(0.6, 1.1) * minScore / 6).roundToInt().toDouble(),
                share = 0.35, expiresWeek = week + 8,
            )
        }
    }

    // Value and status

    /** Back catalogue, fans and engines count toward valuation. */
    internal val assetValue: Double get() = fans * 4 * fanTrust + engines.sumOf { it.level } * 20_000.0

    val statusLine: String
        get() {
            listOfNotNull(project, project2).takeIf { it.isNotEmpty() }?.let { ps ->
                return ps.joinToString("; ") { p ->
                    "${p.title} (${p.concept.size.title} ${p.concept.genreTitle}) ${(p.completion * 100).toInt()}%" + if (p.isReady) ", polishing" else ""
                }
            }
            contract?.let { return "Contract: ${it.title} ${(it.completion * 100).toInt()}%" }
            expansion?.let { return "Expansion: ${it.title} ${(it.completion * 100).toInt()}%" }
            return if (releases.isEmpty()) "No game in development" else "Between projects"
        }
}

/** Intermediate score build shared by launch and [GamesStudio.forecast]. */
private data class ScoreDraft(
    val parts: List<ScorePart>,
    val score: Double,
    val craft: Double,
    val designRatio: Double,
    val bugPenalty: Double,
    val audienceFit: Fit,
    val onTrend: Boolean,
    val repeated: Boolean,
    val rushedSequel: Boolean,
    val stepBack: Boolean,
    val record: Boolean,
    val original: GameRelease?,
    val dated: Boolean,
    val standard: Double,
    val engine: Engine,
)

private fun <T> List<T>.shuffledWith(rng: SeededRNG): List<T> {
    val list = toMutableList()
    for (i in list.indices.reversed()) {
        val j = rng.int(0, i)
        val t = list[i]
        list[i] = list[j]
        list[j] = t
    }
    return list
}
