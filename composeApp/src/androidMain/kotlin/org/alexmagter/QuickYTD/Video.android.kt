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
import android.media.MediaScannerConnection
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.math.max

actual class Video actual constructor(private val linkParam: String) {

    init {
        getData()
    }

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

    actual fun cancelDownload(){
        isCancellingDownload = true
    }

    private var isCancellingDownload = false

    actual fun getData() {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }

        val py = Python.getInstance()
        val module = py.getModule("getData_Android")

        val contextPath = context.filesDir.absolutePath

        val result: String

        try {
            result = module.callAttr("startScript", linkParam, contextPath).toString()
        } catch (_: Exception) {
        }

        val path = File(contextPath)
        val output = VideoData(path, linkParam)
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
        } else {
            downloadMutedVideo(
                onProgressChange = onProgressChange,
                onResult = onResult
            )
        }
    }


    private fun downloadAudio(
        onResult: (Boolean) -> Unit,
        onProgressChange: (Double, String) -> Unit
    ) {
        isCancellingDownload = false

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

                isCancellingDownload
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

            if(isCancellingDownload) {
                onResult(true)
                return@launch
            }

            if (!isSavedAs) { onResult(true); MediaStoreSaver.addAudio(tempFile); return@launch }

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
                    if(isCancellingDownload) {

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

                    MediaStoreSaver.addAudio(tempFile);


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
        isCancellingDownload = false

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

                isCancellingDownload
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

            if(isCancellingDownload) {
                onResult(true)
                return@launch
            }

            if (!isSavedAs) { onResult(true); MediaStoreSaver.addVideo(tempFile); return@launch }

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
                    if(isCancellingDownload) {

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

                    MediaStoreSaver.addVideo(tempFile);

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

        isCancellingDownload = false

        CoroutineScope(Dispatchers.IO).launch {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context))
            }

            val py = Python.getInstance()

            val module = py.getModule("downloadVideo_Android")

            module.put("progress_callback", onProgressChange)
            module.put("is_action_cancelled", {
                val isActive = coroutineContext[Job]?.isActive

                println("Is active thread: $isActive")

                isCancellingDownload
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

            if (isCancellingDownload) {
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

            mergeVideoAndAudio(
                videoFile = videoFile,
                audioFile = audioFile,
                outputFile = outputFile,
                onProgress = onProgressChange
            )

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
                    if(isCancellingDownload) {

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

                    MediaStoreSaver.addVideo(outputFile);

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

    /**
     * Une un archivo de video y uno de audio en un solo archivo MP4 sin recodificar.
     * Esta versión corrige problemas de framerate y congelamiento inicial.
     *
     * @param videoFile El archivo de video de entrada (ej: .mp4).
     * @param audioFile El archivo de audio de entrada (ej: .m4a).
     * @param outputFile El archivo de salida donde se guardará el resultado.
     * @throws IOException Si ocurre un error de E/S.
     * @throws IllegalArgumentException Si no se encuentra una pista de video o audio.
     */
    fun mergeVideoAndAudio(videoFile: File, audioFile: File, outputFile: File, onProgress: (Double, String) -> Unit) {
        try {
            outputFile.parentFile?.mkdirs()

            val videoExtractor = MediaExtractor().apply { setDataSource(videoFile.absolutePath) }
            val audioExtractor = MediaExtractor().apply { setDataSource(audioFile.absolutePath) }
            val muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            // 1. Configurar pistas
            val videoTrackIndex = findTrackIndex(videoExtractor, "video/")
            val audioTrackIndex = findTrackIndex(audioExtractor, "audio/")

            if (videoTrackIndex == -1) throw IllegalArgumentException("No se encontró una pista de video en ${videoFile.name}")
            if (audioTrackIndex == -1) throw IllegalArgumentException("No se encontró una pista de audio en ${audioFile.name}")

            videoExtractor.selectTrack(videoTrackIndex)
            audioExtractor.selectTrack(audioTrackIndex)

            val videoFormat = videoExtractor.getTrackFormat(videoTrackIndex)
            val audioFormat = audioExtractor.getTrackFormat(audioTrackIndex)

            val muxerVideoTrackIndex = muxer.addTrack(videoFormat)
            val muxerAudioTrackIndex = muxer.addTrack(audioFormat)

            // 2. Iniciar muxer y preparar buffers
            muxer.start()

            var videoProgress = 0.0
            var audioProgress = 0.0
            val videoDurationUs = videoExtractor.getTrackFormat(videoTrackIndex).getLong(MediaFormat.KEY_DURATION)
            val audioDurationUs = audioExtractor.getTrackFormat(audioTrackIndex).getLong(MediaFormat.KEY_DURATION)

            val buffer = ByteBuffer.allocate(1 * 1024 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()

            var sawVideoEOS = false
            var sawAudioEOS = false

            var videoFirstSampleTime: Long = -1
            var audioFirstSampleTime: Long = -1
            var lastVideoTimestamp: Long = 0
            var lastAudioTimestamp: Long = 0


            // 3. Bucle principal para copiar las muestras
            while (!sawVideoEOS || !sawAudioEOS) {
                // Priorizamos la muestra con el timestamp más bajo para mantener la sincronización
                val useVideo = if (!sawVideoEOS && (sawAudioEOS || videoExtractor.sampleTime <= audioExtractor.sampleTime)) {
                    true
                } else {
                    false
                }

                if (useVideo) {
                    val sampleSize = videoExtractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) {
                        sawVideoEOS = true
                    } else {
                        if (videoFirstSampleTime == -1L) {
                            videoFirstSampleTime = videoExtractor.sampleTime
                        }
                        val adjustedTime = videoExtractor.sampleTime - videoFirstSampleTime

                        // !! CORRECCIÓN FINAL !!
                        // Aseguramos que el timestamp sea estrictamente creciente.
                        // Si el timestamp ajustado es menor o igual que el último, lo incrementamos en 1.
                        if (adjustedTime <= lastVideoTimestamp) {
                            lastVideoTimestamp += 1
                        } else {
                            lastVideoTimestamp = adjustedTime
                        }

                        bufferInfo.presentationTimeUs = lastVideoTimestamp
                        bufferInfo.flags = videoExtractor.sampleFlags
                        bufferInfo.size = sampleSize
                        muxer.writeSampleData(muxerVideoTrackIndex, buffer, bufferInfo)

                        videoProgress = bufferInfo.presentationTimeUs.toDouble() / videoDurationUs
                        onProgress((videoProgress + audioProgress) * 100 / 2.0, "Exporting")

                        videoExtractor.advance()
                    }
                } else if (!sawAudioEOS) {
                    val sampleSize = audioExtractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) {
                        sawAudioEOS = true
                    } else {
                        if (audioFirstSampleTime == -1L) {
                            audioFirstSampleTime = audioExtractor.sampleTime
                        }
                        val adjustedTime = audioExtractor.sampleTime - audioFirstSampleTime

                        // Hacemos lo mismo para el audio
                        if (adjustedTime <= lastAudioTimestamp) {
                            lastAudioTimestamp += 1
                        } else {
                            lastAudioTimestamp = adjustedTime
                        }

                        bufferInfo.presentationTimeUs = lastAudioTimestamp
                        bufferInfo.flags = audioExtractor.sampleFlags
                        bufferInfo.size = sampleSize
                        muxer.writeSampleData(muxerAudioTrackIndex, buffer, bufferInfo)

                        audioProgress = bufferInfo.presentationTimeUs.toDouble() / videoDurationUs
                        onProgress((videoProgress + audioProgress) * 100 / 2.0, "Exporting")

                        audioExtractor.advance()
                    }
                }
            }

            // 4. Detener y liberar recursos
            muxer.stop()
            muxer.release()
            videoExtractor.release()
            audioExtractor.release()

            MediaScannerConnection.scanFile(
                context,
                arrayOf(outputFile.absolutePath), // La ruta completa del archivo
                arrayOf("video/mp4"), // "video/mp4"
                null // Puedes pasar un callback si lo necesitas
            )

        } catch (e: Exception) {
            println("Error durante el muxing: ${e.message}")
            if (outputFile.exists()) {
                outputFile.delete()
            }
            throw e
        }
    }

    /**
     * Función de ayuda para encontrar el índice de la primera pista que coincida con el MIME type.
     */
    private fun findTrackIndex(extractor: MediaExtractor, mimePrefix: String): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME)
            if (mime?.startsWith(mimePrefix) == true) {
                return i
            }
        }
        return -1
    }


    actual fun downloadThumbnail(path: String, onResult: () -> Unit){
        val source = getThumbnail()
        val targetFile = File(path, "thumbnail_${sanitizeFileName(getName().readText())}.jpg")


        CoroutineScope(Dispatchers.IO).launch {
            source.copyTo(targetFile, overwrite = true)

            withContext(Dispatchers.Main){
                onResult()
                MediaStoreSaver.addPicture(targetFile)
            }
        }
    }

    actual companion object {

        private val context = MainApplication.instance

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