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
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.SharedPreferences
import android.graphics.*
import android.net.Uri
import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import com.nextcloud.android.sso.AccountImporter
import com.nextcloud.android.sso.aidl.NextcloudRequest
import com.nextcloud.android.sso.api.NextcloudAPI
import com.nextcloud.android.sso.exceptions.AccountImportCancelledException
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppNotInstalledException
import com.nextcloud.android.sso.helper.SingleAccountHelper
import com.nextcloud.android.sso.model.SingleSignOnAccount
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.ui.MainViewModel
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.stream.Collectors

@SuppressLint("StaticFieldLeak")
object NextcloudApiProvider : ViewModel() {
    private var context: Context? = null

    private var sharedPreferences: SharedPreferences? = null

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private const val endpoint = "/index.php/apps/passwords/api/1.0"
    private var nextcloudApi: NextcloudAPI? = null

    private val currentAccountState = MutableStateFlow<SingleSignOnAccount?>(value = null)
    val currentAccount = currentAccountState

    private val baseFolder = json.decodeFromString<Folder>(
        string = "{\"id\":\"00000000-0000-0000-0000-000000000000\",\"label\":\"Base\",\"parent\":\"\",\"favorite\":\"false\",\"created\":0,\"edited\":0}"
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
        if (context is FragmentActivity || this.context == null){
            this.context = context as Context

            sharedPreferences = this.context!!.getSharedPreferences("nextpass", 0)
        }
    }

    private val connectedCallback: NextcloudAPI.ApiConnectedListener =
        object : NextcloudAPI.ApiConnectedListener {
            override fun onConnected() {}

            override fun onError(e: Exception) {
                showError()
            }
        }

    fun attemptLogin(): Boolean {
        val client = HttpClient(Android) {
            install(HttpTimeout)
            install(JsonFeature) {
                serializer = KotlinxSerializer(json = json)
            }
        }

        viewModelScope.launch {
            val response =
                client.post<JsonObject>(urlString = "https://nx17597.your-storageshare.de/index.php/login/v2")

            val login = response["login"]!!.jsonPrimitive.content
            val endpoint = response["poll"]!!.jsonObject["endpoint"]!!.jsonPrimitive.content
            val token = response["poll"]!!.jsonObject["token"]!!.jsonPrimitive.content

            context!!.startActivity(Intent(ACTION_VIEW, Uri.parse(login)))

            var loginResponse = JsonObject(mapOf())
            while (loginResponse.isEmpty()) {
                try {
                    loginResponse = client.post(urlString = endpoint) {
                        expectSuccess = false
                        parameter(key = "token", value = token)
                    }
                } catch (e: Exception) {
                }

                delay(timeMillis = 1000)
            }


        }

        /*"https://nx17597.your-storageshare.de/index.php/login/v2".httpPost()
            .responseString { result ->
                when (result) {
                    is Result.Failure -> {

                    }
                    is Result.Success -> {



                        endpoint.httpPost(parameters = listOf("token" to token)).responseString { resultAccess ->
                            when (resultAccess) {
                                is Result.Failure -> println("CIAO ${resultAccess.error}")
                                is Result.Success -> {

                                    println("CIAO ${resultAccess.value}")
                                }
                            }
                        }.join()
                    }
                }
            }.join()*/
        return try {
            currentAccountState.value =
                SingleAccountHelper.getCurrentSingleSignOnAccount(context)

            nextcloudApi = NextcloudAPI(
                context!!,
                currentAccountState.value!!,
                GsonBuilder().create(),
                connectedCallback
            )

            true
        } catch (e: Exception) {
            false
        }
    }

    fun attemptLogout() {
        MainViewModel.showDialog(
            title = context!!.getString(R.string.logout),
            body = { Text(text = context!!.getString(R.string.logout_body), fontSize = 14.sp) },
            confirm = true
        ) {
            storedPasswordsState.value.clear()
            AccountImporter.clearAllAuthTokens(context)
            currentAccountState.value = null
            nextcloudApi = null

            MainViewModel.setRefreshing(refreshing = false)
            MainViewModel.resetUserSettings()
            MainViewModel.navigate(route = "welcome")
            MainViewModel.showSnackbar(message = context!!.getString(R.string.disconnected_snack))
        }
    }

    fun pickNewAccount() {
        try {
            AccountImporter.pickNewAccount(context as Activity)
        } catch (e: NextcloudFilesAppNotInstalledException) {
            MainViewModel.showDialog(
                title = context!!.getString(R.string.missing_nextcloud),
                body = {
                    Text(
                        text = context!!.getString(R.string.missing_nextcloud_body),
                        fontSize = 14.sp
                    )
                },
                confirm = true
            ) {
                context!!.startActivity(
                    Intent(
                        ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=com.nextcloud.client")
                    )
                )
            }
        }
    }

    fun handleAccountImporterResponse(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        try {
            AccountImporter.onActivityResult(
                requestCode,
                resultCode,
                data,
                context as Activity
            ) { account ->
                SingleAccountHelper.setCurrentAccount(context, account.name)

                storedPasswordsState.value = mutableStateListOf()

                MainViewModel.openApp(shouldRememberScreen = currentAccountState.value != null)

                MainViewModel.showSnackbar(
                    message = context!!.getString(
                        R.string.connected_snack,
                        currentAccountState.value?.userId
                    )
                )
            }
        } catch (e: AccountImportCancelledException) {
        }
    }

    private fun listPasswordsRequest(): SnapshotStateList<Password> {
        val listRequest = NextcloudRequest.Builder()
            .setMethod("POST")
            .setUrl("$endpoint/password/list")
            .setParameter(mapOf("details" to "model+tags"))
            .build()

        return try {
            json.decodeFromString(
                deserializer = SnapshotListSerializer(Password.serializer()),
                string = nextcloudApi!!.performNetworkRequest(listRequest)
                    .bufferedReader()
                    .lines()
                    .collect(Collectors.joining("\n"))
            )
        } catch (e: Exception) {
            showError()

            mutableStateListOf()
        }
    }

    private fun listFoldersRequest(): SnapshotStateList<Folder> {
        val listRequest = NextcloudRequest.Builder()
            .setMethod("GET")
            .setUrl("$endpoint/folder/list")
            .build()

        return try {
            json.decodeFromString(
                deserializer = SnapshotListSerializer(Folder.serializer()),
                string = nextcloudApi!!.performNetworkRequest(listRequest)
                    .bufferedReader()
                    .lines()
                    .collect(Collectors.joining("\n"))
            )
        } catch (e: Exception) {
            showError()

            mutableStateListOf()
        }
    }

    private fun listTagsRequest(): SnapshotStateList<Tag> {
        val listRequest = NextcloudRequest.Builder()
            .setMethod("GET")
            .setUrl("$endpoint/tag/list")
            .build()

        return try {
            json.decodeFromString(
                deserializer = SnapshotListSerializer(Tag.serializer()),
                string = nextcloudApi!!.performNetworkRequest(listRequest)
                    .bufferedReader()
                    .lines()
                    .collect(Collectors.joining("\n"))
            )
        } catch (e: Exception) {
            showError()

            mutableStateListOf()
        }
    }

    fun refreshServerList(refreshFolders: Boolean = true, refreshTags: Boolean = true) {
        MainViewModel.setRefreshing(refreshing = true)

        viewModelScope.launch(Dispatchers.IO) {
            val passwords = listPasswordsRequest()

            passwords.sortBy { it.label.lowercase() }
            passwords.forEachIndexed { index, password ->
                password.index = index

                faviconRequest(data = password)
            }

            storedPasswordsState.value = passwords

            MainViewModel.setRefreshing(refreshing = false)
        }

        if (refreshFolders) viewModelScope.launch(Dispatchers.IO) {
            val folders = listFoldersRequest()

            folders.sortBy { it.label.lowercase() }
            folders.forEachIndexed { index, folder -> folder.index = index }
            folders.add(index = 0, element = baseFolder)

            storedFoldersState.value = folders
        }

        if (refreshTags) viewModelScope.launch(Dispatchers.IO) {
            val tags = listTagsRequest()

            tags.sortBy { it.label.lowercase() }

            storedTagsState.value = tags
        }
    }

    fun createPasswordRequest(params: Map<String, String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val createRequest = NextcloudRequest.Builder()
                .setMethod("POST")
                .setUrl("$endpoint/password/create")
                .setParameter(params)
                .build()

            try {
                val response = json.decodeFromString<JsonObject>(
                    string = nextcloudApi!!.performNetworkRequest(createRequest)
                        .bufferedReader()
                        .lines()
                        .collect(Collectors.joining("\n"))
                )["id"]!!.jsonPrimitive.content

                val showRequest = NextcloudRequest.Builder()
                    .setMethod("POST")
                    .setUrl("$endpoint/password/show")
                    .setParameter(mapOf("id" to response))
                    .build()

                val newPassword = json.decodeFromString<Password>(
                    string = nextcloudApi!!.performNetworkRequest(showRequest)
                        .bufferedReader()
                        .lines()
                        .collect(Collectors.joining("\n"))
                )

                val data = storedPasswordsState.value

                faviconRequest(data = newPassword)

                data.add(element = newPassword)
                data.sortBy { it.label.lowercase() }
                data.forEachIndexed { index, password -> password.index = index }

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
            val createRequest = NextcloudRequest.Builder()
                .setMethod("POST")
                .setUrl("$endpoint/folder/create")
                .setParameter(params)
                .build()

            try {
                val response = json.decodeFromString<JsonObject>(
                    string = nextcloudApi!!.performNetworkRequest(createRequest)
                        .bufferedReader()
                        .lines()
                        .collect(Collectors.joining("\n"))
                )["id"]!!.jsonPrimitive.content

                val showRequest = NextcloudRequest.Builder()
                    .setMethod("POST")
                    .setUrl("$endpoint/folder/show")
                    .setParameter(mapOf("id" to response))
                    .build()

                val newFolder = json.decodeFromString<Folder>(
                    string = nextcloudApi!!.performNetworkRequest(showRequest)
                        .bufferedReader()
                        .lines()
                        .collect(Collectors.joining("\n"))
                )

                val data = storedFoldersState.value

                data.removeAt(index = 0)
                data.add(element = newFolder)
                data.sortBy { it.label.lowercase() }
                data.forEachIndexed { index, folder -> folder.index = index + 1 }
                data.add(index = 0, element = baseFolder)

                storedFoldersState.value = data

                MainViewModel.setSelectedFolder(folder = MainViewModel.currentFolder.value)

                MainViewModel.setRefreshing(refreshing = false)
            } catch (e: Exception) {
                showError()
            }
        }
    }

    fun createTagRequest(params: Map<String, String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val createRequest = NextcloudRequest.Builder()
                .setMethod("POST")
                .setUrl("$endpoint/tag/create")
                .setParameter(params)
                .build()

            try {
                val response = json.decodeFromString<JsonObject>(
                    string = nextcloudApi!!.performNetworkRequest(createRequest)
                        .bufferedReader()
                        .lines()
                        .collect(Collectors.joining("\n"))
                )["id"]!!.jsonPrimitive.content

                val showRequest = NextcloudRequest.Builder()
                    .setMethod("POST")
                    .setUrl("$endpoint/tag/show")
                    .setParameter(mapOf("id" to response))
                    .build()

                val newTag = json.decodeFromString<Tag>(
                    string = nextcloudApi!!.performNetworkRequest(showRequest)
                        .bufferedReader()
                        .lines()
                        .collect(Collectors.joining("\n"))
                )

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
            val deleteRequest = NextcloudRequest.Builder()
                .setMethod("DELETE")
                .setUrl("$endpoint/password/delete")
                .setParameter(mapOf("id" to id))
                .build()

            try {
                nextcloudApi!!.performNetworkRequest(deleteRequest)

                storedPasswordsState.value.removeIf { it.id == id }
            } catch (e: Exception) {
                showError()
            }
        }
    }

    fun deleteFolderRequest(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val deleteRequest = NextcloudRequest.Builder()
                .setMethod("DELETE")
                .setUrl("$endpoint/folder/delete")
                .setParameter(mapOf("id" to id))
                .build()

            try {
                nextcloudApi!!.performNetworkRequest(deleteRequest)

                MainViewModel.setSelectedFolder(folder = MainViewModel.currentFolder.value)

                refreshServerList()
            } catch (e: Exception) {
                showError()
            }
        }
    }

    fun deleteTagRequest(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val deleteRequest = NextcloudRequest.Builder()
                .setMethod("DELETE")
                .setUrl("$endpoint/tag/delete")
                .setParameter(mapOf("id" to id))
                .build()

            try {
                nextcloudApi!!.performNetworkRequest(deleteRequest)

                storedTagsState.value.removeIf { it.id == id }

                refreshServerList()
            } catch (e: Exception) {
                showError()
            }
        }
    }

    /*TODO: wait for SSO to support PATCH method*/
    fun updatePasswordRequest(index: Int, params: MutableMap<String, String>) {
        val password = storedPasswordsState.value[index]

        viewModelScope.launch(Dispatchers.IO) {
            if (!params.containsKey(key = "password")) params["password"] = password.password
            if (!params.containsKey(key = "label")) params["label"] = password.label
            if (!params.containsKey(key = "username")) params["username"] = password.username
            if (!params.containsKey(key = "url")) params["url"] = password.url
            if (!params.containsKey(key = "notes")) params["notes"] = password.notes
            if (!params.containsKey(key = "folder")) params["folder"] = password.folder
            /*if (!params.containsKey(key = "tags")) {
                val tags = JsonArray()
                password.tags.forEach { tags.add(JsonParser.parseString(it["id"]!!)) }
                params["tags"] = tags.toString()
            }*/
            if (!params.containsKey(key = "customFields"))
                params["customFields"] =
                    json.encodeToString(password.customFieldsMap.toList().toString())
            params["hash"] = password.hash

            val createRequest = NextcloudRequest.Builder()
                .setMethod("POST")
                .setUrl("$endpoint/password/create")
                .setParameter(params)
                .build()

            try {
                val response = json.decodeFromString<JsonObject>(
                    string = nextcloudApi!!.performNetworkRequest(createRequest)
                        .bufferedReader()
                        .lines()
                        .collect(Collectors.joining("\n"))
                )["id"]!!.jsonPrimitive.content

                val showRequest = NextcloudRequest.Builder()
                    .setMethod("POST")
                    .setUrl("$endpoint/password/show")
                    .setParameter(mapOf("id" to response, "details" to "model+tags"))
                    .build()

                val updatedPassword = json.decodeFromString<Password>(
                    string = nextcloudApi!!.performNetworkRequest(showRequest)
                        .bufferedReader()
                        .lines()
                        .collect(Collectors.joining("\n"))
                )
                updatedPassword.index = index

                if (password.url != params["url"])
                    faviconRequest(data = updatedPassword)
                else updatedPassword.setFavicon(bitmap = password.favicon.value)

                storedPasswordsState.value[index] = updatedPassword

                MainViewModel.setRefreshing(refreshing = false)

                val deleteRequest = NextcloudRequest.Builder()
                    .setMethod("DELETE")
                    .setUrl("$endpoint/password/delete")
                    .setParameter(mapOf("id" to password.id))
                    .build()

                try {
                    nextcloudApi!!.performNetworkRequest(deleteRequest)
                } catch (e: Exception) {
                    showError()
                }
            } catch (e: Exception) {
                showError()
            }
        }
    }

    suspend fun generatePassword(): String {
        val generateRequest = NextcloudRequest.Builder()
            .setMethod("GET")
            .setUrl("$endpoint/service/password")
            .build()

        return withContext(Dispatchers.IO) {
            try {
                json.decodeFromString(
                    nextcloudApi!!.performNetworkRequest(generateRequest)
                        .bufferedReader()
                        .lines()
                        .collect(Collectors.joining("\n"))
                )
            } catch (e: Exception) {
                showError()

                ""
            }
        }
    }

    fun faviconRequest(data: Any) {
        when (data) {
            is Password -> viewModelScope.launch(Dispatchers.IO) {
                val faviconRequest = NextcloudRequest.Builder().setMethod("GET")
                    .setUrl("$endpoint/service/favicon/${Uri.parse(data.url).host ?: data.url}/256")
                    .build()

                try {
                    data.setFavicon(
                        BitmapFactory.decodeStream(
                            nextcloudApi!!.performNetworkRequest(faviconRequest)
                        ).toRoundedCorners()
                    )
                } catch (e: Exception) {
                }
            }
            is String -> viewModelScope.launch(Dispatchers.IO) {
                if (data.isNotEmpty()) {
                    val faviconRequest = NextcloudRequest.Builder().setMethod("GET")
                        .setUrl("$endpoint/service/favicon/${Uri.parse(data).host ?: data}/256")
                        .build()

                    try {
                        currentRequestedFaviconState.value = BitmapFactory.decodeStream(
                            nextcloudApi!!.performNetworkRequest(faviconRequest)
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
        ) {}

        MainViewModel.setRefreshing(refreshing = false)
    }

    fun stopNextcloudApi() {
        viewModelScope.coroutineContext.cancelChildren()
        context = null
        nextcloudApi?.stop()
    }
}