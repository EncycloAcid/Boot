package com.boot.desktop.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.res.useResource
import com.boot.core.CompanyMark
import com.boot.core.logoPackBasename
import org.jetbrains.skia.Image
import java.util.concurrent.ConcurrentHashMap

private val BitmapHits = ConcurrentHashMap<String, ImageBitmap>()
private val BitmapMisses = ConcurrentHashMap.newKeySet<String>()

/** Classpath path under resources for a pack basename, e.g. `logos/ea.png`. */
fun logoPackResourcePath(basename: String): String = "logos/$basename.png"

/** Load a licensed pack PNG for [mark], or null to fall back to vector / monogram. */
fun loadLogoPackBitmap(mark: CompanyMark): ImageBitmap? {
    val base = logoPackBasename(mark) ?: return null
    BitmapHits[base]?.let { return it }
    if (base in BitmapMisses) return null
    val loaded = loadClasspathPng(logoPackResourcePath(base))
    if (loaded != null) BitmapHits[base] = loaded else BitmapMisses.add(base)
    return loaded
}

@Composable
fun rememberLogoPackBitmap(mark: CompanyMark): ImageBitmap? =
    remember(mark.name, mark.logo) { loadLogoPackBitmap(mark) }

private fun loadClasspathPng(path: String): ImageBitmap? = try {
    useResource(path) { stream ->
        Image.makeFromEncoded(stream.readBytes()).toComposeImageBitmap()
    }
} catch (_: Exception) {
    null
}
