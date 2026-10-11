package com.boot.game

import com.boot.core.with
import com.boot.core.runRecord
import com.boot.core.encodeHall
import com.boot.core.decodeHall
import com.boot.core.LegacyPerk
import com.boot.core.StartPoint
import com.boot.core.Scenario
import com.boot.core.scenario
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.boot.core.Difficulty
import com.boot.core.GameException
import com.boot.core.GameConcept
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.core.advance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random

enum class Tab { OFFICE, LANES, RESEARCH, TEAM, COMPANY, NEWS }

/** The game studio's pages: what you're making, what you've made, the market, and the tech. */
enum class StudioPage(val title: String) { MAKE("Make"), GAMES("Games"), MARKET("Market"), TECH("Tech") }

/** How fast the calendar runs while playing: milliseconds per week. */
enum class Speed(val label: String, val millis: Long) {
    NORMAL("1×", 1_100),
    FAST("2×", 500),
    FASTEST("3×", 200),
}

/** Owns the running game, routes player actions into it, runs the clock, and autosaves. */
class GameStore(app: Application) : AndroidViewModel(app) {
    private val saveFile = File(app.filesDir, "autosave.json")
    private val hallFile = File(app.filesDir, "hall.json")

    /** Every run that has ended, kept across games. */
    var hall by mutableStateOf(runCatching { decodeHall(hallFile.readText()) }.getOrDefault(emptyList()))
        private set

    /** Replaced with a fresh copy after every action, so Compose always sees the change. */
    var state by mutableStateOf<GameState?>(null)
        private set

    /** The encoding of [state], reused as the starting point of the next copy and as the save. */
    private var json: String? = null

    var error by mutableStateOf<String?>(null)

    /** Why the clock stopped by itself, shown once. */
    var notice by mutableStateOf<String?>(null)

    var tab by mutableStateOf(Tab.OFFICE)
    var lane by mutableStateOf<Lane?>(null)

    /** Company tab page: 0 overview, 1 money and board, 2 combos. */
    var companyPage by mutableIntStateOf(0)
    var studioPage by mutableStateOf(StudioPage.MAKE)

    /** The newest story the player has seen, for the unread count. */
    var newsSeen by mutableIntStateOf(0)

    /** A "next step" the player waved away; it stays hidden until a different one comes up. */
    var dismissedStep by mutableStateOf<String?>(null)

    /** The game designer is open, optionally seeded (a sequel). */
    var designing by mutableStateOf(false)
    var draft by mutableStateOf<GameConcept?>(null)

    /** How to play is open. */
    var guideOpen by mutableStateOf(false)

    /** A released game whose report is open. */
    var openRelease by mutableStateOf<Int?>(null)

    /** The calendar runs by itself while this is on, like Game Dev Tycoon. */
    var playing by mutableStateOf(false)
        private set
    var speed by mutableStateOf(Speed.NORMAL)

    /** Latest JSON to write; an empty string means delete the save. Writes happen off the main thread, in order. */
    private val pendingSave = MutableStateFlow<String?>(null)

    init {
        state = runCatching { GameState.decode(saveFile.readText()) }.getOrNull()?.takeUnless { it.isOver }
        newsSeen = state?.news?.lastOrNull()?.id ?: 0
        viewModelScope.launch(Dispatchers.IO) {
            pendingSave.filterNotNull().collect { write(it) }
        }
    }

    /** Nothing is waiting on the player: no decision, no unread reviews, no ending. */
    val canAdvance: Boolean
        get() = state?.let { !it.isOver && !it.needsDecision && it.games?.unseenReview == null } ?: false

    /** A full-screen page that holds the clock while it's open. */
    val holdsClock: Boolean get() = designing || openRelease != null || guideOpen

    fun newGame(
        company: String,
        founder: String,
        lane: Lane,
        seed: Long = Random.nextLong(),
        difficulty: Difficulty = Difficulty.NORMAL,
        start: StartPoint = StartPoint.TODAY,
        sandbox: Boolean = false,
        tutorial: Boolean = false,
    ) {
        begin(GameState.create(company, founder, lane, seed, difficulty, LegacyPerk.earned(hall), start, sandbox = sandbox, tutorial = tutorial))
    }

    /** Start one of the scenarios. */
    fun newScenario(scenario: Scenario, company: String, founder: String, seed: Long = Random.nextLong()) {
        begin(GameState.scenario(scenario, company, founder, seed, LegacyPerk.earned(hall)))
    }

    private fun begin(s: GameState) {
        val lane = s.startingLane
        state = s
        newsSeen = 0
        playing = false
        tab = Tab.OFFICE
        this.lane = lane
        save(s)
    }

    fun abandon() {
        playing = false
        state = null
        json = null
        pendingSave.value = ""
    }

    /** Run an action against a copy of the game. On failure the game is left untouched and the reason is shown. */
    fun perform(action: GameState.() -> Unit) {
        val current = state ?: return
        val next = GameState.decode(json ?: current.encode())
        try {
            next.action()
        } catch (e: GameException) {
            error = e.message
            return
        }
        state = next
        save(next)
    }

    fun advance(weeks: Int) {
        val done = state?.goalsDone?.size ?: 0
        val had = state?.achievements?.keys.orEmpty()
        perform { this.advance(weeks) }
        // Goals and achievements don't stop the clock; they just get a moment in the spotlight.
        state?.goalsDone?.drop(done)?.lastOrNull()?.let { notice = "Goal reached: ${it.title}. ${it.reward.label}" }
        state?.achievements?.keys?.firstOrNull { it !in had }?.let { notice = "Achievement unlocked: ${it.title}" }
    }

    fun play() {
        if (canAdvance && !holdsClock) playing = true
    }

    fun pause() {
        playing = false
    }

    fun togglePlay() = if (playing) pause() else play()

    /**
     * One beat of the clock: a week passes. Stops by itself when something needs the player:
     * a decision, reviews, a game ready to ship, or the bank account going red.
     */
    fun tick() {
        val before = state ?: return
        if (!canAdvance || holdsClock) {
            playing = false
            return
        }
        val wasReady = before.games?.project?.isReady == true
        val wasReady2 = before.games?.project2?.isReady == true
        val wasSolvent = before.cash >= 0
        advance(1)
        val now = state ?: return
        val project = now.games?.project
        val reason = when {
            !canAdvance -> ""
            !wasReady && project?.isReady == true -> "${project.title} is ready to ship. Release it, or keep polishing."
            !wasReady2 && now.games?.project2?.isReady == true -> "Team 2's ${now.games?.project2?.title} is ready to ship."
            wasSolvent && now.cash < 0 -> "You're overdrawn. ${now.difficulty.redWeeks} weeks in the red and it's over. A bank loan can buy time."
            else -> null
        }
        if (reason != null) {
            playing = false
            if (reason.isNotEmpty()) notice = reason
        }
    }

    /** Which team the designer is planning for. */
    var designTeam by mutableIntStateOf(1)

    fun designGame(seed: GameConcept? = null, team: Int = 1) {
        pause()
        draft = seed
        designTeam = team
        designing = true
    }

    fun openLane(lane: Lane) {
        this.lane = lane
        tab = Tab.LANES
    }

    fun openStudio(page: StudioPage) {
        studioPage = page
        openLane(Lane.GAMES)
    }

    private fun save(s: GameState) {
        // Sandbox runs stay out of the Hall of Fame.
        s.runRecord()?.takeIf { !s.sandbox }?.let { record ->
            if (record !in hall) {
                hall = hall.with(record)
                val text = encodeHall(hall)
                runCatching { hallFile.writeText(text) }
            }
        }
        val encoded = s.encode()
        json = encoded
        pendingSave.value = encoded
    }

    private fun write(json: String) {
        // Autosave is best effort; the game keeps running.
        runCatching {
            if (json.isEmpty()) {
                saveFile.delete()
            } else {
                val tmp = File(saveFile.parentFile, "autosave.json.tmp")
                tmp.writeText(json)
                tmp.renameTo(saveFile)
            }
        }
    }
}
