package com.boot.core

/** Autopilot many games per starting lane and print outcome stats: `./gradlew :core:balance -Pruns=20`. */
fun main(args: Array<String>) {
    val runs = args.firstOrNull()?.toIntOrNull() ?: 20
    println("Autopilot balance run: $runs games per starting lane\n")
    println("lane       ending distribution                                    median end   peak val: 25% / median / 75%")
    for (lane in Lane.entries) {
        val endings = mutableMapOf<EndingKind, Int>()
        val weeks = mutableListOf<Int>()
        val peaks = mutableListOf<Double>()
        val pivots = mutableMapOf<PivotTrigger, Int>()
        val ecosystems = mutableListOf<Boolean>()
        for (i in 0 until runs) {
            val s = GameState.create("Bot $i", "Bot", lane, seed = 1000L + i)
            Autopilot().playToEnd(s)
            s.ending?.let {
                endings.merge(it.kind, 1, Int::plus)
                weeks += it.week
            }
            peaks += s.stats.peakValuation
            s.pivot?.let { pivots.merge(it.trigger, 1, Int::plus) }
            ecosystems += s.isEcosystem
        }
        weeks.sort()
        peaks.sort()
        val dist = EndingKind.entries.mapNotNull { k -> endings[k]?.let { "${k.name.lowercase()} $it" } }.joinToString(", ")
        val name = lane.name.lowercase().padEnd(10)
        val medianEnd = if (weeks.isEmpty()) "-" else GameClock.label(weeks[weeks.size / 2])
        val q = { f: Double -> Format.money(peaks[(peaks.size * f).toInt().coerceAtMost(peaks.size - 1)]) }
        println("$name ${dist.padEnd(54)} ${medianEnd.padEnd(12)} ${q(0.25)} / ${q(0.5)} / ${q(0.75)}")
        val pivotText = pivots.entries.map { "${it.key.name.lowercase()} ${it.value}" }.sorted().joinToString(", ")
        println("           pivots: $pivotText; ended as ecosystem: ${ecosystems.count { it }}")
    }
}
