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

package eu.seldon1000.nextpass.ui.layout

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.seldon1000.nextpass.MainViewModel
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.ui.theme.Orange500

@Composable
fun MyAlertDialog(viewModel: MainViewModel) {
    val context = LocalContext.current

    val openDialog by viewModel.openDialog.collectAsState()
    val dialogTitle by viewModel.dialogTitle.collectAsState()
    val dialogBody by viewModel.dialogBody.collectAsState()
    val dialogConfirm by viewModel.dialogConfirm.collectAsState()
    val dialogAction by viewModel.dialogAction.collectAsState()

    if (openDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialog() },
            confirmButton = {
                if (dialogConfirm)
                    TextButton(
                        onClick = {
                            dialogAction()
                            viewModel.dismissDialog()
                        }
                    ) { Text(text = context.getString(R.string.confirm), color = Orange500) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDialog() }) {
                    Text(text = context.getString(R.string.dismiss), color = Color.LightGray)
                }
            },
            title = {
                Text(
                    text = dialogTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = dialogBody,
            shape = RoundedCornerShape(10.dp)
        )
    }
}