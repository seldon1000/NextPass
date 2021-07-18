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
import android.content.SharedPreferences
import android.content.res.Resources
import android.graphics.*
import android.net.Uri
import android.webkit.CookieManager
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.CentralAppControl
import eu.seldon1000.nextpass.ui.items.TextFieldItem
import eu.seldon1000.nextpass.ui.layout.Routes
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer

@SuppressLint("StaticFieldLeak")
object NextcloudApiProvider : ViewModel() {
    private var resources: Resources? = null
    private var sharedPreferences: SharedPreferences? = null

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var client = HttpClient(engineFactory = CIO) {
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

    fun initializeApi(res: Resources, pref: SharedPreferences) {
        if (resources == null && sharedPreferences == null) {
            resources = res
            sharedPreferences = pref

            if (sharedPreferences!!.contains("server")) {
                server = sharedPreferences!!.getString("server", "")!!
                loginName = sharedPreferences!!.getString("loginName", "")!!
                appPassword = sharedPreferences!!.getString("appPassword", "")!!

                client = client.config {
                    install(Auth) {
                        basic {
                            sendWithoutRequest { true }
                            credentials {
                                BasicAuthCredentials(
                                    username = loginName,
                                    password = appPassword
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun getCurrentAccount() = "$loginName@${server.removePrefix(prefix = "https://")}"

    fun isLogged() = server.isNotEmpty()

    @ExperimentalMaterialApi
    fun attemptLogin() {
        val url = mutableStateOf(value = "")

        CentralAppControl.showDialog(title = resources!!.getString(R.string.insert_server_url), body = {
            TextFieldItem(
                text = url.value,
                onTextChanged = { url.value = it },
                label = resources!!.getString(R.string.url),
                required = true
            )
        }, confirm = true) {
            if (!url.value.startsWith(prefix = "https://") &&
                !url.value.startsWith(prefix = "http://")
            ) url.value = "https://${url.value}"
            url.value = "${url.value}/index.php/login/v2"

            viewModelScope.launch {
                try {
                    val response = client.post<JsonObject>(urlString = url.value)

                    val login = Uri.encode(response["login"]!!.jsonPrimitive.content, "UTF-8")
                    val endpoint = response["poll"]!!.jsonObject["endpoint"]!!.jsonPrimitive.content
                    val token = response["poll"]!!.jsonObject["token"]!!.jsonPrimitive.content

                    CentralAppControl.navigate(route = Routes.WebView.getRoute(login))

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

                    if (server.isNotEmpty()) CentralAppControl.popBackStack()

                    if (i > 120 &&
                        CentralAppControl.navController.value!!.currentDestination!!.route ==
                        Routes.WebView.getRoute(arg = login)
                    ) CentralAppControl.showDialog(
                        title = resources!!.getString(R.string.timeout_expired),
                        body = {
                            Text(
                                text = resources!!.getString(R.string.timeout_expired_body),
                                fontSize = 14.sp
                            )
                        })
                    else if (i <= 120) {
                        server = loginResponse["server"]!!
                        loginName = loginResponse["loginName"]!!
                        appPassword = loginResponse["appPassword"]!!

                        sharedPreferences!!.edit().putString("server", server).apply()
                        sharedPreferences!!.edit().putString("loginName", loginName).apply()
                        sharedPreferences!!.edit().putString("appPassword", appPassword).apply()

                        client = client.config {
                            install(Auth) {
                                basic {
                                    sendWithoutRequest { true }
                                    credentials {
                                        BasicAuthCredentials(
                                            username = loginName,
                                            password = appPassword
                                        )
                                    }
                                }
                            }
                        }

                        CentralAppControl.openApp(shouldRememberScreen = server.isEmpty())
                        CentralAppControl.showSnackbar(
                            message = resources!!.getString(
                                R.string.connected_snack,
                                loginName
                            )
                        )
                    }

                    val cookieManager = CookieManager.getInstance()
                    cookieManager.removeSessionCookies {}
                    cookieManager.removeAllCookies {}
                    cookieManager.flush()
                } catch (e: Exception) {
                    showError()
                }
            }
        }
    }

    fun attemptLogout() {
        CentralAppControl.showDialog(
            title = resources!!.getString(R.string.logout),
            body = { Text(text = resources!!.getString(R.string.logout_body), fontSize = 14.sp) },
            confirm = true
        ) {
            try {
                viewModelScope.launch {
                    client.delete<Any>(urlString = "$server/ocs/v2.php/core/apppassword") {
                        header("OCS-APIREQUEST", true)
                    }
                }
            } catch (e: Exception) {
                showError()
            }

            storedPasswordsState.value.clear()
            storedFoldersState.value.clear()
            storedTagsState.value.clear()

            server = ""
            loginName = ""
            appPassword = ""

            sharedPreferences!!.edit().remove("server").apply()
            sharedPreferences!!.edit().remove("loginName").apply()
            sharedPreferences!!.edit().remove("appPassword").apply()

            client = HttpClient(CIO) {
                install(JsonFeature) {
                    serializer = KotlinxSerializer(json = json)
                }
            }

            CentralAppControl.setRefreshing(refreshing = false)
            CentralAppControl.resetUserPreferences()
            CentralAppControl.navigate(route = Routes.Welcome.route)
            CentralAppControl.showSnackbar(message = resources!!.getString(R.string.disconnected_snack))
        }
    }

    private suspend inline fun <reified T> listRequest(): SnapshotStateList<T> {
        return try {
            json.decodeFromString(
                deserializer = SnapshotListSerializer(dataSerializer = serializer()),
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
            showError()

            mutableStateListOf()
        }
    }

    fun refreshServerList(refreshFolders: Boolean = true, refreshTags: Boolean = true) {
        CentralAppControl.setRefreshing(refreshing = true)

        client.coroutineContext.cancelChildren()

        viewModelScope.launch {
            val passwords = listRequest<Password>()

            passwords.sortBy { it.label.lowercase() }
            passwords.forEach { password -> faviconRequest(data = password) }

            storedPasswordsState.value = passwords

            CentralAppControl.setRefreshing(refreshing = false)
        }

        if (refreshFolders) {
            viewModelScope.launch {
                val folders = listRequest<Folder>()

                folders.sortBy { it.label.lowercase() }
                folders.add(index = 0, element = baseFolder)

                storedFoldersState.value = folders
            }
        }

        if (refreshTags)
            viewModelScope.launch {
                val tags = listRequest<Tag>()

                tags.sortBy { it.label.lowercase() }

                storedTagsState.value = tags
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

    fun createPasswordRequest(params: Map<String, String>, tags: List<Tag>) {
        viewModelScope.launch {
            try {
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

                CentralAppControl.setSelectedFolder(folder = CentralAppControl.currentFolder.value)

                CentralAppControl.setRefreshing(refreshing = false)
            } catch (e: Exception) {
                showError()
            }
        }
    }

    fun createFolderRequest(params: Map<String, String>) {
        viewModelScope.launch {
            try {
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

                CentralAppControl.setCurrentFolder(folder = storedFoldersState.value.indexOfFirst {
                    it.id == params["parent"]
                })

                CentralAppControl.setRefreshing(refreshing = false)
            } catch (e: Exception) {
                showError()
            }
        }
    }

    fun createTagRequest(params: Map<String, String>) {
        viewModelScope.launch {
            try {
                val newTag = showRequest<Tag>(
                    id = client.post<JsonObject>(urlString = "$server$endpoint/tag/create") {
                        params.forEach { parameter(key = it.key, value = it.value) }
                    }["id"]!!.jsonPrimitive.content
                )

                val data = storedTagsState.value

                data.add(element = newTag)
                data.sortBy { it.label.lowercase() }

                storedTagsState.value = data

                CentralAppControl.setRefreshing(refreshing = false)
            } catch (e: Exception) {
                showError()
            }
        }
    }

    fun deletePasswordRequest(id: String) {
        viewModelScope.launch {
            try {
                client.delete<Any>(urlString = "$server$endpoint/password/delete") {
                    parameter(key = "id", value = id)
                }

                storedPasswordsState.value.removeIf { it.id == id }
            } catch (e: Exception) {
                showError()
            }
        }
    }

    fun deleteFolderRequest(id: String) {
        viewModelScope.launch {
            try {
                client.delete<Any>(urlString = "$server$endpoint/folder/delete") {
                    parameter(key = "id", value = id)
                }

                refreshServerList()
            } catch (e: Exception) {
                showError()
            }
        }
    }

    fun deleteTagRequest(id: String) {
        viewModelScope.launch {
            try {
                client.delete<Any>(urlString = "$server$endpoint/tag/delete") {
                    parameter(key = "id", value = id)
                }

                refreshServerList()
            } catch (e: Exception) {
                showError()
            }
        }
    }

    fun updatePasswordRequest(params: Map<String, String>, tags: List<Tag>) {
        viewModelScope.launch {
            try {
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

                CentralAppControl.setRefreshing(refreshing = false)
            } catch (e: Exception) {
                showError()
            }
        }
    }

    fun updateFolderRequest(params: Map<String, String>) {
        viewModelScope.launch {
            try {
                client.patch<Any>(urlString = "$server$endpoint/folder/update") {
                    params.forEach { parameter(key = it.key, value = it.value) }
                }

                val updatedFolder = showRequest<Folder>(id = params["id"]!!)

                storedFoldersState.value[storedFoldersState.value.indexOfFirst {
                    it.id == params["id"]!!
                }] = updatedFolder

                CentralAppControl.setRefreshing(refreshing = false)
            } catch (e: Exception) {
                showError()
            }
        }
    }

    fun updateTagRequest(params: Map<String, String>) {
        viewModelScope.launch {
            try {
                client.patch<Any>(urlString = "$server$endpoint/tag/update") {
                    params.forEach { parameter(key = it.key, value = it.value) }
                }

                val updatedTag = showRequest<Tag>(id = params["id"]!!)

                storedTagsState.value[storedTagsState.value.indexOfFirst {
                    it.id == params["id"]!!
                }] = updatedTag

                refreshServerList(refreshFolders = false, refreshTags = false)
            } catch (e: Exception) {
                showError()
            }
        }
    }

    suspend fun generatePassword(): String {
        return try {
            client.get<JsonObject>(urlString = "$server$endpoint/service/password") {
                expectSuccess = false
                parameter("details", "model+tags")
            }["password"]!!.jsonPrimitive.content
        } catch (e: Exception) {
            showError()

            ""
        }
    }

    fun faviconRequest(data: Any) {
        when (data) {
            is Password -> viewModelScope.launch(context = Dispatchers.IO) {
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
            is String -> viewModelScope.launch {
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

    private fun showError() {
        CentralAppControl.showDialog(
            title = resources!!.getString(R.string.error),
            body = { Text(text = resources!!.getString(R.string.error_body), fontSize = 14.sp) }
        )

        CentralAppControl.setRefreshing(refreshing = false)
    }
}