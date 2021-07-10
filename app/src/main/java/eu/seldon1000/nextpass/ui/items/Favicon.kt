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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.material.placeholder
import com.google.accompanist.placeholder.material.shimmer

@Composable
fun Favicon(favicon: Bitmap?, size: Dp) {
    Surface(
        modifier = Modifier
            .shadow(
                elevation = if (favicon != null) 8.dp else 0.dp,
                RoundedCornerShape(size = 8.dp),
                clip = true
            )
            .placeholder(visible = favicon == null, highlight = PlaceholderHighlight.shimmer())
    ) {
        Crossfade(
            targetState = favicon,
            animationSpec = tween(durationMillis = 300)
        ) {
            Image(
                bitmap = it?.asImageBitmap() ?: ImageBitmap(width = 1, height = 1),
                contentDescription = "favicon",
                alignment = Alignment.Center,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.size(size = size)
            )
        }
    }
}