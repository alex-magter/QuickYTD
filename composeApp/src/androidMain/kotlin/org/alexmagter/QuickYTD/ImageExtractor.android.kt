package org.alexmagter.QuickYTD

import androidx.compose.ui.graphics.ImageBitmap
import java.io.File
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap

actual fun fileToBitmap(image: File): ImageBitmap? {
    val bitmap = BitmapFactory.decodeFile(image.absolutePath)
    return bitmap?.asImageBitmap()
}