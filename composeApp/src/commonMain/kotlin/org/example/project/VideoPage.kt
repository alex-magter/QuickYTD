package org.example.project

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import org.intellij.lang.annotations.JdkConstants.HorizontalAlignment
import org.jetbrains.compose.ui.tooling.preview.Preview




val robotoNormal = FontFamily(
    Font(
        resource = "fonts/Roboto-VariableFont_wdth,wght.ttf",
        weight = FontWeight.Normal // Puedes usar Bold, Medium, etc. también
    )
)

val robotoLight = FontFamily(
    Font(
        resource = "fonts/Roboto-VariableFont_wdth,wght.ttf",
        weight = FontWeight.Light // Puedes usar Bold, Medium, etc. también
    )
)

val robotoBold = FontFamily(
    Font(
        resource = "fonts/Roboto-VariableFont_wdth,wght.ttf",
        weight = FontWeight.Bold // Puedes usar Bold, Medium, etc. también
    )
)


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class,
    ExperimentalAnimationApi::class
)
@Preview
@Composable
fun VideoPage(viewModel: SharedViewModel) {

    val videoData = viewModel.videoData
    val link = videoData.link
    val thumbnail = remember(videoData.thumbnail) {
        fileToBitmap(videoData.thumbnail)
    }
    val videoName: String = videoData.videoName.readText()
    val videoChannel: String = videoData.channelName.readText()



    MaterialTheme {
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

        var progress by remember { mutableStateOf<String?>("") }
        var isDownloading by remember { mutableStateOf(false) }

        LaunchedEffect(density) {
            windowWidth.value = with(density) {
                900.dp.coerceAtMost( // Usa el menor entre 900.dp y 95% del ancho
                    (windowWidth.value.value * 2f).dp
                )
            }
        }

        Scaffold(
            containerColor = DarkTheme.backgroundColor,
            modifier = Modifier.fillMaxSize()
        ) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
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
                        Image(
                            bitmap = thumbnail,
                            contentDescription = "TODO",
                            modifier = Modifier
                                .aspectRatio(16f/9f)
                                .clip(RoundedCornerShape(16.dp))
                                .fillMaxWidth(),
                            alignment = Alignment.Center
                        )

                        Text(
                            text = videoName ?: "",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            textAlign = TextAlign.Left,
                            modifier = Modifier.fillMaxWidth(),
                            fontFamily = robotoNormal
                        )

                        Text(
                            text = videoChannel ?: "",
                            fontWeight = FontWeight.Thin,
                            color = Color.White,
                            textAlign = TextAlign.Left,
                            modifier = Modifier.fillMaxWidth(),
                            fontFamily = robotoNormal
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

                        DownloadProgress(
                            label = "Download progress",
                            progress = progress,
                            isDownloading = isDownloading
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
                        Button(
                            onClick = {
                                /*if (videoData != null) {
                                    isDownloading = true
                                    download(
                                        link = videoData.link,
                                        type = selectedType,
                                        extension = selectedExtension,
                                        resolution = selectedResolution,
                                        onProgressChange = {
                                            progress = it
                                        }
                                    )
                                }*/
                            },
                            enabled = selectedResolution != "",
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
                                Text("Download")
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Filled.Download, contentDescription = "Descargar")
                            }
                        }

                        val scope = rememberCoroutineScope()
                        Button(
                            onClick = {
                                scope.launch {
                                    selectFolder(videoName + "." + selectedExtension, "audio/mp4") {
                                        if(it != null){
                                            download(
                                                link = link,
                                                downloadPath = it,
                                                type = selectedType,
                                                extension = selectedExtension,
                                                resolution = selectedResolution,
                                                onProgressChange = {

                                                }
                                            )
                                        }
                                    }
                                }
                            },
                            enabled = selectedResolution != "",
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