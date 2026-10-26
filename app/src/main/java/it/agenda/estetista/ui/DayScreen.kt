@file:OptIn(ExperimentalMaterial3Api::class)

package it.agenda.estetista.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import it.agenda.estetista.R
import it.agenda.estetista.ViewMode
import it.agenda.estetista.util.IT
import it.agenda.estetista.util.capitalizeIt
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.agenda.estetista.AgendaViewModel
import it.agenda.estetista.data.AppSettings
import it.agenda.estetista.data.Appointment
import it.agenda.estetista.data.Client
import it.agenda.estetista.data.Availability
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Size
import it.agenda.estetista.data.Operator
import it.agenda.estetista.data.Status
import it.agenda.estetista.data.endMin
import it.agenda.estetista.data.statusEnum
import it.agenda.estetista.util.dataLunga
import it.agenda.estetista.util.hhmm
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/** Scala verticale della griglia: 2 dp per minuto = 120 dp per ora. */
private const val DP_PER_MIN = 2f
private val TIME_COL = 82.dp

@Composable
fun DayScreen(vm: AgendaViewModel, onOpenSettings: () -> Unit, onOpenStats: () -> Unit, onOpenShifts: () -> Unit) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val day by vm.day.collectAsStateWithLifecycle()
    val mode by vm.mode.collectAsStateWithLifecycle()
    val allOps by vm.operators.collectAsStateWithLifecycle()
    val appts by vm.appointments.collectAsStateWithLifecycle()
    val clients by vm.clients.collectAsStateWithLifecycle()
    val avail by vm.availability.collectAsStateWithLifecycle()

    val ops = allOps.take(s.columns)
    var editing by remember { mutableStateOf<Appointment?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showReminders by remember { mutableStateOf(false) }
    var showCancelled by remember { mutableStateOf(false) }
    var showClients by remember { mutableStateOf(false) }
    var showPostIt by remember { mutableStateOf(false) }
    var showBirthdays by remember { mutableStateOf(false) }
    var showOnline by remember { mutableStateOf(false) }
    val onlineSt by vm.onlineState.collectAsStateWithLifecycle()
    val postIts by vm.postIts.collectAsStateWithLifecycle()

    val thisMonth = YearMonth.now()
    val birthdaysToSend = remember(clients) { vm.birthdaysIn(thisMonth, clients).count { !it.sent } }

    var nowMin by remember { mutableIntStateOf(LocalTime.now().let { it.hour * 60 + it.minute }) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            nowMin = LocalTime.now().let { it.hour * 60 + it.minute }
        }
    }
    val isToday = day == LocalDate.now()

    fun newAt(date: LocalDate, opId: Int, startMin: Int, client: Client? = null) {
        editing = Appointment(
            date = date.toEpochDay(), operatorId = opId, startMin = startMin, durationMin = 60,
            clientName = client?.name ?: "", phone = client?.phone ?: "", service = "", notes = "",
            clientId = client?.id,
        )
    }
    fun firstFreeStart(): Int =
        if (isToday) roundUp(nowMin, s.slotMin).coerceIn(s.openMin, s.closeMin - s.slotMin) else s.openMin
    fun openDay(d: LocalDate) { vm.setDay(d); vm.setMode(ViewMode.GIORNO) }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            AgendaHeader(
                s = s, day = day, mode = mode, isToday = isToday, nowMin = nowMin,
                postItCount = postIts.count { !it.done },
                birthdayCount = birthdaysToSend,
                showCancelled = showCancelled,
                onMode = vm::setMode,
                onPrev = { vm.shift(-1) },
                onNext = { vm.shift(1) },
                onToday = { vm.setDay(LocalDate.now()) },
                onPickDate = { showDatePicker = true },
                onClients = { showClients = true },
                onPostIt = { showPostIt = true },
                onReminders = { showReminders = true },
                onBirthdays = { showBirthdays = true },
                onlineCount = onlineSt.requests.size,
                onOnline = { showOnline = true },
                onStats = onOpenStats,
                onToggleCancelled = { showCancelled = !showCancelled },
                onSettings = onOpenSettings,
                onShifts = onOpenShifts,
            )

            if (birthdaysToSend > 0) {
                BirthdayBanner(birthdaysToSend, thisMonth) { showBirthdays = true }
            }
            PostItStrip(vm, onOpen = { showPostIt = true })

            when (mode) {
                ViewMode.GIORNO -> DayGrid(
                    s = s, ops = ops, appts = appts, day = day, isToday = isToday, nowMin = nowMin,
                    showCancelled = showCancelled, avail = avail,
                    onEmptyTap = { opId, m -> newAt(day, opId, m) },
                    onOpen = { editing = it },
                    modifier = Modifier.weight(1f),
                )
                ViewMode.SETTIMANA -> WeekView(
                    vm = vm, day = day, operators = allOps, showCancelled = showCancelled,
                    onOpenDay = ::openDay, onOpen = { editing = it },
                    modifier = Modifier.weight(1f),
                )
                ViewMode.MESE -> MonthView(
                    vm = vm, day = day, operators = allOps, showCancelled = showCancelled,
                    onOpenDay = ::openDay,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Nuovo appuntamento: sempre raggiungibile, in qualunque orientamento.
        FloatingActionButton(
            onClick = {
                val d = if (mode == ViewMode.GIORNO) day else if (day < LocalDate.now()) LocalDate.now() else day
                newAt(d, ops.firstOrNull()?.id ?: 1, if (d == LocalDate.now()) firstFreeStart() else s.openMin)
            },
            containerColor = Rose.Primary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(68.dp),
        ) { Icon(Icons.Filled.Add, "Nuovo appuntamento", Modifier.size(32.dp)) }
    }

    // ── Finestre ──
    editing?.let { a ->
        AppointmentDialog(
            vm = vm,
            initial = a,
            operators = ops.ifEmpty { allOps.take(1) },
            settings = s,
            onDismiss = { editing = null },
        )
    }
    if (showClients) {
        ClientsDialog(
            vm = vm,
            settings = s,
            onDismiss = { showClients = false },
            onBook = { c ->
                showClients = false
                newAt(day, ops.firstOrNull()?.id ?: 1, firstFreeStart(), c)
            },
        )
    }
    if (showPostIt) {
        PostItDialog(vm = vm, day = day, onDismiss = { showPostIt = false })
    }
    if (showReminders) {
        RemindersDialog(vm = vm, settings = s, operators = allOps, onDismiss = { showReminders = false })
    }
    if (showOnline) {
        OnlineDialog(vm = vm, settings = s, operators = ops, onOpenSettings = onOpenSettings, onDismiss = { showOnline = false })
    }
    if (showBirthdays) {
        BirthdaysDialog(vm = vm, settings = s, onDismiss = { showBirthdays = false })
    }
    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        vm.setDay(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showDatePicker = false
                }) { Text("Vai") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Annulla") } },
        ) { DatePicker(state = state) }
    }
}

/** Intestazione: una riga su schermi larghi, due righe in verticale. Solo icone. */
@Composable
private fun AgendaHeader(
    s: AppSettings, day: LocalDate, mode: ViewMode, isToday: Boolean, nowMin: Int,
    postItCount: Int, birthdayCount: Int, showCancelled: Boolean,
    onMode: (ViewMode) -> Unit, onPrev: () -> Unit, onNext: () -> Unit, onToday: () -> Unit,
    onPickDate: () -> Unit, onClients: () -> Unit, onPostIt: () -> Unit, onReminders: () -> Unit,
    onBirthdays: () -> Unit, onStats: () -> Unit, onToggleCancelled: () -> Unit, onSettings: () -> Unit,
    onShifts: () -> Unit,
    onlineCount: Int,
    onOnline: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val wide = maxWidth >= 1000.dp

            val tools: @Composable () -> Unit = {
                HeaderIcon(Icons.Filled.Email, "Richieste online", badge = onlineCount, onClick = onOnline)
                HeaderIcon(Icons.Filled.Person, "Clienti", onClick = onClients)
                HeaderIcon(Icons.Filled.Edit, "Post-it", badge = postItCount, onClick = onPostIt)
                HeaderIcon(Icons.Filled.Notifications, "Promemoria WhatsApp", onClick = onReminders)
                HeaderIcon(painterResource(R.drawable.ic_cake), "Compleanni", badge = birthdayCount, onClick = onBirthdays)
                HeaderIcon(painterResource(R.drawable.ic_chart), "Statistiche", onClick = onStats)
                MoreMenu(showCancelled, onToggleCancelled, onSettings, onShifts)
            }
            val dateNav: @Composable (Modifier) -> Unit = { m ->
                Row(m, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPrev) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Indietro") }
                    TextButton(onClick = onPickDate, modifier = Modifier.weight(1f, fill = false)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                periodLabel(mode, day),
                                style = SerifTitle.copy(fontSize = if (wide) 22.sp else 19.sp),
                                color = Rose.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            if (mode == ViewMode.GIORNO && isToday) {
                                Text(saluto(nowMin), style = MaterialTheme.typography.bodySmall, color = Rose.Muted)
                            }
                        }
                    }
                    IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Avanti") }
                    TextButton(onClick = onToday, enabled = !isToday) { Text("Oggi") }
                }
            }

            if (wide) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CenterLogo(s.logoVersion, 52.dp)
                    Spacer(Modifier.width(8.dp))
                    ModeSwitch(mode, onMode)
                    dateNav(Modifier.weight(1f))
                    tools()
                }
            } else {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CenterLogo(s.logoVersion, 44.dp)
                        Spacer(Modifier.weight(1f))
                        tools()
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ModeSwitch(mode, onMode)
                        dateNav(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderIcon(icon: ImageVector, desc: String, badge: Int = 0, onClick: () -> Unit) =
    HeaderIcon(rememberVectorPainter(icon), desc, badge, onClick)

@Composable
private fun HeaderIcon(painter: Painter, desc: String, badge: Int = 0, onClick: () -> Unit) {
    Box {
        IconButton(onClick = onClick, modifier = Modifier.size(52.dp)) {
            Icon(painter, desc, tint = Rose.Ink, modifier = Modifier.size(28.dp))
        }
        if (badge > 0) {
            Text(
                if (badge > 9) "9+" else badge.toString(),
                fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 2.dp)
                    .clip(CircleShape)
                    .background(Rose.Aqua)
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }
    }
}

@Composable
private fun MoreMenu(showCancelled: Boolean, onToggleCancelled: () -> Unit, onSettings: () -> Unit, onShifts: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.size(52.dp)) {
            Icon(Icons.Filled.MoreVert, "Altro", tint = Rose.Ink)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(if (showCancelled) "Nascondi disdetti" else "Mostra disdetti") },
                onClick = { onToggleCancelled(); open = false },
            )
            DropdownMenuItem(
                text = { Text("Turni e assenze") },
                leadingIcon = { Icon(Icons.Filled.DateRange, null) },
                onClick = { onShifts(); open = false },
            )
            DropdownMenuItem(
                text = { Text("Impostazioni") },
                leadingIcon = { Icon(Icons.Filled.Settings, null) },
                onClick = { onSettings(); open = false },
            )
        }
    }
}

/** Selettore Giorno / Settimana / Mese. */
@Composable
private fun ModeSwitch(mode: ViewMode, onMode: (ViewMode) -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Rose.HourLine, RoundedCornerShape(12.dp))
    ) {
        ViewMode.entries.forEach { m ->
            val sel = m == mode
            Text(
                m.label,
                fontSize = 14.sp,
                fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                color = if (sel) Color.White else Rose.Ink,
                modifier = Modifier
                    .background(if (sel) Rose.Primary else Color.Transparent)
                    .clickable { onMode(m) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }
    }
}

private fun periodLabel(mode: ViewMode, day: LocalDate): String = when (mode) {
    ViewMode.GIORNO -> dataLunga(day)
    ViewMode.SETTIMANA -> {
        val start = day.with(DayOfWeek.MONDAY)
        val end = start.plusDays(6)
        val f = DateTimeFormatter.ofPattern("d MMM", IT)
        "${start.format(f)} – ${end.format(f)} ${end.year}"
    }
    ViewMode.MESE -> day.format(DateTimeFormatter.ofPattern("MMMM yyyy", IT)).capitalizeIt()
}

@Composable
private fun BirthdayBanner(count: Int, month: YearMonth, onOpen: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Rose.AquaSoft)
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.ic_cake), null, tint = Rose.Aqua, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            if (count == 1) "1 cliente festeggia il compleanno a ${month.format(DateTimeFormatter.ofPattern("MMMM", IT))}: invia gli auguri con lo sconto"
            else "$count clienti festeggiano il compleanno a ${month.format(DateTimeFormatter.ofPattern("MMMM", IT))}: invia gli auguri con lo sconto",
            color = Rose.Ink, modifier = Modifier.weight(1f),
        )
        Text("Apri", fontWeight = FontWeight.Bold, color = Rose.Aqua)
    }
}

/** Vista giorno: intestazioni operatrici + griglia oraria. */
@Composable
private fun DayGrid(
    s: AppSettings, ops: List<Operator>, appts: List<Appointment>, day: LocalDate,
    isToday: Boolean, nowMin: Int, showCancelled: Boolean, avail: Availability,
    onEmptyTap: (Int, Int) -> Unit, onOpen: (Appointment) -> Unit, modifier: Modifier,
) {
    val context = LocalContext.current
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, end = 12.dp)
        ) {
            Spacer(Modifier.width(TIME_COL))
            ops.forEach { op ->
                val c = opColor(op.id)
                val count = appts.count { it.operatorId == op.id && it.statusEnum != Status.DISDETTO }
                Row(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.soft)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(c.strong)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(op.name, fontWeight = FontWeight.SemiBold, color = Rose.Ink, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (avail.workIntervals(op.id, day).isEmpty()) {
                        Text(avail.reasonUnavailable(op.id, day), fontSize = 12.sp, color = Rose.Muted)
                    } else {
                        Text(count.toString(), fontWeight = FontWeight.Bold, color = c.strong)
                    }
                }
            }
        }

        val scroll = rememberScrollState()
        val density = LocalDensity.current
        LaunchedEffect(day, s.openMin) {
            val target = if (isToday) (nowMin - s.openMin - 60).coerceAtLeast(0) else 0
            scroll.scrollTo(with(density) { (target * DP_PER_MIN).dp.roundToPx() })
        }
        val totalMin = (s.closeMin - s.openMin).coerceAtLeast(60)

        Box(Modifier.weight(1f)) {
            Row(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(top = 16.dp, end = 12.dp, bottom = 110.dp)
            ) {
                TimeColumn(s, totalMin)
                ops.forEach { op ->
                    OperatorColumn(
                        op = op,
                        appts = appts.filter {
                            it.operatorId == op.id && (showCancelled || it.statusEnum != Status.DISDETTO)
                        },
                        s = s,
                        totalMin = totalMin,
                        nowMin = if (isToday) nowMin else null,
                        blocked = avail.blockedIntervals(op.id, day, s.openMin, s.closeMin),
                        onEmptyTap = { m -> onEmptyTap(op.id, m) },
                        onBlockedTap = {
                            Toast.makeText(
                                context,
                                "${op.name}: ${avail.reasonUnavailable(op.id, day).lowercase()} in questo orario",
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
                        onOpen = onOpen,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (appts.none { it.statusEnum != Status.DISDETTO }) {
                Column(
                    Modifier
                        .align(Alignment.Center)
                        .alpha(0.85f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    RoseMark(64.dp)
                    Spacer(Modifier.height(8.dp))
                    Text("Nessun appuntamento", style = SerifTitle.copy(fontSize = 20.sp), color = Rose.Muted)
                    Text("Tocca un orario libero per prenotare", style = MaterialTheme.typography.bodySmall, color = Rose.Muted)
                }
            }
        }
    }
}

private fun saluto(nowMin: Int) = when {
    nowMin < 13 * 60 -> "Buongiorno!"
    nowMin < 18 * 60 -> "Buon pomeriggio!"
    else -> "Buonasera!"
}

private fun roundUp(min: Int, step: Int) = ((min + step - 1) / step) * step

@Composable
private fun TimeColumn(s: AppSettings, totalMin: Int) {
    Box(
        Modifier
            .width(TIME_COL)
            .height((totalMin * DP_PER_MIN).dp)
    ) {
        var m = ((s.openMin + 29) / 30) * 30
        while (m < s.closeMin) {
            val hour = m % 60 == 0
            Text(
                hhmm(m),
                fontSize = if (hour) 20.sp else 15.sp,
                fontWeight = if (hour) FontWeight.Bold else FontWeight.Normal,
                color = if (hour) Rose.Ink else Rose.Muted,
                modifier = Modifier
                    .offset(y = ((m - s.openMin) * DP_PER_MIN).dp - (if (hour) 13 else 10).dp)
                    .padding(start = 10.dp),
            )
            m += 30
        }
    }
}

@Composable
private fun OperatorColumn(
    op: Operator,
    appts: List<Appointment>,
    s: AppSettings,
    totalMin: Int,
    nowMin: Int?,
    blocked: List<Pair<Int, Int>>,
    onEmptyTap: (Int) -> Unit,
    onBlockedTap: () -> Unit,
    onOpen: (Appointment) -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = opColor(op.id)
    val blockedTap by rememberUpdatedState(onBlockedTap)
    val blockedNow by rememberUpdatedState(blocked)
    val tap by rememberUpdatedState(onEmptyTap)
    val (lanes, laneCount) = remember(appts) { layoutLanes(appts) }

    BoxWithConstraints(
        modifier
            .height((totalMin * DP_PER_MIN).dp)
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Rose.Surface)
            .border(1.dp, Rose.Line, RoundedCornerShape(8.dp))
            .drawBehind {
                val pxPerMin = DP_PER_MIN.dp.toPx()
                // Fasce fuori turno: fondo grigio con tratteggio diagonale.
                blocked.forEach { (b0, b1) ->
                    val top = (b0 - s.openMin) * pxPerMin
                    val h = (b1 - b0) * pxPerMin
                    drawRect(Rose.Blocked, topLeft = Offset(0f, top), size = Size(size.width, h))
                    val step = 14.dp.toPx()
                    var x = -h
                    while (x < size.width) {
                        val x0 = maxOf(x, 0f)
                        val y0 = top + (x0 - x)
                        val x1 = minOf(x + h, size.width)
                        val y1 = top + (x1 - x)
                        if (x1 > x0) drawLine(Rose.BlockedLine, Offset(x0, y0), Offset(x1, y1), strokeWidth = 1.dp.toPx())
                        x += step
                    }
                }
                var m = s.openMin
                while (m <= s.closeMin) {
                    val y = (m - s.openMin) * pxPerMin
                    val hour = m % 60 == 0
                    drawLine(
                        color = if (hour) Rose.HourLine else Rose.Line,
                        start = Offset(0f, y), end = Offset(size.width, y),
                        strokeWidth = if (hour) 1.5f else 1f,
                    )
                    m += s.slotMin
                }
                if (nowMin != null && nowMin in s.openMin..s.closeMin) {
                    val y = (nowMin - s.openMin) * pxPerMin
                    drawLine(Rose.Now, Offset(0f, y), Offset(size.width, y), strokeWidth = 2.dp.toPx())
                    drawCircle(Rose.Now, radius = 4.dp.toPx(), center = Offset(4.dp.toPx(), y))
                }
            }
            .pointerInput(s.openMin, s.closeMin, s.slotMin, op.id) {
                detectTapGestures { off ->
                    val m = (off.y / DP_PER_MIN.dp.toPx()).toInt()
                    val start = s.openMin + (m / s.slotMin) * s.slotMin
                    val end = start + s.slotMin
                    if (start >= s.closeMin) return@detectTapGestures
                    if (blockedNow.any { (b0, b1) -> start < b1 && end > b0 }) blockedTap() else tap(start)
                }
            }
    ) {
        val laneW = maxWidth / laneCount
        appts.forEach { a ->
            val top = maxOf(a.startMin, s.openMin)
            val bottom = minOf(a.endMin, s.closeMin)
            if (bottom <= top) return@forEach
            val lane = lanes[a.id] ?: 0
            AppointmentBlock(
                a = a,
                accent = color.strong,
                fill = color.soft,
                modifier = Modifier
                    .offset(x = laneW * lane, y = ((top - s.openMin) * DP_PER_MIN).dp)
                    .width(laneW)
                    .height(((bottom - top) * DP_PER_MIN).dp)
                    .padding(horizontal = 3.dp, vertical = 1.5.dp)
                    .clickable { onOpen(a) },
            )
        }
    }
}

@Composable
private fun AppointmentBlock(a: Appointment, accent: androidx.compose.ui.graphics.Color, fill: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    val st = a.statusEnum
    val (chipBg, chipFg) = statusColors(st)
    val faded = st == Status.DISDETTO || st == Status.COMPLETATO
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier
            .clip(shape)
            .background(if (st == Status.DA_CONFERMARE) chipBg else fill)
            .border(1.dp, accent.copy(alpha = 0.35f), shape)
            .alpha(if (faded) 0.6f else 1f)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${hhmm(a.startMin)}  ${a.clientName}",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Rose.Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (st == Status.DISDETTO) TextDecoration.LineThrough else null,
                modifier = Modifier.weight(1f),
            )
            if (a.reminderSent) {
                Text("✓", color = accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        if (a.durationMin >= 30) {
            Text(
                a.service.ifBlank { "—" } + "  ·  " + hhmm(a.startMin) + "–" + hhmm(a.endMin),
                fontSize = 12.sp, color = Rose.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        if (a.durationMin >= 45 && st != Status.CONFERMATO) {
            Spacer(Modifier.height(4.dp))
            Text(
                st.label,
                fontSize = 11.sp, fontWeight = FontWeight.Bold, color = chipFg,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(chipBg)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        if (a.durationMin >= 60 && a.notes.isNotBlank()) {
            HorizontalDivider(Modifier.padding(vertical = 4.dp), color = accent.copy(alpha = 0.2f))
            Text(a.notes, fontSize = 12.sp, color = Rose.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Sovrapposizioni nella stessa colonna: assegna corsie affiancate. */
private fun layoutLanes(list: List<Appointment>): Pair<Map<Long, Int>, Int> {
    val laneEnds = mutableListOf<Int>()
    val res = mutableMapOf<Long, Int>()
    for (a in list.sortedBy { it.startMin }) {
        var lane = laneEnds.indexOfFirst { it <= a.startMin }
        if (lane < 0) {
            laneEnds.add(a.endMin)
            lane = laneEnds.lastIndex
        } else {
            laneEnds[lane] = a.endMin
        }
        res[a.id] = lane
    }
    return res to maxOf(1, laneEnds.size)
}
