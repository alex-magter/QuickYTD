package org.alexmagter.QuickYTD

import java.io.File

expect class Video(linkParam: String) {

    val link: String
    var downloadPath: String
    var filename: String
    var extension: String
    var resolution: String
    var isSavedAs: Boolean
    var downloadType: String

    fun getFileData(): File
    fun getName(): File
    fun getChannel(): File
    fun getThumbnail(): File

    fun getData()

    fun download(
        onResult: (Boolean) -> Unit = {},
        onProgressChange: (Double, String) -> Unit
    )

    fun cancelDownload()

    fun downloadThumbnail(path: String, onResult: () -> Unit = {})

    companion object {
        fun checkVideo(link: String, ifErrorOccurred: (Exception) -> Unit = {}, onResult: (Boolean) -> Unit = {})
    }
}