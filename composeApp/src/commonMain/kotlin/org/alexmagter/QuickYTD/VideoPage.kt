package org.alexmagter.QuickYTD

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import java.io.File


val roboto = getRoboto()


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class,
    ExperimentalAnimationApi::class
)
@Preview
@Composable
fun VideoPage(viewModel: SharedViewModel, fileSaver: FileSaver) {
    MaterialTheme {

        val videoData = remember { viewModel.videoData }
        val link = remember { videoData.link }
        val thumbnail = remember(videoData.thumbnail) {
            fileToBitmap(videoData.thumbnail)
        }
        val videoName: String = videoData.videoName.readText()
        val videoChannel: String = videoData.channelName.readText()

        var theme = "Dark"

        val density = LocalDensity.current
        val windowWidth = remember { mutableStateOf(900.dp) }

        var selectedType by remember { mutableStateOf<String>("") }
        val contentTypes = getContentTypes(videoDataFile = videoData.fileData)

        var selectedExtension by remember { mutableStateOf<String>("") }
        var extensionTypes = getContentExtensions(videoDataFile = videoData.fileData, contentType = selectedType)

        var selectedResolution by remember { mutableStateOf<String>("") }
        var resolutions = getContentResolucions(videoDataFile = videoData.fileData, extension = selectedExtension)
        var videoSize by remember { mutableStateOf<String>("") }

        var isVertical by remember { mutableStateOf(false) }

        var progress by remember { mutableDoubleStateOf(0.0) }
        var isDownloading by remember { mutableStateOf(false) }

        var isChoosingPath by remember { mutableStateOf(false) }

        var downloadTask by remember { mutableStateOf("Starting download...") }
        var downloadError by remember { mutableStateOf(false) }
        var isDownloadCompele by remember { mutableStateOf(false) }
        var isDownloadCancelled by remember { mutableStateOf(false) }

        LaunchedEffect(density) {
            windowWidth.value = 900.dp.coerceAtMost( // Usa el menor entre 900.dp y 95% del ancho
                (windowWidth.value.value * 2f).dp
            )
        }

        Scaffold(
            containerColor = DarkTheme.backgroundColor,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer()
                .background(DarkTheme.backgroundColor)
        ) { innerPadding ->

            DownloadWindow(downloading = isDownloading,
                label = downloadTask,
                progress = progress,
                downloadComplete = isDownloadCompele,
                error = downloadError,
                onExit = {
                    downloadTask = "Downloading..."
                    progress = 0.0
                    downloadError = false
                    isDownloadCompele = false
                    isDownloading = false
                    isDownloadCancelled = false
                },
                onCancelRequest = {
                    downloadTask = "Cancelling download..."
                    isDownloadCancelled = true
                    cancelDownload()
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Box(
                    modifier = Modifier
                        .width(400.dp) // Ancho fijo para la zona de contenido
                        .fillMaxHeight() // Alto completo
                        .align(Alignment.Center) // Centrado en el medio
                        .padding(16.dp) // Padding opcional dentro del contenido
                ) {

                    Column (
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer( Modifier.height(3.dp) )
                        if(thumbnail != null) {
                            Image(
                                bitmap = thumbnail                            ,
                                contentDescription = "TODO",
                                modifier = Modifier
                                    .aspectRatio(16f/9f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .fillMaxWidth(),
                                alignment = Alignment.Center
                            )
                        }

                        Text(
                            text = videoName ?: "",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            textAlign = TextAlign.Left,
                            modifier = Modifier.fillMaxWidth(),
                            fontFamily = roboto
                        )

                        Text(
                            text = videoChannel ?: "",
                            fontWeight = FontWeight.Thin,
                            color = Color.White,
                            textAlign = TextAlign.Left,
                            modifier = Modifier.fillMaxWidth(),
                            fontFamily = roboto
                        )

                        Spacer( Modifier.height(3.dp) )

                        Dropdown(
                            label = "Choose content type",
                            elements = contentTypes,
                            selectedValue = selectedType,
                            onValueChange = {
                                selectedType = it ?: ""
                                selectedExtension = ""
                                selectedResolution = ""

                                extensionTypes = getContentExtensions(videoDataFile = videoData.fileData, contentType = selectedType)
                            }
                        )

                        AnimatedVisibility(
                            visible = selectedType != "",

                        ){
                            Column {
                                Dropdown(
                                    label = "Choose extension",
                                    elements = extensionTypes,
                                    selectedValue = selectedExtension,
                                    onValueChange = {
                                        selectedExtension = it ?: ""
                                        selectedResolution = ""
                                        resolutions = getContentResolucions(videoDataFile = videoData.fileData, extension = selectedExtension)
                                    }
                                )

                                AnimatedVisibility(
                                    visible = selectedExtension != ""
                                ) {

                                    Column {
                                        Dropdown(
                                            label = "Choose quality",
                                            elements = resolutions,
                                            selectedValue = selectedResolution
                                        ) {
                                            selectedResolution = it ?: ""


                                            videoSize = getSize(
                                                videoDataFile = videoData.fileData,
                                                contentType = selectedType,
                                                extension = selectedExtension,
                                                resolution = selectedResolution
                                            )

                                        }
                                    }
                                }
                            }
                        }

                        FileSize(
                            size = videoSize,
                            areFieldsFilled = selectedResolution != ""
                        )
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(8.dp)
                            .height(60.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        fun onResult( successful: Boolean ){
                            isDownloadCompele = true

                            if(successful){
                                if(isDownloadCancelled){
                                    downloadTask = "Download canceled successfully"
                                } else {
                                    downloadTask = "Download completed successfully"
                                }

                            } else {
                                downloadTask = "Error while downloading"
                                downloadError = true
                            }
                        }

                        fun onProgress(taskProgress: Double, task: String){
                            progress = taskProgress/100;
                            print(progress); print("\n")
                            downloadTask = task
                        }

                        Button(
                            onClick = {
                                isDownloading = true
                                progress = 0.0

                                val name = videoName

                                download(
                                    link = link,
                                    downloadPath = fileSaver.getDownloadsFolder(),
                                    filename = sanitizeFileName(name),
                                    type = selectedType,
                                    extension = selectedExtension,
                                    resolution = selectedResolution,
                                    savedAs = false,
                                    onResult = { successful ->

                                        onResult(successful)

                                    },
                                    onProgressChange = { taskProgress, task ->

                                        onProgress(taskProgress, task)
                                    }
                                )
                            },
                            enabled = selectedResolution != "" && !isChoosingPath,
                            colors = DarkTheme.ButtonColors(selectedResolution != ""),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(1.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("Send to downloads")
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Filled.Download, contentDescription = "Descargar")
                            }
                        }

                        val scope = rememberCoroutineScope()
                        Button(

                            onClick = {
                                isChoosingPath = true

                                scope.launch {
                                    fileSaver.selectFolder("${sanitizeFileName(videoName)}.$selectedExtension",
                                        selectedExtension) { stream, path, name ->
                                        isChoosingPath = false
                                        if(path != null && name != null){
                                            isDownloading = true
                                            progress = 0.0

                                            println(path)

                                            download(
                                                link = link,
                                                downloadPath = path,
                                                filename = name,
                                                type = selectedType,
                                                extension = selectedExtension,
                                                resolution = selectedResolution,
                                                savedAs = true,
                                                onProgressChange = { taskProgress, task ->

                                                    onProgress(taskProgress, task)

                                                },
                                                onResult = { successful ->

                                                    onResult(successful)
                                                }
                                            )
                                        }
                                    }
                                }
                            },
                            enabled = selectedResolution != "" && !isChoosingPath,
                            colors = DarkTheme.ButtonColors(selectedResolution != ""),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(1.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("Save as")
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Filled.SaveAs, contentDescription = "Compartir")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Dropdown(label: String, elements: List<String>, selectedValue: String, onValueChange: (String?) -> Unit = { }){

    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .menuAnchor(),
            colors = DarkTheme.textFieldColors(),
            shape = DarkTheme.dropdownShape
        )


        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            elements.forEach { dropdownOption ->
                DropdownMenuItem(
                    text = { Text(dropdownOption) },
                    onClick = {
                        if(selectedValue != dropdownOption) onValueChange(dropdownOption)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun FileSize(size: String, areFieldsFilled: Boolean) {
    AnimatedVisibility(
        visible = areFieldsFilled,
        enter = slideInVertically(initialOffsetY = { -40 }) +
                expandIn(expandFrom = Alignment.Center),
        exit = slideOutVertically(targetOffsetY = { -40 }) + shrinkOut( shrinkTowards = Alignment.Center )
    ) {
        Text(
            text = "Estimated filesize: $size",
            color = Color.White,
            modifier = Modifier.padding(5.dp)
        )
    }
}

@Composable
fun DownloadProgress(label: String?, progress: String?, isDownloading: Boolean) {
    AnimatedVisibility(
        visible = isDownloading,
        enter = slideInVertically(initialOffsetY = { -40 }) +
                expandIn(expandFrom = Alignment.Center),
        exit = slideOutVertically(targetOffsetY = { -40 }) + shrinkOut( shrinkTowards = Alignment.Center )
    ) {
        Text(
            text = "$label: $progress",
            color = Color.White,
            modifier = Modifier.padding(5.dp)
        )
    }
}

@Composable
fun DownloadWindow(
    downloading: Boolean = false,
    label: String?, progress: Double,
    downloadComplete: Boolean,
    error: Boolean,
    onExit: () -> Unit = {},
    onCancelRequest: () -> Unit = {}){
    if(downloading){
        Dialog(
            onDismissRequest = {
                if(downloadComplete){
                    onExit()
                }
            }
        ){
            Box(
                modifier = Modifier
                    .background(DarkTheme.backgroundColor, shape = DarkTheme.dropdownShape)
                    .padding(16.dp)
                    .width(300.dp)
                    .height(200.dp)

            ) {

                Column (
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text("$label",
                        color = if(error) Color.Red else Color.White
                    )



                    if(!downloadComplete){

                        Spacer(Modifier.height(20.dp))
                        LinearProgressIndicator(
                            progress = { progress.toFloat() },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = { onCancelRequest() },
                            enabled = true,
                            colors = DarkTheme.CancelButtonColors(true),
                            modifier = Modifier
                                .width(150.dp)
                                .height(50.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(1.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,

                                ) {
                                Text("Cancel")
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Filled.Cancel, contentDescription = "Cancel the download")
                            }
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = {
                                onExit()
                            },
                            enabled = true,
                            colors = DarkTheme.ButtonColors(true),
                            modifier = Modifier
                                .width(150.dp)
                                .height(50.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(1.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,

                                ) {
                                Text("Close")
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Filled.Close, contentDescription = "Close the dialog")
                            }
                        }
                    }
                }
            }
        }
    }
}



