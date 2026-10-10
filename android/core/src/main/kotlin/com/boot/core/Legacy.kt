package com.boot.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/*
 * The Hall of Fame. Every run that ends (an IPO, a sale, staying indie, going bust, being pushed
 * out) is remembered: who, what, how long and for how much. What you've done before follows you
 * into the next company as a small head start: some cash from the last exit, a few research
 * points, a contact or two.
 */

/** One finished run, as the Hall of Fame keeps it. */
@Serializable
data class RunRecord(
    val company: String,
    val founder: String,
    val lane: Lane,
    val kind: EndingKind,
    val title: String,
    val week: Int,
    val valuation: Double,
    val takeHome: Double,
    val ecosystem: Boolean,
    val difficulty: Difficulty,
    val achievements: Int,
    /** The run's seed: a run that ends, plays on and ends again keeps one entry. */
    val seed: Long = 0,
)

/** This run as a Hall of Fame entry, once it has ended. */
fun GameState.runRecord(): RunRecord? = ending?.let { e ->
    RunRecord(
        company = companyName, founder = founder.name, lane = startingLane, kind = e.kind, title = e.title, week = e.week,
        valuation = e.valuation, takeHome = e.founderTakeHome, ecosystem = e.ecosystem, difficulty = difficulty, achievements = achievements.size,
        seed = seed,
    )
}

/** Head starts earned in earlier runs. */
@Serializable
enum class LegacyPerk(val title: String, val detail: String) {
    BEEN_HERE_BEFORE("Been here before", "You've done this once. Start with $10k more."),
    SERIAL_FOUNDER("Serial founder", "Five companies in. Another $15k and 10 research points."),
    IPO_VETERAN("IPO veteran", "You've rung the bell. A star candidate is waiting on day one."),
    SURVIVOR("Survivor", "You went bust once and learned from it. Start with experience in your lane."),
    ECOSYSTEM_BUILDER("Ecosystem builder", "You've built hardware, software and services together. Start with 20 research points."),
    BILLIONAIRE("Billion-dollar founder", "You've built a unicorn. The espresso machine comes with you."),
    ;

    internal fun earned(records: List<RunRecord>): Boolean = when (this) {
        BEEN_HERE_BEFORE -> records.isNotEmpty()
        SERIAL_FOUNDER -> records.size >= 5
        IPO_VETERAN -> records.any { it.kind == EndingKind.IPO }
        SURVIVOR -> records.any { it.kind == EndingKind.BANKRUPT }
        ECOSYSTEM_BUILDER -> records.any { it.ecosystem }
        BILLIONAIRE -> records.any { it.valuation >= 1e9 }
    }

    /** How far along you are, for the perks you haven't earned yet. */
    fun progress(records: List<RunRecord>): String = when (this) {
        BEEN_HERE_BEFORE -> "Finish a run."
        SERIAL_FOUNDER -> "Finish five runs (${minOf(records.size, 5)} of 5)."
        IPO_VETERAN -> "Take a company public."
        SURVIVOR -> "Go bust once."
        ECOSYSTEM_BUILDER -> "End a run as an ecosystem company."
        BILLIONAIRE -> "End a run worth a billion."
    }

    companion object {
        fun earned(records: List<RunRecord>): List<LegacyPerk> = entries.filter { it.earned(records) }
    }
}

/** The Hall of Fame's ranking: biggest valuations first. */
fun List<RunRecord>.ranked(): List<RunRecord> = sortedByDescending { it.valuation }

internal fun GameState.applyLegacy(legacy: List<LegacyPerk>) {
    if (legacy.isEmpty()) return
    for (p in legacy) {
        when (p) {
            LegacyPerk.BEEN_HERE_BEFORE -> cash += 10_000
            LegacyPerk.SERIAL_FOUNDER -> {
                cash += 15_000
                researchPoints += 10
            }
            LegacyPerk.IPO_VETERAN -> candidates = candidates + makeCandidate(star = true)
            LegacyPerk.SURVIVOR -> founder.experience[startingLane] = (founder.experience[startingLane] ?: 0.0) + 40
            LegacyPerk.ECOSYSTEM_BUILDER -> researchPoints += 20
            LegacyPerk.BILLIONAIRE -> perks = perks + Perk.COFFEE
        }
    }
    addLog("Legacy: ${legacy.joinToString { it.title.lowercase() }}.", LogTone.GOOD)
}

/** Add [record] to the Hall of Fame, replacing an earlier ending of the same run. */
fun List<RunRecord>.with(record: RunRecord): List<RunRecord> =
    filterNot { it.seed == record.seed && it.company == record.company && it.founder == record.founder } + record

private val hallJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

fun encodeHall(records: List<RunRecord>): String = hallJson.encodeToString(ListSerializer(RunRecord.serializer()), records)

fun decodeHall(text: String): List<RunRecord> = hallJson.decodeFromString(ListSerializer(RunRecord.serializer()), text)
