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

import android.annotation.SuppressLint
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
import io.ktor.client.features.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@SuppressLint("StaticFieldLeak")
class CentralAppControl(application: Application) : AndroidViewModel(application) {
    val nextcloudApi = NextcloudApi()

    private val context = getApplication<Application>().applicationContext
    private var sharedPreferences: SharedPreferences = context.getSharedPreferences("nextpass", 0)
    private var clipboardManager: ClipboardManager = context.getSystemService(ClipboardManager::class.java)
    private var autofillManager: AutofillManager = context.getSystemService(AutofillManager::class.java)

    private lateinit var snackbarHostState: SnackbarHostState
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var autofillIntent: Intent
    private var promptInfo: BiometricPrompt.PromptInfo
    private var unlocked = true
    private var pendingUnlockAction: (() -> Unit)? = null

    lateinit var navController: MutableStateFlow<NavController>

    val screenProtection = MutableStateFlow(value = false)
    val autofill = MutableStateFlow(value = false)
    val autostart = MutableStateFlow(value = false)
    val pinProtected = MutableStateFlow(value = false)
    val biometricProtected = MutableStateFlow(value = false)
    val biometricDismissed = MutableStateFlow(value = false)
    val lockTimeout = MutableStateFlow(value = (-1).toLong())
    val refreshing = MutableStateFlow(value = false)
    val folderMode = MutableStateFlow(value = false)
    val currentFolder = MutableStateFlow(value = 0)
    val selectedFolder = MutableStateFlow(value = currentFolder.value)
    val folders = MutableStateFlow(value = false)
    val tags = MutableStateFlow(value = false)
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

        if (sharedPreferences.contains("screen")) screenProtection.value = true

        if (autofillManager.hasEnabledAutofillServices()) {
            autostart.value = sharedPreferences.contains("autostart")

            if (autostart.value) autofillIntent =
                Intent(context, NextPassAutofillService::class.java)
        } else sharedPreferences.edit().remove("autostart").apply()

        if (sharedPreferences.contains("PIN")) {
            pinProtected.value = true
            lockTimeout.value = sharedPreferences.getLong("timeout", 0)
            if (lockTimeout.value != (-1).toLong()) unlocked = false
            biometricProtected.value = sharedPreferences.contains("biometric")
        }

        if (sharedPreferences.contains("folders")) {
            setFolderMode(mode = true)
            folders.value = true
        } else setFolderMode(mode = false)

        if (!sharedPreferences.contains("tags")) enableTags(refresh = false)
        else tags.value = sharedPreferences.getBoolean("tags", true)

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.access_nextpass))
            .setSubtitle(context.getString(R.string.access_nextpass_body))
            .setNegativeButtonText(context.getString(R.string.cancel))
            .build()
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
            stopAutofillService(show = false)
            disableAutostart()
            disableAutofill()
            disableScreenProtection(lock = false)
            disableFolders()
            enableTags()
            disablePin(lock = false)

            showSnackbar(message = context.getString(R.string.preferences_reset_snack))
        }

        if (pinProtected.value) lock(shouldRaiseBiometric = true)
        else {
            pendingUnlockAction!!()
            pendingUnlockAction = null
        }
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

    fun disableScreenProtection(lock: Boolean = true) {
        pendingUnlockAction = {
            (navController.value.context as MainActivity).window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            sharedPreferences.edit().remove("screen").apply()
            screenProtection.value = false
        }

        if (pinProtected.value && lock) lock(shouldRaiseBiometric = true)
        else {
            pendingUnlockAction!!()
            pendingUnlockAction = null
        }
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

    fun stopAutofillService(show: Boolean = true) {
        if (autofill.value) {
            context.stopService(autofillIntent)

            if (show) showSnackbar(message = context.getString(R.string.service_terminated_snack))
        } else if (show) showSnackbar(message = context.getString(R.string.service_not_enabled_snack))
    }

    fun enableAutofill() {
        autofill.value = true

        startAutofillService()
    }

    fun disableAutofill() {
        stopAutofillService()

        autofillManager.disableAutofillServices()

        autofill.value = false

        disableAutostart()
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

            if (biometricProtected.value) biometricDismissed.value = false

            refreshing.value = false
            navigate(route = Routes.AccessPin.getRoute(arg = shouldRaiseBiometric))
            dismissDialog()
        }
    }

    fun unlock() {
        unlocked = true

        if (navController.value.previousBackStackEntry?.destination?.route != Routes.Welcome.route)
            navController.value.popBackStack()
        else openApp()

        pendingUnlockAction?.let { it() }
        pendingUnlockAction = null
    }

    fun openApp(shouldRememberScreen: Boolean = false) {
        if (unlocked) {
            if (nextcloudApi.isLogged()) {
                if (!shouldRememberScreen) navigate(route = Routes.Passwords.route)

                executeRequest {
                    nextcloudApi.refreshServerList()

                    startAutofillService()
                }
            }
        } else navigate(route = Routes.AccessPin.getRoute(arg = true))
    }

    fun checkPin(pin: String): Boolean {
        return if (sharedPreferences.getString("PIN", null) == pin) {
            unlocked = true

            true
        } else false
    }

    fun changePin() {
        pendingUnlockAction = { navigate(route = Routes.Pin.route) }

        if (pinProtected.value) lock(shouldRaiseBiometric = true)
        else {
            pendingUnlockAction!!()
            pendingUnlockAction = {}
        }
    }

    fun setPin(pin: String) {
        sharedPreferences.edit().putString("PIN", pin).apply()

        if (!pinProtected.value) {
            pinProtected.value = true

            setLockTimeout(timeout = lockTimeout.value)
        }
    }

    fun disablePin(lock: Boolean = true) {
        pendingUnlockAction = {
            sharedPreferences.edit().remove("PIN").apply()
            pinProtected.value = false
            sharedPreferences.edit().remove("timeout").apply()
            lockTimeout.value = (-1).toLong()
            disableBiometric()

            showSnackbar(message = context.getString(R.string.pin_disabled_snack))
        }

        if (pinProtected.value && lock) lock(shouldRaiseBiometric = true)
        else {
            pendingUnlockAction!!()
            pendingUnlockAction = {}
        }
    }

    fun enableBiometric() {
        showBiometricPrompt(toEnable = true)
    }

    fun disableBiometric() {
        sharedPreferences.edit().remove("biometric").apply()
        biometricProtected.value = false
    }

    fun showBiometricPrompt(toEnable: Boolean = false) {
        biometricPrompt = BiometricPrompt(
            (context as MainActivity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    biometricDismissed.value = true
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    if (toEnable) {
                        sharedPreferences.edit().putBoolean("biometric", true).apply()
                        biometricProtected.value = true
                    } else unlock()
                }

                override fun onAuthenticationFailed() {
                    biometricDismissed.value = true
                }
            }
        )
        biometricPrompt.authenticate(promptInfo)
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
        if (navController.value.currentDestination?.route
                ?.substringBefore(delimiter = "/") != route.substringBefore(delimiter = "/")
        ) {
            navController.value.navigate(route = route) {
                launchSingleTop = true
                restoreState = true
            }

            if (navController.value.currentDestination?.route == Routes.AccessPin.route ||
                navController.value.currentDestination?.route == Routes.Welcome.route ||
                navController.value.currentDestination?.route == Routes.Settings.route ||
                navController.value.currentDestination?.route == Routes.About.route ||
                navController.value.currentDestination?.route == Routes.Pin.route
            ) refreshing.value = false
        }

        setKeyboardMode()
    }

    fun popBackStack(): Boolean {
        try {
            return if ((navController.value.previousBackStackEntry?.destination?.route == Routes.Welcome.route &&
                        navController.value.currentDestination?.route == Routes.WebView.route) ||
                navController.value.previousBackStackEntry?.destination?.route == Routes.AccessPin.route ||
                navController.value.currentDestination?.route == Routes.Welcome.route ||
                (navController.value.currentDestination?.route == Routes.AccessPin.route && pendingUnlockAction == null) ||
                navController.value.currentDestination?.route == Routes.Passwords.route
            )
                if (currentFolder.value != 0) {
                    setCurrentFolder()

                    true
                } else false
            else {
                if (navController.value.currentDestination?.route == Routes.NewPassword.route)
                    nextcloudApi.faviconRequest(data = "")

                navController.value.popBackStack()

                setKeyboardMode()

                true
            }
        } catch (e: Exception) {
            return false
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

        sharedPreferences.edit().putBoolean("tags", true).apply()
        tags.value = true
    }

    fun disableTags() {
        sharedPreferences.edit().putBoolean("tags", false).apply()
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

            viewModelScope.launch(context = Dispatchers.Main) {
                try {
                    val response = nextcloudApi.client.post<JsonObject>(urlString = url.value)

                    val login = Uri.encode(response["login"]!!.jsonPrimitive.content, "UTF-8")
                    val endpoint = response["poll"]!!.jsonObject["endpoint"]!!.jsonPrimitive.content
                    val token = response["poll"]!!.jsonObject["token"]!!.jsonPrimitive.content

                    navigate(route = Routes.WebView.getRoute(login))

                    var i = 0
                    var loginResponse = mapOf<String, String>()

                    while (loginResponse.isEmpty() && i <= 120) {
                        try {
                            loginResponse = nextcloudApi.client.post(urlString = endpoint) {
                                expectSuccess = false
                                parameter(key = "token", value = token)
                            }
                        } catch (e: Exception) {
                        }

                        delay(timeMillis = 500)
                        i++
                    }

                    if (sharedPreferences.contains("server")) popBackStack()

                    if (i > 120 &&
                        navController.value.currentDestination!!.route ==
                        Routes.WebView.getRoute(arg = login)
                    ) showDialog(
                        title = context.getString(R.string.timeout_expired),
                        body = {
                            Text(
                                text = context.getString(R.string.timeout_expired_body),
                                fontSize = 14.sp
                            )
                        })
                    else if (i <= 120) {
                        refreshing.value = true

                        nextcloudApi.login(
                            server = loginResponse["server"]!!,
                            loginName = loginResponse["loginName"]!!,
                            appPassword = loginResponse["appPassword"]!!
                        )

                        openApp(shouldRememberScreen = sharedPreferences.contains("server"))
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
            executeRequest {
                nextcloudApi.logout()

                sharedPreferences.edit().remove("server").apply()
                sharedPreferences.edit().remove("loginName").apply()
                sharedPreferences.edit().remove("appPassword").apply()

                refreshing.value = false
                resetUserPreferences()
                navigate(route = Routes.Welcome.route)
                showSnackbar(message = context.getString(R.string.disconnected_snack))
            }
        }
    }

    fun executeRequest(request: suspend () -> Unit) {
        refreshing.value =
            navController.value.currentDestination?.route == Routes.Search.route ||
                    navController.value.currentDestination?.route == Routes.Passwords.route ||
                    navController.value.currentDestination?.route == Routes.Favorites.route ||
                    navController.value.currentDestination?.route == Routes.PasswordDetails.route ||
                    navController.value.currentDestination?.route == Routes.FolderDetails.route ||
                    navController.value.currentDestination?.route == Routes.NewPassword.route ||
                    navController.value.currentDestination?.route == Routes.NewFolder.route

        viewModelScope.launch {
            try {
                request()
            } catch (e: Exception) {
                showError()
            }

            refreshing.value = false
        }
    }

    private fun showError() {
        showDialog(
            title = context.getString(R.string.error),
            body = { Text(text = context.getString(R.string.error_body), fontSize = 14.sp) }
        )

        refreshing.value = false
    }
}