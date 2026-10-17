package pe.casaconectada.design


import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Jade = Color(0xFF087F72)
val Mint = Color(0xFFD5F5E9)
val Ivory = Color(0xFFF7FAF9)
val Ink = Color(0xFF173242)
val Muted = Color(0xFF617D81)
val Line = Color(0xFFE3ECE8)
val Peach = Color(0xFFFFD6C1)
val Lavender = Color(0xFFE9E8FF)

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
