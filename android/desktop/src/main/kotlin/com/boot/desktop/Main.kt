package com.boot.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.boot.desktop.ui.BootDesktopApp
import com.boot.desktop.ui.BootDesktopTheme
import java.io.File

fun main() = application {
    val store = remember {
        DesktopStore(File(System.getProperty("user.home"), ".boot-desktop"))
    }
    var fullscreen by remember { mutableStateOf(true) }
    val windowState = rememberWindowState(
        placement = if (fullscreen) WindowPlacement.Fullscreen else WindowPlacement.Floating,
        size = DpSize(1280.dp, 800.dp),
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "Boot",
        state = windowState,
        undecorated = fullscreen,
        onKeyEvent = { event ->
            if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                if (fullscreen) {
                    fullscreen = false
                    true
                } else {
                    false
                }
            } else if (event.type == KeyEventType.KeyDown && event.key == Key.F11) {
                fullscreen = !fullscreen
                true
            } else {
                false
            }
        },
    ) {
        BootDesktopTheme {
            BootDesktopApp(
                store = store,
                fullscreen = fullscreen,
                onToggleFullscreen = { fullscreen = !fullscreen },
                onExitFullscreen = { fullscreen = false },
            )
        }
    }
}
