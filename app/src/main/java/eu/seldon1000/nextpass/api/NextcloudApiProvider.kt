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
import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.net.Uri
import android.webkit.CookieManager
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.ui.MainViewModel
import eu.seldon1000.nextpass.ui.items.TextFieldItem
import eu.seldon1000.nextpass.ui.layout.Routes
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.features.*
import io.ktor.client.features.auth.*
import io.ktor.client.features.auth.providers.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer
import java.net.URLEncoder

@SuppressLint("StaticFieldLeak")
object NextcloudApiProvider : ViewModel() {
    private var context: Context? = null

    private var sharedPreferences: SharedPreferences? = null

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var client = HttpClient(Android) {
        install(feature = JsonFeature) {
            serializer = KotlinxSerializer(json = json)
        }
    }

    private var server = ""
    private var loginName = ""
    private var appPassword = ""

    private const val endpoint = "/index.php/apps/passwords/api/1.0"

    private val baseFolder = json.decodeFromString(
        deserializer = Folder.serializer(),
        string = "{\"id\":\"00000000-0000-0000-0000-000000000000\",\"label\":\"Base\",\"parent\":null,\"favorite\":\"false\",\"created\":0,\"edited\":0}"
    )

    private val storedPasswordsState = MutableStateFlow(value = mutableStateListOf<Password>())
    val storedPasswords = storedPasswordsState

    private val storedFoldersState = MutableStateFlow(value = mutableStateListOf(baseFolder))
    val storedFolders = storedFoldersState

    private val storedTagsState = MutableStateFlow(value = mutableStateListOf<Tag>())
    val storedTags = storedTagsState

    private val currentRequestedFaviconState = MutableStateFlow<Bitmap?>(value = null)
    val currentRequestedFavicon = currentRequestedFaviconState

    fun setContext(context: Any) {
        if (context is FragmentActivity || this.context == null) {
            this.context = context as Context

            if (sharedPreferences == null) {
                sharedPreferences = this.context!!.getSharedPreferences("nextpass", 0)

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
    }

    fun getCurrentAccount() = "$loginName@${server.removePrefix(prefix = "https://")}"

    fun isLogged() = server.isNotEmpty()

    fun attemptLogout() {
        MainViewModel.showDialog(
            title = context!!.getString(R.string.logout),
            body = { Text(text = context!!.getString(R.string.logout_body), fontSize = 14.sp) },
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

            client = HttpClient(Android) {
                install(JsonFeature) {
                    serializer = KotlinxSerializer(json = json)
                }
            }

            MainViewModel.setRefreshing(refreshing = false)
            MainViewModel.resetUserSettings()
            MainViewModel.navigate(route = Routes.Welcome.route)
            MainViewModel.showSnackbar(message = context!!.getString(R.string.disconnected_snack))
        }
    }

    @ExperimentalMaterialApi
    fun attemptLogin() {
        val url = mutableStateOf(value = "")

        MainViewModel.showDialog(title = context!!.getString(R.string.insert_server_url), body = {
            TextFieldItem(
                text = url.value,
                onTextChanged = { url.value = it },
                label = context!!.getString(R.string.url),
                required = true
            )
        }, confirm = true) {
            if (!url.value.startsWith(prefix = "https://") ||
                !url.value.startsWith(prefix = "http://")
            ) url.value = "https://${url.value}"
            url.value = "${url.value}/index.php/login/v2"

            viewModelScope.launch {
                try {
                    val response = client.post<JsonObject>(urlString = url.value)

                    val login = response["login"]!!.jsonPrimitive.content
                    val endpoint = response["poll"]!!.jsonObject["endpoint"]!!.jsonPrimitive.content
                    val token = response["poll"]!!.jsonObject["token"]!!.jsonPrimitive.content

                    MainViewModel.navigate(
                        route = Routes.WebView.getRoute(
                            arg = URLEncoder.encode(login, "UTF-8")
                        )
                    )

                    var i = 0
                    var loginResponse = JsonObject(mapOf())

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

                    if (server.isNotEmpty()) MainViewModel.popBackStack()

                    if (i > 120) MainViewModel.showDialog(
                        title = context!!.getString(R.string.timeout_expired),
                        body = {
                            Text(
                                text = context!!.getString(R.string.timeout_expired_body),
                                fontSize = 14.sp
                            )
                        })
                    else {
                        server = loginResponse["server"]!!.jsonPrimitive.content
                        loginName = loginResponse["loginName"]!!.jsonPrimitive.content
                        appPassword = loginResponse["appPassword"]!!.jsonPrimitive.content

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

                        MainViewModel.openApp(shouldRememberScreen = server.isEmpty())
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
                ) { if (T::class == Password::class) parameter("details", "model+tags") })
        } catch (e: Exception) {
            showError()

            mutableStateListOf()
        }
    }

    fun refreshServerList(refreshFolders: Boolean = true, refreshTags: Boolean = true) {
        MainViewModel.setRefreshing(refreshing = true)

        viewModelScope.launch(context = Dispatchers.IO) {
            if (refreshFolders) {
                val folders: SnapshotStateList<Folder> = listRequest()

                folders.sortBy { it.label.lowercase() }
                folders.add(index = 0, element = baseFolder)

                storedFoldersState.value = folders
            }

            if (refreshTags) {
                val tags: SnapshotStateList<Tag> = listRequest()

                tags.sortBy { it.label.lowercase() }

                storedTagsState.value = tags
            }

            val passwords: SnapshotStateList<Password> = listRequest()

            passwords.sortBy { it.label.lowercase() }
            passwords.forEach { faviconRequest(data = it) }

            storedPasswordsState.value = passwords

            MainViewModel.setRefreshing(refreshing = false)
        }
    }

    fun createPasswordRequest(params: Map<String, String>) {
        viewModelScope.launch(context = Dispatchers.IO) {
            try {
                val response =
                    client.post<JsonObject>(urlString = "$server$endpoint/password/create") {
                        params.forEach { parameter(key = it.key, value = it.value) }
                    }["id"]!!.jsonPrimitive.content

                val newPassword =
                    client.post<Password>(urlString = "$server$endpoint/password/show") {
                        parameter(key = "id", value = response)
                        parameter(key = "details", value = "model+tags")
                    }

                faviconRequest(data = newPassword)

                val data = storedPasswordsState.value

                data.add(element = newPassword)
                data.sortBy { it.label.lowercase() }

                storedPasswordsState.value = data

                MainViewModel.setSelectedFolder(folder = MainViewModel.currentFolder.value)

                MainViewModel.setRefreshing(refreshing = false)
            } catch (e: Exception) {
                showError()
            }
        }
    }

    fun createFolderRequest(params: Map<String, String>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response =
                    client.post<JsonObject>(urlString = "$server$endpoint/folder/create") {
                        params.forEach { parameter(key = it.key, value = it.value) }
                    }["id"]!!.jsonPrimitive.content

                val newFolder =
                    client.post<Folder>(urlString = "$server$endpoint/folder/show") {
                        parameter(key = "id", value = response)
                    }

                val data = storedFoldersState.value

                data.removeAt(index = 0)
                data.add(element = newFolder)
                data.sortBy { it.label.lowercase() }
                data.add(index = 0, element = baseFolder)

                storedFoldersState.value = data

                MainViewModel.setCurrentFolder(folder = storedFoldersState.value.indexOfFirst {
                    it.id == params["parent"]
                })

                MainViewModel.setRefreshing(refreshing = false)
            } catch (e: Exception) {
                showError()
            }
        }
    }

    fun createTagRequest(params: Map<String, String>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response =
                    client.post<JsonObject>(urlString = "$server$endpoint/tag/create") {
                        params.forEach { parameter(key = it.key, value = it.value) }
                    }["id"]!!.jsonPrimitive.content

                val newTag =
                    client.post<Tag>(urlString = "$server$endpoint/tag/show") {
                        parameter(key = "id", value = response)
                    }

                val data = storedTagsState.value

                data.add(element = newTag)
                data.sortBy { it.label.lowercase() }

                storedTagsState.value = data

                MainViewModel.setRefreshing(refreshing = false)
            } catch (e: Exception) {
                showError()
            }
        }
    }

    fun deletePasswordRequest(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
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
        viewModelScope.launch(Dispatchers.IO) {
            try {
                client.patch<Any>(urlString = "$server$endpoint/password/update") {
                    params.forEach { parameter(key = it.key, value = it.value) }
                    tags.forEach { parameter(key = "tags[]", value = it.id) }
                }

                val updatedPassword =
                    client.post<Password>(urlString = "$server$endpoint/password/show") {
                        parameter(key = "id", value = params["id"]!!)
                        parameter(key = "details", value = "model+tags")
                    }

                val index = storedPasswordsState.value.indexOfFirst { it.id == params["id"]!! }

                if (params["url"]!! != storedPasswordsState.value[index].url)
                    faviconRequest(data = updatedPassword)
                else updatedPassword.setFavicon(bitmap = storedPasswordsState.value[index].favicon.value)

                storedPasswordsState.value[index] = updatedPassword

                MainViewModel.setRefreshing(refreshing = false)
            } catch (e: Exception) {
                showError()
            }
        }
    }

    fun updateFolderRequest(params: Map<String, String>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                client.patch<Any>(urlString = "$server$endpoint/folder/update") {
                    params.forEach { parameter(key = it.key, value = it.value) }
                }

                val updatedFolder =
                    client.post<Folder>(urlString = "$server$endpoint/folder/show") {
                        parameter(key = "id", value = params["id"]!!)
                    }

                storedFoldersState.value[storedFoldersState.value.indexOfFirst {
                    it.id == params["id"]!!
                }] = updatedFolder

                MainViewModel.setRefreshing(refreshing = false)
            } catch (e: Exception) {
                showError()
            }
        }
    }

    fun updateTagRequest(params: Map<String, String>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                client.patch<Any>(urlString = "$server$endpoint/tag/update") {
                    params.forEach { parameter(key = it.key, value = it.value) }
                }

                val updatedTag =
                    client.post<Tag>(urlString = "$server$endpoint/tag/show") {
                        parameter(key = "id", value = params["id"]!!)
                    }

                storedTagsState.value[storedTagsState.value.indexOfFirst {
                    it.id == params["id"]!!
                }] = updatedTag

                MainViewModel.setRefreshing(refreshing = false)
            } catch (e: Exception) {
                showError()
            }
        }
    }

    suspend fun generatePassword(): String {
        return withContext(viewModelScope.coroutineContext + Dispatchers.IO) {
            try {
                client.get<JsonObject>(urlString = "$server$endpoint/service/password") {
                    expectSuccess = false
                    parameter("details", "model+tags")
                }["password"]!!.jsonPrimitive.content
            } catch (e: Exception) {
                showError()

                ""
            }
        }
    }

    fun faviconRequest(data: Any) {
        when (data) {
            is Password -> viewModelScope.launch(Dispatchers.IO) {
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
            is String -> viewModelScope.launch(Dispatchers.IO) {
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

    private fun Bitmap.toRoundedCorners(cornerRadius: Float = 32F): Bitmap? {
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
        MainViewModel.showDialog(
            title = context!!.getString(R.string.error),
            body = { Text(text = context!!.getString(R.string.error_body), fontSize = 14.sp) }
        )

        MainViewModel.setRefreshing(refreshing = false)
    }

    fun stopNextcloudApi() {
        viewModelScope.coroutineContext.cancelChildren()
        context = null
    }
}