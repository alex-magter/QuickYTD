package org.example.project

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

val context = MainApplication.instance

actual fun getData(link: String, onResult: (VideoData) -> Unit) {
    CoroutineScope(Dispatchers.Main).launch{

        val path = runPythonGetData(context, link)

        val output = VideoData(path, link)

        withContext(Dispatchers.Main) {
            onResult(output)
        }
    }
}

actual fun checkVideo(link: String, onResult: (Boolean) -> Unit){
    runPythonCheckVideo(context, link, onResult)
}

actual fun download(
    link: String,
    downloadPath: String,
    type: String,
    extension: String,
    resolution: String,
    onProgressChange: (String?) -> Unit
) {
}

private fun runPythonCheckVideo(context: Context, videoUrl: String, onResult: (Boolean) -> Unit): Unit {
    if (!Python.isStarted()) {
        Python.start(AndroidPlatform(context))
    }

    val py = Python.getInstance()
    val module = py.getModule("checkVideo_Android")

    val result = module.callAttr("checkVideo", videoUrl).toBoolean()

    val isValid : Boolean = when (result) {
        is Boolean -> result
        else -> false
    }

    onResult(isValid)

}

private suspend fun runPythonGetData(context: Context, videoUrl: String): File {



    if (!Python.isStarted()) {
        Python.start(AndroidPlatform(context))
    }

    val py = Python.getInstance()
    val module = py.getModule("getData_Android")

    val contextPath = context.filesDir.absolutePath

    val result = module.callAttr("startScript", videoUrl, contextPath).toString()

    return File(contextPath)
}

/*fun runPythonGetData(context: Context, videoUrl: String, onResult: (Boolean) -> Unit): File? {
    if (!Python.isStarted()) {
        Python.start(AndroidPlatform(context))
    }

    val outputDir = File(context.filesDir, "yt_output")
    if (!outputDir.exists()) outputDir.mkdirs()

    val py = Python.getInstance()
    val module = py.getModule("getData") // sin .py
    val result = module.callAttr("get_data", videoUrl, outputDir.absolutePath)

    val outputPath = result.toString()
    val outputFile = File(outputPath, "streams.csv")

    return if (outputFile.exists()) outputFile else null
}*/