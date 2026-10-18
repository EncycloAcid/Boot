package com.boot.game.audio

import com.boot.core.Era
import kotlin.random.Random

/*
 * The soundtrack. Every era gets its own tune, composed from a seed in a style that suits its
 * time: chiptune for the home computers, FM synths for the console wars, house for the boom
 * years, lo-fi for the bootstrap years and so on. A track is two eight- or sixteen-bar sections
 * that loop without a seam.
 */

enum class Mode(val steps: IntArray) {
    MAJOR(intArrayOf(0, 2, 4, 5, 7, 9, 11)),
    MINOR(intArrayOf(0, 2, 3, 5, 7, 8, 10)),
    DORIAN(intArrayOf(0, 2, 3, 5, 7, 9, 10)),
}

enum class MusicStyle(val title: String, val bpm: Int, val mode: Mode, val swing: Double, val bars: Int) {
    CHIPTUNE("Chiptune", 138, Mode.MAJOR, 0.0, 32),
    FM("16-bit", 116, Mode.DORIAN, 0.0, 32),
    HOUSE("House", 122, Mode.MINOR, 0.0, 32),
    DOWNBEAT("Downbeat", 86, Mode.MINOR, 0.06, 16),
    LOFI("Lo-fi", 80, Mode.MAJOR, 0.14, 16),
    SYNTHWAVE("Synthwave", 100, Mode.MINOR, 0.0, 16),
}

/** The sound of each era. */
fun styleFor(era: Era): MusicStyle = when (era) {
    Era.HOME_COMPUTERS, Era.VIDEO_GAME_CRASH, Era.RETRO_REVIVAL -> MusicStyle.CHIPTUNE
    Era.CONSOLE_WARS, Era.MULTIMEDIA -> MusicStyle.FM
    Era.DOT_COM_BOOM, Era.WEB_2_0, Era.AI_GOLD_RUSH -> MusicStyle.HOUSE
    Era.DOT_COM_BUST, Era.CRYPTO_WINTER, Era.PRIVACY_PANIC, Era.CHIP_SHORTAGE -> MusicStyle.DOWNBEAT
    Era.BOOTSTRAP_YEARS, Era.REMOTE_WORK_BOOM, Era.CREATOR_ECONOMY, Era.FREE_TO_PLAY, Era.SMARTPHONES -> MusicStyle.LOFI
    Era.VR_HYPE_CYCLE, Era.METAVERSE_HANGOVER, Era.CLOUD_GAMING, Era.SUPER_APPS -> MusicStyle.SYNTHWAVE
}

/** Which tune to play. The same key always composes the same tune. */
data class MusicTrack(val key: String, val style: MusicStyle, val seed: Long) {
    companion object {
        /** The title screen's tune. */
        val TITLE = MusicTrack("title", MusicStyle.LOFI, 1977)

        /** The takes a track has, and the order they play in: tune, new tune, tune, breakdown. */
        const val TAKES = 3
        val ORDER = intArrayOf(0, 1, 0, 2)

        fun forEra(era: Era?): MusicTrack = if (era == null) TITLE else MusicTrack(era.name, styleFor(era), 7_919L * (era.ordinal + 3))
    }

    fun render(): ShortArray = render(0)

    /**
     * One take of the tune. Every take shares the key, chords and tempo, so they follow each other
     * without a seam, but take 1 has a fresh melody and take 2 is a breakdown with the drums
     * pulled back. Played in turn they keep a long session from hearing the same loop over and over.
     */
    fun render(take: Int): ShortArray = Composer(style, Random(seed), take).render()
}

/** A note in 16th-note steps within a two-bar phrase: when, how long, and which scale degree. */
private data class Step(val at: Int, val length: Int, val degree: Int)

private class Composer(val style: MusicStyle, val rng: Random, val take: Int = 0) {
    /** The breakdown take: no kick or snare, and the tune only in its second half. */
    val breakdown = take == 2
    val mode = style.mode.steps
    /** The key's root for chords; bass sits an octave down and the tune an octave up. */
    val root = 52 + rng.nextInt(0, 8)
    val stepFrames = SAMPLE_RATE * 60 / style.bpm / 4
    val frames = style.bars * 16 * stepFrames
    val half = style.bars / 2

    val progressionA: IntArray
    val progressionB: IntArray

    init {
        val options = when (style.mode) {
            Mode.MAJOR -> listOf(intArrayOf(0, 4, 5, 3), intArrayOf(0, 5, 3, 4), intArrayOf(5, 3, 0, 4), intArrayOf(0, 3, 5, 4), intArrayOf(3, 4, 2, 5))
            Mode.MINOR -> listOf(intArrayOf(0, 5, 2, 6), intArrayOf(0, 3, 6, 2), intArrayOf(0, 6, 5, 6), intArrayOf(0, 5, 3, 4), intArrayOf(5, 6, 0, 0))
            Mode.DORIAN -> listOf(intArrayOf(0, 3, 0, 3), intArrayOf(0, 6, 3, 0), intArrayOf(0, 3, 6, 4), intArrayOf(0, 2, 3, 4))
        }
        val shuffled = options.shuffled(rng)
        progressionA = shuffled[0]
        progressionB = shuffled[1]
    }

    /** Melodies draw from their own dice, so each take can sing a different tune over the same chords. */
    val tune: Random = if (take == 1) Random(rng.nextLong() xor 0x5EED) else rng

    /** MIDI note for a scale degree above [base]; degrees past 6 climb into the next octave. */
    fun pitch(degree: Int, base: Int = root): Double {
        val octave = Math.floorDiv(degree, 7)
        return (base + mode[Math.floorMod(degree, 7)] + 12 * octave).toDouble()
    }

    fun chordAt(bar: Int): Int {
        val inSection = bar % half
        return (if (bar < half) progressionA else progressionB)[inSection % 4]
    }

    /** Frame where a 16th step of a bar starts, with swing pushing the off-beat eighths late. */
    fun frameAt(bar: Int, step: Int): Int {
        val swung = if (style.swing > 0 && step % 4 == 2) (style.swing * 2 * stepFrames).toInt() else 0
        return (bar * 16 + step) * stepFrames + swung
    }

    fun seconds(steps: Double) = steps * stepFrames / SAMPLE_RATE

    fun render(): ShortArray {
        val drums = Mix(frames, loop = true, seed = rng.nextInt())
        val bass = Mix(frames, loop = true, seed = rng.nextInt())
        val chords = Mix(frames, loop = true, seed = rng.nextInt())
        val lead = Mix(frames, loop = true, seed = rng.nextInt())
        drums(drums)
        bass(bass)
        chords(chords)
        melody(lead)
        val master = Mix(frames, loop = true, seed = rng.nextInt())
        val beat = 4 * seconds(1.0)
        when (style) {
            MusicStyle.CHIPTUNE -> lead.echo(seconds(3.0), 0.3, repeats = 1, wet = 0.18)
            MusicStyle.FM -> lead.echo(seconds(3.0), 0.35, repeats = 2, wet = 0.22)
            MusicStyle.HOUSE -> {
                lead.echo(seconds(3.0), 0.45, wet = 0.35)
                chords.echo(seconds(3.0), 0.3, repeats = 2, wet = 0.2)
            }
            MusicStyle.DOWNBEAT -> lead.echo(seconds(6.0), 0.45, wet = 0.4)
            MusicStyle.LOFI -> lead.echo(seconds(3.0), 0.35, repeats = 2, wet = 0.25)
            MusicStyle.SYNTHWAVE -> {
                lead.echo(seconds(3.0), 0.5, wet = 0.4)
                chords.echo(beat * 0.75, 0.4, repeats = 2, wet = 0.25)
            }
        }
        if (breakdown) {
            lead.lowPass(0.5)
            chords.echo(seconds(6.0), 0.4, repeats = 2, wet = 0.25)
        }
        master.add(drums, if (style == MusicStyle.LOFI || style == MusicStyle.DOWNBEAT) 0.7 else 0.85)
        master.add(bass, 0.9)
        master.add(chords, 0.8)
        master.add(lead, 0.9)
        when (style) {
            MusicStyle.LOFI -> {
                master.lowPass(0.42)
                master.crackle(1.0)
            }
            MusicStyle.DOWNBEAT -> master.lowPass(0.6)
            else -> Unit
        }
        return master.toPcm(0.72)
    }

    // Drums

    private fun pattern(p: String) = p.mapIndexedNotNull { i, c -> if (c == 'x') i else null }

    private fun drums(mix: Mix) {
        val (kick, snare, hat, open) = when (style) {
            MusicStyle.CHIPTUNE -> listOf("x.......x.x.....", "....x.......x...", "x.x.x.x.x.x.x.x.", "")
            MusicStyle.FM -> listOf("x.....x...x.....", "....x.......x..x", "x.x.x.x.x.x.x.x.", "")
            MusicStyle.HOUSE -> listOf("x...x...x...x...", "....x.......x...", ".x.x.x.x.x.x.x.x", "..x...x...x...x.")
            MusicStyle.DOWNBEAT -> listOf("x.........x.....", "........x.......", "x...x...x...x...", "")
            MusicStyle.LOFI -> listOf("x......x..x.....", "....x.......x...", "x.x.x.x.x.x.x.x.", "")
            MusicStyle.SYNTHWAVE -> listOf("x.......x.......", "....x.......x...", "x.x.x.x.x.x.x.x.", "")
        }.map(::pattern)
        val chip = style == MusicStyle.CHIPTUNE
        val tail = if (style == MusicStyle.SYNTHWAVE) 0.32 else if (chip) 0.1 else 0.18
        for (bar in 0 until style.bars) {
            // A fill to round off every eight bars.
            val fill = bar % 8 == 7 && style in setOf(MusicStyle.CHIPTUNE, MusicStyle.FM, MusicStyle.SYNTHWAVE, MusicStyle.HOUSE)
            if (breakdown) {
                // Just a soft pulse, so the band can breathe before the tune comes back.
                if (bar >= half) for (s in kick) mix.kick(frameAt(bar, s), 0.5, chip)
                for (s in hat) mix.hat(frameAt(bar, s), if (s % 4 == 0) 0.4 else 0.22)
                continue
            }
            for (s in kick) mix.kick(frameAt(bar, s), if (style == MusicStyle.LOFI) 0.8 else 1.0, chip)
            for (s in snare) mix.snare(frameAt(bar, s), if (style == MusicStyle.DOWNBEAT) 0.7 else 0.9, tail)
            if (fill) for (s in listOf(13, 14, 15)) mix.snare(frameAt(bar, s), 0.45 + 0.15 * (s - 13), tail * 0.6)
            for (s in hat) mix.hat(frameAt(bar, s), if (s % 4 == 0) 0.75 else 0.45)
            for (s in open) mix.hat(frameAt(bar, s), 0.5, open = true)
        }
    }

    // Bass

    private fun bass(mix: Mix) {
        val base = root - 12
        val inst = when (style) {
            MusicStyle.CHIPTUNE -> Instrument(Wave.TRIANGLE, Envelope.BASS, 0.6)
            MusicStyle.FM -> Instrument(Wave.FM, Envelope.BASS, 0.5, cutoff = 0.5, fmRatio = 1.0, fmIndex = 3.0)
            MusicStyle.HOUSE -> Instrument(Wave.SAW, Envelope.BASS, 0.55, cutoff = 0.16)
            MusicStyle.DOWNBEAT, MusicStyle.LOFI -> Instrument(Wave.SINE, Envelope.BASS, 0.6)
            MusicStyle.SYNTHWAVE -> Instrument(Wave.SAW, Envelope(0.004, 0.1, 0.4, 0.05), 0.42, cutoff = 0.2)
        }
        // (step, length in steps, 0 for the root or 4 for the fifth, octave up)
        val line: List<IntArray> = when (style) {
            MusicStyle.CHIPTUNE -> (0 until 8).map { intArrayOf(it * 2, 2, 0, it % 2) }
            MusicStyle.FM -> listOf(intArrayOf(0, 3, 0, 0), intArrayOf(3, 1, 0, 1), intArrayOf(6, 2, 0, 0), intArrayOf(8, 2, 0, 0), intArrayOf(10, 2, 4, 0), intArrayOf(12, 2, 0, 1), intArrayOf(14, 2, 0, 0))
            MusicStyle.HOUSE -> listOf(2, 6, 10, 14).map { intArrayOf(it, 2, 0, 0) }
            MusicStyle.DOWNBEAT -> listOf(intArrayOf(0, 6, 0, 0), intArrayOf(8, 4, 4, 0), intArrayOf(14, 2, 0, 0))
            MusicStyle.LOFI -> listOf(intArrayOf(0, 7, 0, 0), intArrayOf(10, 5, 4, 0))
            MusicStyle.SYNTHWAVE -> (0 until 8).map { intArrayOf(it * 2, 2, 0, 0) }
        }
        for (bar in 0 until style.bars) {
            val chord = chordAt(bar)
            for ((step, length, interval, up) in line) {
                val midi = pitch(chord + interval, base) + 12 * up
                mix.note(inst, midi, frameAt(bar, step), seconds(length * 0.9))
            }
        }
    }

    // Chords

    private fun chordTones(chord: Int, sevenths: Boolean) = if (sevenths) listOf(0, 2, 4, 6).map { chord + it } else listOf(0, 2, 4).map { chord + it }

    private fun chords(mix: Mix) {
        for (bar in 0 until style.bars) {
            val chord = chordAt(bar)
            when (style) {
                MusicStyle.CHIPTUNE -> {
                    // The classic arpeggio: chord tones rattling past in 16ths.
                    val inst = Instrument(Wave.SQUARE, Envelope.BLIP, 0.16, duty = 0.125)
                    val tones = chordTones(chord, false) + (chord + 7)
                    for (s in 0 until 16) mix.note(inst, pitch(tones[s % tones.size], root + 12), frameAt(bar, s), seconds(0.8))
                }
                MusicStyle.FM -> {
                    val inst = Instrument(Wave.FM, Envelope.KEYS, 0.14, fmRatio = 2.0, fmIndex = 1.3)
                    for ((s, l) in listOf(0 to 3, 6 to 2, 10 to 4)) {
                        for (d in chordTones(chord, false)) mix.note(inst, pitch(d), frameAt(bar, s), seconds(l.toDouble()))
                    }
                }
                MusicStyle.HOUSE -> {
                    val inst = Instrument(Wave.SAW, Envelope.PLUCK, 0.13, cutoff = 0.22, detune = 12.0)
                    for (s in listOf(2, 6, 10, 14)) {
                        for (d in chordTones(chord, true)) mix.note(inst, pitch(d), frameAt(bar, s), seconds(1.5))
                    }
                }
                MusicStyle.DOWNBEAT -> {
                    val pad = Instrument(Wave.TRIANGLE, Envelope.PAD, 0.12, detune = 8.0)
                    for (d in chordTones(chord, false)) mix.note(pad, pitch(d), frameAt(bar, 0), seconds(15.0))
                    val keys = Instrument(Wave.FM, Envelope.KEYS, 0.12, fmRatio = 1.0, fmIndex = 0.8)
                    val tones = chordTones(chord, true)
                    for ((i, s) in listOf(0, 4, 8, 12).withIndex()) mix.note(keys, pitch(tones[i % tones.size], root + 12), frameAt(bar, s), seconds(3.0))
                }
                MusicStyle.LOFI -> {
                    // An electric piano with a little tape wobble, on the one and pushed ahead of three.
                    val keys = Instrument(Wave.FM, Envelope.KEYS, 0.13, cutoff = 0.55, fmRatio = 1.0, fmIndex = 0.9, vibrato = 0.06)
                    for ((s, l) in listOf(0 to 6, 10 to 5)) {
                        for (d in chordTones(chord, true)) mix.note(keys, pitch(d), frameAt(bar, s), seconds(l.toDouble()))
                    }
                }
                MusicStyle.SYNTHWAVE -> {
                    val pad = Instrument(Wave.SAW, Envelope.PAD, 0.09, cutoff = 0.1, detune = 15.0)
                    for (d in chordTones(chord, false)) mix.note(pad, pitch(d), frameAt(bar, 0), seconds(15.5))
                    val arp = Instrument(Wave.SQUARE, Envelope.PLUCK, 0.07, cutoff = 0.3)
                    val tones = listOf(chord, chord + 4, chord + 7, chord + 2)
                    for (s in 0 until 16) mix.note(arp, pitch(tones[s % tones.size], root + 12), frameAt(bar, s), seconds(0.9))
                }
            }
        }
    }

    // Melody

    private val rhythms: List<List<Pair<Int, Int>>> = when (style) {
        MusicStyle.CHIPTUNE, MusicStyle.FM -> listOf(
            listOf(0 to 2, 2 to 2, 4 to 4, 8 to 2, 10 to 2, 12 to 4, 16 to 2, 18 to 2, 20 to 2, 22 to 2, 24 to 8),
            listOf(0 to 4, 4 to 2, 6 to 2, 8 to 4, 12 to 4, 16 to 6, 22 to 2, 24 to 8),
            listOf(0 to 2, 2 to 2, 4 to 2, 6 to 2, 8 to 6, 14 to 2, 16 to 4, 20 to 4, 24 to 4, 28 to 4),
            listOf(2 to 2, 4 to 2, 6 to 2, 8 to 8, 18 to 2, 20 to 2, 22 to 2, 24 to 8),
        )
        MusicStyle.HOUSE, MusicStyle.SYNTHWAVE -> listOf(
            listOf(0 to 4, 4 to 2, 6 to 2, 8 to 4, 12 to 4, 16 to 6, 22 to 2, 24 to 8),
            listOf(0 to 6, 6 to 2, 8 to 6, 14 to 2, 16 to 4, 20 to 4, 24 to 8),
            listOf(2 to 2, 4 to 4, 10 to 2, 12 to 4, 18 to 2, 20 to 4, 24 to 8),
        )
        MusicStyle.DOWNBEAT, MusicStyle.LOFI -> listOf(
            listOf(0 to 8, 8 to 8, 16 to 12, 28 to 4),
            listOf(4 to 4, 8 to 4, 12 to 4, 16 to 16),
            listOf(2 to 2, 4 to 6, 10 to 6, 18 to 2, 20 to 12),
        )
    }

    /** A two-bar idea starting from [start], fitted to the chords at [bar]. */
    private fun motif(bar: Int, start: Int): List<Step> {
        val rhythm = rhythms[tune.nextInt(rhythms.size)]
        var degree = start
        return rhythm.map { (at, length) ->
            val chord = chordAt(bar + at / 16)
            if (at % 8 == 0) {
                // On the strong beats, land on a chord tone near where the tune already is.
                val tones = (-1..2).flatMap { o -> chordTones(chord, false).map { it + 7 * o } }.filter { it in 3..13 }
                degree = tones.minByOrNull { kotlin.math.abs(it - degree) + tune.nextDouble() * 1.5 } ?: degree
            } else {
                degree = (degree + listOf(-2, -1, -1, 1, 1, 2).random(tune)).coerceIn(3, 13)
            }
            Step(at, length, degree)
        }
    }

    /** The same idea moved to fit the chord a couple of bars on, the way a tune answers itself. */
    private fun answer(m: List<Step>, from: Int, to: Int): List<Step> {
        val shift = chordAt(to) - chordAt(from)
        return m.map { it.copy(degree = (it.degree + shift).coerceIn(3, 14)) }
    }

    private fun melody(mix: Mix) {
        val inst = when (style) {
            MusicStyle.CHIPTUNE -> Instrument(Wave.SQUARE, Envelope.LEAD, 0.26, duty = 0.25, vibrato = 0.15)
            MusicStyle.FM -> Instrument(Wave.FM, Envelope.LEAD, 0.28, fmRatio = 1.0, fmIndex = 2.2, vibrato = 0.12)
            MusicStyle.HOUSE -> Instrument(Wave.SQUARE, Envelope.PLUCK, 0.22, cutoff = 0.35)
            MusicStyle.DOWNBEAT -> Instrument(Wave.SINE, Envelope.LEAD, 0.26, cutoff = 0.6, vibrato = 0.2)
            MusicStyle.LOFI -> Instrument(Wave.TRIANGLE, Envelope.LEAD, 0.24, cutoff = 0.45, vibrato = 0.08)
            MusicStyle.SYNTHWAVE -> Instrument(Wave.SAW, Envelope.LEAD, 0.2, cutoff = 0.3, detune = 10.0, vibrato = 0.25)
        }
        // Calmer styles leave the first section to the band and bring the tune in for the second.
        val quietFirst = style in setOf(MusicStyle.HOUSE, MusicStyle.DOWNBEAT, MusicStyle.LOFI)
        var bar = 0
        while (bar < style.bars) {
            val sectionStart = bar
            val sectionEnd = bar + half
            // Each eight bars: an idea, its answer, something new, and the idea again to come home.
            val phrases = mutableListOf<Pair<Int, List<Step>>>()
            var b = sectionStart
            while (b < sectionEnd) {
                val m = motif(b, 7 + tune.nextInt(-1, 3))
                val n = motif(b + 4, 9)
                phrases += b to m
                phrases += (b + 2) to answer(m, b, b + 2)
                phrases += (b + 4) to n
                phrases += (b + 6) to answer(m, b, b + 6).let { last -> last.dropLast(1) + last.last().copy(degree = 7) }
                b += 8
            }
            val silent = (quietFirst || breakdown) && sectionStart == 0
            if (!silent) {
                for ((at, steps) in phrases) {
                    for (s in steps) {
                        val noteBar = at + s.at / 16
                        mix.note(inst, pitch(s.degree), frameAt(noteBar, s.at % 16), seconds(s.length * 0.92))
                    }
                }
            }
            bar = sectionEnd
        }
    }
}
