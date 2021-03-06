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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import eu.seldon1000.nextpass.ui.layout.Header
import eu.seldon1000.nextpass.ui.layout.MyScaffoldLayout

@ExperimentalFoundationApi
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@Composable
fun Search(viewModel: MainViewModel) {
    val context = LocalContext.current

    val storedPasswords by viewModel.nextcloudApi.storedPasswords.collectAsState()
    val storedFolders by viewModel.nextcloudApi.storedFolders.collectAsState()

    val tags by viewModel.tags.collectAsState()

    var searchedText by remember { mutableStateOf(value = "") }

    var currentTag: Tag? by remember { mutableStateOf(value = null) }

    MyScaffoldLayout(fab = {}, bottomBar = {
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
            TextField(
                value = searchedText,
                onValueChange = { searchedText = it },
                label = { Text(text = context.getString(R.string.search_hint)) },
                singleLine = true,
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )
        }
    }) { paddingValues ->
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = paddingValues.calculateBottomPadding() + 28.dp
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            item { Header(expanded = true, title = context.getString(R.string.search)) }
            if (tags) item {
                TagsRow(
                    tagClickAction = { currentTag = if (it == currentTag) null else it },
                    viewModel = viewModel
                )
            }
            else item { Box(modifier = Modifier.height(height = 12.dp)) }
            if (searchedText.isNotEmpty()) {
                itemsIndexed(items = storedFolders) { index, folder ->
                    if (if (index > 0) folder.label.contains(searchedText, ignoreCase = true)
                        else false
                    ) FolderCard(index = index, folder = folder, viewModel = viewModel)
                }
                itemsIndexed(items = storedPasswords) { index, password ->
                    if ((if (currentTag != null) password.tags.any { it == currentTag }
                        else true) && (password.url.contains(searchedText, ignoreCase = true) ||
                                password.label.contains(searchedText, ignoreCase = true) ||
                                password.username.contains(searchedText, ignoreCase = true) ||
                                password.notes.contains(searchedText, ignoreCase = true)))
                        PasswordCard(index = index, password = password, viewModel = viewModel)
                }
            }
            item {
                val count = storedPasswords.count { password ->
                    (if (currentTag != null) password.tags.any { it == currentTag }
                    else true) && (password.url.contains(searchedText, ignoreCase = true) ||
                            password.label.contains(searchedText, ignoreCase = true) ||
                            password.username.contains(searchedText, ignoreCase = true) ||
                            password.notes.contains(searchedText, ignoreCase = true))
                } + storedFolders.count {
                    it.label.contains(
                        searchedText,
                        ignoreCase = true
                    )
                }

                CountMessage(
                    message = if (searchedText.isNotEmpty())
                        context.resources.getQuantityString(R.plurals.results_number, count, count)
                    else context.getString(R.string.search_message),
                    viewModel = viewModel
                )
            }
        }
    }
}