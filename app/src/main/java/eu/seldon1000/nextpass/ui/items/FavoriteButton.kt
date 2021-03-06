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

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconToggleButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import eu.seldon1000.nextpass.R

@Composable
fun FavoriteButton(favorite: Boolean, action: (Boolean) -> Unit) {
    var isRotated by remember { mutableStateOf(value = false) }
    val angle by animateFloatAsState(
        targetValue = if (isRotated) 360F else 0F,
        animationSpec = tween(
            durationMillis = 1000,
            easing = LinearOutSlowInEasing
        )
    )

    IconToggleButton(checked = favorite, onCheckedChange = {
        action(it)
        isRotated = !isRotated
    }) {
        val tint by animateColorAsState(
            targetValue = if (favorite) Color.Yellow else Color.White,
            animationSpec = tween(durationMillis = 200, easing = LinearEasing)
        )

        Crossfade(
            targetState = favorite, modifier = Modifier.shadow(
                elevation = 8.dp,
                RoundedCornerShape(size = 8.dp),
                clip = true
            )
        ) { state ->
            Icon(
                painter = if (state) painterResource(id = R.drawable.ic_round_star_24)
                else painterResource(id = R.drawable.ic_round_star_border_24),
                contentDescription = "favorite",
                tint = tint,
                modifier = Modifier.rotate(degrees = angle)
            )
        }
    }
}