package eu.seldon1000.nextpass.ui.layout

import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.ui.MainViewModel

@Composable
fun DefaultFab() {
    FloatingActionButton(onClick = { MainViewModel.navigate(route = "new_password") }) {
        Icon(
            painter = painterResource(id = R.drawable.ic_round_add_24),
            contentDescription = "add",
            tint = Color.White
        )
    }
}