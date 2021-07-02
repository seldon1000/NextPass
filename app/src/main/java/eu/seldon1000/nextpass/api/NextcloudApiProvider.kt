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
import android.graphics.*
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.nextcloud.android.sso.AccountImporter
import com.nextcloud.android.sso.aidl.NextcloudRequest
import com.nextcloud.android.sso.api.NextcloudAPI
import com.nextcloud.android.sso.exceptions.AccountImportCancelledException
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppNotInstalledException
import com.nextcloud.android.sso.helper.SingleAccountHelper
import com.nextcloud.android.sso.model.SingleSignOnAccount
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.ui.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.stream.Collectors

@SuppressLint("StaticFieldLeak")
object NextcloudApiProvider : ViewModel() {
    private var context: Context? = null

    private const val endpoint = "/index.php/apps/passwords/api/1.0"
    private var nextcloudApi: NextcloudAPI? = null

    private val currentAccountState = MutableStateFlow<SingleSignOnAccount?>(value = null)
    val currentAccount = currentAccountState

    private val baseFolder =
        Folder(
            folderData = JsonParser.parseString("{\"id\":\"00000000-0000-0000-0000-000000000000\",\"label\":\"Base\",\"parent\":\"\",\"favorite\":\"false\",\"created\":\"0\",\"edited\":\"0\"}").asJsonObject,
            index = 0
        )

    private val storedPasswordsState =
        MutableStateFlow<SnapshotStateList<Password>>(value = mutableStateListOf())
    val storedPasswords = storedPasswordsState

    private val storedFoldersState = MutableStateFlow(value = mutableStateListOf(baseFolder))
    val storedFolders = storedFoldersState

    private val currentRequestedFaviconState = MutableStateFlow<Bitmap?>(value = null)
    val currentRequestedFavicon = currentRequestedFaviconState

    fun setContext(context: Any) {
        if (context is FragmentActivity || this.context == null)
            this.context = context as Context
    }

    private val connectedCallback: NextcloudAPI.ApiConnectedListener =
        object : NextcloudAPI.ApiConnectedListener {
            override fun onConnected() {}

            override fun onError(e: Exception) {
                showError()
            }
        }

    fun attemptLogin(): Boolean {
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
            body = context!!.getString(R.string.logout_body),
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
                body = context!!.getString(R.string.missing_nextcloud_body),
                confirm = true
            ) {
                context!!.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
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

    private fun listPasswordsRequest(): JsonArray {
        val listRequest = NextcloudRequest.Builder()
            .setMethod("GET")
            .setUrl("$endpoint/password/list")
            .build()

        return try {
            JsonParser.parseString(
                nextcloudApi!!.performNetworkRequest(listRequest)
                    .bufferedReader()
                    .lines()
                    .collect(Collectors.joining("\n"))
            ).asJsonArray
        } catch (e: Exception) {
            showError()

            JsonArray()
        }
    }

    private fun listFoldersRequest(): JsonArray {
        val listRequest = NextcloudRequest.Builder()
            .setMethod("GET")
            .setUrl("$endpoint/folder/list")
            .build()

        return try {
            JsonParser.parseString(
                nextcloudApi!!.performNetworkRequest(listRequest)
                    .bufferedReader()
                    .lines()
                    .collect(Collectors.joining("\n"))
            ).asJsonArray
        } catch (e: Exception) {
            showError()

            JsonArray()
        }
    }

    fun refreshServerList() {
        MainViewModel.setRefreshing(refreshing = true)

        viewModelScope.launch(Dispatchers.IO) {
            val passwords = listPasswordsRequest()
            val data = mutableStateListOf<Password>()

            passwords.sortedBy { it.asJsonObject.get("label").asString.lowercase() }
                .forEachIndexed { index, password ->
                    data.add(Password(passwordData = password.asJsonObject, index = index))

                    faviconRequest(data = data[index])
                }

            storedPasswordsState.value = data

            MainViewModel.setRefreshing(refreshing = false)
        }

        viewModelScope.launch(Dispatchers.IO) {
            val folders = listFoldersRequest()
            val data = mutableStateListOf<Folder>()

            folders.sortedBy { it.asJsonObject.get("label").asString.lowercase() }
                .forEachIndexed { index, folder ->
                    data.add(Folder(folderData = folder.asJsonObject, index = index + 1))
                }
            data.add(index = 0, element = baseFolder)

            storedFoldersState.value = data
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
                val response = JsonParser.parseString(
                    nextcloudApi!!.performNetworkRequest(createRequest)
                        .bufferedReader()
                        .lines()
                        .collect(Collectors.joining("\n"))
                ).asJsonObject.get("id").asString

                val listRequest = NextcloudRequest.Builder()
                    .setMethod("POST")
                    .setUrl("$endpoint/password/show")
                    .setParameter(mapOf("id" to response))
                    .build()

                val newPasswordData = JsonParser.parseString(
                    nextcloudApi!!.performNetworkRequest(listRequest)
                        .bufferedReader()
                        .lines()
                        .collect(Collectors.joining("\n"))
                ).asJsonObject

                val data = storedPasswordsState.value
                val newPassword = Password(passwordData = newPasswordData)

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
                val response = JsonParser.parseString(
                    nextcloudApi!!.performNetworkRequest(createRequest)
                        .bufferedReader()
                        .lines()
                        .collect(Collectors.joining("\n"))
                ).asJsonObject.get("id").asString

                val showRequest = NextcloudRequest.Builder()
                    .setMethod("POST")
                    .setUrl("$endpoint/folder/show")
                    .setParameter(mapOf("id" to response))
                    .build()

                val newFolder = JsonParser.parseString(
                    nextcloudApi!!.performNetworkRequest(showRequest)
                        .bufferedReader()
                        .lines()
                        .collect(Collectors.joining("\n"))
                ).asJsonObject

                val data = storedFoldersState.value

                data.removeAt(index = 0)
                data.add(element = Folder(folderData = newFolder!!))
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

    fun deletePasswordRequest(index: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val deleteRequest = NextcloudRequest.Builder()
                .setMethod("DELETE")
                .setUrl("$endpoint/password/delete")
                .setParameter(mapOf("id" to storedPasswordsState.value[index].id))
                .build()

            try {
                val passwords = storedPasswordsState.value

                nextcloudApi!!.performNetworkRequest(deleteRequest)

                passwords.removeAt(index = index)
                passwords.forEachIndexed { i, password -> password.index = i }

                storedPasswordsState.value = passwords
            } catch (e: Exception) {
                showError()
            }
        }
    }

    fun deleteFolderRequest(index: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val deleteRequest = NextcloudRequest.Builder()
                .setMethod("DELETE")
                .setUrl("$endpoint/folder/delete")
                .setParameter(mapOf("id" to storedFoldersState.value[index].id))
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
            if (!params.containsKey(key = "customFields"))
                params["customFields"] = JsonParser.parseString(
                    password.customFields.toList().toString()
                ).asJsonArray.toString()
            params["hash"] = password.hash

            val createRequest = NextcloudRequest.Builder()
                .setMethod("POST")
                .setUrl("$endpoint/password/create")
                .setParameter(params)
                .build()

            try {
                val response = JsonParser.parseString(
                    nextcloudApi!!.performNetworkRequest(createRequest)
                        .bufferedReader()
                        .lines()
                        .collect(Collectors.joining("\n"))
                ).asJsonObject.get("id").asString

                val showRequest = NextcloudRequest.Builder()
                    .setMethod("POST")
                    .setUrl("$endpoint/password/show")
                    .setParameter(mapOf("id" to response))
                    .build()

                val updatedPassword = Password(
                    passwordData = JsonParser.parseString(
                        nextcloudApi!!.performNetworkRequest(showRequest)
                            .bufferedReader()
                            .lines()
                            .collect(Collectors.joining("\n"))
                    ).asJsonObject, index = index
                )

                if (password.url != params["url"])
                    faviconRequest(data = updatedPassword)
                else updatedPassword.setFavicon(bitmap = password.favicon.value)

                storedPasswordsState.value[index] = updatedPassword

                MainViewModel.setRefreshing(refreshing = false)
            } catch (e: Exception) {
                showError()
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
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
        }
    }

    suspend fun generatePassword(): String {
        val generateRequest = NextcloudRequest.Builder()
            .setMethod("GET")
            .setUrl("$endpoint/service/password")
            .build()

        return withContext(Dispatchers.IO) {
            try {
                JsonParser.parseString(
                    nextcloudApi!!.performNetworkRequest(generateRequest)
                        .bufferedReader()
                        .lines()
                        .collect(Collectors.joining("\n"))
                ).asJsonObject.get("password").asString
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
            body = context!!.getString(R.string.error_body)
        ) {}

        MainViewModel.setRefreshing(refreshing = false)
    }

    fun stopNextcloudApi() {
        viewModelScope.coroutineContext.cancelChildren()
        context = null
        nextcloudApi?.stop()
    }
}