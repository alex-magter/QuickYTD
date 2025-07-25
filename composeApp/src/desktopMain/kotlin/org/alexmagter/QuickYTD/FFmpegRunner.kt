package org.alexmagter.QuickYTD

import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.nio.file.Files

fun getOS(): String {
    val osName = System.getProperty("os.name").lowercase()
    return when {
        osName.contains("win") -> "Windows"
        osName.contains("mac") -> "Mac"
        osName.contains("nix") || osName.contains("nux") || osName.contains("aix") -> "Linux"
        else -> "Unknown"
    }
}

fun extractExecutablaByOS(name: String): File?{
    val folder = when(getOS()){
        "Windows" -> "win"
        "Mac" -> "macos"
        "Linux" -> "linux"
        else -> return null
    }

    val extension = when(getOS()){
        "Windows" -> ".exe"
        "Mac" -> ""
        "Linux" -> ""
        else -> return null
    }

    val inputStream = {}.javaClass.getResourceAsStream("/bin/$folder/$name$extension") ?: return null
    val tempFile = Files.createTempFile("executable_temp", extension).toFile()
    tempFile.deleteOnExit() // Se eliminará automáticamente al salir

    inputStream.use { input ->
        FileOutputStream(tempFile).use { output ->
            input.copyTo(output)
        }
    }
    return tempFile
}

fun getVideoDuration(videoFile: File): Double{
    val executable = extractExecutablaByOS("FFprobe")

    val process = ProcessBuilder(
        executable?.absolutePath, "-v", "error",
        "-show_entries", "format=duration",
        "-of", "default=noprint_wrappers=1:nokey=1",
        videoFile.absolutePath
    ).start()

    val durationStr = process.inputStream.bufferedReader().readText().trim()
    process.waitFor()
    return durationStr.toDoubleOrNull() ?: 1.0
}

fun runFFmpegExe(
    name: String,
    audioFile: File,
    videoFile: File,
    outputFile: File
){
    val exeFile = extractExecutablaByOS(name) ?: return

    val processBuilder = ProcessBuilder(exeFile.absolutePath)
    processBuilder.redirectErrorStream(true)

    println("starting process")


    val process = ProcessBuilder(
        exeFile.absolutePath,
        "-i", videoFile.absolutePath,
        "-i", audioFile.absolutePath,
        "-c", "copy",
        "-map", "0:v:0",
        "-map", "1:a:0",
        "-shortest",
        "-progress", "-",
        "-nostats", "-y",
        outputFile.absolutePath
    ).start()

    val reader = BufferedReader(InputStreamReader(process.inputStream))

    val output = mutableListOf<String>()
    var line: String? = reader.readLine()
    while (line != null) {
        output.add(line)
        line = reader.readLine()
    }


    process.waitFor()

}