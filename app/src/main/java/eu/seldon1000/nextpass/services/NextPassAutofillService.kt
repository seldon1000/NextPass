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

import android.R
import android.app.assist.AssistStructure
import android.app.assist.AssistStructure.ViewNode
import android.os.CancellationSignal
import android.service.autofill.*
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import eu.seldon1000.nextpass.api.NextcloudApiProvider
import eu.seldon1000.nextpass.ui.MainViewModel

class NextPassAutofillService : AutofillService() {
    private val usernameHints =
        listOf("username", "nickname", "name", "user", "e-mail", "email", "mail", "phone", "number")

    private var fillResponse = FillResponse.Builder()
    private var usernameId = mutableListOf<AutofillId>()
    private var passwordId = mutableListOf<AutofillId>()
    private var ready = false

    init {
        if (!MainViewModel.checkAutofillEnabled()) stopSelf()
    }

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        fillResponse = FillResponse.Builder()
        usernameId = mutableListOf()
        passwordId = mutableListOf()
        ready = false

        val context = request.fillContexts
        val structure = context.last().structure

        traverseStructure(structure = structure, callback = callback)

        callback.onSuccess(if (ready) fillResponse.build() else null)
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        TODO("Not yet implemented")
    }

    private fun traverseStructure(structure: AssistStructure, callback: FillCallback) {
        val windowNodes = structure.run { (0 until windowNodeCount).map { getWindowNodeAt(it) } }

        windowNodes.forEach { traverseNode(viewNode = it.rootViewNode, callback = callback) }
    }

    private fun traverseNode(viewNode: ViewNode, callback: FillCallback) {
        if (usernameId.isNotEmpty() && passwordId.isNotEmpty() && !ready) {
            NextcloudApiProvider.storedPasswords.value.forEach {
                if (viewNode.idPackage?.contains(it.label, ignoreCase = true) == true) {
                    val usernamePresentation = RemoteViews(packageName, R.layout.simple_list_item_1)
                    val passwordPresentation = RemoteViews(packageName, R.layout.simple_list_item_1)

                    usernamePresentation.setTextViewText(R.id.text1, it.username)
                    usernamePresentation.setImageViewResource(
                        R.id.icon,
                        eu.seldon1000.nextpass.R.drawable.ic_passwords_icon
                    )
                    passwordPresentation.setTextViewText(R.id.text1, it.username)

                    fillResponse.addDataset(
                        Dataset.Builder()
                            .setValue(
                                usernameId.last(),
                                AutofillValue.forText(it.username),
                                usernamePresentation
                            )
                            .setValue(
                                passwordId.last(),
                                AutofillValue.forText(it.password),
                                passwordPresentation
                            ).build()
                    )

                    ready = true
                }
            }
        }

        if (usernameHints.any { viewNode.hint?.lowercase()?.contains(it) == true } &&
            !usernameId.contains(element = viewNode.autofillId)
        ) {
            usernameId.add(element = viewNode.autofillId!!)
            if (passwordId.size == usernameId.size) ready = false
        }

        if (viewNode.hint?.lowercase()?.contains("password") == true &&
            !passwordId.contains(element = viewNode.autofillId)
        ) {
            passwordId.add(element = viewNode.autofillId!!)
            if (passwordId.size < usernameId.size) ready = false
        }

        val children = viewNode.run { (0 until childCount).map { getChildAt(it) } }

        children.forEach { childNode -> traverseNode(viewNode = childNode, callback = callback) }
    }
}