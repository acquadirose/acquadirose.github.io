package it.agenda.estetista.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.agenda.estetista.R
import it.agenda.estetista.util.Logo

/** Titoli in serif, in sintonia con il lettering del logo. */
val SerifTitle = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal, letterSpacing = 0.2.sp)

/** Logo: quello caricato dalle impostazioni, altrimenti quello incluso nell'app. */
@Composable
fun CenterLogo(logoVersion: Int, height: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val custom = remember(logoVersion) { Logo.load(context)?.asImageBitmap() }
    if (custom != null) {
        Image(
            bitmap = custom,
            contentDescription = "Logo",
            contentScale = ContentScale.Fit,
            modifier = modifier.height(height).widthIn(max = height * 4),
        )
    } else {
        Image(
            painter = painterResource(R.drawable.logo_acqua_di_rose),
            contentDescription = "Acqua di Rose, istituto di bellezza",
            contentScale = ContentScale.Fit,
            modifier = modifier.height(height),
        )
    }
}

/** Piccola rosa del logo, usata come ornamento. */
@Composable
fun RoseMark(size: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.rose_mark),
        contentDescription = null,
        modifier = modifier.size(size),
    )
}

/** Filetto con rosa al centro, per separare le sezioni. */
@Composable
fun Ornament(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f), color = Rose.Blush)
        RoseMark(22.dp, Modifier.padding(horizontal = 10.dp))
        HorizontalDivider(Modifier.weight(1f), color = Rose.Blush)
    }
}

/** Selettore a tendina generico. */
@Composable
fun <T> Choice(
    current: T,
    options: List<T>,
    text: (T) -> String,
    onPick: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(text(current))
            Spacer(Modifier.width(6.dp))
            Text("▾", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { o ->
                DropdownMenuItem(text = { Text(text(o)) }, onClick = { onPick(o); open = false })
            }
        }
    }
}

/** − valore + */
@Composable
fun Stepper(
    value: Int,
    step: Int,
    min: Int,
    max: Int,
    format: (Int) -> String,
    onChange: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedIconButton(
            onClick = { onChange((value - step).coerceAtLeast(min)) },
            enabled = value > min,
            modifier = Modifier.size(48.dp),
        ) { Text("−", style = MaterialTheme.typography.titleLarge) }
        Text(
            format(value),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.widthIn(min = 72.dp),
        )
        OutlinedIconButton(
            onClick = { onChange((value + step).coerceAtMost(max)) },
            enabled = value < max,
            modifier = Modifier.size(48.dp),
        ) { Text("+", style = MaterialTheme.typography.titleLarge) }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = SerifTitle.copy(fontSize = 22.sp),
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
fun FieldLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

/** Pulsante compatto icona + etichetta per la barra in alto, con eventuale contatore. */
@Composable
fun ToolButton(icon: ImageVector, label: String, badge: Int = 0, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .widthIn(min = 64.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Icon(icon, null, tint = Rose.Ink, modifier = Modifier.size(26.dp))
            if (badge > 0) {
                Text(
                    badge.toString(),
                    fontSize = 10.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 8.dp, y = (-4).dp)
                        .clip(CircleShape)
                        .background(Rose.Aqua)
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
        }
        Text(label, fontSize = 12.sp, color = Rose.Ink)
    }
}
