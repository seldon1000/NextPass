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

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

class MutableStateSerializer<T>(private val dataSerializer: KSerializer<T>) :
    KSerializer<MutableState<T>> {
    override val descriptor: SerialDescriptor = dataSerializer.descriptor

    override fun deserialize(decoder: Decoder): MutableState<T> =
        mutableStateOf(dataSerializer.deserialize(decoder))

    override fun serialize(encoder: Encoder, value: MutableState<T>) =
        dataSerializer.serialize(encoder, value.value)
}