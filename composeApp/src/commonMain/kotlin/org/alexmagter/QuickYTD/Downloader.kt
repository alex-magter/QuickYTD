package org.alexmagter.QuickYTD

import java.io.OutputStream


expect fun download(
    link: String,
    downloadPath: String,
    type: String,
    extension: String,
    resolution: String,
    onProgressChange: (Float?) -> Unit
)

expect fun download(
    link: String,
    downloadPath: OutputStream,
    type: String,
    extension: String,
    resolution: String,
    onProgressChange: (String?) -> Unit
)


expect fun getData(link: String, ifErrorOccurred: (Exception) -> Unit = {}, onResult: (VideoData) -> Unit = {})

expect fun checkVideo(link: String, ifErrorOccurred: (Exception) -> Unit = {}, onResult: (Boolean) -> Unit = {})