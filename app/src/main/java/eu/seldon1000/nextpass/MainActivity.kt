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

package eu.seldon1000.nextpass

import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.rememberNavController
import eu.seldon1000.nextpass.api.NextcloudApiProvider
import eu.seldon1000.nextpass.ui.MainViewModel
import eu.seldon1000.nextpass.ui.layout.CentralScreenControl
import eu.seldon1000.nextpass.ui.theme.NextPassTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @ExperimentalMaterialApi
    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NextcloudApiProvider.setContext(this)

        MainViewModel.setClipboardManager(manager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)

        setContent {
            val coroutineScope = rememberCoroutineScope()

            MainViewModel.setNavController(controller = rememberNavController())

            coroutineScope.launch { NextcloudApiProvider.attemptLogin() }

            NextPassTheme {
                Surface(color = MaterialTheme.colors.background) {
                    CentralScreenControl()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        NextcloudApiProvider.handleAccountImporterResponse(
            requestCode = requestCode,
            resultCode = resultCode,
            data = data
        )
    }

    override fun onBackPressed() {
        if (!MainViewModel.popBackStack()) {
            NextcloudApiProvider.stopNextcloudApi()
            finish()
        }
    }
}