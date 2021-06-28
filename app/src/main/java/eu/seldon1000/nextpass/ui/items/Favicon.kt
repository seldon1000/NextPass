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

package eu.seldon1000.nextpass.ui.items

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import coil.transform.RoundedCornersTransformation
import com.google.accompanist.coil.rememberCoilPainter

@Composable
fun Favicon(favicon: Bitmap?, size: Dp) {
    Crossfade(
        targetState = favicon,
        animationSpec = tween(durationMillis = 300)
    ) {
        Image(
            painter = rememberCoilPainter(
                request = it,
                requestBuilder = { transformations(RoundedCornersTransformation(radius = 16F)) }
            ),
            alignment = Alignment.Center,
            contentDescription = "favicon",
            modifier = Modifier.size(size = size)
        )
    }
}