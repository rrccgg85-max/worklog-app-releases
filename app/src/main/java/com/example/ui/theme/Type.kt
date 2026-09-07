package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.R

val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val fontPrompt = GoogleFont("Prompt")
val fontKanit = GoogleFont("Kanit")
val fontSarabun = GoogleFont("Sarabun")

val PromptFontFamily: FontFamily = try {
    FontFamily(
        Font(googleFont = fontPrompt, fontProvider = fontProvider, weight = FontWeight.Light),
        Font(googleFont = fontPrompt, fontProvider = fontProvider, weight = FontWeight.Normal),
        Font(googleFont = fontPrompt, fontProvider = fontProvider, weight = FontWeight.Medium),
        Font(googleFont = fontPrompt, fontProvider = fontProvider, weight = FontWeight.SemiBold),
        Font(googleFont = fontPrompt, fontProvider = fontProvider, weight = FontWeight.Bold)
    )
} catch (e: Exception) {
    try {
        FontFamily(
            androidx.compose.ui.text.font.Font(R.font.prompt_light, weight = FontWeight.Light),
            androidx.compose.ui.text.font.Font(R.font.prompt_regular, weight = FontWeight.Normal),
            androidx.compose.ui.text.font.Font(R.font.prompt_medium, weight = FontWeight.Medium),
            androidx.compose.ui.text.font.Font(R.font.prompt_semibold, weight = FontWeight.SemiBold),
            androidx.compose.ui.text.font.Font(R.font.prompt_bold, weight = FontWeight.Bold)
        )
    } catch (e2: Exception) {
        FontFamily.Default
    }
}

val KanitFontFamily: FontFamily = try {
    FontFamily(
        Font(googleFont = fontKanit, fontProvider = fontProvider, weight = FontWeight.Light),
        Font(googleFont = fontKanit, fontProvider = fontProvider, weight = FontWeight.Normal),
        Font(googleFont = fontKanit, fontProvider = fontProvider, weight = FontWeight.Medium),
        Font(googleFont = fontKanit, fontProvider = fontProvider, weight = FontWeight.SemiBold),
        Font(googleFont = fontKanit, fontProvider = fontProvider, weight = FontWeight.Bold)
    )
} catch (e: Exception) {
    PromptFontFamily
}

val SarabunFontFamily: FontFamily = try {
    FontFamily(
        Font(googleFont = fontSarabun, fontProvider = fontProvider, weight = FontWeight.Light),
        Font(googleFont = fontSarabun, fontProvider = fontProvider, weight = FontWeight.Normal),
        Font(googleFont = fontSarabun, fontProvider = fontProvider, weight = FontWeight.Medium),
        Font(googleFont = fontSarabun, fontProvider = fontProvider, weight = FontWeight.SemiBold),
        Font(googleFont = fontSarabun, fontProvider = fontProvider, weight = FontWeight.Bold)
    )
} catch (e: Exception) {
    PromptFontFamily
}

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = PromptFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = PromptFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = PromptFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = PromptFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = PromptFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = PromptFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = PromptFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PromptFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = PromptFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = PromptFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PromptFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = PromptFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = PromptFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PromptFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = PromptFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
