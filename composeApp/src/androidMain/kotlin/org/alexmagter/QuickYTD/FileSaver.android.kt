package org.alexmagter.QuickYTD

import android.net.Uri
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CompletableDeferred
import java.io.OutputStream

actual class FileSaver (private val activity: ComponentActivity){
    actual fun getDownloadsFolder(): String {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return downloadsDir.absolutePath
    }

    var continuation: CompletableDeferred<OutputStream?>? = null

    val createDocumentLauncher = activity.registerForActivityResult(
        ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        val output = uri?.let {
            activity.contentResolver.openOutputStream(it)
        }
        continuation?.complete(output)
        continuation = null
    }

    actual suspend fun selectFolder(
        suggestedFileName: String,
        mimeType: String,
        onResult: (outputStream: OutputStream?, pathOrUri: String?) -> Unit
    ) {

        val uri = CompletableDeferred<Uri?>()

        continuation = CompletableDeferred()

        createDocumentLauncher.launch(suggestedFileName)

        val selectedUri = uri.await()

        val outputStream = selectedUri?.let {
            context.contentResolver.openOutputStream(it)
        }

        val output = continuation?.await()
        onResult(output, uri.toString())
    }
}