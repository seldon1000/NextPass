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

package eu.seldon1000.nextpass

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.view.WindowManager
import android.view.autofill.AutofillManager
import android.webkit.CookieManager
import androidx.biometric.BiometricPrompt
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import eu.seldon1000.nextpass.api.NextcloudApi
import eu.seldon1000.nextpass.services.NextPassAutofillService
import eu.seldon1000.nextpass.services.StartupBroadcastReceiver
import eu.seldon1000.nextpass.ui.items.TextFieldItem
import eu.seldon1000.nextpass.ui.layout.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalUnsignedTypes::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    var nextcloudApi = NextcloudApi()
        private set

    private val context = getApplication<Application>()
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("nextpass", 0)
    private val clipboardManager: ClipboardManager =
        context.getSystemService(ClipboardManager::class.java)
    private val autofillManager: AutofillManager =
        context.getSystemService(AutofillManager::class.java)

    lateinit var navController: MutableStateFlow<NavController>
    private lateinit var snackbarHostState: SnackbarHostState

    private var unlocked = false
    private var pendingUnlockAction: (() -> Unit)? = null
    private var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var autofillIntent: Intent

    private val disablePinAction = {
        unlocked = true
        sharedPreferences.edit().remove("PIN").apply()
        pinProtected.value = false
        sharedPreferences.edit().remove("timeout").apply()
        lockTimeout.value = (-1).toLong()
        disableBiometric()
    }
    private val resetPreferencesAction = {
        disablePinAction()
        stopAutofillService(showSnackMessage = false)
        disableAutofill()
        disableScreenProtection()
        disableFolders()
        enableTags()
    }

    val screenProtection = MutableStateFlow(value = sharedPreferences.contains("screen"))
    val autofill = MutableStateFlow(value = false)
    val autostart = MutableStateFlow(value = sharedPreferences.contains("autostart"))
    val pinProtected = MutableStateFlow(value = sharedPreferences.contains("PIN"))
    val biometricProtected = MutableStateFlow(value = sharedPreferences.contains("biometric"))
    val biometricDismissed = MutableStateFlow(value = false)
    val lockTimeout = MutableStateFlow(value = sharedPreferences.getLong("timeout", 0))
    val refreshing = MutableStateFlow(value = false)
    val folderMode = MutableStateFlow(value = false)
    val currentFolder = MutableStateFlow(value = 0)
    val selectedFolder = MutableStateFlow(value = currentFolder.value)
    val folders = MutableStateFlow(value = sharedPreferences.contains("folders"))
    val tags = MutableStateFlow(value = !sharedPreferences.contains("tags"))
    val openDialog = MutableStateFlow(value = false)
    val dialogTitle = MutableStateFlow(value = "")
    val dialogBody = MutableStateFlow<@Composable () -> Unit> {}
    val dialogAction = MutableStateFlow {}
    val dialogConfirm = MutableStateFlow(value = false)

    init {
        if (sharedPreferences.contains("server"))
            nextcloudApi.login(
                server = sharedPreferences.getString("server", "")!!,
                loginName = sharedPreferences.getString("loginName", "")!!,
                appPassword = sharedPreferences.getString("appPassword", "")!!
            )

        unlocked = !pinProtected.value || lockTimeout.value == (-1).toLong()
        if (autostart.value) startAutofillService()
        if (folders.value) setFolderMode(mode = true)
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.access_nextpass))
            .setSubtitle(context.getString(R.string.access_nextpass_body))
            .setNegativeButtonText(context.getString(R.string.cancel))
            .build()

        //nextcloudApi.test()
    }

    fun setPrimaryClip(label: String, clip: String) {
        viewModelScope.launch {
            clipboardManager.setPrimaryClip(ClipData.newPlainText(label, clip))
        }

        showSnackbar(message = context.getString(R.string.copy_snack_message, label))
    }

    fun setNavController(controller: NavController) {
        navController = MutableStateFlow(value = controller)
    }

    fun setSnackbarHostState(snackbar: SnackbarHostState) {
        snackbarHostState = snackbar
    }

    fun resetUserPreferences() {
        pendingUnlockAction = {
            resetPreferencesAction()

            showSnackbar(message = context.getString(R.string.preferences_reset_snack))
        }

        lock()
    }

    fun showSnackbar(message: String) {
        viewModelScope.launch {
            if (navController.value.currentDestination?.route!! != Routes.AccessPin.route &&
                navController.value.currentDestination?.route!! != Routes.Pin.route
            ) {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(message = message)
            }
        }
    }

    fun enableScreenProtection() {
        sharedPreferences.edit().putBoolean("screen", true).apply()

        (navController.value.context as MainActivity).window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        screenProtection.value = true
    }

    fun disableScreenProtection() {
        pendingUnlockAction = {
            (navController.value.context as MainActivity).window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)

            sharedPreferences.edit().remove("screen").apply()
            screenProtection.value = false
        }

        lock()
    }

    private fun startAutofillService() {
        if (autofillManager.hasEnabledAutofillServices()) {
            if (!this::autofillIntent.isInitialized && StartupBroadcastReceiver.autofillIntent != null)
                autofillIntent = StartupBroadcastReceiver.autofillIntent!!
            else if (!this::autofillIntent.isInitialized)
                autofillIntent = Intent(context, NextPassAutofillService::class.java)

            autofill.value = true

            context.startService(autofillIntent)
        }
    }

    fun stopAutofillService(showSnackMessage: Boolean = true) {
        if (autofill.value) {
            context.stopService(autofillIntent)

            if (showSnackMessage) showSnackbar(message = context.getString(R.string.service_terminated_snack))
        } else if (showSnackMessage) showSnackbar(message = context.getString(R.string.service_not_enabled_snack))
    }

    fun enableAutofill() {
        autofill.value = true

        startAutofillService()
    }

    fun disableAutofill() {
        stopAutofillService()
        disableAutostart()

        autofillManager.disableAutofillServices()

        autofill.value = false
    }

    fun enableAutostart() {
        sharedPreferences.edit().putBoolean("autostart", true).apply()
        autostart.value = true
    }

    fun disableAutostart() {
        sharedPreferences.edit().remove("autostart").apply()
        autostart.value = false
    }

    fun lock(shouldRaiseBiometric: Boolean = true) {
        if (pinProtected.value) {
            unlocked = false
            biometricDismissed.value = false

            navigate(route = Routes.AccessPin.getRoute(arg = shouldRaiseBiometric))
        } else {
            pendingUnlockAction?.let { it() }
            pendingUnlockAction = null
        }
    }

    fun unlock(pin: String? = null) {
        if (pin != null && pin != sharedPreferences.getString("PIN", null)) showDialog(
            title = context.getString(R.string.wrong_pin),
            body = {
                Text(
                    text = context.getString(R.string.wrong_pin_body),
                    fontSize = 14.sp
                )
            }
        )

        unlocked = unlocked || (pin != null && pin == sharedPreferences.getString("PIN", null))

        if (unlocked) {
            if (navController.value.previousBackStackEntry != null) navController.value.popBackStack()

            if (pendingUnlockAction != null) {
                pendingUnlockAction!!()
                pendingUnlockAction = null
            } else if (nextcloudApi.isLogged()) executeRequest { nextcloudApi.refreshServerList() }
        } else lock()
    }

    fun changePin() {
        pendingUnlockAction = { navigate(route = Routes.Pin.route) }

        lock()
    }

    fun setPin(pin: String) {
        if (!pinProtected.value) setLockTimeout(timeout = 0)

        sharedPreferences.edit().putString("PIN", pin).apply()
        pinProtected.value = true
    }

    fun disablePin() {
        pendingUnlockAction = {
            disablePinAction()

            showSnackbar(message = context.getString(R.string.pin_disabled_snack))
        }

        lock()
    }

    fun enableBiometric() {
        showBiometricPrompt(toEnable = true)
    }

    fun disableBiometric() {
        sharedPreferences.edit().remove("biometric").apply()
        biometricProtected.value = false
    }

    fun showBiometricPrompt(toEnable: Boolean = false) {
        BiometricPrompt(
            (navController.value.context as MainActivity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    biometricDismissed.value = true
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    biometricDismissed.value = true

                    if (toEnable) {
                        sharedPreferences.edit().putBoolean("biometric", true).apply()
                        biometricProtected.value = true
                    } else {
                        unlocked = true
                        unlock()
                    }
                }

                override fun onAuthenticationFailed() {
                    biometricDismissed.value = true
                }
            }
        ).authenticate(promptInfo)
    }

    fun setLockTimeout(timeout: Long) {
        sharedPreferences.edit().putLong("timeout", timeout).apply()
        lockTimeout.value = timeout
    }

    private fun setKeyboardMode() {
        if (navController.value.currentDestination?.route == Routes.Search.route ||
            navController.value.currentDestination?.route == Routes.Pin.route
        ) (navController.value.context as MainActivity).window.setSoftInputMode(16)
        else (navController.value.context as MainActivity).window.setSoftInputMode(32)
    }

    fun navigate(route: String) {
        if (navController.value.currentDestination?.route?.substringBefore(delimiter = "/") !=
            route.substringBefore(delimiter = "/")
        )
            navController.value.navigate(route = route) {
                launchSingleTop = true
                restoreState = true
            }

        refreshing.value = false

        setKeyboardMode()
    }

    fun popBackStack(): Boolean {
        return if (navController.value.currentDestination?.route == Routes.Welcome.route ||
            (navController.value.currentDestination?.route == Routes.AccessPin.route && pendingUnlockAction == null && !unlocked) ||
            navController.value.currentDestination?.route == Routes.Passwords.route
        )
            if (currentFolder.value != 0) {
                setCurrentFolder()

                true
            } else false
        else {
            navController.value.popBackStack()

            nextcloudApi.faviconRequest(data = "")

            setKeyboardMode()

            true
        }
    }

    fun setFolderMode(mode: Boolean? = null) {
        if (mode == null) folderMode.value = !folderMode.value
        else folderMode.value = mode
    }

    fun enableFolders() {
        sharedPreferences.edit().putBoolean("folders", false).apply()
        folders.value = true

        setFolderMode(mode = true)
    }

    fun disableFolders() {
        sharedPreferences.edit().remove("folders").apply()
        folders.value = false

        setFolderMode(mode = false)
    }

    fun setCurrentFolder(folder: Int? = null) {
        if (folder != null) {
            currentFolder.value = folder
            selectedFolder.value = folder
        } else {
            currentFolder.value =
                nextcloudApi.storedFolders.value.indexOfFirst {
                    it.id == nextcloudApi.storedFolders.value[currentFolder.value].parent
                }
            selectedFolder.value = currentFolder.value
        }
    }

    fun setSelectedFolder(folder: Int) {
        selectedFolder.value = folder
    }

    fun enableTags(refresh: Boolean = true) {
        if (refresh) executeRequest { nextcloudApi.refreshServerList() }

        sharedPreferences.edit().remove("tags").apply()
        tags.value = true
    }

    fun disableTags() {
        sharedPreferences.edit().putBoolean("tags", true).apply()
        tags.value = false
    }

    fun showDialog(
        title: String,
        body: @Composable () -> Unit,
        confirm: Boolean = false,
        action: () -> Unit = {}
    ) {
        dialogTitle.value = title
        dialogBody.value = body
        dialogConfirm.value = confirm
        dialogAction.value = action
        openDialog.value = true
    }

    fun dismissDialog() {
        openDialog.value = false
    }

    @ExperimentalMaterialApi
    fun attemptLogin() {
        val url = mutableStateOf(value = "")

        showDialog(
            title = context.getString(R.string.insert_server_url),
            body = {
                TextFieldItem(
                    text = url.value,
                    onTextChanged = { url.value = it },
                    label = context.getString(R.string.url),
                    required = true
                )
            },
            confirm = true
        ) {
            if (!url.value.startsWith(prefix = "https://") &&
                !url.value.startsWith(prefix = "http://")
            ) url.value = "https://${url.value}"
            url.value = "${url.value}/index.php/login/v2"

            executeRequest(stopRefreshing = false) {
                val response = nextcloudApi.startLogin(url = url.value)

                navigate(
                    route = Routes.WebView.getRoute(
                        arg = Uri.encode(response["login"]!!.jsonPrimitive.content, "UTF-8")
                    )
                )

                val loginResponse = nextcloudApi.loginPolling(
                    endpoint = response["poll"]!!.jsonObject["endpoint"]!!.jsonPrimitive.content,
                    token = response["poll"]!!.jsonObject["token"]!!.jsonPrimitive.content
                )

                if (loginResponse.isEmpty()) {
                    popBackStack()

                    showDialog(
                        title = context.getString(R.string.timeout_expired),
                        body = {
                            Text(
                                text = context.getString(R.string.timeout_expired_body),
                                fontSize = 14.sp
                            )
                        })
                } else {
                    nextcloudApi.login(
                        server = loginResponse["server"]!!,
                        loginName = loginResponse["loginName"]!!,
                        appPassword = loginResponse["appPassword"]!!
                    )

                    unlock()
                    showSnackbar(
                        message = context.getString(
                            R.string.connected_snack,
                            loginResponse["loginName"]!!
                        )
                    )

                    sharedPreferences.edit().putString(
                        "server",
                        loginResponse["server"]!!
                    ).apply()
                    sharedPreferences.edit().putString(
                        "loginName",
                        loginResponse["loginName"]!!
                    ).apply()
                    sharedPreferences.edit().putString(
                        "appPassword",
                        loginResponse["appPassword"]!!
                    ).apply()
                }

                CookieManager.getInstance().removeAllCookies {}
            }
        }
    }

    fun attemptLogout() {
        showDialog(
            title = context.getString(R.string.logout),
            body = {
                Text(
                    text = context.getString(R.string.logout_body),
                    fontSize = 14.sp
                )
            },
            confirm = true
        ) {
            pendingUnlockAction = {
                executeRequest {
                    nextcloudApi.logout()
                    nextcloudApi = NextcloudApi()

                    navigate(route = Routes.Welcome.route)
                    resetPreferencesAction()
                    showSnackbar(message = context.getString(R.string.disconnected_snack))

                    sharedPreferences.edit().remove("server").apply()
                    sharedPreferences.edit().remove("loginName").apply()
                    sharedPreferences.edit().remove("appPassword").apply()
                }
            }

            lock()
        }
    }

    fun executeRequest(stopRefreshing: Boolean = true, request: suspend () -> Unit) {
        refreshing.value = navController.value.currentDestination?.route == Routes.Search.route ||
                navController.value.currentDestination?.route == Routes.Passwords.route ||
                navController.value.currentDestination?.route == Routes.Favorites.route ||
                navController.value.currentDestination?.route == Routes.PasswordDetails.route ||
                navController.value.currentDestination?.route == Routes.FolderDetails.route ||
                navController.value.currentDestination?.route == Routes.NewPassword.route ||
                navController.value.currentDestination?.route == Routes.NewFolder.route

        viewModelScope.launch {
            try {
                request()

                if (autofillManager.hasEnabledAutofillServices())
                    NextPassAutofillService.nextcloudApi = nextcloudApi
            } catch (e: Exception) {
                showDialog(
                    title = context.getString(R.string.error),
                    body = { Text(text = context.getString(R.string.error_body), fontSize = 14.sp) }
                )
            }

            refreshing.value = !stopRefreshing
        }
    }
}