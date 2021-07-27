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
import android.content.*
import android.net.Uri
import android.view.Window
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
import androidx.navigation.NavController
import eu.seldon1000.nextpass.api.NextcloudApi
import eu.seldon1000.nextpass.services.NextPassAutofillService
import eu.seldon1000.nextpass.ui.items.TextFieldItem
import eu.seldon1000.nextpass.ui.layout.Routes
import io.ktor.client.features.*
import io.ktor.client.request.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@SuppressLint("StaticFieldLeak")
object CentralAppControl {
    private val coroutineScope = CoroutineScope(context = Dispatchers.Unconfined)

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var window: Window
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var autofillManager: AutofillManager

    private lateinit var snackbarHostState: SnackbarHostState
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var autofillIntent: Intent
    private var unlocked = true
    private var pendingUnlockAction: (() -> Unit)? = null

    private lateinit var navControllerState: MutableStateFlow<NavController>
    lateinit var navController: MutableStateFlow<NavController>

    private val screenProtectionState = MutableStateFlow(value = false)
    val screenProtection = screenProtectionState

    private val autofillState = MutableStateFlow(value = false)
    val autofill = autofillState

    private val autostartState = MutableStateFlow(value = false)
    val autostart = autostartState

    private val pinProtectedState = MutableStateFlow(value = false)
    val pinProtected = pinProtectedState

    private val biometricProtectedState = MutableStateFlow(value = false)
    val biometricProtected = biometricProtectedState

    private val biometricDismissedState = MutableStateFlow(value = false)
    val biometricDismissed = biometricDismissedState

    private val lockTimeoutState = MutableStateFlow(value = (-1).toLong())
    val lockTimeout = lockTimeoutState

    private val refreshingState = MutableStateFlow(value = false)
    val refreshing: StateFlow<Boolean> = refreshingState

    private val folderModeState = MutableStateFlow(value = false)
    val folderMode = folderModeState

    private val currentFolderState = MutableStateFlow(value = 0)
    val currentFolder = currentFolderState

    private val selectedFolderState = MutableStateFlow(value = currentFolder.value)
    val selectedFolder = selectedFolderState

    private val foldersState = MutableStateFlow(value = false)
    val folders = foldersState

    private val tagsState = MutableStateFlow(value = false)
    val tags = tagsState

    private val openDialogState = MutableStateFlow(value = false)
    val openDialog = openDialogState

    private val dialogTitleState = MutableStateFlow(value = "")
    val dialogTitle = dialogTitleState

    private val dialogBodyState = MutableStateFlow<@Composable () -> Unit> {}
    val dialogBody = dialogBodyState

    private val dialogActionState = MutableStateFlow {}
    val dialogAction = dialogActionState

    private val dialogConfirmState = MutableStateFlow(value = false)
    val dialogConfirm = dialogConfirmState

    fun setContext(con: Context) {
        context = con
        sharedPreferences = context.getSharedPreferences("nextpass", 0)
        window = (context as MainActivity).window
        clipboardManager = context.getSystemService(ClipboardManager::class.java)
        autofillManager = context.getSystemService(AutofillManager::class.java)

        if (sharedPreferences.contains("server"))
            NextcloudApi.login(
                server = sharedPreferences.getString("server", "")!!,
                loginName = sharedPreferences.getString("loginName", "")!!,
                appPassword = sharedPreferences.getString("appPassword", "")!!
            )

        screenProtectionState.value = sharedPreferences.contains("screen")
        if (screenProtectionState.value) enableScreenProtection()

        if (autofillManager.hasEnabledAutofillServices()) {
            autostartState.value = sharedPreferences.contains("autostart")

            if (autostartState.value) autofillIntent =
                Intent(context, NextPassAutofillService::class.java)
        } else sharedPreferences.edit().remove("autostart").apply()

        if (sharedPreferences.contains("PIN")) {
            pinProtectedState.value = true
            lockTimeoutState.value = sharedPreferences.getLong("timeout", 0)
            if (lockTimeoutState.value != (-1).toLong()) unlocked = false
            biometricProtectedState.value = sharedPreferences.contains("biometric")
        }

        if (sharedPreferences.contains("folders")) {
            setFolderMode(mode = true)
            foldersState.value = true
        } else setFolderMode(mode = false)

        if (!sharedPreferences.contains("tags")) enableTags(refresh = false)
        else tagsState.value = sharedPreferences.getBoolean("tags", true)

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.access_nextpass))
            .setSubtitle(context.getString(R.string.access_nextpass_body))
            .setNegativeButtonText(context.getString(R.string.cancel))
            .build()
    }

    fun setPrimaryClip(label: String, clip: String) {
        coroutineScope.launch {
            clipboardManager.setPrimaryClip(ClipData.newPlainText(label, clip))
        }

        showSnackbar(message = context.getString(R.string.copy_snack_message, label))
    }

    fun setNavController(controller: NavController) {
        navControllerState = MutableStateFlow(value = controller)
        navController = navControllerState
    }

    fun setSnackbarHostState(snackbar: SnackbarHostState) {
        snackbarHostState = snackbar
    }

    fun resetUserPreferences(context: Context) {
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

        if (pinProtectedState.value) lock(shouldRaiseBiometric = true)
        else {
            pendingUnlockAction!!()
            pendingUnlockAction = null
        }
    }

    fun showSnackbar(message: String) {
        coroutineScope.launch {
            if (navControllerState.value.currentDestination?.route!! != Routes.AccessPin.route &&
                navControllerState.value.currentDestination?.route!! != Routes.Pin.route
            ) {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(message = message)
            }
        }
    }

    fun enableScreenProtection() {
        sharedPreferences.edit().putBoolean("screen", true).apply()

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        screenProtectionState.value = true
    }

    fun disableScreenProtection(lock: Boolean = true) {
        pendingUnlockAction = {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            sharedPreferences.edit().remove("screen").apply()
            screenProtectionState.value = false
        }

        if (pinProtectedState.value && lock) lock(shouldRaiseBiometric = true)
        else {
            pendingUnlockAction!!()
            pendingUnlockAction = null
        }
    }

    fun startAutofillService() {
        if (autofillManager.hasEnabledAutofillServices() && NextcloudApi.isLogged()) {
            if (!this::autofillIntent.isInitialized)
                autofillIntent = Intent(context, NextPassAutofillService::class.java)

            autofillState.value = true

            context.startService(autofillIntent)
        }
    }

    fun stopAutofillService(show: Boolean = true) {
        if (autofillState.value) {
            context.stopService(autofillIntent)

            if (show) showSnackbar(message = context.getString(R.string.service_terminated_snack))
        } else if (show) showSnackbar(message = context.getString(R.string.service_not_enabled_snack))
    }

    fun enableAutofill() {
        autofillState.value = true

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
        autostartState.value = true
    }

    fun disableAutostart() {
        sharedPreferences.edit().remove("autostart").apply()
        autostartState.value = false
    }

    fun lock(shouldRaiseBiometric: Boolean = true) {
        if (pinProtectedState.value) {
            unlocked = false

            if (biometricProtectedState.value) biometricDismissedState.value = false

            refreshingState.value = false
            navigate(route = Routes.AccessPin.getRoute(arg = shouldRaiseBiometric))
            dismissDialog()
        }
    }

    fun unlock() {
        unlocked = true

        if (navControllerState.value.previousBackStackEntry?.destination?.route != Routes.Welcome.route)
            navControllerState.value.popBackStack()
        else openApp()

        pendingUnlockAction?.let { it() }
        pendingUnlockAction = null
    }

    fun openApp(shouldRememberScreen: Boolean = false) {
        if (unlocked) {
            if (NextcloudApi.isLogged()) {
                if (!shouldRememberScreen) navigate(route = Routes.Passwords.route)

                executeRequest {
                    NextcloudApi.refreshServerList { showError() }

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

        if (pinProtectedState.value) lock(shouldRaiseBiometric = true)
        else {
            pendingUnlockAction!!()
            pendingUnlockAction = {}
        }
    }

    fun setPin(pin: String) {
        sharedPreferences.edit().putString("PIN", pin).apply()

        if (!pinProtectedState.value) {
            pinProtectedState.value = true

            setLockTimeout(timeout = lockTimeoutState.value)
        }
    }

    fun disablePin(lock: Boolean = true) {
        pendingUnlockAction = {
            sharedPreferences.edit().remove("PIN").apply()
            pinProtectedState.value = false
            sharedPreferences.edit().remove("timeout").apply()
            lockTimeoutState.value = (-1).toLong()
            disableBiometric()
        }

        if (pinProtectedState.value && lock) lock(shouldRaiseBiometric = true)
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
        biometricProtectedState.value = false
    }

    fun showBiometricPrompt(toEnable: Boolean = false) {
        biometricPrompt = BiometricPrompt(
            (context as MainActivity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    biometricDismissedState.value = true
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    if (toEnable) {
                        sharedPreferences.edit().putBoolean("biometric", true).apply()
                        biometricProtectedState.value = true
                    } else unlock()
                }

                override fun onAuthenticationFailed() {
                    biometricDismissedState.value = true
                }
            }
        )
        biometricPrompt.authenticate(promptInfo)
    }

    fun setLockTimeout(timeout: Long) {
        sharedPreferences.edit().putLong("timeout", timeout).apply()
        lockTimeoutState.value = timeout
    }

    fun shouldShowRefresh(): Boolean {
        return navControllerState.value.currentDestination?.route?.contains(other = Routes.AccessPin.route) == false &&
                navControllerState.value.currentDestination?.route?.contains(other = Routes.WebView.route) == false &&
                navControllerState.value.currentDestination?.route!! != Routes.AccessPin.route &&
                navControllerState.value.currentDestination?.route!! != Routes.Welcome.route &&
                navControllerState.value.currentDestination?.route!! != Routes.About.route &&
                navControllerState.value.currentDestination?.route!! != Routes.Pin.route
    }

    private fun setKeyboardMode() {
        if (navControllerState.value.currentDestination?.route!! == Routes.Search.route ||
            navControllerState.value.currentDestination?.route!! == Routes.Pin.route
        ) window.setSoftInputMode(16)
        else window.setSoftInputMode(32)
    }

    fun navigate(route: String) {
        if (navControllerState.value.currentDestination?.route!!
                .substringBefore(delimiter = "/") != route.substringBefore(delimiter = "/")
        ) {
            navControllerState.value.navigate(route = route) {
                launchSingleTop = true
                restoreState = true
            }

            if (navControllerState.value.currentDestination?.route!! == Routes.AccessPin.route ||
                navControllerState.value.currentDestination?.route!! == Routes.Welcome.route ||
                navControllerState.value.currentDestination?.route!! == Routes.Settings.route ||
                navControllerState.value.currentDestination?.route!! == Routes.About.route ||
                navControllerState.value.currentDestination?.route!! == Routes.Pin.route
            ) refreshingState.value = false
        }

        setKeyboardMode()
    }

    fun popBackStack(): Boolean {
        try {
            return if ((navControllerState.value.previousBackStackEntry?.destination?.route!! == Routes.Welcome.route &&
                        navControllerState.value.currentDestination?.route!! == Routes.WebView.route) ||
                navControllerState.value.previousBackStackEntry?.destination?.route!! == Routes.AccessPin.route ||
                navControllerState.value.currentDestination?.route!! == Routes.Welcome.route ||
                (navControllerState.value.currentDestination?.route!! == Routes.AccessPin.route && pendingUnlockAction == null) ||
                navControllerState.value.currentDestination?.route!! == Routes.Passwords.route
            )
                if (currentFolderState.value != 0) {
                    setCurrentFolder()
                    true
                } else false
            else {
                navControllerState.value.popBackStack()

                if (navControllerState.value.currentDestination?.route!! != Routes.NewPassword.route)
                    NextcloudApi.faviconRequest(data = "")

                setKeyboardMode()

                true
            }
        } catch (e: Exception) {
            return false
        }
    }

    fun setFolderMode(mode: Boolean? = null) {
        if (mode == null) folderModeState.value = !folderModeState.value
        else folderModeState.value = mode
    }

    fun enableFolders() {
        sharedPreferences.edit().putBoolean("folders", false).apply()
        foldersState.value = true

        setFolderMode(mode = true)
    }

    fun disableFolders() {
        sharedPreferences.edit().remove("folders").apply()
        foldersState.value = false

        setFolderMode(mode = false)
    }

    fun setCurrentFolder(folder: Int? = null) {
        if (folder != null) {
            currentFolderState.value = folder
            selectedFolderState.value = folder
        } else {
            currentFolderState.value =
                NextcloudApi.storedFolders.value.indexOfFirst {
                    it.id == NextcloudApi.storedFolders.value[currentFolderState.value].parent
                }
            selectedFolderState.value = currentFolderState.value
        }
    }

    fun setSelectedFolder(folder: Int) {
        selectedFolderState.value = folder
    }

    fun enableTags(refresh: Boolean = true) {
        if (refresh) executeRequest { NextcloudApi.refreshServerList { showError() } }

        sharedPreferences.edit().putBoolean("tags", true).apply()
        tagsState.value = true
    }

    fun disableTags() {
        sharedPreferences.edit().putBoolean("tags", false).apply()
        tagsState.value = false
    }

    fun showDialog(
        title: String,
        body: @Composable () -> Unit,
        confirm: Boolean = false,
        action: () -> Unit = {}
    ) {
        dialogTitleState.value = title
        dialogBodyState.value = body
        dialogConfirmState.value = confirm
        dialogActionState.value = action

        openDialogState.value = true
    }

    fun dismissDialog() {
        openDialogState.value = false
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

            coroutineScope.launch(context = Dispatchers.Main) {
                try {
                    val response = NextcloudApi.client.post<JsonObject>(urlString = url.value)

                    val login = Uri.encode(response["login"]!!.jsonPrimitive.content, "UTF-8")
                    val endpoint = response["poll"]!!.jsonObject["endpoint"]!!.jsonPrimitive.content
                    val token = response["poll"]!!.jsonObject["token"]!!.jsonPrimitive.content

                    navigate(route = Routes.WebView.getRoute(login))

                    var i = 0
                    var loginResponse = mapOf<String, String>()

                    while (loginResponse.isEmpty() && i <= 120) {
                        try {
                            loginResponse = NextcloudApi.client.post(urlString = endpoint) {
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
                        NextcloudApi.login(
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
            NextcloudApi.logout { showError() }

            sharedPreferences.edit().remove("server").apply()
            sharedPreferences.edit().remove("loginName").apply()
            sharedPreferences.edit().remove("appPassword").apply()

            refreshingState.value = false
            resetUserPreferences(context = context)
            navigate(route = Routes.Welcome.route)
            showSnackbar(message = context.getString(R.string.disconnected_snack))
        }
    }

    fun executeRequest(request: suspend (() -> Unit) -> Any) {
        refreshingState.value = shouldShowRefresh()

        coroutineScope.launch {
            request { showError() }

            refreshingState.value = false
        }
    }

    private fun showError() {
        showDialog(
            title = context.getString(R.string.error),
            body = { Text(text = context.getString(R.string.error_body), fontSize = 14.sp) }
        )

        refreshingState.value = false
    }
}