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
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.seldon1000.nextpass.CentralAppControl
import eu.seldon1000.nextpass.MainActivity
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.api.NextcloudApi
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
fun Settings() {
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val protected by CentralAppControl.pinProtected.collectAsState()
    val biometricProtected by CentralAppControl.biometricProtected.collectAsState()
    val lockTimeout by CentralAppControl.lockTimeout.collectAsState()
    val screenProtection by CentralAppControl.screenProtection.collectAsState()
    val autofill by CentralAppControl.autofill.collectAsState()
    val autostart by CentralAppControl.autostart.collectAsState()
    val folders by CentralAppControl.folders.collectAsState()
    val tags by CentralAppControl.tags.collectAsState()

    var expanded by remember { mutableStateOf(value = false) }

    var isRotated by remember { mutableStateOf(value = false) }
    val angle by animateFloatAsState(
        targetValue = if (isRotated) 180F else 0F,
        animationSpec = tween(
            durationMillis = 500,
            easing = FastOutSlowInEasing
        )
    )

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

    MyScaffoldLayout(fab = {
        FloatingActionButton(onClick = { CentralAppControl.navigate(route = Routes.About.route) }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_round_help_outline_24),
                contentDescription = "about",
                tint = colors!!.onBackground
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
                    body = NextcloudApi.getCurrentAccount()
                ) {
                    CentralAppControl.setPrimaryClip(
                        label = context.getString(R.string.current_account),
                        clip = NextcloudApi.getCurrentAccount()
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = { CentralAppControl.attemptLogout() },
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text(text = context.getString(R.string.logout))
                    }
                    TextButton(
                        onClick = { CentralAppControl.attemptLogin() },
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
                    CentralAppControl.executeRequest {
                        coroutineScope.launch {
                            CentralAppControl.setPrimaryClip(
                                label = context.getString(R.string.generated_password),
                                clip = NextcloudApi.generatePassword()
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
                                    CentralAppControl.showDialog(
                                        title = context.getString(R.string.disable_pin),
                                        body = {
                                            Text(
                                                text = context.getString(R.string.disable_pin_body)
                                            )
                                        },
                                        confirm = true) { CentralAppControl.disablePin() }
                                } else CentralAppControl.navigate(route = Routes.Pin.route)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Orange500),
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                ) {
                    if (protected) CentralAppControl.changePin()
                    else
                        CentralAppControl.showDialog(
                            title = context.getString(R.string.pin_not_enabled),
                            body = {
                                Text(
                                    text = context.getString(R.string.pin_not_enabled_body),
                                    fontSize = 14.sp
                                )
                            },
                            confirm = true
                        ) { CentralAppControl.navigate(route = Routes.Pin.route) }
                }
            }
            item {
                GenericColumnItem(
                    title = context.getString(R.string.biometric_authentication),
                    body = context.getString(R.string.biometric_authentication_tip),
                    item = {
                        Switch(
                            checked = biometricProtected && protected,
                            onCheckedChange = {
                                if (protected && !biometricProtected) CentralAppControl.enableBiometric()
                                else if (biometricProtected) CentralAppControl.disableBiometric()
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
                        body = context.getString(R.string.lock_timeout_tip),
                        item = {}
                    ) {}
                    Surface(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .align(alignment = Alignment.End)
                            .shadow(
                                elevation = if (protected) 8.dp else Dp.Unspecified,
                                RoundedCornerShape(size = 8.dp),
                                clip = true
                            )
                    ) {
                        Card(
                            onClick = {
                                isRotated = !isRotated
                                expanded = true
                            },
                            enabled = protected,
                            shape = RoundedCornerShape(size = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(all = 8.dp)
                                    .animateContentSize(
                                        animationSpec = tween(
                                            durationMillis = 200,
                                            easing = FastOutSlowInEasing
                                        )
                                    )
                            ) {
                                Text(
                                    text = when (lockTimeout) {
                                        0.toLong() -> context.getString(R.string.immediately)
                                        (-1).toLong() -> context.getString(R.string.never)
                                        (-2).toLong() -> context.getString(R.string.on_restart)
                                        else -> when {
                                            lockTimeout < 3600000 -> context.resources.getQuantityString(
                                                R.plurals.minutes,
                                                (lockTimeout / 60000).toInt(),
                                                (lockTimeout / 60000).toInt()
                                            )
                                            else -> context.resources.getQuantityString(
                                                R.plurals.minutes,
                                                (lockTimeout / 60000).toInt(),
                                                (lockTimeout / 3600000).toInt()
                                            )
                                        }
                                    },
                                    modifier = Modifier.padding(start = 8.dp, end = 16.dp)
                                )

                                Icon(
                                    painter = painterResource(id = R.drawable.ic_round_arrow_drop_up_24),
                                    contentDescription = "expand_timeout_menu",
                                    modifier = Modifier.rotate(degrees = angle)
                                )
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = {
                                    expanded = false
                                    isRotated = !isRotated
                                }) {
                                timeoutOptions.forEach { option ->
                                    DropdownMenuItem(onClick = {
                                        isRotated = !isRotated
                                        expanded = false

                                        CentralAppControl.setLockTimeout(timeout = option)
                                    }, enabled = lockTimeout != option) {
                                        Text(
                                            text = when (option) {
                                                0.toLong() -> context.getString(R.string.immediately)
                                                (-1).toLong() -> context.getString(R.string.never)
                                                (-2).toLong() -> context.getString(R.string.on_restart)
                                                else -> when {
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
                                            },
                                            color = if (lockTimeout == option) Color.Gray else Color.White
                                        )
                                    }
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
                ) { CentralAppControl.lock(shouldRaiseBiometric = false) }
            }
            item {
                GenericColumnItem(
                    title = context.getString(R.string.prevent_screen_capture),
                    body = context.getString(R.string.prevent_screen_capture_tip),
                    item = {
                        Switch(
                            checked = screenProtection,
                            onCheckedChange = {
                                if (screenProtection) CentralAppControl.disableScreenProtection()
                                else CentralAppControl.enableScreenProtection()
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
                                if (autofill) CentralAppControl.disableAutofill()
                                else {
                                    CentralAppControl.showDialog(
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
                                if (autostart) CentralAppControl.disableAutostart()
                                else CentralAppControl.enableAutostart()
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
                ) { CentralAppControl.stopAutofillService() }
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
                                if (folders) CentralAppControl.disableFolders()
                                else CentralAppControl.enableFolders()
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
                                if (tags) CentralAppControl.disableTags()
                                else CentralAppControl.enableTags()
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
                    CentralAppControl.showDialog(
                        title = context.getString(R.string.warning),
                        body = { Text(text = context.getString(R.string.reset_preferences_body)) },
                        confirm = true
                    ) { CentralAppControl.resetUserPreferences(context = context) }
                }
            }
        }
    }
}