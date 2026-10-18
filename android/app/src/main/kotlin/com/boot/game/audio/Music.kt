package com.boot.game.audio

import com.boot.core.Era
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.random.Random

/*
 * The soundtrack. Every era gets two tunes of its own, composed from a seed in styles that suit
 * its time: chiptune for the home computers, FM synths and city pop for the console wars, house
 * and breakbeat for the boom years, lo-fi and lounge jazz for the bootstrap years and so on. The
 * two take turns. When the money runs out, a darker tune takes over until it comes back.
 *
 * A tune is built like a song: an idea stated, repeated with a lift, then a second section with
 * new chords and a higher tune, sometimes with a breakdown, and a harmony line to finish. It loops
 * without a seam.
 */

enum class Mode(val steps: IntArray) {
    MAJOR(intArrayOf(0, 2, 4, 5, 7, 9, 11)),
    MINOR(intArrayOf(0, 2, 3, 5, 7, 8, 10)),
    DORIAN(intArrayOf(0, 2, 3, 5, 7, 9, 10)),
    LYDIAN(intArrayOf(0, 2, 4, 6, 7, 9, 11)),
    /** Minor with a raised seventh: the sound of trouble. */
    HARMONIC(intArrayOf(0, 2, 3, 5, 7, 8, 11)),
}

enum class MusicStyle(val title: String, val bpm: Int, val mode: Mode, val swing: Double, val bars: Int) {
    CHIPTUNE("Chiptune", 138, Mode.MAJOR, 0.0, 32),
    FM("16-bit", 116, Mode.DORIAN, 0.0, 32),
    HOUSE("House", 122, Mode.MINOR, 0.0, 32),
    DOWNBEAT("Downbeat", 86, Mode.MINOR, 0.06, 16),
    LOFI("Lo-fi", 80, Mode.MAJOR, 0.14, 16),
    SYNTHWAVE("Synthwave", 100, Mode.MINOR, 0.0, 32),
    JAZZ("Lounge jazz", 126, Mode.DORIAN, 0.2, 32),
    AMBIENT("Ambient", 70, Mode.LYDIAN, 0.0, 16),
    BREAKBEAT("Breakbeat", 132, Mode.MINOR, 0.0, 32),
    CITY_POP("City pop", 108, Mode.MAJOR, 0.04, 32),
    ;

    /** How long one pass of a tune in this style lasts, in seconds. */
    val seconds: Double get() = bars * 16.0 * (SAMPLE_RATE * 60 / bpm / 4) / SAMPLE_RATE

    /** Dance styles drop the kick for a few bars in the second section. */
    val breakdown: Boolean get() = this == HOUSE || this == BREAKBEAT || this == SYNTHWAVE
}

/** The sounds of each era: its main tune's style first, then the second's. */
fun stylesFor(era: Era): Pair<MusicStyle, MusicStyle> = when (era) {
    Era.HOME_COMPUTERS -> MusicStyle.CHIPTUNE to MusicStyle.CHIPTUNE
    Era.VIDEO_GAME_CRASH -> MusicStyle.CHIPTUNE to MusicStyle.AMBIENT
    Era.CONSOLE_WARS -> MusicStyle.FM to MusicStyle.CITY_POP
    Era.MULTIMEDIA -> MusicStyle.FM to MusicStyle.BREAKBEAT
    Era.DOT_COM_BOOM -> MusicStyle.HOUSE to MusicStyle.BREAKBEAT
    Era.DOT_COM_BUST -> MusicStyle.DOWNBEAT to MusicStyle.JAZZ
    Era.WEB_2_0 -> MusicStyle.HOUSE to MusicStyle.DOWNBEAT
    Era.SMARTPHONES -> MusicStyle.LOFI to MusicStyle.HOUSE
    Era.FREE_TO_PLAY -> MusicStyle.LOFI to MusicStyle.SYNTHWAVE
    Era.BOOTSTRAP_YEARS -> MusicStyle.LOFI to MusicStyle.JAZZ
    Era.AI_GOLD_RUSH -> MusicStyle.HOUSE to MusicStyle.AMBIENT
    Era.VR_HYPE_CYCLE -> MusicStyle.SYNTHWAVE to MusicStyle.AMBIENT
    Era.CRYPTO_WINTER -> MusicStyle.DOWNBEAT to MusicStyle.AMBIENT
    Era.REMOTE_WORK_BOOM -> MusicStyle.LOFI to MusicStyle.CITY_POP
    Era.PRIVACY_PANIC -> MusicStyle.DOWNBEAT to MusicStyle.BREAKBEAT
    Era.CREATOR_ECONOMY -> MusicStyle.LOFI to MusicStyle.CITY_POP
    Era.RETRO_REVIVAL -> MusicStyle.CHIPTUNE to MusicStyle.SYNTHWAVE
    Era.METAVERSE_HANGOVER -> MusicStyle.SYNTHWAVE to MusicStyle.JAZZ
    Era.CHIP_SHORTAGE -> MusicStyle.DOWNBEAT to MusicStyle.CHIPTUNE
    Era.CLOUD_GAMING -> MusicStyle.SYNTHWAVE to MusicStyle.HOUSE
    Era.SUPER_APPS -> MusicStyle.SYNTHWAVE to MusicStyle.CITY_POP
}

/** The sound of each era's main tune. */
fun styleFor(era: Era): MusicStyle = stylesFor(era).first

/** What each era's two tunes are called. */
private fun namesFor(era: Era): Pair<String, String> = when (era) {
    Era.BOOTSTRAP_YEARS -> "Garage Days" to "Ramen Profitable"
    Era.AI_GOLD_RUSH -> "Gold Rush" to "Latent Space"
    Era.VR_HYPE_CYCLE -> "Headset Hype" to "Low Poly Dreams"
    Era.CRYPTO_WINTER -> "Cold Wallet" to "Long Winter"
    Era.REMOTE_WORK_BOOM -> "Camera Off" to "Home Office"
    Era.PRIVACY_PANIC -> "Cookie Banner" to "Data Leak"
    Era.CREATOR_ECONOMY -> "Like and Subscribe" to "Sponsored Segment"
    Era.RETRO_REVIVAL -> "Insert Coin" to "Pixel Nostalgia"
    Era.METAVERSE_HANGOVER -> "Empty Plaza" to "Morning After"
    Era.CHIP_SHORTAGE -> "Backorder" to "Silicon Drought"
    Era.CLOUD_GAMING -> "Input Lag" to "Server Farm"
    Era.SUPER_APPS -> "Everything App" to "Tap to Pay"
    Era.HOME_COMPUTERS -> "Ready Prompt" to "Cassette Loader"
    Era.VIDEO_GAME_CRASH -> "Landfill" to "Bargain Bin"
    Era.CONSOLE_WARS -> "Blast Processing" to "Tokyo Night Drive"
    Era.MULTIMEDIA -> "CD-ROM Drive" to "Big Beat Demo"
    Era.DOT_COM_BOOM -> "Irrational Exuberance" to "Dial-Up Dance"
    Era.DOT_COM_BUST -> "Pink Slip" to "Hotel Lobby"
    Era.WEB_2_0 -> "Rounded Corners" to "Forever Beta"
    Era.SMARTPHONES -> "Slide to Unlock" to "App Store Rush"
    Era.FREE_TO_PLAY -> "Daily Login" to "Gem Bundle"
}

/** Which tune to play. The same key always composes the same tune. */
data class MusicTrack(
    val key: String,
    val style: MusicStyle,
    val seed: Long,
    val title: String = style.title,
    /** The darker take, for when the company is in trouble. */
    val tense: Boolean = false,
) {
    companion object {
        /** The title screen's tunes. */
        val TITLE = MusicTrack("title", MusicStyle.LOFI, 1977, "Boot Up")
        val TITLE_B = MusicTrack("title-b", MusicStyle.JAZZ, 1984, "Lobby Music")

        private fun seed(era: Era) = 7_919L * (era.ordinal + 3)

        /** The era's main tune. */
        fun forEra(era: Era?): MusicTrack =
            if (era == null) TITLE else MusicTrack(era.name, styleFor(era), seed(era), namesFor(era).first)

        /**
         * What to play, in turn: the era's two tunes, or the title screen's. With [trouble], one
         * tense tune in the era's main style until things look up.
         */
        fun playlist(era: Era?, trouble: Boolean = false): List<MusicTrack> {
            if (era == null) return listOf(TITLE, TITLE_B)
            if (trouble) return listOf(MusicTrack("${era.name}-tense", styleFor(era), seed(era) + 5, "Red Ink", tense = true))
            return listOf(forEra(era), MusicTrack("${era.name}-b", stylesFor(era).second, seed(era) * 3 + 1, namesFor(era).second))
        }
    }

    /** How many times round before the next tune takes over: about two and a half minutes. */
    val plays: Int get() = maxOf(2, ceil(150.0 / style.seconds).toInt())

    fun render(): ShortArray = Composer(style, Random(seed), tense).render()
}

/** A note in 16th-note steps within a two-bar phrase: when, how long, and which scale degree. */
private data class Step(val at: Int, val length: Int, val degree: Int)

/** Kick, snare, closed hat, open hat and ghost snares, as 16-step patterns. */
private class Kit(val kick: String, val snare: String, val hat: String, val open: String = "", val ghost: String = "")

private class Composer(val style: MusicStyle, val rng: Random, val tense: Boolean) {
    val scale = if (tense) Mode.HARMONIC else style.mode
    val mode = scale.steps
    /** The key's root for chords; bass sits an octave down and the tune an octave up. */
    val root = 52 + rng.nextInt(0, 8)
    val stepFrames = SAMPLE_RATE * 60 / style.bpm / 4
    val frames = style.bars * 16 * stepFrames
    val half = style.bars / 2

    /** Eight-bar sections: two in a short tune, four in a long one. */
    val sections = style.bars / 8

    val progressionA: IntArray
    val progressionB: IntArray

    init {
        val options = when (scale) {
            Mode.MAJOR -> listOf(
                intArrayOf(0, 4, 5, 3), intArrayOf(0, 5, 3, 4), intArrayOf(5, 3, 0, 4), intArrayOf(0, 3, 5, 4), intArrayOf(3, 4, 2, 5),
                intArrayOf(3, 2, 1, 0), intArrayOf(0, 2, 3, 4),
            )
            Mode.MINOR -> listOf(
                intArrayOf(0, 5, 2, 6), intArrayOf(0, 3, 6, 2), intArrayOf(0, 6, 5, 6), intArrayOf(0, 5, 3, 4), intArrayOf(5, 6, 0, 0),
                intArrayOf(0, 3, 4, 6),
            )
            Mode.DORIAN -> listOf(intArrayOf(0, 3, 0, 3), intArrayOf(0, 6, 3, 0), intArrayOf(0, 3, 6, 4), intArrayOf(0, 2, 3, 4), intArrayOf(1, 4, 0, 0))
            Mode.LYDIAN -> listOf(intArrayOf(0, 1, 0, 1), intArrayOf(0, 4, 1, 0), intArrayOf(0, 5, 1, 4), intArrayOf(3, 1, 0, 0))
            Mode.HARMONIC -> listOf(intArrayOf(0, 3, 4, 0), intArrayOf(0, 5, 3, 4), intArrayOf(0, 5, 4, 4), intArrayOf(5, 3, 4, 0))
        }
        val shuffled = options.shuffled(rng)
        progressionA = shuffled[0]
        progressionB = shuffled[1]
    }

    /** MIDI note for a scale degree above [base]; degrees past 6 climb into the next octave. */
    fun pitch(degree: Int, base: Int = root): Double {
        val octave = Math.floorDiv(degree, 7)
        return (base + mode[Math.floorMod(degree, 7)] + 12 * octave).toDouble()
    }

    fun chordAt(bar: Int): Int {
        val b = Math.floorMod(bar, style.bars)
        return (if (b < half) progressionA else progressionB)[(b % half) % 4]
    }

    /** Frame where a 16th step of a bar starts, with swing pushing the off-beat eighths late. */
    fun frameAt(bar: Int, step: Int): Int {
        val swung = if (style.swing > 0 && step % 4 == 2) (style.swing * 2 * stepFrames).toInt() else 0
        return (bar * 16 + step) * stepFrames + swung
    }

    fun seconds(steps: Double) = steps * stepFrames / SAMPLE_RATE

    /** A little give in how hard each hit lands, the way a player's hands do. */
    fun human(v: Double) = v * (0.88 + rng.nextDouble() * 0.24)

    /** Is [bar] in the kick-less opening of the second section? */
    fun inBreakdown(bar: Int) = style.breakdown && sections == 4 && bar in 16 until 20

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
            MusicStyle.JAZZ -> lead.echo(seconds(4.0), 0.25, repeats = 1, wet = 0.15)
            MusicStyle.AMBIENT -> {
                lead.echo(seconds(6.0), 0.6, repeats = 4, wet = 0.5)
                chords.echo(seconds(8.0), 0.5, repeats = 3, wet = 0.35)
            }
            MusicStyle.BREAKBEAT -> {
                lead.echo(seconds(3.0), 0.4, repeats = 2, wet = 0.3)
                chords.echo(seconds(1.5), 0.35, repeats = 2, wet = 0.2)
            }
            MusicStyle.CITY_POP -> {
                lead.echo(seconds(3.0), 0.3, repeats = 2, wet = 0.22)
                chords.echo(seconds(2.0), 0.25, repeats = 1, wet = 0.15)
            }
        }
        val soft = style in setOf(MusicStyle.LOFI, MusicStyle.DOWNBEAT, MusicStyle.JAZZ, MusicStyle.AMBIENT)
        master.add(drums, if (soft) 0.7 else 0.85)
        master.add(bass, 0.9)
        master.add(chords, 0.8)
        master.add(lead, 0.9)
        when (style) {
            MusicStyle.LOFI -> {
                master.lowPass(0.42)
                master.crackle(1.0)
            }
            MusicStyle.DOWNBEAT -> master.lowPass(0.6)
            MusicStyle.JAZZ -> master.lowPass(0.7)
            MusicStyle.AMBIENT -> master.lowPass(0.5)
            else -> if (tense) master.lowPass(0.75)
        }
        return master.toPcm(level(master))
    }

    /**
     * How loud the peaks may go so every tune sits at the same loudness: a wall of pads would
     * otherwise come out twice as loud as a sparse beat with the same peaks.
     */
    private fun level(mix: Mix): Double {
        var top = 0.0
        var sum = 0.0
        for (v in mix.data) {
            top = maxOf(top, abs(v.toDouble()))
            sum += v.toDouble() * v
        }
        val rms = kotlin.math.sqrt(sum / mix.frames)
        if (top <= 0.0 || rms <= 0.0) return 0.72
        return minOf(0.72, 0.16 * top / rms)
    }

    // Drums

    private fun pattern(p: String) = p.mapIndexedNotNull { i, c -> if (c == 'x') i else null }

    /** Two kits per style: the first for the opening half, a busier one for the second. */
    private val kits: List<Kit> = when (style) {
        MusicStyle.CHIPTUNE -> listOf(
            Kit("x.......x.x.....", "....x.......x...", "x.x.x.x.x.x.x.x."),
            Kit("x.....x...x.....", "....x.......x..x", "x.x.x.x.x.x.x.x."),
        )
        MusicStyle.FM -> listOf(
            Kit("x.....x...x.....", "....x.......x..x", "x.x.x.x.x.x.x.x.", ghost = ".......x.x......"),
            Kit("x.....x.x.x.....", "....x.......x...", "xxx.x.xxx.x.x.xx", ghost = "...x......x....."),
        )
        MusicStyle.HOUSE -> listOf(
            Kit("x...x...x...x...", "....x.......x...", ".x.x.x.x.x.x.x.x", "..x...x...x...x."),
            Kit("x...x...x...x...", "....x.......x...", "xxxxxxxxxxxxxxxx", "..x...x...x...x."),
        )
        MusicStyle.DOWNBEAT -> listOf(
            Kit("x.........x.....", "........x.......", "x...x...x...x..."),
            Kit("x......x..x.....", "........x.......", "x.x.x.x.x.x.x.x.", ghost = "...........x...."),
        )
        MusicStyle.LOFI -> listOf(
            Kit("x......x..x.....", "....x.......x...", "x.x.x.x.x.x.x.x.", ghost = ".......x......x."),
            Kit("x.........xx....", "....x.......x...", "x.x.x.x.x.x.x.x.", ghost = "..x........x...."),
        )
        MusicStyle.SYNTHWAVE -> listOf(
            Kit("x.......x.......", "....x.......x...", "x.x.x.x.x.x.x.x."),
            Kit("x.......x.x.....", "....x.......x...", "xxxxxxxxxxxxxxxx"),
        )
        // The ride does the work in jazz; the kick only feathers and the snare drops the odd bomb.
        MusicStyle.JAZZ -> listOf(
            Kit("x.......x.......", "..............x.", "", ghost = "......x........."),
            Kit("x.......x.......", "......x.........", "", ghost = "...x.......x...."),
        )
        MusicStyle.AMBIENT -> listOf(
            Kit("x...............", "", "..x...x...x...x."),
            Kit("x.......x.......", "", "..x...x...x...x."),
        )
        // The classic break: a kick that skips, a snare on two and four that ghosts in between.
        MusicStyle.BREAKBEAT -> listOf(
            Kit("x.x.......xx....", "....x.......x...", "x.x.x.x.x.x.x.x.", ghost = ".......x.x....x."),
            Kit("x.x...x...x.....", "....x.......x..x", "x.xxx.x.x.xxx.x.", "..............x.", ".......x.x......"),
        )
        MusicStyle.CITY_POP -> listOf(
            Kit("x.....x...x.....", "....x.......x...", "x.x.x.x.x.x.x.x.", ghost = ".......x.....x.."),
            Kit("x.....x.x.x.....", "....x.......x...", "xxx.xxx.xxx.xxx.", "..............x.", "...x......x....."),
        )
    }

    private fun drums(mix: Mix) {
        val chip = style == MusicStyle.CHIPTUNE
        val tail = if (style == MusicStyle.SYNTHWAVE) 0.32 else if (chip) 0.1 else 0.18
        val fills = style in setOf(MusicStyle.CHIPTUNE, MusicStyle.FM, MusicStyle.SYNTHWAVE, MusicStyle.HOUSE, MusicStyle.BREAKBEAT, MusicStyle.CITY_POP)
        val crashes = fills && !chip
        val kickLevel = when (style) {
            MusicStyle.AMBIENT -> 0.45
            MusicStyle.LOFI -> 0.8
            else -> 1.0
        }
        val tomBase = 98.0 + rng.nextDouble() * 20
        for (bar in 0 until style.bars) {
            val kit = kits[if (bar >= half) 1 else 0]
            val breakdown = inBreakdown(bar)
            val fill = fills && bar % 8 == 7
            val end = if (fill) 12 else 16
            if (!breakdown) {
                for (s in pattern(kit.kick)) if (s < end) mix.kick(frameAt(bar, s), human(kickLevel), chip)
            }
            for (s in pattern(kit.snare)) if (s < end) mix.snare(frameAt(bar, s), human(if (style == MusicStyle.DOWNBEAT || style == MusicStyle.JAZZ) 0.6 else 0.9), tail)
            for (s in pattern(kit.ghost)) if (s < end) mix.snare(frameAt(bar, s), human(0.22), tail * 0.5)
            val hatLevel = if (breakdown) 0.5 else 1.0
            for (s in pattern(kit.hat)) mix.hat(frameAt(bar, s), human(hatLevel * if (s % 4 == 0) 0.75 else 0.45))
            for (s in pattern(kit.open)) mix.hat(frameAt(bar, s), human(0.5), open = true)
            if (style == MusicStyle.JAZZ) {
                // Ding, ding-a ding: the ride on every beat with the skip before two and four, and the hat on two and four.
                for (s in listOf(0, 4, 6, 8, 12, 14)) mix.cymbal(frameAt(bar, s), human(if (s % 4 == 0) 0.55 else 0.35), 0.35)
                mix.hat(frameAt(bar, 4), 0.35)
                mix.hat(frameAt(bar, 12), 0.35)
            }
            if (fill) fill(mix, bar, tail, tomBase, chip)
            // A crash opens each new section after a fill (and the top, where the loop comes round).
            if (crashes && bar % 8 == 0 && !breakdown) mix.cymbal(frameAt(bar, 0), 0.8, 1.1)
            // A riser out of the breakdown into the drop.
            if (style.breakdown && sections == 4 && bar == 19) mix.sweep(frameAt(bar, 0), seconds(16.0), 0.5, rising = true)
        }
    }

    /** Rounds off the last beat of an eight-bar section, a different way each time. */
    private fun fill(mix: Mix, bar: Int, tail: Double, tomBase: Double, chip: Boolean) {
        when (if (chip) 0 else rng.nextInt(3)) {
            0 -> for (s in 12 until 16) mix.snare(frameAt(bar, s), 0.4 + 0.15 * (s - 12), tail * 0.6)
            1 -> for ((i, s) in (12 until 16).withIndex()) mix.tom(frameAt(bar, s), tomBase * (1.0 - 0.16 * i), human(0.9))
            else -> {
                mix.kick(frameAt(bar, 12), 0.9)
                mix.snare(frameAt(bar, 13), 0.6, tail * 0.6)
                mix.tom(frameAt(bar, 14), tomBase * 0.8, 0.9)
                mix.tom(frameAt(bar, 15), tomBase * 0.62, 0.9)
            }
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
            // An upright: round, with a thump at the front of each note.
            MusicStyle.JAZZ -> Instrument(Wave.TRIANGLE, Envelope(0.004, 0.25, 0.35, 0.08), 0.62, cutoff = 0.4)
            MusicStyle.AMBIENT -> Instrument(Wave.SINE, Envelope(0.3, 0.6, 0.8, 0.8), 0.5)
            // A detuned saw pair growling under the break.
            MusicStyle.BREAKBEAT -> Instrument(Wave.SAW, Envelope(0.004, 0.2, 0.6, 0.06), 0.45, cutoff = 0.12, detune = 14.0)
            // Slap-ish: a bright pluck that pops on the octave.
            MusicStyle.CITY_POP -> Instrument(Wave.FM, Envelope(0.003, 0.18, 0.3, 0.05), 0.5, cutoff = 0.45, fmRatio = 1.0, fmIndex = 2.2)
        }
        if (style == MusicStyle.JAZZ) return walk(mix, inst, base)
        // (step, length in steps, 0 for the root or 4 for the fifth, octave up)
        val line: List<IntArray> = when (style) {
            MusicStyle.CHIPTUNE -> (0 until 8).map { intArrayOf(it * 2, 2, 0, it % 2) }
            MusicStyle.FM -> listOf(intArrayOf(0, 3, 0, 0), intArrayOf(3, 1, 0, 1), intArrayOf(6, 2, 0, 0), intArrayOf(8, 2, 0, 0), intArrayOf(10, 2, 4, 0), intArrayOf(12, 2, 0, 1), intArrayOf(14, 2, 0, 0))
            MusicStyle.HOUSE -> listOf(2, 6, 10, 14).map { intArrayOf(it, 2, 0, 0) }
            MusicStyle.DOWNBEAT -> listOf(intArrayOf(0, 6, 0, 0), intArrayOf(8, 4, 4, 0), intArrayOf(14, 2, 0, 0))
            MusicStyle.LOFI -> listOf(intArrayOf(0, 7, 0, 0), intArrayOf(10, 4, 4, 0), intArrayOf(14, 2, 0, 0))
            MusicStyle.SYNTHWAVE -> (0 until 8).map { intArrayOf(it * 2, 2, 0, 0) }
            MusicStyle.AMBIENT -> listOf(intArrayOf(0, 15, 0, 0))
            MusicStyle.BREAKBEAT -> listOf(intArrayOf(0, 3, 0, 0), intArrayOf(3, 3, 0, 0), intArrayOf(6, 2, 0, 1), intArrayOf(10, 3, 0, 0), intArrayOf(14, 2, 4, 0))
            MusicStyle.CITY_POP -> listOf(
                intArrayOf(0, 2, 0, 0), intArrayOf(3, 1, 0, 1), intArrayOf(4, 2, 0, 0), intArrayOf(7, 1, 0, 1),
                intArrayOf(8, 2, 4, 0), intArrayOf(10, 2, 0, 0), intArrayOf(12, 1, 0, 1), intArrayOf(14, 2, 0, 0),
            )
            MusicStyle.JAZZ -> emptyList()
        }
        // Basslines that lean into the next chord with a step on the way.
        val leads = style in setOf(MusicStyle.FM, MusicStyle.LOFI, MusicStyle.DOWNBEAT, MusicStyle.CITY_POP)
        for (bar in 0 until style.bars) {
            val chord = chordAt(bar)
            val next = chordAt(bar + 1)
            for ((i, note) in line.withIndex()) {
                val (step, length, interval, up) = note
                // In the breakdown the bass only holds the root, so the drop lands harder.
                if (inBreakdown(bar) && step != 0) continue
                val midi = if (leads && i == line.lastIndex && next != chord) {
                    pitch(next + if (next > chord) -1 else 1, base)
                } else {
                    pitch(chord + interval, base) + 12 * up
                }
                val held = if (inBreakdown(bar)) 15 else length
                mix.note(inst, midi, frameAt(bar, step), seconds(held * 0.9), human(1.0))
            }
        }
    }

    /** A walking bass: a note on every beat, root first, and a half step into the next chord. */
    private fun walk(mix: Mix, inst: Instrument, base: Int) {
        for (bar in 0 until style.bars) {
            val chord = chordAt(bar)
            val next = pitch(chordAt(bar + 1), base)
            val third = pitch(chord + 2, base)
            val fifth = pitch(chord + if (rng.nextBoolean()) 4 else 5, base)
            val approach = next + if (rng.nextBoolean()) -1 else 1
            listOf(pitch(chord, base), third, fifth, approach).forEachIndexed { beat, midi ->
                mix.note(inst, midi, frameAt(bar, beat * 4), seconds(3.4), human(if (beat == 0) 1.0 else 0.85))
            }
        }
    }

    // Chords

    private fun chordTones(chord: Int, sevenths: Boolean) = if (sevenths) listOf(0, 2, 4, 6).map { chord + it } else listOf(0, 2, 4).map { chord + it }

    /**
     * A chord's notes folded into one octave around the middle of the keys, so each chord moves
     * to the next by the smallest steps (the way a pianist plays them) instead of jumping about.
     */
    private fun voiced(degrees: List<Int>, center: Int = root + 7): List<Double> = degrees.map { d ->
        var m = pitch(d)
        while (m >= center + 6) m -= 12
        while (m < center - 6) m += 12
        m
    }.sorted()

    private fun chords(mix: Mix) {
        for (bar in 0 until style.bars) {
            val chord = chordAt(bar)
            when (style) {
                MusicStyle.CHIPTUNE -> {
                    // The classic arpeggio: chord tones rattling past in 16ths, up and then up and down.
                    val inst = Instrument(Wave.SQUARE, Envelope.BLIP, 0.16, duty = 0.125)
                    val tones = chordTones(chord, false) + (chord + 7)
                    val order = if (bar < half) tones else tones + tones.reversed().drop(1).dropLast(1)
                    for (s in 0 until 16) mix.note(inst, pitch(order[s % order.size], root + 12), frameAt(bar, s), seconds(0.8))
                }
                MusicStyle.FM -> {
                    val inst = Instrument(Wave.FM, Envelope.KEYS, 0.14, fmRatio = 2.0, fmIndex = 1.3)
                    for ((s, l) in listOf(0 to 3, 6 to 2, 10 to 4)) {
                        for (m in voiced(chordTones(chord, false))) mix.note(inst, m, frameAt(bar, s), seconds(l.toDouble()))
                    }
                }
                MusicStyle.HOUSE -> {
                    val inst = Instrument(Wave.SAW, Envelope.PLUCK, 0.13, cutoff = if (inBreakdown(bar)) 0.12 else 0.22, detune = 12.0)
                    for (s in listOf(2, 6, 10, 14)) {
                        for (m in voiced(chordTones(chord, true))) mix.note(inst, m, frameAt(bar, s), seconds(1.5))
                    }
                }
                MusicStyle.DOWNBEAT -> {
                    val pad = Instrument(Wave.TRIANGLE, Envelope.PAD, 0.12, detune = 8.0)
                    for (m in voiced(chordTones(chord, false))) mix.note(pad, m, frameAt(bar, 0), seconds(15.0))
                    val keys = Instrument(Wave.FM, Envelope.KEYS, 0.12, fmRatio = 1.0, fmIndex = 0.8)
                    val tones = chordTones(chord, true)
                    for ((i, s) in listOf(0, 4, 8, 12).withIndex()) mix.note(keys, pitch(tones[i % tones.size], root + 12), frameAt(bar, s), seconds(3.0))
                }
                MusicStyle.LOFI -> {
                    // An electric piano with a little tape wobble, on the one and pushed ahead of three.
                    val keys = Instrument(Wave.FM, Envelope.KEYS, 0.13, cutoff = 0.55, fmRatio = 1.0, fmIndex = 0.9, vibrato = 0.06)
                    for ((s, l) in listOf(0 to 6, 10 to 5)) {
                        for (m in voiced(chordTones(chord, true))) mix.note(keys, m, frameAt(bar, s), seconds(l.toDouble()))
                    }
                }
                MusicStyle.SYNTHWAVE -> {
                    val pad = Instrument(Wave.SAW, Envelope.PAD, 0.09, cutoff = 0.1, detune = 15.0)
                    for (m in voiced(chordTones(chord, false))) mix.note(pad, m, frameAt(bar, 0), seconds(15.5))
                    if (!inBreakdown(bar)) {
                        val arp = Instrument(Wave.SQUARE, Envelope.PLUCK, 0.07, cutoff = 0.3)
                        val tones = listOf(chord, chord + 4, chord + 7, chord + 2)
                        for (s in 0 until 16) mix.note(arp, pitch(tones[s % tones.size], root + 12), frameAt(bar, s), seconds(0.9))
                    }
                }
                MusicStyle.JAZZ -> {
                    // Rhodes comping with sevenths and ninths, on the Charleston rhythm and its answer.
                    val keys = Instrument(Wave.FM, Envelope.KEYS, 0.11, cutoff = 0.6, fmRatio = 1.0, fmIndex = 1.1, vibrato = 0.03)
                    val tones = voiced(listOf(chord + 2, chord + 4, chord + 6, chord + 8))
                    val hits = if (bar % 2 == 0) listOf(0 to 3, 6 to 3) else listOf(2 to 2, 10 to 4)
                    for ((s, l) in hits) for (m in tones) mix.note(keys, m, frameAt(bar, s), seconds(l.toDouble()), human(1.0))
                }
                MusicStyle.AMBIENT -> {
                    val pad = Instrument(Wave.TRIANGLE, Envelope(1.2, 1.0, 0.8, 1.5), 0.12, cutoff = 0.4, detune = 10.0)
                    for (m in voiced(chordTones(chord, true))) mix.note(pad, m, frameAt(bar, 0), seconds(15.0))
                    // Bells drifting over the top, a few to a bar.
                    val bell = Instrument(Wave.FM, Envelope.BELL, 0.07, fmRatio = 3.5, fmIndex = 1.4)
                    val tones = chordTones(chord, true)
                    for (s in listOf(0, 6, 10)) if (rng.nextDouble() < 0.7) mix.note(bell, pitch(tones.random(rng), root + 12), frameAt(bar, s), seconds(2.0))
                }
                MusicStyle.BREAKBEAT -> {
                    // Off-beat stabs, with a dark pad under the breakdown.
                    val tones = voiced(chordTones(chord, true))
                    if (inBreakdown(bar)) {
                        val pad = Instrument(Wave.SAW, Envelope.PAD, 0.08, cutoff = 0.1, detune = 15.0)
                        for (m in tones) mix.note(pad, m, frameAt(bar, 0), seconds(15.5))
                    } else {
                        val stab = Instrument(Wave.SAW, Envelope.PLUCK, 0.12, cutoff = 0.25, detune = 10.0)
                        for (s in listOf(0, 7, 10)) for (m in tones) mix.note(stab, m, frameAt(bar, s), seconds(1.2))
                    }
                }
                MusicStyle.CITY_POP -> {
                    // Bright electric piano pushing ahead of the beat, and a string pad holding it together.
                    val keys = Instrument(Wave.FM, Envelope.KEYS, 0.11, fmRatio = 1.0, fmIndex = 1.4)
                    val tones = voiced(chordTones(chord, true))
                    for ((s, l) in listOf(0 to 2, 3 to 2, 6 to 3, 10 to 2, 14 to 2)) for (m in tones) mix.note(keys, m, frameAt(bar, s), seconds(l.toDouble()), human(1.0))
                    val strings = Instrument(Wave.SAW, Envelope.PAD, 0.05, cutoff = 0.14, detune = 12.0)
                    for (m in tones) mix.note(strings, m + 12, frameAt(bar, 0), seconds(15.0))
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
        MusicStyle.HOUSE, MusicStyle.SYNTHWAVE, MusicStyle.BREAKBEAT -> listOf(
            listOf(0 to 4, 4 to 2, 6 to 2, 8 to 4, 12 to 4, 16 to 6, 22 to 2, 24 to 8),
            listOf(0 to 6, 6 to 2, 8 to 6, 14 to 2, 16 to 4, 20 to 4, 24 to 8),
            listOf(2 to 2, 4 to 4, 10 to 2, 12 to 4, 18 to 2, 20 to 4, 24 to 8),
        )
        MusicStyle.DOWNBEAT, MusicStyle.LOFI, MusicStyle.AMBIENT -> listOf(
            listOf(0 to 8, 8 to 8, 16 to 12, 28 to 4),
            listOf(4 to 4, 8 to 4, 12 to 4, 16 to 16),
            listOf(2 to 2, 4 to 6, 10 to 6, 18 to 2, 20 to 12),
        )
        // Swung eighths with room to breathe between the lines.
        MusicStyle.JAZZ -> listOf(
            listOf(0 to 2, 2 to 2, 4 to 2, 6 to 4, 12 to 2, 14 to 2, 16 to 6, 26 to 2, 28 to 4),
            listOf(2 to 2, 4 to 2, 6 to 2, 8 to 2, 10 to 6, 18 to 2, 20 to 2, 22 to 2, 24 to 8),
            listOf(0 to 6, 6 to 2, 8 to 2, 10 to 2, 12 to 4, 20 to 2, 22 to 2, 24 to 8),
        )
        // Pushed sixteenths, the city-pop way.
        MusicStyle.CITY_POP -> listOf(
            listOf(0 to 3, 3 to 3, 6 to 2, 8 to 4, 14 to 2, 16 to 3, 19 to 3, 22 to 2, 24 to 8),
            listOf(0 to 2, 2 to 2, 4 to 3, 7 to 5, 14 to 2, 16 to 2, 18 to 2, 20 to 3, 23 to 9),
        )
    }

    /** A two-bar idea starting from [start], fitted to the chords at [bar], reaching no higher than [top]. */
    private fun motif(bar: Int, start: Int, top: Int = 13): List<Step> {
        val rhythm = rhythms[rng.nextInt(rhythms.size)]
        var degree = start
        // A tune keeps going the way it was heading for a while, and turns back at the edges.
        var direction = if (rng.nextBoolean()) 1 else -1
        return rhythm.map { (at, length) ->
            val chord = chordAt(bar + at / 16)
            if (at % 8 == 0) {
                // On the strong beats, land on a chord tone near where the tune already is.
                val tones = (-1..2).flatMap { o -> chordTones(chord, false).map { it + 7 * o } }.filter { it in 3..top }
                degree = tones.minByOrNull { abs(it - degree) + rng.nextDouble() * 1.5 } ?: degree
            } else {
                if (degree >= top - 1) direction = -1
                if (degree <= 4) direction = 1
                if (rng.nextDouble() < 0.25) direction = -direction
                degree = (degree + direction * listOf(1, 1, 1, 2).random(rng)).coerceIn(3, top)
            }
            Step(at, length, degree)
        }
    }

    /** The same idea moved to fit the chord a couple of bars on, the way a tune answers itself. */
    private fun answer(m: List<Step>, from: Int, to: Int): List<Step> {
        val shift = chordAt(to) - chordAt(from)
        return m.map { it.copy(degree = (it.degree + shift).coerceIn(3, 15)) }
    }

    /** Ends a phrase at home: the last note on the key's own note, held. */
    private fun cadence(m: List<Step>): List<Step> = m.dropLast(1) + m.last().let { it.copy(degree = 7, length = maxOf(it.length, 6)) }

    /**
     * Eight bars of tune for one half of the song, from bar [start]: an idea, its answer,
     * something new, and the idea again to come home. The second time through, the new part
     * climbs higher. [lift] raises the whole half, so the second half of the song soars.
     */
    private fun verse(start: Int, lift: Int): Pair<List<Pair<Int, List<Step>>>, List<Pair<Int, List<Step>>>> {
        val m = motif(start, 7 + lift + rng.nextInt(-1, 3), 13 + lift)
        val n = motif(start + 4, 9 + lift, 13 + lift)
        val climb = motif(start + 4, 11 + lift, 15)
        val first = listOf(
            start to m,
            (start + 2) to answer(m, start, start + 2),
            (start + 4) to n,
            (start + 6) to cadence(answer(m, start, start + 6)),
        )
        return first to first.mapIndexed { i, p -> if (i == 2) p.first to climb else p }
    }

    private fun melody(mix: Mix) {
        val inst = when (style) {
            MusicStyle.CHIPTUNE -> Instrument(Wave.SQUARE, Envelope.LEAD, 0.26, duty = 0.25, vibrato = 0.15)
            MusicStyle.FM -> Instrument(Wave.FM, Envelope.LEAD, 0.28, fmRatio = 1.0, fmIndex = 2.2, vibrato = 0.12)
            MusicStyle.HOUSE -> Instrument(Wave.SQUARE, Envelope.PLUCK, 0.22, cutoff = 0.35)
            MusicStyle.DOWNBEAT -> Instrument(Wave.SINE, Envelope.LEAD, 0.26, cutoff = 0.6, vibrato = 0.2)
            MusicStyle.LOFI -> Instrument(Wave.TRIANGLE, Envelope.LEAD, 0.24, cutoff = 0.45, vibrato = 0.08)
            MusicStyle.SYNTHWAVE -> Instrument(Wave.SAW, Envelope.LEAD, 0.2, cutoff = 0.3, detune = 10.0, vibrato = 0.25)
            // A vibraphone: struck, ringing, with a slow shimmer.
            MusicStyle.JAZZ -> Instrument(Wave.FM, Envelope(0.003, 0.8, 0.3, 0.4), 0.27, fmRatio = 4.0, fmIndex = 0.9, vibrato = 0.08)
            MusicStyle.AMBIENT -> Instrument(Wave.SINE, Envelope(0.08, 0.4, 0.7, 0.6), 0.22, vibrato = 0.1)
            MusicStyle.BREAKBEAT -> Instrument(Wave.SQUARE, Envelope.LEAD, 0.2, cutoff = 0.3, duty = 0.3, glide = -1.0)
            MusicStyle.CITY_POP -> Instrument(Wave.SAW, Envelope.LEAD, 0.19, cutoff = 0.35, detune = 7.0, vibrato = 0.18)
        }
        // A softer voice for the harmony line a third below.
        val harmony = inst.copy(gain = inst.gain * 0.55, vibrato = 0.0)
        // Calmer styles leave the opening section to the band and bring the tune in after it.
        val quietFirst = style in setOf(MusicStyle.HOUSE, MusicStyle.DOWNBEAT, MusicStyle.LOFI, MusicStyle.AMBIENT, MusicStyle.BREAKBEAT)
        val a = verse(0, 0)
        val b = verse(half, 2)
        for (section in 0 until sections) {
            if (quietFirst && section == 0) continue
            val start = section * 8
            val inB = start >= half
            val sourceStart = if (inB) half else 0
            // Each half states its idea, then (in a long tune) says it again with the climb.
            val again = start - sourceStart >= 8
            val source = (if (inB) b else a).let { if (again) it.second else it.first }
            // In the breakdown the tune waits two bars before coming back in.
            val skip = if (inBreakdown(start)) 2 else 0
            // The last section of a long tune adds a harmony on the long notes, to lift the ending.
            val last = sections == 4 && section == sections - 1
            for ((at, steps) in source) {
                val phraseBar = at - sourceStart + start
                if (phraseBar - start < skip) continue
                for (s in steps) {
                    val frame = frameAt(phraseBar + s.at / 16, s.at % 16)
                    mix.note(inst, pitch(s.degree), frame, seconds(s.length * 0.92))
                    if (last && s.length >= 4) mix.note(harmony, pitch(s.degree - 2), frame, seconds(s.length * 0.92))
                }
            }
        }
    }
}
