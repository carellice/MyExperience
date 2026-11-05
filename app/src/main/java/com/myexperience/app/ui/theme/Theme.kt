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

// Palette "pianeta Terra": mare, costa, foreste, terra cotta dal sole, sabbia, roccia di montagna.
val Sea = Color(0xFF2A8197)
val Lagoon = Color(0xFF4FB0A5)
val Forest = Color(0xFF5B8A4E)
val Moss = Color(0xFF8BB063)
val Terracotta = Color(0xFFD07A4D)
val Sand = Color(0xFFE6B65C)
val Rock = Color(0xFF6E7A82)

/** Dal mare alla terraferma. */
val BrandBrush = Brush.linearGradient(listOf(Sea, Forest))
val BrandColors = listOf(Sea, Lagoon, Forest)

/** Gradienti per copertine e schede viaggio: paesaggi della Terra. */
val TripGradients: List<List<Color>> = listOf(
    listOf(Color(0xFF1F6E82), Color(0xFF5FB3C2)), // mare aperto
    listOf(Color(0xFF2F5D3A), Color(0xFF7FA650)), // foresta
    listOf(Color(0xFFB85C35), Color(0xFFE3B66B)), // deserto
    listOf(Color(0xFF4A5560), Color(0xFF93A3AD)), // montagna
    listOf(Color(0xFF8A3B24), Color(0xFFD0784A)), // canyon
    listOf(Color(0xFF2A9D8F), Color(0xFFA8D5BA)), // laguna
    listOf(Color(0xFF34617F), Color(0xFFAFCFDD)), // ghiacciaio
    listOf(Color(0xFF5E6B2E), Color(0xFFB9A85C)), // colline e vigneti
)

fun gradientFor(seed: Long): Brush {
    val c = TripGradients[(seed.mod(TripGradients.size.toLong())).toInt()]
    return Brush.linearGradient(c)
}

// Scuro: oceano di notte
private val DarkScheme = darkColorScheme(
    primary = Color(0xFF5FB3C2),
    onPrimary = Color(0xFF00343E),
    primaryContainer = Color(0xFF173A42),
    onPrimaryContainer = Color(0xFFC4E9EF),
    secondary = Color(0xFF93BF7E),
    onSecondary = Color(0xFF13300F),
    secondaryContainer = Color(0xFF243B22),
    onSecondaryContainer = Color(0xFFCFE8C2),
    tertiary = Color(0xFFE38D5F),
    tertiaryContainer = Color(0xFF4A2A1A),
    onTertiaryContainer = Color(0xFFFFDBC9),
    surfaceTint = Color(0xFF5FB3C2),
    background = Color(0xFF0D1416),
    onBackground = Color(0xFFEDF1EC),
    surface = Color(0xFF141D20),
    onSurface = Color(0xFFEDF1EC),
    surfaceVariant = Color(0xFF20302F),
    onSurfaceVariant = Color(0xFFA5B5AF),
    surfaceContainer = Color(0xFF182326),
    surfaceContainerHigh = Color(0xFF1F2C2F),
    surfaceContainerHighest = Color(0xFF283739),
    outline = Color(0xFF364744),
    outlineVariant = Color(0xFF263533),
)

// Chiaro: sabbia e carta naturale
private val LightScheme = lightColorScheme(
    primary = Color(0xFF1F6E80),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCDE7EB),
    onPrimaryContainer = Color(0xFF06333C),
    secondary = Color(0xFF4E7D3F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7E8C9),
    onSecondaryContainer = Color(0xFF173012),
    tertiary = Color(0xFFB8603A),
    tertiaryContainer = Color(0xFFF6DCCB),
    onTertiaryContainer = Color(0xFF3E1B08),
    surfaceTint = Color(0xFF1F6E80),
    background = Color(0xFFF4F0E6),
    onBackground = Color(0xFF1E2522),
    surface = Color(0xFFFFFCF6),
    onSurface = Color(0xFF1E2522),
    surfaceVariant = Color(0xFFEAE3D3),
    onSurfaceVariant = Color(0xFF66665A),
    surfaceContainer = Color(0xFFF0EADD),
    surfaceContainerHigh = Color(0xFFE9E2D2),
    surfaceContainerHighest = Color(0xFFE1D9C7),
    outline = Color(0xFFCFC6B2),
    outlineVariant = Color(0xFFE2DAC8),
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
