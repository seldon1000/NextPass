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

import androidx.compose.foundation.layout.*
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
import eu.seldon1000.nextpass.api.NextcloudApiProvider
import eu.seldon1000.nextpass.ui.MainViewModel
import eu.seldon1000.nextpass.ui.layout.Header
import eu.seldon1000.nextpass.ui.theme.Orange500

@Composable
fun AccessPin(shouldRaiseBiometric: Boolean) {
    val context = LocalContext.current

    if (MainViewModel.biometricProtected.value && shouldRaiseBiometric) MainViewModel.showBiometricPrompt()

    var pin by remember { mutableStateOf(value = "") }

    var showed by remember { mutableStateOf(value = false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Header(expanded = false, title = context.getString(R.string.authenticate)) {}
        Column(
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it },
                label = { Text(text = "PIN") },
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
                modifier = Modifier.width(width = 200.dp)
            )
            Row {
                if (MainViewModel.biometricProtected.value) {
                    FloatingActionButton(
                        onClick = { MainViewModel.showBiometricPrompt() },
                        backgroundColor = Orange500,
                        modifier = Modifier.padding(bottom = 64.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_round_fingerprint_24),
                            contentDescription = "access",
                            tint = Color.White
                        )
                    }
                    Box(modifier = Modifier.size(size = 16.dp))
                }
                FloatingActionButton(onClick = {
                    if (MainViewModel.checkPin(pin = pin)) {
                        NextcloudApiProvider.attemptLogin()
                        MainViewModel.unlock()
                    } else {
                        MainViewModel.showDialog(
                            title = context.getString(R.string.wrong_pin),
                            body = context.getString(R.string.wrong_pin_body)
                        ) {}
                    }
                }, modifier = Modifier.padding(bottom = 64.dp)) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_round_done_24),
                        contentDescription = "access",
                        tint = Color.White
                    )
                }
            }
        }
    }
}