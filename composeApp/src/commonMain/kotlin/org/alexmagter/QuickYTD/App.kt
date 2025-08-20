package org.alexmagter.QuickYTD

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import quickytd.composeapp.generated.resources.Res
import quickytd.composeapp.generated.resources.github
import quickytd.composeapp.generated.resources.invalid_link
import quickytd.composeapp.generated.resources.loading
import quickytd.composeapp.generated.resources.put_link
import quickytd.composeapp.generated.resources.search


@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App(navController: NavController, viewModel: SharedViewModel, sharedData: String? = null) {
    MaterialTheme {
        var link by remember { mutableStateOf(sharedData ?: "") }
        var theme = "Dark"
        var canSearch by remember { mutableStateOf(true) }
        var isLinkInvalid by remember { mutableStateOf(false) }
        var isGettingData by remember { mutableStateOf(false) }
        var hadErrorGettingVideo by remember { mutableStateOf(false) }
        var error: String = ""

        val density = LocalDensity.current
        val windowWidth = remember { mutableStateOf(700.dp) }

        val maxLinkLenght = 90

        val scope = rememberCoroutineScope()
        val uriHandler = LocalUriHandler.current

        Scaffold(
            containerColor = DarkTheme.backgroundColor,
            modifier = Modifier
                .fillMaxSize()
                .background(DarkTheme.backgroundColor)
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f), // ocupa la mitad de la altura
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = buildAnnotatedString {
                        append("Quick")
                        withStyle(style = SpanStyle(color = Color.Red)) {
                            append("YT") // letra "E" en azul y negrita
                        }
                        withStyle(style = SpanStyle(color = Color.Blue)) {
                            append("D") // letra "E" en azul y negrita
                        }

                    },
                    color = Color.White,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }



            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                fun searchVideo() {
                    canSearch = false
                    Video.checkVideo(
                        link = link,
                        ifErrorOccurred = {
                            hadErrorGettingVideo = true
                            error = it.toString()
                        }
                    ) { result ->
                        isLinkInvalid = !result
                        if (result) {
                            isGettingData = true

                            scope.launch {
                                val video = withContext(Dispatchers.IO) { Video(link) }

                                withContext(Dispatchers.Main) {
                                    isGettingData = false
                                    viewModel.video = video
                                    navController.navigate("VideoPage")
                                }
                            }
                        } else {
                            link = ""
                            canSearch = true
                        }
                    }
                }

                LaunchedEffect(sharedData) {
                    if(sharedData != null && canSearch) searchVideo()
                }

                OutlinedTextField(
                    value = link,
                    onValueChange = { newText: String ->
                        if (newText.length <= maxLinkLenght) {
                            link = newText.withoutSpaces()
                            isLinkInvalid = false
                        }
                    },
                    textStyle = TextStyle(color = Color.White),
                    shape = RoundedCornerShape(50.dp),
                    colors = DarkTheme.textFieldColors(),
                    modifier = Modifier
                        .width(windowWidth.value)
                        .padding(8.dp, 0.dp)
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyUp && keyEvent.key == Key.Enter && canSearch) {
                                searchVideo()
                                return@onPreviewKeyEvent true
                            } else return@onPreviewKeyEvent false
                       },
                    placeholder = { Text(stringResource(Res.string.put_link)) },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            searchVideo()
                        }
                    )
                )

                Button(
                    onClick = { searchVideo() },
                    enabled = canSearch,
                    colors = DarkTheme.SearcbButtonColors(canSearch)
                ){
                    Text(stringResource(Res.string.search))
                }


                LoadingText(isGettingData)
                NoValidLinkWarning(!isLinkInvalid)
                ErrorWarning(hadErrorGettingVideo, error)

            }

            Box(
                Modifier
                    .fillMaxSize()
                    .padding(it)
            ){
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.BottomStart) // Alinea la Row a la parte inferior izquierda del Box
                        .padding(16.dp) // Añade un padding para separarlo de los bordes
                        .clickable { // Añadir el modificador clickable aquí
                            uriHandler.openUri("https://github.com/alex-magter")
                        }
                ) {
                    Image(
                        painter = painterResource(Res.drawable.github),
                        contentDescription = "GitHub logo",
                        modifier = Modifier.size(30.dp)
                    )
                    Text(
                        text = "/alex-magter",
                        color = Color.White,
                        style = TextStyle(textDecoration = TextDecoration.Underline),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

        }
    }
}

@Composable
fun LoadingText(isLoading: Boolean) {
    val offsetY by remember { mutableStateOf<(Int) -> Int>({ -40 }) }

    AnimatedVisibility(
        visible = isLoading,
        enter = slideInVertically(initialOffsetY = offsetY) + expandIn(expandFrom = Alignment.Center),
        exit = slideOutVertically(targetOffsetY = offsetY) + shrinkOut(shrinkTowards = Alignment.Center)
    ) {
        Text(
            text = "${stringResource(Res.string.loading)}...",
            color = Color.Gray
        )
    }
}

@Composable
fun NoValidLinkWarning(valid: Boolean) {
    val offsetY by remember { mutableStateOf<(Int) -> Int>({ -40 }) }

    AnimatedVisibility(
        visible = !valid,
        enter = slideInVertically(initialOffsetY = offsetY) + expandIn(expandFrom = Alignment.Center),
        exit = slideOutVertically(targetOffsetY = offsetY) + shrinkOut(shrinkTowards = Alignment.Center)
    ) {
        Text(
            text = stringResource(Res.string.invalid_link),
            color = Color.Red
        )
    }
}

@Composable
fun ErrorWarning(hadError: Boolean, error: String) {
    val offsetY by remember { mutableStateOf<(Int) -> Int>({ -40 }) }

    AnimatedVisibility(
        visible = hadError,
        enter = slideInVertically(initialOffsetY = offsetY) + expandIn(expandFrom = Alignment.Center),
        exit = slideOutVertically(targetOffsetY = offsetY) + shrinkOut(shrinkTowards = Alignment.Center)
    ) {
        Text(
            text = "Error searching video: $error",
            color = Color.Red
        )
    }
}

private fun String.withoutSpaces(): String {
    return this.replace("\\s+".toRegex(), "")
}
