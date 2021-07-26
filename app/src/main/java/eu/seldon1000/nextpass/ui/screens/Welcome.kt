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

package eu.seldon1000.nextpass.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.seldon1000.nextpass.CentralAppControl
import eu.seldon1000.nextpass.R

@ExperimentalMaterialApi
@Composable
fun WelcomeScreen() {
    val context = LocalContext.current

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize(),
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_app_icon),
            contentDescription = "app_icon",
            modifier = Modifier
                .size(size = 144.dp)
                .clip(shape = RoundedCornerShape(size = 16.dp))
        )
        Text(
            text = context.getString(R.string.welcome_message),
            modifier = Modifier.padding(all = 56.dp), textAlign = TextAlign.Center,
            fontSize = 36.sp, fontWeight = FontWeight.SemiBold,
        )
        Button(
            onClick = { CentralAppControl.attemptLogin() },
            shape = RoundedCornerShape(size = 8.dp)
        ) {
            Text(
                text = context.getString(R.string.login),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}