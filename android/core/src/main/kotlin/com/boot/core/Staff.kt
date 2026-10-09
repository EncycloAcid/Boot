package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

@Serializable
enum class Role {
    ENGINEER, DESIGNER, RESEARCHER, MARKETER, OPERATOR;

    val title: String
        get() = when (this) {
            ENGINEER -> "Engineer"
            DESIGNER -> "Designer"
            RESEARCHER -> "Researcher"
            MARKETER -> "Marketer"
            OPERATOR -> "Operations"
        }

    /** Weekly salary at skill 5, before market adjustments. */
    internal val baseSalary: Double
        get() = when (this) {
            ENGINEER -> 2_200.0
            DESIGNER -> 1_900.0
            RESEARCHER -> 3_400.0
            MARKETER -> 1_700.0
            OPERATOR -> 1_400.0
        }
}

@Serializable
data class Employee(
    val id: Int,
    val name: String,
    val role: Role,
    /** 1...10 */
    val skill: Int,
    /** Weekly. */
    var salary: Double,
    /** 0...1. Low morale cuts output and makes people quit. */
    var morale: Double,
    var lane: Lane?,
    val hiredWeek: Int,
) {
    internal val moraleFactor: Double get() = 0.5 + 0.5 * morale

    internal fun output(lane: Lane): Double = skill * lane.weight(role) * moraleFactor
}

@Serializable
data class Candidate(
    val id: Int,
    val name: String,
    val role: Role,
    val skill: Int,
    val askingSalary: Double,
    /** A rival studio is circling. If you don't hire soon, they're gone. */
    val contestedBy: String?,
    val expiresWeek: Int,
)

@Serializable
class Founder(val name: String) {
    /** 0...100. Hits zero and the game ends in burnout. */
    var energy = 100.0

    /** Weeks of hands-on experience per lane. This is the "skills" you keep when you pivot. */
    val experience: MutableMap<Lane, Double> = mutableMapOf()

    /** Share of attention per lane. Normalised when used. */
    val focus: MutableMap<Lane, Double> = mutableMapOf()

    /** Taking outside gigs to pay rent. Steady money, but the company gets a fraction of you. */
    var freelancing = false
    var vacationWeeksLeft = 0

    val isOnVacation: Boolean get() = vacationWeeksLeft > 0

    /** Weekly income from freelance gigs. Grows with experience. */
    fun freelanceIncome(week: Int): Double = 700.0 + 120.0 * min(10, week / 52)

    /** Experience bonus caps at +4 output after ~3 years in a lane. */
    fun skillBonus(lane: Lane): Double = 4 * (1 - exp(-(experience[lane] ?: 0.0) / 120))

    fun attentionShare(lane: Lane, lanes: List<Lane>): Double {
        if (lane !in lanes) return 0.0
        val total = lanes.sumOf { max(focus[it] ?: 1.0, 0.0) }
        if (total <= 0) return 1.0 / lanes.size
        return max(focus[lane] ?: 1.0, 0.0) / total
    }
}

/** What a lane gets from the people pointed at it this week. */
data class Staffing(
    var output: Double = 0.0,
    var marketing: Double = 0.0,
    var operations: Double = 0.0,
    var research: Double = 0.0,
    var headcount: Int = 0,
    var averageSkill: Double = 5.0,
)

internal object Names {
    val first = listOf(
        "Ada", "Bo", "Cass", "Dev", "Eli", "Fern", "Gus", "Hana", "Ines", "Jules", "Kai", "Lena",
        "Milo", "Nia", "Omar", "Pia", "Quinn", "Rafa", "Sol", "Tess", "Umi", "Vik", "Wren", "Xan",
        "Yara", "Zed", "Ari", "Bex", "Cy", "Dana", "Emeka", "Femi", "Gio", "Hal", "Ivy", "Jin",
    )
    val last = listOf(
        "Okafor", "Lindqvist", "Moreau", "Tanaka", "Reyes", "Novak", "Haddad", "Kowalski", "Osei",
        "Park", "Silva", "Ivanova", "Byrne", "Nakamura", "Petrov", "Mensah", "Dubois", "Castillo",
        "Yilmaz", "Akhtar", "Fischer", "Rossi", "Nguyen", "Adeyemi", "Larsen", "Cohen",
    )
    val clients = listOf(
        "Bramble & Co", "Cask Brewery", "Dentally", "Eastside Gym", "Fernhill Florist", "Gourmand Bistro",
        "Harbor Realty", "Inkwell Press", "Juniper Yoga", "Kettle Cafe", "Loom Textiles", "Mason Law",
        "Northwind Travel", "Orchard Clinic", "Pine & Peak Outfitters", "Quarry Climbing", "Riverbend Vet",
        "Saltwater Surf", "Tidy Home Cleaning", "Umber Gallery", "Vela Bikes", "Wick Candles", "Yonder Tours",
        "Zest Juicery", "Acme Logistics", "Bolt Couriers", "Civic Credit Union", "Driftwood Hotels",
    )
    val games = listOf(
        "Moss Knight", "Neon Courier", "Tiny Tactics", "Hollow Lantern", "Pocket Kingdom", "Starfold",
        "Rust & Rhyme", "Deep Shift", "Paper Moon Racing", "Last Orchard", "Glitch Garden", "Cinder Saga",
        "Overclocked", "Salt Lake Raiders", "Velvet Dungeon", "Echo Drift", "Bramblewood", "Hyperlane",
        "Iron Picnic", "Quiet Planet", "Crab Empire", "Sky Laundry", "Mythic Commute", "Boss Rush Hour",
    )
    val apps = listOf(
        "Tally", "Nudge", "Sprout", "Pocketlog", "Habitat", "Driftnote", "Snapcart", "Quill", "Loop",
        "Mealwise", "Stride", "Budgie", "Focusly", "Glimmer", "Plotter", "Waypoint",
    )
    val socials = listOf("Chirp", "Hive", "Murmur", "Campfire", "Loopd", "Echo", "Plaza", "Huddle")
    val devices = listOf("Arc", "Pebble", "Halo", "Nova", "Slate", "Ember", "Prism", "Orbit", "Volt", "Aura")
    val models = listOf("Sage", "Atlas", "Muse", "Lumen", "Oracle", "Scribe", "Echo", "Vector")
}
