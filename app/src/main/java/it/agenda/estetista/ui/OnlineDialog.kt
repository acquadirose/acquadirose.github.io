package it.agenda.estetista.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.agenda.estetista.AgendaViewModel
import it.agenda.estetista.data.AppSettings
import it.agenda.estetista.data.Appointment
import it.agenda.estetista.data.OnlineRequest
import it.agenda.estetista.data.Operator
import it.agenda.estetista.data.PORTALE_URL
import it.agenda.estetista.data.ADMIN_EMAIL_DEFAULT
import it.agenda.estetista.data.Status
import it.agenda.estetista.util.IT
import it.agenda.estetista.util.WhatsApp
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

private val fasciaLabel = mapOf("mattina" to "Mattina", "pomeriggio" to "Pomeriggio", "indifferente" to "Orario indifferente")

/** Condivide il link della pagina prenotazioni (WhatsApp, Instagram, SMS…). */
fun shareBookingLink(context: android.content.Context, centerName: String) {
    val text = "Prenota il tuo appuntamento da $centerName qui: $PORTALE_URL"
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(send, "Condividi il link per le prenotazioni"))
}

/** Richieste arrivate dalla pagina web: si fissano in agenda o si rifiutano con un messaggio. */
@Composable
fun OnlineDialog(
    vm: AgendaViewModel,
    settings: AppSettings,
    operators: List<Operator>,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val st by vm.onlineState.collectAsStateWithLifecycle()
    var booking by remember { mutableStateOf<OnlineRequest?>(null) }
    var rejecting by remember { mutableStateOf<OnlineRequest?>(null) }
    val fmt = remember { DateTimeFormatter.ofPattern("EEEE d MMMM", IT) }

    LaunchedEffect(Unit) { vm.refreshOnline() }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .widthIn(max = 820.dp)
                .fillMaxWidth(0.93f)
                .padding(vertical = 24.dp),
        ) {
            Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Richieste online", style = SerifTitle.copy(fontSize = 28.sp))
                        Text(
                            when {
                                !st.connected -> "Tablet non collegato"
                                st.loading -> "Aggiornamento…"
                                else -> "Aggiornate automaticamente ogni minuto"
                            },
                            color = Rose.Muted, style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (st.connected) {
                        IconButton(onClick = { scope.launch { vm.refreshOnline() } }) { Icon(Icons.Filled.Refresh, "Aggiorna") }
                    }
                    IconButton(onClick = { shareBookingLink(context, settings.centerName) }) { Icon(Icons.Filled.Share, "Condividi link prenotazioni") }
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Chiudi") }
                }

                if (!st.connected) {
                    Text(
                        "Per ricevere le richieste dalla pagina web collega il tablet in Impostazioni › Prenotazioni online.",
                        color = Rose.Muted,
                    )
                    Button(onClick = { onDismiss(); onOpenSettings() }) { Text("Apri impostazioni") }
                    return@Column
                }
                st.error?.let {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small) {
                        Text(it, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp))
                    }
                }
                if (st.requests.isEmpty() && st.error == null) {
                    Text("Nessuna richiesta in attesa.", color = Rose.Muted, modifier = Modifier.padding(vertical = 12.dp))
                }

                LazyColumn(Modifier.heightIn(max = 520.dp)) {
                    items(st.requests, key = { it.id }) { r ->
                        Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(r.nome.ifBlank { r.email }, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                                    Text(
                                        r.data.format(fmt).replaceFirstChar { it.uppercase() } + " · " + (fasciaLabel[r.fascia] ?: r.fascia),
                                        color = Rose.Ink,
                                    )
                                    Text(
                                        listOf(r.trattamento, r.telefono).filter { it.isNotBlank() }.joinToString(" · "),
                                        color = Rose.Muted, fontSize = 14.sp,
                                    )
                                }
                            }
                            if (r.note.isNotBlank()) {
                                Text(
                                    "«${r.note}»", fontSize = 14.sp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Rose.PrimarySoft)
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = { booking = r }) { Text("Fissa in agenda") }
                                OutlinedButton(onClick = { rejecting = r }) { Text("Non disponibile") }
                                if (r.telefono.isNotBlank()) {
                                    TextButton(onClick = { WhatsApp.open(context, r.telefono, "") }) { Text("WhatsApp") }
                                }
                            }
                        }
                        HorizontalDivider(color = Rose.Line)
                    }
                }
            }
        }
    }

    booking?.let { r ->
        val start = when (r.fascia) {
            "pomeriggio" -> maxOf(15 * 60, settings.openMin).coerceAtMost(settings.closeMin - settings.slotMin)
            else -> settings.openMin
        }
        val services by vm.services.collectAsStateWithLifecycle()
        val dur = services.firstOrNull { it.name.equals(r.trattamento, ignoreCase = true) }?.durationMin ?: 60
        val ops = operators.ifEmpty { listOf(Operator(1, "Operatrice 1")) }
        AppointmentDialog(
            vm = vm,
            initial = Appointment(
                date = r.data.toEpochDay(), operatorId = ops.first().id, startMin = start, durationMin = dur,
                clientName = r.nome, phone = r.telefono, service = r.trattamento,
                notes = listOf("Richiesta online", r.note).filter { it.isNotBlank() }.joinToString(": "),
                status = Status.CONFERMATO.name,
            ),
            operators = ops,
            settings = settings,
            onDismiss = { booking = null },
            onSaved = { a ->
                val opName = ops.firstOrNull { it.id == a.operatorId }?.name.orEmpty()
                scope.launch {
                    runCatching { vm.confirmOnline(r, a, opName) }
                        .onSuccess { Toast.makeText(context, "Confermato: la cliente vede l'orario sulla pagina", Toast.LENGTH_LONG).show() }
                        .onFailure { Toast.makeText(context, "Appuntamento salvato, ma conferma online non inviata: ${it.message}", Toast.LENGTH_LONG).show() }
                }
            },
        )
    }

    rejecting?.let { r ->
        var msg by remember(r.id) { mutableStateOf("Purtroppo quel giorno siamo al completo. Scrivici su WhatsApp e troviamo insieme un'altra data!") }
        AlertDialog(
            onDismissRequest = { rejecting = null },
            properties = DialogProperties(dismissOnClickOutside = false),
            title = { Text("Richiesta non disponibile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Messaggio che ${r.nome.substringBefore(' ')} vedrà sulla pagina:")
                    OutlinedTextField(msg, { msg = it }, minLines = 3, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    val m = msg
                    rejecting = null
                    scope.launch {
                        runCatching { vm.rejectOnline(r, m) }
                            .onFailure { Toast.makeText(context, "Non inviato: ${it.message}", Toast.LENGTH_LONG).show() }
                    }
                }) { Text("Invia") }
            },
            dismissButton = { TextButton(onClick = { rejecting = null }) { Text("Annulla") } },
        )
    }
}

/** Sezione impostazioni: collegamento del tablet al portale. */
@Composable
fun OnlineSettings(vm: AgendaViewModel, settings: AppSettings) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val st by vm.onlineState.collectAsStateWithLifecycle()
    var email by remember { mutableStateOf(ADMIN_EMAIL_DEFAULT) }
    var pass by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Text(
        "Le clienti richiedono l'appuntamento da questa pagina; le richieste arrivano qui sul tablet.",
        style = MaterialTheme.typography.bodyMedium, color = Rose.Muted,
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(PORTALE_URL, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = { shareBookingLink(context, settings.centerName) }) {
            Icon(Icons.Filled.Share, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Condividi")
        }
    }
    if (st.connected) {
        Text("Collegato come ${st.email}", color = Rose.Aqua, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = {
                scope.launch {
                    runCatching { vm.publishNow() }
                        .onSuccess { Toast.makeText(context, "Listino pubblicato", Toast.LENGTH_SHORT).show() }
                        .onFailure { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
                }
            }) { Text("Pubblica listino ora") }
            TextButton(onClick = { vm.onlineDisconnect() }) { Text("Scollega") }
        }
        st.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(email, { email = it }, label = { Text("Email titolare") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(
                pass, { pass = it }, label = { Text("Password") }, singleLine = true,
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            fun go(create: Boolean) {
                busy = true; error = null
                scope.launch {
                    runCatching { vm.onlineConnect(email, pass, create) }
                        .onSuccess { pass = ""; Toast.makeText(context, "Tablet collegato", Toast.LENGTH_SHORT).show() }
                        .onFailure { error = it.message }
                    busy = false
                }
            }
            Button(onClick = { go(false) }, enabled = !busy && email.isNotBlank() && pass.isNotBlank()) { Text("Collega") }
            OutlinedButton(onClick = { go(true) }, enabled = !busy && email.isNotBlank() && pass.length >= 6) { Text("Crea account titolare") }
        }
        Text(
            "La prima volta usa «Crea account titolare» (password di almeno 6 caratteri); poi basta «Collega».",
            style = MaterialTheme.typography.bodySmall, color = Rose.Muted,
        )
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
