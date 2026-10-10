package com.boot.core

import kotlinx.serialization.Serializable

/** The six kinds of business you can run. Each one plays like a different management game. */
@Serializable
enum class Lane {
    GAMES, WEBSITES, APPS, SOCIAL, HARDWARE, AI;

    val title: String
        get() = when (this) {
            GAMES -> "Games"
            WEBSITES -> "Websites"
            APPS -> "Apps"
            SOCIAL -> "Social Media"
            HARDWARE -> "Hardware"
            AI -> "AI"
        }

    val businessName: String
        get() = when (this) {
            GAMES -> "Game Studio"
            WEBSITES -> "Web Agency"
            APPS -> "App Studio"
            SOCIAL -> "Social Platform"
            HARDWARE -> "Device Lab"
            AI -> "AI Lab"
        }

    val pitch: String
        get() = when (this) {
            GAMES -> "Hit-driven and volatile. Long dev cycles, launch-week spikes, review scores, and a fanbase that never forgets a flop."
            WEBSITES -> "An agency sim. Steady contract money, clients who change the brief at 11pm, and scope creep. Low risk, low ceiling."
            APPS -> "A retention game. Launches are small, then you live or die by churn charts, store rankings, and how often you ship updates."
            SOCIAL -> "A slow start that turns exponential if it catches on. Server costs outrun revenue early; success brings bots, moderation fires, and press scandals."
            HARDWARE -> "Phones, laptops and headphones. Book factory capacity months ahead and guess how many units to make. Huge margins if you nail it."
            AI -> "Compute and burn rate. Training runs cost a fortune and you don't know how good the model is until it finishes. Highest ceiling, fastest way to go broke."
        }

    /** Hardware and AI need big money up front. Starting in them means taking venture capital. */
    val isCapitalIntensive: Boolean get() = this == HARDWARE || this == AI

    /** Cash you need on hand to open this lane without raising a round. */
    val selfFundThreshold: Double
        get() = when (this) {
            GAMES, WEBSITES, APPS -> 15_000.0
            SOCIAL -> 40_000.0
            HARDWARE -> 1_500_000.0
            AI -> 3_000_000.0
        }

    /** One-off cost to stand the business up when you add it as a second or third lane. */
    val setupCost: Double
        get() = when (this) {
            GAMES -> 10_000.0
            WEBSITES -> 3_000.0
            APPS -> 8_000.0
            SOCIAL -> 25_000.0
            HARDWARE -> 600_000.0
            AI -> 1_200_000.0
        }

    /** Revenue multiple used when valuing the business. */
    val revenueMultiple: Double
        get() = when (this) {
            GAMES -> 2.0
            WEBSITES -> 1.0
            APPS -> 4.0
            SOCIAL -> 6.0
            HARDWARE -> 3.0
            AI -> 12.0
        }

    /** How much each role contributes to this lane's output. */
    fun weight(role: Role): Double = when (this) {
        GAMES -> when (role) {
            Role.ENGINEER -> 1.0; Role.DESIGNER -> 1.2; Role.RESEARCHER -> 0.3; Role.MARKETER -> 0.4; Role.OPERATOR -> 0.3
        }
        WEBSITES -> when (role) {
            Role.ENGINEER -> 1.0; Role.DESIGNER -> 1.0; Role.RESEARCHER -> 0.2; Role.MARKETER -> 0.5; Role.OPERATOR -> 0.4
        }
        APPS -> when (role) {
            Role.ENGINEER -> 1.2; Role.DESIGNER -> 0.9; Role.RESEARCHER -> 0.3; Role.MARKETER -> 0.4; Role.OPERATOR -> 0.4
        }
        SOCIAL -> when (role) {
            Role.ENGINEER -> 1.0; Role.DESIGNER -> 0.6; Role.RESEARCHER -> 0.3; Role.MARKETER -> 0.5; Role.OPERATOR -> 0.8
        }
        HARDWARE -> when (role) {
            Role.ENGINEER -> 1.3; Role.DESIGNER -> 1.0; Role.RESEARCHER -> 0.6; Role.MARKETER -> 0.4; Role.OPERATOR -> 0.6
        }
        AI -> when (role) {
            Role.ENGINEER -> 0.7; Role.DESIGNER -> 0.3; Role.RESEARCHER -> 1.5; Role.MARKETER -> 0.4; Role.OPERATOR -> 0.4
        }
    }
}

/** How the second (or third) lane is taken on at the Pivot Point. */
@Serializable
enum class PivotMode {
    /** Run both lanes with shared staff and cash, attention split between them. */
    EXPAND,

    /** Sell the first business, take the lump sum and your skills, go all in on the new one. */
    PIVOT,

    /** Turn it all down and keep doing the one thing. A second lane can still come later. */
    STAY;

    val title: String
        get() = when (this) {
            EXPAND -> "Expand"
            PIVOT -> "Pivot"
            STAY -> "Stay focused"
        }

    val summary: String
        get() = when (this) {
            EXPAND -> "Run both lanes with shared staff and cash. Your attention is split between them."
            PIVOT -> "Sell the first business, take the lump sum and your skills, and go all in on the new one."
            STAY -> "Keep doing the one thing you do. You can still add a lane later, once the business is profitable."
        }

    companion object {
        /** The ways to take on a second lane. */
        val intoNewLane = listOf(EXPAND, PIVOT)
    }
}
