package org.alexmagter.QuickYTD

import java.io.OutputStream

expect fun download(
    link: String,
    downloadPath: String,
    filename: String,
    type: String,
    extension: String,
    resolution: String,
    savedAs: Boolean = false,
    onResult: (Boolean) -> Unit = {},
    onProgressChange: (Double, String) -> Unit
)

expect fun cancelDownload()


expect fun getData(link: String, ifErrorOccurred: (Exception) -> Unit = {}, onResult: (VideoData) -> Unit = {})

expect fun checkVideo(link: String, ifErrorOccurred: (Exception) -> Unit = {}, onResult: (Boolean) -> Unit = {})