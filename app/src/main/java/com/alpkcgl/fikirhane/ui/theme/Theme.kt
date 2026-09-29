package com.alpkcgl.fikirhane.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Web sitesiyle aynı palet (css/base.css)
val Primary = Color(0xFFC2410C)
val PrimaryHover = Color(0xFF9A3412)
val PrimarySoft = Color(0xFFFFF1E8)
val Background = Color(0xFFFAF8F5)
val Surface = Color(0xFFFFFFFF)
val TextColor = Color(0xFF1F1D1A)
val TextMuted = Color(0xFF6B665E)
val Border = Color(0xFFE6E1D8)
val Danger = Color(0xFFB91C1C)
val DangerSoft = Color(0xFFFEF2F2)
val Success = Color(0xFF15803D)
val SuccessSoft = Color(0xFFF0FDF4)

private val Colors = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimarySoft,
    onPrimaryContainer = PrimaryHover,
    background = Background,
    onBackground = TextColor,
    surface = Surface,
    onSurface = TextColor,
    onSurfaceVariant = TextMuted,
    outline = Border,
    error = Danger,
    errorContainer = DangerSoft,
)

/** Başlıklar web'deki gibi serif, gövde metni sistem yazı tipi. */
private val Base = Typography()
private val AppTypography = Base.copy(
    headlineLarge = Base.headlineLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold),
    headlineMedium = Base.headlineMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold),
    headlineSmall = Base.headlineSmall.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 22.sp),
)

@Composable
fun FikirhaneTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = AppTypography, content = content)
}
