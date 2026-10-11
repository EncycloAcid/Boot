package com.boot.core

import kotlinx.serialization.Serializable

/*
 * The tutorial. Rosa, a founder who has done it all before, walks you through your first weeks one
 * lesson at a time: make something, let time run, hire, read the numbers, research. Each lesson
 * ticks itself off when you do the thing (a few just need reading), and you can skip the lot.
 */

/** Your mentor's name. */
const val MENTOR = "Rosa"

@Serializable
enum class TutorialStep { WELCOME, FIRST_WORK, PLAY, HIRE, MONEY, RESEARCH, DONE }

@Serializable
data class Tutorial(var done: Set<TutorialStep> = emptySet())

/**
 * One lesson: what to do and why. [acknowledge] lessons are done once their button is pressed; the
 * others when the game sees you've done it. [progress] is how close you are, where that's a number.
 */
data class Lesson(
    val step: TutorialStep,
    val number: Int,
    val of: Int,
    val title: String,
    val body: String,
    val action: String,
    val acknowledge: Boolean,
    val progress: Double? = null,
)

/** The lesson you're on, or null with no tutorial. */
fun GameState.lesson(): Lesson? {
    val t = tutorial ?: return null
    val step = TutorialStep.entries.firstOrNull { it !in t.done && !learned(it) } ?: TutorialStep.DONE
    val n = step.ordinal + 1
    val of = TutorialStep.entries.size
    val lane = startingLane
    return when (step) {
        TutorialStep.WELCOME -> Lesson(
            step, n, of, "Welcome to $companyName",
            "I'm $MENTOR. I've started four companies and sold three, and I'll walk you through your first weeks. " +
                "Your cash is at the top. If it goes below zero and stays there for ${difficulty.redWeeks} weeks, it's over.",
            "Let's go", acknowledge = true,
        )
        TutorialStep.FIRST_WORK -> when (lane) {
            Lane.GAMES -> Lesson(
                step, n, of, "Make your first game",
                "Pick a topic and a genre that suit each other (a space strategy game, a medieval RPG), a platform people own, and start. " +
                    "A small game is quick and cheap, and teaches you what players like.",
                "Develop a game", acknowledge = false,
            )
            Lane.WEBSITES -> Lesson(
                step, n, of, "Take your first contract",
                "Clients send briefs to the agency. Take one your team can finish before its deadline: late work costs you standing, " +
                    "and standing is what brings bigger clients.",
                "See the briefs", acknowledge = false,
            )
            Lane.APPS -> Lesson(
                step, n, of, "Your app is in the works",
                "Every week your team puts in brings launch closer. Once it's live, people install it, some leave, and the ones who stay pay. " +
                    "Keeping them is the whole game.",
                "See the app", acknowledge = true,
            )
            Lane.SOCIAL -> Lesson(
                step, n, of, "Your platform is in the works",
                "It launches once it's built. Growth is slow, then suddenly very fast, as long as you keep the bots and the trolls down.",
                "See the platform", acknowledge = true,
            )
            Lane.HARDWARE -> Lesson(
                step, n, of, "Design your first device",
                "Pick a category and a tier. Budget devices are cheap to build; flagships sell for more and take longer. " +
                    "Order stock before launch: sell out and you lose sales, overorder and it sits in a warehouse.",
                "Design a device", acknowledge = false,
            )
            Lane.AI -> Lesson(
                step, n, of, "Train your first model",
                "Start a small training run. Compute is what costs, so watch your burn rate. A better model wins subscribers and API customers.",
                "Start training", acknowledge = false,
            )
        }
        TutorialStep.PLAY -> Lesson(
            step, n, of, "Let time run",
            "Tap ▶ at the top to run the calendar, or ⏭ for a week at a time. The clock stops by itself when something needs you: " +
                "a decision, a review, something ready to ship.",
            "Play a week", acknowledge = false,
        )
        TutorialStep.HIRE -> Lesson(
            step, n, of, "Hire your first person",
            "On your own you can only do so much. Look for someone good at your business. Traits matter too: press and hold one to see what it does.",
            "Meet candidates", acknowledge = false,
        )
        TutorialStep.MONEY -> Lesson(
            step, n, of, "Know your numbers",
            "The Company tab shows what comes in, what goes out and your runway: how many weeks until the money's gone at this rate. " +
                "Check it whenever you're about to spend.",
            "See the money", acknowledge = true,
        )
        TutorialStep.RESEARCH -> {
            val cheapest = cheapestResearch()
            Lesson(
                step, n, of, "Spend research points",
                "You earn research points every week, faster with more people. Research unlocks new things to make and better ways to make them." +
                    (cheapest?.let { " ${it.title} costs ${it.rp}; you have ${researchPoints.toInt()}." } ?: ""),
                "Research", acknowledge = false,
                progress = cheapest?.let { (researchPoints / it.rp).coerceIn(0.0, 1.0) },
            )
        }
        TutorialStep.DONE -> Lesson(
            step, n, of, "That's the basics",
            "From here the Next step card points at whatever matters most, and How to play in the menu explains everything else. " +
                "Good luck. I'll be reading about you in the papers.",
            "Thanks, $MENTOR", acknowledge = true,
        )
    }
}

/** Tick a lesson off. The last one ends the tutorial. */
fun GameState.completeLesson(step: TutorialStep) {
    val t = tutorial ?: return
    if (step == TutorialStep.DONE) {
        tutorial = null
        addLog("$MENTOR signs off. You're on your own now.", LogTone.MILESTONE)
        return
    }
    t.done = t.done + step
}

/** Start the lessons again, from the menu. Anything already done ticks itself off. */
fun GameState.restartTutorial() {
    tutorial = Tutorial(setOf(TutorialStep.WELCOME))
}

/** No more lessons. */
fun GameState.skipTutorial() {
    tutorial = null
}

/** Each week, tick off whatever you've done since. */
internal fun GameState.tutorialWeek() {
    val t = tutorial ?: return
    t.done = t.done + TutorialStep.entries.filter { learned(it) }
}

/** Whether the game can see you've done this lesson's thing. */
private fun GameState.learned(step: TutorialStep): Boolean = when (step) {
    TutorialStep.WELCOME, TutorialStep.MONEY, TutorialStep.DONE -> false
    TutorialStep.FIRST_WORK -> when (startingLane) {
        Lane.GAMES -> games?.let { it.project != null || it.releases.isNotEmpty() } ?: false
        Lane.WEBSITES -> web?.let { it.active.isNotEmpty() || it.completed > 0 } ?: false
        Lane.APPS -> apps?.isLive ?: false
        Lane.SOCIAL -> social?.isLive ?: false
        Lane.HARDWARE -> hardware?.let { it.design != null || it.modelsShipped > 0 } ?: false
        Lane.AI -> ai?.let { it.training != null || it.generation > 0 } ?: false
    }
    TutorialStep.PLAY -> week >= 1
    TutorialStep.HIRE -> staff.isNotEmpty()
    TutorialStep.RESEARCH -> researched.isNotEmpty()
}

/** The cheapest research you could buy now or soon, for the lesson. */
private fun GameState.cheapestResearch(): Research? = Research.entries.filter { r ->
    (r.lane == null || r.lane in lanes) && (r.kind != Research.Kind.STUDIO && r.kind != Research.Kind.ENGINE || Lane.GAMES in lanes) &&
        researchBlocker(r).let { it == null || it.startsWith("Needs ${r.rp} RP") || it.startsWith("Costs") }
}.minByOrNull { it.rp }
