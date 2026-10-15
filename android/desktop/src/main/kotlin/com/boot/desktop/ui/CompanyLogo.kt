package com.boot.desktop.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boot.core.CompanyMark
import com.boot.core.LogoId
import com.boot.core.companyMark

/**
 * Brand mark badge — licensed pack PNG when bundled, else hand-drawn vector silhouette,
 * else monogram. Year-gating stays in CompanyMarks; this only paints the resolved mark.
 */
@Composable
fun CompanyLogo(name: String, size: Dp = 28.dp, modifier: Modifier = Modifier) {
    CompanyLogo(companyMark(name), size, modifier)
}

@Composable
fun CompanyLogo(mark: CompanyMark, size: Dp = 28.dp, modifier: Modifier = Modifier) {
    val pack = rememberLogoPackBitmap(mark)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        when {
            pack != null -> Image(
                bitmap = pack,
                contentDescription = mark.name,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(size * 0.06f),
                contentScale = ContentScale.Fit,
            )
            else -> {
                Canvas(Modifier.fillMaxSize()) {
                    drawBrandLogo(mark)
                }
                if (mark.logo == LogoId.NONE) {
                    Text(
                        mark.initials.take(4),
                        color = Color(mark.fg),
                        fontWeight = FontWeight.Bold,
                        fontSize = (size.value * when {
                            mark.initials.length >= 4 -> 0.28f
                            mark.initials.length == 3 -> 0.34f
                            else -> 0.42f
                        }).sp,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
