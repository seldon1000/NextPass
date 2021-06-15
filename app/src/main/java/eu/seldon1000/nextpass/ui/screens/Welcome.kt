package eu.seldon1000.nextpass.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.api.NextcloudApiProvider

@Composable
fun WelcomeScreen() {
    val context = LocalContext.current

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize(),
    ) {
        Text(
            text = context.getString(R.string.welcome_message),
            modifier = Modifier.padding(all = 56.dp), textAlign = TextAlign.Center,
            fontSize = 36.sp, fontWeight = FontWeight.SemiBold,
        )
        Button(
            onClick = { NextcloudApiProvider.pickNewAccount() },
            shape = RoundedCornerShape(size = 8.dp)
        ) {
            Text(
                text = context.getString(R.string.login),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}