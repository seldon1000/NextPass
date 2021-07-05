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

import android.icu.text.SimpleDateFormat
import com.google.gson.JsonObject
import java.util.*

data class Tag(val tagData: JsonObject, var index: Int = -1) {
    private val formatter = SimpleDateFormat.getDateTimeInstance()

    val id: String = tagData.get("id").asString
    val label: String = tagData.get("label").asString
    val color: String = tagData.get("color").asString

    val created: String? = formatter.format(Date(tagData.get("created").asLong * 1000))
    val edited: String? = formatter.format(Date(tagData.get("edited").asLong * 1000))

    val favorite: Boolean = tagData.get("favorite").asBoolean
}