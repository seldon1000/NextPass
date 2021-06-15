package eu.seldon1000.nextpass.ui.items

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.ui.MainViewModel

@Composable
fun CountMessage(message: String) {
    val context = LocalContext.current

    val refreshing by MainViewModel.refreshing.collectAsState()

    Text(
        text = if (refreshing) context.getString(R.string.waiting_server) else message,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        color = Color.Gray,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    )
}