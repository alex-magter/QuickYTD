package org.example.project

import java.io.File

data class VideoData(val pythonPath: File, val videoLink: String){
    private val basePath = pythonPath.absoluteFile
    private val outputPath = File(pythonPath, "dataOutput")

    val fileData = File(outputPath, "output.csv")
    val videoName = File(outputPath, "name.txt")
    val channelName = File(outputPath, "channel.txt")
    val thumbnail = File(outputPath, "img.jpg")

    val link: String = videoLink
}
