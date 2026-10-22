package com.bloodconnect.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CoralDarkPrimary,
    onPrimary = DarkCoralOnPrimary,
    primaryContainer = Color(0xFF8B1A24),
    onPrimaryContainer = Color(0xFFFFD9DC),
    secondary = Color(0xFFFFB3B8),
    onSecondary = Color(0xFF561D22),
    secondaryContainer = Color(0xFF733338),
    onSecondaryContainer = Color(0xFFFFD9DC),
    tertiary = Color(0xFF6FDBB1),
    onTertiary = Color(0xFF003823),
    tertiaryContainer = Color(0xFF005235),
    onTertiaryContainer = Color(0xFF8CF8CD),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkBorder,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val LightColorScheme = lightColorScheme(
    primary = CoralPrimary,
    onPrimary = PureWhite,
    primaryContainer = LightCoralAccent,
    onPrimaryContainer = DarkCoral,
    secondary = DarkCoral,
    onSecondary = PureWhite,
    secondaryContainer = LightCoralAccent,
    onSecondaryContainer = DarkCoral,
    tertiary = SuccessGreen,
    onTertiary = PureWhite,
    tertiaryContainer = SuccessGreenContainer,
    onTertiaryContainer = Color(0xFF005235),
    background = SoftPinkBackground,
    onBackground = PrimaryText,
    surface = PureWhite,
    onSurface = PrimaryText,
    surfaceVariant = LightCoralAccent,
    onSurfaceVariant = SecondaryText,
    outline = SubtleBorder,
    outlineVariant = Color(0xFFF2F2F5),
    error = ErrorRed,
    onError = PureWhite,
    errorContainer = ErrorRedContainer,
    onErrorContainer = Color(0xFFB71C1C)
)

@Composable
fun BloodConnectTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+ (defaults to false to preserve brand coral identity)
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

