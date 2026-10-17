package com.boot.game.audio

import com.boot.game.settleMilestones
import com.boot.core.Era
import com.boot.core.GameConcept
import com.boot.core.GameSize
import com.boot.core.GameState
import com.boot.core.Genre
import com.boot.core.Lane
import com.boot.core.Platform
import com.boot.core.RivalMove
import com.boot.core.RivalMoveKind
import com.boot.core.Sandbox
import com.boot.core.Topic
import com.boot.core.advance
import com.boot.core.hire
import com.boot.core.launchGame
import com.boot.core.moveOffice
import com.boot.core.startGame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.DataOutputStream
import java.io.File
import kotlin.math.abs
import kotlin.math.sqrt

class AudioTest {
    private fun rms(pcm: ShortArray, from: Int = 0, to: Int = pcm.size): Double {
        var sum = 0.0
        for (i in from until to) sum += pcm[i].toDouble() * pcm[i]
        return sqrt(sum / (to - from)) / Short.MAX_VALUE
    }

    @Test
    fun everyEraHasATuneThatLoops() {
        val tracks = (Era.entries.map { MusicTrack.forEra(it) } + MusicTrack.TITLE).distinctBy { it.key }
        for (track in tracks) {
            val pcm = track.render()
            val style = track.style
            val expected = style.bars * 16 * (SAMPLE_RATE * 60 / style.bpm / 4)
            assertEquals("${track.key} length", expected, pcm.size)
            val loud = rms(pcm)
            assertTrue("${track.key} is audible ($loud)", loud > 0.04)
            assertTrue("${track.key} isn't a wall of noise ($loud)", loud < 0.45)
            // Every few seconds has something in it: no dead air.
            val window = SAMPLE_RATE * 2
            for (start in 0 until pcm.size - window step window) {
                assertTrue("${track.key} goes quiet at ${start / SAMPLE_RATE}s", rms(pcm, start, start + window) > 0.01)
            }
            // The end flows into the start: no jump bigger than the music makes by itself.
            val seam = abs(pcm.last() - pcm.first())
            var biggest = 0
            for (i in 1 until pcm.size) biggest = maxOf(biggest, abs(pcm[i] - pcm[i - 1]))
            assertTrue("${track.key} clicks at the loop ($seam vs $biggest)", seam <= biggest)
            dump(track.key.lowercase(), pcm)
        }
    }

    @Test
    fun tunesAreTheirOwn() {
        val a = MusicTrack.forEra(Era.BOOTSTRAP_YEARS).render()
        assertTrue("the same era composes the same tune", a.contentEquals(MusicTrack.forEra(Era.BOOTSTRAP_YEARS).render()))
        // Two eras in the same style still get different tunes.
        val b = MusicTrack.forEra(Era.REMOTE_WORK_BOOM)
        assertEquals(MusicStyle.LOFI, b.style)
        assertTrue(!a.contentEquals(b.render()))
        assertNotEquals(styleFor(Era.HOME_COMPUTERS), styleFor(Era.DOT_COM_BOOM))
    }

    @Test
    fun effectsAreShortAndEndQuietly() {
        for (sfx in Sfx.entries) {
            val pcm = sfx.render()
            assertTrue("$sfx is short", pcm.size <= SAMPLE_RATE * 4)
            assertTrue("$sfx is audible", rms(pcm) > 0.01)
            assertTrue("$sfx ends in silence", abs(pcm.last().toInt()) < 200)
            dump("sfx-" + sfx.name.lowercase(), pcm)
        }
        for (score in 1..10) {
            val pcm = Sfx.review(score)
            assertTrue(rms(pcm) > 0.01)
            assertTrue(abs(pcm.last().toInt()) < 200)
        }
        dump("sfx-reviews", (1..10).map { Sfx.review(it) }.reduce { acc, s -> acc + s })
    }

    private fun game(): GameState = GameState.create("Garage Labs", "You", Lane.GAMES, 3)

    private fun GameState.copy() = GameState.decode(encode())

    private fun cue(start: GameState, ticking: Boolean = false, change: GameState.() -> Unit): Sfx? {
        val after = start.copy().apply(change)
        return cueFor(start, after, ticking)
    }

    @Test
    fun theGameCuesItsSounds() {
        val s = game()
        assertEquals(Sfx.HIRE, cue(s) { hire(candidates.first().id, Lane.GAMES) })
        assertEquals(Sfx.ALERT, cue(s) { Sandbox.enqueue(this, RivalMove("Electronic Arts", Lane.GAMES, RivalMoveKind.PRICE_WAR)) })
        assertEquals(Sfx.WARNING, cue(s) { Sandbox.setCash(this, -10.0) })
        assertEquals(Sfx.COIN, cue(s) { Sandbox.setCash(this, cash - 500) })
        // Money moving by itself as the weeks go by stays quiet.
        assertNull(cue(s, ticking = true) { Sandbox.setCash(this, cash - 500) })
        assertEquals(Sfx.LEVEL_UP, cue(s) {
            Sandbox.setCash(this, 1_000_000.0)
            moveOffice()
        })

        // Make a game, see it ready, and ship it.
        val dev = s.copy().apply {
            Sandbox.setCash(this, 1_000_000.0)
            startGame(GameConcept("Moss Knight", Topic.FANTASY, Genre.RPG, platforms = listOf(Platform.PC), size = GameSize.SMALL))
        }
        var before = dev
        var sawReady = false
        repeat(80) {
            if (before.games?.project?.isReady == true) return@repeat
            val after = before.copy().apply { settleMilestones(); advance() }
            if (cueFor(before, after, ticking = true) == Sfx.READY) sawReady = true
            before = after
        }
        assertTrue("the game got ready", before.games?.project?.isReady == true)
        assertTrue("and the ready chime played on the way", sawReady)
        assertEquals(Sfx.LAUNCH, cue(before) { launchGame() })

        assertEquals(Sfx.HIT, verdictFor(8.5))
        assertEquals(Sfx.FLOP, verdictFor(3.0))
        assertNull(verdictFor(6.5))
    }

    /** With -Dboot.audio=dir, writes each sound as a WAV file to listen to. */
    private fun dump(name: String, pcm: ShortArray) {
        val dir = System.getProperty("boot.audio") ?: return
        val file = File(dir).apply { mkdirs() }.resolve("$name.wav")
        DataOutputStream(file.outputStream().buffered()).use { out ->
            fun int(v: Int) = out.writeInt(Integer.reverseBytes(v))
            fun short(v: Int) = out.writeShort(java.lang.Short.reverseBytes(v.toShort()).toInt())
            out.writeBytes("RIFF")
            int(36 + pcm.size * 2)
            out.writeBytes("WAVEfmt ")
            int(16)
            short(1)
            short(1)
            int(SAMPLE_RATE)
            int(SAMPLE_RATE * 2)
            short(2)
            short(16)
            out.writeBytes("data")
            int(pcm.size * 2)
            for (v in pcm) short(v.toInt())
        }
    }
}
