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

import androidx.compose.animation.animateColorAsState
import androidx.compose.material.Icon
import androidx.compose.material.IconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import eu.seldon1000.nextpass.R

@Composable
fun FavoriteIcon(favorite: Boolean, action: (Boolean) -> Unit) {
    IconToggleButton(checked = favorite, onCheckedChange = action) {
        val tint by animateColorAsState(if (favorite) Color.Yellow else Color.White)

        Icon(
            painter = if (favorite) painterResource(id = R.drawable.ic_round_star_yellow_24)
            else painterResource(id = R.drawable.ic_round_star_border_24),
            contentDescription = "favorite",
            tint = tint
        )
    }
}