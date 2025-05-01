package org.example.project

expect fun getDownloadsFolder(): String

expect suspend fun selectFolder(
    suggestedFileName: String,
    mimeType: String = "*/*",
    onResult: (String?) -> Unit
)