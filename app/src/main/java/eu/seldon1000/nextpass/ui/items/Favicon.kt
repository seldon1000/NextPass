package eu.seldon1000.nextpass.ui.items

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil.transform.RoundedCornersTransformation
import com.google.accompanist.coil.rememberCoilPainter
import eu.seldon1000.nextpass.api.Password

@Composable
fun Favicon(data: Any, size: Dp) {
    Image(
        painter = when (data) {
            is Password -> {
                if (data.favicon != null) data.favicon!!
                else requestFaviconPainter(url = data.url)
            }
            else -> requestFaviconPainter(url = data.toString())
        },
        alignment = Alignment.Center,
        contentDescription = "favicon",
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(size = size)
    )
}

@Composable
fun requestFaviconPainter(url: String): Painter {
    return rememberCoilPainter(
        request = "https://besticon-demo.herokuapp.com/icon?url=$url&size=44..144..256",
        requestBuilder = {
            transformations(
                RoundedCornersTransformation(
                    topLeft = 16F,
                    topRight = 16F,
                    bottomLeft = 16F,
                    bottomRight = 16F
                )
            )
        },
        fadeIn = true
    )
}