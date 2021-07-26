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

import android.annotation.SuppressLint
import android.graphics.*
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer

@SuppressLint("StaticFieldLeak")
object NextcloudApi {
    private val coroutineScope = CoroutineScope(context = Dispatchers.Unconfined)

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    var client = HttpClient(engineFactory = CIO) {
        install(feature = JsonFeature) {
            serializer = KotlinxSerializer(json = json)
        }
    }

    private var server = ""
    private var loginName = ""
    private var appPassword = ""

    private const val endpoint = "/index.php/apps/passwords/api/1.0"

    private val baseFolder = json.decodeFromString<Folder>(
        string = "{\"id\":\"00000000-0000-0000-0000-000000000000\"," +
                "\"label\":\"Base\"," +
                "\"parent\":\"\"," +
                "\"favorite\":\"false\"," +
                "\"created\":0," +
                "\"updated\":0}"
    )

    private val storedPasswordsState = MutableStateFlow(value = mutableStateListOf<Password>())
    val storedPasswords = storedPasswordsState

    private val storedFoldersState = MutableStateFlow(value = mutableStateListOf(baseFolder))
    val storedFolders = storedFoldersState

    private val storedTagsState = MutableStateFlow(value = mutableStateListOf<Tag>())
    val storedTags = storedTagsState

    private val currentRequestedFaviconState = MutableStateFlow<Bitmap?>(value = null)
    val currentRequestedFavicon = currentRequestedFaviconState

    fun getCurrentAccount() = "$loginName@${server.removePrefix(prefix = "https://")}"

    fun isLogged() = server.isNotEmpty()

    fun login(server: String, loginName: String, appPassword: String) {
        this.server = server
        this.loginName = loginName
        this.appPassword = appPassword

        client = client.config {
            install(Auth) {
                basic {
                    sendWithoutRequest { true }
                    credentials {
                        BasicAuthCredentials(
                            username = NextcloudApi.loginName,
                            password = NextcloudApi.appPassword
                        )
                    }
                }
            }
        }
    }

    fun logout() {
        coroutineScope.launch {
            client.delete<Any>(urlString = "$server/ocs/v2.php/core/apppassword") {
                header("OCS-APIREQUEST", true)
            }
        }

        storedPasswordsState.value.clear()
        storedFoldersState.value.clear()
        storedTagsState.value.clear()

        server = ""
        loginName = ""
        appPassword = ""

        client = HttpClient(CIO) {
            install(JsonFeature) {
                serializer = KotlinxSerializer(json = json)
            }
        }
    }

    private suspend inline fun <reified T> listRequest(): SnapshotStateList<T> {
        return try {
            json.decodeFromString(deserializer = SnapshotListSerializer(dataSerializer = serializer()),
                string = client.get(
                    urlString = "$server$endpoint/${
                        when (T::class) {
                            Password::class -> "password"
                            Folder::class -> "folder"
                            else -> "tag"
                        }
                    }/list"
                ) {
                    if (T::class == Password::class)
                        parameter(key = "details", value = "model+tags")
                })
        } catch (e: Exception) {
            mutableStateListOf()
        }
    }

    suspend fun refreshServerList(refreshFolders: Boolean = true, refreshTags: Boolean = true) {
        client.coroutineContext.cancelChildren()

        if (refreshFolders) {
            coroutineScope.launch {
                val folders = listRequest<Folder>()

                folders.sortBy { it.label.lowercase() }
                folders.add(index = 0, element = baseFolder)

                storedFoldersState.value = folders
            }
        }

        if (refreshTags)
            coroutineScope.launch {
                val tags = listRequest<Tag>()

                tags.sortBy { it.label.lowercase() }

                storedTagsState.value = tags
            }

        coroutineScope.launch {
            val passwords = listRequest<Password>()

            passwords.sortBy { it.label.lowercase() }
            passwords.forEach { password -> faviconRequest(data = password) }

            storedPasswordsState.value = passwords
        }.join()
    }

    private suspend inline fun <reified T> showRequest(id: String): T {
        return json.decodeFromString(
            deserializer = serializer(),
            string = client.post(
                urlString = "$server$endpoint/${
                    when (T::class) {
                        Password::class -> "password"
                        Folder::class -> "folder"
                        else -> "tag"
                    }
                }/show"
            ) {
                parameter(key = "id", value = id)
                if (T::class == Password::class) parameter(key = "details", value = "model+tags")
            })
    }

    suspend fun createPasswordRequest(params: Map<String, String>, tags: List<Tag>) {
        coroutineScope.launch {
            val newPassword = showRequest<Password>(
                id = client.post<JsonObject>(urlString = "$server$endpoint/password/create") {
                    params.forEach { parameter(key = it.key, value = it.value) }
                    tags.forEach { parameter(key = "tags[]", value = it.id) }
                }["id"]!!.jsonPrimitive.content
            )

            faviconRequest(data = newPassword)

            val data = storedPasswordsState.value

            data.add(element = newPassword)
            data.sortBy { it.label.lowercase() }

            storedPasswordsState.value = data
        }.join()
    }

    suspend fun createFolderRequest(params: Map<String, String>) {
        coroutineScope.launch {
            val newFolder = showRequest<Folder>(
                id = client.post<JsonObject>(urlString = "$server$endpoint/folder/create") {
                    params.forEach { parameter(key = it.key, value = it.value) }
                }["id"]!!.jsonPrimitive.content
            )

            val data = storedFoldersState.value

            data.removeAt(index = 0)
            data.add(element = newFolder)
            data.sortBy { it.label.lowercase() }
            data.add(index = 0, element = baseFolder)

            storedFoldersState.value = data
        }.join()
    }

    suspend fun createTagRequest(params: Map<String, String>) {
        coroutineScope.launch {
            val newTag = showRequest<Tag>(
                id = client.post<JsonObject>(urlString = "$server$endpoint/tag/create") {
                    params.forEach { parameter(key = it.key, value = it.value) }
                }["id"]!!.jsonPrimitive.content
            )

            val data = storedTagsState.value

            data.add(element = newTag)
            data.sortBy { it.label.lowercase() }

            storedTagsState.value = data
        }.join()
    }

    suspend fun deletePasswordRequest(id: String) {
        coroutineScope.launch {
            client.delete<Any>(urlString = "$server$endpoint/password/delete") {
                parameter(key = "id", value = id)
            }

            storedPasswordsState.value.removeIf { it.id == id }
        }.join()
    }

    suspend fun deleteFolderRequest(id: String) {
        coroutineScope.launch {
            client.delete<Any>(urlString = "$server$endpoint/folder/delete") {
                parameter(key = "id", value = id)
            }

            refreshServerList()
        }.join()
    }

    suspend fun deleteTagRequest(id: String) {
        coroutineScope.launch {
            client.delete<Any>(urlString = "$server$endpoint/tag/delete") {
                parameter(key = "id", value = id)
            }

            refreshServerList()
        }.join()
    }

    suspend fun updatePasswordRequest(params: Map<String, String>, tags: List<Tag>) {
        coroutineScope.launch {
            client.patch<Any>(urlString = "$server$endpoint/password/update") {
                params.forEach { parameter(key = it.key, value = it.value) }
                tags.forEach { parameter(key = "tags[]", value = it.id) }
            }

            val updatedPassword = showRequest<Password>(id = params["id"]!!)

            val index = storedPasswordsState.value.indexOfFirst { it.id == params["id"]!! }

            if (params["url"]!! != storedPasswordsState.value[index].url)
                faviconRequest(data = updatedPassword)
            else updatedPassword.setFavicon(bitmap = storedPasswordsState.value[index].favicon.value)

            storedPasswordsState.value[index] = updatedPassword
        }.join()
    }

    suspend fun updateFolderRequest(params: Map<String, String>) {
        coroutineScope.launch {
            client.patch<Any>(urlString = "$server$endpoint/folder/update") {
                params.forEach { parameter(key = it.key, value = it.value) }
            }

            val updatedFolder = showRequest<Folder>(id = params["id"]!!)

            storedFoldersState.value[storedFoldersState.value.indexOfFirst {
                it.id == params["id"]!!
            }] = updatedFolder
        }.join()
    }

    suspend fun updateTagRequest(params: Map<String, String>) {
        coroutineScope.launch {
            client.patch<Any>(urlString = "$server$endpoint/tag/update") {
                params.forEach { parameter(key = it.key, value = it.value) }
            }

            val updatedTag = showRequest<Tag>(id = params["id"]!!)

            storedTagsState.value[storedTagsState.value.indexOfFirst {
                it.id == params["id"]!!
            }] = updatedTag

            refreshServerList(refreshFolders = false, refreshTags = false)
        }.join()
    }

    suspend fun generatePassword(): String {
        return client.get<JsonObject>(urlString = "$server$endpoint/service/password") {
            expectSuccess = false
            parameter("details", "model+tags")
        }["password"]!!.jsonPrimitive.content
    }

    fun faviconRequest(data: Any) {
        when (data) {
            is Password -> coroutineScope.launch(context = Dispatchers.IO) {
                try {
                    data.setFavicon(
                        BitmapFactory.decodeStream(
                            client.get(
                                urlString = "$server$endpoint/service/favicon/${
                                    Uri.parse(data.url).host ?: data.url
                                }/256"
                            )
                        ).toRoundedCorners()
                    )
                } catch (e: Exception) {
                }
            }
            is String -> coroutineScope.launch {
                if (data.isNotEmpty()) {
                    try {
                        currentRequestedFaviconState.value = BitmapFactory.decodeStream(
                            client.get(
                                urlString = "$server$endpoint/service/favicon/${
                                    Uri.parse(data).host ?: data
                                }/256"
                            )
                        ).toRoundedCorners()
                    } catch (e: Exception) {
                    }
                } else currentRequestedFaviconState.value = null
            }
        }
    }

    fun Bitmap.toRoundedCorners(cornerRadius: Float = 32F): Bitmap? {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            clipPath(Path().apply {
                addRoundRect(
                    RectF(0f, 0f, width.toFloat(), height.toFloat()),
                    cornerRadius,
                    cornerRadius,
                    Path.Direction.CCW
                )
            })
        }.drawBitmap(this, 0f, 0f, null)

        return bitmap
    }
}