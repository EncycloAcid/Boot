package com.boot.core

import java.util.Locale
import kotlin.math.abs

object Format {
    private fun f(pattern: String, v: Double) = String.format(Locale.US, pattern, v)

    /** "1 week", "3 weeks". */
    fun weeks(n: Int): String = if (n == 1) "1 week" else "$n weeks"

    /** "a space game", "an ocean game", "a AAA game" (said "triple-A"). */
    fun a(phrase: String): String =
        (if (phrase.firstOrNull()?.lowercaseChar() in "aeiou".toList() && !phrase.startsWith("AAA")) "an " else "a ") + phrase

    /** Two decimals at most, without the trailing zeros: 50, 1.5, 1.25. */
    private fun short(v: Double): String = f("%.2f", v).trimEnd('0').trimEnd('.')

    fun money(v: Double): String {
        val sign = if (v < 0) "-" else ""
        val a = abs(v)
        return when {
            a >= 1e12 -> "$sign\$${short(a / 1e12)}T"
            a >= 1_000_000_000 -> "$sign\$${short(a / 1_000_000_000)}B"
            a >= 1_000_000 -> "$sign\$${short(a / 1_000_000)}M"
            a >= 10_000 -> "$sign\$${f("%.0f", a / 1_000)}k"
            a >= 1_000 -> "$sign\$${f("%.1f", a / 1_000)}k"
            else -> "$sign\$${f("%.0f", a)}"
        }
    }

    /** Money with an explicit + for gains. */
    fun signedMoney(v: Double): String = if (v >= 0) "+${money(v)}" else money(v)

    fun count(v: Double): String = when {
        v >= 1_000_000_000 -> short(v / 1_000_000_000) + "B"
        v >= 1_000_000 -> short(v / 1_000_000) + "M"
        v >= 10_000 -> f("%.0fk", v / 1_000)
        v >= 1_000 -> f("%.1fk", v / 1_000)
        else -> f("%.0f", v)
    }

    fun percent(v: Double, digits: Int = 0): String = f("%.${digits}f", v * 100) + "%"
}

internal fun money(v: Double) = Format.money(v)

internal fun count(v: Double) = Format.count(v)
