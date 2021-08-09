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
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import eu.seldon1000.nextpass.MainActivity
import eu.seldon1000.nextpass.MainViewModel
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.ui.items.GenericColumnItem
import eu.seldon1000.nextpass.ui.layout.DefaultBottomBar
import eu.seldon1000.nextpass.ui.layout.Header
import eu.seldon1000.nextpass.ui.layout.MyScaffoldLayout
import eu.seldon1000.nextpass.ui.layout.Routes
import eu.seldon1000.nextpass.ui.theme.NextcloudBlue
import eu.seldon1000.nextpass.ui.theme.Orange500
import eu.seldon1000.nextpass.ui.theme.colors
import kotlinx.coroutines.launch

@ExperimentalMaterialApi
@Composable
fun Settings(viewModel: MainViewModel) {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val protected by viewModel.pinProtected.collectAsState()
    val biometricProtected by viewModel.biometricProtected.collectAsState()
    val lockTimeout by viewModel.lockTimeout.collectAsState()
    val screenProtection by viewModel.screenProtection.collectAsState()
    val autofill by viewModel.autofill.collectAsState()
    val autostart by viewModel.autostart.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val tags by viewModel.tags.collectAsState()

    var selectedLockTimeout by remember { mutableStateOf(value = lockTimeout) }

    val timeoutOptions = listOf<Long>(
        0,
        60000,
        300000,
        600000,
        1800000,
        3600000,
        86400000,
        -2,
        -1
    )

    MyScaffoldLayout(
        fab = {
            FloatingActionButton(onClick = { viewModel.navigate(route = Routes.About.route) }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_round_help_outline_24),
                    contentDescription = "about",
                    tint = colors!!.onBackground
                )
            }
        },
        bottomBar = {
            DefaultBottomBar(
                lazyListState = lazyListState,
                viewModel = viewModel
            )
        }) { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(
                bottom = paddingValues.calculateBottomPadding() + 48.dp
            ),
            state = lazyListState,
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Box(modifier = Modifier.padding(start = 16.dp)) {
                    Header(expanded = true, title = context.getString(R.string.settings))
                }
            }
            item {
                Text(
                    text = context.getString(R.string.account),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NextcloudBlue,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp)
                )
                GenericColumnItem(
                    title = context.getString(R.string.current_account),
                    body = viewModel.nextcloudApi.getCurrentAccount()
                ) {
                    viewModel.setPrimaryClip(
                        label = context.getString(R.string.current_account),
                        clip = viewModel.nextcloudApi.getCurrentAccount()
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = { viewModel.attemptLogout() },
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text(text = context.getString(R.string.logout))
                    }
                    TextButton(
                        onClick = { viewModel.attemptLogin() },
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
                    title = context.getString(R.string.generate_random_password),
                    body = context.getString(R.string.random_password_tip)
                ) {
                    viewModel.executeRequest {
                        coroutineScope.launch {
                            viewModel.setPrimaryClip(
                                label = context.getString(R.string.generated_password),
                                clip = viewModel.nextcloudApi.generatePassword()
                            )
                        }
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
                    title = context.getString(R.string.pin_authentication),
                    body = context.getString(R.string.pin_authentication_tip),
                    item = {
                        Switch(
                            checked = protected,
                            onCheckedChange = {
                                if (protected) {
                                    viewModel.showDialog(
                                        title = context.getString(R.string.disable_pin),
                                        body = {
                                            Text(
                                                text = context.getString(R.string.disable_pin_body)
                                            )
                                        },
                                        confirm = true
                                    ) { viewModel.disablePin() }
                                } else viewModel.navigate(route = Routes.Pin.route)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Orange500),
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                ) { viewModel.changePin() }
            }
            item {
                GenericColumnItem(
                    title = context.getString(R.string.biometric_authentication),
                    body = context.getString(R.string.biometric_authentication_tip),
                    item = {
                        Switch(
                            checked = biometricProtected && protected,
                            onCheckedChange = {
                                if (protected && !biometricProtected) viewModel.enableBiometric()
                                else if (biometricProtected) viewModel.disableBiometric()
                            },
                            enabled = protected && BiometricManager.from(context).canAuthenticate(
                                BiometricManager.Authenticators.BIOMETRIC_WEAK
                            ) == BiometricManager.BIOMETRIC_SUCCESS,
                            colors = SwitchDefaults.colors(checkedThumbColor = Orange500),
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                ) {}
            }
            item {
                Column {
                    GenericColumnItem(
                        title = context.getString(R.string.lock_timeout),
                        body = context.getString(
                            R.string.lock_timeout_tip, when {
                                lockTimeout == 0.toLong() -> context.getString(R.string.immediately)
                                lockTimeout == (-1).toLong() -> context.getString(R.string.never)
                                lockTimeout == (-2).toLong() -> context.getString(R.string.on_restart)
                                lockTimeout < 3600000 -> context.resources.getQuantityString(
                                    R.plurals.minutes,
                                    (lockTimeout / 60000).toInt(),
                                    (lockTimeout / 60000).toInt()
                                )
                                else -> context.resources.getQuantityString(
                                    R.plurals.hours,
                                    (lockTimeout / 3600000).toInt(),
                                    (lockTimeout / 3600000).toInt()
                                )
                            }
                        ),
                        item = {}
                    ) {
                        viewModel.showDialog(
                            title = context.getString(R.string.choose_lock_timeout),
                            body = {
                                Column {
                                    timeoutOptions.forEach { option ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedLockTimeout = option }) {
                                            RadioButton(
                                                selected = (lockTimeout == option &&
                                                        lockTimeout == selectedLockTimeout) ||
                                                        (selectedLockTimeout == option &&
                                                                lockTimeout != selectedLockTimeout),
                                                onClick = { selectedLockTimeout = option },
                                                modifier = Modifier.padding(
                                                    horizontal = 16.dp,
                                                    vertical = 10.dp
                                                )
                                            )
                                            Text(
                                                text = when {
                                                    option == 0.toLong() -> context.getString(R.string.immediately)
                                                    option == (-1).toLong() -> context.getString(R.string.never)
                                                    option == (-2).toLong() -> context.getString(R.string.on_restart)
                                                    option < 3600000 -> context.resources.getQuantityString(
                                                        R.plurals.minutes,
                                                        (option / 60000).toInt(),
                                                        (option / 60000).toInt()
                                                    )
                                                    else -> context.resources.getQuantityString(
                                                        R.plurals.hours,
                                                        (option / 3600000).toInt(),
                                                        (option / 3600000).toInt()
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            },
                            confirm = true
                        ) { viewModel.setLockTimeout(timeout = selectedLockTimeout) }
                    }
                }
            }
            item {
                GenericColumnItem(
                    title = context.getString(R.string.lock_now),
                    body = context.getString(R.string.lock_now_tip)
                ) { viewModel.lock(shouldRaiseBiometric = false) }
            }
            item {
                GenericColumnItem(
                    title = context.getString(R.string.prevent_screen_capture),
                    body = context.getString(R.string.prevent_screen_capture_tip),
                    item = {
                        Switch(
                            checked = screenProtection,
                            onCheckedChange = {
                                if (screenProtection) viewModel.disableScreenProtection()
                                else viewModel.enableScreenProtection()
                            },
                            enabled = true,
                            colors = SwitchDefaults.colors(checkedThumbColor = Orange500),
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                ) {}
            }
            item {
                Text(
                    text = context.getString(R.string.autofill),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NextcloudBlue,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
            item {
                GenericColumnItem(
                    title = context.getString(R.string.autofill_service),
                    body = context.getString(R.string.autofill_service_tip),
                    item = {
                        Switch(
                            checked = autofill,
                            onCheckedChange = {
                                if (autofill) viewModel.disableAutofill()
                                else {
                                    viewModel.showDialog(
                                        title = context.getString(R.string.autofill_title),
                                        body = {
                                            Text(
                                                text = context.getString(R.string.autofill_body),
                                                fontSize = 14.sp
                                            )
                                        },
                                        confirm = true
                                    ) {
                                        (context as MainActivity).autofillSettingsResult.launch(
                                            Intent(
                                                Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE,
                                                Uri.parse("package:eu.seldon1000.nextpass")
                                            )
                                        )
                                    }
                                }
                            },
                            enabled = true,
                            colors = SwitchDefaults.colors(checkedThumbColor = Orange500),
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                ) {}
            }
            item {
                GenericColumnItem(
                    title = context.getString(R.string.autostart_service),
                    body = context.getString(R.string.autostart_service_tip),
                    item = {
                        Switch(
                            checked = autostart,
                            onCheckedChange = {
                                if (autostart) viewModel.disableAutostart()
                                else viewModel.enableAutostart()
                            },
                            enabled = autofill,
                            colors = SwitchDefaults.colors(checkedThumbColor = Orange500),
                            modifier = Modifier.padding(all = 16.dp)
                        )
                    }
                ) {}
            }
            item {
                GenericColumnItem(
                    title = context.getString(R.string.stop_service_now),
                    body = context.getString(R.string.stop_service_now_tip),
                    item = {}
                ) { viewModel.stopAutofillService() }
            }
            item {
                Text(
                    text = context.getString(R.string.appearance),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NextcloudBlue,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
            item {
                GenericColumnItem(
                    title = context.getString(R.string.always_folder_mode),
                    body = context.getString(R.string.always_folder_mode_tip),
                    item = {
                        Switch(
                            checked = folders,
                            onCheckedChange = {
                                if (folders) viewModel.disableFolders()
                                else viewModel.enableFolders()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Orange500),
                            modifier = Modifier.padding(all = 16.dp)
                        )
                    }
                ) {}
            }
            item {
                GenericColumnItem(
                    title = context.getString(R.string.always_show_tags),
                    body = context.getString(R.string.always_show_tags_tip),
                    item = {
                        Switch(
                            checked = tags,
                            onCheckedChange = {
                                if (tags) viewModel.disableTags()
                                else viewModel.enableTags()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Orange500),
                            modifier = Modifier.padding(all = 16.dp)
                        )
                    }
                ) {}
            }
            item {
                Text(
                    text = context.getString(R.string.danger_zone),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Red,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
            item {
                GenericColumnItem(
                    title = context.getString(R.string.reset_preferences),
                    body = context.getString(R.string.reset_preferences_tip),
                    titleColor = Color.Red,
                    item = {}
                ) {
                    viewModel.showDialog(
                        title = context.getString(R.string.warning),
                        body = { Text(text = context.getString(R.string.reset_preferences_body)) },
                        confirm = true
                    ) { viewModel.resetUserPreferences() }
                }
            }
        }
    }
}