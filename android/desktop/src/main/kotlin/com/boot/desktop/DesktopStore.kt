package com.boot.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.boot.core.Difficulty
import com.boot.core.GameConcept
import com.boot.core.GameException
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.core.LegacyPerk
import com.boot.core.Scenario
import com.boot.core.StartPoint
import com.boot.core.advance
import com.boot.core.decodeHall
import com.boot.core.encodeHall
import com.boot.core.runRecord
import com.boot.core.scenario
import com.boot.core.with
import java.io.File
import kotlin.random.Random

enum class Tab { OFFICE, LANES, RESEARCH, TEAM, COMPANY, NEWS }

enum class Speed(val label: String, val millis: Long) {
    NORMAL("1×", 1_100),
    FAST("2×", 500),
    FASTEST("3×", 200),
}

/**
 * Desktop owner of the running game: actions, clock, autosave.
 * Mirrors Android [com.boot.game.GameStore] without Android ViewModel / Application.
 */
class DesktopStore(dataDir: File) {
    private val saveFile = File(dataDir, "autosave.json")
    private val hallFile = File(dataDir, "hall.json")

    init {
        dataDir.mkdirs()
    }

    var hall by mutableStateOf(
        runCatching { decodeHall(hallFile.readText()) }.getOrDefault(emptyList()),
    )
        private set

    var state by mutableStateOf<GameState?>(
        runCatching { GameState.decode(saveFile.readText()) }.getOrNull()?.takeUnless { it.isOver },
    )
        private set

    private var json: String? = state?.encode()

    var error by mutableStateOf<String?>(null)
    var notice by mutableStateOf<String?>(null)

    var tab by mutableStateOf(Tab.OFFICE)
    var lane by mutableStateOf<Lane?>(state?.startingLane)

    var companyPage by mutableIntStateOf(0)

    /** Newest news story id the player has seen (unread badge). */
    var newsSeen by mutableIntStateOf(state?.news?.lastOrNull()?.id ?: 0)

    /** Game designer overlay is open. */
    var designing by mutableStateOf(false)
    var draft by mutableStateOf<GameConcept?>(null)
    var designTeam by mutableIntStateOf(1)

    var playing by mutableStateOf(false)
        private set
    var speed by mutableStateOf(Speed.NORMAL)

    /** First-run coach tip over the office after New Game / until dismissed. */
    var showCoach by mutableStateOf(false)

    val canAdvance: Boolean
        get() = state?.let { !it.isOver && !it.needsDecision && it.games?.unseenReview == null } ?: false

    val holdsClock: Boolean get() = designing

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
        begin(
            GameState.create(
                company, founder, lane, seed, difficulty,
                LegacyPerk.earned(hall), start,
                sandbox = sandbox, tutorial = tutorial,
            ),
        )
    }

    fun newScenario(scenario: Scenario, company: String, founder: String, seed: Long = Random.nextLong()) {
        begin(GameState.scenario(scenario, company, founder, seed, LegacyPerk.earned(hall)))
    }

    private fun begin(s: GameState) {
        state = s
        playing = false
        designing = false
        draft = null
        designTeam = 1
        tab = Tab.OFFICE
        lane = s.startingLane
        companyPage = 0
        newsSeen = 0
        showCoach = true
        save(s)
    }

    fun dismissCoach() {
        showCoach = false
    }

    fun abandon() {
        playing = false
        designing = false
        draft = null
        state = null
        json = null
        runCatching { saveFile.delete() }
    }

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
        state?.goalsDone?.drop(done)?.lastOrNull()?.let { notice = "Goal reached: ${it.title}" }
        state?.achievements?.keys?.firstOrNull { it !in had }?.let { notice = "Achievement: ${it.title}" }
    }

    fun play() {
        if (canAdvance && !holdsClock) playing = true
    }

    fun pause() {
        playing = false
    }

    fun togglePlay() = if (playing) pause() else play()

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
            !wasReady2 && now.games?.project2?.isReady == true ->
                "Team 2's ${now.games?.project2?.title} is ready to ship."
            wasSolvent && now.cash < 0 ->
                "You're overdrawn. ${now.difficulty.redWeeks} weeks in the red and it's over. A bank loan can buy time."
            else -> null
        }
        if (reason != null) {
            playing = false
            if (reason.isNotEmpty()) notice = reason
        }
    }

    fun designGame(seed: GameConcept? = null, team: Int = 1) {
        pause()
        draft = seed
        designTeam = team
        designing = true
    }

    fun closeDesigner() {
        designing = false
        draft = null
        designTeam = 1
    }

    fun openLane(lane: Lane) {
        this.lane = lane
        tab = Tab.LANES
    }

    fun dismissNotice() {
        notice = null
        error = null
    }

    private fun save(s: GameState) {
        s.runRecord()?.takeIf { !s.sandbox }?.let { record ->
            if (record !in hall) {
                hall = hall.with(record)
                runCatching { hallFile.writeText(encodeHall(hall)) }
            }
        }
        val encoded = s.encode()
        json = encoded
        runCatching {
            val tmp = File(saveFile.parentFile, "autosave.json.tmp")
            tmp.writeText(encoded)
            tmp.renameTo(saveFile)
        }
    }
}
