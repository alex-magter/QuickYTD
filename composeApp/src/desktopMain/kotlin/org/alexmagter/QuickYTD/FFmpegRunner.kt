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

fun extractExecutableByOS(name: String): File?{
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

    val finalName = name + "_" + folder

    val inputStream = {}.javaClass.getResourceAsStream("/bin/$folder/$finalName$extension") ?: return null

    val workingDir = System.getProperty("user.dir")
    val targetFolder = File(workingDir, "py")
    if (!targetFolder.exists()) {
        targetFolder.mkdirs()
    }
    val targetFile = File(targetFolder, "$finalName$extension")

    if(!targetFile.exists()){
        inputStream.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    return targetFile
}

fun getVideoDuration(videoFile: File): Double{
    val executable = extractExecutableByOS("FFprobe")

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



object FFmpegRunner {

    private object FFmpegPath{
        var isResource: Boolean = false
        var path: String = ""
    }

    fun runFFmpegExe(
        name: String,
        audioFile: File,
        videoFile: File,
        outputFile: File
    ): Boolean{
        //val exeFile = extractExecutablaByOS(name) ?: return false

        val exeFile = if(FFmpegPath.isResource){
            extractExecutableByOS(name) ?: return false
        } else {
            File(FFmpegPath.path)
        }

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

        return true

    }

    fun checkFFmpeg(): Boolean{

        val workingDir = System.getProperty("user.dir")

        val resourceFolder = when(getOS()){
            "Windows" -> "win"
            "Mac" -> "macos"
            "Linux" -> "linux"
            else -> return false
        }

        val extension = when(getOS()){
            "Windows" -> ".exe"
            "Mac" -> ""
            "Linux" -> ""
            else -> return false
        }


        val customBinary = File(workingDir, "ffmpeg" + extension)
        if(customBinary.exists()){
            FFmpegPath.isResource = false
            FFmpegPath.path = customBinary.absolutePath

            return true
        } else {
            val finalName = "ffmpeg_$resourceFolder"

            val inputStream =
                {}.javaClass.getResourceAsStream("/bin/$resourceFolder/$finalName$extension")
                    ?: return false

            inputStream.use { input ->
                FFmpegPath.isResource = true
                FFmpegPath.path = "/bin/$resourceFolder/$finalName$extension"
                return@use
            }
            return true
        }
    }
}