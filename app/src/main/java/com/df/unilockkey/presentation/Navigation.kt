package com.df.unilockkey.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun Navigation(
    onBluetoothStateChanged:() -> Unit,
) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.StartScreen.route) {
        composable(Screen.StartScreen.route) {
            StartScreen(navController)
        }
        composable(Screen.KeyInfoScreen.route) {
            KeyInfoScreen(
                onBluetoothStateChanged
            )
        }
    }
}


sealed class Screen(val route: String) {
    object StartScreen: Screen("start_screen")
    object KeyInfoScreen: Screen("key_info_screen")
}