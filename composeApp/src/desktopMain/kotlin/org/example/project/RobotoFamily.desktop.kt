package org.example.project

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font

actual fun getRoboto(): FontFamily {
    val robotoFontFamily = FontFamily(
        Font("fonts/Roboto.ttf", FontWeight.Normal),
        Font("fonts/Roboto.ttf", FontWeight.Bold)
    )
    return robotoFontFamily
}