package org.example.project

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

enum class SearchScreen() {
    Start,
    VideoPage,
    Share
}


@Composable
fun Navigation(){
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
            VideoPage(sharedViewModel)
        }
    }
}