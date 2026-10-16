package com.boot.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.boot.core.DISCLOSED_STAKE
import com.boot.core.Format
import com.boot.core.GameState
import com.boot.core.MAX_STAKE
import com.boot.core.Stock
import com.boot.core.TRADING_FEE
import com.boot.core.TakeoverPremiums
import com.boot.core.bidFor
import com.boot.core.buyBlocker
import com.boot.core.buyShares
import com.boot.core.holdingValue
import com.boot.core.hostileOdds
import com.boot.core.maxBuy
import com.boot.core.ownedValue
import com.boot.core.portfolioValue
import com.boot.core.sellCompany
import com.boot.core.sellShares
import com.boot.core.sharePrice
import com.boot.core.stock
import com.boot.core.stocks
import com.boot.core.takeoverBlocker
import com.boot.core.takeoverQuote
import com.boot.game.GameStore

/** Which companies the market list shows. */
private enum class MarketFilter(val title: String) { YOURS("Your markets"), ALL("Everyone"), HELD("Holdings") }

/** The stock market: your shares, the companies you own, raiders, and every company you can trade. */
fun LazyListScope.marketsItems(store: GameStore, state: GameState) {
    item { PortfolioSection(state) }
    state.portfolio.bid?.let { b ->
        item {
            GroupedSection("Your bid", footer = "The money is set aside until shareholders decide. If they say no, you get it back, less 2% in fees.") {
                ListRow(
                    b.name, subtitle = "${Format.percent(b.premium)} over the market · decided ${com.boot.core.GameClock.label(b.decidesWeek)}",
                    value = Format.money(b.cost), valueDetail = "${Format.percent(b.odds)} likely",
                )
            }
        }
    }
    if (state.listing != null) item { ShareholdersSection(state) }
    if (state.portfolio.owned.isNotEmpty()) item { OwnedSection(store, state) }
    item { MarketList(store, state) }
}

@Composable
private fun PortfolioSection(state: GameState) {
    val colors = gameColors
    val p = state.portfolio
    val value = state.portfolioValue
    val cost = p.holdings.sumOf { it.cost }
    val gain = value - cost
    GroupedSection(
        "Your portfolio",
        footer = "Shares follow what each company is worth, the era and the market's mood. Trades cost ${Format.percent(TRADING_FEE, 1)}. " +
            "Companies that have been public ten years pay dividends every quarter.",
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            TileRow {
                StatTile(
                    "Shares", Format.money(value), Modifier.weight(1f),
                    if (p.holdings.isEmpty()) "nothing yet" else Format.signedMoney(gain) + " on paper",
                    if (p.holdings.isEmpty()) androidx.compose.ui.graphics.Color.Unspecified else if (gain >= 0) colors.good else colors.bad,
                )
                StatTile(
                    "Taken", Format.signedMoney(p.realized + p.dividends), Modifier.weight(1f),
                    "sales ${Format.signedMoney(p.realized)} · dividends ${Format.money(p.dividends)}",
                )
            }
        }
    }
}

@Composable
private fun ShareholdersSection(state: GameState) {
    val l = state.listing ?: return
    val p = state.portfolio
    val raid = p.raid
    val equity = state.venture.founderEquity
    GroupedSection(
        "Your shareholders",
        footer = if (equity >= 0.5) "You own over half of ${state.companyName}: nobody can take it from you."
        else "With under half the company, a raider can build a stake and bid for the rest. In a hostile bid there's about a " +
            "${Format.percent(state.hostileOdds)} chance shareholders sell.",
    ) {
        ListRow("You", subtitle = "Founder", value = Format.percent(equity))
        RowDivider()
        ListRow("The public", subtitle = "${l.ticker} on the exchange", value = Format.percent(1 - equity - (raid?.stake ?: 0.0)))
        if (raid != null) {
            RowDivider()
            ListRow(raid.raider, subtitle = if (raid.bidWeek >= 0) "An offer is coming" else "Building a stake", value = Format.percent(raid.stake))
        }
        if (p.pillUntil > state.week) {
            RowDivider()
            ListRow("Poison pill", subtitle = "Raiders kept out until ${com.boot.core.GameClock.label(p.pillUntil)}")
        }
    }
}

@Composable
private fun OwnedSection(store: GameStore, state: GameState) {
    val owned = state.portfolio.owned
    var selling by rememberSaveable { mutableStateOf<String?>(null) }
    GroupedSection(
        "Companies you own · ${Format.money(owned.sumOf { it.lastProfit })}/wk",
        footer = "Each sends home about 5% of its value a year and counts towards yours. Selling one brings in 90% of what it's worth.",
    ) {
        owned.forEachIndexed { i, o ->
            if (i > 0) RowDivider()
            ActionRow(o.name, "Worth ${Format.money(state.ownedValue(o))} · paid ${Format.money(o.paid)} · earned ${Format.money(o.lifetimeProfit)}", "Sell") {
                selling = o.key
            }
        }
    }
    selling?.let { key ->
        val o = owned.firstOrNull { it.key == key } ?: return
        ConfirmDialog(
            title = "Sell ${o.name}?",
            text = "A buyer will pay ${Format.money(state.ownedValue(o) * 0.9)}.",
            confirm = "Sell",
            onConfirm = { store.perform { sellCompany(key) }; selling = null },
            onDismiss = { selling = null },
        )
    }
}

@Composable
private fun MarketList(store: GameStore, state: GameState) {
    var filter by rememberSaveable { mutableStateOf(MarketFilter.YOURS) }
    val all = state.stocks()
    val shown = when (filter) {
        MarketFilter.YOURS -> all.filter { it.lane in state.lanes }.ifEmpty { all }
        MarketFilter.ALL -> all
        MarketFilter.HELD -> all.filter { it.stake > 0 }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Segmented(MarketFilter.entries, filter, { it.title }) { filter = it }
        GroupedSection("The market") {
            if (shown.isEmpty()) {
                CardBlock { Hint("You don't own any shares yet. Tap a company to buy some.") }
            }
            shown.forEachIndexed { i, s ->
                if (i > 0) RowDivider(72.dp)
                StockRow(s) { store.openStock = s.key }
            }
        }
    }
}

@Composable
private fun StockRow(s: Stock, onClick: () -> Unit) {
    val colors = gameColors
    ListRow(
        s.name,
        subtitle = if (s.listed) "${s.ticker} · ${sharePrice(s.price)}" + (if (s.stake > 0) " · you own ${Format.percent(s.stake, if (s.stake < 0.01) 1 else 0)}" else "")
        else "Private",
        leading = { CompanyLogo(s.name, size = 40.dp) },
        value = Format.money(s.value),
        valueDetail = if (s.listed) (if (s.change >= 0) "+" else "") + Format.percent(s.change, 1) else null,
        valueDetailColor = if (s.change >= 0) colors.good else colors.bad,
        onClick = onClick,
    )
}

/** One company: its chart, your stake, buying and selling, and bidding for the lot. */
@Composable
fun StockSheet(store: GameStore, state: GameState, key: String) {
    val s = state.stock(key)
    val colors = gameColors
    val close = { store.openStock = null }
    var bidding by rememberSaveable { mutableStateOf<Double?>(null) }
    GameDialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = colors.grouped) {
            Column(Modifier.safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = close) { Icon(Icons.Filled.Close, "Close") }
                    Text(s?.let { "${it.name} · ${it.ticker}" } ?: "Gone", style = MaterialTheme.typography.titleMedium)
                }
                if (s == null) {
                    Hint("This company isn't on the market any more.", )
                    return@Column
                }
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    val holding = state.portfolio.holdings.firstOrNull { it.key == key }
                    GroupedSection(null) {
                        Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                            TileRow {
                                StatTile(
                                    "Share price", if (s.listed) sharePrice(s.price) else "Private", Modifier.weight(1f),
                                    if (s.listed) (if (s.change >= 0) "+" else "") + Format.percent(s.change, 1) + " in a month" else null,
                                    if (s.change >= 0) colors.good else colors.bad,
                                )
                                StatTile("Worth", Format.money(s.value), Modifier.weight(1f), s.lane?.let { "mostly ${it.title.lowercase()}" })
                            }
                        }
                        state.portfolio.history[key]?.takeIf { it.size > 1 }?.let { h ->
                            CardBlock { AreaChart(h, s.lane?.let { colors.lane(it) } ?: MaterialTheme.colorScheme.primary, { sharePrice(it) }) }
                        }
                    }
                    if (!s.listed) {
                        GroupedSection(null) { CardBlock { Hint("${s.name} is privately held. Its shares don't trade, and its owners aren't selling.") } }
                        return@Column
                    }
                    GroupedSection(
                        "Your shares",
                        footer = "Past ${Format.percent(DISCLOSED_STAKE)} the papers notice, and a rival won't like it. You can own up to " +
                            "${Format.percent(MAX_STAKE)} on the market; beyond that, bid for the company.",
                    ) {
                        if (holding != null) {
                            val worth = state.holdingValue(holding)
                            ListRow(
                                "You own ${Format.percent(s.stake, if (s.stake < 0.01) 2 else 1)}", subtitle = "Paid ${Format.money(holding.cost)}",
                                value = Format.money(worth), valueDetail = Format.signedMoney(worth - holding.cost),
                                valueDetailColor = if (worth >= holding.cost) colors.good else colors.bad,
                            )
                            RowDivider()
                        }
                        CardBlock(spacing = 10.dp) {
                            Text("Buy", style = MaterialTheme.typography.titleSmall)
                            val most = state.maxBuy(key)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (amount in listOf(10_000.0, 100_000.0, 1e6, 1e7, 1e8, 1e9)) {
                                    if (amount > most * 10 && amount > 10_000) continue
                                    TonalButton(Format.money(amount), { store.perform { buyShares(key, amount) } }, enabled = state.buyBlocker(key, amount) == null)
                                }
                                if (most >= 1_000) TonalButton("Max ${Format.money(most)}", { store.perform { buyShares(key, most) } })
                            }
                            if (most < 1_000) Hint(state.buyBlocker(key, 1_000.0) ?: "")
                            if (holding != null) {
                                Text("Sell", style = MaterialTheme.typography.titleSmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TonalButton("Half", { store.perform { sellShares(key, 0.5) } })
                                    TonalButton("All", { store.perform { sellShares(key, 1.0) } })
                                }
                            }
                        }
                    }
                    GroupedSection(
                        "Take it over",
                        footer = "Bid for every share you don't own. Shareholders decide in four weeks: a bigger premium and a bigger stake make a yes likelier. " +
                            "A company you own pays you about 5% of its value a year. Take over a rival and they're out of your market for good.",
                    ) {
                        TakeoverPremiums.forEachIndexed { i, premium ->
                            if (i > 0) RowDivider()
                            val (cost, odds) = state.takeoverQuote(key, premium)!!
                            val blocker = state.takeoverBlocker(key, premium)
                            ActionRow(
                                "${Format.percent(premium)} premium", "${Format.money(cost)} · ${Format.percent(odds)} likely" + (blocker?.let { "\n$it" } ?: ""),
                                "Bid", enabled = blocker == null,
                            ) { bidding = premium }
                        }
                    }
                }
            }
        }
    }
    bidding?.let { premium ->
        val (cost, odds) = state.takeoverQuote(key, premium) ?: return
        ConfirmDialog(
            title = "Bid for ${s?.name}?",
            text = "${Format.money(cost)} is set aside for four weeks. There's about a ${Format.percent(odds)} chance shareholders accept.",
            confirm = "Bid",
            onConfirm = {
                store.perform { bidFor(key, premium) }
                bidding = null
                close()
            },
            onDismiss = { bidding = null },
        )
    }
}
