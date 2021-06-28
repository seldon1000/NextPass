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
import android.view.autofill.AutofillManager
import androidx.biometric.BiometricPrompt
import androidx.compose.material.SnackbarHostState
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

    private var navController: NavController? = null

    private var clipboardManager: ClipboardManager? = null

    private var snackbarHostState: SnackbarHostState? = null

    private var autofillManager: AutofillManager? = null

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

    private val currentScreenState = MutableStateFlow(value = "passwords")
    val currentScreen = currentScreenState

    private val refreshingState = MutableStateFlow(value = false)
    val refreshing: StateFlow<Boolean> = refreshingState

    private val folderModeState = MutableStateFlow(value = false)
    val folderMode = folderModeState

    private val currentFolderState = MutableStateFlow(value = 0)
    val currentFolder = currentFolderState

    private val selectedFolderState = MutableStateFlow(value = currentFolder.value)
    val selectedFolder = selectedFolderState

    private val openDialogState = MutableStateFlow(value = false)
    val openDialog = openDialogState

    private val dialogTitleState = MutableStateFlow(value = "")
    val dialogTitle = dialogTitleState

    private val dialogTextState = MutableStateFlow(value = "")
    val dialogText = dialogTextState

    private val dialogActionState = MutableStateFlow {}
    val dialogAction = dialogActionState

    private val dialogConfirmState = MutableStateFlow(value = false)
    val dialogConfirm = dialogConfirmState

    fun setContext(context: FragmentActivity) {
        this.context = context

        sharedPreferences = this.context!!.getSharedPreferences("nextpass", 0)

        clipboardManager =
            this.context!!.getSystemService(FragmentActivity.CLIPBOARD_SERVICE) as ClipboardManager
        autofillManager = this.context!!.getSystemService(AutofillManager::class.java)

        if (autofillManager!!.hasEnabledAutofillServices()) {
            autofillState.value = true
            autostartState.value = sharedPreferences!!.contains("autostart")

            context.startForegroundService(
                Intent(context, NextPassAutofillService::class.java)
            )
        }

        if (sharedPreferences!!.contains("PIN")) {
            unlockedState.value = false
            pinProtectedState.value = true
            lockTimeoutState.value = sharedPreferences!!.getLong("timeout", 0)

            biometricProtectedState.value = sharedPreferences!!.contains("biometric")
        }
    }

    fun enableAutofill() {
        autofillState.value = true

        context!!.startForegroundService(Intent(context, NextPassAutofillService::class.java))
    }

    fun enableAutostart() {
        sharedPreferences!!.edit().putBoolean("autostart", true).apply()

        autostartState.value = true
    }

    fun disableAutostart() {
        sharedPreferences!!.edit().remove("autostart").apply()

        autostartState.value = false
    }

    fun setNavController(controller: NavController) {
        navController = controller
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
        sharedPreferences!!.edit().remove("timeout").apply()

        biometricProtectedState.value = false
        pinProtectedState.value = false
        lockTimeoutState.value = (-1).toLong()
        unlock()
    }

    fun enableBiometric() {
        sharedPreferences!!.edit().putBoolean("biometric", true).apply()

        biometricProtectedState.value = true
    }

    fun disableBiometric() {
        sharedPreferences!!.edit().remove("biometric").apply()

        biometricProtectedState.value = false
    }

    fun setLockTimeout(timeout: Long) {
        sharedPreferences!!.edit().putLong("timeout", timeout).apply()

        lockTimeoutState.value = timeout
    }

    fun openApp(shouldRememberScreen: Boolean = false) {
        if (unlockedState.value) {
            if (NextcloudApiProvider.attemptLogin()) {
                if (!shouldRememberScreen) navigate(route = "passwords")

                NextcloudApiProvider.refreshServerList()
            }
        } else navigate(route = "access_pin/true")
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

        if (navController?.previousBackStackEntry?.destination?.route != "welcome") {
            currentScreenState.value = navController?.previousBackStackEntry?.destination?.route!!
            navController?.popBackStack(
                route = navController?.currentBackStackEntry?.destination?.route!!,
                inclusive = true
            )
        } else openApp()
    }

    fun setSnackbarHostState(snackbar: SnackbarHostState) {
        snackbarHostState = snackbar
    }

    fun setRefreshing(refreshing: Boolean) {
        refreshingState.value = refreshing &&
                try {
                    navController?.currentDestination?.route!! != "access_pin/{shouldRaiseBiometric}" &&
                            navController?.currentDestination?.route!! != "welcome" &&
                            navController?.currentDestination?.route!! != "about" &&
                            navController?.currentDestination?.route!! != "pin"
                } catch (e: Exception) {
                    false
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

    fun navigate(route: String) {
        if (navController?.currentDestination?.route!!.substringBefore("/") !=
            route.substringBefore("/")
        ) {
            currentScreenState.value = route
            navController?.navigate(route = route)

            if (navController?.currentDestination?.route!! == "access_pin/{shouldRaiseBiometric}" ||
                navController?.currentDestination?.route!! == "welcome" ||
                navController?.currentDestination?.route!! == "settings" ||
                navController?.currentDestination?.route!! == "about" ||
                navController?.currentDestination?.route!! == "pin"
            ) refreshingState.value = false
        }
    }

    fun popBackStack(): Boolean {
        try {
            return if (navController?.previousBackStackEntry?.destination?.route!! == "welcome" ||
                navController?.previousBackStackEntry?.destination?.route!! == "access_pin/{shouldRaiseBiometric}" ||
                navController?.currentDestination?.route!! == "welcome" ||
                navController?.currentDestination?.route!! == "access_pin/{shouldRaiseBiometric}" ||
                navController?.currentDestination?.route!! == "passwords"
            )
                if (currentFolderState.value != 0) {
                    setCurrentFolder()
                    true
                } else false
            else {
                currentScreenState.value =
                    navController?.previousBackStackEntry?.destination?.route!!
                navController?.popBackStack()

                true
            }
        } catch (e: Exception) {
            return false
        }
    }

    fun setPrimaryClip(label: String, clip: String) {
        viewModelScope.launch {
            clipboardManager!!.setPrimaryClip(ClipData.newPlainText(label, clip))
        }

        showSnackbar(message = context!!.getString(R.string.copy_snack_message, label))
    }

    fun showSnackbar(message: String) {
        viewModelScope.launch {
            if (navController?.currentDestination?.route!! != "access_pin/{shouldRaiseBiometric}" &&
                navController?.currentDestination?.route!! != "pin"
            ) {
                snackbarHostState?.currentSnackbarData?.dismiss()
                snackbarHostState?.showSnackbar(message = message)
            }
        }
    }

    fun showDialog(title: String, body: String, confirm: Boolean = false, action: () -> Unit) {
        dialogTitleState.value = title
        dialogTextState.value = body
        dialogConfirmState.value = confirm
        dialogActionState.value = action

        openDialogState.value = true
    }

    fun dismissDialog() {
        openDialogState.value = false
    }

    fun showBiometricPrompt() {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(context!!.getString(R.string.access_nextpass))
            .setSubtitle(context!!.getString(R.string.access_nextpass_body))
            .setNegativeButtonText(context!!.getString(R.string.cancel))
            .build()

        val biometricPrompt = BiometricPrompt(
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
                    unlock()
                }

                override fun onAuthenticationFailed() {
                    biometricDismissedState.value = true
                }
            }
        )

        biometricPrompt.authenticate(promptInfo)
    }
}