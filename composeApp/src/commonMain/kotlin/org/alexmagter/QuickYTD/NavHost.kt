package org.alexmagter.QuickYTD

import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

enum class SearchScreen() {
    Start,
    VideoPage,
    Share
}



@Composable
fun Navigation(fileSaver: FileSaver){
    val navController = rememberNavController()
    val sharedViewModel: SharedViewModel = viewModel()
    val animationDurationMillis = 300
    val slideUnderPercentage = 0.2
    val fadePercentage = 0.2f

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        NavHost(
            navController = navController,
            startDestination = SearchScreen.Start.name,
            enterTransition = {

                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth }, // Entra desde la derecha (fuera de la pantalla a la derecha)
                    animationSpec = tween(durationMillis = animationDurationMillis)
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> (-fullWidth*slideUnderPercentage).toInt() }, // Sale hacia la izquierda (fuera de la pantalla a la izquierda)
                    animationSpec = tween(durationMillis = animationDurationMillis)
                ) + fadeOut(
                    targetAlpha = fadePercentage,
                    animationSpec = tween(durationMillis = animationDurationMillis)
                )
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -(fullWidth*slideUnderPercentage).toInt() }, // Entra desde la izquierda
                    animationSpec = tween(durationMillis = animationDurationMillis)
                ) + fadeIn(
                    initialAlpha = fadePercentage,
                    animationSpec = tween(durationMillis = animationDurationMillis)
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth }, // Sale hacia la derecha
                    animationSpec = tween(durationMillis = animationDurationMillis)
                )
            },

            ) {
            composable(route = SearchScreen.Start.name) {
                App(navController, sharedViewModel)
            }

            composable(route = SearchScreen.VideoPage.name) {
                VideoPage(navController, sharedViewModel, fileSaver)
            }
        }
    }


}