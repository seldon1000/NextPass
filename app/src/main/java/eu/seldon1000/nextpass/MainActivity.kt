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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Surface
import androidx.compose.runtime.rememberCoroutineScope
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import eu.seldon1000.nextpass.ui.layout.CentralScreenControl
import eu.seldon1000.nextpass.ui.theme.NextPassTheme
import kotlinx.coroutines.*

class MainActivity : FragmentActivity() {
    private val coroutineScope = MainScope()

    private lateinit var viewModel: MainViewModel
    lateinit var autofillSettingsResult: ActivityResultLauncher<Intent>

    @OptIn(
        ExperimentalAnimationApi::class,
        ExperimentalFoundationApi::class,
        ExperimentalMaterialApi::class
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel =
            ViewModelProvider(this).get(MainViewModel(application = application)::class.java)

        autofillSettingsResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == Activity.RESULT_OK) viewModel.enableAutofill()
            }

        setContent {
            rememberCoroutineScope().launch { viewModel.unlock() }

            NextPassTheme {
                Surface {
                    CentralScreenControl(viewModel = viewModel)
                }
            }
        }
    }

    override fun onRestart() {
        super.onRestart()

        coroutineScope.coroutineContext.cancelChildren()
    }

    override fun onStop() {
        super.onStop()

        coroutineScope.launch {
            if (viewModel.lockTimeout.value != (-1).toLong() &&
                viewModel.lockTimeout.value != (-2).toLong()
            ) {
                delay(viewModel.lockTimeout.value)
                viewModel.lock()
            }
        }
    }

    override fun onBackPressed() {
        if (!viewModel.popBackStack()) {
            coroutineScope.cancel()

            finish()
        }
    }
}