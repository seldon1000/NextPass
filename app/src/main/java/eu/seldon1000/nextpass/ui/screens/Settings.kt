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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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

@Composable
fun Settings() {
    val context = LocalContext.current

    val protected by MainViewModel.pinProtected.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    val scrollState = rememberScrollState()

    MyScaffoldLayout(fab = {
        FloatingActionButton(onClick = { MainViewModel.navigate(route = "about") }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_round_help_outline_24),
                contentDescription = "about",
                tint = Color.White
            )
        }
    }, bottomBar = { DefaultBottomBar() }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = scrollState, enabled = true)
        ) {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                Header(expanded = true, title = context.getString(R.string.settings)) {}
            }
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    TextButton(
                        onClick = { NextcloudApiProvider.attemptLogout() },
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text(text = context.getString(R.string.logout))
                    }
                    TextButton(onClick = { NextcloudApiProvider.pickNewAccount() }) {
                        Text(text = context.getString(R.string.switch_account))
                    }
                }
            }
            Text(
                text = context.getString(R.string.random_password),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = NextcloudBlue,
                modifier = Modifier.padding(start = 16.dp)
            )
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
            Text(
                text = context.getString(R.string.security),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = NextcloudBlue,
                modifier = Modifier.padding(start = 16.dp)
            )
            GenericColumnItem(
                title = context.getString(R.string.pin_protection),
                body = context.getString(R.string.pin_protection_tip),
                switch = {
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
            GenericColumnItem(
                title = context.getString(R.string.biometric_protection),
                body = context.getString(R.string.biometric_protection_tip),
                switch = {
                    Checkbox(
                        checked = false,
                        onCheckedChange = {
                            if (protected) else MainViewModel.showDialog(
                                title = context.getString(R.string.enable_pin),
                                body = context.getString(R.string.enable_pin_body)
                            ) {}
                        },
                        enabled = false,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            ) { MainViewModel.showSnackbar(message = "Work in progress...") }
            GenericColumnItem(
                title = context.getString(R.string.lock_now),
                body = context.getString(R.string.lock_now_tip)
            ) {
                if (protected) {
                    MainViewModel.setUnlock(unlock = false)
                    NextcloudApiProvider.stopNextcloudApi()
                    MainViewModel.navigate(route = "access_pin")
                }
            }
            Box(modifier = Modifier.size(size = paddingValues.calculateBottomPadding() + 48.dp))
        }
    }
}