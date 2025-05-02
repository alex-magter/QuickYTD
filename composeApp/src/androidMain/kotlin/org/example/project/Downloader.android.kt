package org.example.project

import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import java.io.File

actual fun getData(link: String, onResult: (VideoData) -> Unit) {
}

actual fun checkVideo(link: String, onResult: (Boolean) -> Unit){
    
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

fun runPythonCheckVideo(context: Context, videoUrl: String, onResult: (Boolean) -> Unit): File? {
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
}