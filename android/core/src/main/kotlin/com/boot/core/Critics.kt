package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/*
 * Critic personalities: each review outlet has favoured genres, publisher pets/grudges,
 * and a lasting relationship with the player (grudge or pet) that nudges future scores
 * and sparks news / feed beats. Profiles reuse Outlet / CompanyMarks titles for logos.
 */

/** Static taste for a named press outlet. Era presence stays on [Outlet.years]. */
data class CriticProfile(
    val outlet: Outlet,
    /** Genres they cheer for. */
    val genres: List<Genre>,
    /** Genres they shrug at or pan. */
    val disliked: List<Genre> = emptyList(),
    /** Publisher / studio name fragments they soft-pedal. */
    val publisherPets: List<String> = emptyList(),
    /** Publishers they distrust. */
    val publisherGrudges: List<String> = emptyList(),
    /** Positive = soft scores, negative = harsh (on top of the classic outlet bias). */
    val generosity: Double = 0.0,
    val blurb: String,
) {
    val harsh: Boolean get() = generosity <= -0.35
    val soft: Boolean get() = generosity >= 0.2

    fun likes(genre: Genre): Boolean = genre in genres
    fun hates(genre: Genre): Boolean = genre in disliked

    /** Extra score from publisher loyalty, beyond the classic [bias] taste. */
    fun publisherBias(company: String, publisher: String?): Double {
        val labels = listOfNotNull(publisher?.takeIf { it.isNotBlank() }, company.takeIf { it.isNotBlank() })
        if (labels.isEmpty()) return 0.0
        var v = 0.0
        for (label in labels) {
            if (publisherPets.any { label.contains(it, ignoreCase = true) }) v += 0.22
            if (publisherGrudges.any { label.contains(it, ignoreCase = true) }) v -= 0.28
        }
        return v.coerceIn(-0.35, 0.3)
    }

    /** Mild genre lean that stacks on the classic outlet bias without rewriting it. */
    fun genreLean(genre: Genre): Double = when {
        likes(genre) -> 0.12
        hates(genre) -> -0.18
        else -> 0.0
    }
}

/** Lasting relationship with one outlet. Defaults keep old saves loading. */
@Serializable
data class CriticStanding(
    val outlet: Outlet,
    /** −1…1 how they feel about you. Negative → grudge, positive → pet. */
    var favor: Double = 0.0,
    /** Recent hits in their wheelhouse. */
    var hitsInLane: Int = 0,
    /** Recent snubs, hated genres, or public spats. */
    var snubs: Int = 0,
    /** "", "pet", or "grudge". */
    var status: String = "",
    /** Week the current status formed. */
    var statusWeek: Int = 0,
) {
    val isPet: Boolean get() = status == "pet"
    val isGrudge: Boolean get() = status == "grudge"

    fun label(): String = when {
        isPet -> "Pet"
        isGrudge -> "Grudge"
        favor >= 0.2 -> "Warm"
        favor <= -0.2 -> "Cool"
        else -> "Neutral"
    }
}

/** Profiles for every scoring outlet. Names match [CompanyMarks] / logos where they exist. */
val CriticProfiles: Map<Outlet, CriticProfile> = listOf(
    CriticProfile(
        Outlet.IGN, listOf(Genre.ACTION, Genre.ADVENTURE, Genre.SHOOTER, Genre.RPG),
        disliked = listOf(Genre.SIMULATION, Genre.STRATEGY),
        publisherPets = listOf("Nintendo", "Sony", "PlayStation", "Microsoft"),
        publisherGrudges = listOf("Acclaim"),
        generosity = 0.25,
        blurb = "Big-platform cheerleaders who love spectacle and a hyped launch.",
    ),
    CriticProfile(
        Outlet.GAMESPOT, listOf(Genre.ACTION, Genre.RPG, Genre.SHOOTER),
        disliked = listOf(Genre.CASUAL),
        publisherPets = listOf("Nintendo", "Capcom"),
        publisherGrudges = listOf("Electronic Arts", "EA"),
        generosity = 0.0,
        blurb = "Straight shooters — polish matters, and they remember buggy launches.",
    ),
    CriticProfile(
        Outlet.GAME_INFORMER, listOf(Genre.ACTION, Genre.RPG, Genre.ADVENTURE),
        disliked = listOf(Genre.CASUAL, Genre.PUZZLE),
        publisherPets = listOf("Bethesda", "Square Enix", "BioWare"),
        publisherGrudges = emptyList(),
        generosity = 0.1,
        blurb = "Cover-story magazine energy: action, RPGs, and big adventures.",
    ),
    CriticProfile(
        Outlet.EUROGAMER, listOf(Genre.ADVENTURE, Genre.PUZZLE, Genre.PLATFORMER, Genre.RPG),
        disliked = listOf(Genre.CASUAL),
        publisherPets = listOf("Nintendo", "Annapurna", "Devolver"),
        publisherGrudges = listOf("Electronic Arts", "Activision", "Ubisoft"),
        generosity = 0.05,
        blurb = "Champions of inventive mid-size games; tired of annualised sludge.",
    ),
    CriticProfile(
        Outlet.PC_GAMER, listOf(Genre.STRATEGY, Genre.SIMULATION, Genre.RPG, Genre.SHOOTER),
        disliked = listOf(Genre.CASUAL, Genre.PLATFORMER),
        publisherPets = listOf("Valve", "Paradox", "CD Projekt", "Firaxis"),
        publisherGrudges = listOf("Ubisoft"),
        generosity = 0.05,
        blurb = "PC faithful: strategy, sims, and tech that pushes the hardware.",
    ),
    CriticProfile(
        Outlet.EDGE, listOf(Genre.ADVENTURE, Genre.PLATFORMER, Genre.PUZZLE, Genre.RPG),
        disliked = listOf(Genre.CASUAL, Genre.SIMULATION),
        publisherPets = listOf("Nintendo", "FromSoftware", "Sony"),
        publisherGrudges = listOf("Electronic Arts", "Activision", "Acclaim"),
        generosity = -0.55,
        blurb = "Famously hard to please — rewards a coherent vision, not marketing.",
    ),
    CriticProfile(
        Outlet.CREATIVE_COMPUTING, listOf(Genre.SIMULATION, Genre.STRATEGY, Genre.PUZZLE),
        disliked = listOf(Genre.ACTION),
        publisherPets = listOf("Apple", "Atari"),
        generosity = 0.2,
        blurb = "Hobbyist magazine: clever programs over flashy graphics.",
    ),
    CriticProfile(
        Outlet.BYTE, listOf(Genre.SIMULATION, Genre.STRATEGY),
        disliked = listOf(Genre.CASUAL),
        publisherPets = listOf("IBM", "Apple"),
        generosity = 0.15,
        blurb = "Serious computing press that forgives rough edges on a clever idea.",
    ),
    CriticProfile(
        Outlet.ELECTRONIC_GAMES, listOf(Genre.ACTION, Genre.ADVENTURE, Genre.PLATFORMER),
        disliked = emptyList(),
        publisherPets = listOf("Atari", "Activision"),
        generosity = 0.3,
        blurb = "The first games magazine — cheerleaders for the new hobby.",
    ),
    CriticProfile(
        Outlet.COMPUTER_GAMING_WORLD, listOf(Genre.STRATEGY, Genre.SIMULATION, Genre.RPG),
        disliked = listOf(Genre.CASUAL, Genre.PLATFORMER),
        publisherPets = listOf("Sierra", "Origin", "MicroProse"),
        publisherGrudges = listOf("Acclaim"),
        generosity = 0.0,
        blurb = "Serious PC coverage before the web — strategy and sims first.",
    ),
    CriticProfile(
        Outlet.ZZAP_64, listOf(Genre.ACTION, Genre.PLATFORMER, Genre.ADVENTURE),
        disliked = listOf(Genre.SIMULATION),
        publisherPets = listOf("Ocean", "Ultimate", "Rare"),
        generosity = 0.1,
        blurb = "Loud Commodore 64 rag — wild percentages and big feelings.",
    ),
    CriticProfile(
        Outlet.NINTENDO_POWER, listOf(Genre.PLATFORMER, Genre.ACTION, Genre.ADVENTURE, Genre.RPG),
        disliked = listOf(Genre.SHOOTER),
        publisherPets = listOf("Nintendo", "Rare", "HAL"),
        publisherGrudges = listOf("Sega", "Sony"),
        generosity = 0.35,
        blurb = "House organ energy: generous to anything on Nintendo hardware.",
    ),
    CriticProfile(
        Outlet.EGM, listOf(Genre.ACTION, Genre.FIGHTING, Genre.SHOOTER, Genre.PLATFORMER),
        disliked = listOf(Genre.SIMULATION, Genre.STRATEGY),
        publisherPets = listOf("Capcom", "Sega", "Namco"),
        publisherGrudges = emptyList(),
        generosity = 0.1,
        blurb = "Four reviewers who rarely agree — action fans at heart.",
    ),
).associateBy { it.outlet }

fun criticProfile(outlet: Outlet): CriticProfile =
    CriticProfiles[outlet] ?: CriticProfile(outlet, emptyList(), blurb = "${outlet.title} covers games.")

/** Outlets reviewing in [year], with personality attached. */
fun criticsIn(year: Int): List<CriticProfile> =
    Outlet.entries.filter { year in it.years }.map { criticProfile(it) }

fun GamesStudio.standingOf(outlet: Outlet): CriticStanding {
    criticStanding.firstOrNull { it.outlet == outlet }?.let { return it }
    val s = CriticStanding(outlet)
    criticStanding += s
    return s
}

fun GamesStudio.favorMap(): Map<Outlet, Double> =
    criticStanding.associate { it.outlet to it.favor }

/** Score nudge from lasting favor (−0.55…0.55). */
fun standingBias(favor: Double): Double = (favor * 0.55).coerceIn(-0.55, 0.55)

/**
 * After a player (or label) release: update grudges/pets from genre fit, score spats,
 * and publisher taste. Emits news + feed when a status forms or a biased review is notable.
 */
internal fun GameState.applyCriticRelations(release: GameRelease) {
    val g = games ?: return
    if (release.reviews.isEmpty()) return
    val avg = release.score
    val genre = release.concept.genre
    val publisher = release.publisher
    var formedPet: CriticStanding? = null
    var formedGrudge: CriticStanding? = null
    var notable: Pair<Review, String>? = null

    for (review in release.reviews) {
        val profile = criticProfile(review.outlet)
        val st = g.standingOf(review.outlet)
        val before = st.status

        // Genre fit streak.
        when {
            profile.likes(genre) && review.exact >= 7.5 -> {
                st.hitsInLane = (st.hitsInLane + 1).coerceAtMost(8)
                st.snubs = max(0, st.snubs - 1)
                st.favor = (st.favor + 0.12).coerceIn(-1.0, 1.0)
            }
            profile.hates(genre) -> {
                st.snubs = (st.snubs + 1).coerceAtMost(8)
                st.hitsInLane = max(0, st.hitsInLane - 1)
                st.favor = (st.favor - 0.14).coerceIn(-1.0, 1.0)
            }
            profile.likes(genre) -> {
                st.favor = (st.favor + 0.04).coerceIn(-1.0, 1.0)
            }
            else -> {
                // Snub their taste by shipping outside the wheelhouse while scoring fine overall.
                if (avg >= 7.0 && review.exact < avg - 0.4) {
                    st.snubs = (st.snubs + 1).coerceAtMost(8)
                    st.favor = (st.favor - 0.06).coerceIn(-1.0, 1.0)
                }
            }
        }

        // Publisher loyalty.
        st.favor = (st.favor + profile.publisherBias(companyName, publisher) * 0.35).coerceIn(-1.0, 1.0)

        // Public spat: this desk is far colder than the pack.
        val gap = review.exact - avg
        if (gap <= -1.4 && review.exact < 6.5) {
            st.snubs = (st.snubs + 1).coerceAtMost(8)
            st.favor = (st.favor - 0.18).coerceIn(-1.0, 1.0)
            if (notable == null) notable = review to "spat"
        } else if (gap >= 1.3 && review.exact >= 8.0 && profile.likes(genre)) {
            st.hitsInLane = (st.hitsInLane + 1).coerceAtMost(8)
            st.favor = (st.favor + 0.1).coerceIn(-1.0, 1.0)
            if (notable == null) notable = review to "rave"
        }

        // Form / clear lasting status.
        when {
            !st.isPet && st.hitsInLane >= 3 && st.favor >= 0.35 -> {
                st.status = "pet"
                st.statusWeek = week
                formedPet = st
            }
            !st.isGrudge && st.snubs >= 3 && st.favor <= -0.35 -> {
                st.status = "grudge"
                st.statusWeek = week
                formedGrudge = st
            }
            st.isPet && st.favor < 0.15 -> {
                st.status = ""
                st.hitsInLane = 0
            }
            st.isGrudge && st.favor > -0.15 -> {
                st.status = ""
                st.snubs = 0
            }
        }
        // Refresh status week if we just formed.
        if (st.status != before && st.status.isNotEmpty()) st.statusWeek = week
    }

    formedPet?.let { beatCriticStatus(it, release, pet = true) }
    formedGrudge?.let { beatCriticStatus(it, release, pet = false) }
    notable?.let { (review, kind) -> beatBiasedReview(review, release, kind) }
}

/** Slow thaw / cool-off so grudges and pets don't last forever without contact. */
internal fun GameState.criticStandingWeek(g: GamesStudio) {
    if (week % 4 != 0) return
    for (st in g.criticStanding) {
        if (st.favor == 0.0 && st.status.isEmpty()) continue
        val pull = when {
            st.isPet -> -0.02
            st.isGrudge -> 0.02
            else -> -st.favor * 0.04
        }
        st.favor = (st.favor + pull).coerceIn(-1.0, 1.0)
        if (abs(st.favor) < 0.05) st.favor = 0.0
        if (st.isPet && st.favor < 0.15) {
            st.status = ""
            st.hitsInLane = max(0, st.hitsInLane - 1)
        } else if (st.isGrudge && st.favor > -0.15) {
            st.status = ""
            st.snubs = max(0, st.snubs - 1)
        }
    }
}

private fun GameState.beatCriticStatus(st: CriticStanding, release: GameRelease, pet: Boolean) {
    val name = st.outlet.title
    val profile = criticProfile(st.outlet)
    val genres = profile.genres.take(2).joinToString(" and ") { it.lower }
    if (pet) {
        publish(
            NewsDesk.GAMES, name,
            "$name can't get enough of $companyName",
            "After ${release.title} and a streak of ${genres.ifEmpty { "genre" }} hits, $name is openly rooting for the studio. Expect warmer scores.",
            LogTone.GOOD, Lane.GAMES, tag = NewsTag.RUMOUR,
            quote = "We've been waiting for a studio to nail this beat.", quoteBy = name,
            cover = release.newsCover(),
        )
        feedOnCriticBeat(name, "pet", release.title)
        addLog("$name has taken a shine to you — a lasting pet in the press.", LogTone.GOOD, Lane.GAMES)
    } else {
        publish(
            NewsDesk.GAMES, name,
            "$name has it in for $companyName",
            "A streak of snubs, ${profile.disliked.firstOrNull()?.lower ?: "odd"} releases, or a public spat over ${release.title} has soured $name. Future coverage may sting.",
            LogTone.BAD, Lane.GAMES, tag = NewsTag.RUMOUR,
            quote = "We're done giving $companyName the benefit of the doubt.", quoteBy = name,
            cover = release.newsCover(),
        )
        feedOnCriticBeat(name, "grudge", release.title)
        addLog("$name is holding a grudge. Watch those review scores.", LogTone.BAD, Lane.GAMES)
    }
}

private fun GameState.beatBiasedReview(review: Review, release: GameRelease, kind: String) {
    val name = review.outlet.title
    val sc = review.formatted + review.outlet.scale
    when (kind) {
        "spat" -> {
            publish(
                NewsDesk.GAMES, name,
                "$name pans ${release.title} harder than anyone",
                "While other desks landed near ${String.format(java.util.Locale.US, "%.1f", release.score)}, $name went with $sc. Industry watchers call it personal.",
                LogTone.BAD, Lane.GAMES, tag = NewsTag.REVIEW,
                quote = review.comment, quoteBy = name, cover = release.newsCover(),
            )
            feedOnCriticBeat(name, "spat", release.title)
        }
        "rave" -> {
            publish(
                NewsDesk.GAMES, name,
                "$name crowns ${release.title}",
                "$name's $sc sits well above the pack — right in their wheelhouse.",
                LogTone.GOOD, Lane.GAMES, tag = NewsTag.REVIEW,
                quote = review.headline.ifEmpty { review.comment }, quoteBy = name, cover = release.newsCover(),
            )
            feedOnCriticBeat(name, "rave", release.title)
        }
    }
}

/** Status line for the Press UI. */
fun CriticStanding.storyLine(profile: CriticProfile): String = when {
    isPet -> "Pet outlet — warmer scores after your ${profile.genres.firstOrNull()?.lower ?: "genre"} streak."
    isGrudge -> "Holding a grudge — expect spicier pans and cooler numbers."
    favor >= 0.2 -> "Warming to you."
    favor <= -0.2 -> "Growing cooler."
    else -> profile.blurb
}
