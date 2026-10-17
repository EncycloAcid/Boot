package com.boot.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.boot.core.Format
import com.boot.core.GameState
import com.boot.core.Research
import com.boot.core.expansionBlocker
import com.boot.core.patchBlocker
import com.boot.core.patchCost
import com.boot.core.patchGame
import com.boot.core.remasterBlocker
import com.boot.core.remasterConcept
import com.boot.core.sequelConcept
import com.boot.core.startExpansion
import com.boot.desktop.DesktopStore

/** Post-ship loop: patch, expansion, sequel / remaster — same core as Android ReleaseSheet. */
@Composable
fun ReleaseReport(store: DesktopStore, state: GameState, releaseId: Int) {
    val g = state.games ?: return
    val r = g.release(releaseId) ?: run {
        store.openRelease = null
        return
    }
    val h = hudColors
    val colors = gameColors
    val own = r.developer == null && r.publisher == null
    Dialog(
        onDismissRequest = { store.openRelease = null },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize().background(Color(0x66000000)), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .widthIn(min = 400.dp, max = 560.dp)
                    .fillMaxWidth(0.58f)
                    .heightIn(max = 680.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(h.panel)
                    .border(1.dp, h.panelBorder, RoundedCornerShape(16.dp)),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(r.title, style = MaterialTheme.typography.headlineMedium, color = h.ink)
                        Hint("${"%.1f".format(r.score)} · ${r.verdict} · ${Format.money(r.lifetimeSales)}")
                    }
                    MacButton("Close", onClick = { store.openRelease = null }, primary = false)
                }
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GlassPanel {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Hint("Earned")
                                Text(Format.money(r.lifetimeSales), style = MaterialTheme.typography.titleLarge, color = h.cash)
                            }
                            Column {
                                Hint("Copies")
                                Text(Format.count(r.unitsSold), style = MaterialTheme.typography.titleLarge, color = h.text)
                            }
                            Column {
                                Hint("Best rank")
                                Text(if (r.bestRank > 0) "#${r.bestRank}" else "—", style = MaterialTheme.typography.titleLarge, color = h.text)
                            }
                        }
                        Hint("Dev cost ${Format.money(r.devCost)} · profit ${Format.signedMoney(r.lifetimeSales - r.devCost)}")
                    }
                    if (r.report.isNotEmpty()) {
                        GlassPanel {
                            Text("What you learned", style = MaterialTheme.typography.titleMedium, color = h.accent)
                            r.report.take(6).forEach { Hint(it) }
                        }
                    }
                    if (own) {
                        GlassPanel {
                            Text("After launch", style = MaterialTheme.typography.titleMedium, color = h.accent)
                            val patch = state.patchBlocker(r.id)
                            MacButton(
                                if (r.patched) "Patched" else "Release a patch · ${Format.money(r.patchCost())}",
                                onClick = { store.perform { patchGame(r.id) } },
                                enabled = patch == null,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            if (patch != null && !r.patched) Hint(patch, colors.bad)
                            else if (!r.patched) Hint("Clears bugs, lifts player score, refreshes sales.")
                            val expansion = state.expansionBlocker(r.id)
                            MacButton(
                                "Make an expansion pack",
                                onClick = {
                                    store.perform { startExpansion(r.id) }
                                    store.openRelease = null
                                },
                                enabled = expansion == null,
                                primary = false,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            if (expansion != null) Hint(expansion, colors.bad)
                        }
                    }
                    if (own && Research.SEQUELS in state.researched && !g.busy) {
                        MacButton(
                            "Make a sequel",
                            onClick = {
                                val seed = store.state?.sequelConcept(r.id)
                                store.openRelease = null
                                store.designGame(seed)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (own && Research.REMASTERS in state.researched && r.concept.remasterOf == null) {
                        val blocker = state.remasterBlocker(r.id)
                        MacButton(
                            "Remaster ${r.title}",
                            onClick = {
                                val seed = store.state?.remasterConcept(r.id)
                                store.openRelease = null
                                store.designGame(seed)
                            },
                            enabled = blocker == null && !g.busy,
                            primary = false,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (blocker != null) Hint(blocker, colors.bad)
                    }
                    Spacer(Modifier.heightIn(min = 8.dp))
                }
            }
        }
    }
}
