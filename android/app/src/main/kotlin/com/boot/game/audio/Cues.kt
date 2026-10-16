package com.boot.game.audio

import com.boot.core.EndingKind
import com.boot.core.GameState
import kotlin.math.abs

/**
 * The one sound worth playing for a change in the game, or null for none. [ticking] is true
 * when the change is a week passing rather than something the player did; money moving by
 * itself every week would soon get tiresome, so it only rings for the player's own actions.
 * Most important first, so a busy week never stacks sounds on top of each other.
 */
fun cueFor(before: GameState, after: GameState, ticking: Boolean): Sfx? {
    after.ending?.takeIf { before.ending == null }?.let {
        return if (it.kind == EndingKind.BANKRUPT || it.kind == EndingKind.OUSTED) Sfx.ENDING_BAD else Sfx.ENDING_GOOD
    }
    val released = (after.games?.releases?.size ?: 0) > (before.games?.releases?.size ?: 0)
    if (released) return Sfx.LAUNCH
    if (after.world.era != before.world.era) return Sfx.ERA
    if (after.office.ordinal > before.office.ordinal) return Sfx.LEVEL_UP
    if (after.achievements.size > before.achievements.size || after.goalsDone.size > before.goalsDone.size) return Sfx.ACHIEVEMENT
    if (after.needsDecision && !before.needsDecision) return Sfx.ALERT
    if (before.cash >= 0 && after.cash < 0) return Sfx.WARNING
    val ready = (after.games?.project?.isReady == true && before.games?.project?.isReady != true) ||
        (after.games?.project2?.isReady == true && before.games?.project2?.isReady != true)
    if (ready) return Sfx.READY
    if (after.researched.size > before.researched.size) return Sfx.RESEARCH
    if (after.staff.size > before.staff.size) return Sfx.HIRE
    if (!ticking && abs(after.cash - before.cash) >= 1.0) return Sfx.COIN
    return null
}

/** The sting after the last review: a cheer for a hit, a sad trombone for a flop, nothing in between. */
fun verdictFor(score: Double): Sfx? = when {
    score >= 8.0 -> Sfx.HIT
    score < 5.0 -> Sfx.FLOP
    else -> null
}
