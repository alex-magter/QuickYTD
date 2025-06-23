package org.alexmagter.QuickYTD

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.System.getProperty
import java.nio.file.Paths
import javax.swing.JFileChooser

actual class FileSaver {
    actual fun getDownloadsFolder(): String {
        val userHome = getProperty("user.home")
        return Paths.get(userHome, "Downloads").toString()
    }

    actual suspend fun selectFolder(
        suggestedFileName: String,
        mimeType: String,
        onResult: (outputStream: OutputStream?, pathOrUri: String?) -> Unit
    ) {

        withContext(Dispatchers.IO) {
            val cleanedName = clearName(suggestedFileName)

            val chooser = JFileChooser()
            chooser.selectedFile = File(cleanedName)
            val result = chooser.showSaveDialog(null)
            val stream = if (result == JFileChooser.APPROVE_OPTION) {
                FileOutputStream(chooser.selectedFile)
            } else null

            val filePath = chooser.selectedFile.absolutePath

            onResult(stream, File(filePath).parent)
        }
    }

}