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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@ExperimentalMaterialApi
@Composable
fun TextFieldItem(
    text: String,
    onTextChanged: (String) -> Unit,
    label: String,
    enabled: Boolean = true,
    required: Boolean = false,
    capitalized: Boolean = false,
    protected: Boolean = false,
    showed: Boolean = true,
    trailingIcon: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .shadow(elevation = 8.dp, RoundedCornerShape(size = 8.dp), clip = true)
    ) {
        Crossfade(targetState = enabled) { enabledState ->
            TextField(
                value = text,
                onValueChange = onTextChanged,
                enabled = enabledState,
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = label)
                        Crossfade(targetState = required && text.isEmpty()) { state ->
                            BadgeBox(
                                backgroundColor = if (state) Color(0xFFB85F6F) else Color.Transparent,
                                modifier = Modifier.padding(start = 8.dp)
                            ) {}
                        }
                    }
                },
                shape = RoundedCornerShape(size = 8.dp),
                isError = required && text.isEmpty(),
                trailingIcon = { Row { trailingIcon() } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (protected) {
                        if (label == "PIN") KeyboardType.NumberPassword
                        else KeyboardType.Password
                    } else KeyboardType.Text,
                    capitalization = if (capitalized) KeyboardCapitalization.Sentences else KeyboardCapitalization.None
                ),
                visualTransformation = if (showed) VisualTransformation.None else PasswordVisualTransformation(),
                colors = TextFieldDefaults.textFieldColors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}