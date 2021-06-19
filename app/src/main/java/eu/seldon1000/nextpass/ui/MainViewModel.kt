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
import androidx.biometric.BiometricPrompt
import androidx.compose.material.SnackbarHostState
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.api.NextcloudApiProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@SuppressLint("StaticFieldLeak")
object MainViewModel : ViewModel() {
    private var context: FragmentActivity? = null

    private var navController: NavController? = null

    private var clipboardManager: ClipboardManager? = null

    private var snackbarHostState: SnackbarHostState? = null

    private val unlockedState = MutableStateFlow(value = true)
    val unlocked = unlockedState

    private val pinProtectedState = MutableStateFlow(value = false)
    val pinProtected = pinProtectedState

    private val biometricProtectedState = MutableStateFlow(value = false)
    val biometricProtected = biometricProtectedState

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

    fun setContext(con: FragmentActivity) {
        context = con

        if (context!!.getSharedPreferences("PIN", 0).contains("PIN")) {
            unlockedState.value = false
            pinProtectedState.value = true
            lockTimeoutState.value =
                context!!.getSharedPreferences("timeout", 0).getLong("timeout", 0)

            if (context!!.getSharedPreferences("biometric", 0).contains("biometric"))
                biometricProtectedState.value = true
        }
    }

    fun setNavController(controller: NavController) {
        navController = controller
    }

    fun checkPin(pin: String): Boolean {
        return if (context!!.getSharedPreferences("PIN", 0).getString("PIN", null) == pin) {
            unlockedState.value = true
            true
        } else false
    }

    fun setNewPin(pin: String) {
        context!!.getSharedPreferences("PIN", 0).edit().putString("PIN", pin).apply()

        if (!pinProtectedState.value) {
            pinProtectedState.value = true

            setLockTimeout(timeout = 0)
        }
    }

    fun disablePin() {
        context!!.getSharedPreferences("PIN", 0).edit().remove("PIN").apply()
        context!!.getSharedPreferences("timeout", 0).edit().remove("timeout").apply()

        biometricProtectedState.value = false
        pinProtectedState.value = false
        lockTimeoutState.value = (-1).toLong()
        unlock()
    }

    fun enableBiometric() {
        context!!.getSharedPreferences("biometric", 0).edit().putString("biometric", "yes").apply()

        biometricProtectedState.value = true
    }

    fun disableBiometric() {
        context!!.getSharedPreferences("biometric", 0).edit().remove("biometric").apply()

        biometricProtectedState.value = false
    }

    fun setLockTimeout(timeout: Long) {
        context!!.getSharedPreferences("timeout", 0).edit().putLong("timeout", timeout).apply()

        lockTimeoutState.value = timeout
    }

    fun lock(shouldRaiseBiometric: Boolean = true) {
        if (pinProtectedState.value) {
            unlockedState.value = false

            setRefreshing(refreshing = false)
            navigate(route = "access_pin/$shouldRaiseBiometric")
        }
    }

    fun unlock() {
        unlockedState.value = true
    }

    fun setClipboardManager(manager: ClipboardManager) {
        clipboardManager = manager
    }

    fun setSnackbarHostState(snackbar: SnackbarHostState) {
        snackbarHostState = snackbar
    }

    fun setRefreshing(refreshing: Boolean) {
        refreshingState.value = refreshing
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
                NextcloudApiProvider.storedFolders.value.indexOfFirst { it.id == NextcloudApiProvider.storedFolders.value[currentFolderState.value].parent }
            selectedFolderState.value = currentFolderState.value
        }
    }

    fun setSelectedFolder(folder: Int) {
        selectedFolderState.value = folder
    }

    fun navigate(route: String) {
        currentScreenState.value = route
        navController!!.navigate(route = route)
    }

    fun popBackStack(): Boolean {
        try {
            return if (navController!!.previousBackStackEntry!!.destination.route!! == "welcome" ||
                navController!!.currentDestination!!.route!! == "welcome" ||
                navController!!.currentDestination!!.route!! == "access_pin/{shouldRaiseBiometric}" ||
                navController!!.currentDestination!!.route!! == "passwords"
            )
                if (currentFolderState.value != 0) {
                    setCurrentFolder()
                    true
                } else false
            else {
                currentScreenState.value =
                    navController!!.previousBackStackEntry!!.destination.route!!
                navController!!.popBackStack()

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
            snackbarHostState!!.currentSnackbarData?.dismiss()
            snackbarHostState!!.showSnackbar(message = message)
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
                }

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    unlockedState.value = true
                    NextcloudApiProvider.attemptLogin()
                }

                override fun onAuthenticationFailed() {}
            }
        )

        biometricPrompt.authenticate(promptInfo)
    }
}