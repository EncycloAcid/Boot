package com.boot.game

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
                // Sideways the game takes the whole screen, like a game should: the status and
                // navigation bars hide, and a swipe from the edge brings them back for a moment.
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    if (store.layout == ScreenLayout.SIDEWAYS) {
                        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        hide(WindowInsetsCompat.Type.systemBars())
                    } else {
                        show(WindowInsetsCompat.Type.systemBars())
                    }
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
