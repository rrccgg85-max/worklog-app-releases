package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CustomAccentOrange,
    onPrimary = Color(0xFF111827),
    primaryContainer = CustomAccentOrangeDark,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF94A3B8),
    secondaryContainer = Color(0xFF1F2937),
    onSecondaryContainer = Color.White,
    background = BentoBackgroundDark,
    surface = BentoSurfaceDark,
    surfaceVariant = BentoSurfaceVariantDark,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = DarkOrangeBorder
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7), // Sky/Ocean blue for strong action buttons
    onPrimary = Color.White,
    primaryContainer = CustomAccentBlue, // #A5E3FA accent
    onPrimaryContainer = SlateDarkText,
    secondary = SlateMediumText,
    secondaryContainer = CustomSurfaceWhite,
    onSecondaryContainer = SlateDarkText,
    tertiary = CustomAccentBlue,
    tertiaryContainer = Color(0xFFE0F2FE),
    background = CustomBgGray, // White/Gray background (#F4F6F8)
    surface = CustomSurfaceWhite, // Pure White cards (#FFFFFF)
    surfaceVariant = Color(0xFFF1F5F9), // Light Gray surface variant
    onBackground = SlateDarkText, // Dark slate text on background
    onSurface = SlateDarkText, // Dark slate text on cards
    onSurfaceVariant = SlateMediumText, // Gray text for subtitles
    outline = CustomBorderGray
)

@Composable
fun WorkLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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

    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as? android.app.Activity)?.window
            if (window != null) {
                androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = {
            CompositionLocalProvider(
                LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = KanitFontFamily)
            ) {
                content()
            }
        }
    )
}
