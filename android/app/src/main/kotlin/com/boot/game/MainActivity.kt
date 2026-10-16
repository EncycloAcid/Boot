package com.boot.game

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.boot.game.ui.BootApp
import com.boot.game.ui.BootTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val store = viewModel<GameStore>()
            // Sideways turns with the phone either way round; upright stays upright.
            LaunchedEffect(store.layout) {
                requestedOrientation = when (store.layout) {
                    ScreenLayout.SIDEWAYS -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    ScreenLayout.VERTICAL -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                }
            }
            // Music stops when the game is out of sight and picks up again when it's back.
            LifecycleStartEffect(store) {
                store.sound.foreground(true)
                onStopOrDispose { store.sound.foreground(false) }
            }
            BootTheme {
                BootApp(store)
            }
        }
    }
}
