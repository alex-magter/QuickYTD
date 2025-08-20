package org.alexmagter.QuickYTD

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader

object SystemUtils {
    fun getOS(): String {
        val osName = System.getProperty("os.name").lowercase()
        return when {
            osName.contains("win") -> "Windows"
            osName.contains("mac") -> "Mac"
            else -> "Linux"
        }
    }

    fun getAppDataDir(appName: String): File {
        val os = getOS()

        val baseDir = when {
            os.contains("Windows") -> {
                // En Windows: %APPDATA%
                System.getenv("APPDATA") ?: System.getProperty("user.home")
            }

            os.contains("Mac") -> {
                // En macOS: ~/Library/Application Support
                System.getProperty("user.home") + "/Library/Application Support"
            }

            else -> {
                // En Linux/Unix: $XDG_DATA_HOME o ~/.local/share
                System.getenv("XDG_DATA_HOME")
                    ?: (System.getProperty("user.home") + "/.local/share")
            }
        }
        val appDir = File(baseDir, appName)
        if (!appDir.exists()) {
            appDir.mkdirs()
        }

        return appDir
    }



    fun runPyScriptFromRes(
        fileName: String,
        args: List<String> = emptyList(),
        onOutput: (String) -> Unit = {},
        onError: (Exception) -> Unit = {},
        cancelProcessWhen: () -> Boolean = { false }
    ): scriptOutput?  {
        //val scriptFile = extractScriptFromRes(fileName) ?: return null
        val launcher = extractExecutableByOS("pyLauncher")
        val scriptFile = extractScriptFromRes(fileName) ?: return null

        val processBuilder = ProcessBuilder(launcher?.absolutePath ?: return null, scriptFile.absolutePath, *args.toTypedArray())
        processBuilder.redirectErrorStream(true)

        return try {
            val process = processBuilder.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            val output = mutableListOf<String>()
            var line: String? = reader.readLine()
            while (line != null) {
                println(line)
                output.add(line)

                onOutput(line)

                if(cancelProcessWhen()){
                    println("Cancelling")
                    process.descendants().forEach { it.destroy() }
                    process.destroy()
                    break
                }

                line = reader.readLine()
            }

            process.waitFor()

            return scriptOutput (File(scriptFile.parentFile?.absolutePath ?: ""), output)
        } catch (e: Exception) {
            onError(e)
            null
        }
    }

    private fun extractExecutableByOS(name: String): File?{
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

        val workingDir = getAppDataDir("QuickYTD")
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

    private fun extractScriptFromRes(fileName: String): File? {
        val inputStream = {}.javaClass.getResourceAsStream("/$fileName") ?: return null


        val workingDir = org.alexmagter.QuickYTD.getAppDataDir("QuickYTD")
        val targetFolder = File(workingDir, "py")
        val targetFile = File(targetFolder, fileName)

        inputStream.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }
        return targetFile
    }
}