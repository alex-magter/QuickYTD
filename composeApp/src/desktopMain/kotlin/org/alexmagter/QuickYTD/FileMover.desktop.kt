package org.alexmagter.QuickYTD

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

actual class FileMover {
    actual suspend fun moveFileToOutputStream(source: File, output: OutputStream) {
        withContext(Dispatchers.IO) {
            source.inputStream().use {
                    inputStream ->
                output.use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            source.delete()
        }
    }

    actual suspend fun moveFileToFile(source: File, output: File) {
        withContext(Dispatchers.IO) {
            try {
                Files.move(
                    source.toPath(),
                    output.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            } catch (e: Exception) {
                // If NIO move fails (less likely on desktop but possible),
                // fall back to copy and delete
                source.copyTo(output, overwrite = true)
                source.delete()
            }
        }
    }

}