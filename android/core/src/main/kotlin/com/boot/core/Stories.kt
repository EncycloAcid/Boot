package com.boot.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.max
import kotlin.math.min

/*
 * Staff stories: short arcs for the people on your team, told over a few months in two or three
 * decisions. Each changes how those people work while it lasts, and how it ends is up to you.
 *
 * - A dream team: two people on the same team who are brilliant together. Pair them up, and later
 *   they may want a project of their own.
 * - Their name on the box: a star on the game team wants credit. Give it, and the next game makes
 *   them famous, for better or worse.
 * - Running on empty: someone staying later every night. Send them home, or let them run until
 *   they need a real break.
 * - A mentor: a veteran takes a newcomer under their wing. Make it official and the newcomer grows
 *   up fast, at some cost to the veteran's own work.
 *
 * Stories sit alongside the one-off people decisions (poaching, raises, burnout, clashes), and a
 * person is only ever in one story at a time.
 */

enum class StoryKind(val title: String) {
    DREAM_TEAM("Dream team"),
    NAME_ON_THE_BOX("Name on the box"),
    RUNNING_ON_EMPTY("Running on empty"),
    MENTOR("Under their wing"),
}

/** One story, its people (the first is the lead) and where it's got to. */
@Serializable
data class StaffStory(
    val id: Int,
    val kind: StoryKind,
    val people: List<Int>,
    val startedWeek: Int,
    /** Where the story has got to; what it means depends on the kind. */
    var stage: Int = 0,
    /** When the next chapter is due, if one is scheduled. */
    var nextWeek: Int? = null,
    /** A level to measure growth against (the newcomer's, in a mentoring story). */
    val mark: Int = 0,
    /** How it ended, once it has. */
    var ending: String? = null,
    /** Ended, but what it changed stays: a pair leading together, a creative director. */
    var lasting: Boolean = false,
)

/** A chapter of a story that needs a decision. */
@Serializable @SerialName("storyBeat")
data class StoryBeat(val storyId: Int, val chapter: Int) : EventKind

// Stages, by kind.
private const val OFFERED = 0
private const val PAIRED = 1
private const val LEADING = 2
private const val CREDITED = 1
private const val DIRECTOR = 2
private const val HOME_ON_TIME = 1
private const val RUNNING_HOT = 2
private const val RESTING = 3
private const val PUSHING = 4
private const val MENTORING = 1

private const val MAX_ACTIVE = 2

val GameState.activeStories: List<StaffStory> get() = stories.filter { it.ending == null }

/** The story someone's in right now, if any. */
fun GameState.storyOf(employeeId: Int): StaffStory? = activeStories.firstOrNull { employeeId in it.people }

/** Stories still shaping how someone works: the one they're in, and any that ended with a lasting change. */
fun GameState.storiesShaping(employeeId: Int): List<StaffStory> = stories.filter { (it.ending == null || it.lasting) && employeeId in it.people }

private fun GameState.who(id: Int): Employee? = staff.firstOrNull { it.id == id }
private fun firstName(name: String) = name.substringBefore(' ')

/** What the story does to this person's output right now. */
internal fun GameState.storyOutput(e: Employee): Double = storiesShaping(e.id).fold(1.0) { m, s -> m * storyOutput(e, s) }

private fun GameState.storyOutput(e: Employee, s: StaffStory): Double {
    return when (s.kind) {
        StoryKind.DREAM_TEAM -> {
            val partner = s.people.firstOrNull { it != e.id }?.let { who(it) }
            if (partner == null || partner.lane != e.lane || partner.leaveUntil > week) 1.0
            else when (s.stage) {
                PAIRED -> 1.15
                LEADING -> 1.25
                else -> 1.0
            }
        }
        StoryKind.RUNNING_ON_EMPTY -> when (s.stage) {
            HOME_ON_TIME -> 0.9
            RUNNING_HOT -> 1.15
            else -> 1.0
        }
        StoryKind.MENTOR -> if (s.stage == MENTORING && e.id == s.people[0]) 0.9 else 1.0
        StoryKind.NAME_ON_THE_BOX -> 1.0
    }
}

/** What the story does to how fast this person learns. */
internal fun GameState.storyXp(e: Employee): Double {
    val s = storyOf(e.id) ?: return 1.0
    return if (s.kind == StoryKind.MENTOR && s.stage == MENTORING && e.id == s.people[1]) 2.0 else 1.0
}

/** Quality a story adds to a lane: a dream team leading a project, a famous creative director. */
internal fun GameState.storyQuality(lane: Lane): Double = stories.filter { it.ending == null || it.lasting }.sumOf { s ->
    val lead = who(s.people[0])
    when {
        lead?.lane != lane -> 0.0
        s.kind == StoryKind.DREAM_TEAM && s.stage == LEADING -> 1.0
        s.kind == StoryKind.NAME_ON_THE_BOX && s.stage == DIRECTOR -> 1.5
        else -> 0.0
    }
}

/** Starts stories now and then, and moves the ones under way along. */
internal fun GameState.storyWeek() {
    for (s in activeStories) advanceStory(s)
    // Lasting changes end when someone leaves.
    for (s in stories.filter { it.lasting }) if (s.people.any { id -> who(id) == null }) s.lasting = false
    if (week < 12 || activeStories.size >= MAX_ACTIVE || !rng.chance(0.03)) return
    val free = staff.filter { storyOf(it.id) == null && it.leaveUntil <= week && it.lane != null }
    val starts = mutableListOf<() -> Unit>()
    // Two people on the same team, good at different things, both happy enough to click.
    val pairs = free.flatMap { a -> free.filter { b -> b.id > a.id && b.lane == a.lane && b.role != a.role && a.morale > 0.5 && b.morale > 0.5 } .map { a to it } }
    if (pairs.isNotEmpty()) starts += { startStory(StoryKind.DREAM_TEAM, rng.pick(pairs).toList().map { it.id }) }
    // A star on the game team.
    val stars = free.filter {
        it.lane == Lane.GAMES && it.skill >= 7 && (it.role == Role.DESIGNER || it.role == Role.ENGINEER) && it.trait != Trait.LOYAL &&
            games?.releases?.isNotEmpty() == true && stories.none { s -> s.kind == StoryKind.NAME_ON_THE_BOX && it.id in s.people }
    }
    if (stars.isNotEmpty() && games?.project != null) starts += { startStory(StoryKind.NAME_ON_THE_BOX, listOf(rng.pick(stars).id)) }
    // Someone working too hard.
    val tired = free.filter { it.skill >= 4 && (it.morale < 0.65 || crunch) }
    if (tired.isNotEmpty()) starts += { startStory(StoryKind.RUNNING_ON_EMPTY, listOf(rng.pick(tired).id)) }
    // A veteran and a newcomer on the same team.
    val mentors = free.flatMap { a -> free.filter { b -> b.lane == a.lane && a.level >= 5 && b.level <= 2 && !b.maxLevel }.map { a to it } }
    if (mentors.isNotEmpty()) starts += { val (a, b) = rng.pick(mentors); startStory(StoryKind.MENTOR, listOf(a.id, b.id), mark = b.level) }
    if (starts.isNotEmpty()) rng.pick(starts)()
}

internal fun GameState.startStory(kind: StoryKind, people: List<Int>, mark: Int = 0) {
    val s = StaffStory(nextId(), kind, people, week, mark = mark)
    stories = stories + s
    enqueue(StoryBeat(s.id, 0))
}

/** Ends a story with a line for the history. */
private fun GameState.close(s: StaffStory, ending: String, tone: LogTone = LogTone.NEUTRAL) {
    s.ending = ending
    s.nextWeek = null
    stats.stories += 1
    addLog(ending, tone, who(s.people[0])?.lane)
    // Keep the last few finished stories for the Team tab.
    val done = stories.filter { it.ending != null && !it.lasting }
    if (done.size > 8) stories = stories - done.first()
}

private fun GameState.advanceStory(s: StaffStory) {
    val people = s.people.map { who(it) }
    // Someone left: the story ends with them.
    if (people.any { it == null }) {
        val left = people.filterNotNull().firstOrNull()
        s.ending = if (left != null) "${firstName(left.name)}'s story ended when their partner left." else "A story ended when they left."
        s.nextWeek = null
        stats.stories += 1
        return
    }
    val a = people[0]!!
    when (s.kind) {
        StoryKind.DREAM_TEAM -> if (s.stage == PAIRED && (s.nextWeek ?: Int.MAX_VALUE) <= week) {
            s.nextWeek = null
            enqueue(StoryBeat(s.id, 1))
        }
        StoryKind.NAME_ON_THE_BOX -> if (s.stage == CREDITED) {
            val release = games?.releases?.firstOrNull { it.week > s.startedWeek }
            if (release != null && s.nextWeek == null) {
                s.nextWeek = week
                enqueue(StoryBeat(s.id, 1))
            }
        }
        StoryKind.RUNNING_ON_EMPTY -> {
            val due = (s.nextWeek ?: Int.MAX_VALUE) <= week
            when {
                s.stage == HOME_ON_TIME && due -> {
                    a.morale = min(1.0, a.morale + 0.1)
                    close(s, "${a.name} is back to their old self after a few weeks of going home on time.", LogTone.GOOD)
                }
                s.stage == RUNNING_HOT -> {
                    a.morale = max(0.0, a.morale - 0.01)
                    if (due) {
                        s.nextWeek = null
                        enqueue(StoryBeat(s.id, 1))
                    }
                }
                s.stage == RESTING && a.leaveUntil <= week -> {
                    a.morale = max(a.morale, 0.85)
                    a.xp += a.xpToNext * 0.5
                    close(s, "${a.name} is back, rested, and grateful you noticed. They're working better than ever.", LogTone.GOOD)
                }
                s.stage == PUSHING && due -> {
                    val holds = if (founderHas(FounderSkill.INSPIRING)) 0.75 else 0.5
                    if (a.trait == Trait.LOYAL || rng.chance(holds)) {
                        a.morale = max(0.0, a.morale - 0.2)
                        close(s, "${a.name} made it through, just. They haven't forgotten.", LogTone.NEUTRAL)
                    } else {
                        staff = staff.filterNot { it.id == a.id }
                        close(s, "${a.name} quit. They'd been running on empty for months.", LogTone.BAD)
                    }
                }
            }
        }
        StoryKind.MENTOR -> if (s.stage == MENTORING) {
            val b = people[1]!!
            if (b.level >= s.mark + 2 || b.maxLevel) {
                a.morale = min(1.0, a.morale + 0.1)
                b.morale = min(1.0, b.morale + 0.1)
                a.xp += a.xpToNext * 0.3
                close(s, "${b.name} has grown up fast under ${firstName(a.name)}'s wing: level ${b.level} already.", LogTone.GOOD)
            } else if (week - s.startedWeek > 40) {
                close(s, "${firstName(a.name)} and ${firstName(b.name)} wrapped up their mentoring.", LogTone.NEUTRAL)
            }
        }
    }
}

internal fun GameState.describeStory(k: StoryBeat): Description? {
    val s = stories.firstOrNull { it.id == k.storyId && it.ending == null } ?: return null
    val a = who(s.people[0]) ?: return null
    val b = s.people.getOrNull(1)?.let { who(it) }
    val an = firstName(a.name)
    val bn = b?.let { firstName(it.name) } ?: ""
    return when (s.kind) {
        StoryKind.DREAM_TEAM -> {
            b ?: return null
            if (k.chapter == 0) {
                Description(
                    "${a.name} and ${b.name} are on a roll",
                    "Your ${a.role.title.lowercase()} and ${b.role.title.lowercase()} have started finishing each other's sentences. " +
                        "What they make together is better than what either makes alone.",
                    listOf(
                        choice("Pair them up", "They work as a pair from now on: 15% more from each, as long as they're on the same team."),
                        choice("Keep things as they are", "A nice moment. They're a little happier."),
                    ),
                    a.lane,
                )
            } else {
                Description(
                    "$an and $bn want their own project",
                    "Months in, the pair are the best thing on the team, and they know it. They want to lead something together, and a raise to go with it.",
                    listOf(
                        choice("Give them the lead", "10% more pay each. 25% more from both, and their team's work gets better."),
                        choice("Not yet", "They stay paired, but they're disappointed."),
                    ),
                    a.lane,
                )
            }
        }
        StoryKind.NAME_ON_THE_BOX -> {
            val g = games ?: return null
            if (k.chapter == 0) {
                Description(
                    "${a.name} wants their name on the box",
                    "Your best ${a.role.title.lowercase()} (skill ${a.skill}) is tired of being invisible. " +
                        "They want a credit on the cover of the next game: \"A ${a.name} game\".",
                    listOf(
                        choice("Put their name on it", "They're thrilled. How the next game does will be theirs, too."),
                        choice("The studio makes the games", "No names on boxes. They take it badly."),
                    ),
                    Lane.GAMES,
                )
            } else {
                val r = g.releases.firstOrNull { it.week > s.startedWeek } ?: return null
                val sc = String.format(java.util.Locale.US, "%.1f", r.score)
                if (r.score >= 7) {
                    Description(
                        "${a.name} is famous now",
                        "\"A ${a.name} game\" reviewed at $sc, and the press wants to talk to $an, not you. Other studios have noticed, too.",
                        listOf(
                            choice("Make them creative director", "25% more pay. Every game the team makes is better for it, and fans follow the name."),
                            choice("Keep them where they are", "No new title. Someone will call with a better offer."),
                        ),
                        Lane.GAMES,
                    )
                } else {
                    Description(
                        "${a.name}'s name is on a flop",
                        "\"A ${a.name} game\" reviewed at $sc. The reviews name $an, and the forums are worse. They're taking it hard.",
                        listOf(
                            choice("Stand by them", "Publicly. They'll remember it."),
                            choice("Take the name off the next one", "Quietly. They'll remember that too."),
                        ),
                        Lane.GAMES,
                    )
                }
            }
        }
        StoryKind.RUNNING_ON_EMPTY -> if (k.chapter == 0) {
            Description(
                "${a.name} keeps staying late",
                "$an is the last one out every night and the first one in. The work is good. The bags under their eyes are not.",
                listOf(
                    choice("Send them home on time", "A little less from them for a few weeks, and they'll be fine."),
                    choice("Let them work", "15% more from them for now. Nobody can keep that up for ever."),
                ),
                a.lane,
            )
        } else {
            Description(
                "${a.name} is running on empty",
                "Months of late nights have caught up with $an: mistakes, short temper, a cold that won't go away. They need a real break.",
                listOf(
                    choice("Four weeks off", "Paid, and no work from them, but they'll come back better than before."),
                    choice("Push through to the next milestone", "They keep working. They might not stay."),
                ),
                a.lane,
            )
        }
        StoryKind.MENTOR -> {
            b ?: return null
            Description(
                "$an has taken $bn under their wing",
                "${a.name} (level ${a.level}) has been showing the new ${b.role.title.lowercase()} ${b.name} the ropes on their own time.",
                listOf(
                    choice("Make it official", "$bn learns twice as fast until they've grown. $an does 10% less of their own work."),
                    choice("Everyone has their own work", "Back to it. $bn learns the slow way."),
                ),
                a.lane,
            )
        }
    }
}

internal fun GameState.resolveStory(k: StoryBeat, choice: Int) {
    val s = stories.firstOrNull { it.id == k.storyId && it.ending == null } ?: return
    val a = who(s.people[0]) ?: return
    val b = s.people.getOrNull(1)?.let { who(it) }
    val an = firstName(a.name)
    val bn = b?.let { firstName(it.name) } ?: ""
    val kind = if (founderHas(FounderSkill.INSPIRING)) 0.5 else 1.0
    when (s.kind) {
        StoryKind.DREAM_TEAM -> {
            b ?: return
            if (k.chapter == 0) {
                if (choice == 0) {
                    s.stage = PAIRED
                    s.nextWeek = week + rng.int(16, 26)
                    addLog("$an and $bn are a pair now.", LogTone.GOOD, a.lane)
                } else {
                    a.morale = min(1.0, a.morale + 0.05)
                    b.morale = min(1.0, b.morale + 0.05)
                    close(s, "$an and $bn went back to their own work.")
                }
            } else if (choice == 0) {
                s.stage = LEADING
                a.salary *= 1.1
                b.salary *= 1.1
                a.morale = min(1.0, a.morale + 0.15)
                b.morale = min(1.0, b.morale + 0.15)
                // The story's over, but the pair keeps leading.
                s.lasting = true
                close(s, "$an and $bn lead together now, and the team's better for it.", LogTone.GOOD)
            } else {
                a.morale = max(0.0, a.morale - 0.15 * kind)
                b.morale = max(0.0, b.morale - 0.15 * kind)
                addLog("$an and $bn will have to wait to lead. They're disappointed.", LogTone.BAD, a.lane)
            }
        }
        StoryKind.NAME_ON_THE_BOX -> if (k.chapter == 0) {
            if (choice == 0) {
                s.stage = CREDITED
                a.morale = min(1.0, a.morale + 0.1)
                addLog("The next game will be \"a ${a.name} game\".", LogTone.NEUTRAL, Lane.GAMES)
            } else {
                a.morale = max(0.0, a.morale - 0.2 * kind)
                close(s, "No names on the box. $an took it badly.", LogTone.BAD)
            }
        } else {
            val r = games?.releases?.firstOrNull { it.week > s.startedWeek }
            val hit = (r?.score ?: 0.0) >= 7
            when {
                hit && choice == 0 -> {
                    s.stage = DIRECTOR
                    a.salary *= 1.25
                    a.morale = min(1.0, a.morale + 0.2)
                    games?.let { it.fans += max(1_000.0, it.fans * 0.03) }
                    publish(NewsDesk.GAMES, "Edge", "${a.name} named creative director at $companyName", "Fans of \"a ${a.name} game\" have a name to follow.", LogTone.GOOD, Lane.GAMES)
                    s.lasting = true
                    close(s, "${a.name} is your creative director now.", LogTone.MILESTONE)
                }
                hit -> {
                    close(s, "${a.name} stayed where they were. The phone started ringing.", LogTone.NEUTRAL)
                    val rival = world.rivals.filter { it.lane == Lane.GAMES }.ifEmpty { world.rivals }.firstOrNull()
                    if (rival != null && a.trait != Trait.LOYAL) enqueue(EventKind.Poach(a.id, rival.name))
                }
                choice == 0 -> {
                    a.morale = min(1.0, a.morale + 0.2)
                    close(s, "You stood by $an. They won't forget it.", LogTone.GOOD)
                }
                else -> {
                    a.morale = max(0.0, a.morale - 0.3 * kind)
                    close(s, "$an's name came off the box.", LogTone.BAD)
                }
            }
        }
        StoryKind.RUNNING_ON_EMPTY -> if (k.chapter == 0) {
            if (choice == 0) {
                s.stage = HOME_ON_TIME
                s.nextWeek = week + 6
                addLog("$an goes home on time now.", LogTone.NEUTRAL, a.lane)
            } else {
                s.stage = RUNNING_HOT
                s.nextWeek = week + rng.int(5, 9)
                addLog("$an keeps working late.", LogTone.NEUTRAL, a.lane)
            }
        } else if (choice == 0) {
            s.stage = RESTING
            a.leaveUntil = week + 4
            addLog("$an is off for four weeks.", LogTone.NEUTRAL, a.lane)
        } else {
            s.stage = PUSHING
            s.nextWeek = week + rng.int(2, 6)
            addLog("$an is pushing through.", LogTone.BAD, a.lane)
        }
        StoryKind.MENTOR -> {
            b ?: return
            if (choice == 0) {
                s.stage = MENTORING
                addLog("$an is mentoring $bn.", LogTone.GOOD, a.lane)
            } else {
                close(s, "$an went back to their own work, and $bn to theirs.")
            }
        }
    }
}

/** One line on where a story has got to, for the Team tab and people's cards. */
fun GameState.storyStatus(s: StaffStory): String {
    if (!s.lasting) s.ending?.let { return it }
    val a = who(s.people[0])?.let { firstName(it.name) } ?: "?"
    val b = s.people.getOrNull(1)?.let { who(it) }?.let { firstName(it.name) } ?: ""
    return when (s.kind) {
        StoryKind.DREAM_TEAM -> when (s.stage) {
            PAIRED -> "$a and $b work as a pair: 15% more from each."
            LEADING -> "$a and $b lead together: 25% more from each, and better work from their team."
            else -> "$a and $b are on a roll."
        }
        StoryKind.NAME_ON_THE_BOX -> when (s.stage) {
            CREDITED -> "The next game is \"a ${who(s.people[0])?.name} game\"."
            DIRECTOR -> "$a is creative director: every game is a notch better."
            else -> "$a wants their name on the box."
        }
        StoryKind.RUNNING_ON_EMPTY -> when (s.stage) {
            HOME_ON_TIME -> "$a goes home on time for a few weeks."
            RUNNING_HOT -> "$a is working late every night: 15% more, for now."
            RESTING -> "$a is on a four-week break."
            PUSHING -> "$a is pushing through."
            else -> "$a keeps staying late."
        }
        StoryKind.MENTOR -> when (s.stage) {
            MENTORING -> "$a is mentoring $b: $b learns twice as fast."
            else -> "$a has taken $b under their wing."
        }
    }
}
