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

import androidx.biometric.BiometricManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.api.NextcloudApiProvider
import eu.seldon1000.nextpass.ui.MainViewModel
import eu.seldon1000.nextpass.ui.items.GenericColumnItem
import eu.seldon1000.nextpass.ui.layout.DefaultBottomBar
import eu.seldon1000.nextpass.ui.layout.Header
import eu.seldon1000.nextpass.ui.layout.MyScaffoldLayout
import eu.seldon1000.nextpass.ui.theme.NextcloudBlue
import eu.seldon1000.nextpass.ui.theme.Orange500
import kotlinx.coroutines.launch

@ExperimentalMaterialApi
@Composable
fun Settings() {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val protected by MainViewModel.pinProtected.collectAsState()
    val biometricProtected by MainViewModel.biometricProtected.collectAsState()
    val lockTimeout by MainViewModel.lockTimeout.collectAsState()

    var expanded by remember { mutableStateOf(value = false) }

    val timeoutOptions = listOf<Long>(
        0,
        60000,
        300000,
        600000,
        1800000,
        3600000,
        86400000,
        -1
    )

    MyScaffoldLayout(fab = {
        FloatingActionButton(onClick = { MainViewModel.navigate(route = "about") }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_round_help_outline_24),
                contentDescription = "about",
                tint = Color.White
            )
        }
    }, bottomBar = { DefaultBottomBar(lazyListState = lazyListState) }) { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(
                bottom = paddingValues.calculateBottomPadding() + 48.dp
            ),
            state = lazyListState,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Box(modifier = Modifier.padding(start = 16.dp)) {
                    Header(
                        expanded = true,
                        title = context.getString(R.string.settings)
                    ) {}
                }
            }
            item {
                Text(
                    text = "Account",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NextcloudBlue,
                    modifier = Modifier.padding(start = 16.dp)
                )
                GenericColumnItem(
                    title = context.getString(R.string.current_account),
                    body = "${NextcloudApiProvider.getAccountName()}"
                ) {
                    MainViewModel.setPrimaryClip(
                        label = context.getString(R.string.current_account),
                        context.getString(
                            R.string.copy_snack_message,
                            NextcloudApiProvider.getAccountName()
                        )
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = { NextcloudApiProvider.attemptLogout() },
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text(text = context.getString(R.string.logout))
                    }
                    TextButton(
                        onClick = { NextcloudApiProvider.pickNewAccount() },
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text(text = context.getString(R.string.switch_account))
                    }
                }
            }
            item {
                Text(
                    text = context.getString(R.string.random_password),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NextcloudBlue,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
            item {
                GenericColumnItem(
                    title = context.getString(R.string.random_password),
                    body = context.getString(R.string.random_password_tip)
                ) {
                    coroutineScope.launch {
                        MainViewModel.setPrimaryClip(
                            label = context.getString(R.string.generated_password),
                            clip = NextcloudApiProvider.generatePassword()
                        )
                    }
                }
            }
            item {
                Text(
                    text = context.getString(R.string.security),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NextcloudBlue,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
            item {
                GenericColumnItem(
                    title = context.getString(R.string.pin_protection),
                    body = context.getString(R.string.pin_protection_tip),
                    item = {
                        Checkbox(
                            checked = protected,
                            onCheckedChange = { MainViewModel.navigate(route = "pin/false") },
                            colors = CheckboxDefaults.colors(checkedColor = Orange500),
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                ) {
                    if (protected)
                        MainViewModel.navigate(route = "pin/true")
                    else
                        MainViewModel.showDialog(
                            title = context.getString(R.string.pin_not_enabled),
                            body = context.getString(R.string.pin_not_enabled_body),
                            confirm = true
                        ) { MainViewModel.navigate(route = "pin/false") }
                }
            }
            item {
                GenericColumnItem(
                    title = context.getString(R.string.biometric_protection),
                    body = context.getString(R.string.biometric_protection_tip),
                    item = {
                        Checkbox(
                            checked = biometricProtected && protected,
                            onCheckedChange = {
                                if (protected && !biometricProtected) MainViewModel.enableBiometric()
                                else if (biometricProtected) MainViewModel.disableBiometric()
                            },
                            enabled = protected && BiometricManager.from(context).canAuthenticate(
                                BiometricManager.Authenticators.BIOMETRIC_WEAK
                            ) == BiometricManager.BIOMETRIC_SUCCESS,
                            colors = CheckboxDefaults.colors(checkedColor = Orange500),
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                ) {}
            }
            item {
                Column {
                    GenericColumnItem(
                        title = context.getString(R.string.lock_timeout),
                        body = context.getString(R.string.lock_timeout_tip),
                        item = {}
                    ) {}
                    Card(
                        onClick = { expanded = true },
                        enabled = protected,
                        shape = RoundedCornerShape(size = 8.dp),
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .align(Alignment.End)
                    ) {
                        Row(modifier = Modifier.padding(all = 8.dp)) {
                            Text(
                                text = when (lockTimeout) {
                                    0.toLong() -> context.getString(R.string.immediately)
                                    (-1).toLong() -> context.getString(R.string.never)
                                    else -> when {
                                        lockTimeout < 3600000 -> "${lockTimeout / 60000} minutes"
                                        else -> "${lockTimeout / 3600000} hours"
                                    }
                                },
                                modifier = Modifier.padding(start = 8.dp, end = 16.dp)
                            )
                            Icon(
                                painter = painterResource(
                                    id = if (expanded) R.drawable.ic_round_arrow_drop_up_24
                                    else R.drawable.ic_round_arrow_drop_down_24
                                ),
                                contentDescription = "expand_timeout_menu"
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }) {
                            timeoutOptions.forEach { option ->
                                DropdownMenuItem(onClick = {
                                    MainViewModel.setLockTimeout(timeout = option)
                                    expanded = false
                                }, enabled = lockTimeout != option) {
                                    Text(
                                        text = when (option) {
                                            0.toLong() -> context.getString(R.string.immediately)
                                            (-1).toLong() -> context.getString(R.string.never)
                                            else -> when {
                                                option < 3600000 -> "${option / 60000} minutes"
                                                else -> "${option / 3600000} hours"
                                            }
                                        },
                                        color = if (lockTimeout == option) Color.Gray else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item {
                GenericColumnItem(
                    title = context.getString(R.string.lock_now),
                    body = context.getString(R.string.lock_now_tip)
                ) { if (protected) MainViewModel.lock(shouldRaiseBiometric = false) }
            }
        }
    }
}