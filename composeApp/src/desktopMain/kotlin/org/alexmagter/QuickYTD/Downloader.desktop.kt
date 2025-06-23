package org.alexmagter.QuickYTD

import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.nio.file.Files
import kotlinx.coroutines.*
import java.io.OutputStream


actual fun checkVideo(link: String, ifErrorOccurred: (Exception) -> Unit, onResult: (Boolean) -> Unit){
    CoroutineScope(Dispatchers.IO).launch {
        val output = runPyScriptFromRes("checkVideo.py", listOf(link))
        val isValid : Boolean
        if(output != null){
            isValid = output.data[0] == "valid"
        } else {
            isValid = false
        }

        // Volvemos al hilo principal para actualizar la UI
        withContext(Dispatchers.Main) {
            onResult(isValid)  // Llamamos al callback con los datos obtenidos
        }
    }


}

actual fun getData(link: String, ifErrorOccurred: (Exception) -> Unit, onResult: (VideoData) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        val output = runPyScriptFromRes("getData.py", listOf(link))

        val data = VideoData(output!!.path, link)
        // Volvemos al hilo principal para actualizar la UI
        withContext(Dispatchers.Main) {
            print(output.path.toString())
            onResult(data)
        }
    }
}

actual fun download(
    link: String,
    downloadPath: String,
    type: String,
    extension: String,
    resolution: String,
    onProgressChange: (Float?) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        val scriptFile = extractScriptFromRes("downloadAudio.py") ?: return@launch

        println("$link $extension $resolution $downloadPath")

        val processBuilder = ProcessBuilder("python3", scriptFile.absolutePath, link, extension, resolution, downloadPath)
        processBuilder.redirectErrorStream(true)

        try {
            val process = processBuilder.start()
            val output = mutableListOf<String>()

            Thread {
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?

                while (true) {
                    line = reader.readLine()
                    if (line == null) break  // Si ya no hay más líneas, salimos del bucle
                    onProgressChange(line.toFloat())
                }

            }.start()

            process.waitFor()

        } catch (e: Exception) {
            null
        }
    }
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

fun extractScriptFromRes(fileName: String): File? {
    val inputStream = {}.javaClass.getResourceAsStream("/$fileName") ?: return null
    val tempFile = Files.createTempFile("script_temp", ".py").toFile()
    tempFile.deleteOnExit() // Se eliminará automáticamente al salir

    inputStream.use { input ->
        FileOutputStream(tempFile).use { output ->
            input.copyTo(output)
        }
    }
    return tempFile
}

suspend fun runPyScriptFromRes(fileName: String, args: List<String> = emptyList()): scriptOutput?  {
    val scriptFile = extractScriptFromRes(fileName) ?: return null

    val processBuilder = ProcessBuilder("python3", scriptFile.absolutePath, *args.toTypedArray())
    processBuilder.redirectErrorStream(true)

    return try {
        val process = processBuilder.start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))

        val output = mutableListOf<String>()
        reader.useLines { lines ->
            output.addAll(lines)
        }

        process.waitFor()
        return scriptOutput (File(scriptFile.parentFile?.absolutePath ?: ""), output)
    } catch (e: Exception) {
        null
    }
}