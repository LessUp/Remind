package app.lessup.remind.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.hilt.navigation.compose.hiltViewModel
import app.lessup.remind.data.settings.ThemeMode

private val LightColors = lightColorScheme(
    primary = TealPrimary,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = TealPrimaryLight,
    onPrimaryContainer = TealPrimaryDark
)

private val DarkColors = darkColorScheme(
    primary = TealPrimaryLight,
    onPrimary = TealPrimaryDark,
    primaryContainer = TealPrimary,
    onPrimaryContainer = androidx.compose.ui.graphics.Color.White
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val vm: ThemeViewModel = hiltViewModel()
    val mode by vm.themeMode.collectAsState()
    val context = LocalContext.current
    val useDarkTheme = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (useDarkTheme) DarkColors else LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
