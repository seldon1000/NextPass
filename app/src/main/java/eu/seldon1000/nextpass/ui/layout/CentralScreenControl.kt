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
            swipeEnabled = currentScreen != "welcome" &&
                    currentScreen != "settings" &&
                    currentScreen != "about"
        ) {
            NavHost(navController = MainViewModel.getNavController(), startDestination = "welcome")
            {
                (context as Activity).window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

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
                    val index = navBackStackEntry.arguments?.getInt("password_data")!!

                    PasswordDetails(index = index, passwordData = storedPasswords[index])
                }
                composable(
                    route = "folder_details/{folder_data}",
                    listOf(navArgument(name = "folder_data") { type = NavType.IntType })
                ) { navBackStackEntry ->
                    val index = navBackStackEntry.arguments?.getInt("folder_data")!!

                    FolderDetails(folder = storedFolders[index])
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

    if (openDialog) {
        AlertDialog(
            onDismissRequest = { MainViewModel.dismissDialog() },
            confirmButton = {
                if (dialogTitle != "Missing info!" && dialogTitle != "Something went wrong!")
                    Button(onClick =
                    {
                        dialogAction()
                        MainViewModel.dismissDialog()
                    }
                    ) {
                        Text(text = context.getString(R.string.confirm))
                    }
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