package org.example.project

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHost
import androidx.navigation.compose.NavHost
import org.jetbrains.compose.ui.tooling.preview.Preview





@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App(navController: NavController, viewModel: SharedViewModel) {
    MaterialTheme {
        var link by remember { mutableStateOf("") }
        var theme = "Dark"
        var isLinkInvalid by remember { mutableStateOf(false) }
        var isGettingData by remember { mutableStateOf(false) }

        val density = LocalDensity.current
        val windowWidth = remember { mutableStateOf(700.dp) }

        val maxLinkLenght = 60

        Scaffold(
            containerColor = DarkTheme.backgroundColor,  // Color de fondo del Scaffold
            modifier = Modifier
                .fillMaxSize()
                .background(DarkTheme.backgroundColor)  // Fondo personalizado para la ventana
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = link,
                    onValueChange = { newText: String ->
                        if (newText.length <= maxLinkLenght) {
                            link = newText.withoutSpaces()
                            isLinkInvalid = false
                        }
                    },
                    textStyle = TextStyle(color = Color.White),
                    shape = RoundedCornerShape(50.dp), // Redondeado completamente
                    colors = DarkTheme.textFieldColors(),
                    modifier = Modifier
                        .width(windowWidth.value)
                        .padding(8.dp, 0.dp),
                    placeholder = { Text("Introduce el link", color = Color.Gray) },
                )

                Button(onClick = {
                    checkVideo(link = link) { result ->
                        isLinkInvalid = !result
                        if (result) {
                            isGettingData = true
                            getData(link) { data ->
                                viewModel.videoData = data
                                navController.navigate("VideoPage")
                            }
                        }
                    }
                }) {
                    Text("Buscar")
                }

                LoadingText(isGettingData)
                NoValidLinkWarning(!isLinkInvalid)
            }
        }
    }
}

@Composable
fun LoadingText(isLoading: Boolean) {
    AnimatedVisibility(
        visible = isLoading,
        enter = slideInVertically(initialOffsetY = { -40 }) + expandIn(expandFrom = Alignment.Center),
        exit = slideOutVertically(targetOffsetY = { -40 }) + shrinkOut(shrinkTowards = Alignment.Center)
    ) {
        Text(
            text = "Cargando datos...",
            color = Color.Gray
        )
    }
}

@Composable
fun NoValidLinkWarning(valid: Boolean) {
    AnimatedVisibility(
        visible = !valid,
        enter = slideInVertically(initialOffsetY = { -40 }) + expandIn(expandFrom = Alignment.Center),
        exit = slideOutVertically(targetOffsetY = { -40 }) + shrinkOut(shrinkTowards = Alignment.Center)
    ) {
        Text(
            text = "El link no es válido",
            color = Color.Red
        )
    }
}

private fun String.withoutSpaces(): String {
    return this.replace("\\s+".toRegex(), "")
}
