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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.api.NextcloudApiProvider
import eu.seldon1000.nextpass.ui.MainViewModel
import eu.seldon1000.nextpass.ui.items.CountMessage
import eu.seldon1000.nextpass.ui.items.FolderCard
import eu.seldon1000.nextpass.ui.items.PasswordCard
import eu.seldon1000.nextpass.ui.items.TagsRow
import eu.seldon1000.nextpass.ui.layout.Header
import eu.seldon1000.nextpass.ui.layout.MyScaffoldLayout
import eu.seldon1000.nextpass.ui.theme.colors

@ExperimentalFoundationApi
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@Composable
fun Search() {
    val context = LocalContext.current

    val storedPasswords by NextcloudApiProvider.storedPasswords.collectAsState()
    val storedFolders by NextcloudApiProvider.storedFolders.collectAsState()

    var searchedText by remember { mutableStateOf(value = "") }

    var currentTag by remember { mutableStateOf(value = "") }

    val showedPasswords = storedPasswords.filter { password ->
        (if (currentTag.isNotEmpty()) password.tags.any { it.containsValue(currentTag) }
        else true) && (password.url.contains(searchedText, ignoreCase = true) ||
                password.label.contains(searchedText, ignoreCase = true) ||
                password.username.contains(searchedText, ignoreCase = true) ||
                password.notes.contains(searchedText, ignoreCase = true))
    }
    val showedFolders = storedFolders.filterIndexed { index, folder ->
        if (index > 0) folder.label.contains(searchedText, ignoreCase = true)
        else false
    }

    MyScaffoldLayout(fab = {
        FloatingActionButton({ searchedText = "" }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_round_search_off_24),
                contentDescription = "restore_search",
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
            item { Header(expanded = true, title = context.getString(R.string.search)) {} }
            item { TagsRow { currentTag = if (it == currentTag) "" else it } }
            if (searchedText.isNotEmpty()) {
                items(items = showedFolders) { folder -> FolderCard(folder = folder) }
                items(items = showedPasswords) { password -> PasswordCard(password = password) }
            }
            item {
                CountMessage(
                    message = if (searchedText.isNotEmpty()) context.getString(
                        R.string.results_number, showedFolders.size + showedPasswords.size
                    ) else context.getString(R.string.search_message)
                )
            }
        }
    }
}