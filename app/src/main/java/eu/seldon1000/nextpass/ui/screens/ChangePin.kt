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

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.ui.MainViewModel
import eu.seldon1000.nextpass.ui.layout.Header
import eu.seldon1000.nextpass.ui.layout.MyScaffoldLayout

@Composable
fun ChangePin(change: Boolean) {
    val context = LocalContext.current

    (context as Activity).window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

    val protected by MainViewModel.pinProtected.collectAsState()

    var pin by remember { mutableStateOf(value = "") }
    var tempPin by remember { mutableStateOf(value = "") }

    var showed by remember { mutableStateOf(value = false) }
    var ready by remember { mutableStateOf(value = false) }
    var confirm by remember { mutableStateOf(value = false) }

    MyScaffoldLayout(fab = {
        FloatingActionButton({
            if (protected) {
                if (change) {
                    if (confirm) {
                        if (pin == tempPin) {
                            MainViewModel.showDialog(
                                title = context.getString(R.string.change_pin),
                                body = context.getString(R.string.change_pin_body),
                                confirm = true
                            ) {
                                MainViewModel.setNewPin(pin = pin)
                                MainViewModel.popBackStack()
                                MainViewModel.showSnackbar(message = context.getString(R.string.pin_changed_snack))
                            }
                        } else MainViewModel.showDialog(
                            title = context.getString(R.string.wrong_pin),
                            body = context.getString(R.string.wrong_pin_body)
                        ) {}
                    } else if (ready) {
                        if (pin.length >= 4) {
                            tempPin = pin
                            pin = ""
                            showed = false
                            confirm = true
                        } else MainViewModel.showDialog(
                            title = context.getString(R.string.pin_short),
                            body = context.getString(R.string.pin_short_body)
                        ) {}
                    } else {
                        if (MainViewModel.checkPin(pin = pin)) {
                            pin = ""
                            showed = false
                            ready = true
                        } else {
                            MainViewModel.showDialog(
                                title = context.getString(R.string.wrong_pin),
                                body = context.getString(R.string.wrong_pin_body)
                            ) {}
                        }
                    }
                } else {
                    if (MainViewModel.checkPin(pin = pin)) {
                        MainViewModel.showDialog(
                            title = context.getString(R.string.disable_pin),
                            body = context.getString(R.string.disable_pin_body),
                            confirm = true
                        ) {
                            MainViewModel.disablePin()
                            MainViewModel.popBackStack()
                            MainViewModel.showSnackbar(message = context.getString(R.string.pin_disabled_snack))
                        }
                    } else {
                        MainViewModel.showDialog(
                            title = context.getString(R.string.wrong_pin),
                            body = context.getString(R.string.wrong_pin_body)
                        ) {}
                    }
                }
            } else {
                if (confirm) {
                    if (pin == tempPin) {
                        MainViewModel.showDialog(
                            title = context.getString(R.string.change_pin),
                            body = context.getString(R.string.change_pin_body),
                            confirm = true
                        ) {
                            MainViewModel.setNewPin(pin = pin)
                            MainViewModel.popBackStack()
                            MainViewModel.showSnackbar(message = context.getString(R.string.pin_changed_snack))
                        }
                    } else MainViewModel.showDialog(
                        title = context.getString(R.string.wrong_pin),
                        body = context.getString(R.string.wrong_pin_body)
                    ) {}
                } else {
                    if (pin.length >= 4) {
                        tempPin = pin
                        pin = ""
                        showed = false
                        confirm = true
                    } else MainViewModel.showDialog(
                        title = context.getString(R.string.pin_short),
                        body = context.getString(R.string.pin_short_body)
                    ) {}
                }
            }
        }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_round_done_24),
                contentDescription = "save",
                tint = Color.White
            )
        }
    }, bottomBar = {
        BottomAppBar(backgroundColor = Color.Black, cutoutShape = CircleShape) {
            IconButton(onClick = {
                MainViewModel.showDialog(
                    title = context.getString(R.string.revert_changes),
                    body = context.getString(R.string.revert_changes_body),
                    confirm = true
                ) { MainViewModel.popBackStack() }
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_round_back_arrow_24),
                    contentDescription = "back"
                )
            }
        }
    }) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Header(
                expanded = false,
                title = if (protected) {
                    when {
                        confirm -> context.getString(R.string.confirm_new_pin)
                        ready -> context.getString(R.string.create_new_pin)
                        else -> context.getString(R.string.insert_current_pin)
                    }
                } else {
                    if (confirm) context.getString(R.string.confirm_new_pin)
                    else context.getString(R.string.create_new_pin)
                }
            ) {}
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it },
                label = { Text(text = "PIN") },
                isError = pin.length < 4,
                shape = RoundedCornerShape(size = 8.dp),
                singleLine = true,
                visualTransformation = if (showed) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                trailingIcon = {
                    IconButton(onClick = { showed = !showed }) {
                        Icon(
                            painter = painterResource(
                                id = if (showed) R.drawable.ic_round_visibility_24
                                else R.drawable.ic_round_visibility_off_24
                            ),
                            contentDescription = "show_password"
                        )
                    }
                },
                modifier = Modifier
                    .width(width = 200.dp)
                    .padding(top = 192.dp),
            )
        }
    }
}