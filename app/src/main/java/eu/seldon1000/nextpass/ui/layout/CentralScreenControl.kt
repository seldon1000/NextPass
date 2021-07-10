/*
 * Copyright 2021 Nicolas Mariniello
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.seldon1000.nextpass.ui.layout

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.*
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import eu.seldon1000.nextpass.api.NextcloudApiProvider
import eu.seldon1000.nextpass.ui.MainViewModel
import eu.seldon1000.nextpass.ui.screens.*

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalMaterialApi
@Composable
fun CentralScreenControl() {
    val navController = rememberNavController()
    MainViewModel.setNavController(navController = navController)

    val currentScreen by navController.currentBackStackEntryAsState()

    val scaffoldState = rememberScaffoldState()
    MainViewModel.setSnackbarHostState(snackbar = scaffoldState.snackbarHostState)

    val storedPasswords by NextcloudApiProvider.storedPasswords.collectAsState()
    val storedFolders by NextcloudApiProvider.storedFolders.collectAsState()

    val refreshing by MainViewModel.refreshing.collectAsState()
    val refreshState = rememberSwipeRefreshState(isRefreshing = refreshing)

    MyAlertDialog()
    Scaffold(
        scaffoldState = scaffoldState,
        snackbarHost = { MySnackbar(snackbarHostState = scaffoldState.snackbarHostState) }) {
        SwipeRefresh(
            state = refreshState,
            onRefresh = { NextcloudApiProvider.refreshServerList() },
            swipeEnabled = currentScreen?.destination?.route?.contains("access_pin") == false &&
                    currentScreen?.destination?.route?.contains("webview") == false &&
                    currentScreen?.destination?.route != "welcome" &&
                    currentScreen?.destination?.route != "settings" &&
                    currentScreen?.destination?.route != "about" &&
                    currentScreen?.destination?.route != "pin"
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.Welcome.route
            )
            {
                composable(
                    route = Routes.AccessPin.route,
                    listOf(navArgument(name = "shouldRaiseBiometric") { type = NavType.BoolType })
                ) { AccessPin(shouldRaiseBiometric = it.arguments?.getBoolean("shouldRaiseBiometric")!!) }
                composable(
                    route = Routes.WebView.route,
                    listOf(navArgument(name = "url") { type = NavType.StringType })
                ) { WebPageVisualizer(urlToRender = it.arguments?.getString("url")!!) }
                composable(route = Routes.Welcome.route) { WelcomeScreen() }
                composable(route = Routes.Search.route) { Search() }
                composable(route = Routes.Passwords.route) { PasswordList() }
                composable(route = Routes.NewPassword.route) { NewPassword() }
                composable(route = Routes.NewFolder.route) { NewFolder() }
                composable(route = Routes.Favorites.route) { Favorites() }
                composable(route = Routes.Settings.route) { Settings() }
                composable(route = Routes.About.route) { About() }
                composable(
                    route = Routes.PasswordDetails.route,
                    listOf(navArgument(name = "data") { type = NavType.IntType })
                ) { PasswordDetails(passwordData = storedPasswords[it.arguments?.getInt("data")!!]) }
                composable(
                    route = Routes.FolderDetails.route,
                    listOf(navArgument(name = "data") { type = NavType.IntType })
                ) { FolderDetails(folder = storedFolders[it.arguments?.getInt("data")!!]) }
                composable(
                    route = Routes.Pin.route,
                    listOf(navArgument(name = "change") { type = NavType.BoolType })
                ) { ChangePin(change = it.arguments?.getBoolean("change")!!) }
            }
        }
    }
}

@Composable
fun MySnackbar(snackbarHostState: SnackbarHostState) {
    SnackbarHost(
        hostState = snackbarHostState,
        snackbar = { data ->
            Snackbar(
                backgroundColor = Color.DarkGray,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 96.dp)
            ) { Text(text = data.message, color = Color.White) }
        })
}