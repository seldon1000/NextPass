package eu.seldon1000.nextpass.ui.screens

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.api.NextcloudApiProvider
import eu.seldon1000.nextpass.ui.items.CountMessage
import eu.seldon1000.nextpass.ui.items.FolderCard
import eu.seldon1000.nextpass.ui.items.PasswordCard
import eu.seldon1000.nextpass.ui.layout.DefaultBottomBar
import eu.seldon1000.nextpass.ui.layout.DefaultFab
import eu.seldon1000.nextpass.ui.layout.Header
import eu.seldon1000.nextpass.ui.layout.MyScaffoldLayout

@ExperimentalMaterialApi
@ExperimentalAnimationApi
@Composable
fun Favorites() {
    val context = LocalContext.current

    val storedFolders by NextcloudApiProvider.storedFolders.collectAsState()
    val storedPasswords by NextcloudApiProvider.storedPasswords.collectAsState()

    val favoritePasswords = storedPasswords.filter { it.favorite }
    val favoriteFolders = storedFolders.filter { it.favorite }

    MyScaffoldLayout(
        fab = { DefaultFab() },
        bottomBar = { DefaultBottomBar() }) { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = paddingValues.calculateBottomPadding() + 28.dp
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            item { Header(expanded = true, title = context.getString(R.string.favorites)) {} }
            items(favoriteFolders) { folder ->
                if (folder.favorite) FolderCard(folder = folder)
            }
            items(favoritePasswords) { password ->
                if (password.favorite) PasswordCard(password = password)
            }
            item {
                CountMessage(
                    message = context.getString(
                        R.string.favorite_passwords_number,
                        favoritePasswords.size
                    )
                )
            }
        }
    }
}