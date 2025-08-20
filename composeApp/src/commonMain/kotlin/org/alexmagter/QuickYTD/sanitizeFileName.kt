package org.alexmagter.QuickYTD

fun sanitizeFileName(fileName: String): String {
    // Definimos los caracteres prohibidos
    val forbidden = "[<>:\"/\\\\|?*]".toRegex()
    // Los reemplazamos por nada (los eliminamos)
    return fileName.replace(forbidden, "")
}