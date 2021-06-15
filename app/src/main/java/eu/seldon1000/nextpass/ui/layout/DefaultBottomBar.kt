package eu.seldon1000.nextpass.ui.layout

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.BottomAppBar
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.ui.MainViewModel
import eu.seldon1000.nextpass.ui.theme.NextcloudBlue

@Composable
fun DefaultBottomBar() {
    val selected by MainViewModel.currentScreen.collectAsState()

    BottomAppBar(backgroundColor = Color.Black, cutoutShape = CircleShape) {
        BottomNavigationItem(
            selected = selected == "search",
            onClick = { if (selected != "search") MainViewModel.navigate(route = "search") },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_round_search_24),
                    contentDescription = "search"
                )
            },
            selectedContentColor = NextcloudBlue,
            unselectedContentColor = Color.White
        )
        BottomNavigationItem(
            selected = selected == "passwords",
            onClick = {
                if (selected != "passwords") MainViewModel.navigate(route = "passwords")
                else MainViewModel.setCurrentFolder(folder = 0)
            },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_round_list_alt_24),
                    contentDescription = "home"
                )
            },
            selectedContentColor = NextcloudBlue,
            unselectedContentColor = Color.White
        )
        BottomNavigationItem(
            selected = selected == "favorites",
            onClick = { if (selected != "favorites") MainViewModel.navigate(route = "favorites") },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_round_star_yellow_24),
                    contentDescription = "favorites"
                )
            },
            selectedContentColor = NextcloudBlue,
            unselectedContentColor = Color.White
        )
        BottomNavigationItem(
            selected = selected == "settings",
            onClick = { if (selected != "settings") MainViewModel.navigate(route = "settings") },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_round_settings_24),
                    contentDescription = "settings"
                )
            },
            selectedContentColor = NextcloudBlue,
            unselectedContentColor = Color.White
        )
        BottomNavigationItem(selected = false, onClick = {}, enabled = false, icon = {})
    }
}