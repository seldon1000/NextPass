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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class Password(
    val favorite: Boolean,
    val shared: Boolean,

    val id: String,
    val label: String,
    val url: String,
    val username: String,
    val password: String,
    val notes: String,
    val hash: String,
    val folder: String,
    val statusCode: String,

    @Serializable(with = SnapshotListSerializer::class) var tags: SnapshotStateList<Tag> = mutableStateListOf(),
    val customFields: String,

    val status: Int,

    private val created: Long,
    private val updated: Long
) {
    @Contextual
    private val formatter = SimpleDateFormat.getDateTimeInstance()

    @Contextual
    val createdDate: String = formatter.format(Date(created * 1000))

    @Contextual
    val updatedDate: String = formatter.format(Date(updated * 1000))

    @Contextual
    var customFieldsList = try {
        NextcloudApi.json.decodeFromString(
            deserializer = SnapshotListSerializer(CustomField.serializer()),
            string = customFields
        )
    } catch (e: Exception) {
        mutableStateListOf()
    }

    @Contextual
    private val faviconState = MutableStateFlow<@Contextual Bitmap?>(value = null)

    @Contextual
    val favicon = faviconState

    fun setFavicon(bitmap: Bitmap?) {
        faviconState.value = bitmap
    }

    fun resetCustomFields() {
        customFieldsList = try {
            NextcloudApi.json.decodeFromString(
                deserializer = SnapshotListSerializer(CustomField.serializer()),
                string = customFields
            )
        } catch (e: Exception) {
            mutableStateListOf()
        }
    }
}