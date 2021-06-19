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
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.rememberCoroutineScope
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewModelScope
import eu.seldon1000.nextpass.api.NextcloudApiProvider
import eu.seldon1000.nextpass.ui.MainViewModel
import eu.seldon1000.nextpass.ui.layout.CentralScreenControl
import eu.seldon1000.nextpass.ui.theme.NextPassTheme
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    @ExperimentalMaterialApi
    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NextcloudApiProvider.setContext(this)
        MainViewModel.setContext(con = this)
        MainViewModel.setClipboardManager(manager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)

        setContent {
            if (MainViewModel.unlocked.value) rememberCoroutineScope().launch { NextcloudApiProvider.attemptLogin() }
            else rememberCoroutineScope().launch { MainViewModel.navigate(route = "access_pin/true") }

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

    override fun onPause() {
        MainViewModel.setUnlock(unlock = false)

        super.onPause()
    }

    override fun onDestroy() {
        MainViewModel.setUnlock(unlock = false)
        MainViewModel.setRefreshing(refreshing = false)
        NextcloudApiProvider.viewModelScope.cancel()
        NextcloudApiProvider.stopNextcloudApi()

        super.onDestroy()
    }

    override fun onBackPressed() {
        if (!MainViewModel.popBackStack()) finish()
    }
}