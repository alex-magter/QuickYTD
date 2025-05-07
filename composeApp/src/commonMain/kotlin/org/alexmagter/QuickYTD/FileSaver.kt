package org.alexmagter.QuickYTD

import java.io.OutputStream

expect class FileSaver {

    fun getDownloadsFolder(): String

    suspend fun selectFolder(
        suggestedFileName: String,
        mimeType: String = "*/*",
        onResult: (OutputStream?) -> Unit
    )
}