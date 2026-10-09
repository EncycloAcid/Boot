package com.boot.game

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

enum class Tab { HOME, LANES, RESEARCH, PEOPLE, COMPANY, NEWS }

/** Owns the running game, routes player actions into it, and autosaves. */
class GameStore(app: Application) : AndroidViewModel(app) {
    private val saveFile = File(app.filesDir, "autosave.json")

    /** Replaced with a fresh copy after every action, so Compose always sees the change. */
    var state by mutableStateOf<GameState?>(null)
        private set
    var error by mutableStateOf<String?>(null)
    var tab by mutableStateOf(Tab.HOME)
    var lane by mutableStateOf<Lane?>(null)

    /** Company tab page: 0 money and board, 1 combos. */
    var companyPage by mutableIntStateOf(0)

    /** The game designer is open, optionally seeded (a sequel). */
    var designing by mutableStateOf(false)
    var draft by mutableStateOf<GameConcept?>(null)

    /** A released game whose report is open. */
    var openRelease by mutableStateOf<Int?>(null)

    /** Latest JSON to write; an empty string means delete the save. Writes happen off the main thread, in order. */
    private val pendingSave = MutableStateFlow<String?>(null)

    init {
        state = runCatching { GameState.decode(saveFile.readText()) }.getOrNull()?.takeUnless { it.isOver }
        viewModelScope.launch(Dispatchers.IO) {
            pendingSave.filterNotNull().collect { write(it) }
        }
    }

    val canAdvance: Boolean get() = state?.let { !it.isOver && !it.needsDecision } ?: false

    fun newGame(company: String, founder: String, lane: Lane, seed: Long = Random.nextLong()) {
        val s = GameState.create(company, founder, lane, seed)
        state = s
        tab = Tab.HOME
        this.lane = lane
        save(s)
    }

    fun abandon() {
        state = null
        pendingSave.value = ""
    }

    /** Run an action against a copy of the game. On failure the game is left untouched and the reason is shown. */
    fun perform(action: GameState.() -> Unit) {
        val next = state?.copy() ?: return
        try {
            next.action()
        } catch (e: GameException) {
            error = e.message
            return
        }
        state = next
        save(next)
    }

    fun advance(weeks: Int) = perform { this.advance(weeks) }

    fun designGame(seed: GameConcept? = null) {
        draft = seed
        designing = true
    }

    fun openLane(lane: Lane) {
        this.lane = lane
        tab = Tab.LANES
    }

    private fun save(s: GameState) {
        pendingSave.value = s.encode()
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
