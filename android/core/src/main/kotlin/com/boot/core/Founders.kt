package com.boot.core

import kotlinx.serialization.Serializable

/*
 * You, the founder: how you look (chosen on the new game screen and kept in the save, so the
 * office and every portrait show you as you made yourself) and the one special trait that shapes
 * how your run plays.
 */

/** How the founder looks, as positions in the game's palettes of skin tones, hair, styles and clothes. */
@Serializable
data class Appearance(
    val skin: Int = 0,
    val hair: Int = 0,
    val style: Int = 0,
    val glasses: Boolean = false,
    val beard: Boolean = false,
    val outfit: Int = 0,
    val top: Int = 0,
)

/** A founder's special talent. One each. */
enum class FounderTrait(val title: String, val detail: String) {
    CODER("Coder", "You build things yourself: your own work counts 40% more."),
    VISIONARY("Visionary", "Ideas come easily: your design sense lifts the studio's design."),
    PEOPLE_PERSON("People person", "Everyone likes working for you: morale settles 8 points higher."),
    SHOWRUNNER("Showrunner", "Every stage loves you: shows, trailers and showcases build 30% more hype."),
    TINKERER("Tinkerer", "Always taking things apart: research comes 25% faster."),
    BOOTSTRAPPER("Bootstrapper", "You saved hard before you started: 60% more cash on day one."),
    MEDIA_DARLING("Media darling", "The press adores you: reputation gains are 30% bigger and losses 30% smaller."),
}

/** Sets the founder up as chosen on the new game screen. Call before the first week. */
fun GameState.introduceFounder(trait: FounderTrait?, appearance: Appearance?) {
    founder.trait = trait
    founder.appearance = appearance
    if (trait == FounderTrait.BOOTSTRAPPER && !sandbox) {
        cash *= 1.6
        ledger = listOf(WeekLedger(0, cash = cash))
    }
    if (trait == FounderTrait.MEDIA_DARLING) reputation.lift = 1.3
}

/** The founder's trait, if they have it. */
internal fun GameState.founderIs(trait: FounderTrait) = founder.trait == trait
