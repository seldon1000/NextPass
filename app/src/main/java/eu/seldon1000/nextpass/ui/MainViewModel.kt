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

package eu.seldon1000.nextpass.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.SharedPreferences
import android.view.WindowManager
import android.view.autofill.AutofillManager
import androidx.biometric.BiometricPrompt
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.api.NextcloudApiProvider
import eu.seldon1000.nextpass.services.NextPassAutofillService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@SuppressLint("StaticFieldLeak")
object MainViewModel : ViewModel() {
    private var context: FragmentActivity? = null
    private var sharedPreferences: SharedPreferences? = null

    private var clipboardManager: ClipboardManager? = null
    private var autofillManager: AutofillManager? = null

    private var autofillIntent: Intent? = null

    private var navControllerState = MutableStateFlow<NavController?>(value = null)
    val navController = navControllerState
    private var snackbarHostState: SnackbarHostState? = null

    private var pendingUnlockAction = {}

    private val screenProtectionState = MutableStateFlow(value = false)
    val screenProtection = screenProtectionState

    private val autofillState = MutableStateFlow(value = false)
    val autofill = autofillState

    private val autostartState = MutableStateFlow(value = false)
    val autostart = autostartState

    private val unlockedState = MutableStateFlow(value = true)
    val unlocked = unlockedState

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

    private var promptInfo: BiometricPrompt.PromptInfo? = null
    private var biometricPrompt: BiometricPrompt? = null

    val pickerColors = listOf(
        Color(android.graphics.Color.parseColor("#d50000")),
        Color(android.graphics.Color.parseColor("#c41061")),
        Color(android.graphics.Color.parseColor("#aa00ff")),
        Color(android.graphics.Color.parseColor("#6200ea")),
        Color(android.graphics.Color.parseColor("#304ffe")),
        Color(android.graphics.Color.parseColor("#2962ff")),
        Color(android.graphics.Color.parseColor("#0091ea")),
        Color(android.graphics.Color.parseColor("#00b8d4")),
        Color(android.graphics.Color.parseColor("#00bfa5")),
        Color(android.graphics.Color.parseColor("#00c853")),
        Color(android.graphics.Color.parseColor("#64dd17")),
        Color(android.graphics.Color.parseColor("#aeea00")),
        Color(android.graphics.Color.parseColor("#ffd600")),
        Color(android.graphics.Color.parseColor("#ffab00")),
        Color(android.graphics.Color.parseColor("#ff6d00")),
        Color(android.graphics.Color.parseColor("#dd2c00")),
        Color(android.graphics.Color.parseColor("#4e342e")),
        Color(android.graphics.Color.parseColor("#37474f"))
    )

    fun setContext(context: FragmentActivity) {
        this.context = context

        NextcloudApiProvider.setContext(context = this.context!!)

        sharedPreferences = this.context!!.getSharedPreferences("nextpass", 0)

        clipboardManager = this.context!!.getSystemService(ClipboardManager::class.java)
        autofillManager = this.context!!.getSystemService(AutofillManager::class.java)

        screenProtectionState.value = sharedPreferences!!.contains("screen")
        if (screenProtectionState.value) enableScreenProtection()

        if (autofillManager!!.hasEnabledAutofillServices())
            autostartState.value = sharedPreferences!!.contains("autostart")
        else sharedPreferences!!.edit().remove("autostart").apply()

        if (sharedPreferences!!.contains("PIN")) {
            pinProtectedState.value = true
            lockTimeoutState.value = sharedPreferences!!.getLong("timeout", 0)
            if (lockTimeoutState.value != (-1).toLong()) unlockedState.value = false
            biometricProtectedState.value = sharedPreferences!!.contains("biometric")
        }

        if (!sharedPreferences!!.contains("tags")) enableTags(refresh = false)
        else tagsState.value = sharedPreferences!!.getBoolean("tags", true)

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(MainViewModel.context!!.getString(R.string.access_nextpass))
            .setSubtitle(MainViewModel.context!!.getString(R.string.access_nextpass_body))
            .setNegativeButtonText(MainViewModel.context!!.getString(R.string.cancel))
            .build()
    }

    fun resetUserSettings() {
        stopAutofillService()
        disablePin()
        disableAutostart()
        disableScreenProtection()
    }

    fun setPrimaryClip(label: String, clip: String) {
        viewModelScope.launch {
            clipboardManager!!.setPrimaryClip(ClipData.newPlainText(label, clip))
        }

        showSnackbar(message = context!!.getString(R.string.copy_snack_message, label))
    }

    fun setAutofillIntent(intent: Intent): Boolean {
        return if (autofillIntent == null) {
            autofillIntent = intent
            true
        } else false
    }

    fun setNavController(navController: NavController) {
        navControllerState.value = navController
    }

    fun setSnackbarHostState(snackbar: SnackbarHostState) {
        snackbarHostState = snackbar
    }

    fun showSnackbar(message: String) {
        viewModelScope.launch {
            if (navControllerState.value?.currentDestination?.route!! != "access_pin/{shouldRaiseBiometric}" &&
                navControllerState.value?.currentDestination?.route!! != "pin"
            ) {
                snackbarHostState?.currentSnackbarData?.dismiss()
                snackbarHostState?.showSnackbar(message = message)
            }
        }
    }

    fun enableScreenProtection() {
        sharedPreferences!!.edit().putBoolean("screen", true).apply()

        context!!.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        screenProtectionState.value = true
    }

    fun disableScreenProtection() {
        pendingUnlockAction = {
            context!!.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            sharedPreferences!!.edit().remove("screen").apply()
            screenProtectionState.value = false
        }

        if (pinProtectedState.value) lock(shouldRaiseBiometric = true)
        else {
            pendingUnlockAction()
            pendingUnlockAction = {}
        }
    }

    private fun startAutofillService(): Boolean {
        return if (autofillManager!!.hasEnabledAutofillServices() &&
            NextcloudApiProvider.attemptLogin() && setAutofillIntent(
                intent = Intent(context, NextPassAutofillService::class.java)
            )
        ) {
            autofillState.value = true

            context!!.startService(autofillIntent)

            true
        } else false
    }

    fun stopAutofillService() {
        if (!autofillState.value)
            showSnackbar(message = context!!.getString(R.string.service_not_enabled_snack))
        else {
            context!!.stopService(autofillIntent)

            showSnackbar(message = context!!.getString(R.string.service_terminated_snack))
        }
    }

    fun enableAutofill() {
        autofillState.value = true

        startAutofillService()
    }

    fun disableAutofill() {
        stopAutofillService()

        autofillManager!!.disableAutofillServices()

        autofill.value = false

        disableAutostart()
    }

    fun enableAutostart() {
        sharedPreferences!!.edit().putBoolean("autostart", true).apply()
        autostartState.value = true
    }

    fun disableAutostart() {
        sharedPreferences!!.edit().remove("autostart").apply()
        autostartState.value = false
    }

    fun lock(shouldRaiseBiometric: Boolean = true) {
        if (pinProtectedState.value) {
            unlockedState.value = false

            if (biometricProtectedState.value) biometricDismissedState.value = false

            setRefreshing(refreshing = false)
            navigate(route = "access_pin/$shouldRaiseBiometric")
        }
    }

    fun unlock() {
        unlockedState.value = true

        if (navControllerState.value?.previousBackStackEntry?.destination?.route != "welcome") {
            navControllerState.value?.popBackStack(
                route = navControllerState.value?.currentBackStackEntry?.destination?.route!!,
                inclusive = true
            )
        } else openApp()

        pendingUnlockAction()
        pendingUnlockAction = {}
    }

    fun openApp(shouldRememberScreen: Boolean = false) {
        if (unlockedState.value) {
            if (NextcloudApiProvider.attemptLogin()) {
                startAutofillService()

                if (!shouldRememberScreen) navigate(route = "passwords")

                NextcloudApiProvider.refreshServerList()
            }
        } else navigate(route = "access_pin/true")
    }

    fun checkPin(pin: String): Boolean {
        return if (sharedPreferences!!.getString("PIN", null) == pin) {
            unlockedState.value = true

            true
        } else false
    }

    fun setNewPin(pin: String) {
        sharedPreferences!!.edit().putString("PIN", pin).apply()

        if (!pinProtectedState.value) {
            pinProtectedState.value = true

            setLockTimeout(timeout = lockTimeoutState.value)
        }
    }

    fun disablePin() {
        sharedPreferences!!.edit().remove("PIN").apply()
        pinProtectedState.value = false
        sharedPreferences!!.edit().remove("timeout").apply()
        lockTimeoutState.value = (-1).toLong()
        disableBiometric()

        unlock()
    }

    fun enableBiometric() {
        showBiometricPrompt(toEnable = true)
    }

    fun disableBiometric() {
        sharedPreferences!!.edit().remove("biometric").apply()
        biometricProtectedState.value = false
    }

    fun showBiometricPrompt(toEnable: Boolean = false) {
        biometricPrompt = BiometricPrompt(
            context!!,
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
                        sharedPreferences!!.edit().putBoolean("biometric", true).apply()
                        biometricProtectedState.value = true
                    } else unlock()
                }

                override fun onAuthenticationFailed() {
                    biometricDismissedState.value = true
                }
            }
        )
        biometricPrompt!!.authenticate(promptInfo!!)
    }

    fun setLockTimeout(timeout: Long) {
        sharedPreferences!!.edit().putLong("timeout", timeout).apply()
        lockTimeoutState.value = timeout
    }

    fun setRefreshing(refreshing: Boolean) {
        refreshingState.value = refreshing &&
                try {
                    navControllerState.value?.currentDestination?.route!! != "access_pin/{shouldRaiseBiometric}" &&
                            navControllerState.value?.currentDestination?.route!! != "welcome" &&
                            navControllerState.value?.currentDestination?.route!! != "about" &&
                            navControllerState.value?.currentDestination?.route!! != "pin"
                } catch (e: Exception) {
                    false
                }
    }

    private fun setKeyboardMode() {
        if (navControllerState.value?.currentDestination?.route!! == "search" ||
            navControllerState.value?.currentDestination?.route!! == "pin/{change}"
        )
            context!!.window.setSoftInputMode(16)
        else
            context!!.window.setSoftInputMode(32)
    }

    fun navigate(route: String) {
        if (navControllerState.value?.currentDestination?.route!!.substringBefore("/") !=
            route.substringBefore("/")
        ) {
            navControllerState.value?.navigate(route = route) {
                launchSingleTop = true
                restoreState = true
            }

            if (navControllerState.value?.currentBackStackEntry?.destination?.route!! == "access_pin/{shouldRaiseBiometric}" ||
                navControllerState.value?.currentBackStackEntry?.destination?.route!! == "welcome" ||
                navControllerState.value?.currentBackStackEntry?.destination?.route!! == "settings" ||
                navControllerState.value?.currentBackStackEntry?.destination?.route!! == "about" ||
                navControllerState.value?.currentBackStackEntry?.destination?.route!! == "pin"
            ) refreshingState.value = false
        }

        setKeyboardMode()
    }

    fun popBackStack(): Boolean {
        try {
            return if (navControllerState.value?.previousBackStackEntry?.destination?.route!! == "welcome" ||
                navControllerState.value?.previousBackStackEntry?.destination?.route!! == "access_pin/{shouldRaiseBiometric}" ||
                navControllerState.value?.currentBackStackEntry?.destination?.route!! == "welcome" ||
                navControllerState.value?.currentBackStackEntry?.destination?.route!! == "access_pin/{shouldRaiseBiometric}" ||
                navControllerState.value?.currentBackStackEntry?.destination?.route!! == "passwords"
            )
                if (currentFolderState.value != 0) {
                    setCurrentFolder()
                    true
                } else false
            else {
                navControllerState.value?.popBackStack()

                if (navControllerState.value?.currentBackStackEntry?.destination?.route!! != "new_password")
                    NextcloudApiProvider.faviconRequest(data = "")

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

    fun setCurrentFolder(folder: Int? = null) {
        if (folder != null) {
            currentFolderState.value = folder
            selectedFolderState.value = folder
        } else {
            currentFolderState.value =
                NextcloudApiProvider.storedFolders.value.indexOfFirst {
                    it.id == NextcloudApiProvider.storedFolders.value[currentFolderState.value].parent
                }
            selectedFolderState.value = currentFolderState.value
        }
    }

    fun setSelectedFolder(folder: Int) {
        selectedFolderState.value = folder
    }

    fun enableTags(refresh: Boolean = true) {
        if (refresh) NextcloudApiProvider.refreshServerList(refreshTags = true)

        sharedPreferences!!.edit().putBoolean("tags", true).apply()
        tagsState.value = true
    }

    fun disableTags() {
        sharedPreferences!!.edit().putBoolean("tags", false).apply()
        tagsState.value = false
    }

    fun showDialog(
        title: String,
        body: @Composable () -> Unit,
        confirm: Boolean = false,
        action: () -> Unit
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
}