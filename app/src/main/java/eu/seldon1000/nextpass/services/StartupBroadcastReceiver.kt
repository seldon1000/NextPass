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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.view.autofill.AutofillManager
import eu.seldon1000.nextpass.ui.MainViewModel

class StartupBroadcastReceiver : BroadcastReceiver() {
    private val networkRequest = NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
        .build()

    override fun onReceive(context: Context, intent: Intent?) {
        val autofillManager = context.getSystemService(AutofillManager::class.java)

        if (autofillManager.hasEnabledAutofillServices() &&
            context.getSharedPreferences("nextpass", 0).contains("autostart")
        ) {
            (context.getSystemService(ConnectivityManager::class.java))
                .registerNetworkCallback(
                    networkRequest,
                    object : ConnectivityManager.NetworkCallback() {
                        override fun onAvailable(network: Network) {
                            super.onAvailable(network)

                            val autofillIntent =
                                Intent(context, NextPassAutofillService::class.java)

                            MainViewModel.setAutofillIntent(intent = autofillIntent)

                            context.startService(autofillIntent)
                        }
                    }
                )
        }
    }
}