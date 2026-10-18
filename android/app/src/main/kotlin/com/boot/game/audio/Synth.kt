package com.boot.game.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

/*
 * A small synthesiser. Everything the game plays, music and sound effects alike, is drawn here
 * from oscillators, so there are no audio files to ship or license. Plain Kotlin with no Android
 * in it, so it can be tested on the JVM.
 */

/** Samples a second. Low enough to render quickly on a phone, high enough for game music. */
const val SAMPLE_RATE = 22_050

enum class Wave { SINE, TRIANGLE, SQUARE, SAW, FM, NOISE }

/** Attack, decay and release in seconds; sustain is a level from 0 to 1. */
data class Envelope(val attack: Double, val decay: Double, val sustain: Double, val release: Double) {
    /** The level [t] seconds after the note starts, for a note held [held] seconds. */
    fun level(t: Double, held: Double): Double {
        val on = when {
            t < attack -> t / attack
            t < attack + decay -> 1.0 - (1.0 - sustain) * (t - attack) / decay
            else -> sustain
        }
        if (t < held) return on
        // Release from wherever the note had got to when it was let go.
        val atRelease = when {
            held < attack -> held / attack
            held < attack + decay -> 1.0 - (1.0 - sustain) * (held - attack) / decay
            else -> sustain
        }
        val r = (t - held) / release
        return if (r >= 1.0) 0.0 else atRelease * (1.0 - r)
    }

    companion object {
        val PLUCK = Envelope(0.004, 0.18, 0.25, 0.12)
        val KEYS = Envelope(0.006, 0.6, 0.35, 0.3)
        val PAD = Envelope(0.35, 0.5, 0.75, 0.6)
        val LEAD = Envelope(0.01, 0.12, 0.7, 0.12)
        val BASS = Envelope(0.005, 0.15, 0.6, 0.06)
        val BLIP = Envelope(0.002, 0.05, 0.5, 0.04)
        val BELL = Envelope(0.002, 0.9, 0.0, 0.4)
    }
}

/** How a note sounds. */
data class Instrument(
    val wave: Wave,
    val envelope: Envelope,
    val gain: Double,
    /** One-pole low-pass amount, 0 to 1. 1 leaves the sound open; lower is darker. */
    val cutoff: Double = 1.0,
    /** Pulse width for [Wave.SQUARE]. */
    val duty: Double = 0.5,
    /** Modulator ratio and depth for [Wave.FM]. */
    val fmRatio: Double = 2.0,
    val fmIndex: Double = 1.5,
    /** A second oscillator this many cents sharp, for width. 0 for one. */
    val detune: Double = 0.0,
    /** Vibrato depth in semitones, at 5.5 Hz, easing in after a moment. */
    val vibrato: Double = 0.0,
    /** Pitch that slides into the note, in semitones (negative starts below). */
    val glide: Double = 0.0,
)

fun midiToHz(midi: Double): Double = 440.0 * 2.0.pow((midi - 69.0) / 12.0)

/** PolyBLEP: smooths the jumps in square and saw waves so high notes don't alias into noise. */
private fun polyBlep(phase: Double, dt: Double): Double = when {
    phase < dt -> {
        val t = phase / dt
        t + t - t * t - 1.0
    }
    phase > 1.0 - dt -> {
        val t = (phase - 1.0) / dt
        t * t + t + t + 1.0
    }
    else -> 0.0
}

/**
 * A mono buffer that notes and drums are mixed into. With [loop] on, anything that runs past
 * the end wraps round to the start, so the buffer plays back seamlessly on repeat.
 */
class Mix(val frames: Int, private val loop: Boolean = false, seed: Int = 1) {
    val data = FloatArray(frames)
    private val noise = Random(seed)

    private fun add(index: Int, value: Double) {
        if (index < frames) {
            data[index] += value.toFloat()
        } else if (loop) {
            data[index % frames] += value.toFloat()
        }
    }

    fun seconds(frame: Int) = frame.toDouble() / SAMPLE_RATE

    /** One note of [instrument] at [midi], starting at frame [start] and held [length] seconds. */
    fun note(instrument: Instrument, midi: Double, start: Int, length: Double, velocity: Double = 1.0) {
        val env = instrument.envelope
        val total = ((length + env.release) * SAMPLE_RATE).toInt() + 1
        val base = midiToHz(midi)
        val second = if (instrument.detune != 0.0) base * 2.0.pow(instrument.detune / 1200.0) else 0.0
        var phase = 0.0
        var phase2 = 0.25
        var modPhase = 0.0
        var filtered = 0.0
        val amp = instrument.gain * velocity
        for (i in 0 until total) {
            val t = i.toDouble() / SAMPLE_RATE
            val level = env.level(t, length)
            if (level <= 0.0 && t > length) break
            var hz = base
            if (instrument.vibrato != 0.0 && t > 0.15) {
                hz *= 2.0.pow(instrument.vibrato * min(1.0, (t - 0.15) * 4) * sin(2 * PI * 5.5 * t) / 12.0)
            }
            if (instrument.glide != 0.0) hz *= 2.0.pow(instrument.glide * exp(-t * 30) / 12.0)
            val dt = hz / SAMPLE_RATE
            var v = osc(instrument, phase, dt, modPhase, level)
            if (second > 0.0) {
                val dt2 = dt * second / base
                v = 0.5 * (v + osc(instrument, phase2, dt2, modPhase, level))
                phase2 = (phase2 + dt2) % 1.0
            }
            phase = (phase + dt) % 1.0
            modPhase = (modPhase + dt * instrument.fmRatio) % 1.0
            filtered += instrument.cutoff * (v - filtered)
            add(start + i, filtered * level * amp)
        }
    }

    private fun osc(instrument: Instrument, phase: Double, dt: Double, modPhase: Double, level: Double): Double = when (instrument.wave) {
        Wave.SINE -> sin(2 * PI * phase)
        Wave.TRIANGLE -> 1.0 - 4.0 * abs(phase - 0.5)
        Wave.SAW -> 2.0 * phase - 1.0 - polyBlep(phase, dt)
        Wave.SQUARE -> {
            val duty = instrument.duty
            var v = if (phase < duty) 1.0 else -1.0
            v += polyBlep(phase, dt)
            v -= polyBlep((phase + 1.0 - duty) % 1.0, dt)
            v
        }
        // The brightness follows the envelope, the way a struck FM bell or electric piano does.
        Wave.FM -> sin(2 * PI * phase + instrument.fmIndex * (0.3 + 0.7 * level) * sin(2 * PI * modPhase))
        Wave.NOISE -> noise.nextDouble(-1.0, 1.0)
    }

    /** A kick drum: a sine that drops in pitch, with a click on top. [chip] makes it a 8-bit thump. */
    fun kick(start: Int, velocity: Double = 1.0, chip: Boolean = false) {
        val total = (0.32 * SAMPLE_RATE).toInt()
        var phase = 0.0
        for (i in 0 until total) {
            val t = i.toDouble() / SAMPLE_RATE
            val hz = 48.0 + 110.0 * exp(-t * 28)
            phase = (phase + hz / SAMPLE_RATE) % 1.0
            val body = if (chip) 1.0 - 4.0 * abs(phase - 0.5) else sin(2 * PI * phase)
            val click = if (t < 0.004) noise.nextDouble(-0.4, 0.4) else 0.0
            add(start + i, (body * exp(-t * 11) + click) * 0.9 * velocity)
        }
    }

    /** A snare: a short burst of noise over a low tone. */
    fun snare(start: Int, velocity: Double = 1.0, tail: Double = 0.18) {
        val total = ((tail + 0.05) * SAMPLE_RATE).toInt()
        var low = 0.0
        for (i in 0 until total) {
            val t = i.toDouble() / SAMPLE_RATE
            val n = noise.nextDouble(-1.0, 1.0)
            low += 0.35 * (n - low)
            val hiss = (n - low) * exp(-t * 4.0 / tail)
            val tone = sin(2 * PI * 185.0 * t) * exp(-t * 30)
            add(start + i, (hiss * 0.55 + tone * 0.45) * 0.7 * velocity)
        }
    }

    /** A hi-hat: bright noise, short when closed and longer when open. */
    fun hat(start: Int, velocity: Double = 1.0, open: Boolean = false) {
        val decay = if (open) 0.22 else 0.035
        val total = ((decay * 3) * SAMPLE_RATE).toInt()
        var low = 0.0
        for (i in 0 until total) {
            val t = i.toDouble() / SAMPLE_RATE
            val n = noise.nextDouble(-1.0, 1.0)
            low += 0.6 * (n - low)
            add(start + i, (n - low) * exp(-t / decay) * 0.32 * velocity)
        }
    }

    /** A tom: a sine that bends down onto [hz], for fills. */
    fun tom(start: Int, hz: Double, velocity: Double = 1.0) {
        val total = (0.34 * SAMPLE_RATE).toInt()
        var phase = 0.0
        for (i in 0 until total) {
            val t = i.toDouble() / SAMPLE_RATE
            phase = (phase + hz * (1.0 + 0.6 * exp(-t * 18)) / SAMPLE_RATE) % 1.0
            val click = if (t < 0.003) noise.nextDouble(-0.3, 0.3) else 0.0
            add(start + i, (sin(2 * PI * phase) * exp(-t * 9) + click) * 0.55 * velocity)
        }
    }

    /** A crash or ride: bright noise with a metallic ring, fading over [decay] seconds. */
    fun cymbal(start: Int, velocity: Double = 1.0, decay: Double = 0.9) {
        val total = (decay * 2.5 * SAMPLE_RATE).toInt()
        var low = 0.0
        for (i in 0 until total) {
            val t = i.toDouble() / SAMPLE_RATE
            val n = noise.nextDouble(-1.0, 1.0)
            low += 0.7 * (n - low)
            val ring = 0.35 * (sin(2 * PI * 3_150.0 * t) + sin(2 * PI * 4_730.0 * t)) * exp(-t * 3 / decay)
            add(start + i, ((n - low) + ring) * exp(-t / decay) * 0.16 * velocity)
        }
    }

    /** A falling or rising sweep of noise, for whooshes. */
    fun sweep(start: Int, length: Double, velocity: Double = 1.0, rising: Boolean = true) {
        val total = (length * SAMPLE_RATE).toInt()
        var low = 0.0
        for (i in 0 until total) {
            val x = i.toDouble() / total
            val shape = if (rising) x else 1.0 - x
            val n = noise.nextDouble(-1.0, 1.0)
            low += (0.05 + 0.5 * shape) * (n - low)
            add(start + i, low * sin(PI * x) * 0.6 * velocity)
        }
    }

    /** Mixes another buffer in at [gain]. */
    fun add(other: Mix, gain: Double = 1.0) {
        for (i in 0 until min(frames, other.frames)) data[i] += (other.data[i] * gain).toFloat()
    }

    /** Echoes: each repeat [delay] seconds after the last and [feedback] as loud. Wraps round on a loop. */
    fun echo(delay: Double, feedback: Double, repeats: Int = 3, wet: Double = 0.35) {
        val dry = data.copyOf()
        val step = (delay * SAMPLE_RATE).toInt()
        var gain = wet
        for (k in 1..repeats) {
            val shift = step * k
            for (i in 0 until frames) {
                val from = i - shift
                val v = when {
                    from >= 0 -> dry[from]
                    loop -> dry[((from % frames) + frames) % frames]
                    else -> 0f
                }
                data[i] += (v * gain).toFloat()
            }
            gain *= feedback
        }
    }

    /** A gentle low-pass over the whole buffer, for warmth. */
    fun lowPass(amount: Double) {
        var y = data[frames - 1].toDouble()
        for (i in 0 until frames) {
            y += amount * (data[i] - y)
            data[i] = y.toFloat()
        }
    }

    /** Record crackle and hiss, for lo-fi. */
    fun crackle(level: Double) {
        for (i in 0 until frames) {
            var v = noise.nextDouble(-1.0, 1.0) * 0.012
            if (noise.nextDouble() < 0.0004) v += noise.nextDouble(-1.0, 1.0) * 0.5
            data[i] += (v * level).toFloat()
        }
    }

    /** 16-bit samples, levelled so the loudest point sits at [peak], with soft clipping. */
    fun toPcm(peak: Double = 0.8): ShortArray {
        var top = 0f
        for (v in data) top = max(top, abs(v))
        val scale = if (top > 0f) peak / top else 1.0
        return ShortArray(frames) { i ->
            val v = tanh(data[i] * scale * 1.1) / tanh(1.1)
            (v * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }
}
