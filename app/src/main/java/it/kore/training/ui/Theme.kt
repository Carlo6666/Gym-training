package it.kore.training.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object K {
    val Bg = Color(0xFF0A0A0A)
    val Surface = Color(0xFF151515)
    val Surface2 = Color(0xFF1E1E1E)
    val Line = Color(0xFF2A2A2A)
    val Accent = Color(0xFFD4AF63)
    val Accent2 = Color(0xFFE8823A)
    val Text = Color(0xFFF5F5F5)
    val Muted = Color(0xFF9A9A9A)
    val Good = Color(0xFF3DDC84)
    val Warn = Color(0xFFFFC23D)
    val Bad = Color(0xFFFF4D4D)
    val Blue = Color(0xFF4DA3FF)

    val accentGradient = Brush.linearGradient(listOf(Color(0xFFA8803A), Color(0xFFE9CF8A), Color(0xFFB8904A)))
    val heroGradient = Brush.verticalGradient(listOf(Color(0xFF241B0C), Bg))
}

private val scheme = darkColorScheme(
    primary = K.Accent,
    onPrimary = Color.Black,
    secondary = K.Accent2,
    onSecondary = Color.Black,
    background = K.Bg,
    onBackground = K.Text,
    surface = K.Surface,
    onSurface = K.Text,
    surfaceVariant = K.Surface2,
    onSurfaceVariant = K.Muted,
    surfaceContainer = K.Surface,
    surfaceContainerHigh = K.Surface2,
    surfaceContainerHighest = K.Surface2,
    outline = K.Line,
    error = K.Bad
)

val Serif = FontFamily.Serif

private val base = Typography()
private val typography = Typography(
    displaySmall = base.displaySmall.copy(fontFamily = Serif, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.copy(fontFamily = Serif, fontWeight = FontWeight.Bold),
    headlineSmall = base.headlineSmall.copy(fontFamily = Serif, fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Bold),
    labelSmall = base.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
)

val Mono = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)

@Composable
fun KoreTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
}
