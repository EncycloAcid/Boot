package com.boot.game.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/** What the UI needs from the sound system. The default plays nothing, for previews and tests. */
interface SoundOut {
    fun play(sfx: Sfx) {}

    fun review(score: Int) {}
}

object Silent : SoundOut

/**
 * Plays the game's music and sound effects, and keeps the volume settings. The music for the
 * current era loops in the background with a fade between tracks; effects play over the top.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SoundBoard(context: Context, private val settings: SharedPreferences) : SoundOut {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    /** Robolectric has no speakers, and a music thread there would only spin. */
    private val enabled = Build.FINGERPRINT != "robolectric"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Music is started, switched and stopped one step at a time, in order. */
    private val musicLane = Dispatchers.Default.limitedParallelism(1)

    /** 0 to 1. Off at 0. */
    var musicVolume by mutableFloatStateOf(settings.getFloat("music", 0.55f))
        private set

    var effectsVolume by mutableFloatStateOf(settings.getFloat("effects", 0.8f))
        private set

    fun setMusic(volume: Float) {
        musicVolume = volume.coerceIn(0f, 1f)
        settings.edit().putFloat("music", musicVolume).apply()
        scope.launch(musicLane) { applyMusic() }
    }

    /** Silences everything, remembering the levels for [unmute]. */
    fun mute() {
        if (musicVolume > 0f) settings.edit().putFloat("musicBeforeMute", musicVolume).apply()
        if (effectsVolume > 0f) settings.edit().putFloat("effectsBeforeMute", effectsVolume).apply()
        setMusic(0f)
        setEffects(0f)
    }

    fun unmute() {
        setMusic(settings.getFloat("musicBeforeMute", 0.55f))
        setEffects(settings.getFloat("effectsBeforeMute", 0.8f))
    }

    fun setEffects(volume: Float) {
        effectsVolume = volume.coerceIn(0f, 1f)
        settings.edit().putFloat("effects", effectsVolume).apply()
    }

    // Music

    private var wanted: MusicTrack? = null
    private var loop: MusicLoop? = null
    private var foreground = true

    /** If another app was already playing music when ours would start, stay out of its way. */
    private var otherMusic = false
    private val rendered = ConcurrentHashMap<String, Array<ShortArray?>>()

    /** Plays [track] from now on, fading from whatever was playing. */
    fun music(track: MusicTrack) {
        scope.launch(musicLane) {
            if (wanted == track) return@launch
            wanted = track
            applyMusic()
        }
    }

    /** The app went to the background or came back. */
    fun foreground(on: Boolean) {
        scope.launch(musicLane) {
            foreground = on
            if (on) otherMusic = loop == null && audio?.isMusicActive == true
            applyMusic()
        }
    }

    /** Brings the playing loop in line with what's wanted. Runs on [musicLane] only. */
    private suspend fun applyMusic() {
        if (!enabled) return
        val track = wanted
        val audible = track != null && foreground && musicVolume > 0f && !otherMusic
        val current = loop
        if (!audible) {
            loop = null
            current?.fadeOutAndStop()
            return
        }
        if (current != null && current.key == track!!.key) {
            current.volume = gain(musicVolume)
            return
        }
        // Compose the new tune first, while the old one keeps playing, so there's no gap.
        val first = takes(track!!)[0] ?: return
        // The world may have moved on while that was composing.
        if (wanted != track || !foreground) return
        val next = runCatching { MusicLoop(track.key, first).also { it.start(gain(musicVolume)) } }.getOrNull() ?: return
        loop = next
        // A true crossfade: the old tune bows out while the new one swells in.
        current?.let { old -> scope.launch { old.fadeOutAndStop(1800) } }
        // The other takes are composed in the background and join the rotation once they're ready.
        scope.launch {
            for (take in 1 until MusicTrack.TAKES) {
                if (wanted != track) return@launch
                val pcm = takes(track).let { slots -> slots[take] ?: track.render(take).also { slots[take] = it } }
                next.ready(take, pcm)
            }
        }
    }

    /** The cached takes of a track; slot 0 is always filled. Keeps only the last couple of tracks. */
    private fun takes(track: MusicTrack): Array<ShortArray?> = rendered.getOrPut(track.key) {
        if (rendered.size >= 2) rendered.clear()
        arrayOfNulls<ShortArray>(MusicTrack.TAKES).also { it[0] = track.render(0) }
    }

    // Effects

    private val effects = ConcurrentHashMap<Sfx, ShortArray>()
    private val reviews = ConcurrentHashMap<Int, ShortArray>()
    private val playingNow = AtomicInteger(0)

    override fun play(sfx: Sfx) {
        if (!enabled || effectsVolume <= 0f || !foreground) return
        scope.launch {
            val pcm = effects.getOrPut(sfx) { sfx.render() }
            shoot(pcm, gain(effectsVolume) * loudness(sfx))
        }
    }

    override fun review(score: Int) {
        if (!enabled || effectsVolume <= 0f || !foreground) return
        scope.launch {
            val pcm = reviews.getOrPut(score) { Sfx.review(score) }
            shoot(pcm, gain(effectsVolume) * 0.8f)
        }
    }

    /** Warms the effects up so the first of each plays on time. */
    fun prepare() {
        if (!enabled) return
        scope.launch { for (sfx in Sfx.entries) effects.getOrPut(sfx) { sfx.render() } }
    }

    private suspend fun shoot(pcm: ShortArray, volume: Float) {
        // A handful at once is plenty; past that the mix turns to mush.
        if (playingNow.incrementAndGet() > 4) {
            playingNow.decrementAndGet()
            return
        }
        try {
            val track = runCatching { buildTrack(AudioTrack.MODE_STATIC, pcm.size * 2) }.getOrNull() ?: return
            try {
                track.write(pcm, 0, pcm.size)
                track.setVolume(volume)
                track.play()
                delay(pcm.size * 1000L / SAMPLE_RATE + 120)
                track.stop()
            } finally {
                track.release()
            }
        } finally {
            playingNow.decrementAndGet()
        }
    }

    fun release() {
        loop?.stopNow()
        loop = null
        scope.cancel()
    }

    private fun loudness(sfx: Sfx): Float = when (sfx) {
        Sfx.CLICK -> 0.22f
        Sfx.COIN, Sfx.HIRE -> 0.6f
        Sfx.WARNING, Sfx.READY, Sfx.RESEARCH -> 0.75f
        else -> 0.9f
    }

    /** Sliders move in even steps to the ear, not to the meter. */
    private fun gain(v: Float) = v * v

    private inner class MusicLoop(val key: String, first: ShortArray) {
        /** Each take as it becomes ready; the loop plays them in [MusicTrack.ORDER], skipping any not yet composed. */
        private val takes = arrayOfNulls<ShortArray>(MusicTrack.TAKES).also { it[0] = first }

        fun ready(take: Int, pcm: ShortArray) {
            takes[take] = pcm
        }

        private val track = buildTrack(
            AudioTrack.MODE_STREAM,
            maxOf(AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT) * 2, SAMPLE_RATE / 2),
        )

        @Volatile private var running = true

        /** Where the volume is now, fading or not. */
        @Volatile private var level = 0f
        private var fader: Job? = null
        private var writer: Thread? = null

        /** Jumps straight to [value], for the settings slider. */
        var volume: Float
            get() = level
            set(value) {
                fader?.cancel()
                setLevel(value)
            }

        private fun setLevel(value: Float) {
            level = value
            runCatching { track.setVolume(value) }
        }

        fun start(target: Float) {
            setLevel(0f)
            track.play()
            writer = Thread({
                var at = 0
                var turn = 0
                var pcm = takes[0]!!
                val chunk = SAMPLE_RATE / 10
                while (running) {
                    val wrote = track.write(pcm, at, minOf(chunk, pcm.size - at))
                    if (wrote < 0) break
                    at += wrote
                    if (at >= pcm.size) {
                        at = 0
                        turn = (turn + 1) % MusicTrack.ORDER.size
                        pcm = takes[MusicTrack.ORDER[turn]] ?: takes[0]!!
                    }
                }
            }, "boot-music").apply {
                isDaemon = true
                start()
            }
            fade(target, 2000)
        }

        private fun fade(to: Float, millis: Long): Job {
            fader?.cancel()
            val from = level
            return scope.launch {
                val steps = 30
                for (i in 1..steps) {
                    setLevel(from + (to - from) * i / steps)
                    delay(millis / steps)
                }
            }.also { fader = it }
        }

        suspend fun fadeOutAndStop(millis: Long = 900) {
            fade(0f, millis).join()
            stopNow()
        }

        fun stopNow() {
            running = false
            fader?.cancel()
            runCatching { track.pause() }
            // Emptying the buffer lets a write that's waiting for room return.
            runCatching { track.flush() }
            writer?.join(500)
            runCatching { track.stop() }
            track.release()
        }
    }

    private fun buildTrack(mode: Int, bytes: Int): AudioTrack = AudioTrack.Builder()
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
        )
        .setAudioFormat(
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
        )
        .setTransferMode(mode)
        .setBufferSizeInBytes(bytes)
        .build()
}
