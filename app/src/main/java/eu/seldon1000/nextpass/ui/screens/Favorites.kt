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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.api.NextcloudApiProvider
import eu.seldon1000.nextpass.api.Tag
import eu.seldon1000.nextpass.ui.MainViewModel
import eu.seldon1000.nextpass.ui.items.CountMessage
import eu.seldon1000.nextpass.ui.items.FolderCard
import eu.seldon1000.nextpass.ui.items.PasswordCard
import eu.seldon1000.nextpass.ui.items.TagsRow
import eu.seldon1000.nextpass.ui.layout.DefaultBottomBar
import eu.seldon1000.nextpass.ui.layout.DefaultFab
import eu.seldon1000.nextpass.ui.layout.Header
import eu.seldon1000.nextpass.ui.layout.MyScaffoldLayout

@ExperimentalFoundationApi
@ExperimentalMaterialApi
@ExperimentalAnimationApi
@Composable
fun Favorites() {
    val context = LocalContext.current

    val lazyListState = rememberLazyListState()

    val storedFolders by NextcloudApiProvider.storedFolders.collectAsState()
    val storedPasswords by NextcloudApiProvider.storedPasswords.collectAsState()

    val tags by MainViewModel.tags.collectAsState()

    var currentTag: Tag? by remember { mutableStateOf(value = null) }

    MyScaffoldLayout(
        fab = { DefaultFab() },
        bottomBar = { DefaultBottomBar(lazyListState = lazyListState) }) { paddingValues ->
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = paddingValues.calculateBottomPadding() + 52.dp
            ),
            state = lazyListState,
            modifier = Modifier.fillMaxSize()
        ) {
            item { Header(expanded = true, title = context.getString(R.string.favorites)) }
            if (tags) item { TagsRow { currentTag = if (it == currentTag) null else it } }
            else item { Box(modifier = Modifier.height(12.dp)) }
            itemsIndexed(items = storedFolders) { index, folder ->
                if (folder.favorite) FolderCard(index = index, folder = folder)
            }
            itemsIndexed(items = storedPasswords) { index, password ->
                if (if (currentTag != null) password.tags.any { it == currentTag } else password.favorite)
                    PasswordCard(index = index, password = password)
            }
            item {
                val count = storedPasswords.count { it.favorite }

                CountMessage(
                    message = context.resources.getQuantityString(
                        R.plurals.favorites_number,
                        count,
                        count
                    )
                )
            }
        }
    }
}