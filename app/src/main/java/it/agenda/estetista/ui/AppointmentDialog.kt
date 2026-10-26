@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package it.agenda.estetista.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.agenda.estetista.AgendaViewModel
import it.agenda.estetista.data.endMin
import it.agenda.estetista.data.statusEnum
import it.agenda.estetista.data.AppSettings
import it.agenda.estetista.data.Appointment
import it.agenda.estetista.data.Client
import it.agenda.estetista.data.Operator
import it.agenda.estetista.data.Status
import it.agenda.estetista.util.WhatsApp
import it.agenda.estetista.util.dataLunga
import it.agenda.estetista.util.durata
import it.agenda.estetista.util.hhmm
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun AppointmentDialog(
    vm: AgendaViewModel,
    initial: Appointment,
    operators: List<Operator>,
    settings: AppSettings,
    onDismiss: () -> Unit,
    onSaved: ((Appointment) -> Unit)? = null,
) {
    val context = LocalContext.current
    val services by vm.services.collectAsStateWithLifecycle()
    val isNew = initial.id == 0L

    var clientName by remember(initial) { mutableStateOf(initial.clientName) }
    var phone by remember(initial) { mutableStateOf(initial.phone) }
    var service by remember(initial) { mutableStateOf(initial.service) }
    var notes by remember(initial) { mutableStateOf(initial.notes) }
    var operatorId by remember(initial) { mutableStateOf(initial.operatorId) }
    var date by remember(initial) { mutableStateOf(LocalDate.ofEpochDay(initial.date)) }
    var startMin by remember(initial) { mutableStateOf(initial.startMin) }
    var duration by remember(initial) { mutableStateOf(initial.durationMin) }
    var status by remember(initial) { mutableStateOf(initial.statusEnum) }
    var reminderSent by remember(initial) { mutableStateOf(initial.reminderSent) }
    var clientId by remember(initial) { mutableStateOf(initial.clientId) }
    var showPicker by remember { mutableStateOf(false) }
    val allClients by vm.clients.collectAsStateWithLifecycle()
    val linked = allClients.firstOrNull { it.id == clientId }

    fun useClient(c: Client) {
        clientId = c.id
        clientName = c.name
        if (c.phone.isNotBlank()) phone = c.phone
    }

    var suggestions by remember { mutableStateOf<List<Client>>(emptyList()) }
    var conflicts by remember { mutableStateOf<List<Appointment>>(emptyList()) }
    var serviceMenu by remember { mutableStateOf(false) }
    var askDelete by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }

    fun draft() = initial.copy(
        date = date.toEpochDay(), operatorId = operatorId, startMin = startMin, durationMin = duration,
        clientName = clientName.trim(), phone = phone.trim(), service = service.trim(), notes = notes.trim(),
        status = status.name, reminderSent = reminderSent, clientId = clientId,
    )

    LaunchedEffect(clientName) {
        suggestions = if (clientName.trim().length >= 2 && clientId == null) {
            vm.searchClients(clientName)
        } else emptyList()
    }
    LaunchedEffect(operatorId, startMin, duration, date) {
        conflicts = vm.conflicts(draft())
    }

    val slots = remember(settings) {
        (settings.openMin until settings.closeMin step settings.slotMin).toList()
    }
    // Turni: non si prenota un'operatrice fuori turno o assente.
    val avail by vm.availability.collectAsStateWithLifecycle()
    val sameSlot = !isNew && operatorId == initial.operatorId && startMin == initial.startMin &&
        duration == initial.durationMin && date.toEpochDay() == initial.date
    val inShift = avail.isAvailable(operatorId, date, startMin, startMin + duration)
    val canSave = clientName.isNotBlank() && (inShift || sameSlot)

    // Non chiudere perdendo i dati: tocco fuori disattivato, conferma su Indietro/Annulla.
    val dirty = if (isNew) {
        clientName.isNotBlank() || phone.isNotBlank() || service.isNotBlank() || notes.isNotBlank()
    } else {
        draft().copy(reminderSent = initial.reminderSent) != initial.copy(clientName = initial.clientName.trim())
    }
    var askDiscard by remember { mutableStateOf(false) }
    fun tryClose() { if (dirty) askDiscard = true else onDismiss() }

    Dialog(
        onDismissRequest = { tryClose() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isNew) "Nuovo appuntamento" else "Appuntamento",
                        style = SerifTitle.copy(fontSize = 28.sp),
                        modifier = Modifier.weight(1f),
                    )
                    RoseMark(36.dp)
                }

                // Cliente
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = {
                            clientName = it
                            if (linked != null && !it.trim().equals(linked.name, ignoreCase = true)) clientId = null
                        },
                        label = { Text("Cliente") }, singleLine = true,
                        supportingText = {
                            Text(
                                when {
                                    linked != null -> "Dalla rubrica"
                                    clientName.isNotBlank() -> "Nuova cliente: verrà aggiunta alla rubrica"
                                    else -> "Scrivi il nome o apri la rubrica"
                                }
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { showPicker = true }) {
                                Icon(Icons.Filled.Person, "Apri rubrica clienti")
                            }
                        },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.weight(1.4f),
                    )
                    OutlinedTextField(
                        value = phone, onValueChange = { phone = it },
                        label = { Text("Cellulare") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.weight(1f),
                    )
                }
                if (suggestions.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestions.forEach { c ->
                            AssistChip(
                                onClick = { useClient(c); suggestions = emptyList() },
                                label = { Text(if (c.phone.isBlank()) c.name else "${c.name} · ${c.phone}") },
                                leadingIcon = { Icon(Icons.Filled.Person, null, Modifier.size(16.dp)) },
                            )
                        }
                    }
                }
                if (linked != null && linked.notes.isNotBlank()) {
                    Surface(color = Rose.PrimarySoft, shape = MaterialTheme.shapes.small) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Dalla scheda cliente", style = MaterialTheme.typography.labelMedium, color = Rose.Muted)
                            Text(linked.notes, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Servizio
                Column {
                    OutlinedTextField(
                        value = service, onValueChange = { service = it },
                        label = { Text("Trattamento") }, singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { serviceMenu = true }) {
                                Icon(Icons.Filled.ArrowDropDown, "Scegli dal listino")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DropdownMenu(expanded = serviceMenu, onDismissRequest = { serviceMenu = false }) {
                        services.forEach { sv ->
                            DropdownMenuItem(
                                text = { Text("${sv.name}  ·  ${durata(sv.durationMin)}") },
                                onClick = { service = sv.name; duration = sv.durationMin; serviceMenu = false },
                            )
                        }
                    }
                }

                // Operatrice
                if (operators.size > 1) {
                    Column {
                        FieldLabel("Operatrice")
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            operators.forEach { op ->
                                val c = opColor(op.id)
                                FilterChip(
                                    selected = operatorId == op.id,
                                    onClick = { operatorId = op.id },
                                    label = { Text(op.name) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = c.soft, selectedLabelColor = Rose.Ink,
                                    ),
                                )
                            }
                        }
                    }
                }

                // Data, ora, durata
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column {
                        FieldLabel("Giorno")
                        OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.height(48.dp)) {
                            Text(dataLunga(date))
                        }
                    }
                    Column {
                        FieldLabel("Ora")
                        Choice(
                            current = startMin,
                            options = if (startMin in slots) slots else (slots + startMin).sorted(),
                            text = { hhmm(it) },
                            onPick = { startMin = it },
                        )
                    }
                    Column {
                        FieldLabel("Durata")
                        Stepper(duration, settings.slotMin, settings.slotMin, 8 * 60, ::durata) { duration = it }
                    }
                }
                Text(
                    "Fine prevista: ${hhmm(startMin + duration)}",
                    style = MaterialTheme.typography.bodySmall, color = Rose.Muted,
                )
                if (!inShift && status != Status.DISDETTO) {
                    val opName = operators.firstOrNull { it.id == operatorId }?.name ?: "L'operatrice"
                    val work = avail.workIntervals(operatorId, date)
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small) {
                        Text(
                            "$opName non è disponibile in questo orario (" + avail.reasonUnavailable(operatorId, date).lowercase() + "). " +
                                (if (work.isEmpty()) "Scegli un altro giorno o un'altra operatrice."
                                else "In turno: " + work.joinToString { "${hhmm(it.first)}–${hhmm(it.second)}" } + "."),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
                if (conflicts.isNotEmpty()) {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.small) {
                        Text(
                            "Attenzione, si sovrappone a: " + conflicts.joinToString { "${it.clientName} ${hhmm(it.startMin)}–${hhmm(it.endMin)}" },
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }

                // Stato
                Column {
                    FieldLabel("Stato")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Status.entries.forEach { st ->
                            val (bg, fg) = statusColors(st)
                            FilterChip(
                                selected = status == st,
                                onClick = { status = st },
                                label = { Text(st.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = bg, selectedLabelColor = fg,
                                ),
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Note (allergie, preferenze, colore smalto...)") },
                    minLines = 2, modifier = Modifier.fillMaxWidth(),
                )

                // WhatsApp e telefono
                if (phone.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = {
                            val ok = WhatsApp.open(context, phone, WhatsApp.message(settings.reminderTemplate, draft(), settings))
                            if (ok) {
                                reminderSent = true
                                if (!isNew) vm.save(draft())
                            }
                            else Toast.makeText(context, "Numero non valido o WhatsApp assente", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (reminderSent) "Reinvia promemoria" else "Promemoria WhatsApp")
                        }
                        OutlinedButton(onClick = { WhatsApp.call(context, phone) }) {
                            Icon(Icons.Filled.Call, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Chiama")
                        }
                        if (reminderSent) Text("✓ promemoria inviato", color = Rose.Aqua, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Ornament(Modifier.padding(vertical = 4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isNew) {
                        TextButton(
                            onClick = { askDelete = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Icon(Icons.Filled.Delete, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Elimina")
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { tryClose() }) { Text("Annulla") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { val a = draft(); vm.save(a); onSaved?.invoke(a); onDismiss() },
                        enabled = canSave,
                        modifier = Modifier.height(48.dp),
                    ) { Text(if (isNew) "Prenota" else "Salva") }
                }
            }
        }
    }

    if (askDiscard) {
        DiscardDialog(onKeep = { askDiscard = false }, onDiscard = { askDiscard = false; onDismiss() })
    }
    if (showPicker) {
        ClientsDialog(
            vm = vm,
            settings = settings,
            onDismiss = { showPicker = false },
            onPick = { c -> useClient(c); showPicker = false },
        )
    }
    if (askDelete) {
        AlertDialog(
            onDismissRequest = { askDelete = false },
            title = { Text("Eliminare l'appuntamento?") },
            text = { Text("Se la cliente ha disdetto puoi invece impostare lo stato «Disdetto» e conservarlo nello storico.") },
            confirmButton = {
                TextButton(onClick = { vm.delete(initial); askDelete = false; onDismiss() }) { Text("Elimina") }
            },
            dismissButton = { TextButton(onClick = { askDelete = false }) { Text("Annulla") } },
        )
    }
    if (pickDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    pickDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Annulla") } },
        ) { DatePicker(state = state) }
    }
}
