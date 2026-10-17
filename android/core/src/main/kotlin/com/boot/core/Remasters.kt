package com.boot.core

/*
 * Remasters: an old hit rebuilt for today's platforms. The design is done, so it takes half the
 * work of a new game. Critics judge it like any other game, but its launch is smaller: it sells to
 * the people who remember the original, more of them the better the original was.
 */

/** A remaster's share of a new game's work. */
const val REMASTER_WORK = 0.5

/** How old a game has to be before a remaster makes sense. */
const val REMASTER_AGE = 156

/** Launch sales against a new game's, from the original's score: 0.6 for a 7, 0.8 for a 9. */
internal fun remasterLift(originalScore: Double): Double = (0.6 + 0.1 * (originalScore - 7)).clamped(0.5, 0.9)

/** Why [releaseId] can't be remastered, or null if it can. */
fun GameState.remasterBlocker(releaseId: Int): String? {
    val g = games ?: return "You don't run a game studio."
    if (Research.REMASTERS !in researched) return "Research remasters first."
    val r = g.release(releaseId) ?: return "That game is gone."
    if (r.developer != null) return "${r.title} belongs to ${r.developer}."
    if (r.publisher != null) return "${r.publisher} owns ${r.title}."
    if (r.concept.remasterOf != null) return "That's already a remaster."
    if (r.score < 7) return "Only games that scored 7 or more are worth remastering."
    if (week - r.week < REMASTER_AGE) return "Too recent. Wait until it's three years old (${GameClock.label(r.week + REMASTER_AGE)})."
    if (g.releases.any { it.concept.remasterOf == r.id } || listOfNotNull(g.project, g.project2).any { it.concept.remasterOf == r.id }) {
        return "${r.title} has already been remastered."
    }
    return null
}

/** A remaster of [releaseId] for the designer: same game, today's engine and platforms. */
fun GameState.remasterConcept(releaseId: Int): GameConcept {
    val g = games ?: throw notAvailable("You don't run a game studio.")
    val r = g.release(releaseId) ?: throw invalid("No such game.")
    val platforms = r.concept.platforms.filter { availablePlatform(it) }.ifEmpty {
        platformsOnSale.filter { it != Platform.OWN_CONSOLE }.sortedByDescending { platformPlayers(it) }.take(1)
    }
    return r.concept.copy(
        title = "${r.title} Remastered", sequelOf = null, remasterOf = r.id, engineId = g.engines.maxBy { it.level }.id,
        platforms = platforms, live = false,
    )
}

/** Your games that could be remastered now. */
val GameState.remasterable: List<GameRelease>
    get() = games?.releases?.filter { remasterBlocker(it.id) == null }.orEmpty()
