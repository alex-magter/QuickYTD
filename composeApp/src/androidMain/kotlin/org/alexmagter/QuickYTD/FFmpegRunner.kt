package org.alexmagter.QuickYTD

import java.io.File
import android.content.Context
import androidx.compose.foundation.gestures.forEach
import androidx.media3.common.util.Log

fun copyFfmpegExecutable(context: Context): File {

    val abi = android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "jniLibs/arm64-v8a"


    val libPath = "$abi/ffmpeg"

    val outFile = File(context.filesDir, "ffmpeg")

    if(!outFile.exists()){
        context.assets.open(libPath).use { input ->
            outFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    outFile.setExecutable(true, false)


    return outFile

}

fun runFfmpeg(context: Context){
    val ffmpegFile = File(context.applicationInfo.nativeLibraryDir, "libffmpeg.so")
    val command = listOf(ffmpegFile.absolutePath, "-version")


    val process = ProcessBuilder(command)
        .redirectErrorStream(true)
        .start()

    val tag = "Ffmpeg"

    val nativeLibraryDir = context.applicationInfo.nativeLibraryDir
    val files = File(context.applicationInfo.nativeLibraryDir).listFiles()

    Log.d(tag, "Contenido de nativeLibraryDir ($nativeLibraryDir):")
    for (fileInDir in files!!) {
        val fileType = if (fileInDir.isDirectory) "Directorio" else "Archivo"
        val canRead = if (fileInDir.canRead()) "Sí" else "No"
        val canWrite = if (fileInDir.canWrite()) "Sí" else "No" // No debería ser escribible
        val canExecute = if (fileInDir.canExecute()) "Sí" else "No"
        val lastModified = java.util.Date(fileInDir.lastModified()).toString()

        Log.d(tag,
            " - Nombre: ${fileInDir.name}, " +
                    "Tipo: $fileType, " +
                    "Tamaño: ${fileInDir.length()} bytes, " +
                    "Puede Leer: $canRead, " +
                    "Puede Escribir: $canWrite, " +
                    "Puede Ejecutar: $canExecute, " +
                    "Modificado: $lastModified"
        )
    }



    process.inputStream.bufferedReader().use{
        it.lines().forEach { line ->
            Log.d("Ffmpeg", "Linea impresa: $line")
        }
    }

    println("Se acabo ffmpeg")
}