package com.boot.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.boot.game.GameStore
import com.boot.game.audio.Sfx

/** The music and effects volumes, as two sliders. */
@Composable
fun SoundSliders(store: GameStore) {
    val sound = store.sound
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        VolumeRow("Music", sound.musicVolume, onChange = sound::setMusic)
        VolumeRow("Effects", sound.effectsVolume, onChange = sound::setEffects) {
            // A taste of the new level when the slider is let go.
            sound.play(Sfx.COIN)
        }
    }
}

@Composable
private fun VolumeRow(label: String, value: Float, onChange: (Float) -> Unit, onDone: () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(72.dp))
        Slider(
            value = value,
            onValueChange = onChange,
            onValueChangeFinished = onDone,
            modifier = Modifier.weight(1f).semantics { contentDescription = "$label volume" },
        )
        Text(
            if (value <= 0f) "Off" else "${(value * 100).toInt()}%",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.width(48.dp).padding(start = 8.dp),
        )
    }
}

/** Sound and music settings, from the menu. */
@Composable
fun SoundDialog(store: GameStore) {
    AlertDialog(
        onDismissRequest = { store.soundOpen = false },
        title = { Text("Sound and music") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SoundSliders(store)
                Hint("Every era has its own tune, composed by the game itself. Music stays quiet if another app is already playing some.")
            }
        },
        confirmButton = { TextButton(onClick = { store.soundOpen = false }) { Text("Done") } },
    )
}
