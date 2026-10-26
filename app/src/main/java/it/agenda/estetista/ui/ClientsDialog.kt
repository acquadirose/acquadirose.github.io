package it.agenda.estetista.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.agenda.estetista.AgendaViewModel
import it.agenda.estetista.data.AppSettings
import it.agenda.estetista.data.Client
import it.agenda.estetista.data.statusEnum
import it.agenda.estetista.data.birthdayDayMonth
import androidx.compose.runtime.LaunchedEffect
import it.agenda.estetista.util.WhatsApp
import it.agenda.estetista.util.hhmm
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Rubrica clienti.
 * - onPick ≠ null: modalità scelta (dal dialogo appuntamento), pulsante "Usa per l'appuntamento".
 * - onBook ≠ null: pulsante "Nuovo appuntamento" per la cliente selezionata.
 */
@Composable
fun ClientsDialog(
    vm: AgendaViewModel,
    settings: AppSettings,
    onDismiss: () -> Unit,
    onPick: ((Client) -> Unit)? = null,
    onBook: ((Client) -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clients by vm.clients.collectAsStateWithLifecycle()
    val visits by vm.visitCounts.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var creating by remember { mutableStateOf(false) }

    val q = query.trim()
    val digits = q.filter { it.isDigit() }
    val filtered = clients.filter {
        q.isEmpty() || it.name.contains(q, ignoreCase = true) ||
            (digits.length >= 3 && it.phone.filter { ch -> ch.isDigit() }.contains(digits))
    }
    val selected = clients.firstOrNull { it.id == selectedId }

    // Protezione: non si perde una scheda in modifica toccando fuori o cambiando cliente.
    var formDirty by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    fun guarded(action: () -> Unit) { if (formDirty) pending = action else action() }

    Dialog(
        onDismissRequest = { guarded(onDismiss) },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .widthIn(max = 1100.dp)
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
        ) {
            Row(Modifier.fillMaxSize()) {
                // ── Elenco ──
                Column(
                    Modifier
                        .weight(0.42f)
                        .fillMaxHeight()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(if (onPick != null) "Scegli la cliente" else "Clienti", style = SerifTitle.copy(fontSize = 28.sp))
                            Text("${clients.size} in rubrica", color = Rose.Muted, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { guarded(onDismiss) }) { Icon(Icons.Filled.Close, "Chiudi") }
                    }
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        placeholder = { Text("Cerca per nome o telefono") },
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = { guarded { formDirty = false; creating = true; selectedId = null } },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                    ) {
                        Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Nuova cliente")
                    }
                    LazyColumn(Modifier.weight(1f)) {
                        items(filtered, key = { it.id }) { c ->
                            val isSel = c.id == selectedId && !creating
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) Rose.PrimarySoft else Rose.Surface)
                                    .clickable { guarded { formDirty = false; selectedId = c.id; creating = false } }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Initials(c.name)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(c.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (c.phone.isNotBlank()) Text(c.phone, style = MaterialTheme.typography.bodySmall, color = Rose.Muted)
                                }
                                val n = visits[c.id] ?: 0
                                Text(if (n == 1) "1 visita" else "$n visite", style = MaterialTheme.typography.bodySmall, color = Rose.Muted)
                            }
                        }
                        if (filtered.isEmpty()) {
                            item {
                                Text(
                                    if (clients.isEmpty()) "La rubrica si riempie da sola a ogni nuovo appuntamento, oppure aggiungi una cliente qui."
                                    else "Nessuna cliente trovata.",
                                    color = Rose.Muted, modifier = Modifier.padding(12.dp),
                                )
                            }
                        }
                    }
                }

                VerticalDivider(color = Rose.Line)

                // ── Scheda ──
                Box(
                    Modifier
                        .weight(0.58f)
                        .fillMaxHeight()
                        .background(Rose.Ground)
                ) {
                    when {
                        creating -> ClientForm(
                            key = "new",
                            client = Client(name = query.trim().takeIf { it.any { ch -> ch.isLetter() } } ?: ""),
                            vm = vm,
                            settings = settings,
                            onSaved = { id -> formDirty = false; creating = false; selectedId = id },
                            onDeleted = {},
                            onPick = onPick,
                            onBook = onBook,
                            onCancel = { guarded { formDirty = false; creating = false } },
                            onDirty = { formDirty = it },
                        )
                        selected != null -> ClientForm(
                            key = selected.id.toString(),
                            client = selected,
                            vm = vm,
                            settings = settings,
                            onSaved = { formDirty = false },
                            onDeleted = { formDirty = false; selectedId = null },
                            onPick = onPick,
                            onBook = onBook,
                            onCancel = { guarded { formDirty = false; selectedId = null } },
                            onDirty = { formDirty = it },
                        )
                        else -> Column(
                            Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            RoseMark(64.dp)
                            Spacer(Modifier.height(8.dp))
                            Text("Seleziona una cliente", style = SerifTitle.copy(fontSize = 20.sp), color = Rose.Muted)
                        }
                    }
                }
            }
        }
    }
    pending?.let { action ->
        DiscardDialog(onKeep = { pending = null }, onDiscard = { pending = null; formDirty = false; action() })
    }
}

val MESI = listOf(
    "gennaio", "febbraio", "marzo", "aprile", "maggio", "giugno",
    "luglio", "agosto", "settembre", "ottobre", "novembre", "dicembre",
)

/** Conferma prima di perdere dati non salvati. */
@Composable
fun DiscardDialog(onKeep: () -> Unit, onDiscard: () -> Unit) {
    AlertDialog(
        onDismissRequest = onKeep,
        title = { Text("Uscire senza salvare?") },
        text = { Text("Hai inserito dati non ancora salvati: se esci andranno persi.") },
        confirmButton = { Button(onClick = onKeep) { Text("Continua a scrivere") } },
        dismissButton = {
            TextButton(
                onClick = onDiscard,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text("Esci senza salvare") }
        },
    )
}

@Composable
private fun Initials(name: String) {
    val ini = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2)
        .joinToString("") { it.first().uppercase() }
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Rose.AquaSoft),
        contentAlignment = Alignment.Center,
    ) {
        Text(ini.ifEmpty { "?" }, fontWeight = FontWeight.Bold, color = Rose.Aqua)
    }
}

@Composable
private fun ClientForm(
    key: String,
    client: Client,
    vm: AgendaViewModel,
    settings: AppSettings,
    onSaved: (Long) -> Unit,
    onDeleted: () -> Unit,
    onPick: ((Client) -> Unit)?,
    onBook: ((Client) -> Unit)?,
    onCancel: () -> Unit,
    onDirty: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember(key) { mutableStateOf(client.name) }
    var phone by remember(key) { mutableStateOf(client.phone) }
    var email by remember(key) { mutableStateOf(client.email) }
    val parsedBirthday = remember(key) { client.birthdayDayMonth() }
    var bDay by remember(key) { mutableStateOf(parsedBirthday?.first ?: 0) }
    var bMonth by remember(key) { mutableStateOf(parsedBirthday?.second ?: 0) }
    val birthday = if (bDay > 0 && bMonth > 0) "%02d/%02d".format(bDay, bMonth) else ""
    var notes by remember(key) { mutableStateOf(client.notes) }
    var askDelete by remember { mutableStateOf(false) }

    val isNew = client.id == 0L
    val current = client.copy(name = name, phone = phone, email = email, birthday = birthday, notes = notes)
    val originalBirthday = parsedBirthday?.let { "%02d/%02d".format(it.first, it.second) } ?: ""
    val dirty = if (isNew) {
        name.isNotBlank() || phone.isNotBlank() || email.isNotBlank() || notes.isNotBlank() || birthday.isNotEmpty()
    } else {
        name != client.name || phone != client.phone || email != client.email ||
            notes != client.notes || birthday != originalBirthday
    }
    LaunchedEffect(dirty) { onDirty(dirty) }

    fun save(after: (Client) -> Unit = {}) {
        scope.launch {
            val id = vm.saveClient(current)
            onSaved(id)
            after(current.copy(id = id, name = name.trim(), phone = phone.trim()))
            Toast.makeText(context, "Scheda salvata", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(if (isNew) "Nuova cliente" else client.name, style = SerifTitle.copy(fontSize = 26.sp))

        OutlinedTextField(
            name, { name = it }, label = { Text("Nome e cognome") }, singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                phone, { phone = it }, label = { Text("Cellulare") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.weight(1f),
            )
            Column(Modifier.weight(1f)) {
                FieldLabel("Compleanno")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Choice(bDay, (0..31).toList(), { if (it == 0) "Giorno" else it.toString() }, { bDay = it })
                    Choice(bMonth, (0..12).toList(), { if (it == 0) "Mese" else MESI[it - 1] }, { bMonth = it })
                }
            }
        }
        OutlinedTextField(
            email, { email = it }, label = { Text("Email") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            notes, { notes = it },
            label = { Text("Note (allergie, tipo di pelle, preferenze...)") },
            minLines = 3, modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { save() }, enabled = name.isNotBlank() && (dirty || isNew), modifier = Modifier.height(48.dp)) {
                Text(if (isNew) "Salva in rubrica" else "Salva modifiche")
            }
            if (onPick != null) {
                Button(
                    onClick = { if (dirty) save { onPick(it) } else onPick(client) },
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Rose.Aqua),
                    modifier = Modifier.height(48.dp),
                ) { Text("Usa per l'appuntamento") }
            } else if (onBook != null && !isNew) {
                Button(
                    onClick = { onBook(client) },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose.Aqua),
                    modifier = Modifier.height(48.dp),
                ) { Text("Nuovo appuntamento") }
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onCancel) { Text("Chiudi scheda") }
        }

        if (phone.isNotBlank()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = {
                    if (!WhatsApp.open(context, phone, "")) {
                        Toast.makeText(context, "Numero non valido o WhatsApp assente", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Scrivi su WhatsApp") }
                OutlinedButton(onClick = { WhatsApp.call(context, phone) }) {
                    Icon(Icons.Filled.Call, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Chiama")
                }
            }
        }

        if (!isNew) {
            Ornament(Modifier.padding(vertical = 6.dp))
            Text("Storico appuntamenti", style = SerifTitle.copy(fontSize = 20.sp))
            val historyFlow = remember(client.id) { vm.appointmentsOfClient(client.id) }
            val history by historyFlow.collectAsState(initial = emptyList())
            if (history.isEmpty()) {
                Text("Nessun appuntamento collegato.", color = Rose.Muted)
            }
            val fmt = remember { DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ITALIAN) }
            val today = LocalDate.now().toEpochDay()
            history.forEach { a ->
                val st = a.statusEnum
                val (bg, fg) = statusColors(st)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        LocalDate.ofEpochDay(a.date).format(fmt) + "  " + hhmm(a.startMin),
                        fontWeight = if (a.date >= today) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.width(210.dp),
                    )
                    Text(a.service.ifBlank { "—" }, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        st.label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = fg,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(bg)
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }
                HorizontalDivider(color = Rose.Line)
            }

            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = { askDelete = true },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text("Elimina dalla rubrica") }
        }
    }

    if (askDelete) {
        AlertDialog(
            onDismissRequest = { askDelete = false },
            title = { Text("Eliminare ${client.name}?") },
            text = { Text("Gli appuntamenti restano in agenda, ma non saranno più collegati alla scheda.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteClient(client); askDelete = false; onDeleted() }) { Text("Elimina") }
            },
            dismissButton = { TextButton(onClick = { askDelete = false }) { Text("Annulla") } },
        )
    }
}
