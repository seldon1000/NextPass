package eu.seldon1000.nextpass.ui.screens

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.api.Folder
import eu.seldon1000.nextpass.api.NextcloudApiProvider
import eu.seldon1000.nextpass.ui.MainViewModel
import eu.seldon1000.nextpass.ui.items.CopyButton
import eu.seldon1000.nextpass.ui.items.DropdownFolderList
import eu.seldon1000.nextpass.ui.layout.Header
import eu.seldon1000.nextpass.ui.layout.MyScaffoldLayout
import eu.seldon1000.nextpass.ui.theme.NextcloudBlue

@Composable
@ExperimentalAnimationApi
@ExperimentalMaterialApi
fun FolderDetails(folder: Folder) { /*TODO: allow proper folder edit, once SSO support PATCH method */
    val context = LocalContext.current

    val scrollState = rememberScrollState()

    val label by remember { mutableStateOf(value = folder.label) }

    MyScaffoldLayout(fab = {}, bottomBar = {
        BottomAppBar(backgroundColor = Color.Black, cutoutShape = CircleShape) {
            IconButton(
                onClick = { MainViewModel.popBackStack() },
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_round_back_arrow_24),
                    contentDescription = "back"
                )
            }
        }
    }) { paddingValues ->
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = scrollState, enabled = true)
        ) {
            Header(expanded = false, title = context.getString(R.string.folder_details)) {}
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(all = 72.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_round_folder_24),
                    contentDescription = "folder_big_icon",
                    tint = NextcloudBlue,
                    modifier = Modifier.size(size = 144.dp)
                )
            }
            Card(
                elevation = 6.dp,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = paddingValues.calculateBottomPadding() + 48.dp
                    )
                ) {
                    Column {
                        Text(
                            text = "ID: ${folder.id}",
                            fontSize = 17.sp,
                            overflow = TextOverflow.Clip,
                            maxLines = 2,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                        Text(
                            text = context.getString(
                                R.string.date_info,
                                folder.created,
                                folder.edited
                            ),
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        ) {
                            DropdownFolderList(
                                enabled = false,
                                folder = MainViewModel.currentFolder.value
                            )
                            IconButton(
                                onClick = {},
                                enabled = false
                            ) {
                                Icon(
                                    painter = if (folder.favorite)
                                        painterResource(id = R.drawable.ic_round_star_yellow_24)
                                    else
                                        painterResource(id = R.drawable.ic_round_star_border_24),
                                    contentDescription = "favorite",
                                    tint = if (folder.favorite) Color.Yellow else Color.White
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = label,
                        onValueChange = {},
                        enabled = false,
                        label = { Text(text = context.getString(R.string.label)) },
                        isError = label.isEmpty(),
                        trailingIcon = {
                            CopyButton(
                                label = context.getString(R.string.folder_label),
                                clip = label
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    )
                    OutlinedTextField(
                        value = folder.parent,
                        onValueChange = {},
                        enabled = false,
                        label = { Text(text = context.getString(R.string.parent)) },
                        trailingIcon = {
                            CopyButton(
                                label = context.getString(R.string.parent),
                                clip = folder.parent
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                    )
                    TextButton(onClick = {
                        MainViewModel.showDialog(
                            title = context.getString(R.string.delete_folder),
                            body = context.getString(R.string.delete_folder_body)
                        ) {
                            NextcloudApiProvider.deleteFolderRequest(index = folder.index)
                            MainViewModel.popBackStack()
                            MainViewModel.showSnackbar(message = context.getString(R.string.folder_deleted_snack))
                        }
                    }
                    ) {
                        Text(
                            text = context.getString(R.string.delete),
                            color = Color.Red
                        )
                    }
                }
            }
        }
    }
}