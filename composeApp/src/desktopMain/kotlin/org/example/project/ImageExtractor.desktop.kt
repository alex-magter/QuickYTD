package org.example.project

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import org.jetbrains.skia.Color
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import java.io.File
import java.io.FileInputStream

@OptIn(ExperimentalResourceApi::class)
actual fun fileToBitmap(image: File?): ImageBitmap {

    // Si no hay archivo, devolvemos el placeholder directamente
    if (image == null || !image.exists()) {
        println("⚠️ El archivo no existe: ${image?.absolutePath}")
        return createPlaceholderBitmap() // Devuelves el placeholder si el archivo no existe
    }

    println("✔️ Archivo encontrado: ${image.absolutePath}")

    if (image == null) return createPlaceholderBitmap()

    return try {
        FileInputStream(image).use { stream ->
            stream.readAllBytes().decodeToImageBitmap()
        }
    } catch (e: Exception) {
        // Si algo falla (formato inválido, fichero corrupto…), devolvemos el placeholder
        println("Error en la miniatura")
        createPlaceholderBitmap()
    }
}
private fun createPlaceholderBitmap(): ImageBitmap {
    val width = 1280
    val height = 720
    val surface = Surface.makeRasterN32Premul(width, height)
    val canvas = surface.canvas
    val paint = Paint().apply { color = Color.RED }
    canvas.drawRect(Rect.makeXYWH(0f, 0f, width.toFloat(), height.toFloat()), paint)
    return surface.makeImageSnapshot().toComposeImageBitmap()
}