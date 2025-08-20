package org.alexmagter.QuickYTD

import java.io.File

fun getContentTypes(videoDataFile: File?): List<String> {

    if(videoDataFile == null) { return listOf("Error") }

    val fullData =  videoDataFile.readLines()
        .filter { it.isNotBlank() }
        .map { linea ->
            linea.split(";").map { it.trim() }.toTypedArray()
        }
        .toTypedArray()

    val types = mutableListOf<String>()

    for (column in fullData) {

        types.add(column[0])
    }

    return types.toSet().toList();
}

fun getContentExtensions(videoDataFile: File?, contentType: String?): List<String> {

    if (contentType == null) return listOf("No Options")

    if (videoDataFile == null) { return listOf("Error") }

    val fullData =  videoDataFile.readLines()
        .filter { it.isNotBlank() }
        .map { linea ->
            linea.split(";").map { it.trim() }.toTypedArray()
        }
        .toTypedArray()

    val types = mutableListOf<String>()

    for (column in fullData) {
        if(column[0].equals(contentType, ignoreCase = true))
        types.add(column[1])
    }

    return types.toSet().toList();
}

fun getContentResolutions(videoDataFile: File?, extension: String?): List<String> {

    if (extension == null) return listOf("No Options")

    if (videoDataFile == null) { return listOf("Error") }

    val fullData =  videoDataFile.readLines()
        .filter { it.isNotBlank() }
        .map { linea ->
            linea.split(";").map { it.trim() }.toTypedArray()
        }
        .toTypedArray()

    val types = mutableListOf<String>()

    for (column in fullData) {
        if(column[1].equals(extension, ignoreCase = true))
        types.add(column[2])
    }

    return types.toSet().toList();
}

fun getSize(videoDataFile: File?, contentType: String?, extension: String?, resolution: String?): String{
    if (videoDataFile == null || contentType == null || extension == null || resolution == null) return "0MiB"



    val fullData =  videoDataFile.readLines()
        .filter { it.isNotBlank() }
        .map { linea ->
            linea.split(";").map { it.trim() }.toTypedArray()
        }
        .toTypedArray()

    val types = mutableListOf<String>()

    for (column in fullData) {
        if(column[0].equals(contentType, ignoreCase = true) && column[1].equals(extension, ignoreCase = true) && column[2].equals(resolution, ignoreCase = true)){
            return column[3]
        }
    }

    return "0MiB"

}