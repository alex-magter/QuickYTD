package org.alexmagter.QuickYTD

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.material3.AlertDialog
import com.chaquo.python.PyException
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream
import androidx.core.net.toUri
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

val context = MainApplication.instance

interface FileCopyProgressListener {
    fun onCopyProgress(bytesCopied: Long, totalBytes: Long)
    fun onCopyComplete(success: Boolean)
}

actual fun getData(link: String, ifErrorOccurred: (Exception) -> Unit, onResult: (VideoData) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch{

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }

        val py = Python.getInstance()
        val module = py.getModule("getData_Android")

        val contextPath = context.filesDir.absolutePath

        val result: String

        try {
            result = module.callAttr("startScript", link, contextPath).toString()
        } catch (e: Exception) {
            ifErrorOccurred(e)
            return@launch
        }

        val path = File(contextPath)
        val output = VideoData(path, link)
        withContext(Dispatchers.Main) {
            onResult(output)
        }
    }
}

actual fun checkVideo(link: String, ifErrorOccurred: (Exception) -> Unit, onResult: (Boolean) -> Unit){
    if (!Python.isStarted()) {
        Python.start(AndroidPlatform(context))
    }

    val py = Python.getInstance()
    val module = py.getModule("checkVideo_Android")

    val result: Boolean

    try {
        result = module.callAttr("checkVideo", link).toBoolean()
    } catch (e: Exception){
        ifErrorOccurred(e)
        return
    }

    val isValid : Boolean = result

    onResult(isValid)
}

actual fun download(
    link: String,
    downloadPath: String,
    filename: String,
    type: String,
    extension: String,
    resolution: String,
    savedAs: Boolean,
    onResult: (Boolean) -> Unit,
    onProgressChange: (Float, String) -> Unit
) {

    val tempDir = if(savedAs) context.cacheDir else File(downloadPath)
    val tempFileName = if(savedAs) "temp_media_${System.currentTimeMillis()}.tmp" else "$filename.$extension"
    val tempFile = File(tempDir, tempFileName)

    val tempFilePath = tempFile.absolutePath



    if(type == "Audio"){

        CoroutineScope(Dispatchers.IO).launch{
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context))
            }

            val py = Python.getInstance()
            val module = py.getModule("download_Android")

            println("Vamos a llamar a python")

            try {
                module.callAttr("downloadAudio", link, extension, resolution, tempDir.toString(), tempFileName,
                    { println("Callback progreso inline") })
            } catch (e: PyException) {
                Log.e("DownloadError", "Error al descargar: ${e.message}", e)
                tempFile.delete()

                withContext(Dispatchers.Main){
                    onResult(false)
                }
                return@launch

            }

            if (!savedAs) { onResult(true); return@launch }

            if(!tempFile.exists() || tempFile.length() == 0L) {
                Log.e("FileCopy", "El archivo temporal no existe o esta vacío: ${tempFile.absolutePath}")
                tempFile.delete()
                withContext(Dispatchers.Main){
                    onResult(false)
                }
                return@launch
            }

            Log.d("FileCopy", "Iniciando copia desde: ${tempFile.absolutePath} hacia URI $downloadPath")

            var success = false

            val totalBytesToCopy = tempFile.length()

            try {
                context.contentResolver.openInputStream(downloadPath.toUri())?.use {
                    // Comprobamos si el destino es escribible
                }

                val outputStream: OutputStream? = context.contentResolver.openOutputStream(downloadPath.toUri())
                if(outputStream == null){
                    Log.e("FileCopy", "No se pudo abrir OutputStream para la Uri: $downloadPath")
                    withContext(Dispatchers.Main){
                        onResult(false)
                    }
                    return@launch
                }

                val inputStream: InputStream = FileInputStream(tempFile)

                /*outputStream.use { os ->
                    inputStream.use{ ins ->
                        Log.d("FileCopy", "Copiando datos...")

                        val bufferSize = 8 /* Size in kb */ * 1024

                        val bytesCopied = ins.copyTo(os, bufferSize)
                        os.flush()
                        Log.d("FileCopy", "Copia completada. Se han copiado ${bytesCopied / 1024 / 1024}MB")
                        success = true
                    }
                }*/

                val bufferSize = 8 /* Size in kb */ * 1024
                val buffer = ByteArray(bufferSize) // Bytes que se van a escribir en esta iteracion
                var bytesRead: Int // Numero de bytes que se van a escribir en esta iteracion
                var lastReportedPercentage = -1
                var bytesCopiedSoFar = 0L

                while (true){
                    bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break

                    outputStream.write(buffer, 0, bytesRead)
                    bytesCopiedSoFar += bytesRead

                    val currentPercentage = ((bytesCopiedSoFar * 100) / totalBytesToCopy).toInt()
                    if (currentPercentage > lastReportedPercentage || bytesCopiedSoFar == totalBytesToCopy) {
                        withContext(Dispatchers.Main){
                            onProgressChange(currentPercentage.toFloat(), "Copying file to the desired ubication...")
                        }
                        lastReportedPercentage = currentPercentage
                    }
                }
            } catch (e: IOException) {
                Log.e("FileCopy", "I/O Error during copy", e)
                success = false
            } catch (e: SecurityException) {
                Log.e("FileCopy", "Security error during copy")
                success = false
            } catch (e: Exception) {
                Log.e("FileCopy", "Unexpected error during copy")
            } finally {
                if(tempFile.exists()) {
                    Log.d("FileCopy", "Deleting temporal file: ${tempFile.absolutePath}")
                    if(!tempFile.delete()) {
                        Log.w("FileCopy", "Cant delete temp file: ${tempFile.absolutePath}")
                    }
                }

                withContext(Dispatchers.Main){
                    onResult(success)
                }
            }
        }
    } else return
}

actual fun download(
    link: String,
    downloadPath: OutputStream,
    type: String,
    extension: String,
    resolution: String,
    onProgressChange: (String?) -> Unit
){

}
