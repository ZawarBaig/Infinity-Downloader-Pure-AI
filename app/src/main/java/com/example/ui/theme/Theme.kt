package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = BentoPrimary,
    onPrimary = BentoOnPrimary,
    secondary = BentoSecondary,
    onSecondary = BentoOnSecondary,
    tertiary = BentoTertiary,
    background = BentoBackground,
    onBackground = BentoOnSurface,
    surface = BentoSurface,
    onSurface = BentoOnSurface,
    surfaceVariant = BentoSurfaceVariant,
    onSurfaceVariant = BentoOnSurfaceVariant,
    outline = BentoOutline
  )

private val LightColorScheme =
  lightColorScheme(
    primary = BentoPrimary,
    onPrimary = BentoOnPrimary,
    secondary = BentoSecondary,
    onSecondary = BentoOnSecondary,
    tertiary = BentoTertiary,
    background = BentoBackground,
    onBackground = BentoOnSurface,
    surface = BentoSurface,
    onSurface = BentoOnSurface,
    surfaceVariant = BentoSurfaceVariant,
    onSurfaceVariant = BentoOnSurfaceVariant,
    outline = BentoOutline
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
