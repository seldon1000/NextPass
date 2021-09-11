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

import android.graphics.*
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.ionspin.kotlin.crypto.LibsodiumInitializer
import com.ionspin.kotlin.crypto.box.crypto_box_SEEDBYTES
import com.ionspin.kotlin.crypto.generichash.GenericHash
import com.ionspin.kotlin.crypto.generichash.crypto_generichash_blake2b_BYTES_MAX
import com.ionspin.kotlin.crypto.pwhash.*
import com.ionspin.kotlin.crypto.util.LibsodiumUtil
import com.ionspin.kotlin.crypto.util.encodeToUByteArray
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer

class NextcloudApi {
    private val coroutineScope = CoroutineScope(context = Dispatchers.Unconfined)

    private var server = ""
    private var loginName = ""
    private var appPassword = ""
    private val endpoint = "/index.php/apps/passwords/api/1.0"

    private var client = HttpClient(engineFactory = CIO) {
        install(feature = JsonFeature) {
            serializer = KotlinxSerializer(json = json)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private val baseFolder = json.decodeFromString<Folder>(
        string = "{\"id\":\"00000000-0000-0000-0000-000000000000\"," +
                "\"label\":\"Base\"," +
                "\"parent\":\"\"," +
                "\"favorite\":\"false\"," +
                "\"created\":0," +
                "\"updated\":0}"
    )

    val storedPasswords = MutableStateFlow(value = mutableStateListOf<Password>())
    val storedFolders = MutableStateFlow(value = mutableStateListOf(baseFolder))
    val storedTags = MutableStateFlow(value = mutableStateListOf<Tag>())
    val currentRequestedFavicon = MutableStateFlow<Bitmap?>(value = null)

    fun getCurrentAccount() = "$loginName@${server.removePrefix(prefix = "https://")}"

    fun isLogged() = server.isNotEmpty()

    suspend fun startLogin(url: String) = client.post<JsonObject>(urlString = url)

    suspend fun loginPolling(endpoint: String, token: String): Map<String, String> {
        var i = 0
        var loginResponse = mapOf<String, String>()

        while (loginResponse.isEmpty() && i <= 120) {
            try {
                loginResponse = client.post(urlString = endpoint) {
                    expectSuccess = false
                    parameter(key = "token", value = token)
                }
            } catch (e: Exception) {
            }

            delay(timeMillis = 500)
            i++
        }

        return loginResponse
    }

    fun login(server: String, loginName: String, appPassword: String) {
        this.server = server
        this.loginName = loginName
        this.appPassword = appPassword

        client = client.config {
            install(Auth) {
                basic {
                    sendWithoutRequest { true }
                    credentials {
                        BasicAuthCredentials(username = loginName, password = appPassword)
                    }
                }
            }
        }
    }

    suspend fun logout() {
        client.delete<Any>(urlString = "$server/ocs/v2.php/core/apppassword") {
            header("OCS-APIREQUEST", true)
        }
    }

    private suspend inline fun <reified T> listRequest(): SnapshotStateList<T> {
        return json.decodeFromString(deserializer = SnapshotListSerializer(dataSerializer = serializer()),
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
    }

    suspend fun refreshServerList(refreshFolders: Boolean = true, refreshTags: Boolean = true) {
        if (refreshFolders)
            coroutineScope.launch {
                try {
                    val folders = listRequest<Folder>()

                    folders.sortBy { it.label.lowercase() }
                    folders.add(index = 0, element = baseFolder)

                    storedFolders.value = folders
                } catch (e: Exception) {
                }
            }

        if (refreshTags) coroutineScope.launch {
            try {
                val tags = listRequest<Tag>()

                tags.sortBy { it.label.lowercase() }

                storedTags.value = tags
            } catch (e: Exception) {
            }
        }

        val passwords = listRequest<Password>()

        passwords.sortBy { it.label.lowercase() }
        passwords.forEach { password -> faviconRequest(data = password) }

        storedPasswords.value = passwords
    }

    @kotlin.ExperimentalUnsignedTypes
    fun test() {
        coroutineScope.launch {
            val io = (client.get<JsonObject>(
                urlString = "$server$endpoint/session/request"
            ) {})["challenge"]!!.jsonObject["salts"]!!.jsonArray

            LibsodiumInitializer.initialize()

            val salt0 = LibsodiumUtil.fromHex(data = io[0].jsonPrimitive.content)
            val salt1 = LibsodiumUtil.fromHex(data = io[1].jsonPrimitive.content)
            val salt2 = LibsodiumUtil.fromHex(data = io[2].jsonPrimitive.content)

            val k = GenericHash.genericHash(
                message = "***$salt0".encodeToUByteArray(),
                requestedHashLength = crypto_generichash_blake2b_BYTES_MAX,
                key = salt1
            )

            val g = PasswordHash.pwhash(
                crypto_box_SEEDBYTES,
                k.toString(),
                salt2,
                crypto_pwhash_OPSLIMIT_INTERACTIVE.toULong(),
                crypto_pwhash_MEMLIMIT_INTERACTIVE,
                crypto_pwhash_ALG_DEFAULT
            )

            val challenge = LibsodiumUtil.toHex(data = g)

            client.post<JsonObject>(urlString = "$server$endpoint/session/open") {
                parameter(key = "challenge", value = challenge)
            }
        }
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
        val newPassword = showRequest<Password>(
            id = client.post<JsonObject>(urlString = "$server$endpoint/password/create") {
                params.forEach { parameter(key = it.key, value = it.value) }
                tags.forEach { parameter(key = "tags[]", value = it.id) }
            }["id"]!!.jsonPrimitive.content
        )

        faviconRequest(data = newPassword)

        val data = storedPasswords.value
        data.add(element = newPassword)
        data.sortBy { it.label.lowercase() }

        storedPasswords.value = data
    }

    suspend fun createFolderRequest(params: Map<String, String>) {
        val newFolder = showRequest<Folder>(
            id = client.post<JsonObject>(urlString = "$server$endpoint/folder/create") {
                params.forEach { parameter(key = it.key, value = it.value) }
            }["id"]!!.jsonPrimitive.content
        )

        val data = storedFolders.value

        data.removeAt(index = 0)
        data.add(element = newFolder)
        data.sortBy { it.label.lowercase() }
        data.add(index = 0, element = baseFolder)

        storedFolders.value = data
    }

    suspend fun createTagRequest(params: Map<String, String>) {
        val newTag = showRequest<Tag>(
            id = client.post<JsonObject>(urlString = "$server$endpoint/tag/create") {
                params.forEach { parameter(key = it.key, value = it.value) }
            }["id"]!!.jsonPrimitive.content
        )

        val data = storedTags.value

        data.add(element = newTag)
        data.sortBy { it.label.lowercase() }

        storedTags.value = data
    }

    suspend fun deletePasswordRequest(id: String) {
        client.delete<Any>(urlString = "$server$endpoint/password/delete") {
            parameter(key = "id", value = id)
        }

        storedPasswords.value.removeIf { it.id == id }
    }

    suspend fun deleteFolderRequest(id: String) {
        client.delete<Any>(urlString = "$server$endpoint/folder/delete") {
            parameter(key = "id", value = id)
        }
    }

    suspend fun deleteTagRequest(id: String) {
        client.delete<Any>(urlString = "$server$endpoint/tag/delete") {
            parameter(key = "id", value = id)
        }
    }

    suspend fun updatePasswordRequest(params: Map<String, String>, tags: List<Tag>) {
        client.patch<Any>(urlString = "$server$endpoint/password/update") {
            params.forEach { parameter(key = it.key, value = it.value) }
            tags.forEach { parameter(key = "tags[]", value = it.id) }
        }

        val updatedPassword = showRequest<Password>(id = params["id"]!!)

        val index = storedPasswords.value.indexOfFirst { it.id == params["id"]!! }

        if (params["url"]!! != storedPasswords.value[index].url)
            faviconRequest(data = updatedPassword)
        else updatedPassword.setFavicon(bitmap = storedPasswords.value[index].favicon.value)

        storedPasswords.value[index] = updatedPassword
    }

    suspend fun updateFolderRequest(params: Map<String, String>) {
        client.patch<Any>(urlString = "$server$endpoint/folder/update") {
            params.forEach { parameter(key = it.key, value = it.value) }
        }

        val updatedFolder = showRequest<Folder>(id = params["id"]!!)

        storedFolders.value[storedFolders.value.indexOfFirst {
            it.id == params["id"]!!
        }] = updatedFolder
    }

    suspend fun updateTagRequest(params: Map<String, String>) {
        client.patch<Any>(urlString = "$server$endpoint/tag/update") {
            params.forEach { parameter(key = it.key, value = it.value) }
        }

        val updatedTag = showRequest<Tag>(id = params["id"]!!)

        storedTags.value[storedTags.value.indexOfFirst {
            it.id == params["id"]!!
        }] = updatedTag
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
                        currentRequestedFavicon.value = BitmapFactory.decodeStream(
                            client.get(
                                urlString = "$server$endpoint/service/favicon/${
                                    Uri.parse(data).host ?: data
                                }/256"
                            )
                        ).toRoundedCorners()
                    } catch (e: Exception) {
                    }
                } else currentRequestedFavicon.value = null
            }
        }
    }

    companion object {
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
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
}