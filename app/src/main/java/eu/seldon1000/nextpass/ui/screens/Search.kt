package eu.seldon1000.nextpass.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.api.NextcloudApiProvider
import eu.seldon1000.nextpass.ui.MainViewModel
import eu.seldon1000.nextpass.ui.items.CountMessage
import eu.seldon1000.nextpass.ui.items.FolderCard
import eu.seldon1000.nextpass.ui.items.PasswordCard
import eu.seldon1000.nextpass.ui.layout.Header
import eu.seldon1000.nextpass.ui.layout.MyScaffoldLayout

@ExperimentalMaterialApi
@ExperimentalAnimationApi
@Composable
fun Search() {
    val context = LocalContext.current

    (context as Activity).window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

    val storedPasswords by NextcloudApiProvider.storedPasswords.collectAsState()
    val storedFolders by NextcloudApiProvider.storedFolders.collectAsState()

    var searchedText by remember { mutableStateOf(value = "") }

    val resultFolders = storedFolders.filterIndexed { index, folder ->
        if (index > 0) folder.label.contains(searchedText, ignoreCase = true)
        else false
    }
    val resultPasswords = storedPasswords.filter { password ->
        password.url.contains(searchedText, ignoreCase = true) ||
                password.label.contains(searchedText, ignoreCase = true) ||
                password.username.contains(searchedText, ignoreCase = true) ||
                password.notes.contains(searchedText, ignoreCase = true)
    }

    MyScaffoldLayout(fab = {
        FloatingActionButton({ searchedText = "" }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_round_search_off_24),
                contentDescription = "restore_search",
                tint = Color.White
            )
        }
    }, bottomBar = {
        BottomAppBar(backgroundColor = Color.Black, cutoutShape = CircleShape) {
            IconButton(onClick = { MainViewModel.popBackStack() }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_round_back_arrow_24),
                    contentDescription = "back"
                )
            }
            TextField(
                value = searchedText,
                onValueChange = { searchedText = it },
                label = { Text(text = context.getString(R.string.search_hint)) },
                singleLine = true,
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Black,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )
        }
    }) { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = paddingValues.calculateBottomPadding() + 28.dp
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            item { Header(expanded = true, title = context.getString(R.string.search)) {} }
            if (searchedText.isNotEmpty()) {
                items(items = resultFolders) { folder -> FolderCard(folder = folder) }
                items(items = resultPasswords) { password -> PasswordCard(password = password) }
            }
            if (resultPasswords.isNotEmpty() && searchedText.isNotEmpty()) item {
                CountMessage(
                    message = context.getString(
                        R.string.results_number, resultFolders.size + resultPasswords.size
                    )
                )
            }
        }
    }
}