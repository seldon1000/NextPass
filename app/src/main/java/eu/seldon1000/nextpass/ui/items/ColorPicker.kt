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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.ui.MainViewModel
import eu.seldon1000.nextpass.ui.layout.SimpleFlowRow

@Composable
fun ColorPicker(onClickAction: (color: Color) -> Unit) {
    var selected by remember { mutableStateOf(value = 0) }

    SimpleFlowRow(
        verticalGap = 8.dp,
        horizontalGap = 8.dp,
        alignment = Alignment.Start,
        modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
    ) {
        MainViewModel.pickerColors.forEachIndexed { index, color ->
            Surface(modifier = Modifier.shadow(elevation = 8.dp, shape = CircleShape)) {
                IconButton(
                    onClick = {
                        selected = index
                        onClickAction(MainViewModel.pickerColors[selected])
                    },
                    modifier = Modifier
                        .size(size = 56.dp)
                        .background(color = color.copy(alpha = 0.75f))
                        .border(width = 3.dp, color = color, shape = CircleShape)
                ) {
                    Crossfade(targetState = selected) { state ->
                        if (state == index) Icon(
                            painter = painterResource(id = R.drawable.ic_round_done_24),
                            contentDescription = "select_color",
                            tint = eu.seldon1000.nextpass.ui.theme.colors!!.onBackground
                        )
                    }
                }
            }
        }
    }
}