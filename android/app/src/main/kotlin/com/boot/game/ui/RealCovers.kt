package com.boot.game.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.text.Normalizer

/*
 * Real box art for real games, from the covers bundled in assets/covers. A game whose title
 * matches one (an old Call of Duty, a Zelda, a Pokémon) shows its real cover; everything else,
 * your own games included, keeps the drawn box art.
 */
object RealCovers {
    private var index: Map<String, String>? = null
    private val cache = LruCache<String, ImageBitmap>(48)

    /** "Pokémon Gold and Silver" and "pokemon gold & silver" match the same cover. */
    fun key(title: String): String =
        Normalizer.normalize(title.replace("&", " and "), Normalizer.Form.NFKD).replace(Regex("\\p{M}+"), "")
            .lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()

    private fun index(context: Context): Map<String, String> = index ?: runCatching {
        context.assets.open("covers/index.tsv").bufferedReader().useLines { lines ->
            lines.mapNotNull { line -> line.split('\t').takeIf { it.size >= 4 }?.let { it[0] to it[3] } }.toMap()
        }
    }.getOrDefault(emptyMap()).also { index = it }

    /** Whether there's a real cover for [title]. */
    fun has(context: Context, title: String): Boolean = key(title) in index(context)

    /** The real cover for [title], or null. */
    fun load(context: Context, title: String): ImageBitmap? {
        val file = index(context)[key(title)] ?: return null
        cache.get(file)?.let { return it }
        return runCatching {
            context.assets.open("covers/$file").use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
        }.getOrNull()?.also { cache.put(file, it) }
    }
}
