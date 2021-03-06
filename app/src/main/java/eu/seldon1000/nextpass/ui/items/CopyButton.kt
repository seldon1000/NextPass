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

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import eu.seldon1000.nextpass.MainViewModel
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.ui.theme.colors

@Composable
fun CopyButton(label: String, clip: String, viewModel: MainViewModel) {
    IconButton(onClick = { viewModel.setPrimaryClip(label = label, clip = clip) }) {
        Icon(
            painter = painterResource(id = R.drawable.ic_round_content_copy_24),
            contentDescription = "copy",
            tint = colors!!.onBackground
        )
    }
}