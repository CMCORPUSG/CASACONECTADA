package pe.casaconectada.design


import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Jade = Color(0xFF087F72)
val Mint = Color(0xFFDCF5EB)
val Ivory = Color(0xFFF9FBF8)
val Ink = Color(0xFF173242)
val Muted = Color(0xFF617D81)
val Line = Color(0xFFE3ECE8)
val Peach = Color(0xFFFFE4D8)
val Lavender = Color(0xFFEEE9FF)

private val CasaColors = lightColorScheme(
    primary = Jade,
    onPrimary = Color.White,
    primaryContainer = Mint,
    onPrimaryContainer = Ink,
    secondary = Color(0xFF0E6D63),
    background = Ivory,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF2F8F4),
    onSurfaceVariant = Muted,
    outlineVariant = Line,
)

@Composable
fun CasaTheme(content: @Composable () -> Unit) {
    // El diseño de referencia aprobado es claro, independientemente del modo del sistema.
    MaterialTheme(colorScheme = CasaColors, typography = Typography(), content = content)
}
