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
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.api.NextcloudApiProvider
import eu.seldon1000.nextpass.ui.MainViewModel
import eu.seldon1000.nextpass.ui.items.GenericColumnItem
import eu.seldon1000.nextpass.ui.layout.DefaultBottomBar
import eu.seldon1000.nextpass.ui.layout.Header
import eu.seldon1000.nextpass.ui.layout.MyScaffoldLayout
import kotlinx.coroutines.launch

@Composable
fun Settings() {
    val context = LocalContext.current

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
            Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding() + 28.dp)
                .verticalScroll(state = scrollState, enabled = true)
        ) {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                Header(expanded = true, title = context.getString(R.string.settings)) {}
            }
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                GenericColumnItem(
                    title = context.getString(R.string.current_account),
                    body = "${NextcloudApiProvider.getAccountName()}",
                    icon = {}) {
                    MainViewModel.setPrimaryClip(
                        label = context.getString(R.string.current_account),
                        context.getString(R.string.copy_snack_message, NextcloudApiProvider.getAccountName())
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
            GenericColumnItem(
                title = context.getString(R.string.random_password),
                body = context.getString(R.string.random_password_tip),
                icon = {}
            ) {
                coroutineScope.launch {
                    MainViewModel.setPrimaryClip(
                        label = context.getString(R.string.generated_password),
                        clip = NextcloudApiProvider.generatePassword()
                    )
                }
            }
        }
    }
}