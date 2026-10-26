package it.agenda.estetista.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import it.agenda.estetista.AgendaViewModel
import it.agenda.estetista.data.statusEnum
import it.agenda.estetista.data.AppSettings
import it.agenda.estetista.data.Operator
import it.agenda.estetista.data.Status
import it.agenda.estetista.util.WhatsApp
import it.agenda.estetista.util.dataLunga
import it.agenda.estetista.util.hhmm
import java.time.LocalDate

/** Elenco dei promemoria da mandare (domani di default): un tocco per cliente. */
@Composable
fun RemindersDialog(
    vm: AgendaViewModel,
    settings: AppSettings,
    operators: List<Operator>,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var day by remember { mutableStateOf(LocalDate.now().plusDays(1)) }
    val flow = remember(day) { vm.appointmentsFor(day) }
    val list by flow.collectAsState(initial = emptyList())
    val todo = list.filter { it.statusEnum != Status.DISDETTO && it.statusEnum != Status.COMPLETATO }
    val names = operators.associate { it.id to it.name }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth(0.9f)
                .padding(vertical = 24.dp),
        ) {
            Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Promemoria WhatsApp", style = SerifTitle.copy(fontSize = 28.sp))
                        Text(dataLunga(day), color = Rose.Muted)
                    }
                    RoseMark(36.dp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val tomorrow = LocalDate.now().plusDays(1)
                    FilterChip(selected = day == LocalDate.now(), onClick = { day = LocalDate.now() }, label = { Text("Oggi") })
                    FilterChip(selected = day == tomorrow, onClick = { day = tomorrow }, label = { Text("Domani") })
                    FilterChip(selected = day == tomorrow.plusDays(1), onClick = { day = tomorrow.plusDays(1) }, label = { Text("Dopodomani") })
                }
                val sent = todo.count { it.reminderSent }
                Text(
                    if (todo.isEmpty()) "Nessun appuntamento da ricordare." else "Inviati $sent su ${todo.size}",
                    style = MaterialTheme.typography.bodyMedium, color = Rose.Muted,
                )

                LazyColumn(Modifier.heightIn(max = 460.dp)) {
                    items(todo, key = { it.id }) { a ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(opColor(a.operatorId).strong)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(hhmm(a.startMin), fontWeight = FontWeight.SemiBold, modifier = Modifier.width(56.dp))
                            Column(Modifier.weight(1f)) {
                                Text(a.clientName, fontWeight = FontWeight.SemiBold)
                                Text(
                                    listOf(a.service, names[a.operatorId].orEmpty()).filter { it.isNotBlank() }.joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall, color = Rose.Muted,
                                )
                            }
                            when {
                                a.phone.isBlank() -> Text("manca il numero", style = MaterialTheme.typography.bodySmall, color = Rose.Muted)
                                a.reminderSent -> OutlinedButton(onClick = { send(context, vm, settings, a) }) { Text("✓ Reinvia") }
                                else -> Button(onClick = { send(context, vm, settings, a) }) { Text("Invia") }
                            }
                        }
                        HorizontalDivider(color = Rose.Line)
                    }
                }
                Row {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Chiudi") }
                }
            }
        }
    }
}

private fun send(context: android.content.Context, vm: AgendaViewModel, s: AppSettings, a: it.agenda.estetista.data.Appointment) {
    val ok = WhatsApp.open(context, a.phone, WhatsApp.message(s.reminderTemplate, a, s))
    if (ok) vm.save(a.copy(reminderSent = true))
    else Toast.makeText(context, "Numero non valido o WhatsApp assente", Toast.LENGTH_SHORT).show()
}
