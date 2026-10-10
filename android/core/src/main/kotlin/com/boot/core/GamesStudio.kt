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
    val work: Double get() = concept.work
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
    /** What players think, 1...10. Word of mouth: it decides how long the game keeps selling. -1 on old saves. */
    val userScore: Double = -1.0,
    val aspects: Map<ReviewAspect, Double> = emptyMap(),
    val pros: List<String> = emptyList(),
    val cons: List<String> = emptyList(),
    val salesHistory: MutableList<Double> = mutableListOf(),
    /** The community of a live game. */
    var live: LiveService? = null,
    /** The indie who made it, when your label published it. */
    val developer: String? = null,
    /** Its pro league, if it has one. */
    var league: League? = null,
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

    /** Side income last week, for the studio screen. */
    var lastRoyalties = 0.0
    var lastMerch = 0.0

    /** Studios you've bought. */
    val subsidiaries = mutableListOf<Subsidiary>()

    /** Your label: pitches waiting for an answer, games you've funded, and the ones you've put out. */
    val pitches = mutableListOf<IndiePitch>()
    val signed = mutableListOf<SignedGame>()
    val published = mutableListOf<GameRelease>()

    /** Releases whose fans have petitioned for a sequel, and the one you promised. */
    val petitions: MutableSet<Int> = mutableSetOf()
    var promisedSequel: Int? = null
    private var nextId = 1

    internal fun nextId(): Int = nextId++

    val weeklySales: Double get() = releases.sumOf { it.weeklySales * it.netShare } + published.sumOf { it.weeklySales * it.netShare }
    val bestGame: GameRelease? get() = releases.maxByOrNull { it.score }

    fun engine(id: Int): Engine = engines.firstOrNull { it.id == id } ?: engines.first()

    fun knownFit(topic: Topic, genre: Genre): Fit? = knownFits["TG:${topic.name}:${genre.name}"]
    fun knownFit(topic: Topic, audience: Audience): Fit? = knownFits["TA:${topic.name}:${audience.name}"]
    fun knownFit(platform: Platform, genre: Genre): Fit? = knownFits["PG:${platform.name}:${genre.name}"]
    fun knowsWeight(genre: Genre, area: FocusArea): Boolean = "${genre.name}:${area.name}" in insights

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
    fun nextTitle(releaseId: Int, genre: Genre? = null, reroll: Int = 0): String {
        val r = release(releaseId) ?: return "Untitled"
        val entries = series(seriesRoot(r))
        val latest = entries.last()
        val name = seriesName(seriesRoot(r).title)
        val n = entries.size + 1
        val used = releases.map { it.title }.toSet()
        val title = if (genre != null && genre != latest.concept.genre) {
            spinOffTitle(name, genre, reroll)
        } else {
            sequelTitle(name, n, latest.concept.topic, latest.id.toLong() + reroll * 101L)
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
    internal fun suggestTitle(rng: SeededRNG, topic: Topic = Topic.FANTASY, genre: Genre = Genre.ACTION): String =
        gameTitle(topic, genre, rng.int(0, 1_000_000).toLong(), releases.map { it.title }.toSet() + listOfNotNull(project?.title, project2?.title))

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
            report.revenue += r.weeklySales * r.netShare
            r.lifetimeSales += r.weeklySales * r.netShare
            r.unitsSold += r.weeklySales / r.concept.unitPrice
            r.salesHistory += r.weeklySales
            if (r.salesHistory.size > 52) r.salesHistory.removeAt(0)
            r.weeklySales *= salesDecay(age, if (age < 6) r.score else r.wordOfMouth) * (if (r.patched) 1.01 else 1.0)
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
        val budget = p.concept.size.weeklyBudget * ctx.mods.cost * (if (p.deal != null) 0.5 else 1.0) * (if (p.concept.live) 1.25 else 1.0)
        p.spent += budget
        report.expenses += budget
    }

    // Launch, reviews and the game report

    internal fun launch(ctx: LaneContext, rng: SeededRNG, reach: Double, audienceSuited: Boolean, clash: Clash? = null, team: Int = 1): LaneReport {
        val p = projectOf(team) ?: throw invalid("No game in development.")
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
        val repeated = c.sequelOf == null && c.remasterOf == null && last != null && last.concept.topic == c.topic && last.concept.genre == c.genre
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

        // The critics, and then the players.
        val areas = FocusArea.entries
        val weakest = areas.maxByOrNull { a -> c.idealShare(a) - p.focus[a.stage][a.ordinal % 3] / max(0.01, p.focus[a.stage].sum()) }!!
        val verdict = writeReviews(
            ReviewInputs(
                title = p.title, concept = c, focus = p.focus, score = score, designRatio = designRatio, focusMatch = p.focusMatch,
                bugPenalty = bugPenalty, polish = p.polish / p.work, engineLevel = engine.level, engineStandard = standard,
                hype = p.hype, fanTrust = fanTrust, onTrend = onTrend, repeated = repeated, rushedSequel = rushedSequel,
                worthySequel = original != null && original.score >= 7 && !rushedSequel, stepBack = stepBack, record = record,
                wrongCrowd = audienceFit < 0.9, awkwardPlatform = c.platforms.firstOrNull { it.fit(c.genre) == Fit.BAD }, price = c.price,
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
        // A remaster sells to the people who remember the original, and fewer of them than a new game finds.
        val remastered = c.remasterOf?.let { release(it) }
        val nostalgia = remastered?.let { remasterLift(it.score) } ?: 1.0
        val gross = c.size.launchSales * (avg / 7).pow(3) * fanLift * fanTrust * ctx.reception * ctx.marketDemand *
            ctx.marketingBoost * (1 + p.hype / 80) * reachMult * (if (onTrend) 1.12 else 1.0) * sequelLift *
            (clash?.penalty ?: 1.0) * c.price.revenueFactor(avg) * (if (c.live) 0.85 else 1.0) * nostalgia
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
            userScore = verdict.userScore, aspects = verdict.aspects, pros = verdict.pros, cons = verdict.cons,
            live = if (c.live) LiveService(livePlayers(gross / c.unitPrice), week) else null,
        )
        if (c.live) {
            lines += "It launched as a live game with ${Format.count(release.live!!.players)} players. Keep them with a new season every few months."
        }
        releases.add(0, release)
        setProject(team, null)
        unseenReview = release.id
        topQuality = max(topQuality, q)
        topScore = max(topScore, avg)
        report.add(Incident.Stat(StatKey.RELEASE))

        val buyers = gross / c.unitPrice
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
                    "Port a puzzle game to the iPhone", "An advergame for ${rng.pick(GameNames.clients)}",
                    "Bug-fix sprint for Ubisoft", "A level pack for Electronic Arts", "A tech demo for ${rng.pick(GameNames.clients)}",
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
