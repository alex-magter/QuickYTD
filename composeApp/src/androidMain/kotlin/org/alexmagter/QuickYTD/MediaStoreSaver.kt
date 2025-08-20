package org.alexmagter.QuickYTD

import android.content.ContentValues
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

object MediaStoreSaver {
    private val context = MainApplication.instance

    fun addPicture(file: File){
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            arrayOf("image/jpeg"),
            null
        )
    }

    fun addAudio(file: File){
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            arrayOf("audio/mp4"),
            null
        )
    }

    fun addVideo(file: File){
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            arrayOf("video/mp4"),
            null
        )
    }
}