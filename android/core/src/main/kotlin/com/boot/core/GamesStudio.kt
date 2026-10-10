package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.exp
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

/** New content for a released game. The whole team works on it, like a small game. */
@Serializable
data class GameExpansion(
    val releaseId: Int,
    val title: String,
    val work: Double,
    val weeklyBudget: Double,
    var done: Double = 0.0,
    var weeks: Int = 0,
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
) {
    val title: String get() = concept.title
    val work: Double get() = concept.size.work
    val isReady: Boolean get() = progress >= work
    val completion: Double get() = min(1.0, progress / work)

    /** 0, 1 or 2 while in development. */
    val stage: Int get() = min(2, (progress / work * 3).toInt())

    val focusMatch: Double get() = if (matchWork > 0) matchSum / matchWork else 0.5
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
    val netShare: Double,
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
    val salesHistory: MutableList<Double> = mutableListOf(),
) {
    /** 0...100, for the rest of the company's bookkeeping. */
    val review: Int get() = (score * 10).roundToInt()

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

@Serializable
class GamesStudio {
    var project: GameProject? = null
    var contract: GameContract? = null
    var expansion: GameExpansion? = null

    /** Something is keeping the team busy. */
    val busy: Boolean get() = project != null || contract != null || expansion != null
    val contractOffers = mutableListOf<GameContract>()
    val dealOffers = mutableListOf<PublisherDeal>()
    val releases = mutableListOf<GameRelease>()
    var fans = 0.0

    /** 0.3...1.2. Flops knock it down and only hits bring it back. The fanbase never forgets. */
    var fanTrust = 1.0
    var hits = 0
    var flops = 0
    val topics: MutableList<Topic> = Topic.entries.filter { it.starter }.toMutableList()
    val engines = mutableListOf(Engine(0, "Starter kit", emptyList(), 0))
    var engineBuild: EngineBuild? = null

    /** What game reports have taught you: fits keyed "TG:topic:genre", "TA:topic:audience", "PG:platform:genre". */
    val knownFits: MutableMap<String, Fit> = mutableMapOf()

    /** Genre focus lessons, keyed "GENRE:AREA". */
    val insights: MutableSet<String> = mutableSetOf()
    var trend: Trend? = null
    var topQuality = 0.0
    var topScore = 0.0

    /** A review the player hasn't looked at yet. */
    var unseenReview: Int? = null

    /** Work done on games or contracts last week, for research. */
    var lastWork = 0.0

    /** Rival games, the sales chart and the awards. */
    val market = GameMarket()

    /** Releases whose fans have petitioned for a sequel, and the one you promised. */
    val petitions: MutableSet<Int> = mutableSetOf()
    var promisedSequel: Int? = null
    private var nextId = 1

    internal fun nextId(): Int = nextId++

    val weeklySales: Double get() = releases.sumOf { it.weeklySales * it.netShare }
    val bestGame: GameRelease? get() = releases.maxByOrNull { it.score }

    fun engine(id: Int): Engine = engines.firstOrNull { it.id == id } ?: engines.first()

    fun knownFit(topic: Topic, genre: Genre): Fit? = knownFits["TG:${topic.name}:${genre.name}"]
    fun knownFit(topic: Topic, audience: Audience): Fit? = knownFits["TA:${topic.name}:${audience.name}"]
    fun knownFit(platform: Platform, genre: Genre): Fit? = knownFits["PG:${platform.name}:${genre.name}"]
    fun knowsWeight(genre: Genre, area: FocusArea): Boolean = "${genre.name}:${area.name}" in insights

    fun release(id: Int): GameRelease? = releases.firstOrNull { it.id == id }

    /** Suggest a name nobody has used. */
    internal fun suggestTitle(rng: SeededRNG): String {
        val used = releases.map { it.title }.toSet() + listOfNotNull(project?.title)
        repeat(20) {
            val t = "${rng.pick(GameNames.first)} ${rng.pick(GameNames.second)}"
            if (t !in used) return t
        }
        return "Untitled ${releases.size + 1}"
    }

    // Weekly simulation

    internal fun tick(ctx: LaneContext, rng: SeededRNG): LaneReport {
        val report = LaneReport()
        val work = ctx.workOutput
        lastWork = 0.0

        engineBuild?.let { b ->
            val left = b.weeksLeft - 1
            if (left <= 0) {
                val engine = Engine(nextId(), b.name, b.features, ctx.week)
                engines += engine
                engineBuild = null
                report.log("${engine.name} is finished: tech level ${engine.level}. New games can use it.", LogTone.MILESTONE)
            } else {
                engineBuild = b.copy(weeksLeft = left)
            }
        }

        project?.let { p ->
            lastWork = work
            p.weeksInDev += 1
            val engineMult = engine(p.concept.engineId).multiplier
            if (!p.isReady) {
                val stage = p.stage
                val alloc = p.focus[stage]
                val total = alloc.sum().takeIf { it > 0 } ?: 1.0
                val areas = FocusArea.forStage(stage)
                val designShare = areas.indices.sumOf { alloc[it] / total * areas[it].designShare }
                val d = work * 2 * designShare * (ctx.staffing.avgDesign / 5) * engineMult
                val t = work * 2 * (1 - designShare) * (ctx.staffing.avgTech / 5) * engineMult
                val b = work * 0.3 * (1.3 - ctx.staffing.avgTech / 10) * (if (ctx.crunch) 1.6 else 1.0)
                p.design += d
                p.tech += t
                p.bugs += b
                p.lastDesign = d
                p.lastTech = t
                p.lastBugs = b
                p.matchSum += focusMatch(p.concept, stage, alloc) * work
                p.matchWork += work
                val wasStage = stage
                p.progress += work
                if (p.isReady) {
                    report.log("${p.title} is content complete. Fix bugs and polish, or ship it now.", LogTone.GOOD)
                } else if (p.stage != wasStage) {
                    report.log("${p.title} moves on to ${FocusArea.STAGES[p.stage].lowercase()}.")
                }
            } else {
                // Bug fixing and polish once the content is in.
                val fixed = min(p.bugs, work * 0.6)
                p.bugs -= fixed
                p.polish = min(p.work * 0.4, p.polish + work * 0.5 * ctx.polishEfficiency)
                p.lastDesign = 0.0
                p.lastTech = 0.0
                p.lastBugs = -fixed
            }
            p.hype *= 0.99
            val budget = p.concept.size.weeklyBudget * ctx.mods.cost * (if (p.deal != null) 0.5 else 1.0)
            p.spent += budget
            report.expenses += budget
        }

        contract?.let { c ->
            lastWork = work
            c.done += work
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

        expansion?.let { x ->
            lastWork = work
            x.done += work
            x.weeks += 1
            val budget = x.weeklyBudget * ctx.mods.cost
            report.expenses += budget
            if (x.done >= x.work) {
                expansion = null
                release(x.releaseId)?.let { r ->
                    r.expansions += x.title
                    val attach = (0.3 * (r.score / 8).pow(2)).clamped(0.05, 0.5)
                    val buyers = r.unitsSold * attach
                    r.weeklySales += buyers * r.concept.size.price * 0.4 * 0.3 * ctx.marketDemand
                    fans += buyers * 0.1
                    report.log("${x.title} is out. About ${Format.count(buyers)} players came back for more.", LogTone.GOOD)
                }
            }
        }

        contractOffers.removeAll { it.expiresWeek < ctx.week }
        dealOffers.removeAll { it.expiresWeek < ctx.week }

        for (r in releases) {
            if (r.weeklySales <= 0) continue
            val age = ctx.week - r.week
            report.revenue += r.weeklySales * r.netShare
            r.lifetimeSales += r.weeklySales * r.netShare
            r.unitsSold += r.weeklySales / r.concept.size.price
            r.salesHistory += r.weeklySales
            if (r.salesHistory.size > 52) r.salesHistory.removeAt(0)
            r.weeklySales *= salesDecay(age, r.score) * (if (r.patched) 1.01 else 1.0)
            if (r.weeklySales < 40) {
                r.weeklySales = 0.0
                r.chartRank = 0
            }
        }
        return report
    }

    // Launch, reviews and the game report

    internal fun launch(ctx: LaneContext, rng: SeededRNG, reach: Double, audienceSuited: Boolean, clash: RivalGame? = null): LaneReport {
        val p = project ?: throw invalid("No game in development.")
        if (!p.isReady) throw invalid("${p.title} isn't content complete yet.")
        val c = p.concept
        val report = LaneReport()
        val week = ctx.week
        val engine = engine(c.engineId)

        // Quality: points against what the era expects, then everything that makes a game fit together.
        val eraTech = 1 + 0.1 * week / 52.0
        val base = (p.design + p.tech) / (2.6 * p.work * eraTech)
        val designRatio = p.design / max(1.0, p.design + p.tech)
        val ratioFit = (1 - abs(designRatio - c.idealDesignShare) * 2.2).clamped(0.45, 1.0)
        val focusMult = 0.7 + 0.4 * p.focusMatch
        val audienceFit = when (c.topic.fit(c.audience)) {
            Fit.GOOD, Fit.GREAT -> 1.05
            Fit.OKAY -> 0.97
            Fit.BAD -> 0.82
        } * (if (audienceSuited) 1.0 else 0.92)
        val platformFit = c.platforms.map {
            when (it.fit(c.genre)) {
                Fit.GOOD, Fit.GREAT -> 1.06
                Fit.OKAY -> 1.0
                Fit.BAD -> 0.85
            }
        }.average()
        val bugPenalty = 1 - min(0.35, p.bugs / (p.work * 0.6) * 0.35)
        val polishBonus = 1 + min(0.06, 0.2 * p.polish / p.work)
        val last = releases.firstOrNull()
        val repeated = c.sequelOf == null && last != null && last.concept.topic == c.topic && last.concept.genre == c.genre
        val original = c.sequelOf?.let { release(it) }
        val rushedSequel = original != null && week - original.week < 52
        val onTrend = trend?.matches(c) == true
        val standard = week / 20.0
        val dated = engine.level < standard - 5
        val engineFit = if (dated) max(0.82, 1 - 0.015 * (standard - 5 - engine.level)) else 1.0
        val q = base * ratioFit * focusMult * c.comboFit * audienceFit * platformFit * bugPenalty * polishBonus *
            (if (repeated) 0.85 else 1.0) * (if (rushedSequel) 0.92 else 1.0) * (if (onTrend) 1.08 else 1.0) *
            engineFit * (1 - 0.02 * (c.platforms.size - 1)) * (1 + ctx.mods.quality / 100)

        // Like any reviewer, they judge you against your own best work: keep improving or scores slide.
        val expectation = max(0.95, topQuality)
        val ratio = q / expectation
        val score = (10 * (1 - exp(-(ratio / 0.85).pow(2.4)))).clamped(1.0, 10.0)
        val stepBack = topQuality > 0 && ratio < 0.85
        val record = q > topQuality && releases.isNotEmpty()

        // Four outlets, four opinions.
        val areas = FocusArea.entries
        val weakest = areas.maxByOrNull { a -> c.idealShare(a) - p.focus[a.stage][a.ordinal % 3] / max(0.01, p.focus[a.stage].sum()) }!!
        val candidates = mutableListOf<Pair<Double, String>>()
        when (c.topic.fit(c.genre)) {
            Fit.GREAT -> candidates += 2.0 to "${c.topic.title} and ${c.genre.lower} games were made for each other."
            Fit.BAD -> candidates += 3.0 to "Who asked for ${Format.a(c.topic.title.lowercase())} ${c.genre.lower} game?"
            else -> {}
        }
        if (designRatio < c.idealDesignShare - 0.12) candidates += 2.0 to "All tech, no soul."
        if (designRatio > c.idealDesignShare + 0.12) candidates += 2.0 to "Thin on the technical side."
        if (p.focusMatch < 0.55) candidates += 2.0 to "${weakest.title} felt like an afterthought."
        if (p.focusMatch > 0.85) candidates += 1.5 to "Every part of it pulls its weight."
        if (bugPenalty < 0.9) candidates += 3.0 to "Crashed twice in the first hour."
        if (dated) candidates += 2.0 to "Looks like it came out five years ago."
        if (onTrend) candidates += 1.0 to "Right on trend."
        if (repeated) candidates += 2.0 to "Haven't we played this before?"
        if (rushedSequel) candidates += 2.0 to "A rushed sequel."
        if (stepBack) candidates += 2.5 to "A step back from their last game."
        if (record && score >= 8) candidates += 2.0 to "Their best work yet."
        if (audienceFit < 0.9) candidates += 1.5 to "Made for the wrong crowd."
        candidates += 1.0 to when {
            score >= 9 -> rng.pick(listOf("A must-play.", "An instant classic."))
            score >= 7 -> rng.pick(listOf("Really fun.", "Easy to recommend."))
            score >= 5 -> rng.pick(listOf("Solid, if unremarkable.", "Has its moments."))
            else -> rng.pick(listOf("Hard to recommend.", "Skip it."))
        }
        val reviews = Outlet.entries.map { outlet ->
            val s = (score + outlet.bias(c, 1 - designRatio) + rng.normal(sd = 0.45)).roundToInt().clamped(1, 10)
            val total = candidates.sumOf { it.first }
            var roll = rng.unit() * total
            var comment = candidates.last().second
            for ((w, text) in candidates) {
                roll -= w
                if (roll <= 0) {
                    comment = text
                    break
                }
            }
            Review(outlet, s, comment)
        }
        val avg = reviews.map { it.score }.average()

        // Launch week.
        val fanLift = 1 + 1.5 * fans / (fans + c.size.fanScale)
        val reachMult = reach.pow(0.75).clamped(0.25, 2.0)
        val sequelLift = if (original != null && original.score >= 7) 1.15 else 1.0
        val gross = c.size.launchSales * (avg / 7).pow(3) * fanLift * fanTrust * ctx.reception * ctx.marketDemand *
            ctx.marketingBoost * (1 + p.hype / 80) * reachMult * (if (onTrend) 1.12 else 1.0) * sequelLift *
            (if (clash != null) 0.85 else 1.0)
        val ownShare = c.platforms.count { it == Platform.OWN_CONSOLE }.toDouble() / c.platforms.size
        val netShare = (0.7 + 0.3 * ownShare) * (p.deal?.share ?: 1.0)

        // The game report: what this release taught you.
        val lines = mutableListOf<String>()
        fun learn(key: String, fit: Fit) {
            knownFits[key] = fit
        }
        learn("TG:${c.topic.name}:${c.genre.name}", c.topic.fit(c.genre))
        lines += "${c.topic.title} + ${c.genre.title}: ${c.topic.fit(c.genre).title.lowercase()} combination."
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
            designRatio < c.idealDesignShare - 0.08 -> "${c.genre.title} games want more design than this had."
            designRatio > c.idealDesignShare + 0.08 -> "${c.genre.title} games want more tech than this had."
            else -> "The design and tech balance was right for ${c.genre.lower}."
        }
        val lessons = (listOf(weakest) + FocusArea.entries.shuffledWith(rng)).distinct().take(2)
        for (a in lessons) {
            insights += "${c.genre.name}:${a.name}"
            lines += "${a.title} " + when (c.genre.weight(a)) {
                2 -> "matters a lot in ${c.genre.lower} games."
                1 -> "matters somewhat in ${c.genre.lower} games."
                else -> "barely matters in ${c.genre.lower} games."
            }
        }
        if (bugPenalty < 0.95) lines += "Bugs cost you. Polish longer next time."
        if (dated) lines += "Your engine is behind the times. Research and build a new one."
        if (onTrend) lines += "Riding the ${trend?.title?.lowercase()} trend helped sales."
        if (clash != null) {
            lines += "${clash.title} by ${clash.studio} came out weeks earlier in the same genre and took some of your buyers."
        }

        val release = GameRelease(
            id = nextId(), title = p.title, concept = c, week = week, reviews = reviews, score = avg, weeklySales = gross,
            netShare = netShare, design = p.design, tech = p.tech, bugs = p.bugs, hype = p.hype, devCost = p.spent,
            report = lines, publisher = p.deal?.publisher,
        )
        releases.add(0, release)
        project = null
        unseenReview = release.id
        topQuality = max(topQuality, q)
        topScore = max(topScore, avg)
        report.add(Incident.Stat(StatKey.RELEASE))

        val buyers = gross / c.size.price
        val scoreText = String.format(java.util.Locale.US, "%.1f", avg)
        when {
            avg >= 8 -> {
                hits += 1
                fanTrust = min(1.2, fanTrust + 0.05)
                fans += buyers * 0.35
                report.add(Incident.Stat(StatKey.HIT))
                report.add(Incident.Reputation(4.0))
                report.log("${p.title} reviews at $scoreText/10. Launch week: ${money(gross)}. It's a hit.", LogTone.MILESTONE)
            }
            avg >= 5 -> {
                fans += buyers * 0.15
                report.log("${p.title} reviews at $scoreText/10. Launch week: ${money(gross)}.", LogTone.GOOD)
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

    internal fun refreshOffers(week: Int, output: Double, rng: SeededRNG, unlockedSizes: List<GameSize>, platforms: List<Platform>) {
        if (week % 4 == 0 && contractOffers.size < 2) {
            val work = rng.range(15.0, 45.0) * (1 + output / 40)
            val title = rng.pick(
                listOf(
                    "Port a puzzle game to the grPhone", "An advergame for ${rng.pick(GameNames.clients)}",
                    "Bug-fix sprint for Gigaplay", "A level pack for a rival", "A tech demo for ${rng.pick(GameNames.clients)}",
                    "An educational game for ${rng.pick(GameNames.clients)}",
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
            dealOffers += PublisherDeal(
                id = nextId(), publisher = rng.pick(GameNames.publishers), topic = topic, genre = genre, platform = platform,
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
            project?.let { p ->
                return "${p.title} (${p.concept.size.title} ${p.concept.genreTitle}) ${(p.completion * 100).toInt()}%" +
                    if (p.isReady) ", polishing" else ""
            }
            contract?.let { return "Contract: ${it.title} ${(it.completion * 100).toInt()}%" }
            expansion?.let { return "Expansion: ${it.title} ${(it.completion * 100).toInt()}%" }
            return if (releases.isEmpty()) "No game in development" else "Between projects"
        }
}

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
