package org.alexmagter.QuickYTD

import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CompletableDeferred
import java.io.OutputStream

actual class FileSaver (private val activity: ComponentActivity){
    actual fun getDownloadsFolder(): String {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return downloadsDir.absolutePath
    }

    private data class SaveAsResult(val uri: Uri?, val outputStream: OutputStream?)

    private var continuation: CompletableDeferred<SaveAsResult?>? = null

    private val createMP4DocumentLauncher = activity.registerForActivityResult(
        ActivityResultContracts.CreateDocument("video/mp4")
    ) { uri ->
        val output = uri?.let {
            activity.contentResolver.openOutputStream(it)
        }
        continuation?.complete(SaveAsResult(uri, output))
        continuation = null
    }

    private val createM4ADocumentLauncher = activity.registerForActivityResult(
        ActivityResultContracts.CreateDocument("audio/mp4")
    ) { uri ->
        val output = uri?.let {
            activity.contentResolver.openOutputStream(it)
        }
        continuation?.complete(SaveAsResult(uri, output))
        continuation = null
    }

    actual suspend fun selectFolder(
        suggestedFileName: String,
        mimeType: String,
        onResult: (outputStream: OutputStream?, pathOrUri: String?, filename: String?) -> Unit
    ) {

        val cleanedName = clearName(suggestedFileName)

        val uri = CompletableDeferred<Uri?>()

        continuation = CompletableDeferred()

        if (mimeType == "m4a"){
            createM4ADocumentLauncher.launch(cleanedName)
        } else {
            createMP4DocumentLauncher.launch(cleanedName)
        }

        val result = continuation?.await()

        val actualFilename = result?.uri?.let { uri ->
            var name: String? = null
            if (uri.scheme == "content") {
                activity.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                        Log.d("SelectFolder", "Nombre final del archivo $name")
                    }
                }
            }
            name ?: suggestedFileName
        }

        println("Uri de android: $uri")
        onResult(result?.outputStream, result?.uri.toString(), actualFilename)
    }
}