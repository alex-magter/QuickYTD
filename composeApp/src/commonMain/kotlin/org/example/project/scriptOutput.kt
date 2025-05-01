package org.example.project

import java.io.File

data class scriptOutput(val path : File, val output : List<String>){
    val scriptPath: File = path
    val data : List<String> = output
}