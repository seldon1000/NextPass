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

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.api.Folder
import eu.seldon1000.nextpass.api.NextcloudApiProvider
import eu.seldon1000.nextpass.ui.MainViewModel
import eu.seldon1000.nextpass.ui.items.CopyButton
import eu.seldon1000.nextpass.ui.items.DropdownFolderList
import eu.seldon1000.nextpass.ui.items.FavoriteIcon
import eu.seldon1000.nextpass.ui.items.TextFieldItem
import eu.seldon1000.nextpass.ui.layout.Header
import eu.seldon1000.nextpass.ui.layout.MyScaffoldLayout
import eu.seldon1000.nextpass.ui.theme.NextcloudBlue

@Composable
@ExperimentalAnimationApi
@ExperimentalMaterialApi
fun FolderDetails(folder: Folder) { /*TODO: allow proper folder edit, once SSO support PATCH method */
    val context = LocalContext.current

    val scrollState = rememberScrollState()

    val label by remember { mutableStateOf(value = folder.label) }

    MyScaffoldLayout(fab = {}, bottomBar = {
        BottomAppBar(cutoutShape = CircleShape) {
            IconButton(
                onClick = { MainViewModel.popBackStack() },
            ) {
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
            Header(expanded = false, title = context.getString(R.string.folder_details)) {}
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
                elevation = 6.dp,
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
                                folder.created,
                                folder.edited
                            ),
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        ) {
                            DropdownFolderList(
                                enabled = false,
                                folder = MainViewModel.currentFolder.value
                            )
                            FavoriteIcon(favorite = folder.favorite) {} /*TODO: add updateFolderRequest, needs PATCH support from SSO*/
                        }
                    }
                    TextFieldItem(
                        text = label,
                        onTextChanged = {},
                        label = context.getString(R.string.label),
                        enabled = false,
                        required = true,
                        capitalized = true
                    ) {
                        CopyButton(
                            label = context.getString(R.string.folder_label),
                            clip = label
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
                            clip = folder.parent
                        )
                    }
                    TextButton(onClick = {
                        MainViewModel.showDialog(
                            title = context.getString(R.string.delete_folder),
                            body = context.getString(R.string.delete_folder_body),
                            confirm = true
                        ) {
                            NextcloudApiProvider.deleteFolderRequest(index = folder.index)
                            MainViewModel.popBackStack()
                            MainViewModel.showSnackbar(message = context.getString(R.string.folder_deleted_snack))
                        }
                    }
                    ) {
                        Text(
                            text = context.getString(R.string.delete),
                            color = Color.Red
                        )
                    }
                }
            }
        }
    }
}