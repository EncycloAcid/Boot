package com.boot.game.ui

import androidx.compose.ui.text.font.FontFamily
import com.boot.core.PlayerQuote
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.boot.core.GameRelease
import com.boot.core.Review
import com.boot.core.ReviewAspect
import java.util.Locale

/** Aggregator colors: green for generally favorable, yellow for mixed, red for unfavorable. */
private fun metaColor(score100: Int): Color = when {
    score100 >= 75 -> Color(0xFF00CE7A)
    score100 >= 50 -> Color(0xFFFFBD3F)
    else -> Color(0xFFFF6874)
}

/** The critics' average, aggregator style: a number on a colored tile. */
@Composable
fun MetascoreBadge(score: Int, size: Dp = 64.dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.22f))
            .background(metaColor(score)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$score",
            style = if (size >= 60.dp) MaterialTheme.typography.headlineMediumEmphasized else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = if (score in 50..74) Color(0xFF1C1C1E) else Color.White,
        )
    }
}

/** What players think, in a circle, the way user scores usually look. */
@Composable
fun PlayerScoreBadge(score: Double, size: Dp = 64.dp) {
    val known = score > 0
    val c = if (known) metaColor((score * 10).toInt()) else MaterialTheme.colorScheme.outline
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(c),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (known) String.format(Locale.US, "%.1f", score) else "—",
            style = if (size >= 60.dp) MaterialTheme.typography.titleLargeEmphasized else MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            color = if (known && score * 10 in 50.0..74.9) Color(0xFF1C1C1E) else Color.White,
        )
    }
}

/** Critics and players side by side, with captions. */
@Composable
fun ScoreSummary(r: GameRelease) {
    Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MetascoreBadge(r.metascore)
            Spacer(Modifier.size(6.dp))
            Text("Metascore", style = MaterialTheme.typography.labelMedium)
            Text(if (r.reviews.isEmpty()) "${r.concept.collects.size} games" else "${r.reviews.size} critics", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PlayerScoreBadge(r.userScore)
            Spacer(Modifier.size(6.dp))
            Text(
                when {
                    r.userScore <= 0 -> "Players"
                    r.userScoreSettled -> "Players"
                    else -> "Early player score"
                },
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                when {
                    r.userScore <= 0 -> "no data"
                    !r.userScoreSettled -> "settles in a few weeks"
                    r.userScore >= r.score + 0.7 -> "love it more"
                    r.userScore <= r.score - 0.7 -> "less impressed"
                    else -> "agree"
                },
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** An outlet's score in that outlet's own format: stars, a percentage, quarters or a plain number. */
@Composable
fun OutletScore(review: Review) {
    val color = metaColor((review.exact * 10).toInt())
    Box(
        Modifier
            .widthIn(min = 56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            review.formatted,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/** One outlet: name, score, and the line people will quote. */
@Composable
fun ReviewRow(review: Review, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        CompanyLogo(review.outlet.title, size = 32.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(review.outlet.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                review.badge?.let { Pill(it, gameColors.milestone) }
            }
            // The pull quote, then the verdict.
            if (review.headline.isNotEmpty()) {
                Text(review.headline, style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold)
            }
            Text("“${review.comment}”", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        OutletScore(review)
    }
}

/** What players said: a thumb, the hours they put in, and a line. */
@Composable
fun PlayerReviews(quotes: List<PlayerQuote>) {
    if (quotes.isEmpty()) return
    val colors = gameColors
    GroupedSection("Players") {
        quotes.forEachIndexed { i, q ->
            if (i > 0) RowDivider()
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (q.recommended) "Recommended" else "Not recommended", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
                        color = if (q.recommended) colors.good else colors.bad,
                    )
                    Text(" · ${q.hours} hrs on record", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("“${q.text}”", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** How each part of the game landed. */
@Composable
fun AspectBreakdown(aspects: Map<ReviewAspect, Double>) {
    if (aspects.isEmpty()) return
    GroupedSection("Breakdown", footer = "From where the team spent its effort, the engine, and the bugs it shipped with.") {
        CardBlock(spacing = 10.dp) {
            for (a in ReviewAspect.entries) {
                val v = aspects[a] ?: continue
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(a.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(110.dp))
                    Box(
                        Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(gameColors.fill),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth((v / 10).toFloat().coerceIn(0.03f, 1f))
                                .height(8.dp)
                                .background(metaColor((v * 10).toInt()), CircleShape),
                        )
                    }
                    Text(
                        String.format(Locale.US, if (v % 1.0 == 0.0) "%.0f" else "%.1f", v),
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(40.dp),
                    )
                }
            }
        }
    }
}

/** The good and the bad, review-site style. */
@Composable
fun ProsCons(pros: List<String>, cons: List<String>) {
    if (pros.isEmpty() && cons.isEmpty()) return
    val colors = gameColors
    GroupedSection("The good and the bad") {
        val rows = pros.map { true to it } + cons.map { false to it }
        rows.forEachIndexed { i, (good, text) ->
            if (i > 0) RowDivider(52.dp)
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (good) Icons.Filled.AddCircle else Icons.Filled.RemoveCircle, null,
                    tint = if (good) colors.good else colors.bad, modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(14.dp))
                Text(text, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
