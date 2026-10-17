package com.boot.core

import kotlinx.serialization.Serializable
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Deterministic, serializable random source. Every run is reproducible from its seed,
 * which keeps saves, tests and balance runs stable.
 */
@Serializable
class SeededRNG(private var state: Long) {
    companion object {
        private const val GOLDEN = -7046029254386353131L // 0x9E3779B97F4A7C15
        private const val MIX1 = -4658895280553007687L // 0xBF58476D1CE4E5B9
        private const val MIX2 = -7723592293110705685L // 0x94D049BB133111EB

        fun fromSeed(seed: Long) = SeededRNG(seed + GOLDEN)
    }

    // SplitMix64
    fun next(): Long {
        state += GOLDEN
        var z = state
        z = (z xor (z ushr 30)) * MIX1
        z = (z xor (z ushr 27)) * MIX2
        return z xor (z ushr 31)
    }

    /** Uniform in [0, 1). */
    fun unit(): Double = (next() ushr 11).toDouble() * (1.0 / (1L shl 53))

    fun chance(p: Double): Boolean = unit() < p

    fun range(lo: Double, hi: Double): Double = lo + (hi - lo) * unit()

    /** Uniform integer in lo..hi, inclusive. */
    fun int(lo: Int, hi: Int): Int = (lo + Math.floorMod(next(), hi.toLong() - lo + 1)).toInt()

    /** Box-Muller normal sample. */
    fun normal(mean: Double = 0.0, sd: Double = 1.0): Double {
        val u1 = max(unit(), 1e-12)
        val u2 = unit()
        return mean + sd * sqrt(-2 * ln(u1)) * cos(2 * Math.PI * u2)
    }

    fun <T> pick(items: List<T>): T = items[int(0, items.size - 1)]

    /** Knuth's Poisson sampler; fine for the small means the sim uses. */
    fun poisson(mean: Double): Int {
        if (mean <= 0) return 0
        val l = exp(-min(mean, 30.0))
        var k = 0
        var p = 1.0
        do {
            k += 1
            p *= unit()
        } while (p > l)
        return k - 1
    }
}

internal fun Double.clamped(lo: Double, hi: Double): Double = min(max(this, lo), hi)

internal fun Int.clamped(lo: Int, hi: Int): Int = min(max(this, lo), hi)

/** Nudge a 0...1 value, staying inside the range. */
internal fun Double.nudged(delta: Double, lo: Double = 0.0, hi: Double = 1.0): Double = (this + delta).clamped(lo, hi)

object GameClock {
    const val WEEKS_PER_YEAR = 52
    const val WEEKS_PER_QUARTER = 13

    private val Months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    /**
     * The year the calendar starts in when the run in play started in the past, so dates read as real
     * ones ("Mar 1983"). Null for a run that starts today, whose dates count years from the start.
     */
    var startYear: Int? = null

    fun year(week: Int) = week / WEEKS_PER_YEAR + 1
    fun weekOfYear(week: Int) = week.mod(WEEKS_PER_YEAR) + 1
    fun label(week: Int): String {
        val start = startYear ?: return "Y${year(week)} W${weekOfYear(week)}"
        return "${Months[(weekOfYear(week) - 1) * 12 / WEEKS_PER_YEAR]} ${start + week.floorDiv(WEEKS_PER_YEAR)}"
    }

    /** "In year 3", or "In 1983" when the calendar shows real dates. */
    fun inYear(week: Int): String = startYear?.let { "In ${it + week.floorDiv(WEEKS_PER_YEAR)}" } ?: "In year ${year(week)}"
}
