package org.alexmagter.QuickYTD

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.alexmagter.QuickYTD.FFmpegRunner.runFFmpegExe
import org.alexmagter.QuickYTD.SystemUtils.runPyScriptFromRes
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

actual class Video actual constructor(private val linkParam: String) {

    init {
        getData()
    }

    actual val link: String = linkParam

    private lateinit var videoData: VideoData

    actual fun getFileData(): File {
        return videoData.fileData
    }

    actual fun getName(): File {
        return videoData.videoName
    }

    actual fun getChannel(): File {
        return videoData.channelName
    }

    actual fun getThumbnail(): File {
        return videoData.thumbnail
    }

    actual var downloadPath: String = ""
    actual var filename: String = ""
    actual var extension: String = ""
    actual var resolution: String  = ""
    actual var isSavedAs: Boolean = false
    actual var downloadType: String = ""

    private var isDownloading = false
    private var isCancellingDownload = false

    actual fun getData() {

        val output = runPyScriptFromRes("getData.py", listOf(linkParam, getAppDataDir("QuickYTD").absolutePath))

        val data = VideoData(output!!.path, linkParam)
        videoData = data

    }

    actual fun download(
        onResult: (Boolean) -> Unit,
        onProgressChange: (Double, String) -> Unit
    ) {
        isDownloading = true
        isCancellingDownload = false

        if(downloadType == "Audio"){
            downloadAudio(
                onProgressChange = onProgressChange,
                onResult = onResult
            )
        } else if(downloadType == "Video"){
            downloadVideo(
                onProgressChange = onProgressChange,
                onResult = onResult
            )
        } else if (downloadType == "Video (muted)"){
            downloadMutedVideo(
                onProgressChange = onProgressChange,
                onResult = onResult
            )
        } else {
            throw IllegalStateException("Couldn't detect selected type")
        }

        isDownloading = false
    }

    private fun downloadAudio(
        onResult: (Boolean) -> Unit,
        onProgressChange: (Double, String) -> Unit
    ) {
        isCancellingDownload = false

        CoroutineScope(Dispatchers.IO).launch {

            //val scriptFile = extractScriptFromRes("downloadAudio.py") ?: return@launch

            runPyScriptFromRes(
                fileName = "downloadAudio.py",
                args = listOf(link, extension, resolution, downloadPath, "$filename.$extension"),
                onOutput = { line ->
                    onProgressChange(line.toDouble(), "Downloading...")
                },
                cancelProcessWhen = { isCancellingDownload }
            )

            println("$link $extension $resolution $downloadPath $filename")

            if(isCancellingDownload){
                File(downloadPath, "$filename.$extension").delete()
            }

            withContext(Dispatchers.Main){
                onResult(true)
            }
        }
    }

    private fun downloadMutedVideo(
        onResult: (Boolean) -> Unit,
        onProgressChange: (Double, String) -> Unit
    ) {
        isCancellingDownload = false

        CoroutineScope(Dispatchers.IO).launch {

            runPyScriptFromRes(
                fileName = "downloadVideoMuted.py",
                args = listOf(link, extension, resolution, downloadPath, "$filename.$extension"),
                onOutput = { line ->
                    onProgressChange(line.toDouble(), "Downloading...")
                },
                cancelProcessWhen = { isCancellingDownload }
            )

            if(isCancellingDownload){
                val result = File(downloadPath, "$filename.$extension").delete()
                println(result)
                onResult(result)
            }

            onResult(true)

        }
    }

    private fun downloadVideo(
        onResult: (Boolean) -> Unit,
        onProgressChange: (Double, String) -> Unit
    ) {
        val audioFile = Files.createTempFile("tempAudio", ".m4a").toFile()
        val videoFile = Files.createTempFile("tempVideo", ".$extension").toFile()
        var output = File(downloadPath, "$filename.$extension")

        /*val scriptFile = extractScriptFromRes("downloadVideo.py")
        if (scriptFile == null) { onResult(false); return }

        val processBuilder = ProcessBuilder("python3", scriptFile.absolutePath, link, extension, resolution, audioFile.parent, audioFile.name, videoFile.name)
        processBuilder.redirectErrorStream(true)*/

        CoroutineScope(Dispatchers.IO).launch{

                /*val process = processBuilder.start()
                downloadProcess = process


                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?

                while (true) {
                    line = reader.readLine()
                    if (line == null) break

                    withContext(Dispatchers.Main){
                        onProgressChange(line.toDouble(), "Downloading...")
                    }

                }

                process.waitFor()
                 */

            runPyScriptFromRes(
                fileName = "downloadVideo.py",
                args = listOf(link, extension, resolution, audioFile.parent, audioFile.name, videoFile.name),
                onOutput = { line ->
                    onProgressChange(line.toDouble(), "Downloading...")
                },
                onError = {
                    println(it)
                    onResult(false)
                    return@runPyScriptFromRes
                },
                cancelProcessWhen = { isCancellingDownload }
            )

            if(isCancellingDownload){
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

            if(!isSavedAs){
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

            val result = runFFmpegExe(
                name = "ffmpeg",
                audioFile = audioFile,
                videoFile = videoFile,
                outputFile = output
            )

            if(result == false) { onResult(false); return@launch; }

            if (isCancellingDownload) { output.delete() }

            audioFile.delete()
            videoFile.delete()

            onProgressChange(1.0, "Exporting")

            onResult(true)

        }
    }

    actual fun cancelDownload(){
        isCancellingDownload = true
    }

    actual fun downloadThumbnail(path: String, onResult: () -> Unit){
        val targetFile: File = File(path, "thumbnail.jpg")

        val source: Path = Paths.get(getThumbnail().absolutePath)
        val target: Path = Paths.get(targetFile.absolutePath)

        CoroutineScope(Dispatchers.IO).launch {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
            withContext(Dispatchers.Main){
                onResult()
            }
        }
    }

    actual companion object {
        actual fun checkVideo(
            link: String,
            ifErrorOccurred: (Exception) -> Unit,
            onResult: (Boolean) -> Unit
        ){
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
    }
}