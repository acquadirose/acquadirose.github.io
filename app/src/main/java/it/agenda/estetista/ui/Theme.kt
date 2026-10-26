package it.agenda.estetista.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import it.agenda.estetista.data.Status

// Palette "Acqua di Rose": inchiostro nero come il logo, bianco, rosa cipria come solo accento.
object Rose {
    val Primary = Color(0xFF231C1F)      // inchiostro (pulsanti principali)
    val PrimarySoft = Color(0xFFF3ECEE)
    val Aqua = Color(0xFFA23E62)         // accento rosa (badge, selezioni)
    val AquaSoft = Color(0xFFF7E4EB)
    val Blush = Color(0xFFE9C9D3)        // decorazioni (filetti, ornamenti)
    val Ground = Color(0xFFFBF8F7)
    val Surface = Color(0xFFFFFFFF)
    val Ink = Color(0xFF2B2226)
    val Muted = Color(0xFF6B5E63)
    val Line = Color(0xFFEDE3E3)
    val HourLine = Color(0xFFDCCDCF)
    val Now = Color(0xFFD1495B)
    val Blocked = Color(0xFFF1EDEE)
    val BlockedLine = Color(0xFFDDD5D7)
}

/** Colore per colonna operatrice: tinta forte (testo/etichette) e fondo tenue (blocchi). */
data class OpColor(val strong: Color, val soft: Color)

val OperatorColors = listOf(
    OpColor(Color(0xFFA23E62), Color(0xFFF7E4EB)), // rosa
    OpColor(Color(0xFF2E7C82), Color(0xFFDFF0F0)), // acqua
    OpColor(Color(0xFF6E4F8C), Color(0xFFEDE6F4)), // glicine
    OpColor(Color(0xFF5E7A3A), Color(0xFFE8F0DD)), // salvia
)

fun opColor(id: Int) = OperatorColors[(id - 1).mod(OperatorColors.size)]

fun statusColors(s: Status): Pair<Color, Color> = when (s) {
    Status.DA_CONFERMARE -> Color(0xFFFBEBD3) to Color(0xFF7A4A0C)
    Status.CONFERMATO -> Color(0xFFDCEFE6) to Color(0xFF1F5C46)
    Status.COMPLETATO -> Color(0xFFECE8E8) to Color(0xFF4F4649)
    Status.DISDETTO -> Color(0xFFF8E1E1) to Color(0xFF8E2F25)
}

@Composable
fun AgendaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Rose.Primary,
            onPrimary = Color.White,
            primaryContainer = Rose.PrimarySoft,
            onPrimaryContainer = Rose.Primary,
            secondary = Rose.Aqua,
            onSecondary = Color.White,
            secondaryContainer = Rose.AquaSoft,
            onSecondaryContainer = Color(0xFF5C1F36),
            background = Rose.Ground,
            onBackground = Rose.Ink,
            surface = Rose.Surface,
            onSurface = Rose.Ink,
            surfaceVariant = Color(0xFFF4ECEC),
            onSurfaceVariant = Rose.Muted,
            outline = Color(0xFFCBBDC0),
            outlineVariant = Rose.Line,
            error = Color(0xFFB3261E),
        ),
        shapes = Shapes(
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(14.dp),
            large = RoundedCornerShape(20.dp),
        ),
        content = content,
    )
}
