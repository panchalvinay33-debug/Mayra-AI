package ai.mayra.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MayraColors = darkColorScheme()

@Composable
fun MayraAITheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MayraColors,
        content = content
    )
}
