package com.boot.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.boot.core.GameSeason
import com.boot.core.GameState
import com.boot.core.Lane
import java.nio.file.Files
import java.nio.file.Path

private val savePath: Path = Path.of(System.getProperty("user.home"), ".boot", "desktop-save.json")

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Boot — Game Tycoon") {
        var state by remember { mutableStateOf(newGame()) }
        var notice by remember { mutableStateOf("Welcome to your studio. Your next decision is yours.") }

        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = Color(0xFF9EE6C1),
                secondary = Color(0xFFB8C7FF),
                background = Color(0xFF101411),
                surface = Color(0xFF1B211D),
            ),
        ) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Row(Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.width(230.dp).fillMaxSize()
                            .background(Color(0xFF171D19)).padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("BOOT", style = MaterialTheme.typography.headlineLarge, color = Color(0xFF9EE6C1))
                        Text("GAME TYCOON", style = MaterialTheme.typography.labelLarge, color = Color.LightGray)
                        Spacer(Modifier.height(16.dp))
                        Text("STUDIO", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        Text(state.companyName, style = MaterialTheme.typography.titleMedium)
                        Text("Founder: " + state.founder.name, color = Color.LightGray)
                        Spacer(Modifier.weight(1f))
                        Button(onClick = {
                            try {
                                state.advance()
                                notice = "Week " + state.week + " closed. Check your finances and studio status."
                            } catch (e: Exception) {
                                notice = e.message ?: "Could not advance the week."
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Advance week  ↵") }
                        OutlinedButton(onClick = {
                            try {
                                Files.createDirectories(savePath.parent)
                                Files.writeString(savePath, state.encode())
                                notice = "Saved to " + savePath.toAbsolutePath()
                            } catch (e: Exception) {
                                notice = "Save failed: " + e.message
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Save game") }
                        OutlinedButton(onClick = {
                            try {
                                if (Files.exists(savePath)) {
                                    state = GameState.decode(Files.readString(savePath))
                                    notice = "Save loaded."
                                } else notice = "No desktop save found yet."
                            } catch (e: Exception) {
                                notice = "Load failed: " + e.message
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Load game") }
                        OutlinedButton(onClick = {
                            state = newGame()
                            notice = "New studio founded. Good luck."
                        }, modifier = Modifier.fillMaxWidth()) { Text("New game") }
                    }

                    Column(
                        modifier = Modifier.weight(1f).fillMaxSize().verticalScroll(rememberScrollState()).padding(30.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Studio overview", style = MaterialTheme.typography.headlineMedium)
                            Text("A desktop-first home for the Boot simulation. This build reuses the shared Kotlin game engine.", color = Color.LightGray)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                            MetricCard("CASH ON HAND", "$" + state.cash.toLong().toString(), "Current studio balance", Modifier.weight(1f))
                            MetricCard("WEEK", state.week.toString(), state.dateLabel, Modifier.weight(1f))
                            MetricCard("TEAM", state.staff.size.toString(), "People on the roster", Modifier.weight(1f))
                        }
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF202A24)), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("SEASONAL MARKET", style = MaterialTheme.typography.labelLarge, color = Color(0xFF9EE6C1))
                                    Text(GameSeason.forWeek(state.week + 1).title, style = MaterialTheme.typography.headlineLarge)
                                    Text(GameSeason.forWeek(state.week + 1).marketTip, color = Color.LightGray)
                                    Text(GameSeason.forWeek(state.week + 1).weeksLabel, color = Color.Gray)
                                }
                                Text("↗", fontSize = 54.sp, color = Color(0xFF9EE6C1))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("GAME DEVELOPMENT", style = MaterialTheme.typography.titleLarge)
                                    Text(state.games?.project?.title ?: "No game currently in production", color = Color.LightGray)
                                    Text(if (state.games?.project != null) "Project in progress — guide its development." else "The desktop foundation is connected to the simulation. Project creation is the next milestone.", color = Color.Gray)
                                }
                            }
                            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("WEEKLY FINANCES", style = MaterialTheme.typography.titleLarge)
                                    Text("Estimated weekly burn: $" + state.weeklyBurn.toLong(), color = Color.LightGray)
                                    Text("Runway: " + if (state.runwayWeeks.isInfinite()) "No projected burn" else state.runwayWeeks.toInt().toString() + " weeks", color = Color.Gray)
                                    Text("Studio valuation: $" + state.valuation.toLong(), color = Color.Gray)
                                }
                            }
                        }
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("STUDIO FEED", style = MaterialTheme.typography.titleLarge)
                                Text(notice, color = Color(0xFF9EE6C1))
                                Spacer(Modifier.height(4.dp))
                                state.log.takeLast(6).reversed().forEach { entry ->
                                    Text("•  " + entry.message, color = Color.LightGray)
                                }
                                if (state.log.isEmpty()) Text("Your studio updates will appear here.", color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, caption: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFF202A24))) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.headlineMedium, color = Color(0xFF9EE6C1))
            Text(caption, style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
        }
    }
}

private fun newGame(): GameState = GameState.create(
    companyName = "Boot Studio",
    founderName = "Founder",
    lane = Lane.GAMES,
)
