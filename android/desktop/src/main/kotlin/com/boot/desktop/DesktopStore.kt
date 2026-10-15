package com.boot.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.boot.core.Difficulty
import com.boot.core.GameException
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.core.LegacyPerk
import com.boot.core.StartPoint
import com.boot.core.advance
import com.boot.core.decodeHall
import com.boot.core.encodeHall
import com.boot.core.runRecord
import com.boot.core.with
import java.io.File
import kotlin.random.Random

enum class Tab { OFFICE, LANES, RESEARCH, TEAM, COMPANY }

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

    var playing by mutableStateOf(false)
        private set
    var speed by mutableStateOf(Speed.NORMAL)

    val canAdvance: Boolean
        get() = state?.let { !it.isOver && !it.needsDecision && it.games?.unseenReview == null } ?: false

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

    private fun begin(s: GameState) {
        state = s
        playing = false
        tab = Tab.OFFICE
        lane = s.startingLane
        save(s)
    }

    fun abandon() {
        playing = false
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
        if (canAdvance) playing = true
    }

    fun pause() {
        playing = false
    }

    fun togglePlay() = if (playing) pause() else play()

    fun tick() {
        val before = state ?: return
        if (!canAdvance) {
            playing = false
            return
        }
        val wasReady = before.games?.project?.isReady == true
        val wasSolvent = before.cash >= 0
        advance(1)
        val now = state ?: return
        val project = now.games?.project
        val reason = when {
            !canAdvance -> ""
            !wasReady && project?.isReady == true -> "${project.title} is ready to ship."
            wasSolvent && now.cash < 0 -> "You're overdrawn."
            else -> null
        }
        if (reason != null) {
            playing = false
            if (reason.isNotEmpty()) notice = reason
        }
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
