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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import eu.seldon1000.nextpass.MainViewModel
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.ui.items.TextFieldItem
import eu.seldon1000.nextpass.ui.layout.Header
import eu.seldon1000.nextpass.ui.theme.Orange500
import eu.seldon1000.nextpass.ui.theme.colors

@ExperimentalMaterialApi
@Composable
fun AccessPin(shouldRaiseBiometric: Boolean, viewModel: MainViewModel) {
    val context = LocalContext.current

    val biometricDismissed by viewModel.biometricDismissed.collectAsState()

    if (viewModel.biometricProtected.value && shouldRaiseBiometric && !biometricDismissed)
        viewModel.showBiometricPrompt()

    var showed by remember { mutableStateOf(value = false) }
    var pin by remember { mutableStateOf(value = "") }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Header(expanded = false, title = context.getString(R.string.authenticate))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .width(width = 176.dp)
                    .padding(top = 200.dp, bottom = 100.dp)
            ) {
                TextFieldItem(
                    text = pin,
                    onTextChanged = { pin = it },
                    label = "PIN",
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
            Row {
                if (viewModel.biometricProtected.value) {
                    FloatingActionButton(
                        onClick = { viewModel.showBiometricPrompt() },
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
                FloatingActionButton(
                    onClick = { viewModel.checkPin(pin = pin) },
                    modifier = Modifier.padding(bottom = 64.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_round_arrow_forward_24),
                        contentDescription = "access",
                        tint = colors!!.onBackground
                    )
                }
            }
        }
    }
}