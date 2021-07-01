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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Surface
import androidx.compose.runtime.rememberCoroutineScope
import androidx.fragment.app.FragmentActivity
import eu.seldon1000.nextpass.api.NextcloudApiProvider
import eu.seldon1000.nextpass.ui.MainViewModel
import eu.seldon1000.nextpass.ui.layout.CentralScreenControl
import eu.seldon1000.nextpass.ui.theme.NextPassTheme
import kotlinx.coroutines.*

const val AUTOFILL_SETTINGS_CODE = 7799

class MainActivity : FragmentActivity() {
    private val coroutineScope = MainScope()

    @ExperimentalMaterialApi
    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MainViewModel.setContext(context = this)

        setContent {
            rememberCoroutineScope().launch { MainViewModel.openApp() }

            NextPassTheme {
                Surface {
                    CentralScreenControl()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == AUTOFILL_SETTINGS_CODE && resultCode == Activity.RESULT_OK)
            MainViewModel.enableAutofill()
        else NextcloudApiProvider.handleAccountImporterResponse(
            requestCode = requestCode,
            resultCode = resultCode,
            data = data
        )
    }

    override fun onRestart() {
        super.onRestart()

        coroutineScope.coroutineContext.cancelChildren()
    }

    override fun onStop() {
        super.onStop()

        coroutineScope.launch {
            if (!MainViewModel.unlocked.value) MainViewModel.lock()
            else if (MainViewModel.lockTimeout.value == (-1).toLong() ||
                MainViewModel.lockTimeout.value == (-2).toLong()
            ) return@launch
            else {
                delay(MainViewModel.lockTimeout.value)
                MainViewModel.lock()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        coroutineScope.cancel()
        if (!MainViewModel.autofill.value) NextcloudApiProvider.stopNextcloudApi()
    }

    override fun onBackPressed() {
        if (!MainViewModel.popBackStack()) finish()
    }
}