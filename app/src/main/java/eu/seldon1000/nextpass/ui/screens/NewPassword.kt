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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonParser
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.api.NextcloudApiProvider
import eu.seldon1000.nextpass.ui.MainViewModel
import eu.seldon1000.nextpass.ui.items.DropdownFolderList
import eu.seldon1000.nextpass.ui.items.Favicon
import eu.seldon1000.nextpass.ui.items.FavoriteIcon
import eu.seldon1000.nextpass.ui.items.TextFieldItem
import eu.seldon1000.nextpass.ui.layout.Header
import eu.seldon1000.nextpass.ui.layout.MyScaffoldLayout
import eu.seldon1000.nextpass.ui.theme.colors
import kotlinx.coroutines.launch
import java.math.BigInteger
import java.security.MessageDigest

@ExperimentalMaterialApi
@Composable
fun NewPassword() {
    val context = LocalContext.current

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    val storedFolders by NextcloudApiProvider.storedFolders.collectAsState()

    val selectedFolder by MainViewModel.selectedFolder.collectAsState()

    var showed by remember { mutableStateOf(value = false) }
    var favorite by remember { mutableStateOf(value = false) }

    var url by remember { mutableStateOf(value = "") }
    var label by remember { mutableStateOf(value = "") }
    var username by remember { mutableStateOf(value = "") }
    var password by remember { mutableStateOf(value = "") }
    var notes by remember { mutableStateOf(value = "") }
    val favicon by NextcloudApiProvider.currentRequestedFavicon.collectAsState()
    val customFields = SnapshotStateList<SnapshotStateMap<String, String>>()

    MyScaffoldLayout(fab = {
        FloatingActionButton(onClick = {
            if (label.isNotEmpty() && password.isNotEmpty()) {
                MainViewModel.showDialog(
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
                                    "label" to "\"${customField["label"]!!}\"",
                                    "type" to if (customField["type"]!! != "secret" &&
                                        android.util.Patterns.EMAIL_ADDRESS.matcher(customField["value"]!!)
                                            .matches()
                                    ) "email"
                                    else if (customField["type"]!! != "secret" &&
                                        android.util.Patterns.WEB_URL.matcher(customField["value"]!!)
                                            .matches()
                                    ) "url"
                                    else customField["type"]!!,
                                    "value" to "\"${customField["value"]!!}\""
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
                        "customFields" to JsonParser.parseString(concreteCustomFields.toString()).asJsonArray.toString()
                    )
                    if (favorite) params["favorite"] = "true"

                    MainViewModel.setRefreshing(refreshing = true)
                    NextcloudApiProvider.createPasswordRequest(params = params)
                    MainViewModel.popBackStack()
                    MainViewModel.showSnackbar(message = context.getString(R.string.new_password_snack))
                }
            } else MainViewModel.showDialog(
                title = context.getString(R.string.missing_info),
                body = {
                    Text(text = context.getString(R.string.missing_info_body), fontSize = 14.sp)
                }
            ) {}
        }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_round_save_24),
                contentDescription = "save",
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
        }
    }) { paddingValues ->
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = scrollState, enabled = true)
        ) {
            Header(expanded = false, title = context.getString(R.string.new_password)) {}
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(all = 72.dp)
            ) { Favicon(favicon = favicon, size = 144.dp) }
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
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        DropdownFolderList(folder = selectedFolder)
                        FavoriteIcon(favorite = favorite) { favorite = !favorite }
                    }
                    TextFieldItem(text = url, onTextChanged = {
                        if (it.length >= url.length || it.isEmpty())
                            NextcloudApiProvider.faviconRequest(data = it)

                        url = it
                    }, label = context.getString(R.string.url)) {}
                    TextFieldItem(
                        text = label,
                        onTextChanged = { label = it },
                        label = context.getString(R.string.label),
                        required = true,
                        capitalized = true
                    ) {}
                    TextFieldItem(
                        text = username,
                        onTextChanged = { username = it },
                        label = context.getString(R.string.username)
                    ) {}
                    TextFieldItem(
                        text = password,
                        onTextChanged = { password = it },
                        label = context.getString(R.string.password),
                        required = true,
                        protected = true,
                        showed = showed
                    ) {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                password = NextcloudApiProvider.generatePassword()
                            }
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_round_autorenew_24),
                                contentDescription = "generate_password",
                                tint = colors!!.onBackground
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
                    ) {}
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
                                } else MainViewModel.showDialog(
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