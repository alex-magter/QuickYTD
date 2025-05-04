package org.alexmagter.QuickYTD

expect fun getDownloadsFolder(): String

expect suspend fun selectFolder(
    suggestedFileName: String,
    mimeType: String = "*/*",
    onResult: (String?) -> Unit
)