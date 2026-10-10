package com.boot.game

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import com.boot.game.ui.CoverWithScore
import com.boot.game.ui.GameCover
import com.boot.core.gameTitle
import com.boot.core.Topic
import com.boot.core.Platform
import com.boot.core.Genre
import com.boot.core.GameSize
import com.boot.core.GameConcept
import com.boot.core.Audience
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.core.Office
import com.boot.core.Perk
import com.boot.core.advance
import com.boot.core.assign
import com.boot.core.buyPerk
import com.boot.core.choose
import com.boot.core.hire
import com.boot.core.moveOffice
import com.boot.game.ui.BootTheme
import com.boot.game.ui.OfficeScene
import com.boot.game.ui.FounderPortrait
import com.boot.game.ui.StaffPortrait
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Every office, drawn, so the art can be checked by eye. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w393dp-h852dp-xxhdpi")
class SceneGalleryTest {
    @get:Rule
    val compose = createComposeRule()

    private fun office(tier: Office, people: Int, perks: List<Perk>, lanes: List<Lane> = listOf(Lane.GAMES)): GameState {
        val base = GameState.create("Pixel Forge", "Rae", lanes.first(), seed = 5)
        val s = GameState.decode(base.encode().replace(Regex("\"cash\":[-0-9.Ee+]+"), "\"cash\":5.0E9"))
        while (s.office != tier) s.moveOffice()
        perks.forEach { s.buyPerk(it) }
        var guard = 0
        while (s.staff.size < people && guard++ < 80) {
            s.candidates.toList().forEach { if (s.staff.size < people) runCatching { s.hire(it.id) } }
            if (s.staff.size < people) {
                s.advance()
                while (s.needsDecision) s.choose(s.events[0].choices.size - 1)
            }
        }
        if (lanes.size > 1) s.staff.forEachIndexed { i, e -> if (i % 4 == 3) s.assign(e.id, null) }
        assertEquals(people, s.staff.size)
        return s
    }

    private fun save(name: String) {
        compose.waitForIdle()
        val view = (compose.onRoot().fetchSemanticsNode().root as ViewRootForTest).view
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun gallery(name: String) {
        val rooms = listOf(
            office(Office.APARTMENT, 2, listOf(Perk.COFFEE)),
            office(Office.COWORKING, 6, listOf(Perk.COFFEE, Perk.ARCADE, Perk.PLANTS)),
            office(Office.STUDIO, 7, listOf(Perk.COFFEE, Perk.ARCADE, Perk.CHEF, Perk.LIBRARY, Perk.NAP_PODS), listOf(Lane.GAMES)),
            office(Office.DOWNTOWN, 9, listOf(Perk.COFFEE, Perk.ARCADE, Perk.CHEF, Perk.LIBRARY, Perk.GYM, Perk.ROOFTOP, Perk.PLANTS)),
            office(Office.CAMPUS, 11, Perk.entries.toList()),
        )
        compose.setContent {
            BootTheme {
                Column(Modifier.padding(8.dp)) {
                    rooms.forEach { s ->
                        OfficeScene(s, playing = false, modifier = Modifier.fillMaxWidth().height(160.dp).padding(bottom = 4.dp))
                    }
                }
            }
        }
        save(name)
    }

    @Test
    fun everyOfficeByDay() = gallery("50-offices-day")

    @Test
    @Config(qualifiers = "+night")
    fun everyOfficeByNight() = gallery("51-offices-night")

    private fun covers(topics: List<Topic>, name: String) {
        val platforms = listOf(Platform.PC, Platform.PLAYSYSTEM_5, Platform.SWITCHEROO, Platform.MBOX_Z, Platform.GRPHONE)
        val concepts = topics.map { t ->
            val i = t.ordinal
            val genre = Genre.entries[i % Genre.entries.size]
            val base = gameTitle(t, genre, i.toLong())
            GameConcept(
                if (i % 3 == 0) "$base ${2 + i % 3}" else base, t, genre, platforms = listOf(platforms[i % platforms.size]),
                audience = Audience.entries[i % Audience.entries.size], size = GameSize.MEDIUM,
            )
        }
        compose.setContent {
            BootTheme {
                Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    concepts.chunked(4).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { row.forEach { GameCover(it, 88.dp, studio = "Pixel Forge") } }
                    }
                }
            }
        }
        save(name)
    }

    @Test
    fun boxArt() = covers(Topic.entries.take(20), "53-box-art")

    @Test
    fun moreBoxArt() {
        covers(Topic.entries.drop(20), "54-box-art")
    }

    @Test
    fun logo() {
        val ctx = org.robolectric.RuntimeEnvironment.getApplication()
        val size = 432
        val out = Bitmap.createBitmap(size * 3 + 80, size + 40, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(android.graphics.Color.rgb(242, 242, 247))
        // The launcher icon as a circle, as a rounded square, and the themed monochrome version.
        val layers = listOf(com.boot.game.R.drawable.ic_launcher_background, com.boot.game.R.drawable.ic_launcher_foreground)
        val shapes = listOf(size / 2f, size * 0.22f)
        shapes.forEachIndexed { i, radius ->
            val tile = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val c = Canvas(tile)
            for (id in layers) ctx.getDrawable(id)!!.apply { setBounds(-size / 4, -size / 4, size + size / 4, size + size / 4) }.draw(c)
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { shader = android.graphics.BitmapShader(tile, android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP) }
            canvas.save()
            canvas.translate(20f + i * (size + 20f), 20f)
            canvas.drawRoundRect(0f, 0f, size.toFloat(), size.toFloat(), radius, radius, paint)
            canvas.restore()
        }
        val mono = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val mc = Canvas(mono)
        mc.drawColor(android.graphics.Color.rgb(46, 52, 64))
        ctx.getDrawable(com.boot.game.R.drawable.ic_launcher_monochrome)!!.apply {
            setTint(android.graphics.Color.rgb(200, 215, 255))
            setBounds(-size / 4, -size / 4, size + size / 4, size + size / 4)
        }.draw(mc)
        val mp = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { shader = android.graphics.BitmapShader(mono, android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP) }
        canvas.save()
        canvas.translate(20f + 2 * (size + 20f), 20f)
        canvas.drawRoundRect(0f, 0f, size.toFloat(), size.toFloat(), size / 2f, size / 2f, mp)
        canvas.restore()
        File("build/screenshots").mkdirs()
        File("build/screenshots/00-logo.png").outputStream().use { out.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun portraits() {
        val s = office(Office.STUDIO, 12, emptyList(), listOf(Lane.GAMES, Lane.GAMES))
        compose.setContent {
            BootTheme {
                Column(Modifier.padding(8.dp)) {
                    OfficeScene(s, playing = false, modifier = Modifier.fillMaxWidth().height(260.dp))
                    Row { FounderPortrait(s, 64.dp); s.staff.take(5).forEach { StaffPortrait(it, 64.dp) } }
                    Row { s.staff.drop(5).take(6).forEach { StaffPortrait(it, 64.dp) } }
                }
            }
        }
        save("52-portraits")
    }
}
