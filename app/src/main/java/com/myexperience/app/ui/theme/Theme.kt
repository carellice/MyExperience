package com.myexperience.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Coral = Color(0xFFFF7A59)
val Pink = Color(0xFFFF3D7F)
val Violet = Color(0xFF7B5CFF)
val Ocean = Color(0xFF22C1C3)
val Lime = Color(0xFF9BE15D)
val Gold = Color(0xFFFFC145)

val BrandBrush = Brush.linearGradient(listOf(Coral, Pink, Violet))
val BrandColors = listOf(Coral, Pink, Violet)

/** Gradienti per copertine e schede viaggio. */
val TripGradients: List<List<Color>> = listOf(
    listOf(Color(0xFFFF7A59), Color(0xFFFF3D7F)),
    listOf(Color(0xFF7B5CFF), Color(0xFF22C1C3)),
    listOf(Color(0xFF00B09B), Color(0xFF96C93D)),
    listOf(Color(0xFFFC466B), Color(0xFF3F5EFB)),
    listOf(Color(0xFFF7971E), Color(0xFFFFD200)),
    listOf(Color(0xFF8E2DE2), Color(0xFFFF6CAB)),
    listOf(Color(0xFF0F2027), Color(0xFF2C5364)),
    listOf(Color(0xFF11998E), Color(0xFF38EF7D)),
)

fun gradientFor(seed: Long): Brush {
    val c = TripGradients[(seed.mod(TripGradients.size.toLong())).toInt()]
    return Brush.linearGradient(c)
}

private val DarkScheme = darkColorScheme(
    primary = Pink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3A1730),
    onPrimaryContainer = Color(0xFFFFD9E3),
    secondary = Ocean,
    onSecondary = Color.White,
    tertiary = Coral,
    background = Color(0xFF0D0E18),
    onBackground = Color(0xFFF2F1F8),
    surface = Color(0xFF161726),
    onSurface = Color(0xFFF2F1F8),
    surfaceVariant = Color(0xFF222338),
    onSurfaceVariant = Color(0xFFA9A8C2),
    surfaceContainer = Color(0xFF1B1C2E),
    surfaceContainerHigh = Color(0xFF242539),
    surfaceContainerHighest = Color(0xFF2D2E45),
    outline = Color(0xFF3A3B55),
    outlineVariant = Color(0xFF2C2D44),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFFE8336F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0EA),
    onPrimaryContainer = Color(0xFF5C0A2A),
    secondary = Color(0xFF0E9FA1),
    onSecondary = Color.White,
    tertiary = Coral,
    background = Color(0xFFF6F4FB),
    onBackground = Color(0xFF15142A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF15142A),
    surfaceVariant = Color(0xFFEDEAF6),
    onSurfaceVariant = Color(0xFF6A6884),
    surfaceContainer = Color(0xFFF1EEF8),
    surfaceContainerHigh = Color(0xFFEAE6F4),
    surfaceContainerHighest = Color(0xFFE2DEF0),
    outline = Color(0xFFD3CFE3),
    outlineVariant = Color(0xFFE4E0F0),
)

private val base = Typography()
private val AppTypography = Typography(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
    headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.8).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Bold),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.copy(lineHeight = 24.sp),
    bodyMedium = base.bodyMedium.copy(lineHeight = 21.sp),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

val LocalDarkTheme = staticCompositionLocalOf { true }

@Composable
fun MyExperienceTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) DarkScheme else LightScheme
    CompositionLocalProvider(LocalDarkTheme provides dark) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AppTypography,
            shapes = Shapes(
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(18.dp),
                large = RoundedCornerShape(26.dp),
                extraLarge = RoundedCornerShape(32.dp),
            ),
        ) {
            // colore testo di default leggibile anche in tema scuro
            CompositionLocalProvider(LocalContentColor provides scheme.onBackground, content = content)
        }
    }
}

val GradientTextStyle = TextStyle(brush = BrandBrush, fontFamily = FontFamily.Default)
