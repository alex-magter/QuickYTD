package org.alexmagter.QuickYTD

import java.io.File
import java.io.OutputStream

expect class FileMover {
    suspend fun moveFileToOutputStream(
        source: File,
        output: OutputStream
    )

    suspend fun moveFileToFile(
        source: File,
        output: File
    )
}