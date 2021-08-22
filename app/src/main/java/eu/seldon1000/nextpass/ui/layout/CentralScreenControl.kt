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
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navArgument
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import eu.seldon1000.nextpass.MainViewModel
import eu.seldon1000.nextpass.ui.screens.*

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun CentralScreenControl(viewModel: MainViewModel) {
    val context = LocalContext.current

    val navController = rememberAnimatedNavController()
    val scaffoldState = rememberScaffoldState()

    viewModel.setNavController(controller = navController)
    viewModel.setSnackbarHostState(snackbar = scaffoldState.snackbarHostState)

    val storedPasswords by viewModel.nextcloudApi.storedPasswords.collectAsState()
    val storedFolders by viewModel.nextcloudApi.storedFolders.collectAsState()

    val currentScreen by viewModel.navController.collectAsState().value.currentBackStackEntryAsState()
    val refreshing by viewModel.refreshing.collectAsState()
    val refreshState = rememberSwipeRefreshState(isRefreshing = refreshing)

    MyAlertDialog(viewModel = viewModel)
    Scaffold(
        scaffoldState = scaffoldState,
        snackbarHost = { MySnackbar(snackbarHostState = scaffoldState.snackbarHostState) }) {
        SwipeRefresh(
            state = refreshState,
            onRefresh = { viewModel.executeRequest { viewModel.nextcloudApi.refreshServerList() } },
            swipeEnabled = currentScreen?.destination?.route == Routes.Search.route ||
                    currentScreen?.destination?.route == Routes.Passwords.route ||
                    currentScreen?.destination?.route == Routes.Favorites.route ||
                    currentScreen?.destination?.route == Routes.PasswordDetails.route ||
                    currentScreen?.destination?.route == Routes.FolderDetails.route
        ) {
            AnimatedNavHost(
                navController = navController,
                startDestination = when {
                    context.getSharedPreferences("nextpass", 0)
                        .contains("server") -> Routes.Passwords.route
                    else -> Routes.Welcome.route
                }
            ) {
                composable(route = "seldon1000") {} //TODO: hmm, compose navigation doesn't like 13
                composable(route = Routes.Welcome.route) { WelcomeScreen(viewModel = viewModel) }
                composable(route = Routes.Search.route) { Search(viewModel = viewModel) }
                composable(route = Routes.Passwords.route) { PasswordList(viewModel = viewModel) }
                composable(route = Routes.Favorites.route) { Favorites(viewModel = viewModel) }
                composable(route = Routes.Settings.route) { Settings(viewModel = viewModel) }
                composable(
                    route = Routes.NewPassword.route,
                    enterTransition = { _, _ ->
                        slideInVertically(
                            initialOffsetY = { 3000 },
                            animationSpec = tween(durationMillis = 500)
                        )
                    },
                    popExitTransition = { _, _ ->
                        slideOutVertically(
                            targetOffsetY = { 3000 },
                            animationSpec = tween(durationMillis = 500)
                        )
                    }) { NewPassword(viewModel = viewModel) }
                composable(route = Routes.NewFolder.route,
                    enterTransition = { _, _ ->
                        slideInVertically(
                            initialOffsetY = { 3000 },
                            animationSpec = tween(durationMillis = 500)
                        )
                    },
                    popExitTransition = { _, _ ->
                        slideOutVertically(
                            targetOffsetY = { 3000 },
                            animationSpec = tween(durationMillis = 500)
                        )
                    }) { NewFolder(viewModel = viewModel) }
                composable(route = Routes.About.route,
                    enterTransition = { _, _ ->
                        slideInVertically(
                            initialOffsetY = { 3000 },
                            animationSpec = tween(durationMillis = 500)
                        )
                    },
                    popExitTransition = { _, _ ->
                        slideOutVertically(
                            targetOffsetY = { 3000 },
                            animationSpec = tween(durationMillis = 500)
                        )
                    }) { About(viewModel = viewModel) }
                composable(route = Routes.Pin.route,
                    enterTransition = { _, _ ->
                        slideInVertically(
                            initialOffsetY = { 3000 },
                            animationSpec = tween(durationMillis = 500)
                        )
                    },
                    popExitTransition = { _, _ ->
                        slideOutVertically(
                            targetOffsetY = { 3000 },
                            animationSpec = tween(durationMillis = 500)
                        )
                    }) { Pin(viewModel = viewModel) }
                composable(
                    route = Routes.AccessPin.route,
                    arguments = listOf(element = navArgument(name = "shouldRaiseBiometric") {
                        type = NavType.BoolType
                    }),
                    enterTransition = { _, _ ->
                        slideInVertically(
                            initialOffsetY = { -3000 },
                            animationSpec = tween(durationMillis = 500)
                        )
                    },
                    exitTransition = { _, _ ->
                        slideOutVertically(
                            targetOffsetY = { -3000 },
                            animationSpec = tween(durationMillis = 500)
                        )
                    },
                    popExitTransition = { _, _ ->
                        slideOutVertically(
                            targetOffsetY = { -3000 },
                            animationSpec = tween(durationMillis = 500)
                        )
                    }
                ) {
                    AccessPin(
                        shouldRaiseBiometric = it.arguments?.getBoolean("shouldRaiseBiometric")!!,
                        viewModel = viewModel
                    )
                }
                composable(
                    route = Routes.WebView.route,
                    arguments = listOf(element = navArgument(name = "url") {
                        type = NavType.StringType
                    }),
                    enterTransition = { _, _ ->
                        slideInVertically(
                            initialOffsetY = { 3000 },
                            animationSpec = tween(durationMillis = 500)
                        )
                    },
                    exitTransition = { _, _ ->
                        slideOutVertically(
                            targetOffsetY = { -3000 },
                            animationSpec = tween(durationMillis = 500)
                        )
                    },
                    popExitTransition = { _, _ ->
                        slideOutVertically(
                            targetOffsetY = { 3000 },
                            animationSpec = tween(durationMillis = 500)
                        )
                    }
                ) {
                    WebPageVisualizer(
                        urlToRender = it.arguments?.getString("url")!!,
                        viewModel = viewModel
                    )
                }
                composable(
                    route = Routes.PasswordDetails.route,
                    arguments = listOf(element = navArgument(name = "data") {
                        type = NavType.IntType
                    }),
                    enterTransition = { _, _ ->
                        slideInVertically(
                            initialOffsetY = { 3000 },
                            animationSpec = tween(durationMillis = 500)
                        )
                    },
                    popExitTransition = { _, _ ->
                        slideOutVertically(
                            targetOffsetY = { 3000 },
                            animationSpec = tween(durationMillis = 500)
                        )
                    }
                ) {
                    PasswordDetails(
                        passwordData = storedPasswords[it.arguments?.getInt("data")!!],
                        viewModel = viewModel
                    )
                }
                composable(
                    route = Routes.FolderDetails.route,
                    arguments = listOf(element = navArgument(name = "data") {
                        type = NavType.IntType
                    }),
                    enterTransition = { _, _ ->
                        slideInVertically(
                            initialOffsetY = { 3000 },
                            animationSpec = tween(durationMillis = 500)
                        )
                    },
                    popExitTransition = { _, _ ->
                        slideOutVertically(
                            targetOffsetY = { 3000 },
                            animationSpec = tween(durationMillis = 500)
                        )
                    }
                ) {
                    FolderDetails(
                        folder = storedFolders[it.arguments?.getInt("data")!!],
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}