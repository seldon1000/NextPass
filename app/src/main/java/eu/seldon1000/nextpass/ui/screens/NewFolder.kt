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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.seldon1000.nextpass.MainViewModel
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.ui.items.DropdownFolderList
import eu.seldon1000.nextpass.ui.items.FavoriteButton
import eu.seldon1000.nextpass.ui.items.TextFieldItem
import eu.seldon1000.nextpass.ui.layout.Header
import eu.seldon1000.nextpass.ui.layout.MyScaffoldLayout
import eu.seldon1000.nextpass.ui.theme.NextcloudBlue
import eu.seldon1000.nextpass.ui.theme.colors

@ExperimentalMaterialApi
@Composable
fun NewFolder(viewModel: MainViewModel) {
    val context = LocalContext.current

    val scrollState = rememberScrollState()

    val storedFolders by viewModel.nextcloudApi.storedFolders.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()

    var favorite by remember { mutableStateOf(value = false) }

    var label by remember { mutableStateOf(value = "") }

    MyScaffoldLayout(fab = {
        FloatingActionButton(onClick = {
            if (label.isNotEmpty()) {
                viewModel.showDialog(
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

                    viewModel.executeRequest {
                        viewModel.nextcloudApi.createFolderRequest(params = params)
                        viewModel.setCurrentFolder(folder = selectedFolder)
                        viewModel.popBackStack()
                        viewModel.showSnackbar(message = context.getString(R.string.folder_created_snack))
                    }
                }

            } else viewModel.showDialog(
                title = context.getString(R.string.missing_info),
                body = {
                    Text(text = context.getString(R.string.missing_info_body), fontSize = 14.sp)
                }
            )
        }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_round_save_24),
                contentDescription = "save",
                tint = colors!!.onBackground
            )
        }
    }, bottomBar = {
        BottomAppBar(
            backgroundColor = Color.Black,
            cutoutShape = CircleShape,
            modifier = Modifier.clip(shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
        ) {
            IconButton(onClick = { viewModel.popBackStack() }) {
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
            Header(expanded = false, title = context.getString(R.string.new_folder))
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
                        DropdownFolderList(
                            canAdd = false,
                            folder = selectedFolder,
                            viewModel = viewModel
                        )
                        FavoriteButton(favorite = favorite) { favorite = !favorite }
                    }
                    TextFieldItem(
                        text = label,
                        onTextChanged = { label = it },
                        label = context.getString(R.string.label),
                        required = true,
                        capitalized = true
                    )
                }
            }
        }
    }
}