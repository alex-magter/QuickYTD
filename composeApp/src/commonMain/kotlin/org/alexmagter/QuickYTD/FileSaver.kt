package org.alexmagter.QuickYTD

import java.io.OutputStream

expect class FileSaver {

    fun getDownloadsFolder(): String

    suspend fun selectFolder(
        suggestedFileName: String,
        mimeType: String = "*/*",
        onResult: (outputStream: OutputStream?, pathOrUri: String?, filename: String?) -> Unit
    )
}

fun clearName(name: String): String{
    val prohibitedCharacters = "[\\\\/:*?\"<>|]"
    return name.replace(Regex(prohibitedCharacters), "")
}