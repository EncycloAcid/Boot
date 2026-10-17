package com.boot.game

import com.boot.core.GameState
import org.json.JSONObject

/** How many games can be kept on the go at once. */
const val SLOT_COUNT = 3

/** What a save slot holds, in a line or two, for the title screen. */
data class SlotInfo(
    val number: Int,
    val company: String,
    val date: String,
    val cash: Double,
    val valuation: Double,
    /** Career and its lanes, a scenario's name, or Sandbox. */
    val mode: String,
    /** When it was last played, in milliseconds since 1970. */
    val savedAt: Long,
    val over: Boolean,
) {
    fun encode(): String = JSONObject()
        .put("company", company)
        .put("date", date)
        .put("cash", cash)
        .put("valuation", valuation)
        .put("mode", mode)
        .put("savedAt", savedAt)
        .put("over", over)
        .toString()

    companion object {
        fun decode(number: Int, text: String): SlotInfo? = runCatching {
            val o = JSONObject(text)
            SlotInfo(
                number, o.getString("company"), o.getString("date"), o.getDouble("cash"), o.getDouble("valuation"),
                o.getString("mode"), o.getLong("savedAt"), o.optBoolean("over"),
            )
        }.getOrNull()

        fun of(number: Int, s: GameState, savedAt: Long) = SlotInfo(
            number, s.companyName, s.dateLabel, s.cash, s.valuation,
            when {
                s.sandbox -> "Sandbox"
                s.scenario != null -> s.scenario!!.scenario.title
                else -> s.lanes.joinToString(" + ") { it.businessName }
            },
            savedAt, s.isOver,
        )
    }
}
