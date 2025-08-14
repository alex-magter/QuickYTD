package org.alexmagter.QuickYTD

import android.media.MediaCodec
import android.util.Log
import androidx.core.net.toUri
import com.chaquo.python.PyException
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

actual class Video actual constructor(linkParam: String) {
    actual val link: String = linkParam
    actual var downloadPath: String = ""
    actual var filename: String = ""
    actual var extension: String = ""
    actual var resolution: String = ""
    actual var isSavedAs: Boolean = false
    actual var downloadType: String = ""

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

    actual fun getData() {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }

        val py = Python.getInstance()
        val module = py.getModule("getData_Android")

        val contextPath = context.filesDir.absolutePath

        val result: String

        try {
            result = module.callAttr("startScript", link, contextPath).toString()
        } catch (_: Exception) {
        }

        val path = File(contextPath)
        val output = VideoData(path, link)
        videoData = output
    }


    actual fun download(
        onResult: (Boolean) -> Unit,
        onProgressChange: (Double, String) -> Unit
    ) {
        if (downloadType == "Audio") {
            downloadAudio(
                onProgressChange = onProgressChange,
                onResult = onResult
            )
        } else if (downloadType == "Video") {
            downloadVideo(
                onProgressChange = onProgressChange,
                onResult = onResult
            )
        } else if (downloadPath == "Video (muted)") {
            downloadMutedVideo(
                onProgressChange = onProgressChange,
                onResult = onResult
            )
        } else {
            throw IllegalStateException("Couldn't detect selected type")
        }
    }


    private fun downloadAudio(
        onResult: (Boolean) -> Unit,
        onProgressChange: (Double, String) -> Unit
    ) {
        Cancelling = false

        val tempDir = if(isSavedAs) context.cacheDir else File(downloadPath)
        val tempFileName = if(isSavedAs) "temp_media_${System.currentTimeMillis()}.tmp" else "$filename.$extension"
        val tempFile = File(tempDir, tempFileName)

        val tempFilePath = tempFile.absolutePath

        CoroutineScope(Dispatchers.IO).launch {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context))
            }

            val py = Python.getInstance()

            val module = py.getModule("downloadAudio_Android")

            module.put("progress_callback", onProgressChange)
            module.put("is_action_cancelled", {
                val isActive = coroutineContext[Job]?.isActive

                println("Is active thread: $isActive")

                Cancelling
            })


            println("Vamos a llamar a python")

            try {
                module.callAttr("download", link, extension, resolution, tempDir.toString(), tempFileName)
            } catch (e: PyException) {
                if(e.message == "Download cancelled") { onResult(true); return@launch }

                Log.e("DownloadError", "Error al descargar: ${e.message}", e)
                tempFile.delete()

                withContext(Dispatchers.Main){
                    onResult(false)
                }
                return@launch

            }

            if(Cancelling) {
                onResult(true)
                return@launch
            }

            if (!isSavedAs) { onResult(true); return@launch }

            if(!tempFile.exists() || tempFile.length() == 0L) {
                Log.e("FileCopy", "El archivo temporal no existe o esta vacío: ${tempFile.absolutePath}")
                tempFile.delete()
                withContext(Dispatchers.Main){
                    onResult(false)
                }
                return@launch
            }

            Log.d("FileCopy", "Iniciando copia desde: ${tempFile.absolutePath} hacia URI $downloadPath")

            var success = false

            val totalBytesToCopy = tempFile.length()

            try {
                context.contentResolver.openInputStream(downloadPath.toUri())?.use {
                    // Comprobamos si el destino es escribible
                }

                val outputStream: OutputStream? = context.contentResolver.openOutputStream(downloadPath.toUri())
                if(outputStream == null){
                    Log.e("FileCopy", "No se pudo abrir OutputStream para la Uri: $downloadPath")
                    withContext(Dispatchers.Main){
                        onResult(false)
                    }
                    return@launch
                }

                val inputStream: InputStream = FileInputStream(tempFile)

                val bufferSize = 8 /* Size in kb */ * 1024
                val buffer = ByteArray(bufferSize) // Bytes que se van a escribir en esta iteracion
                var bytesRead: Int // Numero de bytes que se van a escribir en esta iteracion
                var lastReportedPercentage = -1
                var bytesCopiedSoFar = 0L

                while (true){
                    if(Cancelling) {

                        context.contentResolver.openOutputStream(downloadPath.toUri(), "w")?.use {
                        } ?: run {

                        }

                        onResult(true)
                        return@launch
                    }

                    bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break

                    outputStream.write(buffer, 0, bytesRead)
                    bytesCopiedSoFar += bytesRead

                    val currentPercentage = ((bytesCopiedSoFar * 100) / totalBytesToCopy).toInt()
                    if (currentPercentage > lastReportedPercentage || bytesCopiedSoFar == totalBytesToCopy) {
                        withContext(Dispatchers.Main){
                            onProgressChange(currentPercentage.toDouble(), "Copying file to the desired ubication...")
                        }
                        lastReportedPercentage = currentPercentage
                    }


                }
            } catch (e: IOException) {
                Log.e("FileCopy", "I/O Error during copy", e)
                success = false
            } catch (e: SecurityException) {
                Log.e("FileCopy", "Security error during copy")
                success = false
            } catch (e: Exception) {
                Log.e("FileCopy", "Unexpected error during copy")
            } finally {
                if(tempFile.exists()) {
                    Log.d("FileCopy", "Deleting temporal file: ${tempFile.absolutePath}")
                    if(!tempFile.delete()) {
                        Log.w("FileCopy", "Cant delete temp file: ${tempFile.absolutePath}")
                    } else {
                        success = true
                    }
                }

                withContext(Dispatchers.Main){
                    onResult(success)
                }
            }
        }
    }

    private fun downloadMutedVideo(
        onResult: (Boolean) -> Unit,
        onProgressChange: (Double, String) -> Unit
    ) {
        Cancelling = false

        val tempDir = if(isSavedAs) context.cacheDir else File(downloadPath)
        val tempFileName = if(isSavedAs) "temp_media_${System.currentTimeMillis()}.tmp" else "$filename.$extension"
        val tempFile = File(tempDir, tempFileName)

        val tempFilePath = tempFile.absolutePath

        CoroutineScope(Dispatchers.IO).launch {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context))
            }

            val py = Python.getInstance()

            val module = py.getModule("downloadVideoMuted_Android")

            module.put("progress_callback", onProgressChange)
            module.put("is_action_cancelled", {
                val isActive = coroutineContext[Job]?.isActive

                println("Is active thread: $isActive")

                Cancelling
            })


            println("Vamos a llamar a python")

            try {
                module.callAttr("download", link, extension, resolution, tempDir.toString(), tempFileName)
            } catch (e: PyException) {
                if(e.message == "Download cancelled") { onResult(true); return@launch }

                Log.e("DownloadError", "Error al descargar: ${e.message}", e)
                tempFile.delete()

                withContext(Dispatchers.Main){
                    onResult(false)
                }
                return@launch

            }

            if(Cancelling) {
                onResult(true)
                return@launch
            }

            if (!isSavedAs) { onResult(true); return@launch }

            if(!tempFile.exists() || tempFile.length() == 0L) {
                Log.e("FileCopy", "El archivo temporal no existe o esta vacío: ${tempFile.absolutePath}")
                tempFile.delete()
                withContext(Dispatchers.Main){
                    onResult(false)
                }
                return@launch
            }

            Log.d("FileCopy", "Iniciando copia desde: ${tempFile.absolutePath} hacia URI $downloadPath")

            var success = false

            val totalBytesToCopy = tempFile.length()

            try {
                context.contentResolver.openInputStream(downloadPath.toUri())?.use {
                    // Comprobamos si el destino es escribible
                }

                val outputStream: OutputStream? = context.contentResolver.openOutputStream(downloadPath.toUri())
                if(outputStream == null){
                    Log.e("FileCopy", "No se pudo abrir OutputStream para la Uri: $downloadPath")
                    withContext(Dispatchers.Main){
                        onResult(false)
                    }
                    return@launch
                }

                val inputStream: InputStream = FileInputStream(tempFile)

                val bufferSize = 8 /* Size in kb */ * 1024
                val buffer = ByteArray(bufferSize) // Bytes que se van a escribir en esta iteracion
                var bytesRead: Int // Numero de bytes que se van a escribir en esta iteracion
                var lastReportedPercentage = -1
                var bytesCopiedSoFar = 0L

                while (true){
                    if(Cancelling) {

                        context.contentResolver.openOutputStream(downloadPath.toUri(), "w")?.use {
                        } ?: run {

                        }

                        onResult(true)
                        return@launch
                    }

                    bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break

                    outputStream.write(buffer, 0, bytesRead)
                    bytesCopiedSoFar += bytesRead

                    val currentPercentage = ((bytesCopiedSoFar * 100) / totalBytesToCopy).toInt()
                    if (currentPercentage > lastReportedPercentage || bytesCopiedSoFar == totalBytesToCopy) {
                        withContext(Dispatchers.Main){
                            onProgressChange(currentPercentage.toDouble(), "Copying file to the desired ubication...")
                        }
                        lastReportedPercentage = currentPercentage
                    }


                }
            } catch (e: IOException) {
                Log.e("FileCopy", "I/O Error during copy", e)
                success = false
            } catch (e: SecurityException) {
                Log.e("FileCopy", "Security error during copy")
                success = false
            } catch (e: Exception) {
                Log.e("FileCopy", "Unexpected error during copy")
            } finally {
                if(tempFile.exists()) {
                    Log.d("FileCopy", "Deleting temporal file: ${tempFile.absolutePath}")
                    if(!tempFile.delete()) {
                        Log.w("FileCopy", "Cant delete temp file: ${tempFile.absolutePath}")
                    } else {
                        success = true
                    }
                }

                withContext(Dispatchers.Main){
                    onResult(success)
                }
            }
        }
    }

    private fun downloadVideo(
        onResult: (Boolean) -> Unit,
        onProgressChange: (Double, String) -> Unit
    ) {
        val temp = context.cacheDir
        val audioFile = File(temp, "tempAudio.m4a")
        val videoFile = File(temp, "tempVideo.$extension")
        var outputFile = if(isSavedAs) File(temp, "temp_media_${System.currentTimeMillis()}.$extension") else File(downloadPath, "$filename.$extension")


        Cancelling = false

        downloadThread = CoroutineScope(Dispatchers.IO).launch {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context))
            }

            val py = Python.getInstance()

            val module = py.getModule("downloadVideo_Android")

            module.put("progress_callback", onProgressChange)
            module.put("is_action_cancelled", {
                val isActive = coroutineContext[Job]?.isActive

                println("Is active thread: $isActive")

                Cancelling
            })


            println("Vamos a llamar a python")

            try {
                module.callAttr(
                    "download",
                    link,
                    extension,
                    resolution,
                    temp.toString(),
                    audioFile.name,
                    videoFile.name
                )
            } catch (e: PyException) {
                if (e.message == "Download cancelled") {
                    onResult(true); return@launch
                }

                Log.e("DownloadError", "Error al descargar: ${e.message}", e)
                audioFile.delete()
                videoFile.delete()

                withContext(Dispatchers.Main) {
                    onResult(false)
                }
                return@launch

            }

            if (Cancelling) {
                onResult(true)
                return@launch
            }

            Log.d(
                "JoinAudioVideo",
                "Vamos a unir los archivos ${audioFile.absolutePath} y ${videoFile.absolutePath}"
            )

            val exportTag = "Exporting"



            if(!isSavedAs){
                val name = outputFile.nameWithoutExtension
                val fileExtension = outputFile.extension
                var attempts = 0;

                while(true){
                    if (outputFile.exists()){
                        attempts++;
                        outputFile = File(outputFile.parent, "$name($attempts).$fileExtension")
                        continue
                    } else {
                        break
                    }
                }
            }

            onProgressChange(0.0, exportTag)

            val videoExtractor = MediaExtractor().apply { setDataSource(videoFile.absolutePath) }
            val audioExtractor = MediaExtractor().apply { setDataSource(audioFile.absolutePath) }
            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            var muxerVideoTrackIndex = -1
            var muxerAudioTrackIndex = -1

            val videoTrackIndex = (0 until videoExtractor.trackCount)
                .firstOrNull {
                    val format = videoExtractor.getTrackFormat(it)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                    if(mime.startsWith("video/")){
                        muxerVideoTrackIndex = muxer.addTrack(format)
                        videoExtractor.selectTrack(it)
                        true
                    } else false
                } ?: throw IllegalArgumentException("No video track found")

            val audioTrackIndex = (0 until audioExtractor.trackCount)
                .firstOrNull {
                    val format = audioExtractor.getTrackFormat(it)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                    if (mime.startsWith("audio/")) {
                        muxerAudioTrackIndex = muxer.addTrack(format)
                        audioExtractor.selectTrack(it)
                        true
                    } else false
                } ?: throw IllegalArgumentException("No audio track found")

            muxer.start()

            var videoProgress = 0.0
            var audioProgress = 0.0
            val videoDurationUs = videoExtractor.getTrackFormat(videoTrackIndex).getLong(MediaFormat.KEY_DURATION)
            val audioDurationUs = audioExtractor.getTrackFormat(audioTrackIndex).getLong(MediaFormat.KEY_DURATION)

            val videoBuffer = ByteBuffer.allocate(1024 * 1024)
            val videoBufferInfo = MediaCodec.BufferInfo()
            val audioBuffer = ByteBuffer.allocate(1024 * 1024) // Buffer separado puede ser más seguro
            val audioBufferInfo = MediaCodec.BufferInfo()

            var sawVideoEOS = false
            var sawAudioEOS = false

            // Pre-carga la primera muestra de cada uno si están disponibles
            var videoSampleSize = videoExtractor.readSampleData(videoBuffer, 0)
            if (videoSampleSize < 0) sawVideoEOS = true else {
                videoBufferInfo.offset = 0
                videoBufferInfo.size = videoSampleSize
                videoBufferInfo.presentationTimeUs = videoExtractor.sampleTime
                videoBufferInfo.flags = videoExtractor.sampleFlags
            }

            var audioSampleSize = audioExtractor.readSampleData(audioBuffer, 0)
            if (audioSampleSize < 0) sawAudioEOS = true else {
                audioBufferInfo.offset = 0
                audioBufferInfo.size = audioSampleSize
                audioBufferInfo.presentationTimeUs = audioExtractor.sampleTime
                audioBufferInfo.flags = audioExtractor.sampleFlags
            }


            while (!sawVideoEOS || !sawAudioEOS) {

                if(Cancelling){

                    muxer.stop()
                    muxer.release()
                    videoExtractor.release()
                    audioExtractor.release()

                    audioFile.delete()
                    videoFile.delete()
                    outputFile.delete()

                    onResult(true)
                    return@launch

                }

                val writeVideo = !sawVideoEOS && (sawAudioEOS || videoBufferInfo.presentationTimeUs <= audioBufferInfo.presentationTimeUs)

                if (writeVideo) {
                    muxer.writeSampleData(muxerVideoTrackIndex, videoBuffer, videoBufferInfo)
                    videoProgress = videoBufferInfo.presentationTimeUs.toDouble() / videoDurationUs
                    onProgressChange((videoProgress + audioProgress) * 100 / 2.0, "Exporting")

                    if (!videoExtractor.advance()) {
                        sawVideoEOS = true
                    } else {
                        videoSampleSize = videoExtractor.readSampleData(videoBuffer, 0)
                        if (videoSampleSize < 0) sawVideoEOS = true else {
                            videoBufferInfo.offset = 0
                            videoBufferInfo.size = videoSampleSize
                            videoBufferInfo.presentationTimeUs = videoExtractor.sampleTime
                            videoBufferInfo.flags = videoExtractor.sampleFlags
                        }
                    }
                } else if (!sawAudioEOS) { // writeAudio
                    muxer.writeSampleData(muxerAudioTrackIndex, audioBuffer, audioBufferInfo)
                    audioProgress = audioBufferInfo.presentationTimeUs.toDouble() / audioDurationUs
                    onProgressChange((videoProgress + audioProgress) * 100 / 2.0, "Exporting")

                    if (!audioExtractor.advance()) {
                        sawAudioEOS = true
                    } else {
                        audioSampleSize = audioExtractor.readSampleData(audioBuffer, 0)
                        if (audioSampleSize < 0) sawAudioEOS = true else {
                            audioBufferInfo.offset = 0
                            audioBufferInfo.size = audioSampleSize
                            audioBufferInfo.presentationTimeUs = audioExtractor.sampleTime
                            audioBufferInfo.flags = audioExtractor.sampleFlags
                        }
                    }
                }
            }

            muxer.stop()
            muxer.release()
            videoExtractor.release()
            audioExtractor.release()

            if (!isSavedAs) {
                onResult(true); return@launch
            }

            Log.d("FileCopy", "Iniciando copia desde: ${outputFile.absolutePath} hacia URI $downloadPath")

            var success = false

            val totalBytesToCopy = outputFile.length()

            try {
                context.contentResolver.openInputStream(downloadPath.toUri())?.use {
                    // Comprobamos si el destino es escribible
                }

                val outputStream: OutputStream? = context.contentResolver.openOutputStream(downloadPath.toUri())
                if(outputStream == null){
                    Log.e("FileCopy", "No se pudo abrir OutputStream para la Uri: $downloadPath")
                    withContext(Dispatchers.Main){
                        onResult(false)
                    }
                    return@launch
                }

                val inputStream: InputStream = FileInputStream(outputFile)

                val bufferSize = 8 /* Size in kb */ * 1024
                val buffer = ByteArray(bufferSize) // Bytes que se van a escribir en esta iteracion
                var bytesRead: Int // Numero de bytes que se van a escribir en esta iteracion
                var lastReportedPercentage = -1
                var bytesCopiedSoFar = 0L

                while (true){
                    if(Cancelling) {

                        context.contentResolver.openOutputStream(downloadPath.toUri(), "w")?.use {
                        } ?: run {

                        }

                        onResult(true)
                        return@launch
                    }

                    bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break

                    outputStream.write(buffer, 0, bytesRead)
                    bytesCopiedSoFar += bytesRead

                    val currentPercentage = ((bytesCopiedSoFar * 100) / totalBytesToCopy).toInt()
                    if (currentPercentage > lastReportedPercentage || bytesCopiedSoFar == totalBytesToCopy) {
                        withContext(Dispatchers.Main){
                            onProgressChange(currentPercentage.toDouble(), "Copying file to the desired ubication...")
                        }
                        lastReportedPercentage = currentPercentage
                    }


                }
            } catch (e: IOException) {
                Log.e("FileCopy", "I/O Error during copy", e)
                success = false
            } catch (e: SecurityException) {
                Log.e("FileCopy", "Security error during copy")
                success = false
            } catch (e: Exception) {
                Log.e("FileCopy", "Unexpected error during copy")
            } finally {
                if(outputFile.exists()) {
                    Log.d("FileCopy", "Deleting temporal file: ${outputFile.absolutePath}")
                    if(!outputFile.delete()) {
                        Log.w("FileCopy", "Cant delete temp file: ${outputFile.absolutePath}")
                    } else {
                        success = true
                    }
                }

                withContext(Dispatchers.Main){
                    onResult(success)
                }
            }

        }
    }

    actual fun downloadThumbnail(path: String, onResult: () -> Unit){
        val source = getThumbnail()
        val targetFile = File(path, "thumbnail.jpg")


        CoroutineScope(Dispatchers.IO).launch {
            source.copyTo(targetFile, overwrite = true)

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
        ) {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context))
            }

            val py = Python.getInstance()
            val module = py.getModule("checkVideo_Android")

            val result: Boolean

            try {
                result = module.callAttr("checkVideo", link).toBoolean()
            } catch (e: Exception) {
                ifErrorOccurred(e)
                return
            }

            val isValid: Boolean = result

            onResult(isValid)
        }
    }
}