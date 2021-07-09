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

import android.graphics.Bitmap
import android.icu.text.SimpleDateFormat
import androidx.annotation.Keep
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.*

@Keep
@Serializable
data class Password(
    val id: String,
    val label: String,
    val url: String,
    val username: String,
    val password: String,
    val notes: String,
    val hash: String,
    val folder: String,

    val created: Long,
    val edited: Long,

    val favorite: Boolean,
    val shared: Boolean,

    val status: Int,

    @Serializable(with = SnapshotListSerializer::class) var tags: SnapshotStateList<Tag>,
    val customFields: String,
) {
    @Contextual
    var index: Int = -1

    @Contextual
    private val formatter = SimpleDateFormat.getDateTimeInstance()

    @Contextual
    val createdDate: String = formatter.format(Date(created * 1000))

    @Contextual
    val editedDate: String = formatter.format(Date(edited * 1000))

    @Contextual
    var customFieldsMap = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }.decodeFromString(
        deserializer = SnapshotListSerializer(CustomField.serializer()),
        string = customFields
    )

    @Contextual
    private val faviconState = MutableStateFlow<@Contextual Bitmap?>(value = null)

    @Contextual
    val favicon = faviconState

    fun setFavicon(bitmap: Bitmap?) {
        faviconState.value = bitmap
    }
}