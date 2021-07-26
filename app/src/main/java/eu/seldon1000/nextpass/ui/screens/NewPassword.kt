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

import androidx.compose.animation.Crossfade
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
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.api.NextcloudApi
import eu.seldon1000.nextpass.api.Tag
import eu.seldon1000.nextpass.CentralAppControl
import eu.seldon1000.nextpass.ui.items.*
import eu.seldon1000.nextpass.ui.layout.Header
import eu.seldon1000.nextpass.ui.layout.MyScaffoldLayout
import eu.seldon1000.nextpass.ui.theme.colors
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import java.math.BigInteger
import java.security.MessageDigest

@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun NewPassword() {
    val context = LocalContext.current

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    val storedFolders by NextcloudApi.storedFolders.collectAsState()

    val selectedFolder by CentralAppControl.selectedFolder.collectAsState()

    var showed by remember { mutableStateOf(value = false) }
    var favorite by remember { mutableStateOf(value = false) }

    var url by remember { mutableStateOf(value = "") }
    var label by remember { mutableStateOf(value = "") }
    var username by remember { mutableStateOf(value = "") }
    var password by remember { mutableStateOf(value = "") }
    var notes by remember { mutableStateOf(value = "") }
    val tags by remember { mutableStateOf(value = mutableStateListOf<Tag>()) }
    val customFields by remember { mutableStateOf(value = mutableStateListOf<SnapshotStateMap<String, String>>()) }
    val favicon by NextcloudApi.currentRequestedFavicon.collectAsState()

    MyScaffoldLayout(fab = {
        FloatingActionButton(onClick = {
            if (label.isNotEmpty() && password.isNotEmpty()) {
                CentralAppControl.showDialog(
                    title = context.getString(R.string.create_password),
                    body = {
                        Text(
                            text = context.getString(R.string.create_password_body),
                            fontSize = 14.sp
                        )
                    },
                    confirm = true
                ) {
                    var hash = BigInteger(
                        1, MessageDigest.getInstance("SHA-1")
                            .digest(password.toByteArray())
                    ).toString(16)
                    while (hash.length < 32) hash = "0$hash"

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
                        "hash" to hash,
                        "folder" to storedFolders[selectedFolder].id,
                        "customFields" to NextcloudApi.json.encodeToString(value = concreteCustomFields)
                    )
                    if (favorite) params["favorite"] = "true"

                    CentralAppControl.executeRequest {
                        NextcloudApi.createPasswordRequest(params = params, tags = tags)
                        CentralAppControl.setSelectedFolder(folder = CentralAppControl.currentFolder.value)
                        CentralAppControl.popBackStack()
                        CentralAppControl.showSnackbar(message = context.getString(R.string.password_created_snack))
                    }
                }
            } else CentralAppControl.showDialog(
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
                painter = painterResource(id = R.drawable.ic_round_save_24),
                contentDescription = "save",
                tint = colors!!.onBackground
            )
        }
    }, bottomBar = {
        BottomAppBar(
            backgroundColor = Color.Black,
            cutoutShape = CircleShape,
            modifier = Modifier.clip(shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
        ) {
            IconButton(onClick = { CentralAppControl.popBackStack() }) {
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
            Header(expanded = false, title = context.getString(R.string.new_password))
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
                    TagsRow(
                        tags = tags,
                        alignment = Alignment.Start
                    ) {
                        if (it != null) {
                            if (tags.contains(element = it)) tags.remove(element = it)
                            else tags.add(element = it)
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        DropdownFolderList(folder = selectedFolder)
                        FavoriteButton(favorite = favorite) { favorite = !favorite }
                    }
                    TextFieldItem(text = url, onTextChanged = {
                        if (it.length >= url.length || it.isEmpty())
                            NextcloudApi.faviconRequest(data = it)

                        url = it
                    }, label = context.getString(R.string.url))
                    TextFieldItem(
                        text = label,
                        onTextChanged = { label = it },
                        label = context.getString(R.string.label),
                        required = true,
                        capitalized = true
                    )
                    TextFieldItem(
                        text = username,
                        onTextChanged = { username = it },
                        label = context.getString(R.string.username)
                    )
                    TextFieldItem(
                        text = password,
                        onTextChanged = { password = it },
                        label = context.getString(R.string.password),
                        required = true,
                        protected = true,
                        showed = showed
                    ) {
                        var rotation by remember { mutableStateOf(value = 0F) }
                        val angle by animateFloatAsState(
                            targetValue = rotation,
                            animationSpec = tween(
                                durationMillis = 1000,
                                easing = LinearOutSlowInEasing
                            )
                        )

                        IconButton(onClick = {
                            coroutineScope.launch {
                                password = NextcloudApi.generatePassword()
                            }

                            if (rotation >= 360F * 10) {
                                rotation = 0F

                                CentralAppControl.showSnackbar(message = context.getString(R.string.refresh_easter_egg))
                            } else rotation += 360F
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_round_autorenew_24),
                                contentDescription = "generate_password",
                                tint = colors!!.onBackground,
                                modifier = Modifier.rotate(degrees = angle)
                            )
                        }
                        IconButton(onClick = { showed = !showed }) {
                            Crossfade(targetState = showed) { state ->
                                Icon(
                                    painter = painterResource(
                                        id = if (state) R.drawable.ic_round_visibility_24
                                        else R.drawable.ic_round_visibility_off_24
                                    ),
                                    contentDescription = "show_password",
                                    tint = colors!!.onBackground
                                )
                            }
                        }
                    }
                    TextFieldItem(
                        text = notes,
                        onTextChanged = { notes = it },
                        label = context.getString(R.string.notes),
                        capitalized = true
                    )
                    customFields.forEach { customField ->
                        if (customField.containsKey(key = "value"))
                            TextFieldItem(
                                text = customField["value"]!!,
                                onTextChanged = { customField["value"] = it },
                                label = customField["label"]!!,
                                protected = customField["type"]!! != "secret",
                                showed = customField["type"]!! != "secret"
                            ) {
                                IconButton(onClick = {
                                    if (customField["type"]!! == "secret")
                                        customField["type"] = "text"
                                    else customField["type"] = "secret"
                                }) {
                                    Crossfade(targetState = customField["type"]!! == "text") { state ->
                                        Icon(
                                            painter = if (state) painterResource(
                                                id = R.drawable.ic_round_lock_open_24
                                            ) else painterResource(id = R.drawable.ic_round_lock_24),
                                            contentDescription = "make_field_secret",
                                            tint = colors!!.onBackground
                                        )
                                    }
                                }
                                IconButton(onClick = { customFields.remove(element = customField) }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_round_delete_forever_24),
                                        contentDescription = "delete_custom_field",
                                        tint = Color.Red
                                    )
                                }
                            }
                        else TextFieldItem(
                            text = customField["label"]!!,
                            onTextChanged = { customField["label"] = it },
                            label = context.getString(R.string.custom_field),
                            required = true
                        ) {
                            IconButton(onClick = {
                                if (customField["label"]!!.isNotEmpty()) {
                                    customField["type"] = "text"
                                    customField["value"] = ""
                                } else CentralAppControl.showDialog(
                                    title = context.getString(R.string.missing_info),
                                    body = {
                                        Text(
                                            text = context.getString(R.string.missing_info_body),
                                            fontSize = 14.sp
                                        )
                                    }
                                ) {}
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
                    Box(
                        contentAlignment = Alignment.CenterEnd,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = { customFields.add(element = mutableStateMapOf("label" to "")) }
                        ) { Text(text = context.getString(R.string.add_custom_field)) }
                    }
                }
            }
        }
    }
}