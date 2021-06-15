package eu.seldon1000.nextpass.ui.items

import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.ui.MainViewModel

@Composable
fun CopyButton(label: String, clip: String) {
    IconButton(onClick = {
        MainViewModel.setPrimaryClip(
            label = label,
            clip = clip
        )
    }) {
        Icon(
            painter = painterResource(id = R.drawable.ic_round_content_copy_24),
            contentDescription = "copy"
        )
    }
}