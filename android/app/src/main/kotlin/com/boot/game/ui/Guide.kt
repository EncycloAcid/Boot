package com.boot.game.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.boot.core.GameState
import com.boot.core.Lane
import com.boot.core.MENTOR
import com.boot.core.restartTutorial
import com.boot.game.GameStore

/** One page of How to play. [lane] pages come first for the businesses you run. */
data class GuideTopic(val title: String, val summary: String, val paragraphs: List<String>, val lane: Lane? = null)

val guideTopics: List<GuideTopic> = listOf(
    GuideTopic(
        "The basics", "Weeks, money and how a run ends",
        listOf(
            "Time moves in weeks. Tap ▶ to let the calendar run (1×, 2× or 3×), or ⏭ for one week at a time. The clock stops by itself when something needs you: a decision, reviews coming in, a game ready to ship, the account going red.",
            "Money comes in from your businesses and goes out on their costs, payroll and rent. The top bar shows your cash and what you made last week. If your cash stays below zero for eight weeks (twelve on Easy, six on Hard), you're bankrupt.",
            "Decisions arrive two ways. Events stop the clock until you answer. The inbox on the office screen holds opportunities that wait for a few weeks without stopping anything; let one lapse and it's a no.",
            "There's no single way to win. Go public, sell to a rival, stay independent to the end, go bust or get fired by your own board: the ending you get tells the story of how you got there.",
        ),
    ),
    GuideTopic(
        "Games", "A studio in the Game Dev Tycoon mould",
        listOf(
            "Design a game: a topic, one or two genres, an audience, the platforms and a size. Topics suit some genres better than others, and genres suit some platforms. Game reports after each launch tell you what worked, and the designer remembers.",
            "Development runs in three stages with three focus areas each. Different genres want different splits: RPGs care about story and quests, action games about gameplay and level design.",
            "Scores compare a game against your own best, so you have to keep getting better. Hype, fans and the platforms' players drive sales. Avoid launching next to a giant release.",
            "Research unlocks marketing, sequels, bigger games, custom engines, your own publishing label, remasters, live-service games and esports.",
        ),
        Lane.GAMES,
    ),
    GuideTopic(
        "Websites", "An agency: briefs, deadlines and standing",
        listOf(
            "Clients send briefs every week. Take the ones you can finish in time: 40% is paid up front, the rest on delivery. Late work costs standing, and standing is what brings bigger clients.",
            "Your rate decides the work: budget brings more, smaller briefs; premium fewer, bigger ones. When clients change the brief, absorb it (happy clients, thin margins) or bill for it.",
            "Happy clients offer retainers: steady money for a steady slice of your team's week. Now and then a big brand asks for pitches.",
        ),
        Lane.WEBSITES,
    ),
    GuideTopic(
        "Apps", "A retention game",
        listOf(
            "Ship version 1.0, then live on the churn chart and the store rank. Rank brings installs, and installs lift rank.",
            "Churn creeps up the longer you go without an update: choose small, frequent updates or big, rare ones. Monetization trades installs against revenue and churn.",
            "A/B experiments and price levels let you tune it, but nobody knows how an experiment turns out until it's run.",
        ),
        Lane.APPS,
    ),
    GuideTopic(
        "Social media", "Slow, then very fast",
        listOf(
            "Growth is slow until the platform catches on, at around 250,000 people, and then it can go vertical. Servers cost money long before ads pay.",
            "Success brings bots, trolls and scandals. Moderation and ad load are sliders: too little moderation and toxicity drives people away; too many ads and they leave anyway.",
            "Angel investors fund growth curves, so a growing platform can raise money before it earns any.",
        ),
        Lane.SOCIAL,
    ),
    GuideTopic(
        "Hardware", "Supply chains and timing",
        listOf(
            "Design a device at a budget or flagship tier; research and development costs every week. Book factory runs twelve weeks ahead and guess how many you'll sell.",
            "Guess low and you sell out while scalpers profit. Guess high and last year's model sits in a warehouse until it's cleared at 70% off.",
            "Starting here is hard mode: you begin with a seed round and a board that sets targets and can overrule you, or fire you.",
        ),
        Lane.HARDWARE,
    ),
    GuideTopic(
        "AI", "Compute and burn rate",
        listOf(
            "Training runs cost a fortune every week and take months, and you don't know how good the model is until it finishes. Some runs are write-offs.",
            "Each run size has a ceiling, so progress means ever-bigger runs. Rent GPUs (their price swings with the era), buy them, or reserve them for a year at a discount.",
            "Like hardware, starting here means a seed round and a board. The highest ceiling in the game, and the fastest way to go broke.",
        ),
        Lane.AI,
    ),
    GuideTopic(
        "Platform Lab", "Kernels, OEMs and ecosystem",
        listOf(
            "Ship OS versions with a codename and a target (PC, phone, or both as eras unlock). Engineers and researchers build design and tech; bugs pile up if you rush.",
            "A beta delays ship, cuts bugs and raises OEM trust. Shipping raw can force a rollback that freezes partners.",
            "Revenue comes from OEM licences and installed base. Hardware and Apps lanes give optional synergies when you ship.",
        ),
        Lane.OS,
    ),
    GuideTopic(
        "People", "Hiring, morale and offices",
        listOf(
            "Five roles (engineer, designer, researcher, marketer, operations) matter differently to each business. Everyone has a skill, a salary, morale and often a trait: press and hold a trait to see what it does.",
            "People level up with experience, and training (once researched) adds skill. Low morale cuts output and makes people quit; crunch gets more done and burns morale fast.",
            "Every hire needs a desk. Move to a bigger office when you run out: more desks, more rent and room for more perks. Rivals will try to poach your best people.",
        ),
    ),
    GuideTopic(
        "Research", "Points, the tree and history",
        listOf(
            "You earn research points every week, faster with researchers and while making games. Spend them on the Research tab: studio features, engine tech, company upgrades and upgrades for each business.",
            "Every item has the year it was invented, so a run set in the past can't research the future. Each era also has its own research that can only be bought while it lasts, and stays yours for good.",
        ),
    ),
    GuideTopic(
        "Money and investors", "Runway, loans, the board and the stock market",
        listOf(
            "Runway is how many weeks your cash lasts at the current rate. The Company tab shows where money comes from and where it goes.",
            "A bank loan lends against your revenue and value, repaid weekly over a year. Investors buy a slice of the company in rounds; with them comes a board that sets growth targets.",
            "From year five, a business worth $500M with $50M a year in revenue can go public: ring the bell and retire, or stay on as the CEO of a listed company.",
        ),
    ),
    GuideTopic(
        "The Pivot Point and more businesses", "Expanding, pivoting and combos",
        listOf(
            "Between years two and four something forces a choice: a buyout offer, a market crash or a lucky accident. Expand into a second business, pivot (sell the first and go all in) or stay focused.",
            "Once every business you run has been profitable for twelve weeks, you can add another, up to three. Every pair has its own crossover product and its own trouble. Hardware, software and services together make an ecosystem company.",
        ),
    ),
    GuideTopic(
        "Eras and history", "The world changes around you",
        listOf(
            "Eras change demand for each business, salaries, GPU prices and how keen investors are. The era card on the Company tab says what's good and bad right now and what's coming.",
            "Each era has a challenge with a reward, and at its height a one-off decision you'll only get then. When it ends, the paper reviews how you did.",
            "A run can start today or at one of tech's revolutions (1977, 1985, 1995 or 2007) and play through history to the present.",
        ),
    ),
    GuideTopic(
        "Rivals", "They fight back",
        listOf(
            "Each business has a main rival. The more of their market you take, the angrier they get, and an angry rival makes moves: copycats, price wars, poaching, lawsuits and smear campaigns.",
            "You can strike first from the rivals card on each business page. Push a rival weak enough and they leave the market for a year.",
        ),
    ),
    GuideTopic(
        "Goals, achievements and the Hall of Fame", "What you're working towards",
        listOf(
            "Goals run from your first product to being worth a billion, and each pays cash, research points, fans or reputation. The next three are on the office screen.",
            "Achievements are kept for good. Every finished run goes into the Hall of Fame, and some earn legacy perks that give your next company a head start.",
        ),
    ),
    GuideTopic(
        "Scenarios and sandbox", "Other ways to play",
        listOf(
            "A scenario is a fixed start, an objective and a deadline. Meet it in the first half of the time for gold, by three quarters for silver, or by the deadline for bronze.",
            "A sandbox run starts with a billion dollars and can't go bankrupt. It earns no achievements and stays out of the Hall of Fame.",
        ),
    ),
)

/** How to play: a list of topics, your own businesses first, and a page for each. */
@Composable
fun GuideSheet(store: GameStore, state: GameState) {
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    val topics = guideTopics.sortedBy { if (it.lane != null && it.lane in state.lanes) 0 else 1 }
    val topic = topics.firstOrNull { it.title == open }
    val colors = gameColors
    val close = { store.guideOpen = false }
    BackHandler(topic != null) { open = null }
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = colors.grouped) {
            Column(Modifier.safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (topic != null) {
                        IconButton(onClick = { open = null }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                    } else {
                        IconButton(onClick = close) { Icon(Icons.Filled.Close, "Close") }
                    }
                    Text(topic?.title ?: "How to play", style = MaterialTheme.typography.titleMedium)
                }
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    if (topic != null) {
                        GroupedSection(null) {
                            CardBlock(spacing = 12.dp) {
                                topic.paragraphs.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                            }
                        }
                    } else {
                        GroupedSection(null) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                MentorPortrait(40.dp)
                                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                                    Hint(
                                        if (state.tutorial != null) "$MENTOR's lessons are on the office screen. Everything else is here."
                                        else "Want $MENTOR to walk you through it again?",
                                    )
                                }
                                if (state.tutorial == null) {
                                    TonalButton("Lessons", {
                                        store.perform { restartTutorial() }
                                        close()
                                    })
                                }
                            }
                        }
                        GroupedSection("Topics") {
                            topics.forEachIndexed { i, t ->
                                if (i > 0) RowDivider()
                                ListRow(
                                    t.title,
                                    subtitle = t.summary + if (t.lane != null && t.lane in state.lanes) " · your business" else "",
                                    leading = t.lane?.let { l -> { LaneBadge(l, 36.dp) } },
                                    onClick = { open = t.title },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
