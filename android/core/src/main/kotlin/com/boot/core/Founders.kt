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
    refreshFounderPerks()
}

/** The founder's trait, if they have it. */
internal fun GameState.founderIs(trait: FounderTrait) = founder.trait == trait || founder.trait2 == trait

/*
 * The founder grows. You earn experience by running the company, a little every week and more for
 * each launch and hit, and every level brings a skill point for a small tree in three branches:
 * craft, people and business. Each branch has to be learned in order. At level 5 you pick a
 * second trait.
 */

/** The three branches of the founder's skill tree. */
enum class SkillBranch(val title: String) { CRAFT("Craft"), PEOPLE("People"), BUSINESS("Business") }

/** A founder skill, in a [branch] at a [tier] (1 to 3, learned in order). */
enum class FounderSkill(val branch: SkillBranch, val tier: Int, val title: String, val detail: String) {
    HANDS_ON(SkillBranch.CRAFT, 1, "Hands-on", "Your own work counts 20% more."),
    DESIGN_EYE(SkillBranch.CRAFT, 2, "Eye for design", "You lift the design of everything you touch."),
    CRAFTSMANSHIP(SkillBranch.CRAFT, 3, "Craftsmanship", "Everything the company makes is a notch better."),
    MENTOR(SkillBranch.PEOPLE, 1, "Mentor", "Your people gain experience 20% faster."),
    STEADY_HAND(SkillBranch.PEOPLE, 2, "Steady hand", "Morale settles 4 points higher."),
    INSPIRING(SkillBranch.PEOPLE, 3, "Inspiring", "People quit half as often, and stories end better."),
    PITCH(SkillBranch.BUSINESS, 1, "Pitch", "Shows and trailers build 15% more hype."),
    NETWORKER(SkillBranch.BUSINESS, 2, "Networker", "Reputation gains are 15% bigger and losses 15% smaller."),
    DEALMAKER(SkillBranch.BUSINESS, 3, "Dealmaker", "4% more demand for everything you sell."),
}

const val FOUNDER_MAX_LEVEL = 10

/** The level at which you pick a second trait. */
const val SECOND_TRAIT_LEVEL = 5

/** Experience to go from [level] to the next. */
fun founderXpToNext(level: Int): Double = 40.0 + 20.0 * level

/** Skill points earned and not yet spent. */
val Founder.skillPoints: Int get() = level - 1 - skills.size

/** The founder has [skill]. */
internal fun GameState.founderHas(skill: FounderSkill) = skill in founder.skills

/** Why [skill] can't be learned now, or null if it can. */
fun GameState.skillBlocker(skill: FounderSkill): String? = when {
    founderHas(skill) -> "You already have it."
    FounderSkill.entries.any { it.branch == skill.branch && it.tier < skill.tier && it !in founder.skills } ->
        "Learn the ${skill.branch.title.lowercase()} skills before it first."
    founder.skillPoints <= 0 -> "You need a skill point: they come with each level."
    else -> null
}

fun GameState.learnSkill(skill: FounderSkill) {
    requireLive()
    skillBlocker(skill)?.let { throw invalid(it) }
    founder.skills += skill
    refreshFounderPerks()
    addLog("You learned ${skill.title.lowercase()}. ${skill.detail}", LogTone.GOOD)
}

/** The traits you could take as your second. Bootstrapper only matters on day one. */
val GameState.secondTraitChoices: List<FounderTrait>
    get() = FounderTrait.entries.filter { it != founder.trait && it != FounderTrait.BOOTSTRAPPER }

fun GameState.pickSecondTrait(trait: FounderTrait) {
    requireLive()
    if (founder.level < SECOND_TRAIT_LEVEL) throw invalid("A second trait comes at level $SECOND_TRAIT_LEVEL.")
    if (founder.trait2 != null) throw invalid("You've already picked your second trait.")
    if (trait !in secondTraitChoices) throw invalid("Pick a different trait.")
    founder.trait2 = trait
    refreshFounderPerks()
    addLog("You're a ${trait.title.lowercase()} now, too. ${trait.detail}", LogTone.MILESTONE)
}

/** Keeps the effects that live outside the founder (the press's view of you) in step with your traits and skills. */
internal fun GameState.refreshFounderPerks() {
    reputation.lift = (if (founderIs(FounderTrait.MEDIA_DARLING)) 1.3 else 1.0) * (if (founderHas(FounderSkill.NETWORKER)) 1.15 else 1.0)
}

/** Experience for the founder, and a level (or several) when there's enough. */
internal fun GameState.founderXp(amount: Double) {
    val f = founder
    if (f.level >= FOUNDER_MAX_LEVEL) return
    f.xp += amount
    while (f.level < FOUNDER_MAX_LEVEL && f.xp >= founderXpToNext(f.level)) {
        f.xp -= founderXpToNext(f.level)
        f.level += 1
        stats.founderLevels += 1
        val extra = if (f.level == SECOND_TRAIT_LEVEL) " and a second trait to pick" else ""
        addLog("You reached level ${f.level}. A skill point to spend$extra: see You on the Team tab.", LogTone.MILESTONE)
    }
    if (f.level >= FOUNDER_MAX_LEVEL) f.xp = 0.0
}

/** Your hype multiplier on stage. */
internal val GameState.founderHype: Double
    get() = (if (founderIs(FounderTrait.SHOWRUNNER)) 1.3 else 1.0) * (if (founderHas(FounderSkill.PITCH)) 1.15 else 1.0)
