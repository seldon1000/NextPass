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

package eu.seldon1000.nextpass.services

import android.annotation.SuppressLint
import android.app.assist.AssistStructure
import android.app.assist.AssistStructure.ViewNode
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.CancellationSignal
import android.service.autofill.*
import android.text.InputType
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import eu.seldon1000.nextpass.CentralAppControl
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.api.NextcloudApi
import eu.seldon1000.nextpass.api.NextcloudApi.generatePassword
import eu.seldon1000.nextpass.api.NextcloudApi.toRoundedCorners
import eu.seldon1000.nextpass.api.Password
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import java.math.BigInteger
import java.security.MessageDigest

class NextPassAutofillService : AutofillService() {
    private val coroutineScope = CoroutineScope(context = Dispatchers.Unconfined)

    private var usernameHints = arrayOf<String>()
    private var passwordHints = arrayOf<String>()

    private var idPackage = ""
    private var viewWebDomain = ""

    private var saveUsername = ""
    private var savePassword = ""

    private var fillResponse = FillResponse.Builder()
    private var usernameId = mutableListOf<AutofillId>()
    private var passwordId = mutableListOf<AutofillId>()
    private var ready = false

    override fun onCreate() {
        super.onCreate()

        val sharedPreferences = getSharedPreferences("nextpass", 0)

        NextcloudApi.login(
            server = sharedPreferences.getString("server", "")!!,
            loginName = sharedPreferences.getString("loginName", "")!!,
            appPassword = sharedPreferences.getString("appPassword", "")!!
        )

        usernameHints = resources.getStringArray(R.array.username_hints)
        passwordHints = resources.getStringArray(R.array.password_hints)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return if (NextcloudApi.storedPasswords.value.isEmpty() &&
            NextcloudApi.isLogged()
        ) {
            coroutineScope.launch {
                NextcloudApi.refreshServerList(refreshFolders = false, refreshTags = false)
            }

            START_STICKY
        } else {
            stopSelf()

            START_NOT_STICKY
        }
    }

    override fun onConnected() {
        super.onConnected()

        if (NextcloudApi.storedPasswords.value.isEmpty() &&
            NextcloudApi.isLogged()
        ) coroutineScope.launch {
            NextcloudApi.refreshServerList(refreshFolders = false, refreshTags = false)
        }
    }

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        coroutineScope.launch {
            idPackage = ""
            viewWebDomain = ""

            saveUsername = ""
            savePassword = ""

            fillResponse = FillResponse.Builder()
            usernameId = mutableListOf()
            passwordId = mutableListOf()
            ready = false

            val context = request.fillContexts
            val structure = context.last().structure

            traverseStructure(structure = structure, mode = false)

            if (usernameId.isNotEmpty() && passwordId.isNotEmpty()) {
                fillResponse.setSaveInfo(
                    SaveInfo.Builder(
                        SaveInfo.SAVE_DATA_TYPE_USERNAME or SaveInfo.SAVE_DATA_TYPE_PASSWORD,
                        arrayOf(usernameId.last(), passwordId.last())
                    ).build()
                )
            }

            if (passwordId.isNotEmpty()) {
                val randomPassword = async { generatePassword() }

                passwordId.forEach {
                    val credentialsPresentation =
                        RemoteViews(packageName, R.layout.autofill_list_item)
                    credentialsPresentation.setTextViewText(
                        R.id.label,
                        getString(R.string.random_password)
                    )
                    credentialsPresentation.setTextViewText(
                        R.id.username,
                        getString(R.string.autofill_random_password)
                    )
                    credentialsPresentation.setImageViewBitmap(
                        R.id.favicon, BitmapFactory.decodeResource(
                            resources,
                            R.drawable.ic_app_icon
                        ).toRoundedCorners()
                    )

                    try {
                        fillResponse.addDataset(
                            Dataset.Builder()
                                .setValue(
                                    it,
                                    AutofillValue.forText(randomPassword.await()),
                                    credentialsPresentation
                                ).build()
                        )
                    } catch (e: Exception) {
                    }
                }

                try {
                    callback.onSuccess(fillResponse.build())
                } catch (e: Exception) {
                }
            }
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        idPackage = ""
        viewWebDomain = ""

        saveUsername = ""
        savePassword = ""

        fillResponse = FillResponse.Builder()
        usernameId = mutableListOf()
        passwordId = mutableListOf()
        ready = false

        val context = request.fillContexts
        val structure = context.last().structure

        traverseStructure(structure = structure, mode = true)

        if (saveUsername.isNotEmpty() && savePassword.isNotEmpty()) {
            var hash = BigInteger(
                1,
                MessageDigest.getInstance("SHA-1").digest(savePassword.toByteArray())
            ).toString(16)
            while (hash.length < 32) hash = "0$hash"

            val appName = idPackage.substringAfter(delimiter = ".").substringBefore(delimiter = ".")

            val params = mutableMapOf<String, String>(
                "password" to savePassword,
                "label" to when {
                    viewWebDomain.isNotEmpty() -> viewWebDomain.removePrefix(prefix = "www.")
                        .substringBefore(delimiter = ".")
                        .replaceFirstChar { it.titlecase() }
                    idPackage.isNotEmpty() -> {
                        try {
                            packageManager.getApplicationLabel(
                                packageManager.getApplicationInfo(
                                    idPackage,
                                    0
                                )
                            ).toString()
                        } catch (e: Exception) {
                            appName.replaceFirstChar { it.titlecase() }
                        }
                    }
                    else -> "Unknown"
                },
                "username" to saveUsername,
                "url" to viewWebDomain.ifEmpty { "$appName.com" },
                "hash" to hash
            )

            if (viewWebDomain.isEmpty()) {
                params["customFields"] = NextcloudApi.json.encodeToString(
                    value = listOf(
                        mapOf(
                            "label" to "Android app",
                            "type" to "text",
                            "value" to idPackage
                        )
                    )
                )
            }

            CentralAppControl.executeRequest {
                NextcloudApi.createPasswordRequest(params = params, tags = emptyList())

                callback.onSuccess()
            }
        }
    }

    private fun traverseStructure(structure: AssistStructure, mode: Boolean) {
        val windowNodes = structure.run { (0 until windowNodeCount).map { getWindowNodeAt(it) } }

        windowNodes.forEach { traverseNode(viewNode = it.rootViewNode, mode = mode) }
    }

    @SuppressLint("RestrictedApi")
    private fun traverseNode(viewNode: ViewNode, mode: Boolean) {
        if (viewNode.webDomain != null && viewWebDomain.isEmpty())
            viewWebDomain = viewNode.webDomain!!
        if (viewNode.idPackage?.contains(".") == true)
            idPackage = viewNode.idPackage.toString()

        if (!mode) {
            if (usernameId.isNotEmpty() && passwordId.isNotEmpty() && !ready) {
                NextcloudApi.storedPasswords.value.forEach { password ->
                    if (checkSuggestions(password = password)) {
                        val credentialsPresentation =
                            RemoteViews(packageName, R.layout.autofill_list_item)
                        credentialsPresentation.setTextViewText(R.id.label, password.label)
                        credentialsPresentation.setTextViewText(
                            R.id.username,
                            password.username
                        )
                        credentialsPresentation.setImageViewBitmap(
                            R.id.favicon,
                            if (password.favicon.value != null) password.favicon.value
                            else BitmapFactory.decodeResource(
                                resources,
                                R.drawable.ic_app_icon
                            ).toRoundedCorners()
                        )

                        fillResponse.addDataset(
                            Dataset.Builder()
                                .setValue(
                                    usernameId.last(),
                                    AutofillValue.forText(password.username),
                                    credentialsPresentation
                                )
                                .setValue(
                                    passwordId.last(),
                                    AutofillValue.forText(password.password),
                                    credentialsPresentation
                                ).build()
                        )

                        ready = true
                    }
                }
            }

            if (checkUsernameHints(viewNode = viewNode) &&
                !usernameId.contains(element = viewNode.autofillId)
            ) {
                usernameId.add(element = viewNode.autofillId!!)

                if (usernameId.size == passwordId.size) ready = false
            } else if (checkPasswordHints(viewNode = viewNode) &&
                !passwordId.contains(element = viewNode.autofillId)
            ) {
                passwordId.add(element = viewNode.autofillId!!)

                if (passwordId.size < usernameId.size) ready = false
            } else fillResponse.setIgnoredIds(viewNode.autofillId)
        } else {
            if (checkUsernameHints(viewNode = viewNode) && viewNode.text?.isNotEmpty() == true)
                saveUsername = viewNode.text.toString()
            else if (checkPasswordHints(viewNode = viewNode) && viewNode.text?.isNotEmpty() == true)
                savePassword = viewNode.text.toString()
            else fillResponse.setIgnoredIds(viewNode.autofillId)
        }

        val children = viewNode.run { (0 until childCount).map { getChildAt(it) } }
        children.forEach { childNode -> traverseNode(viewNode = childNode, mode = mode) }
    }

    private fun checkUsernameHints(viewNode: ViewNode): Boolean {
        return usernameHints.any { hint ->
            viewNode.autofillHints?.any {
                it.contains(other = hint, ignoreCase = true) ||
                        hint.contains(other = it, ignoreCase = true)
            } == true || viewNode.hint?.contains(other = hint, ignoreCase = true) == true ||
                    hint.contains(other = viewNode.hint.toString(), ignoreCase = true)
        }
    }

    private fun checkPasswordHints(viewNode: ViewNode): Boolean {
        return passwordHints.any { hint ->
            viewNode.autofillHints?.any {
                it.contains(other = hint, ignoreCase = true) ||
                        hint.contains(other = it, ignoreCase = true)
            } == true || viewNode.hint?.contains(other = hint, ignoreCase = true) == true ||
                    hint.contains(other = viewNode.hint.toString(), ignoreCase = true)
        } && (viewNode.inputType and InputType.TYPE_TEXT_VARIATION_PASSWORD == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                viewNode.inputType and InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
                viewNode.inputType and InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
                viewNode.inputType and InputType.TYPE_NUMBER_VARIATION_PASSWORD == InputType.TYPE_NUMBER_VARIATION_PASSWORD ||
                viewNode.inputType and InputType.TYPE_DATETIME_VARIATION_NORMAL == InputType.TYPE_DATETIME_VARIATION_NORMAL) // this is necessary for autofill to work on Amazon's apps
    }

    private fun checkSuggestions(password: Password): Boolean {
        val domain = viewWebDomain.removePrefix(prefix = "www.")

        return ((domain.isNotEmpty() && (password.url.contains(other = domain, ignoreCase = true) ||
                domain.contains(other = password.label, ignoreCase = true) ||
                domain.contains(other = password.url, ignoreCase = true) ||
                password.url.contains(
                    other = domain.substringBefore(delimiter = "."),
                    ignoreCase = true
                ) ||
                domain.substringBefore(delimiter = ".").contains(
                    other = password.label,
                    ignoreCase = true
                ))) ||
                (domain.isEmpty() && idPackage.isNotEmpty() && (idPackage.contains(
                    other = password.label,
                    ignoreCase = true
                ) || try {
                    idPackage.contains(
                        other = Uri.parse(password.url).host!!,
                        ignoreCase = true
                    )
                } catch (e: Exception) {
                    false
                } || password.customFields.contains(other = idPackage, ignoreCase = true))))
    }
}