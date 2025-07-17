package org.alexmagter.QuickYTD

import com.sun.source.tree.TryTree
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.nio.file.Files
import kotlinx.coroutines.*
import java.io.OutputStream


actual fun checkVideo(link: String, ifErrorOccurred: (Exception) -> Unit, onResult: (Boolean) -> Unit){
    println("JAVA_HOME = ${System.getenv("JAVA_HOME")}")

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

var Cancelling = false
var downloadProcess: Process? = null
var outputFile: File? = null

actual fun cancelDownload(){
    Cancelling = true

    downloadProcess?.destroy()

    CoroutineScope(Dispatchers.IO).launch {
        delay(100)
        outputFile?.delete()
    }


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
    onProgressChange: (Double, String) -> Unit
) {
    Cancelling = false

    CoroutineScope(Dispatchers.IO).launch {

        if (type == "Video"){
            downloadVideo(
                link = link,
                downloadPath = downloadPath,
                fileName = filename,
                extension = extension,
                resolution = resolution,
                savedAs = savedAs,
                onResult = onResult,
                onProgressChange = onProgressChange
            )
            return@launch
        }

        val scriptFile = when (type){
            "Audio" -> extractScriptFromRes("downloadAudio.py")
            "Video (muted)" -> extractScriptFromRes("downloadVideoMuted.py")
            else -> return@launch
        } ?: return@launch

        println("$link $extension $resolution $downloadPath $filename")

        outputFile = File(downloadPath, "$filename.$extension")

        val processBuilder = ProcessBuilder("python3", scriptFile.absolutePath, link, extension, resolution, downloadPath, "$filename.$extension")
        processBuilder.redirectErrorStream(true)

        try {

            val process = processBuilder.start()
            downloadProcess = process

            val output = mutableListOf<String>()

            Thread {
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?

                while (true) {
                    line = reader.readLine()
                    if (line == null) break
                    onProgressChange(line.toDouble(), "Downloading...")
                }

            }.start()

            process.waitFor()

            if(Cancelling){
                try {
                    File(downloadPath, filename).delete()
                    onResult(true)
                } catch (e: Exception){
                    println(e)
                    onResult(false)
                }
                return@launch
            }

            onResult(true)

        } catch (e: Exception) {
            onResult(false)
        }
    }
}

fun downloadVideo(
    link: String,
    downloadPath: String,
    fileName: String,
    extension: String,
    resolution: String,
    savedAs: Boolean,
    onResult: (Boolean) -> Unit,
    onProgressChange: (Double, String) -> Unit
){
    val audioFile = Files.createTempFile("tempAudio", ".m4a").toFile()
    val videoFile = Files.createTempFile("tempVideo", ".$extension").toFile()
    var output = File(downloadPath, "$fileName.$extension")

    val scriptFile = extractScriptFromRes("downloadVideo.py")
    if (scriptFile == null) { onResult(false); return }

    val processBuilder = ProcessBuilder("python3", scriptFile.absolutePath, link, extension, resolution, audioFile.parent, audioFile.name, videoFile.name)
    processBuilder.redirectErrorStream(true)

    CoroutineScope(Dispatchers.IO).launch{
        try {
            val process = processBuilder.start()
            downloadProcess = process

            Thread {
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?

                while (true) {
                    line = reader.readLine()
                    if (line == null) break
                    onProgressChange(line.toDouble(), "Downloading...")
                }

            }.start()

            process.waitFor()

            if(Cancelling){
                try {
                    audioFile.delete()
                    videoFile.delete()
                    onResult(true)
                } catch (e: Exception){
                    println(e)
                    onResult(false)
                }
                return@launch
            }

            if(!savedAs){
                val name = output.nameWithoutExtension
                val fileExtension = output.extension
                var attempts = 0;

                while(true){
                    if (output.exists()){
                        attempts++;
                        output = File(output.parent, "$name($attempts).$fileExtension")
                        continue
                    } else {
                        break
                    }
                }
            }

            println("Calling ffmpeg")

            println("Audio: ${audioFile.absolutePath}, Video: ${videoFile.absolutePath}, Output: ${output.absolutePath}")

            onProgressChange(0.0, "Exporting")

            runFFmpegExe(
                name = "ffmpeg",
                audioFile = audioFile,
                videoFile = videoFile,
                outputFile = output
            )

            if (Cancelling) { output.delete() }

            audioFile.delete()
            videoFile.delete()

            onProgressChange(1.0, "Exporting")

            onResult(true)

        } catch (e: Exception) {
            onResult(false)
        }
    }
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

fun runPyScriptFromRes(fileName: String, args: List<String> = emptyList()): scriptOutput?  {
    val scriptFile = extractScriptFromRes(fileName) ?: return null

    val processBuilder = ProcessBuilder("python3", scriptFile.absolutePath, *args.toTypedArray())
    processBuilder.redirectErrorStream(true)

    return try {
        val process = processBuilder.start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))

        val output = mutableListOf<String>()
        var line: String? = reader.readLine()
        while (line != null) {
            output.add(line)
            line = reader.readLine()
        }

        process.waitFor()
        return scriptOutput (File(scriptFile.parentFile?.absolutePath ?: ""), output)
    } catch (e: Exception) {
        null
    }
}