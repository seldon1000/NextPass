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

import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.seldon1000.nextpass.MainViewModel
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.api.Folder
import eu.seldon1000.nextpass.ui.items.CopyButton
import eu.seldon1000.nextpass.ui.items.DropdownFolderList
import eu.seldon1000.nextpass.ui.items.FavoriteButton
import eu.seldon1000.nextpass.ui.items.TextFieldItem
import eu.seldon1000.nextpass.ui.layout.Header
import eu.seldon1000.nextpass.ui.layout.MyScaffoldLayout
import eu.seldon1000.nextpass.ui.theme.NextcloudBlue
import eu.seldon1000.nextpass.ui.theme.colors

@Composable
@ExperimentalAnimationApi
@ExperimentalMaterialApi
fun FolderDetails(folder: Folder, viewModel: MainViewModel) {
    val context = LocalContext.current

    val scrollState = rememberScrollState()

    val storedFolders by viewModel.nextcloudApi.storedFolders.collectAsState()

    val currentFolder by viewModel.selectedFolder.collectAsState()

    var edit by remember { mutableStateOf(value = false) }

    var label by remember { mutableStateOf(value = folder.label) }

    MyScaffoldLayout(fab = {
        FloatingActionButton(onClick = {
            if (edit) {
                if (label.isNotEmpty()) {
                    viewModel.showDialog(
                        title = context.getString(R.string.update_folder),
                        body = {
                            Text(
                                text = context.getString(R.string.update_action_body),
                                fontSize = 14.sp
                            )
                        },
                        confirm = true
                    ) {
                        edit = false

                        val params = mutableMapOf(
                            "id" to folder.id,
                            "label" to label,
                            "parent" to storedFolders[currentFolder].id
                        )
                        if (folder.favorite) params["favorite"] = "true"

                        viewModel.executeRequest {
                            viewModel.nextcloudApi.updateFolderRequest(params = params)
                            viewModel.showSnackbar(message = context.getString(R.string.folder_updated_snack))
                        }
                    }
                } else viewModel.showDialog(
                    title = context.getString(R.string.missing_info),
                    body = {
                        Text(text = context.getString(R.string.missing_info_body), fontSize = 14.sp)
                    }
                )
            } else edit = true
        }) {
            Crossfade(targetState = edit) { state ->
                Icon(
                    painter = if (state) painterResource(id = R.drawable.ic_round_save_24)
                    else painterResource(id = R.drawable.ic_round_edit_24),
                    contentDescription = "save",
                    tint = colors!!.onBackground
                )
            }
        }
    }, bottomBar = {
        BottomAppBar(
            backgroundColor = Color.Black,
            cutoutShape = CircleShape,
            modifier = Modifier.clip(shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
        ) {
            IconButton(
                onClick = {
                    if (edit) {
                        edit = false

                        label = folder.label

                        viewModel.setSelectedFolder(folder = storedFolders.indexOfFirst { it.id == folder.parent })
                    } else viewModel.popBackStack()
                }
            ) {
                Crossfade(targetState = edit) { state ->
                    Icon(
                        painter = if (state) painterResource(id = R.drawable.ic_round_settings_backup_restore_24)
                        else painterResource(id = R.drawable.ic_round_back_arrow_24),
                        contentDescription = "restore"
                    )
                }
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
            Header(expanded = false, title = context.getString(R.string.folder_details))
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
                    Column {
                        Text(
                            text = "ID: ${folder.id}",
                            fontSize = 17.sp,
                            overflow = TextOverflow.Clip,
                            maxLines = 2,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                        Text(
                            text = context.getString(
                                R.string.date_info,
                                folder.createdDate,
                                folder.updatedDate
                            ),
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        ) {
                            DropdownFolderList(
                                enabled = edit,
                                folder = storedFolders.indexOfFirst { it.id == folder.parent },
                                viewModel = viewModel
                            )
                            FavoriteButton(favorite = folder.favorite) {
                                val params = mutableMapOf(
                                    "id" to folder.id,
                                    "label" to folder.label,
                                    "parent" to storedFolders[currentFolder].id
                                )
                                if (it) params["favorite"] = "true"

                                viewModel.executeRequest {
                                    viewModel.nextcloudApi.updateFolderRequest(params = params)
                                }
                            }
                        }
                    }
                    TextFieldItem(
                        text = label,
                        onTextChanged = { label = it },
                        label = context.getString(R.string.label),
                        enabled = edit,
                        required = true,
                        capitalized = true
                    ) {
                        CopyButton(
                            label = context.getString(R.string.folder_label),
                            clip = label,
                            viewModel = viewModel
                        )
                    }
                    TextFieldItem(
                        text = folder.parent,
                        onTextChanged = {},
                        label = context.getString(R.string.parent),
                        enabled = false
                    ) {
                        CopyButton(
                            label = context.getString(R.string.parent),
                            clip = folder.parent,
                            viewModel = viewModel
                        )
                    }
                    TextButton(onClick = {
                        viewModel.showDialog(
                            title = context.getString(R.string.delete_folder),
                            body = {
                                Text(
                                    text = context.getString(R.string.delete_folder_body),
                                    fontSize = 14.sp
                                )
                            },
                            confirm = true
                        ) {
                            viewModel.executeRequest {
                                viewModel.nextcloudApi.deleteFolderRequest(id = folder.id)
                                viewModel.popBackStack()
                                viewModel.showSnackbar(message = context.getString(R.string.folder_deleted_snack))
                                viewModel.nextcloudApi.refreshServerList()
                            }
                        }
                    }
                    ) { Text(text = context.getString(R.string.delete), color = Color.Red) }
                }
            }
        }
    }
}