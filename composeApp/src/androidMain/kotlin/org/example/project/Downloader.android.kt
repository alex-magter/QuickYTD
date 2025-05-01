package org.example.project

actual fun getData(link: String, onResult: (VideoData) -> Unit) {
}

actual fun checkVideo(link: String, onResult: (Boolean) -> Unit){
}

actual fun download(
    link: String,
    downloadPath: String,
    type: String,
    extension: String,
    resolution: String,
    onProgressChange: (String?) -> Unit
) {
}