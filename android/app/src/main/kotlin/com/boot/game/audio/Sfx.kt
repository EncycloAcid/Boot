package com.boot.game.audio

/** Every sound effect in the game. */
enum class Sfx {
    /** A tab or button. Quiet, so it never gets tiring. */
    CLICK,

    /** Money spent or made by your own hand. */
    COIN,
    HIRE,
    RESEARCH,

    /** Something needs a decision. */
    ALERT,

    /** A game goes out of the door. */
    LAUNCH,

    /** The reviews landed well, or badly. */
    HIT,
    FLOP,
    ACHIEVEMENT,

    /** A bigger office. */
    LEVEL_UP,

    /** A game is ready to ship. */
    READY,

    /** The bank account went red. */
    WARNING,

    /** A new era begins. */
    ERA,
    ENDING_GOOD,
    ENDING_BAD,
    ;

    fun render(): ShortArray = when (this) {
        CLICK -> sound(0.06) { note(Instrument(Wave.SINE, Envelope(0.001, 0.03, 0.0, 0.02), 0.5), 88.0, 0, 0.02) }
        COIN -> sound(0.45) {
            val coin = Instrument(Wave.SQUARE, Envelope(0.002, 0.08, 0.6, 0.15), 0.3, duty = 0.5, cutoff = 0.6)
            note(coin, 83.0, 0, 0.06)
            note(coin, 88.0, at(0.06), 0.22)
        }
        HIRE -> sound(0.6) {
            val pop = Instrument(Wave.SINE, Envelope(0.002, 0.12, 0.3, 0.12), 0.5, glide = -7.0)
            note(pop, 72.0, 0, 0.1)
            note(Instrument(Wave.FM, Envelope.BELL, 0.3, fmRatio = 3.5, fmIndex = 1.2), 84.0, at(0.08), 0.3)
        }
        RESEARCH -> sound(1.1) {
            val sparkle = Instrument(Wave.TRIANGLE, Envelope(0.002, 0.15, 0.2, 0.2), 0.35)
            listOf(72, 76, 79, 84, 88).forEachIndexed { i, m -> note(sparkle, m.toDouble(), at(0.06 * i), 0.12) }
            echo(0.11, 0.4, repeats = 3, wet = 0.4)
        }
        ALERT -> sound(1.0) {
            val bell = Instrument(Wave.FM, Envelope.BELL, 0.45, fmRatio = 3.5, fmIndex = 1.6)
            note(bell, 79.0, 0, 0.3)
            note(bell, 86.0, at(0.16), 0.5)
        }
        LAUNCH -> sound(2.6) {
            // A short brass fanfare over a snare roll.
            val brass = Instrument(Wave.SAW, Envelope(0.03, 0.2, 0.75, 0.35), 0.24, cutoff = 0.35, detune = 8.0, vibrato = 0.12)
            listOf(60 to 0.0, 64 to 0.14, 67 to 0.28).forEach { (m, t) -> note(brass, m.toDouble(), at(t), 0.12) }
            for (m in listOf(60, 64, 67, 72)) note(brass, m.toDouble(), at(0.42), 1.3)
            for (i in 0 until 6) snare(at(0.42 * i / 6), 0.3 + 0.08 * i, 0.08)
            snare(at(0.42), 0.9, 0.3)
            kick(at(0.42))
            note(Instrument(Wave.FM, Envelope.BELL, 0.25, fmRatio = 3.5, fmIndex = 1.0), 84.0, at(0.42), 0.6)
            echo(0.18, 0.3, repeats = 2, wet = 0.25)
        }
        HIT -> sound(1.8) {
            val lead = Instrument(Wave.SQUARE, Envelope(0.005, 0.1, 0.6, 0.25), 0.22, duty = 0.25, cutoff = 0.5)
            listOf(72, 76, 79, 84).forEachIndexed { i, m -> note(lead, m.toDouble(), at(0.09 * i), if (i == 3) 0.7 else 0.08) }
            val pad = Instrument(Wave.SAW, Envelope(0.05, 0.3, 0.6, 0.5), 0.12, cutoff = 0.25, detune = 12.0)
            for (m in listOf(60, 64, 67)) note(pad, m.toDouble(), at(0.27), 0.8)
            // A crowd-like swell of noise behind it.
            sweep(at(0.2), 1.2, 0.5, rising = false)
            echo(0.15, 0.35, repeats = 2, wet = 0.3)
        }
        FLOP -> sound(2.2) {
            // The sad trombone: three steps down and a long wobbly last note.
            val bone = Instrument(Wave.SAW, Envelope(0.04, 0.2, 0.8, 0.25), 0.3, cutoff = 0.12, vibrato = 0.0)
            listOf(58 to 0.0, 57 to 0.32, 56 to 0.64).forEach { (m, t) -> note(bone, m.toDouble(), at(t), 0.26) }
            note(bone.copy(vibrato = 0.5), 55.0, at(0.96), 0.9)
        }
        ACHIEVEMENT -> sound(1.4) {
            val bell = Instrument(Wave.FM, Envelope.BELL, 0.35, fmRatio = 3.0, fmIndex = 1.3)
            listOf(79, 84, 88, 91).forEachIndexed { i, m -> note(bell, m.toDouble(), at(0.08 * i), 0.4) }
            echo(0.14, 0.35, repeats = 2, wet = 0.3)
        }
        LEVEL_UP -> sound(1.8) {
            val lead = Instrument(Wave.SQUARE, Envelope(0.004, 0.1, 0.6, 0.2), 0.2, duty = 0.5, cutoff = 0.5)
            listOf(60, 64, 67, 72, 76, 79, 84).forEachIndexed { i, m -> note(lead, m.toDouble(), at(0.07 * i), 0.07) }
            val chord = Instrument(Wave.FM, Envelope.BELL, 0.25, fmRatio = 2.0, fmIndex = 1.0)
            for (m in listOf(72, 76, 79, 84)) note(chord, m.toDouble(), at(0.5), 0.8)
            echo(0.16, 0.35, repeats = 2, wet = 0.25)
        }
        READY -> sound(1.1) {
            val bell = Instrument(Wave.FM, Envelope.BELL, 0.4, fmRatio = 3.5, fmIndex = 1.0)
            note(bell, 76.0, 0, 0.3)
            note(bell, 83.0, at(0.12), 0.5)
            echo(0.15, 0.3, repeats = 2, wet = 0.25)
        }
        WARNING -> sound(0.9) {
            val buzz = Instrument(Wave.SQUARE, Envelope(0.005, 0.05, 0.8, 0.06), 0.2, cutoff = 0.25)
            note(buzz, 50.0, 0, 0.18)
            note(buzz, 46.0, at(0.24), 0.4)
        }
        ERA -> sound(2.6) {
            sweep(0, 1.2, 0.7, rising = true)
            val pad = Instrument(Wave.SAW, Envelope(0.3, 0.4, 0.7, 0.8), 0.13, cutoff = 0.18, detune = 14.0)
            for (m in listOf(57, 64, 69, 72, 76)) note(pad, m.toDouble(), at(0.9), 1.0)
            note(Instrument(Wave.FM, Envelope.BELL, 0.3, fmRatio = 3.5, fmIndex = 1.2), 81.0, at(0.9), 0.8)
            echo(0.22, 0.4, repeats = 3, wet = 0.3)
        }
        ENDING_GOOD -> sound(3.4) {
            val brass = Instrument(Wave.SAW, Envelope(0.03, 0.25, 0.75, 0.6), 0.2, cutoff = 0.33, detune = 9.0, vibrato = 0.15)
            listOf(listOf(60, 64, 67) to 0.0, listOf(65, 69, 72) to 0.5, listOf(67, 71, 74) to 1.0, listOf(72, 76, 79, 84) to 1.5).forEach { (chord, t) ->
                for (m in chord) note(brass, m.toDouble(), at(t), if (t >= 1.5) 1.3 else 0.42)
            }
            kick(at(1.5))
            snare(at(1.5), 0.8, 0.4)
            echo(0.2, 0.35, repeats = 2, wet = 0.25)
        }
        ENDING_BAD -> sound(3.4) {
            val pad = Instrument(Wave.TRIANGLE, Envelope(0.2, 0.5, 0.7, 0.9), 0.24, detune = 10.0)
            listOf(listOf(57, 60, 64) to 0.0, listOf(53, 57, 60) to 0.9, listOf(52, 55, 59) to 1.8).forEach { (chord, t) ->
                for (m in chord) note(pad, m.toDouble(), at(t), 0.8)
            }
            echo(0.3, 0.4, repeats = 2, wet = 0.3)
        }
    }

    companion object {
        /** One critic's score, a ding that climbs and brightens with the number. */
        fun review(score: Int): ShortArray {
            val s = score.coerceIn(1, 10)
            return sound(0.7) {
                val good = s >= 7
                val inst = if (s >= 5) {
                    Instrument(Wave.FM, Envelope.BELL, 0.42, fmRatio = if (good) 3.5 else 2.0, fmIndex = 0.6 + 0.12 * s)
                } else {
                    Instrument(Wave.TRIANGLE, Envelope(0.002, 0.18, 0.0, 0.1), 0.5, cutoff = 0.5)
                }
                // Scores walk up a major scale; the good ones get a second note a fifth above.
                val scale = intArrayOf(0, 2, 4, 5, 7, 9, 11, 12, 14, 16)
                val midi = 64.0 + scale[s - 1]
                note(inst, midi, 0, 0.18)
                if (good) note(inst.copy(gain = 0.25), midi + 7, at(0.07), 0.2)
            }
        }
    }
}

private fun at(seconds: Double) = (seconds * SAMPLE_RATE).toInt()

private fun sound(seconds: Double, draw: Mix.() -> Unit): ShortArray {
    val mix = Mix((seconds * SAMPLE_RATE).toInt())
    mix.draw()
    // Fade the last moment so nothing ends with a click.
    val fade = minOf(mix.frames, SAMPLE_RATE / 50)
    for (i in 0 until fade) mix.data[mix.frames - 1 - i] *= i.toFloat() / fade
    return mix.toPcm(0.85)
}
