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

import android.content.ClipboardManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.api.Folder
import eu.seldon1000.nextpass.api.NextcloudApi
import eu.seldon1000.nextpass.CentralAppControl
import eu.seldon1000.nextpass.ui.layout.Routes
import eu.seldon1000.nextpass.ui.theme.NextcloudBlue

@ExperimentalMaterialApi
@Composable
fun FolderCard(index: Int, folder: Folder, icon: Painter? = null) {
    val context = LocalContext.current

    val storedFolders by NextcloudApi.storedFolders.collectAsState()

    val currentFolder by CentralAppControl.currentFolder.collectAsState()

    var expanded by remember { mutableStateOf(value = false) }

    Card(
        onClick = {
            if (icon == null) expanded = true
            else CentralAppControl.setCurrentFolder()
        },
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.padding(all = 8.dp)) {
                Icon(
                    painter = icon ?: painterResource(id = R.drawable.ic_round_folder_24),
                    contentDescription = "folder_icon",
                    tint = if (icon == null) NextcloudBlue else Color.White,
                    modifier = if (icon == null) Modifier.size(size = 44.dp) else Modifier
                )
            }
            Text(
                text = folder.label,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                modifier = Modifier.weight(weight = 1f)
            )
            FavoriteButton(favorite = folder.favorite) {
                val params = mutableMapOf(
                    "id" to folder.id,
                    "label" to folder.label,
                    "parent" to storedFolders[currentFolder].id
                )
                if (it) params["favorite"] = "true"

                CentralAppControl.executeRequest { NextcloudApi.updateFolderRequest(params = params) }
            }
        }
        if (icon == null) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                offset = DpOffset(x = 150.dp, y = 0.dp),
                modifier = Modifier.width(width = 200.dp)
            ) {
                DropdownMenuItem({
                    CentralAppControl.setFolderMode(mode = true)
                    CentralAppControl.setCurrentFolder(folder = index)
                    CentralAppControl.navigate(route = Routes.Passwords.route)

                    expanded = false
                }) {
                    Row {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_round_folder_24),
                            contentDescription = "open_folder"
                        )
                        Text(
                            text = context.getString(R.string.open_folder),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
                DropdownMenuItem({
                    CentralAppControl.setPrimaryClip(
                        manager = context.getSystemService(ClipboardManager::class.java),
                        label = context.getString(R.string.folder_label),
                        clip = folder.label
                    )

                    expanded = false
                }) {
                    Row {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_round_label_24),
                            contentDescription = "copy_folder_label"
                        )
                        Text(
                            text = context.getString(R.string.copy_folder_label),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
                DropdownMenuItem({
                    CentralAppControl.setCurrentFolder(folder = storedFolders.indexOfFirst { it.id == folder.parent })
                    CentralAppControl.navigate(route = Routes.FolderDetails.getRoute(arg = index))

                    expanded = false
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_round_info_24),
                        contentDescription = "folder_details"
                    )
                    Text(
                        text = context.getString(R.string.details),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
                DropdownMenuItem({
                    CentralAppControl.showDialog(
                        title = context.getString(R.string.delete_folder),
                        body = {
                            Text(
                                text = context.getString(R.string.delete_folder_body),
                                fontSize = 14.sp
                            )
                        },
                        confirm = true
                    ) {
                        CentralAppControl.executeRequest {
                            NextcloudApi.deleteFolderRequest(id = folder.id)
                            CentralAppControl.showSnackbar(message = context.getString(R.string.folder_deleted_snack))
                        }
                    }

                    expanded = false
                }) {
                    Row {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_round_delete_forever_24),
                            contentDescription = "delete_folder",
                            tint = Color.Red
                        )
                        Text(
                            text = context.getString(R.string.delete),
                            color = Color.Red,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        }
    }
}