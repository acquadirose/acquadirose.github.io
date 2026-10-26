package it.agenda.estetista.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.agenda.estetista.AgendaViewModel
import it.agenda.estetista.R
import it.agenda.estetista.data.AppSettings
import it.agenda.estetista.util.IT
import it.agenda.estetista.util.WhatsApp
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/** Auguri di compleanno del mese con lo sconto deciso nelle impostazioni. */
@Composable
fun BirthdaysDialog(vm: AgendaViewModel, settings: AppSettings, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clients by vm.clients.collectAsStateWithLifecycle()
    var month by remember { mutableStateOf(YearMonth.now()) }
    val entries = vm.birthdaysIn(month, clients)
    val monthName = month.format(DateTimeFormatter.ofPattern("MMMM", IT))
    val withoutBirthday = clients.count { it.birthday.isBlank() }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
        ) {
            Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Compleanni", style = SerifTitle.copy(fontSize = 28.sp))
                        Text(
                            "Sconto in regalo: ${settings.birthdayDiscount} · modificabile in Impostazioni",
                            color = Rose.Muted, style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Icon(painterResource(R.drawable.ic_cake), null, tint = Rose.Aqua, modifier = Modifier.size(36.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val now = YearMonth.now()
                    FilterChip(selected = month == now, onClick = { month = now }, label = { Text("Questo mese") })
                    FilterChip(selected = month == now.plusMonths(1), onClick = { month = now.plusMonths(1) }, label = { Text("Mese prossimo") })
                }
                val sent = entries.count { it.sent }
                Text(
                    if (entries.isEmpty()) "Nessun compleanno a $monthName."
                    else "Compleanni a $monthName: ${entries.size} · auguri inviati $sent",
                    color = Rose.Muted,
                )

                LazyColumn(Modifier.heightIn(max = 440.dp)) {
                    items(entries, key = { it.client.id }) { e ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                e.day.toString(),
                                style = SerifTitle.copy(fontSize = 24.sp),
                                color = Rose.Aqua,
                                modifier = Modifier.width(48.dp),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(e.client.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    e.client.phone.ifBlank { "manca il numero" },
                                    style = MaterialTheme.typography.bodySmall, color = Rose.Muted,
                                )
                            }
                            when {
                                e.client.phone.isBlank() -> {}
                                e.sent -> OutlinedButton(onClick = {
                                    WhatsApp.open(context, e.client.phone, WhatsApp.birthdayMessage(settings, e.client, e.month))
                                }) { Text("✓ Reinvia") }
                                else -> Button(onClick = {
                                    val ok = WhatsApp.open(context, e.client.phone, WhatsApp.birthdayMessage(settings, e.client, e.month))
                                    if (ok) vm.markBirthdaySent(e.client, e.month.year)
                                    else Toast.makeText(context, "Numero non valido o WhatsApp assente", Toast.LENGTH_SHORT).show()
                                }) { Text("Invia auguri") }
                            }
                        }
                        HorizontalDivider(color = Rose.Line)
                    }
                }

                if (withoutBirthday > 0) {
                    Text(
                        "$withoutBirthday clienti in rubrica non hanno la data di compleanno: aggiungila nella loro scheda.",
                        style = MaterialTheme.typography.bodySmall, color = Rose.Muted,
                    )
                }
                Row {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Chiudi") }
                }
            }
        }
    }
}
