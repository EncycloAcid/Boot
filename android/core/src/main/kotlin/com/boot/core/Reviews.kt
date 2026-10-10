package com.boot.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.roundToInt

/*
 * Reviews. Six outlets, each with its own taste and its own way of scoring; a category breakdown
 * that shows where the game shone and where it didn't; pros and cons; and a player score that
 * can disagree with the critics and decides how long a game keeps selling.
 */

/** How an outlet writes its score. */
enum class ScoreStyle { OUT_OF_TEN, QUARTERS, STARS, PERCENT }

/** Game review outlets. Old saves' parody outlets map onto the closest real ones. */
@Serializable
enum class Outlet(val title: String, val style: ScoreStyle) {
    @SerialName("PIXEL_GAZETTE") IGN("IGN", ScoreStyle.OUT_OF_TEN),
    @SerialName("GAMESPOTTY") GAMESPOT("GameSpot", ScoreStyle.OUT_OF_TEN),
    @SerialName("CRITICAL_HIT") GAME_INFORMER("Game Informer", ScoreStyle.QUARTERS),
    @SerialName("LOADING_SCREEN") EUROGAMER("Eurogamer", ScoreStyle.STARS),
    PC_GAMER("PC Gamer", ScoreStyle.PERCENT),
    EDGE("Edge", ScoreStyle.OUT_OF_TEN);

    /** A score out of ten, the way this outlet prints it. */
    fun format(score: Double): String = when (style) {
        ScoreStyle.OUT_OF_TEN -> "${score.roundToInt().coerceIn(1, 10)}"
        ScoreStyle.QUARTERS -> {
            val q = (score * 4).roundToInt() / 4.0
            if (q % 1.0 == 0.0) "${q.toInt()}" else String.format(java.util.Locale.US, "%.2f", q).trimEnd('0')
        }
        ScoreStyle.STARS -> "★".repeat(stars(score)) + "☆".repeat(5 - stars(score))
        ScoreStyle.PERCENT -> "${(score * 10).roundToInt().coerceIn(5, 100)}%"
    }

    /** What the scale is out of, for the small print. */
    val scale: String
        get() = when (style) {
            ScoreStyle.OUT_OF_TEN, ScoreStyle.QUARTERS -> "/10"
            ScoreStyle.STARS, ScoreStyle.PERCENT -> ""
        }

    companion object {
        fun stars(score: Double): Int = (score / 2).roundToInt().coerceIn(1, 5)
    }
}

@Serializable
data class Review(
    val outlet: Outlet,
    /** Rounded to a whole number out of ten. */
    val score: Int,
    val comment: String,
    /** The outlet's score before rounding into its own format. */
    val exact: Double = score.toDouble(),
) {
    val formatted: String get() = outlet.format(exact)
}

/** The parts of a game reviewers and players talk about. */
@Serializable
enum class ReviewAspect(val title: String, internal val areas: List<FocusArea>) {
    GAMEPLAY("Gameplay", listOf(FocusArea.GAMEPLAY, FocusArea.LEVEL_DESIGN, FocusArea.AI)),
    STORY("Story & world", listOf(FocusArea.STORY, FocusArea.DIALOGUES, FocusArea.WORLD_DESIGN)),
    VISUALS("Visuals", listOf(FocusArea.ENGINE, FocusArea.GRAPHICS)),
    SOUND("Sound", listOf(FocusArea.SOUND)),
    POLISH("Polish", emptyList());

    /** A line for the pros, and ways a reviewer might put it. */
    internal val strength: Pair<String, List<String>>
        get() = when (this) {
            GAMEPLAY -> "Superb moment-to-moment gameplay" to
                listOf("the moment-to-moment gameplay is superb", "it just feels great to play", "the core loop is hard to put down")
            STORY -> "A story and world worth getting lost in" to
                listOf("its story and world are worth getting lost in", "the writing is genuinely moving", "the world-building is superb")
            VISUALS -> "Gorgeous to look at" to
                listOf("it looks gorgeous", "every frame is a screenshot", "the art direction is stunning")
            SOUND -> "A fantastic soundtrack" to
                listOf("the soundtrack is fantastic", "the sound design is superb", "the score will stay with you")
            POLISH -> "Rock solid at launch" to
                listOf("it runs like a dream", "it's remarkably polished", "we didn't hit a single bug")
        }

    internal val weakness: Pair<String, List<String>>
        get() = when (this) {
            GAMEPLAY -> "Gameplay gets repetitive" to
                listOf("the gameplay gets repetitive fast", "it's a chore to actually play", "the controls fight you")
            STORY -> "A thin story and world" to
                listOf("the story and world feel thin", "the writing falls flat", "there's no reason to care about anyone in it")
            VISUALS -> "Dated visuals" to
                listOf("it looks dated", "the visuals are muddy", "it's ugly in places")
            SOUND -> "Forgettable audio" to
                listOf("the audio is forgettable", "the same few songs loop forever", "the sound design is flat")
            POLISH -> "Buggy at launch" to
                listOf("bugs and crashes keep getting in the way", "it crashed on us more than once", "it needed another month in the oven")
        }
}

/** Everything the reviewers look at, gathered at launch. */
internal data class ReviewInputs(
    val title: String,
    val concept: GameConcept,
    /** Effort per stage, three areas each, as the player left the sliders. */
    val focus: List<List<Double>>,
    /** The underlying score, 1...10, before any outlet's taste. */
    val score: Double,
    val designRatio: Double,
    val focusMatch: Double,
    val bugPenalty: Double,
    val polish: Double,
    val engineLevel: Int,
    val engineStandard: Double,
    val hype: Double,
    val fanTrust: Double,
    val onTrend: Boolean,
    val repeated: Boolean,
    val rushedSequel: Boolean,
    val worthySequel: Boolean,
    val stepBack: Boolean,
    val record: Boolean,
    val wrongCrowd: Boolean,
    val awkwardPlatform: Platform?,
    val price: PriceTier = PriceTier.STANDARD,
)

internal data class ReviewResult(
    val reviews: List<Review>,
    val average: Double,
    val userScore: Double,
    val aspects: Map<ReviewAspect, Double>,
    val pros: List<String>,
    val cons: List<String>,
)

private data class Point(val weight: Double, val line: String, val clauses: List<String>) {
    constructor(weight: Double, line: String, clause: String) : this(weight, line, listOf(clause))
}

internal fun writeReviews(i: ReviewInputs, rng: SeededRNG): ReviewResult {
    val c = i.concept

    // How each part of the game came out, from the effort it got against what the genre needs.
    val aspects = ReviewAspect.entries.associateWith { aspect ->
        val v = when (aspect) {
            ReviewAspect.POLISH -> i.score + (i.bugPenalty - 0.9) * 12 + i.polish * 5
            else -> {
                val effort = aspect.areas.map { a ->
                    val stage = i.focus[a.stage]
                    val share = stage[FocusArea.forStage(a.stage).indexOf(a)] / max(0.01, stage.sum())
                    (share / max(0.05, c.idealShare(a))).coerceIn(0.0, 2.0)
                }.average()
                var d = (effort - 1) * 1.6
                if (aspect == ReviewAspect.VISUALS) d += ((i.engineLevel - i.engineStandard) * 0.08).coerceIn(-1.5, 1.0)
                i.score + d + rng.normal(sd = 0.3)
            }
        }
        (v * 2).roundToInt() / 2.0
    }.mapValues { it.value.coerceIn(1.0, 10.0) }

    // Pros and cons, strongest first.
    val pros = mutableListOf<Point>()
    val cons = mutableListOf<Point>()
    for ((aspect, v) in aspects) {
        // A strength has to be good in its own right, not just less bad than the rest.
        if (v >= 6.5 && (v >= i.score + 0.75 || (aspect == ReviewAspect.POLISH && i.bugPenalty > 0.985))) {
            pros += Point(v - i.score + 1, aspect.strength.first, aspect.strength.second)
        }
        if (v <= 7.5 && (v <= i.score - 0.75 || (aspect == ReviewAspect.POLISH && i.bugPenalty < 0.9))) {
            cons += Point(i.score - v + 1, aspect.weakness.first, aspect.weakness.second)
        }
    }
    val topic = c.topic.title
    when (c.topic.fit(c.genre)) {
        Fit.GREAT -> pros += Point(
            1.8, "$topic and ${c.genre.lower} are a perfect match",
            listOf("${topic.lowercase()} and ${c.genre.lower} are a perfect match", "the ${topic.lowercase()} setting and the ${c.genre.lower} design fit like a glove"),
        )
        Fit.BAD -> cons += Point(
            2.2, "$topic and ${c.genre.lower} don't mix",
            listOf(
                "${topic.lowercase()} and ${c.genre.lower} just don't mix", "nobody asked for ${Format.a(topic.lowercase())} ${c.genre.lower} game",
                "the ${topic.lowercase()} theme fights the ${c.genre.lower} design at every turn",
            ),
        )
        else -> {}
    }
    if (i.focusMatch > 0.85) pros += Point(1.4, "Every part pulls its weight", listOf("every part of it pulls its weight", "it knows exactly what it wants to be"))
    if (i.onTrend) pros += Point(0.8, "Right on trend", listOf("it's right on trend", "it arrives at exactly the right time"))
    if (i.worthySequel) pros += Point(1.2, "A worthy sequel", listOf("it's a worthy sequel", "it builds on the original in every way"))
    if (i.record && i.score >= 7.5) pros += Point(1.5, "The studio's best work yet", listOf("it's the studio's best work yet", "the studio has outdone itself"))
    if (i.engineLevel >= i.engineStandard + 3) pros += Point(0.9, "Cutting-edge tech", listOf("the tech is cutting-edge", "it pushes the hardware hard"))
    if (i.repeated) cons += Point(1.6, "Feels like a retread", listOf("it feels like a retread of their last game", "we've played this before"))
    if (i.rushedSequel) cons += Point(1.6, "A rushed sequel", listOf("it feels rushed out the door", "it needed more time"))
    if (i.stepBack) cons += Point(1.8, "A step back", listOf("it's a step back from their last game", "it falls short of what came before"))
    if (i.wrongCrowd) cons += Point(1.2, "Made for the wrong crowd", listOf("it's aimed at the wrong crowd", "it's not clear who it's for"))
    i.awkwardPlatform?.let { cons += Point(1.0, "Awkward on ${it.title}", listOf("it feels awkward on ${it.title}", "it's a poor fit for ${it.title}")) }
    if (i.price == PriceTier.PREMIUM && i.score < 7) {
        cons += Point(1.7, "Overpriced", listOf("it's hard to justify the premium price", "at this price, it needed to be much better"))
    }
    if (i.price == PriceTier.BUDGET && i.score >= 6) {
        pros += Point(1.3, "Great value", listOf("at this price it's a steal", "it's terrific value for money"))
    }
    if (i.focusMatch < 0.55) {
        val weakest = FocusArea.entries.maxBy { a ->
            val stage = i.focus[a.stage]
            c.idealShare(a) - stage[FocusArea.forStage(a.stage).indexOf(a)] / max(0.01, stage.sum())
        }
        cons += Point(
            1.5, "${weakest.title} feels like an afterthought",
            listOf("${weakest.title.lowercase()} feels like an afterthought", "${weakest.title.lowercase()} was clearly left until last"),
        )
    }
    val sortedPros = pros.distinctBy { it.line }.sortedByDescending { it.weight }
    val sortedCons = cons.distinctBy { it.line }.sortedByDescending { it.weight }

    // Each outlet's verdict.
    val outlets = Outlet.entries.filter { it != Outlet.PC_GAMER || Platform.PC in c.platforms }
    val reviews = outlets.mapIndexed { n, outlet ->
        val exact = (i.score + bias(outlet, i, aspects) + rng.normal(sd = 0.4)).coerceIn(1.0, 10.0)
        Review(outlet, exact.roundToInt().coerceIn(1, 10), comment(outlet, n, exact, sortedPros, sortedCons, rng), exact)
    }
    val average = reviews.map { it.exact }.average()

    // Players: they care about fun and bugs, and they remember being overpromised.
    var user = average + (aspects.getValue(ReviewAspect.GAMEPLAY) - average) * 0.4 +
        (aspects.getValue(ReviewAspect.POLISH) - average) * 0.3 + (i.fanTrust - 1) * 1.5
    if (i.hype > 60 && average < 7) user -= 1.0
    if (i.bugPenalty < 0.9) user -= 0.8
    if (i.worthySequel) user += 0.3
    if (i.price == PriceTier.PREMIUM && average < 7.5) user -= 0.6
    if (i.price == PriceTier.BUDGET) user += 0.4
    user += rng.normal(sd = 0.35)
    val userScore = (user.coerceIn(1.0, 10.0) * 10).roundToInt() / 10.0

    return ReviewResult(
        reviews, average, userScore, aspects,
        sortedPros.take(3).map { it.line }, sortedCons.take(3).map { it.line },
    )
}

/** Each outlet's taste. */
private fun bias(outlet: Outlet, i: ReviewInputs, aspects: Map<ReviewAspect, Double>): Double {
    val c = i.concept
    return when (outlet) {
        // Generous, and loves a big, hyped release.
        Outlet.IGN -> 0.25 + (if (c.size >= GameSize.LARGE) 0.2 else 0.0) + i.hype / 400
        // Plays it straight, but bugs cost you.
        Outlet.GAMESPOT -> (aspects.getValue(ReviewAspect.POLISH) - i.score).coerceAtMost(0.0) * 0.4
        // Big on action, RPGs and adventures.
        Outlet.GAME_INFORMER -> if (c.genre in listOf(Genre.ACTION, Genre.RPG, Genre.ADVENTURE)) 0.3 else -0.1
        // Champions small, creative games; tired of the same old thing.
        Outlet.EUROGAMER -> (if (c.size <= GameSize.MEDIUM) 0.25 else -0.1) + (if (i.repeated || i.rushedSequel) -0.4 else 0.0) +
            (if (i.designRatio > 0.55) 0.15 else 0.0)
        // PC players want tech, strategy and simulation.
        Outlet.PC_GAMER -> (if (c.genre in listOf(Genre.STRATEGY, Genre.SIMULATION)) 0.35 else 0.0) +
            (aspects.getValue(ReviewAspect.VISUALS) - i.score) * 0.2
        // Famously hard to please, but rewards a coherent vision.
        Outlet.EDGE -> -0.7 + (if (i.focusMatch > 0.8) 0.3 else 0.0)
    }
}

private fun comment(outlet: Outlet, n: Int, score: Double, pros: List<Point>, cons: List<Point>, rng: SeededRNG): String {
    // Outlets fan out over the top points so they don't all say the same thing.
    fun pick(points: List<Point>): String? {
        val top = points.take(3)
        if (top.isEmpty()) return null
        return rng.pick(top[(n + rng.int(0, 1)) % top.size].clauses)
    }
    val pro = pick(pros)
    val con = pick(cons)
    val body = when {
        score >= 8 -> pro ?: rng.pick(listOf("it's a joy from start to finish", "it's easy to lose whole evenings to it", "it's the real deal"))
        score >= 6.5 -> when {
            pro != null && con != null -> "$pro, but $con"
            pro != null -> pro
            con != null -> "it's fun, even if $con"
            else -> rng.pick(listOf("it does what it sets out to do", "it's a solid, safe bet"))
        }
        score >= 5 -> when {
            pro != null && con != null -> "$pro, but $con"
            pro != null -> "$pro, " + rng.pick(
                listOf("but little else stands out", "but the rest is merely fine", "though that's about it", "but it never quite comes together"),
            )
            con != null -> con
            else -> rng.pick(listOf("it's competent and instantly forgettable", "it never rises above average"))
        }
        else -> when {
            con != null && rng.chance(0.7) -> con
            else -> rng.pick(listOf("it never comes together", "there's little here to recommend", "it's hard to see who this is for"))
        }
    }
    val lead = when (outlet) {
        Outlet.EDGE -> return body.replaceFirstChar { it.uppercase() } + "."
        Outlet.EUROGAMER -> when (Outlet.stars(score)) {
            5 -> "Essential."
            4 -> "Recommended."
            1, 2 -> "Avoid."
            else -> ""
        }
        else -> when {
            score >= 9.5 -> "A masterpiece:"
            score >= 8.5 -> "Brilliant:"
            score >= 7.5 -> "Great fun:"
            score >= 6.5 -> "Good, not great:"
            score >= 5 -> "A mixed bag:"
            score >= 3.5 -> "Disappointing:"
            else -> "Avoid:"
        }
    }
    val sentence = body.replaceFirstChar { it.uppercase() } + "."
    return if (lead.isEmpty()) sentence else if (lead.endsWith(":")) "$lead $body." else "$lead $sentence"
}
