package com.boot.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.boot.core.Format
import com.boot.core.GameClock
import com.boot.core.GameProject
import com.boot.core.GameState
import com.boot.core.GamesStudio
import com.boot.core.IpPath
import com.boot.core.LegacyBeatKind
import com.boot.core.Region
import com.boot.core.accessibilityBlocker
import com.boot.core.accessibilityCost
import com.boot.core.acquisitionIpCost
import com.boot.core.buyIp
import com.boot.core.buyIpBlocker
import com.boot.core.devDiaryBlocker
import com.boot.core.devDiaryCost
import com.boot.core.dormantIp
import com.boot.core.goSilent
import com.boot.core.investAccessibility
import com.boot.core.investLocalisation
import com.boot.core.ipsForSale
import com.boot.core.localisationBlocker
import com.boot.core.localisationCost
import com.boot.core.museumTimeline
import com.boot.core.overshareRoadmap
import com.boot.core.overshareRoadmapBlocker
import com.boot.core.publishDevDiary
import com.boot.core.reviveIpBlocker
import com.boot.core.startIpRevival
import com.boot.core.transparencyLabel
import com.boot.game.GameStore
import java.util.Locale

/*
 * Android affordances for the legacy / trust pack: antique IP vault, transparency meter,
 * accessibility & localisation bets, and the company museum timeline.
 */

/** Transparency meter + diary / silence / roadmap on the studio overview. */
@Composable
fun TransparencySection(store: GameStore, state: GameState, g: GamesStudio, team: Int = 1) {
    val colors = gameColors
    val p = g.projectOf(team)
    val diaryBlock = state.devDiaryBlocker(team)
    val silenceCrisis = state.reputation.scars.any { it.severity > 0.1 }
    GroupedSection(
        "Transparency · ${g.transparencyLabel()}",
        footer = "Dev diaries and open roadmaps build trust and wishlists. Radio silence — especially in a crisis — cools fans and mod goodwill.",
    ) {
        CardBlock {
            Meter(
                "Community trust",
                g.transparency,
                when {
                    g.transparency >= 0.55 -> colors.good
                    g.transparency >= 0.35 -> colors.warn
                    else -> colors.bad
                },
                caption = "${Format.percent(g.transparency)} · ${g.diariesPublished} ${if (g.diariesPublished == 1) "diary" else "diaries"}",
            )
        }
        RowDivider()
        ActionRow(
            "Publish a dev diary",
            diaryBlock ?: (p?.let { "${Format.money(state.devDiaryCost(team))} · ${it.title}" } ?: "Nothing in development"),
            "Diary",
            enabled = diaryBlock == null,
        ) { store.perform { publishDevDiary(team) } }
        RowDivider()
        ActionRow(
            "Overshare the roadmap",
            state.overshareRoadmapBlocker(team) ?: "Hype spike now — overpromise risk later",
            "Dump",
            enabled = state.overshareRoadmapBlocker(team) == null,
        ) { store.perform { overshareRoadmap(team) } }
        RowDivider()
        ActionRow(
            if (silenceCrisis) "Go silent during a crisis" else "Radio silence",
            if (silenceCrisis) "Hurts trust and wishlists harder" else "Cool the meter a little",
            "Silent",
        ) { store.perform { goSilent() } }
    }
}

/** Accessibility & localisation bets while a game is in development. */
@Composable
fun AccessLocalSection(store: GameStore, state: GameState, p: GameProject, team: Int = 1) {
    val colors = gameColors
    val accessBlock = state.accessibilityBlocker(team)
    val localBlock = state.localisationBlocker(team, null)
    GroupedSection(
        "Accessibility & localisation",
        footer = "Costly upfront. Accessibility builds long-tail goodwill; localisation opens regional markets (and warms offices you already have).",
    ) {
        if (p.accessibilityBet) {
            ValueRow("Accessibility", "Pass booked", colors.good)
        } else {
            ActionRow(
                "Accessibility pass",
                accessBlock ?: "${Format.money(state.accessibilityCost(p))} · +${Format.percent(0.08)} work",
                "Invest",
                enabled = accessBlock == null,
            ) { store.perform { investAccessibility(team) } }
        }
        RowDivider()
        if (p.localisationRegions.isNotEmpty()) {
            ValueRow(
                "Localised",
                p.localisationRegions.joinToString { it.short },
                colors.good,
            )
        }
        ActionRow(
            "Broad localisation push",
            localBlock ?: "${Format.money(state.localisationCost(p))} · EU · APAC · LATAM",
            "Localise",
            enabled = localBlock == null,
        ) { store.perform { investLocalisation(team, null) } }
        // Single-region options when they have an office.
        for (region in listOf(Region.EUROPE, Region.ASIA_PACIFIC, Region.LATIN_AMERICA, Region.NORTH_AMERICA)) {
            if (region in p.localisationRegions) continue
            if (state.strategy.office(region) == null && region != Region.EUROPE) continue
            val block = state.localisationBlocker(team, region)
            RowDivider()
            ActionRow(
                "Localise for ${region.title}",
                block ?: Format.money(state.localisationCost(p, region)),
                region.short,
                enabled = block == null,
            ) { store.perform { investLocalisation(team, region) } }
        }
        if (p.techDemoPromise > 0) {
            RowDivider()
            ValueRow(
                "Tech demo promise",
                String.format(Locale.US, "%.0f hype bar", p.techDemoPromise),
                if (p.techDemoPromise > 25) colors.warn else colors.milestone,
            )
            Hint("If the shipped score undersells this flash, player reviews backlash.")
        }
    }
}

/** Antique IP vault: buy dormant brands and greenlight remakes/reboots. */
@Composable
fun AntiqueIpSection(store: GameStore, state: GameState, g: GamesStudio) {
    val colors = gameColors
    if (g.ownedIps.isNotEmpty()) {
        GroupedSection(
            "IP vault · ${g.ownedIps.size}",
            footer = "Remake (faithful, safer) or reboot (new take, meme risk). Eighteen months between revivals of the same brand.",
        ) {
            g.ownedIps.forEachIndexed { i, owned ->
                if (i > 0) RowDivider(68.dp)
                val ip = dormantIp(owned.id) ?: return@forEachIndexed
                Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(ip.brand, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Hint(
                        ip.blurb + " · memory ${Format.percent(ip.qualityMemory)} · meme risk ${Format.percent(ip.memeRisk)}" +
                            (if (owned.memeFlop) " · last revival was a meme flop" else "") +
                            (if (owned.lastScore >= 0) " · last ${String.format(Locale.US, "%.1f", owned.lastScore)}" else ""),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val remakeBlock = state.reviveIpBlocker(owned.id, IpPath.REMAKE)
                        val rebootBlock = state.reviveIpBlocker(owned.id, IpPath.REBOOT)
                        TonalButton(
                            "Remake",
                            { store.perform { startIpRevival(owned.id, IpPath.REMAKE) } },
                            enabled = remakeBlock == null && !g.busy,
                        )
                        TonalButton(
                            "Reboot",
                            { store.perform { startIpRevival(owned.id, IpPath.REBOOT) } },
                            enabled = rebootBlock == null && !g.busy,
                        )
                    }
                    (state.reviveIpBlocker(owned.id, IpPath.REMAKE) ?: state.reviveIpBlocker(owned.id, IpPath.REBOOT))?.let {
                        Hint(it, colors.warn)
                    }
                }
            }
        }
    }
    val forSale = state.ipsForSale()
    if (forSale.isEmpty()) return
    GroupedSection(
        "Antique IP · dormant brands",
        footer = "Buy dead brands and revive them. Era-aware catalogue — only brands that have already gone dormant this year.",
    ) {
        forSale.take(6).forEachIndexed { i, ip ->
            if (i > 0) RowDivider(68.dp)
            val block = state.buyIpBlocker(ip.id)
            ActionRow(
                ip.brand,
                block ?: "${ip.blurb} · died ${ip.diedYear} · ${Format.money(state.acquisitionIpCost(ip))}",
                "Buy",
                enabled = block == null,
            ) { store.perform { buyIp(ip.id) } }
        }
    }
}

/** Company museum wing: readable timeline of hits, flops, scars, triumphs. */
fun LazyListScope.museumItems(state: GameState) {
    val beats = state.museumTimeline()
    item {
        GroupedSection(
            "Museum · legacy wing",
            footer = "A readable timeline of this run's hits, flops, scars and triumphs — not a new sim. Fills as you ship, scar, list, and revive.",
        ) {
            if (beats.isEmpty()) {
                CardBlock { Hint("Nothing on exhibit yet. Ship games, take scars, ring the bell, buy antique IPs — they land here.") }
            } else {
                beats.takeLast(24).asReversed().forEachIndexed { i, beat ->
                    if (i > 0) RowDivider()
                    ListRow(
                        beat.title,
                        subtitle = beat.detail.ifBlank { beat.kind.title },
                        value = GameClock.label(beat.week),
                        valueDetail = beat.kind.title,
                        valueDetailColor = when (beat.kind) {
                            LegacyBeatKind.HIT, LegacyBeatKind.TRIUMPH -> gameColors.good
                            LegacyBeatKind.FLOP, LegacyBeatKind.SCAR -> gameColors.bad
                            LegacyBeatKind.MILESTONE -> gameColors.milestone
                        },
                    )
                }
            }
        }
    }
    if (state.stats.antiqueIps > 0 || state.stats.devDiaries > 0 || state.stats.techDemos > 0) {
        item {
            GroupedSection("Legacy stats") {
                if (state.stats.antiqueIps > 0) ValueRow("Antique IPs bought", "${state.stats.antiqueIps}")
                if (state.stats.antiqueHits > 0) {
                    RowDivider()
                    ValueRow("Antique hits", "${state.stats.antiqueHits}", gameColors.good)
                }
                if (state.stats.memeFlops > 0) {
                    RowDivider()
                    ValueRow("Meme flops", "${state.stats.memeFlops}", gameColors.bad)
                }
                if (state.stats.devDiaries > 0) {
                    RowDivider()
                    ValueRow("Dev diaries", "${state.stats.devDiaries}")
                }
                if (state.stats.accessibilityBets > 0) {
                    RowDivider()
                    ValueRow("Accessibility bets", "${state.stats.accessibilityBets}")
                }
                if (state.stats.localisationBets > 0) {
                    RowDivider()
                    ValueRow("Localisation bets", "${state.stats.localisationBets}")
                }
                if (state.stats.techDemos > 0) {
                    RowDivider()
                    ValueRow("Tech demos", "${state.stats.techDemos}")
                }
            }
        }
    }
}
