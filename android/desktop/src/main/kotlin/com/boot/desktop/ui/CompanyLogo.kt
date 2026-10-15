package com.boot.desktop.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boot.core.CompanyMark
import com.boot.core.MarkShape
import com.boot.core.companyMark
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Geometric monogram badge for a company — not trademark artwork. */
@Composable
fun CompanyLogo(name: String, size: Dp = 28.dp, modifier: Modifier = Modifier) {
    CompanyLogo(companyMark(name), size, modifier)
}

@Composable
fun CompanyLogo(mark: CompanyMark, size: Dp = 28.dp, modifier: Modifier = Modifier) {
    val bg = Color(mark.bg)
    val fg = Color(mark.fg)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawMarkShape(mark.shape, bg)
        }
        Text(
            mark.initials.take(4),
            color = fg,
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

private fun DrawScope.drawMarkShape(shape: MarkShape, color: Color) {
    val pad = size.minDimension * 0.04f
    val w = size.width - pad * 2
    val h = size.height - pad * 2
    val topLeft = Offset(pad, pad)
    when (shape) {
        MarkShape.DISC -> drawOval(color, topLeft, Size(w, h))
        MarkShape.ROUNDED_SQUARE -> drawRoundRect(color, topLeft, Size(w, h), CornerRadius(w * 0.22f))
        MarkShape.PILL -> drawRoundRect(color, topLeft, Size(w, h), CornerRadius(h / 2f))
        MarkShape.DIAMOND -> {
            val path = Path().apply {
                moveTo(size.width / 2f, pad)
                lineTo(size.width - pad, size.height / 2f)
                lineTo(size.width / 2f, size.height - pad)
                lineTo(pad, size.height / 2f)
                close()
            }
            drawPath(path, color)
        }
        MarkShape.HEX -> {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = min(w, h) / 2f
            val path = Path()
            for (i in 0 until 6) {
                val a = Math.toRadians(60.0 * i - 30.0)
                val x = cx + r * cos(a).toFloat()
                val y = cy + r * sin(a).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, color)
        }
    }
}
