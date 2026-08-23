package com.theplace.receiptscanner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = Teal80,
    secondary = TealGrey80,
    tertiary = Sand80,
)

private val LightColors = lightColorScheme(
    primary = Teal40,
    secondary = TealGrey40,
    tertiary = Sand40,
)

/**
 * Thème CMP commun. Le dynamic color Android 12+ est appliqué via une
 * surcharge plateforme (cf. AndroidDynamicColors).
 */
@Composable
fun ReceiptScannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColors: ColorScheme? = null,
    content: @Composable () -> Unit,
) {
    val colors = dynamicColors
        ?: if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
}
