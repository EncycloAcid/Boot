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
    EDGE("Edge", ScoreStyle.OUT_OF_TEN),

    // The magazines of the past, for runs set there.
    CREATIVE_COMPUTING("Creative Computing", ScoreStyle.OUT_OF_TEN),
    BYTE("Byte", ScoreStyle.STARS),
    ELECTRONIC_GAMES("Electronic Games", ScoreStyle.OUT_OF_TEN),
    COMPUTER_GAMING_WORLD("Computer Gaming World", ScoreStyle.STARS),
    ZZAP_64("Zzap!64", ScoreStyle.PERCENT),
    NINTENDO_POWER("Nintendo Power", ScoreStyle.OUT_OF_TEN),
    EGM("Electronic Gaming Monthly", ScoreStyle.QUARTERS);

    /** The years it was reviewing games. */
    val years: IntRange
        get() = when (this) {
            IGN, GAMESPOT -> 1996..9999
            GAME_INFORMER -> 1991..9999
            EUROGAMER -> 1999..9999
            PC_GAMER, EDGE -> 1993..9999
            CREATIVE_COMPUTING -> 1974..1985
            BYTE -> 1975..1998
            ELECTRONIC_GAMES -> 1981..1985
            COMPUTER_GAMING_WORLD -> 1981..2006
            ZZAP_64 -> 1985..1992
            NINTENDO_POWER -> 1988..2012
            EGM -> 1989..2014
        }

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
    /** The few words on the cover: the pull quote. */
    val headline: String = "",
    /** The outlet's own seal for its best-reviewed games, if this earned it. */
    val badge: String? = null,
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
                listOf("the moment-to-moment gameplay is superb", "it just feels great to play", "the core loop is hard to put down", "every system clicks into place", "it's the kind of game you think about at work", "the controls are as tight as they come")
            STORY -> "A story and world worth getting lost in" to
                listOf("its story and world are worth getting lost in", "the writing is genuinely moving", "the world-building is superb", "the characters stay with you long after the credits", "the dialogue is sharp and often very funny", "it has one of the best endings in years")
            VISUALS -> "Gorgeous to look at" to
                listOf("it looks gorgeous", "every frame is a screenshot", "the art direction is stunning", "the lighting alone is worth the price", "it has a look all its own", "it's a showpiece for the hardware")
            SOUND -> "A fantastic soundtrack" to
                listOf("the soundtrack is fantastic", "the sound design is superb", "the score will stay with you", "you'll want to play it with headphones", "the music swells at exactly the right moments")
            POLISH -> "Rock solid at launch" to
                listOf("it runs like a dream", "it's remarkably polished", "we didn't hit a single bug", "it's polished to a mirror shine", "loading times are a thing of the past")
        }

    internal val weakness: Pair<String, List<String>>
        get() = when (this) {
            GAMEPLAY -> "Gameplay gets repetitive" to
                listOf("the gameplay gets repetitive fast", "it's a chore to actually play", "the controls fight you", "the novelty wears off within the hour", "there's not enough to do", "the difficulty is all over the place")
            STORY -> "A thin story and world" to
                listOf("the story and world feel thin", "the writing falls flat", "there's no reason to care about anyone in it", "the plot is held together with string", "the ending comes out of nowhere", "the world feels empty")
            VISUALS -> "Dated visuals" to
                listOf("it looks dated", "the visuals are muddy", "it's ugly in places", "it looks a generation behind", "the animation is stiff", "the art has no identity")
            SOUND -> "Forgettable audio" to
                listOf("the audio is forgettable", "the same few songs loop forever", "the sound design is flat", "you'll play it on mute", "the voice acting is wooden")
            POLISH -> "Buggy at launch" to
                listOf("bugs and crashes keep getting in the way", "it crashed on us more than once", "it needed another month in the oven", "the frame rate tanks when it matters", "we lost two hours to a save bug", "it feels like a beta")
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
    val year: Int = MODERN_YEAR,
)

internal data class ReviewResult(
    val reviews: List<Review>,
    val average: Double,
    val userScore: Double,
    val aspects: Map<ReviewAspect, Double>,
    val pros: List<String>,
    val cons: List<String>,
    val players: List<PlayerQuote> = emptyList(),
)

/** A player's review: thumbs up or down, how long they played, and what they said. */
@Serializable
data class PlayerQuote(val recommended: Boolean, val hours: Int, val text: String)

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
            listOf(
                "${topic.lowercase()} and ${c.genre.lower} are a perfect match",
                "the ${topic.lowercase()} setting and the ${c.genre.lower} design fit like a glove",
                genreTopicPraise(c.genre, c.topic),
            ),
        )
        Fit.BAD -> cons += Point(
            2.2, "$topic and ${c.genre.lower} don't mix",
            listOf(
                "${topic.lowercase()} and ${c.genre.lower} just don't mix",
                "nobody asked for ${Format.a(topic.lowercase())} ${c.genre.lower} game",
                "the ${topic.lowercase()} theme fights the ${c.genre.lower} design at every turn",
                genreTopicPan(c.genre, c.topic),
            ),
        )
        else -> {}
    }
    topicClause(c.topic)?.let { (pro, con) ->
        when (c.topic.fit(c.genre)) {
            Fit.GREAT, Fit.GOOD -> pros += Point(0.7, pro, pro.lowercase())
            Fit.BAD -> cons += Point(0.9, con, con.lowercase())
            else -> {}
        }
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
    val outlets = Outlet.entries.filter { i.year in it.years && (it != Outlet.PC_GAMER || Platform.PC in c.platforms) }
        .ifEmpty { listOf(Outlet.CREATIVE_COMPUTING) }
    // Words alone (pull quotes, players' lines) come from their own dice, so they never change how a game does.
    val words = SeededRNG.fromSeed(i.title.hashCode().toLong() * 31 + (i.score * 1_000).toLong() + i.year)
    val reviews = outlets.mapIndexed { n, outlet ->
        val exact = (i.score + bias(outlet, i, aspects) + rng.normal(sd = 0.4)).coerceIn(1.0, 10.0)
        Review(
            outlet, exact.roundToInt().coerceIn(1, 10), comment(outlet, n, exact, sortedPros, sortedCons, rng, i.title, words), exact,
            headline = pullQuote(outlet, exact, words), badge = badge(outlet, exact),
        )
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
        playerQuotes(i, userScore, aspects, words),
    )
}

/** The few words an outlet puts on the cover, in its own voice. */
private fun pullQuote(outlet: Outlet, score: Double, rng: SeededRNG): String {
    // Edge keeps it to a word.
    if (outlet == Outlet.EDGE) return when {
        score >= 9 -> "Exceptional"
        score >= 8 -> "Accomplished"
        score >= 6.5 -> "Competent"
        score >= 5 -> "Flawed"
        else -> "Misjudged"
    }
    // Zzap!64 shouted.
    if (outlet == Outlet.ZZAP_64) return when {
        score >= 9 -> "Absolutely brilliant!"
        score >= 7.5 -> "Great stuff!"
        score >= 5 -> "Worth a look"
        else -> "Rubbish!"
    }
    return rng.pick(
        when {
            score >= 9.5 -> listOf("A masterpiece", "An instant classic", "Unmissable", "One for the ages", "A landmark", "Perfection, nearly")
            score >= 8.5 -> listOf("Brilliant", "A triumph", "Superb", "Essential playing", "A stunner", "Top of its class")
            score >= 7.5 -> listOf("Great fun", "Easy to recommend", "Well worth your time", "A confident step", "A real treat", "Hard to put down")
            score >= 6.5 -> listOf("Good, not great", "Solid", "Worth a look", "Flawed but fun", "Promising", "A decent time")
            score >= 5 -> listOf("A mixed bag", "Uneven", "So close", "Middling", "Forgettable", "Rough around the edges")
            score >= 3.5 -> listOf("Disappointing", "A letdown", "Hard to love", "Missed potential", "Not there yet")
            else -> listOf("Avoid", "A mess", "Not worth your time", "Broken", "A misfire")
        },
    )
}

/** The outlet's own seal for the games it rates highest. */
private fun badge(outlet: Outlet, score: Double): String? = when (outlet) {
    Outlet.IGN, Outlet.GAMESPOT, Outlet.PC_GAMER -> if (score >= 9) "Editor's Choice" else null
    Outlet.EUROGAMER -> when {
        score >= 9 -> "Essential"
        score >= 8 -> "Recommended"
        else -> null
    }
    Outlet.ZZAP_64 -> when {
        score >= 9.5 -> "Gold Medal"
        score >= 9 -> "Sizzler"
        else -> null
    }
    Outlet.GAME_INFORMER -> if (score >= 9) "Editors' Choice" else null
    Outlet.EDGE -> if (score >= 9.5) "Edge 10" else null
    Outlet.EGM -> if (score >= 9) "Gold Award" else null
    else -> if (score >= 9.5) "Game of the Month" else null
}

/** Three players' reviews, from the people who loved it to the people who didn't. */
private fun playerQuotes(i: ReviewInputs, user: Double, aspects: Map<ReviewAspect, Double>, rng: SeededRNG): List<PlayerQuote> {
    val title = i.title
    val buggy = i.bugPenalty < 0.92
    val ups = mutableListOf<String>()
    val downs = mutableListOf<String>()
    if (aspects.getValue(ReviewAspect.GAMEPLAY) >= 7.5) ups += listOf("Lost my whole weekend to this.", "\"One more go\" at 3am. Again.", "The controls just feel right.")
    if (aspects.getValue(ReviewAspect.STORY) >= 7.5) ups += listOf("Didn't expect to cry at a ${i.concept.genre.lower} game.", "I need to talk to someone about that ending.")
    if (aspects.getValue(ReviewAspect.VISUALS) >= 7.5) ups += "My screenshot folder is 4GB now."
    if (aspects.getValue(ReviewAspect.SOUND) >= 7.5) ups += "Soundtrack is on repeat while I work."
    if (i.price == PriceTier.BUDGET) ups += "At this price? Just buy it."
    if (i.worthySequel) ups += "Better than the first one in every way."
    ups += listOf("Exactly what I wanted from $title.", "Solid. Would play again.", "Fun with friends.")
    if (buggy) downs += listOf("Crashed three times in the first hour.", "Wait for the patch.", "Fell through the floor. Twice.")
    if (aspects.getValue(ReviewAspect.GAMEPLAY) < 6) downs += listOf("Got bored after an hour.", "It's a chore to play.")
    if (aspects.getValue(ReviewAspect.STORY) < 5.5) downs += "Skipped every cutscene. Nothing lost."
    if (i.hype > 60 && user < 7) downs += "The trailers lied."
    if (i.price == PriceTier.PREMIUM && user < 7.5) downs += "Not worth full price. Wait for a sale."
    if (i.repeated) downs += "Haven't I played this already?"
    downs += listOf("Not for me.", "Refunded.", "Wanted to like it more.")
    // How many of the three like it follows the players' score.
    val positive = when {
        user >= 8.5 -> 3
        user >= 7 -> 2
        user >= 5 -> 1
        else -> 0
    }
    val good = ups.distinct().shuffledWith(rng)
    val bad = downs.distinct().shuffledWith(rng)
    return (0 until 3).map { n ->
        val up = n < positive
        PlayerQuote(up, if (up) rng.int(12, 140) else rng.int(1, 9), if (up) good[n % good.size] else bad[(n - positive) % bad.size])
    }
}

private fun <T> List<T>.shuffledWith(rng: SeededRNG): List<T> {
    val out = toMutableList()
    for (k in out.indices.reversed()) {
        val j = rng.int(0, k)
        val t = out[k]
        out[k] = out[j]
        out[j] = t
    }
    return out
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
        // Hobbyists: they love a clever program and forgive rough edges.
        Outlet.CREATIVE_COMPUTING, Outlet.BYTE -> (if (i.designRatio > 0.55) 0.3 else 0.0) - (aspects.getValue(ReviewAspect.VISUALS) - i.score).coerceAtMost(0.0) * 0.2
        // The first games magazine: cheerleaders for the hobby.
        Outlet.ELECTRONIC_GAMES -> 0.3
        // Serious about strategy and simulation, like the PC crowd after them.
        Outlet.COMPUTER_GAMING_WORLD -> (if (c.genre in listOf(Genre.STRATEGY, Genre.SIMULATION, Genre.RPG)) 0.35 else -0.1)
        // Percentages that swing wildly: great games get gushed over, bad ones get savaged.
        Outlet.ZZAP_64 -> (i.score - 6.5) * 0.15
        // Generous to anything on a Nintendo.
        Outlet.NINTENDO_POWER -> if (c.platforms.any { it.maker == "Nintendo" }) 0.5 else 0.0
        // Four reviewers who rarely agree; action fans at heart.
        Outlet.EGM -> if (c.genre == Genre.ACTION) 0.25 else 0.0
    }
}

private fun genreTopicPraise(genre: Genre, topic: Topic): String = when (genre) {
    Genre.ACTION -> "the ${topic.title.lowercase()} set-pieces hit hard"
    Genre.ADVENTURE -> "exploring a ${topic.title.lowercase()} world actually feels wondrous"
    Genre.RPG -> "the ${topic.title.lowercase()} lore gives the systems soul"
    Genre.SIMULATION -> "running a ${topic.title.lowercase()} sim is oddly compulsive"
    Genre.STRATEGY -> "${topic.title.lowercase()} makes for meaty strategic puzzles"
    Genre.CASUAL -> "the ${topic.title.lowercase()} hook is instant and friendly"
}

private fun genreTopicPan(genre: Genre, topic: Topic): String = when (genre) {
    Genre.ACTION -> "the ${topic.title.lowercase()} never earns the action"
    Genre.ADVENTURE -> "wandering a thin ${topic.title.lowercase()} setting wears thin"
    Genre.RPG -> "the ${topic.title.lowercase()} story can't carry the grind"
    Genre.SIMULATION -> "the ${topic.title.lowercase()} sim never finds a loop"
    Genre.STRATEGY -> "${topic.title.lowercase()} and strategy keep stepping on each other"
    Genre.CASUAL -> "even as a casual game, the ${topic.title.lowercase()} theme feels forced"
}

/** Optional topic-flavoured colour for pros/cons lists. */
private fun topicClause(topic: Topic): Pair<String, String>? = when (topic) {
    Topic.HEIST -> "A heist that actually pays off" to "The heist never gets clever"
    Topic.STEAMPUNK -> "Brass-and-steam atmosphere to spare" to "Steampunk window dressing, little else"
    Topic.SURVIVAL -> "Every scrap of gear feels hard-won" to "Survival systems that punish without teaching"
    Topic.BALLET -> "Graceful motion that sells the fantasy" to "The ballet theme is mostly costume"
    Topic.FOLKLORE -> "Folk tales with teeth" to "Folklore reduced to wallpaper"
    Topic.ESPORTS_SCENE -> "Competitive structure that respects the scene" to "Esports flavour without the stakes"
    Topic.HORROR -> "Dread that sticks after you quit" to "Jumpscares standing in for atmosphere"
    Topic.CYBERPUNK -> "Neon grit with a point of view" to "Cyberpunk clichés on autopilot"
    else -> null
}

private fun outletEncore(outlet: Outlet, score: Double, rng: SeededRNG, words: SeededRNG = rng): String? {
    if (score < 8 && score > 4) return null
    val high = score >= 8
    return when (outlet) {
        Outlet.IGN -> if (high) rng.pick(listOf("Don't miss this one.", "Our review score says it all."))
        else rng.pick(listOf("Skip it.", "Hard recommend: pass."))
        Outlet.GAMESPOT -> if (high) rng.pick(listOf("A must-play.", "It earns every point."))
        else rng.pick(listOf("Not worth your time.", "The problems stack up."))
        Outlet.GAME_INFORMER -> if (high) words.pick(listOf("Fans of the genre should start here.", "A new benchmark for the genre.")) else words.pick(listOf("Even die-hards will struggle.", "Genre fans deserve better."))
        Outlet.EUROGAMER -> if (high) words.pick(listOf("It belongs on this year's shortlists.", "One to cherish.")) else words.pick(listOf("There's little reason to return.", "A missed opportunity."))
        Outlet.PC_GAMER -> if (high) words.pick(listOf("PC players are spoiled this week.", "Clear your weekend.")) else words.pick(listOf("Even on PC, it frustrates more than it rewards.", "Wait for the mods to fix it."))
        Outlet.EDGE -> if (high) words.pick(listOf("A rare ten-adjacent triumph.", "Exemplary.")) else words.pick(listOf("Below the standard we expect.", "A muddle."))
        Outlet.NINTENDO_POWER -> if (high) "A new favourite on Nintendo hardware." else "Not up to Nintendo Power standards."
        Outlet.EGM -> if (high) "Four out of four of us would play again." else "The panel was not kind."
        Outlet.ZZAP_64 -> if (high) "Get it now!" else "Save your pocket money."
        Outlet.COMPUTER_GAMING_WORLD -> if (high) "A must for any serious player." else "Serious players will want more."
        else -> if (high) rng.pick(listOf("A standout.", "Highly recommended."))
        else rng.pick(listOf("Give it a miss.", "Not recommended."))
    }
}

private fun comment(
    outlet: Outlet, n: Int, score: Double, pros: List<Point>, cons: List<Point>, rng: SeededRNG, title: String = "",
    words: SeededRNG = SeededRNG(0),
): String {
    // Outlets fan out over the top points so they don't all say the same thing.
    fun pick(points: List<Point>): String? {
        val top = points.take(3)
        if (top.isEmpty()) return null
        return rng.pick(top[(n + rng.int(0, 1)) % top.size].clauses)
    }
    fun second(points: List<Point>): String? = points.take(3).getOrNull((n + 1) % 3)?.let { words.pick(it.clauses) }
    val pro = pick(pros)
    val con = pick(cons)
    val pro2 = second(pros)?.takeIf { it != pro }
    val con2 = second(cons)?.takeIf { it != con }
    // Now and then a critic names the game, and backs a point up with a second.
    if (title.isNotEmpty() && words.chance(0.35)) {
        val named = when {
            score >= 8 && pro != null -> words.pick(
                listOfNotNull(
                    "$title nails it: $pro",
                    pro2?.let { "$pro, and $it. $title is the real thing" },
                    "We kept coming back to $title, because $pro",
                ),
            )
            score >= 6.5 && pro != null && con != null -> "$title gets a lot right ($pro), but $con"
            score < 5 && con != null -> words.pick(listOfNotNull("$title stumbles early: $con", con2?.let { "$con, and $it" }))
            else -> null
        }
        if (named != null) {
            val encore = outletEncore(outlet, score, words)
            val core = named.replaceFirstChar { it.uppercase() } + "."
            return if (encore != null) "$core $encore" else core
        }
    }
    val body = when {
        score >= 8 -> pro ?: rng.pick(listOf("it's a joy from start to finish", "it's easy to lose whole evenings to it", "it's the real deal"))
        score >= 6.5 -> when {
            pro != null && con != null -> when (rng.int(0, 2)) {
                0 -> "$pro, but $con"
                1 -> "You'll love that $pro — just know $con"
                else -> "Strong where it counts ($pro), weaker elsewhere ($con)"
            }
            pro != null -> pro
            con != null -> "it's fun, even if $con"
            else -> rng.pick(listOf("it does what it sets out to do", "it's a solid, safe bet"))
        }
        score >= 5 -> when {
            pro != null && con != null -> when (rng.int(0, 1)) {
                0 -> "$pro, but $con"
                else -> "For every $pro, there's $con"
            }
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
    // Drawn and unused: it once picked a sentence shape, and taking the draw keeps every game's dice where they were.
    rng.int(0, 2)
    val core = body.replaceFirstChar { it.uppercase() } + "."
    val encore = outletEncore(outlet, score, rng, words)
    // The pull quote and the outlet's seal give the verdict, so the comment gets straight to the point.
    if (outlet == Outlet.EDGE) return if (encore != null && (score >= 8 || score <= 4)) "$core $encore" else core
    return if (encore != null) "$core $encore" else core
}
