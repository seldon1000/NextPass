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

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.currentBackStackEntryAsState
import eu.seldon1000.nextpass.CentralAppControl
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.api.CustomField
import eu.seldon1000.nextpass.api.NextcloudApi
import eu.seldon1000.nextpass.api.Password
import eu.seldon1000.nextpass.api.SnapshotListSerializer
import eu.seldon1000.nextpass.ui.layout.Routes

@ExperimentalMaterialApi
@ExperimentalAnimationApi
@Composable
fun PasswordCard(index: Int, password: Password) {
    val context = LocalContext.current

    val storedFolders by NextcloudApi.storedFolders.collectAsState()

    val currentScreen by CentralAppControl.navController.collectAsState().value.currentBackStackEntryAsState()
    val folderMode by CentralAppControl.folderMode.collectAsState()
    val currentFolder by CentralAppControl.currentFolder.collectAsState()

    val favicon by password.favicon.collectAsState()

    var expanded by remember { mutableStateOf(value = false) }

    Card(
        onClick = { expanded = true },
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.width(width = 4.dp)) {
                password.tags.forEach {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(height = (64 / password.tags.size).dp)
                            .background(
                                color = Color(
                                    color = android.graphics.Color.parseColor(it.color)
                                ).copy(alpha = 0.8f)
                            )
                    )
                }
            }
            Box(
                modifier = Modifier.padding(
                    start = 6.dp,
                    top = 10.dp,
                    end = 10.dp,
                    bottom = 10.dp
                )
            ) { Favicon(favicon = favicon, size = 44.dp) }
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(weight = 1f)
            ) {
                Text(
                    text = password.label,
                    fontWeight = FontWeight.SemiBold,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
                Text(
                    text = password.username,
                    fontSize = 13.sp,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    color = Color.Gray
                )
            }
            if (!folderMode && password.folder != storedFolders[0].id)
                Icon(
                    painter = painterResource(id = R.drawable.ic_round_drive_file_move_24),
                    contentDescription = "folder",
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .shadow(elevation = 8.dp, RoundedCornerShape(size = 8.dp), clip = true)
                )
            Status(password = password)
            FavoriteButton(favorite = password.favorite) {
                val params = mutableMapOf(
                    "id" to password.id,
                    "label" to password.label,
                    "username" to password.username,
                    "password" to password.password,
                    "url" to password.url,
                    "notes" to password.notes,
                    "customFields" to NextcloudApi.json.encodeToString(
                        serializer = SnapshotListSerializer(
                            dataSerializer = CustomField.serializer()
                        ), value = password.customFieldsList
                    ),
                    "folder" to password.folder,
                    "hash" to password.hash
                )
                if (it) params["favorite"] = "true"

                CentralAppControl.executeRequest {
                    NextcloudApi.updatePasswordRequest(params = params, tags = password.tags)
                }
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(x = 150.dp, y = 0.dp),
            modifier = Modifier.width(width = 200.dp)
        ) {
            DropdownMenuItem(onClick = {
                expanded = false

                CentralAppControl.setPrimaryClip(
                    label = context.getString(R.string.username),
                    clip = password.username
                )
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_round_person_24),
                    contentDescription = "copy_username"
                )
                Text(
                    text = context.getString(R.string.copy_username),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            DropdownMenuItem(onClick = {
                expanded = false

                CentralAppControl.setPrimaryClip(
                    label = context.getString(R.string.password),
                    clip = password.password
                )
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_round_password_24),
                    contentDescription = "copy_password"
                )
                Text(
                    text = context.getString(R.string.copy_password),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            DropdownMenuItem(onClick = {
                expanded = false

                CentralAppControl.navigate(route = Routes.PasswordDetails.getRoute(arg = index))
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_round_info_24),
                    contentDescription = "password_details"
                )
                Text(
                    text = context.getString(R.string.details),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            if (password.folder != storedFolders[currentFolder].id ||
                currentScreen?.destination?.route == Routes.Search.route ||
                currentScreen?.destination?.route == Routes.Favorites.route
            )
                DropdownMenuItem(onClick = {
                    expanded = false

                    CentralAppControl.setFolderMode(mode = true)
                    CentralAppControl.setCurrentFolder(folder = storedFolders.indexOfFirst { password.folder == it.id })
                    CentralAppControl.navigate(route = Routes.Passwords.route)
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_round_drive_file_move_24),
                        contentDescription = "go_to_folder"
                    )
                    Text(
                        text = context.getString(R.string.go_to_folder),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            DropdownMenuItem(onClick = {
                expanded = false

                CentralAppControl.showDialog(
                    title = context.getString(R.string.delete_password),
                    body = {
                        Text(
                            text = context.getString(R.string.delete_password_body),
                            fontSize = 14.sp
                        )
                    },
                    confirm = true
                ) {
                    CentralAppControl.executeRequest {
                        NextcloudApi.deletePasswordRequest(id = password.id)
                        CentralAppControl.showSnackbar(message = context.getString(R.string.password_deleted))
                    }
                }
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_round_delete_forever_24),
                    contentDescription = "delete_password",
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

@Composable
fun Status(password: Password) {
    val painter = when (password.status) {
        0 -> painterResource(id = R.drawable.ic_security_good_24)
        2 -> painterResource(id = R.drawable.ic_security_bad_24)
        else -> painterResource(id = R.drawable.ic_security_weak_24)
    }

    Icon(
        painter = painter,
        contentDescription = "security_status",
        tint = if (password.status == 0) Color.Green else if (password.status == 1) Color.Yellow else Color.Red,
        modifier = Modifier
            .padding(start = 8.dp)
            .shadow(elevation = 8.dp, RoundedCornerShape(size = 8.dp), clip = true)
    )
}