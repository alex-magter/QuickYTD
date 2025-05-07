package org.alexmagter.QuickYTD

import androidx.compose.runtime.Composable
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

    NavHost(
        navController = navController,
        startDestination = SearchScreen.Start.name
    ) {
        composable(route = SearchScreen.Start.name) {
            App(navController, sharedViewModel)
        }

        composable(route = SearchScreen.VideoPage.name) {
            VideoPage(sharedViewModel, fileSaver)
        }
    }
}