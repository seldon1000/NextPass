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

package eu.seldon1000.nextpass.ui.items

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.seldon1000.nextpass.MainViewModel
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.api.Tag
import eu.seldon1000.nextpass.ui.layout.SimpleFlowRow
import eu.seldon1000.nextpass.ui.theme.pickerColors

@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun TagsRow(
    tags: List<Tag>? = null,
    alignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    tagClickAction: (tag: Tag?) -> Unit,
    viewModel: MainViewModel
) {
    val context = LocalContext.current

    val storedTags by viewModel.nextcloudApi.storedTags.collectAsState()

    var newTagLabel by remember { mutableStateOf(value = "") }
    var newTagColor by remember { mutableStateOf(value = pickerColors[0]) }

    var selected by rememberSaveable { mutableStateOf(value = -1) }

    SimpleFlowRow(
        verticalGap = 8.dp,
        horizontalGap = 8.dp,
        alignment = alignment,
        modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
    ) {
        storedTags.forEachIndexed { index, tag ->
            val color = Color(android.graphics.Color.parseColor(tag.color))

            Surface(
                modifier = Modifier.shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(size = 8.dp)
                )
            ) {
                Crossfade(targetState = selected) { state ->
                    var expanded by remember { mutableStateOf(value = false) }

                    Card(
                        backgroundColor = color.copy(
                            alpha = if ((tags != null && tags.contains(
                                    element = tag
                                )) || state == index
                            ) 0.8f else 0.3f
                        ),
                        modifier = Modifier
                            .padding(end = 4.dp, bottom = 2.dp)
                            .clip(shape = RoundedCornerShape(size = 8.dp))
                            .border(
                                width = 1.dp,
                                color = color,
                                shape = RoundedCornerShape(size = 8.dp)
                            )
                            .combinedClickable(
                                onClick = {
                                    tagClickAction(tag)
                                    if (tags == null) selected = if (state == index) -1 else index
                                },
                                onLongClick = { expanded = true })
                    ) {
                        Text(
                            text = tag.label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.width(width = 140.dp)
                    ) {
                        DropdownMenuItem(onClick = {
                            expanded = false

                            newTagLabel = tag.label
                            newTagColor = tag.color

                            viewModel.showDialog(
                                title = context.getString(R.string.edit_tag),
                                body = {
                                    Column {
                                        TextFieldItem(
                                            text = newTagLabel,
                                            onTextChanged = { newTagLabel = it },
                                            label = context.getString(R.string.label),
                                            required = true,
                                            capitalized = true
                                        ) {
                                            CopyButton(
                                                label = context.getString(R.string.tag_label),
                                                clip = newTagLabel,
                                                viewModel = viewModel
                                            )
                                        }
                                        ColorPicker(selected = newTagColor) { newTagColor = it }
                                    }
                                },
                                confirm = true
                            ) {
                                val params = mapOf(
                                    "id" to tag.id,
                                    "label" to newTagLabel,
                                    "color" to newTagColor
                                )

                                viewModel.executeRequest {
                                    viewModel.nextcloudApi.updateTagRequest(params = params)
                                    viewModel.nextcloudApi.refreshServerList(refreshFolders = false)
                                    viewModel.showSnackbar(message = context.getString(R.string.tag_updated_snack))
                                }

                                tagClickAction(null)
                                selected = -1
                            }
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_round_edit_24),
                                contentDescription = "password_details"
                            )
                            Text(
                                text = context.getString(R.string.edit_tag),
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                        DropdownMenuItem(onClick = {
                            viewModel.showDialog(
                                title = context.getString(R.string.delete_tag),
                                body = {
                                    Text(
                                        text = context.getString(R.string.delete_tag_body),
                                        fontSize = 14.sp
                                    )
                                },
                                confirm = true
                            ) {
                                viewModel.executeRequest {
                                    viewModel.nextcloudApi.deleteTagRequest(id = tag.id)
                                    viewModel.nextcloudApi.refreshServerList()
                                    viewModel.showSnackbar(message = context.getString(R.string.tag_deleted_snack))
                                }

                                tagClickAction(null)
                                selected = -1
                            }

                            expanded = false
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_round_delete_forever_24),
                                contentDescription = "delete_tag",
                                tint = Color.Red
                            )
                            Text(
                                text = context.getString(R.string.delete),
                                color = Color.Red,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }
        }
        Surface(
            shape = RoundedCornerShape(size = 8.dp),
            modifier = Modifier.shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(size = 8.dp)
            )
        ) {
            Card(
                onClick = {
                    newTagLabel = ""
                    newTagColor = pickerColors[0]

                    viewModel.showDialog(
                        title = context.getString(R.string.new_tag),
                        body = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                TextFieldItem(
                                    text = newTagLabel,
                                    onTextChanged = { newTagLabel = it },
                                    label = context.getString(R.string.label),
                                    required = true,
                                    capitalized = true
                                )
                                ColorPicker(selected = newTagColor) { newTagColor = it }
                            }
                        },
                        confirm = true
                    ) {
                        if (newTagLabel.isEmpty()) newTagLabel =
                            context.getString(R.string.new_tag_default, storedTags.size + 1)

                        val params = mapOf(
                            "label" to newTagLabel,
                            "color" to newTagColor
                        )

                        viewModel.executeRequest {
                            viewModel.nextcloudApi.createTagRequest(params = params)
                            viewModel.showSnackbar(message = context.getString(R.string.tag_created_snack))
                        }
                    }
                },
                shape = RoundedCornerShape(size = 8.dp),
                modifier = Modifier.animateContentSize(
                    animationSpec = tween(
                        durationMillis = 200,
                        easing = FastOutSlowInEasing
                    )
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (storedTags.size < 1) context.getString(R.string.add_new_tag) else "",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(
                            horizontal = if (storedTags.size < 1) 18.dp else 0.dp,
                            vertical = 8.dp
                        )
                    )
                    Icon(
                        painter = painterResource(id = R.drawable.ic_round_add_24),
                        contentDescription = "add_tag",
                        modifier = Modifier.padding(
                            start = if (storedTags.size < 1) 0.dp else 6.dp,
                            end = 6.dp
                        )
                    )
                }
            }
        }
    }
}
