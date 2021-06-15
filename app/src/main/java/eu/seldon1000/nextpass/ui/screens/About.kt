package eu.seldon1000.nextpass.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.seldon1000.nextpass.R
import eu.seldon1000.nextpass.ui.MainViewModel
import eu.seldon1000.nextpass.ui.items.GenericColumnItem
import eu.seldon1000.nextpass.ui.layout.Header
import eu.seldon1000.nextpass.ui.layout.MyScaffoldLayout

@ExperimentalMaterialApi
@Composable
fun About() {
    val context = LocalContext.current

    val scrollState = rememberScrollState()

    val greetings = listOf(
        "Hello there",
        "Nice to meet you",
        "Stop tapping on me",
        "I'm your friend",
        "How's it going?",
        "Have a good day",
        "I'm NextPass",
        "What are you doing?",
        "Don't you have other things to do?",
        "Yes?",
        "What do you want?",
        "It tickles"
    )

    MyScaffoldLayout(fab = {}, bottomBar = {
        BottomAppBar(backgroundColor = Color.Black, cutoutShape = CircleShape) {
            IconButton(onClick = { MainViewModel.popBackStack() }) {
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
            Header(expanded = false, title = context.getString(R.string.about)) {}
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(all = 72.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        shape = RoundedCornerShape(size = 16.dp),
                        elevation = 0.dp,
                        onClick = { MainViewModel.showSnackbar(message = greetings.random()) }) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher),
                            contentDescription = "app_icon",
                            modifier = Modifier.size(size = 176.dp)
                        )
                    }
                    Text(
                        text = context.getString(
                            R.string.app_version,
                            context.packageManager.getPackageInfo(
                                context.packageName,
                                0
                            ).versionName
                        ),
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
            Card(
                elevation = 6.dp,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = paddingValues.calculateBottomPadding())
                ) {
                    GenericColumnItem(
                        title = "Nicolas Mariniello (seldon1000)",
                        body = context.getString(R.string.main_developer_body),

                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_round_code_24),
                                contentDescription = "main_developer",
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    ) {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/seldon1000")
                            )
                        )
                    }
                    GenericColumnItem(
                        title = "Nextcloud",
                        body = context.getString(R.string.nextcloud_body),

                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_nextcloud_icon),
                                contentDescription = "nextcloud",
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    ) {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://nextcloud.com")
                            )
                        )
                    }
                    GenericColumnItem(
                        title = "Passwords",
                        body = context.getString(R.string.passwords_body),
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_passwords_icon),
                                contentDescription = "passwords",
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    ) {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://apps.nextcloud.com/apps/passwords")
                            )
                        )
                    }
                    GenericColumnItem(
                        title = context.getString(R.string.need_help),
                        body = context.getString(R.string.need_help_body),
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_round_help_center_24),
                                contentDescription = "help",
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    ) {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/seldon1000/NextPass/issues/new")
                            )
                        )
                    }
                    GenericColumnItem(
                        title = context.getString(R.string.send_email),
                        body = context.getString(R.string.send_email_body),
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_round_email_24),
                                contentDescription = "email",
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    ) {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_SENDTO,
                                Uri.parse("mailto:seldon1000@tutanota.com")
                            )
                        )
                    }
                    GenericColumnItem(
                        title = context.getString(R.string.special_thanks),
                        body = context.getString(R.string.special_thanks_body),
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_round_people_24),
                                contentDescription = "special_thanks",
                                modifier = Modifier.padding(start = 16.dp, top = 16.dp)
                            )
                        }
                    ) {}
                }
            }
        }
    }
}