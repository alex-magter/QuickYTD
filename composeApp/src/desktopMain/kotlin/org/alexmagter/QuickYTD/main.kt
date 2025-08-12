package org.alexmagter.QuickYTD

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.alexmagter.QuickYTD.FFmpegRunner.checkFFmpeg
import javax.swing.JOptionPane

fun main() = application {
    val windowState = rememberWindowState(
        width = 400.dp,
        height = 700.dp
    )

    if(checkFFmpeg() == false){
        JOptionPane.showMessageDialog(
            null,                 // Componente padre (null = pantalla centrada)
            "FFmpeg wasn't found. Please put a valid binary in the installation folder", // Mensaje
            "Error",              // Título
            JOptionPane.ERROR_MESSAGE // Tipo de mensaje
        )
    } else {
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
}

/*import androidx.compose.material.Text

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Test App") {
        Text("¡Hola desde la App Empaquetada!")
    }
    println("Aplicación Compose iniciada.") // No lo verás, pero es para tu info
}*/