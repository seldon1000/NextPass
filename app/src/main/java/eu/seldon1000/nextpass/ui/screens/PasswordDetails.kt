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

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonParser
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.api.NextcloudApiProvider
import eu.seldon1000.nextpass.api.Password
import eu.seldon1000.nextpass.ui.MainViewModel
import eu.seldon1000.nextpass.ui.items.CopyButton
import eu.seldon1000.nextpass.ui.items.DropdownFolderList
import eu.seldon1000.nextpass.ui.items.Favicon
import eu.seldon1000.nextpass.ui.items.FavoriteIcon
import eu.seldon1000.nextpass.ui.layout.Header
import eu.seldon1000.nextpass.ui.layout.MyScaffoldLayout
import kotlinx.coroutines.launch

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@Composable
fun PasswordDetails(passwordData: Password) {
    val context = LocalContext.current

    val storedFolders by NextcloudApiProvider.storedFolders.collectAsState()

    val selectedFolder by MainViewModel.selectedFolder.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var showed by remember { mutableStateOf(value = false) }
    var edit by remember { mutableStateOf(value = false) }

    var url by remember { mutableStateOf(value = passwordData.url) }
    var label by remember { mutableStateOf(value = passwordData.label) }
    var username by remember { mutableStateOf(value = passwordData.username) }
    var password by remember { mutableStateOf(value = passwordData.password) }
    var notes by remember { mutableStateOf(value = passwordData.notes) }
    var customFields by remember { mutableStateOf(value = passwordData.customFields) }
    val favicon by passwordData.favicon.collectAsState()

    MyScaffoldLayout(fab = {
        FloatingActionButton({
            if (edit) {
                if (label.isNotEmpty() && password.isNotEmpty()) {
                    val concreteCustomFields = mutableListOf<Map<String, String>>()
                    customFields.forEach { customField ->
                        try {
                            concreteCustomFields.add(
                                mapOf(
                                    "label" to customField["label"]!!,
                                    "type" to if (customField["type"]!! != "secret" &&
                                        android.util.Patterns.EMAIL_ADDRESS.matcher(customField["value"]!!)
                                            .matches()
                                    ) "email"
                                    else if (customField["type"]!! != "secret" &&
                                        android.util.Patterns.WEB_URL.matcher(customField["value"]!!)
                                            .matches()
                                    ) "url"
                                    else customField["type"]!!,
                                    "value" to customField["value"]!!
                                )
                            )
                        } catch (e: Exception) {
                        }
                    }

                    val params = mutableMapOf(
                        "password" to password,
                        "label" to label,
                        "username" to username,
                        "url" to url,
                        "notes" to notes,
                        "folder" to storedFolders[selectedFolder].id,
                        "customFields" to JsonParser.parseString(concreteCustomFields.toString()).asJsonArray.toString()
                    )
                    if (passwordData.favorite) params["favorite"] = "true"

                    MainViewModel.showDialog(
                        title = context.getString(R.string.update_password),
                        body = context.getString(R.string.update_password_body),
                        confirm = true
                    ) {
                        edit = false

                        MainViewModel.setRefreshing(refreshing = true)
                        NextcloudApiProvider.updatePasswordRequest(
                            index = passwordData.index,
                            params = params
                        )
                        MainViewModel.showSnackbar(message = context.getString(R.string.update_password_snack))
                    }
                } else MainViewModel.showDialog(
                    title = context.getString(R.string.missing_info),
                    body = context.getString(R.string.missing_info_body)
                ) {}
            } else edit = true
        }) {
            if (edit) Icon(
                painter = painterResource(id = R.drawable.ic_round_save_24),
                contentDescription = "save",
                tint = Color.White
            )
            else Icon(
                painter = painterResource(id = R.drawable.ic_round_edit_24),
                contentDescription = "edit",
                tint = Color.White
            )
        }
    }, bottomBar = {
        BottomAppBar(backgroundColor = Color.Black, cutoutShape = CircleShape) {
            IconButton(
                onClick = {
                    if (edit) {
                        edit = false

                        url = passwordData.url
                        label = passwordData.label
                        username = passwordData.username
                        password = passwordData.password
                        notes = passwordData.notes

                        passwordData.restoreCustomFields()
                        customFields = passwordData.customFields

                        MainViewModel.setSelectedFolder(folder = storedFolders.indexOfFirst { passwordData.folder == it.id })
                    } else {
                        MainViewModel.popBackStack()
                    }
                },
            ) {
                if (edit) Icon(
                    painter = painterResource(id = R.drawable.ic_round_settings_backup_restore_24),
                    contentDescription = "restore"
                )
                else Icon(
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
            Header(expanded = false, title = context.getString(R.string.password_details)) {}
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(all = 72.dp)
            ) {
                Favicon(favicon = favicon, size = 144.dp)
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
                            text = "ID: ${passwordData.id}",
                            fontSize = 17.sp,
                            overflow = TextOverflow.Clip,
                            maxLines = 2,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                        Text(
                            text = context.getString(
                                R.string.security_status, when (passwordData.status) {
                                    0 -> context.getString(R.string.good)
                                    2 -> context.getString(R.string.bad)
                                    else -> context.getString(R.string.weak)
                                }
                            ),
                            fontSize = 14.sp,
                            color = when (passwordData.status) {
                                0 -> Color.Green
                                2 -> Color.Red
                                else -> Color.Yellow
                            },
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Text(
                            text = context.getString(
                                R.string.password_shared,
                                if (passwordData.shared) context.getString(R.string.yes)
                                else context.getString(R.string.no)
                            ),
                            fontSize = 14.sp,
                            color = when (passwordData.shared) {
                                true -> Color.Yellow
                                else -> Color.Green
                            },
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Text(
                            text = context.getString(
                                R.string.date_info,
                                passwordData.created,
                                passwordData.edited
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
                                enabled = edit,
                                folder = storedFolders.indexOfFirst { passwordData.folder == it.id })
                            FavoriteIcon(favorite = passwordData.favorite) {
                                MainViewModel.setRefreshing(refreshing = true)

                                NextcloudApiProvider.updatePasswordRequest(
                                    index = passwordData.index,
                                    params = if (!passwordData.favorite) mutableMapOf("favorite" to "true") else mutableMapOf()
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        enabled = edit,
                        label = { Text(text = context.getString(R.string.url)) },
                        shape = RoundedCornerShape(size = 8.dp),
                        trailingIcon = {
                            Row {
                                IconButton(onClick = {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    )
                                }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_round_public_24),
                                        contentDescription = "browse_url"
                                    )
                                }
                                CopyButton(label = context.getString(R.string.url), clip = url)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    )
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        enabled = edit,
                        label = { Text(text = context.getString(R.string.label)) },
                        shape = RoundedCornerShape(size = 8.dp),
                        isError = label.isEmpty(),
                        trailingIcon = {
                            CopyButton(label = context.getString(R.string.label), clip = label)
                        },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        enabled = edit,
                        label = { Text(text = context.getString(R.string.username)) },
                        shape = RoundedCornerShape(size = 8.dp),
                        trailingIcon = {
                            CopyButton(
                                label = context.getString(R.string.username),
                                clip = username
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        enabled = edit,
                        label = { Text(text = context.getString(R.string.password)) },
                        shape = RoundedCornerShape(size = 8.dp),
                        isError = password.isEmpty(),
                        singleLine = true,
                        visualTransformation = if (showed) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            Row {
                                IconButton(onClick = {
                                    coroutineScope.launch {
                                        password = NextcloudApiProvider.generatePassword()
                                    }
                                }, enabled = edit) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_round_autorenew_24),
                                        contentDescription = "generate_password"
                                    )
                                }
                                IconButton(onClick = { showed = !showed }) {
                                    Icon(
                                        painter = painterResource(
                                            id = if (showed) R.drawable.ic_round_visibility_24
                                            else R.drawable.ic_round_visibility_off_24
                                        ),
                                        contentDescription = "show_password"
                                    )
                                }
                                CopyButton(
                                    label = context.getString(R.string.password),
                                    clip = password
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        enabled = edit,
                        label = { Text(text = context.getString(R.string.notes)) },
                        shape = RoundedCornerShape(size = 8.dp),
                        trailingIcon = {
                            CopyButton(label = context.getString(R.string.notes), clip = notes)
                        },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    )
                    customFields.forEach { customField ->
                        if (customField.containsKey(key = "value"))
                            OutlinedTextField(
                                value = customField["value"]!!,
                                onValueChange = { customField["value"] = it },
                                label = { Text(text = customField["label"]!!) },
                                shape = RoundedCornerShape(size = 8.dp),
                                enabled = edit,
                                visualTransformation = if (customField["type"] == "secret")
                                    PasswordVisualTransformation() else VisualTransformation.None,
                                trailingIcon = {
                                    Row {
                                        IconButton(
                                            onClick = {
                                                if (customField["type"] == "secret")
                                                    customField["type"] = "text"
                                                else customField["type"] = "secret"
                                            },
                                            enabled = edit
                                        ) {
                                            Icon(
                                                painter = if (customField["type"] != "secret")
                                                    painterResource(id = R.drawable.ic_round_lock_open_24)
                                                else painterResource(id = R.drawable.ic_round_lock_24),
                                                contentDescription = "make_field_secret"
                                            )
                                        }
                                        IconButton(
                                            onClick = { customFields.remove(element = customField) },
                                            enabled = edit
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_round_delete_forever_24),
                                                contentDescription = "delete_custom_field",
                                                tint = if (edit) Color.Red else Color.Gray
                                            )
                                        }
                                        CopyButton(
                                            label = context.getString(R.string.custom_field),
                                            clip = customField["value"]!!
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                            )
                        else OutlinedTextField(
                            value = customField["label"]!!,
                            onValueChange = { customField["label"] = it },
                            label = { Text(text = context.getString(R.string.custom_field)) },
                            shape = RoundedCornerShape(size = 8.dp),
                            isError = customField["label"]!!.isEmpty(),
                            trailingIcon = {
                                Row {
                                    IconButton(onClick = {
                                        if (customField["label"]!!.isNotEmpty()) {
                                            customField["type"] = "text"
                                            customField["value"] = ""
                                        } else MainViewModel.showDialog(
                                            title = context.getString(R.string.missing_info),
                                            body = context.getString(R.string.missing_info_body)
                                        ) {}
                                    }) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_round_done_24),
                                            contentDescription = "confirm_new_custom_label"
                                        )
                                    }
                                    IconButton(onClick = { customFields.remove(element = customField) }) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_round_close_24),
                                            contentDescription = "delete_custom_field"
                                        )
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = {
                            MainViewModel.showDialog(
                                title = context.getString(R.string.delete_password),
                                body = context.getString(R.string.delete_password_body),
                                confirm = true
                            ) {
                                NextcloudApiProvider.deletePasswordRequest(index = passwordData.index)
                                MainViewModel.popBackStack()
                            }
                        }
                        ) {
                            Text(
                                text = context.getString(R.string.delete),
                                color = Color.Red
                            )
                        }
                        TextButton(
                            onClick = { customFields.add(element = mutableStateMapOf("label" to "")) },
                            enabled = edit
                        ) {
                            Text(text = context.getString(R.string.add_custom_field))
                        }
                    }
                }
            }
        }
    }
}