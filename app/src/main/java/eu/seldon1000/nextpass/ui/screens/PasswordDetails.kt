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
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.api.*
import eu.seldon1000.nextpass.CentralAppControl
import eu.seldon1000.nextpass.ui.items.*
import eu.seldon1000.nextpass.ui.layout.Header
import eu.seldon1000.nextpass.ui.layout.MyScaffoldLayout
import eu.seldon1000.nextpass.ui.theme.colors
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString

@ExperimentalFoundationApi
@ExperimentalAnimationApi
@ExperimentalMaterialApi
@Composable
fun PasswordDetails(passwordData: Password) {
    val context = LocalContext.current

    val storedFolders by NextcloudApi.storedFolders.collectAsState()

    val selectedFolder by CentralAppControl.selectedFolder.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var showed by remember { mutableStateOf(value = false) }
    var edit by remember { mutableStateOf(value = false) }

    var url by remember { mutableStateOf(value = passwordData.url) }
    var label by remember { mutableStateOf(value = passwordData.label) }
    var username by remember { mutableStateOf(value = passwordData.username) }
    var password by remember { mutableStateOf(value = passwordData.password) }
    var notes by remember { mutableStateOf(value = passwordData.notes) }
    var tags by remember { mutableStateOf(value = passwordData.tags) }
    var customFields by remember { mutableStateOf(value = passwordData.customFieldsList) }
    val favicon by passwordData.favicon.collectAsState()

    MyScaffoldLayout(fab = {
        FloatingActionButton(onClick = {
            if (edit) {
                if (label.isNotEmpty() && password.isNotEmpty()) {
                    CentralAppControl.showDialog(
                        title = context.getString(R.string.update_password),
                        body = {
                            Text(
                                text = context.getString(R.string.update_action_body),
                                fontSize = 14.sp
                            )
                        },
                        confirm = true
                    ) {
                        edit = false

                        val concreteCustomFields = mutableListOf<Map<String, String>>()
                        customFields.forEach { customField ->
                            try {
                                concreteCustomFields.add(
                                    mapOf(
                                        "label" to customField.label.value,
                                        "type" to if (customField.type.value != "secret" &&
                                            android.util.Patterns.EMAIL_ADDRESS.matcher(customField.value.value)
                                                .matches()
                                        ) "email"
                                        else if (customField.type.value != "secret" &&
                                            android.util.Patterns.WEB_URL.matcher(customField.value.value)
                                                .matches()
                                        ) "url"
                                        else customField.type.value,
                                        "value" to customField.value.value
                                    )
                                )
                            } catch (e: Exception) {
                            }
                        }
                        val params = mutableMapOf(
                            "id" to passwordData.id,
                            "label" to label,
                            "username" to username,
                            "password" to password,
                            "url" to url,
                            "notes" to notes,
                            "customFields" to NextcloudApi.json.encodeToString(value = concreteCustomFields),
                            "folder" to storedFolders[selectedFolder].id,
                            "hash" to passwordData.hash
                        )
                        if (passwordData.favorite) params["favorite"] = "true"

                        CentralAppControl.executeRequest {
                            NextcloudApi.updatePasswordRequest(
                                params = params,
                                tags = tags
                            ) { it() }
                            CentralAppControl.showSnackbar(message = context.getString(R.string.password_updated_snack))
                        }
                    }
                } else CentralAppControl.showDialog(
                    title = context.getString(R.string.missing_info),
                    body = {
                        Text(text = context.getString(R.string.missing_info_body), fontSize = 14.sp)
                    }
                )
            } else edit = true
        }) {
            Crossfade(targetState = edit) { state ->
                Icon(
                    painter = if (state) painterResource(id = R.drawable.ic_round_save_24)
                    else painterResource(id = R.drawable.ic_round_edit_24),
                    contentDescription = "save",
                    tint = colors!!.onBackground
                )
            }
        }
    }, bottomBar = {
        BottomAppBar(
            backgroundColor = Color.Black,
            cutoutShape = CircleShape,
            modifier = Modifier.clip(shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
        ) {
            IconButton(
                onClick = {
                    if (edit) {
                        edit = false

                        url = passwordData.url
                        label = passwordData.label
                        username = passwordData.username
                        password = passwordData.password
                        notes = passwordData.notes
                        tags = passwordData.tags

                        passwordData.resetCustomFields()
                        customFields = passwordData.customFieldsList

                        CentralAppControl.setSelectedFolder(folder = storedFolders.indexOfFirst { it.id == passwordData.folder })
                    } else CentralAppControl.popBackStack()
                }
            ) {
                Crossfade(targetState = edit) { state ->
                    Icon(
                        painter = if (state) painterResource(id = R.drawable.ic_round_settings_backup_restore_24)
                        else painterResource(id = R.drawable.ic_round_back_arrow_24),
                        contentDescription = "restore"
                    )
                }
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
            Header(expanded = false, title = context.getString(R.string.password_details))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(all = 72.dp)
            ) { Favicon(favicon = favicon, size = 144.dp) }
            Card(
                elevation = 8.dp,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = paddingValues.calculateBottomPadding() + 40.dp
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
                                R.string.security_status,
                                passwordData.statusCode
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
                                passwordData.createdDate,
                                passwordData.updatedDate
                            ),
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        TagsRow(
                            tags = tags,
                            alignment = Alignment.Start
                        ) {
                            if (it != null) {
                                if (tags.contains(element = it)) tags.remove(element = it)
                                else tags.add(element = it)

                                val params = mutableMapOf(
                                    "id" to passwordData.id,
                                    "label" to passwordData.label,
                                    "username" to passwordData.username,
                                    "password" to passwordData.password,
                                    "url" to passwordData.url,
                                    "notes" to passwordData.notes,
                                    "customFields" to passwordData.customFields,
                                    "folder" to passwordData.folder,
                                    "hash" to passwordData.hash
                                )
                                if (passwordData.favorite) params["favorite"] = "true"

                                CentralAppControl.executeRequest { handler ->
                                    NextcloudApi.updatePasswordRequest(
                                        params = params,
                                        tags = tags
                                    ) { handler() }
                                }
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Crossfade(targetState = edit) { state ->
                                DropdownFolderList(
                                    enabled = state,
                                    folder = storedFolders.indexOfFirst { passwordData.folder == it.id })
                            }
                            FavoriteButton(favorite = passwordData.favorite) {
                                val params = mutableMapOf(
                                    "id" to passwordData.id,
                                    "label" to passwordData.label,
                                    "username" to passwordData.username,
                                    "password" to passwordData.password,
                                    "url" to passwordData.url,
                                    "notes" to passwordData.notes,
                                    "customFields" to passwordData.customFields,
                                    "folder" to passwordData.folder,
                                    "hash" to passwordData.hash
                                )
                                if (it) params["favorite"] = "true"

                                CentralAppControl.executeRequest { handler ->
                                    NextcloudApi.updatePasswordRequest(
                                        params = params,
                                        tags = passwordData.tags
                                    ) { handler() }
                                }
                            }
                        }
                    }
                    TextFieldItem(
                        text = url,
                        onTextChanged = { url = it },
                        label = context.getString(R.string.url),
                        enabled = edit
                    ) {
                        IconButton(onClick = {
                            try {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW, Uri.parse(
                                            if (!(url.startsWith("https") ||
                                                        url.startsWith("http"))
                                            ) "https://$url" else url
                                        )
                                    )
                                )
                            } catch (e: Exception) {
                                CentralAppControl.showSnackbar(message = context.getString(R.string.link_broken_snack))
                            }
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_round_public_24),
                                contentDescription = "browse_url",
                                tint = colors!!.onBackground
                            )
                        }
                        CopyButton(label = context.getString(R.string.url), clip = url)
                    }
                    TextFieldItem(
                        text = label,
                        onTextChanged = { label = it },
                        label = context.getString(R.string.label),
                        enabled = edit,
                        required = true,
                        capitalized = true
                    ) { CopyButton(label = context.getString(R.string.label), clip = label) }
                    TextFieldItem(
                        text = username,
                        onTextChanged = { username = it },
                        label = context.getString(R.string.username),
                        enabled = edit
                    ) { CopyButton(label = context.getString(R.string.username), clip = username) }
                    TextFieldItem(
                        text = password,
                        onTextChanged = { password = it },
                        label = context.getString(R.string.password),
                        enabled = edit,
                        required = true,
                        protected = true,
                        showed = showed
                    ) {
                        Crossfade(targetState = edit) { state ->
                            var rotation by remember { mutableStateOf(value = 0F) }
                            val angle by animateFloatAsState(
                                targetValue = rotation,
                                animationSpec = tween(
                                    durationMillis = 1000,
                                    easing = LinearOutSlowInEasing
                                )
                            )

                            IconButton(onClick = {
                                CentralAppControl.executeRequest {
                                    coroutineScope.launch {
                                        password = NextcloudApi.generatePassword()
                                    }
                                }

                                if (rotation >= 360F * 10) {
                                    rotation = 0F

                                    CentralAppControl.showSnackbar(message = context.getString(R.string.refresh_easter_egg))
                                } else rotation += 360F
                            }, enabled = state) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_round_autorenew_24),
                                    contentDescription = "generate_password",
                                    tint = if (state) colors!!.onBackground else Color.Gray,
                                    modifier = Modifier.rotate(degrees = angle)
                                )
                            }
                        }
                        IconButton(onClick = { showed = !showed }) {
                            Crossfade(targetState = showed) { state1 ->
                                Icon(
                                    painter = painterResource(
                                        id = if (state1) R.drawable.ic_round_visibility_24
                                        else R.drawable.ic_round_visibility_off_24
                                    ),
                                    contentDescription = "show_password",
                                    tint = colors!!.onBackground
                                )
                            }
                        }
                        CopyButton(
                            label = context.getString(R.string.password),
                            clip = password
                        )
                    }
                    TextFieldItem(
                        text = notes,
                        onTextChanged = { notes = it },
                        label = context.getString(R.string.notes),
                        enabled = edit,
                        capitalized = true
                    ) {
                        CopyButton(
                            label = context.getString(R.string.notes),
                            clip = notes
                        )
                    }
                    customFields.forEach { customField ->
                        if (customField.type.value.isNotEmpty())
                            TextFieldItem(
                                text = customField.value.value,
                                onTextChanged = { customField.value.value = it },
                                label = customField.label.value,
                                enabled = edit,
                                protected = customField.type.value != "secret",
                                showed = customField.type.value != "secret"
                            ) {
                                Crossfade(targetState = edit) { state ->
                                    Row {
                                        IconButton(
                                            onClick = {
                                                if (customField.type.value == "secret")
                                                    customField.type.value = "text"
                                                else customField.type.value = "secret"
                                            },
                                            enabled = state
                                        ) {
                                            Crossfade(targetState = customField.type.value != "secret") { state1 ->
                                                Icon(
                                                    painter = if (state1)
                                                        painterResource(id = R.drawable.ic_round_lock_open_24)
                                                    else painterResource(id = R.drawable.ic_round_lock_24),
                                                    contentDescription = "make_field_secret",
                                                    tint = if (state) colors!!.onBackground else Color.Gray
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = { customFields.remove(element = customField) },
                                            enabled = state
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_round_delete_forever_24),
                                                contentDescription = "delete_custom_field",
                                                tint = if (state) Color.Red else Color.Gray
                                            )
                                        }
                                    }
                                }
                                CopyButton(
                                    label = context.getString(R.string.custom_field),
                                    clip = customField.value.value
                                )
                            }
                        else TextFieldItem(
                            text = customField.label.value,
                            onTextChanged = { customField.label.value = it },
                            label = context.getString(R.string.custom_field),
                            enabled = edit,
                            required = true,
                            capitalized = true
                        ) {
                            IconButton(onClick = {
                                if (customField.label.value.isNotEmpty())
                                    customField.type.value = "text"
                                else CentralAppControl.showDialog(
                                    title = context.getString(R.string.missing_info),
                                    body = {
                                        Text(
                                            text = context.getString(R.string.missing_info_body),
                                            fontSize = 14.sp
                                        )
                                    }
                                )
                            }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_round_done_24),
                                    contentDescription = "confirm_new_custom_label",
                                    tint = colors!!.onBackground
                                )
                            }
                            IconButton(onClick = { customFields.remove(element = customField) }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_round_close_24),
                                    contentDescription = "delete_custom_field",
                                    tint = colors!!.onBackground
                                )
                            }
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = {
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
                                    NextcloudApi.deletePasswordRequest(id = passwordData.id) { it() }
                                    CentralAppControl.popBackStack()
                                }
                            }
                        }
                        ) {
                            Text(
                                text = context.getString(R.string.delete),
                                color = Color.Red
                            )
                        }
                        Crossfade(targetState = edit) { state ->
                            TextButton(
                                onClick = { customFields.add(element = CustomField()) },
                                enabled = state
                            ) { Text(text = context.getString(R.string.add_custom_field)) }
                        }
                    }
                }
            }
        }
    }
}