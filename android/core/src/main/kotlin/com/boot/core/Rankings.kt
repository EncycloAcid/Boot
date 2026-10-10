package com.boot.core

import kotlin.math.pow
import kotlin.math.sin

/*
 * Where you stand: a league table of real tech and games companies by value, with you in it once
 * you're big enough to notice. Their values drift with the eras and the years, and wobble a little.
 */

data class RankedCompany(val name: String, val value: Double, val lane: Lane?, val mine: Boolean)

/** Rough values at the start of a run, and the lane each mostly lives in. */
private val Industry = listOf(
    Triple("NVIDIA", 3.5e12, Lane.AI),
    Triple("Apple", 3.4e12, Lane.HARDWARE),
    Triple("Microsoft", 3.2e12, Lane.AI),
    Triple("Alphabet", 2.3e12, Lane.APPS),
    Triple("Amazon", 2.2e12, Lane.WEBSITES),
    Triple("Meta", 1.5e12, Lane.SOCIAL),
    Triple("Tesla", 1.0e12, Lane.HARDWARE),
    Triple("TSMC", 0.9e12, Lane.HARDWARE),
    Triple("Tencent", 0.5e12, Lane.GAMES),
    Triple("OpenAI", 0.5e12, Lane.AI),
    Triple("Netflix", 0.4e12, Lane.APPS),
    Triple("Samsung", 0.35e12, Lane.HARDWARE),
    Triple("xAI", 0.2e12, Lane.AI),
    Triple("Anthropic", 0.18e12, Lane.AI),
    Triple("Sony", 0.15e12, Lane.GAMES),
    Triple("Shopify", 0.15e12, Lane.WEBSITES),
    Triple("Spotify", 0.12e12, Lane.APPS),
    Triple("Nintendo", 0.08e12, Lane.GAMES),
    Triple("Roblox", 0.06e12, Lane.GAMES),
    Triple("Take-Two Interactive", 0.04e12, Lane.GAMES),
    Triple("Electronic Arts", 0.04e12, Lane.GAMES),
    Triple("Reddit", 0.03e12, Lane.SOCIAL),
    Triple("Epic Games", 0.03e12, Lane.GAMES),
    Triple("Pinterest", 0.025e12, Lane.SOCIAL),
    Triple("Duolingo", 0.015e12, Lane.APPS),
    Triple("Snap", 0.015e12, Lane.SOCIAL),
    Triple("Discord", 0.015e12, Lane.SOCIAL),
    Triple("Mistral AI", 0.014e12, Lane.AI),
    Triple("Capcom", 0.012e12, Lane.GAMES),
    Triple("Supercell", 0.01e12, Lane.GAMES),
    Triple("Wix", 0.01e12, Lane.WEBSITES),
    Triple("Squarespace", 0.007e12, Lane.WEBSITES),
    Triple("CD Projekt", 0.006e12, Lane.GAMES),
    Triple("Ubisoft", 0.002e12, Lane.GAMES),
)

/** Everyone, biggest first, you included. */
fun GameState.rankings(): List<RankedCompany> {
    val years = week / 52.0
    // Companies you've bought aren't on the table any more: they're part of you.
    val owned = games?.subsidiaries?.map { if (it.name == "CD Projekt Red") "CD Projekt" else it.name }.orEmpty().toSet()
    val others = Industry.filter { it.first !in owned }.mapIndexed { i, (name, base, lane) ->
        val era = world.era.demand(lane).pow(1.5)
        val wobble = 1 + 0.06 * sin(week / 9.0 + i * 1.7)
        RankedCompany(name, base * era * 1.06.pow(years) * wobble, lane, mine = false)
    }
    val me = RankedCompany(companyName, listing?.marketCap ?: valuation, null, mine = true)
    return (others + me).sortedByDescending { it.value }
}

/** Your place in the table, 1-based. */
val GameState.rank: Int get() = rankings().indexOfFirst { it.mine } + 1

/** The company just above you, the one to beat next. */
val GameState.nextToBeat: RankedCompany?
    get() {
        val all = rankings()
        val i = all.indexOfFirst { it.mine }
        return all.getOrNull(i - 1)
    }
