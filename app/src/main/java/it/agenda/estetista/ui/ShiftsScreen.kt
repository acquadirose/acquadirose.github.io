@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package it.agenda.estetista.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.agenda.estetista.AgendaViewModel
import it.agenda.estetista.data.Absence
import it.agenda.estetista.data.Operator
import it.agenda.estetista.data.Shift
import it.agenda.estetista.util.IT
import it.agenda.estetista.util.hhmm
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

private val TIMES = (6 * 60..23 * 60 step 30).toList()
private fun dayShort(dow: Int) = DayOfWeek.of(dow).getDisplayName(TextStyle.SHORT, IT).replaceFirstChar { it.uppercase() }

/** Turni settimanali delle operatrici e assenze (ferie, permessi). */
@Composable
fun ShiftsScreen(vm: AgendaViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val s by vm.settings.collectAsStateWithLifecycle()
    val allOps by vm.operators.collectAsStateWithLifecycle()
    val shifts by vm.shifts.collectAsStateWithLifecycle()
    val absences by vm.absences.collectAsStateWithLifecycle()
    val ops = allOps.take(s.columns)

    var editCell by remember { mutableStateOf<Pair<Operator, Int>?>(null) }
    var addAbsence by remember { mutableStateOf(false) }
    var askClear by remember { mutableStateOf<Operator?>(null) }

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
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro") }
                Text("Turni e assenze", style = SerifTitle.copy(fontSize = 26.sp))
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // ── Orario settimanale ──
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, Rose.Line),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle("Orario settimanale")
                    Text(
                        "Tocca una casella per impostare il turno. Fuori turno l'agenda è ombreggiata e non si possono prendere appuntamenti. " +
                            "Un'operatrice senza turni impostati è sempre disponibile.",
                        style = MaterialTheme.typography.bodyMedium, color = Rose.Muted,
                    )
                    Column(Modifier.horizontalScroll(rememberScrollState())) {
                        Row {
                            Spacer(Modifier.width(170.dp))
                            (1..7).forEach { dow ->
                                Text(
                                    dayShort(dow), fontWeight = FontWeight.SemiBold, color = Rose.Muted,
                                    modifier = Modifier
                                        .width(118.dp)
                                        .padding(start = 6.dp, bottom = 6.dp),
                                )
                            }
                        }
                        ops.forEach { op ->
                            val opShifts = shifts.filter { it.operatorId == op.id }
                            val configured = opShifts.isNotEmpty()
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                Column(Modifier.width(170.dp).padding(end = 8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(opColor(op.id).strong)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(op.name, fontWeight = FontWeight.SemiBold)
                                    }
                                    if (configured) {
                                        TextButton(onClick = { askClear = op }) { Text("Togli turni", fontSize = 12.sp) }
                                    } else {
                                        Text("sempre disponibile", fontSize = 12.sp, color = Rose.Muted)
                                    }
                                }
                                (1..7).forEach { dow ->
                                    val day = opShifts.filter { it.dayOfWeek == dow }.sortedBy { it.startMin }
                                    Column(
                                        Modifier
                                            .width(112.dp)
                                            .heightIn(min = 64.dp)
                                            .padding(end = 6.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                when {
                                                    !configured -> Rose.Ground
                                                    day.isEmpty() -> Rose.Blocked
                                                    else -> opColor(op.id).soft
                                                }
                                            )
                                            .border(1.dp, Rose.Line, RoundedCornerShape(10.dp))
                                            .clickable { editCell = op to dow }
                                            .padding(8.dp),
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        when {
                                            !configured -> Text("—", color = Rose.Muted)
                                            day.isEmpty() -> Text("Riposo", fontSize = 13.sp, color = Rose.Muted)
                                            else -> day.forEach {
                                                Text("${hhmm(it.startMin)}–${hhmm(it.endMin)}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Assenze ──
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, Rose.Line),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionTitle("Ferie e assenze", Modifier.weight(1f))
                        Button(onClick = { addAbsence = true }) { Text("Aggiungi assenza") }
                    }
                    val today = LocalDate.now().toEpochDay()
                    val upcoming = absences.filter { it.toDate >= today }
                    if (upcoming.isEmpty()) Text("Nessuna assenza in programma.", color = Rose.Muted)
                    val f = DateTimeFormatter.ofPattern("EEE d MMM", IT)
                    upcoming.forEach { a ->
                        val name = allOps.firstOrNull { it.id == a.operatorId }?.name ?: "Operatrice ${a.operatorId}"
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(opColor(a.operatorId).strong)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("$name · ${a.reason.ifBlank { "Assenza" }}", fontWeight = FontWeight.SemiBold)
                                val from = LocalDate.ofEpochDay(a.fromDate).format(f)
                                val to = LocalDate.ofEpochDay(a.toDate).format(f)
                                val hours = if (a.startMin == 0 && a.endMin >= 24 * 60) "tutto il giorno" else "${hhmm(a.startMin)}–${hhmm(a.endMin)}"
                                Text(
                                    (if (a.fromDate == a.toDate) from else "dal $from al $to") + " · " + hours,
                                    color = Rose.Muted, fontSize = 13.sp,
                                )
                            }
                            IconButton(onClick = { vm.deleteAbsence(a) }) { Icon(Icons.Filled.Delete, "Elimina assenza", tint = Rose.Muted) }
                        }
                        HorizontalDivider(color = Rose.Line)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    editCell?.let { (op, dow) ->
        ShiftDialog(
            op = op,
            dow = dow,
            current = shifts.filter { it.operatorId == op.id && it.dayOfWeek == dow }.sortedBy { it.startMin },
            configured = shifts.any { it.operatorId == op.id },
            defaultOpen = s.openMin,
            defaultClose = s.closeMin,
            onSave = { days, ranges -> vm.saveShifts(op.id, days, ranges); editCell = null },
            onDismiss = { editCell = null },
        )
    }
    if (addAbsence) {
        AbsenceDialog(ops = ops, onSave = { vm.saveAbsence(it); addAbsence = false }, onDismiss = { addAbsence = false })
    }
    askClear?.let { op ->
        AlertDialog(
            onDismissRequest = { askClear = null },
            title = { Text("Togliere i turni di ${op.name}?") },
            text = { Text("Tornerà sempre disponibile in agenda.") },
            confirmButton = { TextButton(onClick = { vm.clearShifts(op.id); askClear = null }) { Text("Togli turni") } },
            dismissButton = { TextButton(onClick = { askClear = null }) { Text("Annulla") } },
        )
    }
}

/** Turno di un giorno: fino a due fasce (mattina/pomeriggio) o riposo, copiabile su altri giorni. */
@Composable
private fun ShiftDialog(
    op: Operator,
    dow: Int,
    current: List<Shift>,
    configured: Boolean,
    defaultOpen: Int,
    defaultClose: Int,
    onSave: (List<Int>, List<Pair<Int, Int>>) -> Unit,
    onDismiss: () -> Unit,
) {
    var working by remember { mutableStateOf(current.isNotEmpty() || !configured) }
    var s1 by remember { mutableStateOf(current.getOrNull(0)?.startMin ?: defaultOpen) }
    var e1 by remember { mutableStateOf(current.getOrNull(0)?.endMin ?: minOf(13 * 60, defaultClose)) }
    var split by remember { mutableStateOf(current.size >= 2 || current.isEmpty()) }
    var s2 by remember { mutableStateOf(current.getOrNull(1)?.startMin ?: maxOf(15 * 60, e1)) }
    var e2 by remember { mutableStateOf(current.getOrNull(1)?.endMin ?: defaultClose) }
    val days = remember { mutableStateListOf(dow) }

    val valid = !working || (s1 < e1 && (!split || (s2 < e2 && e1 <= s2)))

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(0.9f)) {
            Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("${op.name} · ${DayOfWeek.of(dow).getDisplayName(TextStyle.FULL, IT)}", style = SerifTitle.copy(fontSize = 24.sp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = working, onClick = { working = true }, label = { Text("Al lavoro") })
                    FilterChip(selected = !working, onClick = { working = false }, label = { Text("Riposo") })
                }
                if (working) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Dalle", modifier = Modifier.width(56.dp))
                        Choice(s1, TIMES, ::hhmm, { s1 = it })
                        Text("alle")
                        Choice(e1, TIMES, ::hhmm, { e1 = it })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = split, onCheckedChange = { split = it })
                        Spacer(Modifier.width(10.dp))
                        Text("Pausa pranzo (seconda fascia)")
                    }
                    if (split) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Dalle", modifier = Modifier.width(56.dp))
                            Choice(s2, TIMES, ::hhmm, { s2 = it })
                            Text("alle")
                            Choice(e2, TIMES, ::hhmm, { e2 = it })
                        }
                    }
                    if (!valid) Text("Controlla gli orari: la fine deve venire dopo l'inizio.", color = MaterialTheme.colorScheme.error)
                }
                Column {
                    FieldLabel("Applica anche a")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..7).forEach { d ->
                            FilterChip(
                                selected = d in days,
                                onClick = { if (d in days) { if (d != dow) days.remove(d) } else days.add(d) },
                                label = { Text(dayShort(d)) },
                            )
                        }
                    }
                }
                Row {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Annulla") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val ranges = when {
                                !working -> emptyList()
                                split -> listOf(s1 to e1, s2 to e2)
                                else -> listOf(s1 to e1)
                            }
                            onSave(days.toList(), ranges)
                        },
                        enabled = valid,
                    ) { Text("Salva turno") }
                }
            }
        }
    }
}

@Composable
private fun AbsenceDialog(ops: List<Operator>, onSave: (Absence) -> Unit, onDismiss: () -> Unit) {
    var opId by remember { mutableStateOf(ops.firstOrNull()?.id ?: 1) }
    var from by remember { mutableStateOf(LocalDate.now()) }
    var to by remember { mutableStateOf(LocalDate.now()) }
    var allDay by remember { mutableStateOf(true) }
    var start by remember { mutableStateOf(9 * 60) }
    var end by remember { mutableStateOf(13 * 60) }
    var reason by remember { mutableStateOf("Ferie") }
    var picking by remember { mutableStateOf<String?>(null) }
    val f = DateTimeFormatter.ofPattern("EEE d MMMM yyyy", IT)
    val valid = !to.isBefore(from) && (allDay || start < end)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)) {
        Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(0.9f)) {
            Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Nuova assenza", style = SerifTitle.copy(fontSize = 24.sp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ops.forEach { op ->
                        FilterChip(selected = opId == op.id, onClick = { opId = op.id }, label = { Text(op.name) })
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Ferie", "Permesso", "Malattia", "Formazione").forEach { r ->
                        FilterChip(selected = reason == r, onClick = { reason = r }, label = { Text(r) })
                    }
                }
                OutlinedTextField(reason, { reason = it }, label = { Text("Motivo") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column {
                        FieldLabel("Dal")
                        OutlinedButton(onClick = { picking = "from" }) { Text(from.format(f)) }
                    }
                    Column {
                        FieldLabel("Al")
                        OutlinedButton(onClick = { picking = "to" }) { Text(to.format(f)) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = allDay, onCheckedChange = { allDay = it })
                    Text("Tutto il giorno")
                }
                if (!allDay) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Dalle")
                        Choice(start, TIMES, ::hhmm, { start = it })
                        Text("alle")
                        Choice(end, TIMES, ::hhmm, { end = it })
                    }
                }
                if (to.isBefore(from)) Text("La data di fine è prima di quella di inizio.", color = MaterialTheme.colorScheme.error)
                Row {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Annulla") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(
                                Absence(
                                    operatorId = opId, fromDate = from.toEpochDay(), toDate = to.toEpochDay(),
                                    startMin = if (allDay) 0 else start, endMin = if (allDay) 24 * 60 else end,
                                    reason = reason.trim(),
                                )
                            )
                        },
                        enabled = valid,
                    ) { Text("Salva assenza") }
                }
            }
        }
    }

    picking?.let { which ->
        val init = if (which == "from") from else to
        val state = rememberDatePickerState(initialSelectedDateMillis = init.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        val d = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                        if (which == "from") { from = d; if (to.isBefore(d)) to = d } else to = d
                    }
                    picking = null
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { picking = null }) { Text("Annulla") } },
        ) { DatePicker(state = state) }
    }
}
