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

package eu.seldon1000.nextpass.ui.items

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.api.NextcloudApiProvider
import eu.seldon1000.nextpass.ui.MainViewModel

@ExperimentalMaterialApi
@Composable
fun DropdownFolderList(enabled: Boolean = true, canAdd: Boolean = true, folder: Int) {
    val context = LocalContext.current

    val storedFolders by NextcloudApiProvider.storedFolders.collectAsState()

    val selectedFolder by MainViewModel.selectedFolder.collectAsState()

    var expanded by remember { mutableStateOf(value = false) }
    var folderChanged by remember { mutableStateOf(value = false) }

    Card(
        onClick = { expanded = true },
        shape = RoundedCornerShape(size = 8.dp),
        elevation = if (enabled) 8.dp else 0.dp,
        backgroundColor = if (enabled) Color.DarkGray else Color.Unspecified,
        enabled = enabled
    ) {
        Row(modifier = Modifier.padding(all = 8.dp)) {
            Icon(
                painter = painterResource(id = R.drawable.ic_round_folder_24),
                contentDescription = "folder"
            )
            Text(
                text = storedFolders[if (folderChanged) selectedFolder else folder].label,
                modifier = Modifier.padding(start = 8.dp, end = 16.dp)
            )
            Icon(
                painter = painterResource(
                    id = if (expanded) R.drawable.ic_round_arrow_drop_up_24
                    else R.drawable.ic_round_arrow_drop_down_24
                ),
                contentDescription = "expand_folder_list"
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            storedFolders.forEachIndexed { index, folder ->
                DropdownMenuItem(onClick = {
                    expanded = false
                    folderChanged = true
                    MainViewModel.setSelectedFolder(folder = index)
                }) {
                    Text(text = folder.label)
                }
            }
            if (canAdd) DropdownMenuItem(onClick = {
                expanded = false
                MainViewModel.navigate(route = "new_folder")
            }) {
                Text(text = context.getString(R.string.add_new_folder))
            }
        }
    }
}