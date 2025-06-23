package org.alexmagter.QuickYTD

import android.content.Context
import androidx.compose.material3.AlertDialog
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream

val context = MainApplication.instance

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
    type: String,
    extension: String,
    resolution: String,
    onProgressChange: (Float?) -> Unit
) {
    if(type == "Audio"){

        CoroutineScope(Dispatchers.IO).launch{
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context))
            }

            val py = Python.getInstance()
            val module = py.getModule("download_Android")

            val contextPath = context.filesDir.absolutePath

            println("Vamos a llamar a python")

            module.callAttr("downloadAudio", link, extension, resolution, downloadPath,
                onProgressChange)

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
