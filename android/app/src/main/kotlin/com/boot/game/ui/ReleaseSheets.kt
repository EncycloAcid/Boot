package com.boot.game.ui

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.testTag
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.boot.game.audio.Sfx
import com.boot.game.audio.verdictFor
import androidx.compose.ui.window.DialogProperties
import com.boot.core.portOutlook
import com.boot.core.PortQuality
import com.boot.core.saleOutlook
import com.boot.core.SaleDepths
import com.boot.core.cutPrice
import com.boot.core.priceCutBlocker
import com.boot.core.onSale
import com.boot.core.price
import com.boot.core.PriceCuts
import com.boot.core.ExpansionScope
import com.boot.core.addStore
import com.boot.core.addStoreBlocker
import com.boot.core.storeName
import com.boot.core.storesOf
import com.boot.core.storesOpen
import com.boot.core.Platform
import com.boot.core.releaseModTools
import com.boot.core.modToolsCost
import com.boot.core.modToolsBlocker
import com.boot.core.ModPolicy
import com.boot.core.hasModScene
import com.boot.core.modDocsBlocker
import com.boot.core.modDocsCost
import com.boot.core.supportModDocs
import com.boot.core.modKitBlocker
import com.boot.core.modKitCost
import com.boot.core.releaseOfficialModKit
import com.boot.core.clampMods
import com.boot.core.clampModsBlocker
import com.boot.core.partnerModTeam
import com.boot.core.partnerModBlocker
import com.boot.core.partnerModCost
import com.boot.core.ignoreModScene
import com.boot.core.ignoreModSceneBlocker
import com.boot.core.fixBrokenMods
import com.boot.core.fixBrokenModsBlocker
import com.boot.core.modEraStrength
import com.boot.core.moddable
import com.boot.core.releaseUpdate
import com.boot.core.updateCost
import com.boot.core.updateBlocker
import com.boot.core.MAX_UPDATES
import com.boot.core.isCollection
import com.boot.core.Format
import com.boot.core.GameRelease
import com.boot.core.GameState
import com.boot.core.GamesStudio
import com.boot.core.Research
import com.boot.core.Review
import com.boot.core.acknowledgeReview
import com.boot.core.expansionBlocker
import com.boot.core.patchBlocker
import com.boot.core.portBlocker
import com.boot.core.portGame
import com.boot.core.portQuote
import com.boot.core.portTargets
import com.boot.core.runSale
import com.boot.core.saleBlocker
import com.boot.core.patchCost
import com.boot.core.patchGame
import com.boot.core.leagueBlocker
import com.boot.core.leagueCost
import com.boot.core.remasterBlocker
import com.boot.core.startLeague
import com.boot.core.remasterConcept
import com.boot.core.sequelConcept
import com.boot.core.startExpansion
import com.boot.game.GameStore
import androidx.compose.ui.graphics.Color
import java.util.Locale

/** Launch day: the critics give their verdicts one after another, then the players weigh in. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReviewSheet(store: GameStore, state: GameState, releaseId: Int) {
    val r = state.games?.release(releaseId) ?: return
    val sheet = rememberBottomSheetState(initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded))
    ModalBottomSheet(
        onDismissRequest = { store.perform { acknowledgeReview() } },
        sheetState = sheet,
        containerColor = gameColors.grouped,
    ) {
        Scaled {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Hint("The reviews are in")
                    Spacer(Modifier.size(10.dp))
                    GameCover(r.concept, 132.dp, studio = r.developer ?: r.publisher ?: state.companyName)
                    Spacer(Modifier.size(12.dp))
                    Text(r.title, style = MaterialTheme.typography.headlineMediumEmphasized)
                    Spacer(Modifier.size(12.dp))
                    ScoreSummary(r)
                    Spacer(Modifier.size(10.dp))
                    Text(
                        "${r.verdict} · launch week ${Format.money(r.salesHistory.firstOrNull() ?: r.weeklySales)}" +
                            if (r.chartRank > 0) " · #${r.chartRank} on the charts" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Why, before what each critic said.
                if (r.breakdown.isNotEmpty()) ScoreBreakdown(r, top = 4)
                GroupedSection("Critics") {
                    r.reviews.forEachIndexed { i, review ->
                        if (i > 0) RowDivider()
                        // The last critic's ding is followed by a cheer or a groan for the whole game.
                        RevealedReview(review, i, verdict = if (i == r.reviews.lastIndex) verdictFor(r.score) else null, studio = state.games)
                    }
                }
                ProsCons(r.pros, r.cons)
                PlayerReviews(r.playerQuotes)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TonalButton("Game report", {
                        store.openRelease = r.id
                        store.perform { acknowledgeReview() }
                    }, Modifier.weight(1f))
                    PrimaryButton("Continue", { store.perform { acknowledgeReview() } }, Modifier.weight(1f))
                }
            }
        }
    }
}

/** One outlet's score, springing in a beat after the one before. */
@Composable
private fun RevealedReview(review: Review, index: Int, verdict: Sfx? = null, studio: GamesStudio? = null) {
    val reveal = remember { Animatable(0f) }
    val spec = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    val sound = LocalSound.current
    LaunchedEffect(Unit) {
        // A beat longer than the animation alone, so each score gets its own ding.
        kotlinx.coroutines.delay(380L * index)
        sound.review(review.score)
        reveal.animateTo(1f, spec)
        if (verdict != null) {
            kotlinx.coroutines.delay(250)
            sound.play(verdict)
        }
    }
    ReviewRow(
        review,
        Modifier
            .alpha(reveal.value.coerceIn(0f, 1f))
            .scale(0.92f + 0.08f * reveal.value),
        studio = studio,
    )
}

/** Everything about a released game: reviews, sales, and the report that teaches you for next time. */
@Composable
fun ReleaseSheet(store: GameStore, state: GameState, releaseId: Int) {
    val g = state.games ?: return
    val r: GameRelease = g.release(releaseId) ?: return
    val colors = gameColors
    val canSequel = Research.SEQUELS in state.researched && !g.busy && r.developer == null && r.publisher == null && !r.isCollection
    GameDialog(onDismissRequest = { store.openRelease = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = colors.grouped) {
            Column(Modifier.safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { store.openRelease = null }) { Icon(Icons.Filled.Close, "Close") }
                    Text("Game report", style = MaterialTheme.typography.titleMedium)
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.separator))
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        GameCover(r.concept, 108.dp, studio = r.developer ?: r.publisher ?: state.companyName)
                        Column {
                            Text(r.title, style = MaterialTheme.typography.headlineMediumEmphasized)
                            Hint(
                                (if (r.isCollection) "A collection of ${r.concept.collects.size} games · " else "${r.concept.size.title} ${r.concept.topic.title.lowercase()} ${r.concept.genreLower} · ") +
                                    r.concept.platforms.joinToString { it.title } + (r.publisher?.let { " · published by $it" } ?: "") +
                                    (r.developer?.let { " · made by $it, published by you" } ?: ""),
                            )
                            r.publisher?.let {
                                Spacer(Modifier.size(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CompanyLogo(it, size = 28.dp)
                                    Text(it, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { ScoreSummary(r) }
                    GroupedSection("Sales") {
                        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                            TileRow {
                                StatTile("Earned", Format.money(r.lifetimeSales), Modifier.weight(1f), "after the cut")
                                StatTile("Copies", Format.count(r.unitsSold), Modifier.weight(1f))
                                StatTile("Best rank", if (r.bestRank > 0) "#${r.bestRank}" else "—", Modifier.weight(1f))
                            }
                        }
                        if (r.salesHistory.size > 1) CardBlock { AreaChart(r.salesHistory, colors.lane(com.boot.core.Lane.GAMES), Format::money) }
                        RowDivider()
                        ValueRow("Development cost", Format.money(r.devCost))
                        RowDivider()
                        ValueRow(
                            "Profit so far", Format.signedMoney(r.lifetimeSales - r.devCost),
                            if (r.lifetimeSales >= r.devCost) colors.good else colors.bad,
                        )
                    }
                    LiveServiceSection(store, state, r)
                    ShelfSection(r)
                    if (r.reviews.isNotEmpty()) {
                        GroupedSection("Critics") {
                            r.reviews.forEachIndexed { i, review ->
                                if (i > 0) RowDivider()
                                ReviewRow(review, studio = g)
                            }
                        }
                    }
                    AspectBreakdown(r.aspects)
                    ProsCons(r.pros, r.cons)
                    PlayerReviews(r.playerQuotes)
                    if (r.breakdown.isNotEmpty()) ScoreBreakdown(r)
                    GroupedSection(
                        "What you learned",
                        footer = String.format(Locale.US, "Design %.0f · Tech %.0f · Hype %.0f · ", r.design, r.tech, r.hype) +
                            if (r.patched) "patched" else String.format(Locale.US, "%.0f bugs shipped", r.bugs),
                    ) {
                        r.report.forEachIndexed { i, line ->
                            if (i > 0) RowDivider(52.dp)
                            ListRow(line, leading = { Icon(Icons.Filled.Lightbulb, null, tint = colors.warn) })
                        }
                    }
                    AfterLaunch(store, state, r)
                    if (canSequel) {
                        PrimaryButton("Make a sequel", {
                            val seed = store.state?.sequelConcept(r.id)
                            store.openRelease = null
                            store.designGame(seed)
                        }, Modifier.fillMaxWidth())
                    }
                    if (r.developer == null && r.publisher == null && r.concept.remasterOf == null && !r.isCollection && Research.REMASTERS in state.researched) {
                        val blocker = state.remasterBlocker(r.id)
                        TonalButton("Remaster ${r.title}", {
                            val seed = store.state?.remasterConcept(r.id)
                            store.openRelease = null
                            store.designGame(seed)
                        }, Modifier.fillMaxWidth(), enabled = blocker == null && !g.busy)
                        Hint(blocker ?: if (g.busy) "The main team is busy." else "Rebuild it for today's platforms at half the work. Its old fans come back for it.")
                    }
                }
            }
        }
    }
}

/** Keep a game alive after launch: patch it, update it, price it, port it, open it to modders, and sell more to the people who loved it. */
@Composable
private fun AfterLaunch(store: GameStore, state: GameState, r: GameRelease) {
    if (r.publisher != null || r.developer != null || r.isCollection) return
    val patch = state.patchBlocker(r.id)
    val expansion = state.expansionBlocker(r.id)
    GroupedSection(
        "After launch",
        Modifier.testTag("after-launch"),
        footer = if (r.expansions.isEmpty()) null else "Expansions: ${r.expansions.joinToString()}.",
    ) {
        ActionRow(
            if (r.patched) "Patched" else "Release a patch",
            if (r.patched) {
                "Bugs cleared. Player score recovered, and sales held up longer."
            } else {
                patch ?: "Fixes ${r.bugs.toInt()} bugs for ${Format.money(r.patchCost())}. Raises the player score, refreshes sales, and brings live players back."
            },
            if (r.patched) "Done" else "Patch",
            enabled = patch == null,
        ) { store.perform { patchGame(r.id) } }
        RowDivider()
        val update = state.updateBlocker(r)
        ActionRow(
            "Free content update" + if (r.updates > 0) " (${r.updates} of $MAX_UPDATES)" else "",
            update ?: "${Format.money(r.updateCost())}. New things to do: +25% sales, a better player score, and fans who stay. Up to $MAX_UPDATES, half a year apart.",
            "Update",
            enabled = update == null,
        ) { store.perform { releaseUpdate(r.id) } }
        if (r.live == null) {
            RowDivider()
            PriceRow(store, state, r)
        }
        if (Research.ESPORTS in state.researched) {
            RowDivider()
            val league = r.league
            if (league == null) {
                val blocker = state.leagueBlocker(r.id)
                ActionRow(
                    "Start an esports league",
                    blocker ?: "Costs ${Format.money(r.leagueCost())}. Sponsors pay for viewers, and watching keeps people playing.",
                    "Start",
                    enabled = blocker == null,
                ) { store.perform { startLeague(r.id) } }
            } else {
                ValueRow(league.name, if (league.closed) "folded" else "${Format.count(league.viewers)} viewers")
            }
        }
        RowDivider()
        PortRow(store, state, r)
        if (r.moddable() || r.modTools || r.hasModScene) {
            RowDivider()
            ModCommunityRow(store, state, r)
        }
        if (Platform.PC in r.concept.platforms) {
            val addable = state.storesOpen().filter { it !in state.storesOf(r) }
            if (addable.isNotEmpty()) {
                RowDivider()
                ListRow("Stores", subtitle = "On ${state.storesOf(r).joinToString { state.storeName(it) }}.") {
                    Spacer(Modifier.width(8.dp))
                    MenuButton("Add…", addable, { s -> state.storeName(s) + (state.addStoreBlocker(r, s)?.let { " · can't" } ?: " · ${Format.percent(s.cut)} cut") }, enabled = addable.any { state.addStoreBlocker(r, it) == null }) { s ->
                        store.perform { addStore(r.id, s) }
                    }
                }
            }
        }
        RowDivider()
        ListRow(
            "Make an expansion",
            subtitle = expansion ?: "New content the team builds like a small game. Owners of ${r.title} come back for it.",
        ) {
            Spacer(Modifier.width(8.dp))
            MenuButton("Start…", ExpansionScope.entries, { "${it.title} · ${it.blurb}" }, enabled = expansion == null) { scope ->
                store.perform { startExpansion(r.id, scope) }
                store.openRelease = null
            }
        }
    }
}

/** The price, a permanent cut, and sales. */
@Composable
private fun PriceRow(store: GameStore, state: GameState, r: GameRelease) {
    val cuts = PriceCuts.filter { it > r.priceCut + 1e-9 }
    ListRow(
        "Price: ${Format.money(r.price)}" + if (r.priceCut > 0) " (${Format.percent(r.priceCut)} off for good)" else "",
        subtitle = if (r.onSale(state.week)) "On sale at ${Format.percent(r.saleDepth)} off until ${com.boot.core.GameClock.label(r.saleUntil)}." else "Old games sell more cheap. Cut too soon and launch buyers feel cheated.",
    ) {
        Spacer(Modifier.width(8.dp))
        MenuButton("Cut…", cuts, { "${Format.percent(it)} off · ${Format.money(r.concept.unitPrice * (1 - it))}" + (state.priceCutBlocker(r, it)?.let { " · can't" } ?: "") }, enabled = cuts.any { state.priceCutBlocker(r, it) == null }) { cut ->
            store.perform { cutPrice(r.id, cut) }
        }
    }
    RowDivider()
    val sale = state.saleBlocker(r)
    ListRow(
        "Put it on sale",
        subtitle = sale ?: "Two weeks off: more copies now, but some of them would have paid full price later, and the more you run sales the more players wait for them." +
            if (r.saleFatigue > 0.2) " Players are already waiting for the next one." else "",
    ) {
        Spacer(Modifier.width(8.dp))
        MenuButton("Sale…", SaleDepths, { d ->
            val o = state.saleOutlook(r, d)
            "${Format.percent(d)} off · +${Format.percent(o.lift - 1)} now, −${Format.percent(o.pull)} later"
        }, enabled = sale == null) { d -> store.perform { runSale(r.id, d) } }
    }
}

/** Mod tools, workshop culture, and how you treat the community. */
@Composable
private fun ModCommunityRow(store: GameStore, state: GameState, r: GameRelease) {
    val era = state.modEraStrength()
    val mods = state.modToolsBlocker(r)
    ActionRow(
        if (r.modTools) "Mod tools are out" else "Release mod tools",
        if (r.modTools) {
            "Modders keep it selling longer. A scene can form; hit mods and Mod of the Year can follow."
        } else {
            mods ?: "${Format.money(state.modToolsCost(r))}. Players make more of it: sales tail off more slowly, and a community can form."
        },
        if (r.modTools) "Done" else "Release",
        enabled = !r.modTools && mods == null,
    ) { store.perform { releaseModTools(r.id) } }
    if (era <= 0 && !r.hasModScene) {
        Hint("Mod culture hasn't started yet — wait for the mid-nineties on PC.")
        return
    }
    if (r.hasModScene) {
        RowDivider()
        val policy = r.modPolicy.title
        val partner = r.modPartner?.let { " · partner $it" } ?: ""
        val kit = if (r.modKit) " · official kit" else ""
        val broken = if (r.brokenMods) " · broken mods on shelf" else ""
        ValueRow(
            "Mod scene",
            String.format(Locale.US, "buzz %.0f%% · health %.0f%%", r.modBuzz * 100, r.modHealth * 100),
        )
        Hint("$policy$kit$partner$broken. Healthy buzz slows the sales fade; toxic scenes nibble the player score.")
        RowDivider()
        val docs = state.modDocsBlocker(r)
        ActionRow(
            "Support tools & docs",
            docs ?: "${Format.money(state.modDocsCost(r))}. ${ModPolicy.SUPPORT.blurb}",
            "Support",
            enabled = docs == null,
        ) { store.perform { supportModDocs(r.id) } }
        RowDivider()
        val kitBlock = state.modKitBlocker(r)
        ActionRow(
            if (r.modKit) "Official mod kit is out" else "Official mod kit",
            if (r.modKit) "Workshops are using your SDK." else kitBlock ?: "${Format.money(state.modKitCost(r))}. ${ModPolicy.OFFICIAL.blurb}",
            if (r.modKit) "Done" else "Ship kit",
            enabled = !r.modKit && kitBlock == null,
        ) { store.perform { releaseOfficialModKit(r.id) } }
        RowDivider()
        val partnerBlock = state.partnerModBlocker(r)
        ActionRow(
            if (r.modPartner != null) "Partnered with ${r.modPartner}" else "Partner a big mod team",
            if (r.modPartner != null) "Sticky fans and a healthier scene." else partnerBlock ?: "${Format.money(state.partnerModCost(r))}. ${ModPolicy.PARTNER.blurb}",
            if (r.modPartner != null) "Done" else "Partner",
            enabled = r.modPartner == null && partnerBlock == null,
        ) { store.perform { partnerModTeam(r.id) } }
        RowDivider()
        val clamp = state.clampModsBlocker(r)
        ActionRow(
            "Crack down / ban",
            clamp ?: ModPolicy.CLAMP.blurb,
            "Clamp",
            enabled = clamp == null,
        ) { store.perform { clampMods(r.id) } }
        RowDivider()
        val ignore = state.ignoreModSceneBlocker(r)
        ActionRow(
            "Leave the scene alone",
            ignore ?: ModPolicy.IGNORE.blurb,
            "Ignore",
            enabled = ignore == null,
        ) { store.perform { ignoreModScene(r.id) } }
        if (r.brokenMods) {
            RowDivider()
            val fix = state.fixBrokenModsBlocker(r)
            ActionRow(
                "Fix broken storefront mods",
                fix ?: "Pull the junk before it leaks into reviews.",
                "Clean up",
                enabled = fix == null,
            ) { store.perform { fixBrokenMods(r.id) } }
        }
    } else if (r.modTools || r.moddable()) {
        Hint(
            if (r.modTools) "A community scene can form once tools have been out a few weeks."
            else "On PC in later decades, a scene can form on its own around a beloved title — or after you ship tools.",
        )
    }
}

/** Ports: where to, and how well. */
@Composable
private fun PortRow(store: GameStore, state: GameState, r: GameRelease) {
    var quality by remember(r.id) { mutableStateOf(PortQuality.IN_HOUSE) }
    val targets = state.portTargets(r)
    val porting = state.games?.ports?.firstOrNull { it.releaseId == r.id }
    val (portCost, portWeeks) = state.portQuote(r, quality)
    ListRow(
        "Port it",
        subtitle = when {
            porting != null -> "Coming to ${porting.platform.title} ${com.boot.core.GameClock.label(porting.doneWeek)} (${porting.quality.title.lowercase()})." +
                (if (r.exclusiveTo != null && state.week < r.exclusiveUntil) " A ${r.exclusiveTo} exclusive until then." else "")
            r.exclusiveTo != null && r.exclusiveUntil == Int.MAX_VALUE && targets.isEmpty() -> "A ${r.exclusiveTo} exclusive, for good."
            targets.isEmpty() -> "It's on every platform it can go to."
            else -> "${Format.money(portCost)} and ${Format.weeks(portWeeks)}. A big platform whose players like the genre sells best." +
                (if (r.extraPlatforms.isNotEmpty()) " Already on ${r.extraPlatforms.joinToString { it.title }}." else "")
        },
    ) {
        Spacer(Modifier.width(8.dp))
        MenuButton("Port to…", targets, { p ->
            "${p.title} · " + (state.portBlocker(r, p, quality)?.let { "can't" } ?: "sells ~${Format.percent(state.portOutlook(r, p, quality))} of a normal port")
        }, enabled = porting == null && targets.any { state.portBlocker(r, it, quality) == null }) { p ->
            store.perform { portGame(r.id, p, quality) }
        }
    }
    if (porting == null && targets.isNotEmpty()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Segmented(PortQuality.entries, quality, { it.title }) { quality = it }
            Hint(quality.blurb)
        }
    }
}

/**
 * Why a game scored what it did: the baseline, then every part that added or took points off,
 * biggest first. [top] shows only the biggest few, for the verdict screen.
 */
@Composable
fun ScoreBreakdown(r: GameRelease, top: Int? = null) {
    val colors = gameColors
    val baseline = r.breakdown.first()
    val all = r.breakdown.drop(1).sortedByDescending { kotlin.math.abs(it.points) }
    // The verdict screen shows real reasons; the critics' mood is in the full report.
    val shown = top?.let { all.filter { p -> p.label != "Critics' mood" }.take(it) } ?: all
    val total = r.breakdown.sumOf { it.points }.coerceIn(1.0, 10.0)
    GroupedSection(
        if (top != null) "Why it scored ${String.format(Locale.US, "%.1f", r.score)}" else "How the score added up",
        footer = if (top != null && all.size > top) "The full breakdown is in the game report." else
            "Every game starts at ${String.format(Locale.US, "%.1f", baseline.points)}. The critics' average lands near the total, each with their own taste.",
    ) {
        if (top == null) {
            ListRow(baseline.label, subtitle = baseline.note, value = String.format(Locale.US, "%.1f", baseline.points))
            RowDivider()
        }
        shown.forEachIndexed { i, part ->
            if (i > 0) RowDivider()
            ListRow(
                part.label,
                subtitle = part.note.ifEmpty { null },
                value = (if (part.points > 0) "+" else "") + String.format(Locale.US, "%.1f", part.points),
                valueColor = when {
                    part.points > 0 -> colors.good
                    part.points < 0 -> colors.bad
                    else -> Color.Unspecified
                },
            )
        }
        if (top == null) {
            RowDivider()
            ListRow("Total", value = String.format(Locale.US, "%.1f", total))
            RowDivider()
            // The critics' own average, which is what the game scored: near the total, but each has a taste.
            ListRow("What the critics gave it", value = String.format(Locale.US, "%.1f", r.score), valueColor = MaterialTheme.colorScheme.primary)
        }
    }
}
