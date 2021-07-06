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

package eu.seldon1000.nextpass.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.api.NextcloudApiProvider
import eu.seldon1000.nextpass.ui.MainViewModel
import eu.seldon1000.nextpass.ui.items.DropdownFolderList
import eu.seldon1000.nextpass.ui.items.TextFieldItem
import eu.seldon1000.nextpass.ui.layout.Header
import eu.seldon1000.nextpass.ui.layout.MyScaffoldLayout
import eu.seldon1000.nextpass.ui.theme.NextcloudBlue
import eu.seldon1000.nextpass.ui.theme.colors

@ExperimentalMaterialApi
@Composable
fun NewFolder() {
    val context = LocalContext.current

    val scrollState = rememberScrollState()

    val storedFolders by NextcloudApiProvider.storedFolders.collectAsState()
    val selectedFolder by MainViewModel.selectedFolder.collectAsState()

    var favorite by remember { mutableStateOf(value = false) }

    var label by remember { mutableStateOf(value = "") }

    MyScaffoldLayout(fab = {
        FloatingActionButton(onClick = {
            if (label.isNotEmpty()) {
                MainViewModel.showDialog(
                    title = context.getString(R.string.create_folder),
                    body = {
                        Text(
                            text = context.getString(R.string.create_folder_body),
                            fontSize = 14.sp
                        )
                    },
                    confirm = true
                ) {
                    val params = mutableMapOf(
                        "label" to label,
                        "parent" to storedFolders[selectedFolder].id
                    )
                    if (favorite) params["favorite"] = "true"

                    MainViewModel.setRefreshing(refreshing = true)
                    NextcloudApiProvider.createFolderRequest(params = params)
                    MainViewModel.popBackStack()
                    MainViewModel.showSnackbar(message = context.getString(R.string.new_password_snack))
                }

            } else MainViewModel.showDialog(
                title = context.getString(R.string.missing_info),
                body = {
                    Text(text = context.getString(R.string.missing_info_body), fontSize = 14.sp)
                }
            ) {}
        }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_round_save_24),
                contentDescription = "save",
                tint = colors!!.onBackground
            )
        }
    }, bottomBar = {
        BottomAppBar(cutoutShape = CircleShape) {
            IconButton(onClick = { MainViewModel.popBackStack() }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_round_back_arrow_24),
                    contentDescription = "back"
                )
            }
        }
    }) { paddingValues ->
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = scrollState, enabled = true)
        ) {
            Header(expanded = false, title = context.getString(R.string.new_folder)) {}
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(all = 72.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_round_folder_24),
                    contentDescription = "folder_big_icon",
                    tint = NextcloudBlue,
                    modifier = Modifier.size(size = 144.dp)
                )
            }
            Card(
                elevation = 8.dp,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = paddingValues.calculateBottomPadding() + 48.dp
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        DropdownFolderList(canAdd = false, folder = selectedFolder)
                        IconButton({
                            favorite = !favorite
                        }) { /*TODO: replace with standard FavoriteIcon composable, once SSO supports PATCH method*/
                            Icon(
                                painter = if (favorite)
                                    painterResource(id = R.drawable.ic_round_star_24)
                                else
                                    painterResource(id = R.drawable.ic_round_star_border_24),
                                contentDescription = "favorite",
                                tint = if (favorite) Color.Yellow else Color.White
                            )
                        }
                    }
                    TextFieldItem(
                        text = label,
                        onTextChanged = { label = it },
                        label = context.getString(R.string.label),
                        required = true,
                        capitalized = true
                    ) {}
                }
            }
        }
    }
}