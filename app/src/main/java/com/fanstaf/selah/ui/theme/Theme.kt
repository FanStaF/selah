package com.fanstaf.selah.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Brand palette — a calm deep indigo with a warm gold accent.
private val Indigo = Color(0xFF1E2A4A)
private val IndigoDeep = Color(0xFF141C33)
private val Gold = Color(0xFFE4B95B)
private val GoldSoft = Color(0xFFF0D79A)
private val Cream = Color(0xFFF7F4EC)
private val InkOnCream = Color(0xFF20242E)

private val LightColors = lightColorScheme(
    primary = Indigo,
    onPrimary = Color.White,
    secondary = Gold,
    onSecondary = IndigoDeep,
    background = Cream,
    onBackground = InkOnCream,
    surface = Color.White,
    onSurface = InkOnCream,
    surfaceVariant = Color(0xFFECE7DA),
    onSurfaceVariant = Color(0xFF4A4E58),
)

private val DarkColors = darkColorScheme(
    primary = GoldSoft,
    onPrimary = IndigoDeep,
    secondary = Gold,
    onSecondary = IndigoDeep,
    background = IndigoDeep,
    onBackground = Cream,
    surface = Indigo,
    onSurface = Cream,
    surfaceVariant = Color(0xFF2A3556),
    onSurfaceVariant = Color(0xFFC7CBD6),
)

private val SelahTypography = Typography().let { base ->
    base.copy(
        // Verse text uses a serif for a settled, readable feel.
        headlineMedium = base.headlineMedium.copy(
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Normal,
            lineHeight = 34.sp,
        ),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

/** Serif style for the verse body, scaled by the user's font-size setting. */
fun verseTextStyle(base: TextStyle, fontScale: Float): TextStyle =
    base.copy(
        fontFamily = FontFamily.Serif,
        fontSize = base.fontSize * fontScale,
        lineHeight = base.lineHeight * fontScale,
    )

@Composable
fun SelahTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = SelahTypography,
        content = content,
    )
}
