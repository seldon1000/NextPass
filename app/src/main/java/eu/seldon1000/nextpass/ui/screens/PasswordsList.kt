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

import android.annotation.SuppressLint
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import eu.seldon1000.nextpass.MainViewModel
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.api.Tag
import eu.seldon1000.nextpass.ui.items.CountMessage
import eu.seldon1000.nextpass.ui.items.FolderCard
import eu.seldon1000.nextpass.ui.items.PasswordCard
import eu.seldon1000.nextpass.ui.items.TagsRow
import eu.seldon1000.nextpass.ui.layout.*

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@ExperimentalFoundationApi
@SuppressLint("UnusedCrossfadeTargetStateParameter")
@Composable
fun PasswordList(viewModel: MainViewModel) {
    val context = LocalContext.current

    val lazyListState = rememberLazyListState()

    val folderMode by viewModel.folderMode.collectAsState()
    val currentFolder by viewModel.currentFolder.collectAsState()

    val storedPasswords by viewModel.nextcloudApi.storedPasswords.collectAsState()
    val storedFolders by viewModel.nextcloudApi.storedFolders.collectAsState()

    val tags by viewModel.tags.collectAsState()

    var currentTag: Tag? by remember { mutableStateOf(value = null) }

    MyScaffoldLayout(
        fab = { DefaultFab(viewModel = viewModel) },
        bottomBar = {
            DefaultBottomBar(
                lazyListState = lazyListState,
                viewModel = viewModel
            )
        }) { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = paddingValues.calculateBottomPadding() + 52.dp
            ),
            state = lazyListState,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Header(expanded = true, title = context.getString(R.string.passwords)) {
                        Row {
                            if (folderMode) {
                                Crossfade(targetState = folderMode) {
                                    IconButton(
                                        onClick = { viewModel.setCurrentFolder(folder = 0) },
                                        enabled = currentFolder != 0
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_round_home_24),
                                            contentDescription = "base_folder"
                                        )
                                    }
                                }
                                Crossfade(targetState = folderMode) {
                                    IconButton(onClick = { viewModel.navigate(route = Routes.NewFolder.route) }) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_round_create_new_folder_24),
                                            contentDescription = "new_folder"
                                        )
                                    }
                                }
                            }
                            Card(
                                shape = CircleShape,
                                elevation = if (folderMode) 8.dp else 0.dp
                            ) {
                                IconButton(onClick = {
                                    if (folderMode) viewModel.setCurrentFolder(folder = 0)
                                    viewModel.setFolderMode()
                                }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_round_folder_24),
                                        contentDescription = "folder_mode"
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (tags) item {
                TagsRow(tagClickAction = {
                    currentTag = if (it == currentTag) null else it
                }, viewModel = viewModel)
            }
            else item { Box(modifier = Modifier.height(height = 12.dp)) }
            if (currentFolder != 0 && folderMode) item {
                FolderCard(
                    currentFolder,
                    folder = storedFolders[currentFolder],
                    icon = painterResource(id = R.drawable.ic_round_back_arrow_24),
                    viewModel = viewModel
                )
            }
            if (folderMode && storedFolders.size > 1) itemsIndexed(items = storedFolders) { index, folder ->
                if (index > 0 && index != currentFolder && folder.parent == storedFolders[currentFolder].id)
                    FolderCard(index = index, folder = folder, viewModel = viewModel)
            }
            itemsIndexed(items = storedPasswords) { index, password ->
                if (if (currentTag != null) password.tags.contains(element = currentTag)
                    else !folderMode || password.folder == storedFolders[currentFolder].id
                ) PasswordCard(index = index, password = password, viewModel = viewModel)
            }
            item {
                CountMessage(
                    message = context.resources.getQuantityString(
                        R.plurals.passwords_number,
                        storedPasswords.size,
                        storedPasswords.size
                    ),
                    viewModel = viewModel
                )
            }
        }
    }
}