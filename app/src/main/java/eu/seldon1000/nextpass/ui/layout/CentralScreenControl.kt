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
import eu.seldon1000.nextpass.CentralAppControl
import eu.seldon1000.nextpass.api.NextcloudApi
import eu.seldon1000.nextpass.ui.screens.*

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun CentralScreenControl() {
    val navController = rememberNavController()
    val scaffoldState = rememberScaffoldState()

    CentralAppControl.setNavController(controller = navController)
    CentralAppControl.setSnackbarHostState(snackbar = scaffoldState.snackbarHostState)

    val currentScreen by navController.currentBackStackEntryAsState()

    val storedPasswords by NextcloudApi.storedPasswords.collectAsState()
    val storedFolders by NextcloudApi.storedFolders.collectAsState()

    val refreshing by CentralAppControl.refreshing.collectAsState()
    val refreshState = rememberSwipeRefreshState(isRefreshing = refreshing)

    MyAlertDialog()
    Scaffold(
        scaffoldState = scaffoldState,
        snackbarHost = { MySnackbar(snackbarHostState = scaffoldState.snackbarHostState) }) {
        SwipeRefresh(
            state = refreshState,
            onRefresh = {
                CentralAppControl.refreshLists { NextcloudApi.refreshServerList() }
            },
            swipeEnabled = currentScreen?.destination?.route?.contains(other = Routes.AccessPin.route) == false &&
                    currentScreen?.destination?.route?.contains(other = Routes.WebView.route) == false &&
                    currentScreen?.destination?.route != Routes.Welcome.route &&
                    currentScreen?.destination?.route != Routes.Settings.route &&
                    currentScreen?.destination?.route != Routes.About.route &&
                    currentScreen?.destination?.route != Routes.Pin.route
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.Welcome.route
            ) {
                composable(route = "get_yourself_together_google") {} //TODO: remove when new navigation alpha is out (maybe, I don't know)
                composable(route = Routes.Welcome.route) { WelcomeScreen() }
                composable(route = Routes.Search.route) { Search() }
                composable(route = Routes.Passwords.route) { PasswordList() }
                composable(route = Routes.NewPassword.route) { NewPassword() }
                composable(route = Routes.NewFolder.route) { NewFolder() }
                composable(route = Routes.Favorites.route) { Favorites() }
                composable(route = Routes.Settings.route) { Settings() }
                composable(route = Routes.About.route) { About() }
                composable(route = Routes.Pin.route) { ChangePin() }
                composable(
                    route = Routes.AccessPin.route,
                    arguments = listOf(element = navArgument(name = "shouldRaiseBiometric") {
                        type = NavType.BoolType
                    })
                ) { AccessPin(shouldRaiseBiometric = it.arguments?.getBoolean("shouldRaiseBiometric")!!) }
                composable(
                    route = Routes.WebView.route,
                    arguments = listOf(element = navArgument(name = "url") {
                        type = NavType.StringType
                    })
                ) { WebPageVisualizer(urlToRender = it.arguments?.getString("url")!!) }
                composable(
                    route = Routes.PasswordDetails.route,
                    arguments = listOf(element = navArgument(name = "data") {
                        type = NavType.IntType
                    })
                ) { PasswordDetails(passwordData = storedPasswords[it.arguments?.getInt("data")!!]) }
                composable(
                    route = Routes.FolderDetails.route,
                    arguments = listOf(element = navArgument(name = "data") {
                        type = NavType.IntType
                    })
                ) { FolderDetails(folder = storedFolders[it.arguments?.getInt("data")!!]) }
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