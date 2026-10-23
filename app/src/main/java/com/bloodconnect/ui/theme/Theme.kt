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
    primary = StitchCrimson,
    onPrimary = PureWhite,
    primaryContainer = Color(0xFF8B1A24),
    onPrimaryContainer = Color(0xFFFFD9DC),
    secondary = Color(0xFFFFB3B8),
    onSecondary = Color(0xFF561D22),
    secondaryContainer = Color(0xFF733338),
    onSecondaryContainer = Color(0xFFFFD9DC),
    tertiary = StitchGreen,
    onTertiary = PureWhite,
    tertiaryContainer = Color(0xFF005235),
    onTertiaryContainer = Color(0xFF8CF8CD),
    background = StitchObsidianBg,
    onBackground = StitchTextPrimary,
    surface = StitchCardBg,
    onSurface = StitchTextPrimary,
    surfaceVariant = StitchInputBg,
    onSurfaceVariant = StitchTextSecondary,
    outline = StitchCardBorder,
    outlineVariant = StitchInputBorder,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val LightColorScheme = lightColorScheme(
    primary = StitchCrimson,
    onPrimary = PureWhite,
    primaryContainer = Color(0xFFFFEBEE),
    onPrimaryContainer = Color(0xFFB71C1C),
    secondary = StitchCrimsonDark,
    onSecondary = PureWhite,
    secondaryContainer = Color(0xFFFEE2E2),
    onSecondaryContainer = Color(0xFF991B1B),
    tertiary = StitchGreen,
    onTertiary = PureWhite,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF065F46),
    background = StitchLightBg,
    onBackground = StitchLightTextPrimary,
    surface = StitchLightCardBg,
    onSurface = StitchLightTextPrimary,
    surfaceVariant = StitchLightInputBg,
    onSurfaceVariant = StitchLightTextSecondary,
    outline = StitchLightCardBorder,
    outlineVariant = StitchLightInputBorder,
    error = Color(0xFFDC2626),
    onError = PureWhite,
    errorContainer = Color(0xFFFEF2F2),
    onErrorContainer = Color(0xFF991B1B)
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

