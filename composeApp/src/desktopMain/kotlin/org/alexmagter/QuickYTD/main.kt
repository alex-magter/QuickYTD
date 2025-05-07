package org.alexmagter.QuickYTD

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    val windowState = rememberWindowState(
        width = 400.dp,
        height = 700.dp
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "QuickYTD",
        state = windowState,
        resizable = true
    ) {
        LaunchedEffect(Unit) {
            window.minimumSize = java.awt.Dimension(400, 700)
        }

        Navigation(FileSaver())
    }
}