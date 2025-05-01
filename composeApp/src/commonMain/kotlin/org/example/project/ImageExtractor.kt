package org.example.project
import androidx.compose.ui.graphics.ImageBitmap
import java.io.File


expect fun fileToBitmap(image: File?): ImageBitmap

