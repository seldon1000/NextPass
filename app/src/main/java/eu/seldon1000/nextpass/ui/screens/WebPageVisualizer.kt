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

import android.annotation.SuppressLint
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.BottomAppBar
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.MainViewModel
import eu.seldon1000.nextpass.ui.layout.MyScaffoldLayout
import eu.seldon1000.nextpass.ui.theme.colors

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebPageVisualizer(urlToRender: String, viewModel: MainViewModel) {
    val context = LocalContext.current

    val webView by remember {
        mutableStateOf(value = WebView(context).apply {
            settings.safeBrowsingEnabled = true
            settings.javaScriptEnabled = true
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            webViewClient = WebViewClient()
            loadUrl(urlToRender)
        })
    }

    var rotation by remember { mutableStateOf(value = 0F) }
    val angle by animateFloatAsState(
        targetValue = rotation,
        animationSpec = tween(
            durationMillis = 1000,
            easing = LinearOutSlowInEasing
        )
    )

    MyScaffoldLayout(fab = {
        FloatingActionButton(onClick = {
            webView.stopLoading()
            webView.loadUrl(urlToRender)

            if (rotation >= 360F * 10) {
                rotation = 0F

                viewModel.showSnackbar(message = context.getString(R.string.refresh_easter_egg))
            } else rotation += 360F
        }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_round_refresh_24),
                contentDescription = "refresh",
                tint = colors!!.onBackground,
                modifier = Modifier.rotate(degrees = angle)
            )
        }
    }, bottomBar = {
        BottomAppBar(
            backgroundColor = Color.Black,
            cutoutShape = CircleShape,
            modifier = Modifier.clip(shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
        ) {
            IconButton(onClick = {
                if (webView.canGoBack()) webView.goBack()
                else viewModel.popBackStack()
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_round_back_arrow_24),
                    contentDescription = "back"
                )
            }
        }
    }) {
        AndroidView(
            factory = { webView },
            modifier = Modifier.fillMaxSize()
        )
    }
}