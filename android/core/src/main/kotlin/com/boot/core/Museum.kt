package com.boot.core

import kotlinx.serialization.Serializable

/*
 * Museum / legacy mode: an endgame wing on the Hall / Company screen exhibiting decades of
 * hits, flops, scars and triumphs. Readable timeline — not a new sim lane.
 */

@Serializable
enum class LegacyBeatKind(val title: String) {
    HIT("Hit"),
    FLOP("Flop"),
    SCAR("Scar"),
    TRIUMPH("Triumph"),
    MILESTONE("Milestone"),
}

/** One exhibit in the company museum. */
@Serializable
data class LegacyBeat(
    val week: Int,
    val kind: LegacyBeatKind,
    val title: String,
    val detail: String,
)

private const val MUSEUM_CAP = 80

/** Record a beat for the museum timeline. Safe to call anytime. */
fun GameState.museumBeat(kind: LegacyBeatKind, title: String, detail: String = "") {
    museum += LegacyBeat(week, kind, title, detail)
    if (museum.size > MUSEUM_CAP) museum = museum.takeLast(MUSEUM_CAP)
}

/** Chronological exhibits for the UI (oldest first). */
fun GameState.museumTimeline(): List<LegacyBeat> = museum.sortedBy { it.week }

/** Hook scars into the museum without duplicating Reputation. */
internal fun GameState.museumOnScar(kind: ScarKind, note: String) {
    museumBeat(LegacyBeatKind.SCAR, kind.title, note)
}

/** After a player game ships — hits and flops for the wing. */
internal fun GameState.museumOnShip(r: GameRelease) {
    when {
        r.score >= 9 -> museumBeat(LegacyBeatKind.HIT, "Masterpiece: ${r.title}", "Scored ${String.format(java.util.Locale.US, "%.1f", r.score)}.")
        r.score >= 8 -> museumBeat(LegacyBeatKind.HIT, "Hit: ${r.title}", "Scored ${String.format(java.util.Locale.US, "%.1f", r.score)}.")
        r.score < 5 -> museumBeat(LegacyBeatKind.FLOP, "Flop: ${r.title}", "Scored ${String.format(java.util.Locale.US, "%.1f", r.score)}.")
    }
}
