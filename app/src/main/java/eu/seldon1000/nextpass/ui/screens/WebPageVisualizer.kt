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

import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.ui.layout.Header

@Composable
fun WebPageVisualizer(urlToRender: String) {
    val context = LocalContext.current

    Column {
        Header(expanded = false, title = context.getString(R.string.login))
        Surface(
            shape = RoundedCornerShape(size = 16.dp),
            elevation = 8.dp,
            modifier = Modifier.padding(all = 16.dp)
        ) {
            AndroidView(
                factory = {
                    WebView(it).apply {
                        settings.safeBrowsingEnabled = true
                        settings.javaScriptEnabled = true
                        settings.cacheMode = WebSettings.LOAD_NO_CACHE
                        webViewClient = WebViewClient()
                        loadUrl(urlToRender)
                    }
                }, modifier = Modifier.fillMaxSize()
            )
        }
    }
}