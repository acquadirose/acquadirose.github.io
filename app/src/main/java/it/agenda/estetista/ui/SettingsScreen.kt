@file:OptIn(ExperimentalLayoutApi::class)

package it.agenda.estetista.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.agenda.estetista.AgendaViewModel
import it.agenda.estetista.data.DEFAULT_TEMPLATE
import it.agenda.estetista.data.DEFAULT_BIRTHDAY_TEMPLATE
import it.agenda.estetista.data.Service
import it.agenda.estetista.util.durata
import it.agenda.estetista.util.hhmm
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun SettingsScreen(vm: AgendaViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val saved by vm.settings.collectAsStateWithLifecycle()
    val operators by vm.operators.collectAsStateWithLifecycle()
    val services by vm.services.collectAsStateWithLifecycle()

    var s by remember { mutableStateOf(saved) }
    val names = remember { mutableStateMapOf<Int, String>() }
    LaunchedEffect(operators) { operators.forEach { if (it.id !in names) names[it.id] = it.name } }

    var svcName by remember { mutableStateOf("") }
    var svcDur by remember { mutableStateOf(60) }
    var svcEditing by remember { mutableStateOf<Service?>(null) }
    var confirmImport by remember { mutableStateOf<android.net.Uri?>(null) }
    var changePwd by remember { mutableStateOf(false) }

    // Uscire con modifiche non salvate chiede conferma.
    val unsaved = s.copy(logoVersion = saved.logoVersion) != saved ||
        operators.any { op -> names[op.id] != null && names[op.id] != op.name }
    var askDiscard by remember { mutableStateOf(false) }
    fun tryBack() { if (unsaved) askDiscard = true else onBack() }
    BackHandler(onBack = { tryBack() })

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

    val pickLogo = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            runCatching { vm.setLogo(uri) }
                .onSuccess { s = s.copy(logoVersion = vm.settings.value.logoVersion); toast("Logo aggiornato") }
                .onFailure { toast("Immagine non valida") }
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            runCatching { vm.exportTo(uri) }
                .onSuccess { toast("Backup salvato") }
                .onFailure { toast("Errore nel backup: ${it.message}") }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) confirmImport = uri
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { tryBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro") }
                Text("Impostazioni", style = SerifTitle.copy(fontSize = 26.sp), modifier = Modifier.weight(1f))
                Button(onClick = {
                    vm.updateSettings(s.copy(logoVersion = vm.settings.value.logoVersion))
                    vm.renameOperators(names.toMap())
                    toast("Impostazioni salvate")
                    onBack()
                }, modifier = Modifier.height(48.dp)) { Text("Salva") }
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier
                    .widthIn(max = 880.dp)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                // ── Centro ──
                SettingsCard("Il centro") {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        CenterLogo(saved.logoVersion, 96.dp)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { pickLogo.launch("image/*") }) { Text("Cambia logo") }
                            TextButton(onClick = {
                                scope.launch { vm.setLogo(null); toast("Logo originale ripristinato") }
                            }) { Text("Ripristina logo originale") }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(s.centerName, { s = s.copy(centerName = it) }, label = { Text("Nome") },
                            singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(s.city, { s = s.copy(city = it) }, label = { Text("Città") },
                            singleLine = true, modifier = Modifier.weight(1f))
                    }
                }

                // ── Operatrici ──
                SettingsCard("Colonne e operatrici") {
                    FieldLabel("Quante colonne mostrare")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..4).forEach { n ->
                            FilterChip(
                                selected = s.columns == n,
                                onClick = { s = s.copy(columns = n) },
                                label = { Text(if (n == 1) "1 colonna" else "$n colonne") },
                            )
                        }
                    }
                    (1..s.columns).forEach { id ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(opColor(id).strong)
                            )
                            Spacer(Modifier.width(12.dp))
                            OutlinedTextField(
                                value = names[id].orEmpty(),
                                onValueChange = { names[id] = it },
                                label = { Text("Colonna $id") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    Text(
                        "Nascondere una colonna non cancella i suoi appuntamenti: ricompaiono riattivandola.",
                        style = MaterialTheme.typography.bodySmall, color = Rose.Muted,
                    )
                }

                // ── Orari ──
                SettingsCard("Orari") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column {
                            FieldLabel("Apertura")
                            Choice(s.openMin, (6 * 60..13 * 60 step 30).toList(), { hhmm(it) }, {
                                s = s.copy(openMin = it, closeMin = maxOf(s.closeMin, it + 60))
                            })
                        }
                        Column {
                            FieldLabel("Chiusura")
                            Choice(s.closeMin, (12 * 60..23 * 60 step 30).toList(), { hhmm(it) }, {
                                s = s.copy(closeMin = it, openMin = minOf(s.openMin, it - 60))
                            })
                        }
                        Column {
                            FieldLabel("Griglia")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(10, 15, 30).forEach { m ->
                                    FilterChip(selected = s.slotMin == m, onClick = { s = s.copy(slotMin = m) }, label = { Text("$m min") })
                                }
                            }
                        }
                    }
                }

                // ── Listino ──
                SettingsCard("Trattamenti") {
                    services.forEach { sv ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(
                                onClick = { svcEditing = sv; svcName = sv.name; svcDur = sv.durationMin },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(sv.name, color = Rose.Ink, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                Text(durata(sv.durationMin), color = Rose.Muted)
                            }
                            IconButton(onClick = { vm.deleteService(sv) }) {
                                Icon(Icons.Filled.Delete, "Elimina ${sv.name}", tint = Rose.Muted)
                            }
                        }
                        HorizontalDivider(color = Rose.Line)
                    }
                    Spacer(Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedTextField(
                            svcName, { svcName = it },
                            label = { Text(if (svcEditing == null) "Nuovo trattamento" else "Modifica trattamento") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier.widthIn(min = 260.dp),
                        )
                        Stepper(svcDur, 5, 5, 480, ::durata) { svcDur = it }
                        Button(
                            onClick = {
                                vm.saveService((svcEditing ?: Service(name = "", durationMin = 0)).copy(name = svcName.trim(), durationMin = svcDur))
                                svcEditing = null; svcName = ""; svcDur = 60
                            },
                            enabled = svcName.isNotBlank(),
                        ) { Text(if (svcEditing == null) "Aggiungi" else "Aggiorna") }
                        if (svcEditing != null) {
                            TextButton(onClick = { svcEditing = null; svcName = ""; svcDur = 60 }) { Text("Annulla") }
                        }
                    }
                }

                // ── Messaggio ──
                SettingsCard("Messaggio di promemoria") {
                    OutlinedTextField(
                        s.reminderTemplate, { s = s.copy(reminderTemplate = it) },
                        minLines = 4, modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Parole sostituite in automatico: {nome} {giorno} {ora} {servizio} {centro} {citta}",
                        style = MaterialTheme.typography.bodySmall, color = Rose.Muted,
                    )
                    TextButton(onClick = { s = s.copy(reminderTemplate = DEFAULT_TEMPLATE) }) { Text("Ripristina testo originale") }
                }

                // ── Compleanni ──
                SettingsCard("Auguri di compleanno") {
                    Text(
                        "A inizio mese l'agenda mostra chi festeggia il compleanno: con un tocco parte il messaggio WhatsApp con lo sconto.",
                        style = MaterialTheme.typography.bodyMedium, color = Rose.Muted,
                    )
                    OutlinedTextField(
                        s.birthdayDiscount, { s = s.copy(birthdayDiscount = it) },
                        label = { Text("Sconto in regalo (es. 10%, 15 €)") }, singleLine = true,
                        modifier = Modifier.widthIn(max = 320.dp),
                    )
                    OutlinedTextField(
                        s.birthdayTemplate, { s = s.copy(birthdayTemplate = it) },
                        label = { Text("Messaggio") }, minLines = 4, modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "Parole sostituite in automatico: {nome} {sconto} {scadenza} (fine del mese) {mese} {centro} {citta}",
                        style = MaterialTheme.typography.bodySmall, color = Rose.Muted,
                    )
                    TextButton(onClick = { s = s.copy(birthdayTemplate = DEFAULT_BIRTHDAY_TEMPLATE) }) { Text("Ripristina testo originale") }
                }

                // ── Prenotazioni online ──
                SettingsCard("Prenotazioni online") {
                    OnlineSettings(vm, saved)
                }

                // ── Sicurezza ──
                SettingsCard("Password di accesso") {
                    Text(
                        "L'app chiede la password all'apertura e dopo 5 minuti in secondo piano.",
                        style = MaterialTheme.typography.bodyMedium, color = Rose.Muted,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { changePwd = true }) { Text("Cambia password") }
                        TextButton(onClick = { vm.lock() }) { Text("Blocca ora") }
                    }
                }

                // ── Backup ──
                SettingsCard("Backup") {
                    Text(
                        "I dati sono solo su questo tablet. Fai un backup almeno una volta a settimana " +
                            "(su Google Drive o su una chiavetta): se il tablet si rompe, si ripristina tutto da qui.",
                        style = MaterialTheme.typography.bodyMedium, color = Rose.Muted,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { exportLauncher.launch("agenda-acqua-di-rose-${LocalDate.now()}.json") }) {
                            Text("Salva backup")
                        }
                        OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }) {
                            Text("Ripristina da backup")
                        }
                    }
                }

                Ornament(Modifier.padding(vertical = 8.dp))
                Text(
                    "${saved.centerName} · ${saved.city}",
                    style = SerifTitle.copy(fontSize = 16.sp), color = Rose.Muted,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }

    if (changePwd) {
        ChangePasswordDialog(onDone = { msg -> changePwd = false; if (msg != null) toast(msg) })
    }
    if (askDiscard) {
        DiscardDialog(onKeep = { askDiscard = false }, onDiscard = { askDiscard = false; onBack() })
    }
    confirmImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { confirmImport = null },
            title = { Text("Ripristinare il backup?") },
            text = { Text("Tutti gli appuntamenti e i trattamenti attuali verranno sostituiti con quelli del file.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmImport = null
                    scope.launch {
                        runCatching { vm.importFrom(uri) }
                            .onSuccess { n -> s = vm.settings.value; names.clear(); toast("Ripristinati $n appuntamenti") }
                            .onFailure { toast("Errore: ${it.message}") }
                    }
                }) { Text("Ripristina") }
            },
            dismissButton = { TextButton(onClick = { confirmImport = null }) { Text("Annulla") } },
        )
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Rose.Line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(title)
            content()
        }
    }
}

@Composable
private fun ChangePasswordDialog(onDone: (String?) -> Unit) {
    val context = LocalContext.current
    val security = remember { it.agenda.estetista.data.Security(context) }
    var old by remember { mutableStateOf("") }
    var new1 by remember { mutableStateOf("") }
    var new2 by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val digits = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword)
    val hidden = androidx.compose.ui.text.input.PasswordVisualTransformation()

    AlertDialog(
        onDismissRequest = { onDone(null) },
        properties = androidx.compose.ui.window.DialogProperties(dismissOnClickOutside = false),
        title = { Text("Cambia password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(old, { old = it.filter(Char::isDigit).take(8) }, label = { Text("Password attuale") },
                    singleLine = true, keyboardOptions = digits, visualTransformation = hidden)
                OutlinedTextField(new1, { new1 = it.filter(Char::isDigit).take(8) }, label = { Text("Nuova password (4-8 cifre)") },
                    singleLine = true, keyboardOptions = digits, visualTransformation = hidden)
                OutlinedTextField(new2, { new2 = it.filter(Char::isDigit).take(8) }, label = { Text("Ripeti la nuova password") },
                    singleLine = true, keyboardOptions = digits, visualTransformation = hidden)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                error = when {
                    !security.check(old) -> "La password attuale non è corretta"
                    new1.length < 4 -> "La nuova password deve avere almeno 4 cifre"
                    new1 != new2 -> "Le due nuove password non coincidono"
                    else -> null
                }
                if (error == null) {
                    security.setPassword(new1)
                    onDone("Password cambiata")
                }
            }) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = { onDone(null) }) { Text("Annulla") } },
    )
}
