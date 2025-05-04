package org.alexmagter.QuickYTD

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

actual fun getRoboto(): FontFamily {
    return FontFamily(
        Font(R.font.roboto, FontWeight.Normal),
        Font(R.font.roboto, FontWeight.Bold)
    )
}