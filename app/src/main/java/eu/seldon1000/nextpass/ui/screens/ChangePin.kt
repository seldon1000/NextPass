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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.CentralAppControl
import eu.seldon1000.nextpass.ui.items.TextFieldItem
import eu.seldon1000.nextpass.ui.layout.Header
import eu.seldon1000.nextpass.ui.layout.MyScaffoldLayout
import eu.seldon1000.nextpass.ui.theme.colors

@ExperimentalMaterialApi
@Composable
fun ChangePin(viewModel: CentralAppControl) {
    val context = LocalContext.current

    var pin by remember { mutableStateOf(value = "") }
    var tempPin by remember { mutableStateOf(value = "") }

    var showed by remember { mutableStateOf(value = false) }
    var confirm by remember { mutableStateOf(value = false) }

    MyScaffoldLayout(fab = {
        FloatingActionButton({
            if (confirm) {
                if (pin == tempPin) {
                    viewModel.showDialog(
                        title = context.getString(R.string.change_pin),
                        body = {
                            Text(
                                text = context.getString(R.string.change_pin_body),
                                fontSize = 14.sp
                            )
                        },
                        confirm = true
                    ) {
                        viewModel.setPin(pin = pin)
                        viewModel.setLockTimeout(timeout = 0)
                        viewModel.popBackStack()
                        viewModel.showSnackbar(message = context.getString(R.string.pin_changed_snack))
                    }
                } else viewModel.showDialog(
                    title = context.getString(R.string.wrong_pin),
                    body = {
                        Text(
                            text = context.getString(R.string.wrong_pin_body),
                            fontSize = 14.sp
                        )
                    }
                )
            } else {
                if (pin.length >= 4) {
                    tempPin = pin
                    pin = ""
                    showed = false
                    confirm = true
                } else viewModel.showDialog(
                    title = context.getString(R.string.pin_short),
                    body = {
                        Text(
                            text = context.getString(R.string.pin_short_body),
                            fontSize = 14.sp
                        )
                    }
                )
            }
        }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_round_done_24),
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
            IconButton(onClick = {
                if (confirm) {
                    confirm = false
                    pin = tempPin
                } else viewModel.popBackStack()
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
                title = when {
                    confirm -> context.getString(R.string.confirm_new_pin)
                    else -> context.getString(R.string.create_new_pin)
                }
            )
            Box(
                modifier = Modifier
                    .width(width = 176.dp)
                    .padding(top = 200.dp)
            ) {
                TextFieldItem(
                    text = pin,
                    onTextChanged = { pin = it },
                    label = "PIN",
                    required = pin.length < 4,
                    protected = true,
                    showed = showed
                ) {
                    IconButton(onClick = { showed = !showed }) {
                        Crossfade(targetState = showed) { state ->
                            Icon(
                                painter = painterResource(
                                    id = if (state) R.drawable.ic_round_visibility_24
                                    else R.drawable.ic_round_visibility_off_24
                                ),
                                contentDescription = "show_password"
                            )
                        }
                    }
                }
            }
        }
    }
}