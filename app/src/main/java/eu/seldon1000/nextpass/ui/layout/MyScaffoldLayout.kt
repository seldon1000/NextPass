package eu.seldon1000.nextpass.ui.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.FabPosition
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun MyScaffoldLayout(
    fab: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    content: @Composable (paddingValues: PaddingValues) -> Unit
) {
    Scaffold(
        floatingActionButton = fab,
        floatingActionButtonPosition = FabPosition.End,
        isFloatingActionButtonDocked = true,
        bottomBar = bottomBar,
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        content(paddingValues)
    }
}