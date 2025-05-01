package org.example.project


expect fun download(
    link: String,
    downloadPath: String,
    type: String,
    extension: String,
    resolution: String,
    onProgressChange: (String?) -> Unit
)

expect fun getData(link: String, onResult: (VideoData) -> Unit)

expect fun checkVideo(link: String, onResult: (Boolean) -> Unit)