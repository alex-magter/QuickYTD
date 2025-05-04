package org.alexmagter.QuickYTD
import androidx.compose.ui.graphics.ImageBitmap
import java.io.File


expect fun fileToBitmap(image: File): ImageBitmap?

