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

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navArgument
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.api.NextcloudApiProvider
import eu.seldon1000.nextpass.ui.MainViewModel
import eu.seldon1000.nextpass.ui.screens.*

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@Composable
fun CentralScreenControl() {
    val context = LocalContext.current

    val navController = rememberNavController()
    MainViewModel.setNavController(controller = navController)

    val scaffoldState = rememberScaffoldState()
    MainViewModel.setSnackbarHostState(snackbar = scaffoldState.snackbarHostState)

    val storedPasswords by NextcloudApiProvider.storedPasswords.collectAsState()
    val storedFolders by NextcloudApiProvider.storedFolders.collectAsState()

    val currentScreen by MainViewModel.currentScreen.collectAsState()
    val refreshing by MainViewModel.refreshing.collectAsState()
    val refreshState = rememberSwipeRefreshState(isRefreshing = refreshing)

    MyAlertDialog()
    Scaffold(
        scaffoldState = scaffoldState,
        snackbarHost = { MySnackbar(snackbarHostState = scaffoldState.snackbarHostState) }) {
        SwipeRefresh(
            state = refreshState,
            onRefresh = { NextcloudApiProvider.refreshServerList() },
            swipeEnabled = currentScreen != "access_pin" &&
                    currentScreen != "welcome" &&
                    currentScreen != "settings" &&
                    currentScreen != "about" &&
                    currentScreen != "pin"
        ) {
            NavHost(navController = navController, startDestination = "welcome")
            {
                (context as Activity).window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

                composable(route = "access_pin") { AccessPin() }
                composable(route = "welcome") { WelcomeScreen() }
                composable(route = "search") { Search() }
                composable(route = "passwords") { PasswordList() }
                composable(route = "new_password") { NewPassword() }
                composable(route = "new_folder") { NewFolder() }
                composable(route = "favorites") { Favorites() }
                composable(route = "settings") { Settings() }
                composable(route = "about") { About() }
                composable(
                    route = "password_details/{password_data}",
                    listOf(navArgument(name = "password_data") { type = NavType.IntType })
                ) { navBackStackEntry ->
                    PasswordDetails(
                        passwordData = storedPasswords[navBackStackEntry.arguments?.getInt("password_data")!!]
                    )
                }
                composable(
                    route = "folder_details/{folder_data}",
                    listOf(navArgument(name = "folder_data") { type = NavType.IntType })
                ) { navBackStackEntry ->
                    FolderDetails(folder = storedFolders[navBackStackEntry.arguments?.getInt("folder_data")!!])
                }
                composable(
                    route = "pin/{change}",
                    listOf(navArgument(name = "change") { type = NavType.BoolType })
                ) { navBackStackEntry ->
                    ChangePin(change = navBackStackEntry.arguments?.getBoolean("change")!!)
                }
            }
        }
    }
}

@Composable
fun MyAlertDialog() {
    val context = LocalContext.current

    val openDialog by MainViewModel.openDialog.collectAsState()
    val dialogTitle by MainViewModel.dialogTitle.collectAsState()
    val dialogText by MainViewModel.dialogText.collectAsState()
    val dialogAction by MainViewModel.dialogAction.collectAsState()
    val dialogConfirm by MainViewModel.dialogConfirm.collectAsState()

    if (openDialog) {
        AlertDialog(
            onDismissRequest = { MainViewModel.dismissDialog() },
            confirmButton = {
                if (dialogConfirm)
                    Button(onClick =
                    {
                        dialogAction()
                        MainViewModel.dismissDialog()
                    }
                    ) { Text(text = context.getString(R.string.confirm)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { MainViewModel.dismissDialog() },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent)
                ) {
                    Text(text = context.getString(R.string.dismiss))
                }
            },
            title = {
                Text(
                    text = dialogTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = { Text(text = dialogText, fontSize = 14.sp) },
            shape = RoundedCornerShape(8.dp)
        )
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
            ) {
                Text(text = data.message, color = Color.White)
            }
        })
}