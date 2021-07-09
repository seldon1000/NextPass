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

package eu.seldon1000.nextpass.api

import android.os.Bundle
import android.os.PersistableBundle
import android.webkit.WebView
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Surface
import androidx.fragment.app.FragmentActivity
import eu.seldon1000.nextpass.ui.layout.CentralScreenControl
import eu.seldon1000.nextpass.ui.screens.WebPageVisualizer
import eu.seldon1000.nextpass.ui.theme.NextPassTheme

class NextcloudWebAccessActivity : FragmentActivity() {
    @ExperimentalMaterialApi
    @ExperimentalAnimationApi
    @ExperimentalFoundationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.getStringExtra("url")!!

        setContent {
            NextPassTheme {
                WebPageVisualizer(urlToRender = url)
            }
        }
    }
}