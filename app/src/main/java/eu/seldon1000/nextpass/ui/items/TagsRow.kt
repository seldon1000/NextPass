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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.api.NextcloudApiProvider
import eu.seldon1000.nextpass.api.Tag
import eu.seldon1000.nextpass.ui.MainViewModel
import eu.seldon1000.nextpass.ui.layout.SimpleFlowRow

@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun TagsRow(
    tags: SnapshotStateList<Tag>? = null,
    alignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    tagClickAction: (tag: String) -> Unit
) {
    val context = LocalContext.current

    val storedTags by NextcloudApiProvider.storedTags.collectAsState()

    var newTagLabel by remember { mutableStateOf(value = "") }
    var newTagColor by remember { mutableStateOf(value = MainViewModel.pickerColors[0]) }

    var selected by remember { mutableStateOf(value = -1) }

    SimpleFlowRow(
        verticalGap = 8.dp,
        horizontalGap = 8.dp,
        alignment = alignment,
        modifier = Modifier.padding(top = 16.dp, bottom = 22.dp)
    ) {
        (tags ?: storedTags).forEachIndexed { index, tag ->
            val color = Color(android.graphics.Color.parseColor(tag.color))

            Surface(modifier = Modifier.shadow(elevation = 8.dp, shape = CircleShape)) {
                Crossfade(targetState = selected) { state ->
                    var expanded by remember { mutableStateOf(value = false) }

                    //var label by remember { mutableStateOf(value = tag.label)}

                    Card(
                        backgroundColor = color.copy(alpha = if (tags != null || state == index) 0.7f else 0.15f),
                        modifier = Modifier
                            .padding(end = 4.dp, bottom = 2.dp)
                            .clip(shape = CircleShape)
                            .border(
                                width = 1.dp,
                                color = color,
                                shape = CircleShape
                            )
                            .combinedClickable(
                                onClick = {
                                    if (tags == null) {
                                        tagClickAction(tag.id)
                                        selected = if (state == index) -1 else index
                                    }
                                },
                                onLongClick = { expanded = true })
                    ) {
                        Text(
                            text = tag.label,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.width(width = 120.dp)
                    ) {
                        /*DropdownMenuItem(onClick = { // TODO: enable tag update action once SSO supports PATCH method
                            expanded = false

                            MainViewModel.showDialog(title = "Edit tag", body = {
                                Column {
                                    TextFieldItem(
                                        text = label,
                                        onTextChanged = { label = it },
                                        label = context.getString(R.string.label),
                                        required = true,
                                        capitalized = true
                                    ) {}
                                    ColorPicker { newTagColor = it }
                                }
                            }, confirm = true) {}
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_round_edit_24),
                                contentDescription = "password_details"
                            )
                            Text(
                                text = "Edit",
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }*/
                        DropdownMenuItem(onClick = {
                            MainViewModel.showDialog(
                                title = context.getString(R.string.delete_tag),
                                body = {
                                    Text(
                                        text = context.getString(R.string.delete_tag_body),
                                        fontSize = 14.sp
                                    )
                                },
                                confirm = true
                            ) {
                                NextcloudApiProvider.deleteTagRequest(index = index)
                                MainViewModel.showSnackbar(message = context.getString(R.string.tag_deleted_snack))

                                tagClickAction("")
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
        if (tags == null || tags.size < 1) Card(
            onClick = {
                    MainViewModel.showDialog(title = context.getString(R.string.new_tag), body = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            TextFieldItem(
                                text = newTagLabel,
                                onTextChanged = { newTagLabel = it },
                                label = context.getString(R.string.label),
                                required = true,
                                capitalized = true
                            ) {}
                            ColorPicker { newTagColor = it }
                        }
                    }, confirm = true) {
                        if (newTagLabel.isEmpty()) newTagLabel =
                            context.getString(R.string.new_tag_default, storedTags.size + 1)

                        val params = mapOf(
                            "label" to newTagLabel,
                            "color" to "#${
                                String.format("%06X", newTagColor.toArgb()).removePrefix("FF")
                            }"
                        )

                        MainViewModel.setRefreshing(refreshing = true)
                        NextcloudApiProvider.createTagRequest(params = params)
                        MainViewModel.showSnackbar(message = context.getString(R.string.tag_created_snack))

                        newTagLabel = ""
                        newTagColor = Color.Blue
                    }
            },
            enabled = tags == null,
            elevation = 8.dp,
            shape = CircleShape,
            modifier = Modifier.animateContentSize(
                animationSpec = tween(
                    durationMillis = 200,
                    easing = FastOutSlowInEasing
                )
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (tags == null) {
                        if (storedTags.size < 1) context.getString(R.string.add_new_tag) else ""
                    } else "No tags",
                    fontSize = 14.sp,
                    modifier = Modifier.padding(
                        start = if (storedTags.size < 1) 18.dp else 0.dp,
                        top = 8.dp,
                        end = if (tags == null) 0.dp else 18.dp,
                        bottom = 8.dp
                    )
                )
                if (tags == null) Icon(
                    painter = painterResource(id = R.drawable.ic_round_add_24),
                    contentDescription = "add_tag",
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}
