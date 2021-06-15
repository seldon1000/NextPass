package eu.seldon1000.nextpass.ui.items

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.api.NextcloudApiProvider
import eu.seldon1000.nextpass.api.Password
import eu.seldon1000.nextpass.ui.MainViewModel

@Composable
fun FavoriteIcon(index: Int, password: Password) {
    IconButton({
        MainViewModel.setRefreshing(refreshing = true)

        NextcloudApiProvider.updatePasswordRequest(
            index = index,
            params = if (!password.favorite) mutableMapOf("favorite" to "true") else mutableMapOf()
        )
    }) {
        Icon(
            painter = if (password.favorite) painterResource(id = R.drawable.ic_round_star_yellow_24)
            else painterResource(id = R.drawable.ic_round_star_border_24),
            contentDescription = "favorite",
            tint = if (password.favorite) Color.Yellow else Color.White
        )
    }
}