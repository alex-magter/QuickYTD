package org.alexmagter.QuickYTD

import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.nio.file.Files
import kotlinx.coroutines.*
import java.io.OutputStream


actual fun checkVideo(link: String, onResult: (Boolean) -> Unit){
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
            getData(
                link,
                {

                }
            )
        }
    }


}

actual fun getData(link: String, onResult: (VideoData) -> Unit) {
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
    onProgressChange: (String?) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        val output = runPyScriptFromResInRealTime("download.py", listOf(link, type, extension, resolution), onProgressChange)
    }
}


actual fun download(
    link: String,
    downloadPath: OutputStream,
    type: String,
    extension: String,
    resolution: String,
    onProgressChange: (String?) -> Unit
) {
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

suspend fun runPyScriptFromResInRealTime(fileName: String, args: List<String> = emptyList(), forEveryOutput: (String?) -> Unit): scriptOutput?  {
    val scriptFile = extractScriptFromRes(fileName) ?: return null

    val processBuilder = ProcessBuilder("python3", scriptFile.absolutePath, *args.toTypedArray())
    processBuilder.redirectErrorStream(true)

    return try {
        val process = processBuilder.start()
        val output = mutableListOf<String>()
        val reader = BufferedReader(InputStreamReader(process.inputStream))

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            println("PYTHON >> $line")
            val regex = Regex("""Progreso:\s+([\d.]+%)""")

            val match = regex.find(line as CharSequence)
            if (match != null) {
                val percentage = match.groupValues[1]
                forEveryOutput(percentage)
            }
            output.add(
                line!!)
        }

        process.waitFor()
        return scriptOutput (File(scriptFile.parentFile?.absolutePath ?: ""), output)
    } catch (e: Exception) {
        null
    }
}
