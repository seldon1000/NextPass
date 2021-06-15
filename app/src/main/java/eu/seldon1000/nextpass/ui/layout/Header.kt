package eu.seldon1000.nextpass.ui.layout

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Header(expanded: Boolean, title: String, item: @Composable () -> Unit) {
    if (expanded) {
        Column(
            verticalArrangement = Arrangement.Bottom,
            modifier = Modifier.padding(top = 156.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold
                )
                item()
            }
        }
    } else {
        Text(
            text = title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
    }
}