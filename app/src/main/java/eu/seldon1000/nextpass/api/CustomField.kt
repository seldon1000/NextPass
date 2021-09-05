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

import androidx.annotation.Keep
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.serialization.Serializable

@Keep
@Serializable
class CustomField(
    @Serializable(with = MutableStateSerializer::class)
    var label: MutableState<String> = mutableStateOf(value = ""),
    @Serializable(with = MutableStateSerializer::class)
    var type: MutableState<String> = mutableStateOf(value = ""),
    @Serializable(with = MutableStateSerializer::class)
    var value: MutableState<String> = mutableStateOf(value = "")
)