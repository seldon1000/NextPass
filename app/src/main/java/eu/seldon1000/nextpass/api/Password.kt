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
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.*

data class Password(val passwordData: JsonObject, var index: Int = -1) {
    private val typeToken =
        object : TypeToken<SnapshotStateList<Tag>>() {}.type
    private val formatter = SimpleDateFormat.getDateTimeInstance()

    val id: String = passwordData.get("id").asString
    val label: String = passwordData.get("label").asString
    val url: String = passwordData.get("url").asString
    val username: String = passwordData.get("username").asString
    val password: String = passwordData.get("password").asString
    val notes: String = passwordData.get("notes").asString
    val hash: String = passwordData.get("hash").asString
    val folder: String = passwordData.get("folder").asString
    var tags: SnapshotStateList<Tag> =
        try {
            Gson().fromJson(passwordData.get("tags").asJsonArray, typeToken)
        } catch (e: Exception) {
            mutableStateListOf()
        }
    var customFields: SnapshotStateList<SnapshotStateMap<String, String>> =
        try {
            Gson().fromJson(passwordData.get("customFields").asString, typeToken)
        } catch (e: Exception) {
            mutableStateListOf()
        }

    val created: String = formatter.format(Date(passwordData.get("created").asLong * 1000))
    val edited: String = formatter.format(Date(passwordData.get("edited").asLong * 1000))

    val favorite: Boolean = passwordData.get("favorite").asBoolean
    val shared: Boolean = passwordData.get("shared").asBoolean

    val status: Int = passwordData.get("status").asInt

    private val faviconState = MutableStateFlow<Bitmap?>(value = null)
    val favicon = faviconState

    fun setFavicon(bitmap: Bitmap?) {
        faviconState.value = bitmap
    }

    fun restoreCustomFields() {
        customFields = Gson().fromJson(passwordData.get("customFields").asString, typeToken)
    }
}