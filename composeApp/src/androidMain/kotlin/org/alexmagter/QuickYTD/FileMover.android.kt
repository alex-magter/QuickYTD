package org.alexmagter.QuickYTD

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream

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
        try {
            source.renameTo(output)
        } catch (e: Exception) {
            source.copyTo(
                output,
                overwrite = true
            )
            source.delete()
        }
    }

}