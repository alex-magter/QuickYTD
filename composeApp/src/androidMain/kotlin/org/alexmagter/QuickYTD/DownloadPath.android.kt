package org.alexmagter.QuickYTD

import android.os.Environment

actual fun getDownloadsFolder(): String {
    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    return downloadsDir.absolutePath
}

actual suspend fun selectFolder(
    suggestedFileName: String,
    mimeType: String,
    onResult: (String?) -> Unit
) {

}