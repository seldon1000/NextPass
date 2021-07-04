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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.api.NextcloudApiProvider
import eu.seldon1000.nextpass.ui.MainViewModel
import eu.seldon1000.nextpass.ui.items.CountMessage
import eu.seldon1000.nextpass.ui.items.FolderCard
import eu.seldon1000.nextpass.ui.items.PasswordCard
import eu.seldon1000.nextpass.ui.layout.DefaultBottomBar
import eu.seldon1000.nextpass.ui.layout.DefaultFab
import eu.seldon1000.nextpass.ui.layout.Header
import eu.seldon1000.nextpass.ui.layout.MyScaffoldLayout

@SuppressLint("UnusedCrossfadeTargetStateParameter")
@ExperimentalAnimationApi
@ExperimentalMaterialApi
@Composable
fun PasswordList() {
    val context = LocalContext.current

    val lazyListState = rememberLazyListState()

    val folderMode by MainViewModel.folderMode.collectAsState()
    val currentFolder by MainViewModel.currentFolder.collectAsState()

    val storedPasswords by NextcloudApiProvider.storedPasswords.collectAsState()
    val storedFolders by NextcloudApiProvider.storedFolders.collectAsState()

    MyScaffoldLayout(
        fab = { DefaultFab() },
        bottomBar = { DefaultBottomBar(lazyListState = lazyListState) }) { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = paddingValues.calculateBottomPadding() + 28.dp
            ),
            state = lazyListState,
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
                                        onClick = { MainViewModel.setCurrentFolder(folder = 0) },
                                        enabled = currentFolder != 0
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_round_home_24),
                                            contentDescription = "base_folder"
                                        )
                                    }
                                }
                                Crossfade(targetState = folderMode) {
                                    IconButton(onClick = { MainViewModel.navigate(route = "new_folder") }) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_round_create_new_folder_24),
                                            contentDescription = "new_folder"
                                        )
                                    }
                                }
                            }
                            Crossfade(targetState = folderMode) { state ->
                                Card(shape = CircleShape, elevation = if (state) 8.dp else 0.dp) {
                                    IconButton(onClick = {
                                        if (folderMode)
                                            MainViewModel.setCurrentFolder(folder = 0)
                                        MainViewModel.setFolderMode()
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
            }
            if (currentFolder != 0 && folderMode) item {
                FolderCard(
                    folder = storedFolders[currentFolder],
                    icon = painterResource(id = R.drawable.ic_round_back_arrow_24)
                )
            }
            if (folderMode && storedFolders.size > 1) items(items = storedFolders) { folder ->
                if (folder.parent == storedFolders[currentFolder].id)
                    FolderCard(folder = folder)
            }
            items(storedPasswords) { password ->
                if (!folderMode || password.folder == storedFolders[currentFolder].id)
                    PasswordCard(password = password)
            }
            item {
                CountMessage(
                    message = context.getString(
                        R.string.passwords_number,
                        storedPasswords.size
                    )
                )
            }
        }
    }
}