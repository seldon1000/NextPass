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

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomAppBar
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.ui.MainViewModel
import eu.seldon1000.nextpass.ui.theme.NextcloudBlue
import eu.seldon1000.nextpass.ui.theme.colors
import kotlinx.coroutines.launch

@Composable
fun DefaultBottomBar(lazyListState: LazyListState) {
    val coroutineScope = rememberCoroutineScope()

    val currentScreen by MainViewModel.navController.collectAsState().value!!.currentBackStackEntryAsState()

    BottomAppBar(
        backgroundColor = Color.Black,
        cutoutShape = CircleShape,
        modifier = Modifier.clip(shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        BottomNavigationItem(
            selected = currentScreen?.destination?.route == Routes.Favorites.route,
            onClick = {
                if (currentScreen?.destination?.route != Routes.Favorites.route)
                    MainViewModel.navigate(route = Routes.Favorites.route)
                else coroutineScope.launch { lazyListState.scrollToItem(index = 0) }
            },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_round_star_24),
                    contentDescription = "favorites"
                )
            },
            selectedContentColor = NextcloudBlue,
            unselectedContentColor = colors!!.onBackground
        )
        BottomNavigationItem(
            selected = currentScreen?.destination?.route == Routes.Passwords.route,
            onClick = {
                if (currentScreen?.destination?.route != Routes.Passwords.route)
                    MainViewModel.navigate(route = Routes.Passwords.route)
                else {
                    MainViewModel.setCurrentFolder(folder = 0)
                    coroutineScope.launch { lazyListState.scrollToItem(index = 0) }
                }
            },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_round_list_alt_24),
                    contentDescription = "home"
                )
            },
            selectedContentColor = NextcloudBlue,
            unselectedContentColor = colors!!.onBackground
        )
        BottomNavigationItem(selected = false, onClick = {}, enabled = false, icon = {})
        BottomNavigationItem(
            selected = currentScreen?.destination?.route == Routes.Search.route,
            onClick = {
                if (currentScreen?.destination?.route != Routes.Search.route)
                    MainViewModel.navigate(route = Routes.Search.route)
            },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_round_search_24),
                    contentDescription = "search"
                )
            },
            selectedContentColor = NextcloudBlue,
            unselectedContentColor = colors!!.onBackground
        )
        BottomNavigationItem(
            selected = currentScreen?.destination?.route == Routes.Settings.route,
            onClick = {
                if (currentScreen?.destination?.route != Routes.Settings.route)
                    MainViewModel.navigate(route = Routes.Settings.route)
                else coroutineScope.launch { lazyListState.scrollToItem(0) }
            },
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_round_settings_24),
                    contentDescription = "settings"
                )
            },
            selectedContentColor = NextcloudBlue,
            unselectedContentColor = colors!!.onBackground
        )
    }
}