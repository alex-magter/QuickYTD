package org.alexmagter.QuickYTD

import android.annotation.SuppressLint
import android.media.MediaCodec
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.TransformationRequest
import androidx.media3.transformer.Transformer
import com.chaquo.python.PyException
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import androidx.media3.transformer.Composition as Media3Composition
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaCodec.BufferInfo;
import android.provider.MediaStore.Audio.Media
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import java.nio.ByteBuffer
import kotlin.text.toDouble

val context = MainApplication.instance

interface FileCopyProgressListener {
    fun onCopyProgress(bytesCopied: Long, totalBytes: Long)
    fun onCopyComplete(success: Boolean)
}

actual fun getData(link: String, ifErrorOccurred: (Exception) -> Unit, onResult: (VideoData) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch{

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }

        val py = Python.getInstance()
        val module = py.getModule("getData_Android")

        val contextPath = context.filesDir.absolutePath

        val result: String

        try {
            result = module.callAttr("startScript", link, contextPath).toString()
        } catch (e: Exception) {
            ifErrorOccurred(e)
            return@launch
        }

        val path = File(contextPath)
        val output = VideoData(path, link)
        withContext(Dispatchers.Main) {
            onResult(output)
        }
    }
}

actual fun checkVideo(link: String, ifErrorOccurred: (Exception) -> Unit, onResult: (Boolean) -> Unit){
    if (!Python.isStarted()) {
        Python.start(AndroidPlatform(context))
    }

    val py = Python.getInstance()
    val module = py.getModule("checkVideo_Android")

    val result: Boolean

    try {
        result = module.callAttr("checkVideo", link).toBoolean()
    } catch (e: Exception){
        ifErrorOccurred(e)
        return
    }

    val isValid : Boolean = result

    onResult(isValid)
}

var downloadThread: Job? = null

var targetFile: File? = null

var Cancelling = false

actual fun cancelDownload(){
    //downloadThread?.cancel()

    Cancelling = true

    CoroutineScope(Dispatchers.IO).launch{

        delay(100)

        val files = context.cacheDir.listFiles()

        try {
            for (file in files!!){
                file.delete()
            }
        } catch (e: Exception){
            println(e)
        } finally {
            return@launch
        }
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

    if(type == "Video"){
        downloadVideo(
            link = link,
            downloadPath = downloadPath,
            filename = filename,
            type = type,
            extension = extension,
            resolution = resolution,
            savedAs = savedAs,
            onResult = onResult,
            onProgressChange = onProgressChange
        )
    }

    Cancelling = false

    val tempDir = if(savedAs) context.cacheDir else File(downloadPath)
    val tempFileName = if(savedAs) "temp_media_${System.currentTimeMillis()}.tmp" else "$filename.$extension"
    val tempFile = File(tempDir, tempFileName)

    val tempFilePath = tempFile.absolutePath




    downloadThread = CoroutineScope(Dispatchers.IO).launch {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }

        val py = Python.getInstance()

        val module = when (type){
            "Audio" -> py.getModule("downloadAudio_Android")
            "Video (muted)" -> py.getModule("downloadVideoMuted_Android")
            else -> return@launch
        }

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

        if (!savedAs) { onResult(true); return@launch }

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

@SuppressLint("WrongConstant")
@OptIn(UnstableApi::class)
fun downloadVideo(
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

    val temp = context.cacheDir
    val audioFile = File(temp, "tempAudio.m4a")
    val videoFile = File(temp, "tempVideo.$extension")
    var outputFile = if(savedAs) File(temp, "temp_media_${System.currentTimeMillis()}.$extension") else File(downloadPath, "$filename.$extension")


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

        if (!savedAs) {
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
