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

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.app.assist.AssistStructure.ViewNode
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.CancellationSignal
import android.os.PowerManager
import android.service.autofill.*
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import eu.seldon1000.nextpass.MainActivity
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.api.NextcloudApiProvider

class NextPassAutofillService : AutofillService() {
    private var isServiceStarted = false
    private var wakeLock: PowerManager.WakeLock? = null

    private var usernameHints = arrayOf<String>()
    private var fillResponse = FillResponse.Builder()
    private var usernameId = mutableListOf<AutofillId>()
    private var passwordId = mutableListOf<AutofillId>()
    private var ready = false

    override fun onCreate() {
        super.onCreate()

        NextcloudApiProvider.setContext(context = this)

        usernameHints = resources.getStringArray(R.array.username_hints)

        val notification = createNotification()
        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (NextcloudApiProvider.attemptLogin()) startService()
        else stopService()

        return START_STICKY
    }

    private fun startService() {
        if (!isServiceStarted) {
            if (NextcloudApiProvider.storedPasswords.value.isEmpty())
                NextcloudApiProvider.refreshServerList()

            isServiceStarted = true

            wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NextPassAutofill::lock").apply {
                    acquire()
                }
            }
        }
    }

    private fun stopService() {
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
        }

        isServiceStarted = false
    }

    private fun createNotification(): Notification { // TODO: temporary, the notification is showed for test purposes
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            "NextPass Autofill Service",
            "NextPass Autofill Service",
            NotificationManager.IMPORTANCE_MIN
        ).apply { description = "NextPass Autofill Service" }

        notificationManager.createNotificationChannel(channel)

        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }

        return Notification.Builder(this, channel.id)
            .setContentTitle("NextPass Autofill Service")
            .setContentText("Provide login suggestions when needed.")
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.drawable.ic_passwords_icon)
            .build()
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
            NextcloudApiProvider.storedPasswords.value.forEach { password ->
                if (viewNode.idPackage?.contains(
                        password.label,
                        ignoreCase = true
                    ) == true || try {
                        viewNode.idPackage?.contains(
                            Uri.parse(password.url).host!!,
                            ignoreCase = true
                        ) == true
                    } catch (e: Exception) {
                        false
                    } || password.customFields.any { customField ->
                        customField.values.any {
                            try {
                                it.contains(viewNode.idPackage!!, ignoreCase = true)
                            } catch (e: Exception) {
                                false
                            }
                        }
                    }
                ) {
                    val credentialsPresentation =
                        RemoteViews(packageName, R.layout.autofill_list_item)
                    credentialsPresentation.setTextViewText(R.id.label, password.label)
                    credentialsPresentation.setTextViewText(R.id.username, password.username)
                    credentialsPresentation.setImageViewBitmap(R.id.favicon, password.favicon.value)

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