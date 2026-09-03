package com.courseflow.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Pine = Color(0xFF0F5C55)
val Teal = Color(0xFF0D9488)
val Mint = Color(0xFFE0F3EE)
val Coral = Color(0xFFFF7452)
val Ink = Color(0xFF173331)
val Canvas = Color(0xFFF7FAF9)
val Line = Color(0xFFDCE8E4)

private val LightColors = lightColorScheme(
    primary = Pine,
    onPrimary = Color.White,
    primaryContainer = Mint,
    onPrimaryContainer = Ink,
    secondary = Teal,
    onSecondary = Color.White,
    tertiary = Coral,
    onTertiary = Ink,
    background = Canvas,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEDF4F2),
    onSurfaceVariant = Color(0xFF45615E),
    outline = Color(0xFF708B87),
    outlineVariant = Line,
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF82D5C8),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF075048),
    onPrimaryContainer = Color(0xFFA0F2E5),
    secondary = Color(0xFF70D8CA),
    tertiary = Color(0xFFFFB5A1),
    background = Color(0xFF0D1514),
    onBackground = Color(0xFFE1EAE7),
    surface = Color(0xFF121D1B),
    onSurface = Color(0xFFE1EAE7),
    surfaceVariant = Color(0xFF23302E),
    onSurfaceVariant = Color(0xFFBECBC8),
    outline = Color(0xFF899692),
)

@Composable
fun CourseFlowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = CourseFlowTypography,
        content = content,
    )
}
