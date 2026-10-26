package it.agenda.estetista.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.agenda.estetista.AgendaViewModel
import it.agenda.estetista.data.Appointment
import it.agenda.estetista.data.Operator
import it.agenda.estetista.data.Status
import it.agenda.estetista.data.statusEnum
import it.agenda.estetista.util.IT
import it.agenda.estetista.util.durata
import it.agenda.estetista.util.hhmm
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle

/** Vista settimana: 7 colonne lunedì-domenica, appuntamenti in elenco per ora. */
@Composable
fun WeekView(
    vm: AgendaViewModel,
    day: LocalDate,
    operators: List<Operator>,
    showCancelled: Boolean,
    onOpenDay: (LocalDate) -> Unit,
    onOpen: (Appointment) -> Unit,
    modifier: Modifier = Modifier,
) {
    val start = day.with(DayOfWeek.MONDAY)
    val flow = remember(start) { vm.appointmentsBetween(start, start.plusDays(6)) }
    val list by flow.collectAsState(initial = emptyList())
    val today = LocalDate.now()
    val names = operators.associate { it.id to it.name }

    val visible = list.filter { showCancelled || it.statusEnum != Status.DISDETTO }
    val active = list.filter { it.statusEnum != Status.DISDETTO }

    Column(modifier.padding(horizontal = 12.dp)) {
        // Riepilogo settimana
        Row(
            Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Summary("Appuntamenti", active.size.toString())
            Summary("Ore prenotate", durata(active.sumOf { it.durationMin }))
            Summary("Clienti", active.map { it.clientName.lowercase() }.distinct().size.toString())
        }
        Row(Modifier.fillMaxSize()) {
            (0..6).forEach { i ->
                val d = start.plusDays(i.toLong())
                val dayList = visible.filter { it.date == d.toEpochDay() }
                val isToday = d == today
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 3.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isToday) Rose.PrimarySoft else Rose.Surface)
                        .border(1.dp, if (isToday) Rose.Blush else Rose.Line, RoundedCornerShape(10.dp)),
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpenDay(d) }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            d.dayOfWeek.getDisplayName(TextStyle.SHORT, IT).uppercase(),
                            fontSize = 12.sp, color = Rose.Muted, fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            d.dayOfMonth.toString(),
                            style = SerifTitle.copy(fontSize = 26.sp),
                            color = if (isToday) Rose.Aqua else Rose.Ink,
                        )
                        val n = dayList.count { it.statusEnum != Status.DISDETTO }
                        Text(if (n == 0) "libero" else if (n == 1) "1 app." else "$n app.", fontSize = 12.sp, color = Rose.Muted)
                    }
                    Column(
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        dayList.forEach { a -> WeekItem(a, names[a.operatorId].orEmpty()) { onOpen(a) } }
                        Spacer(Modifier.height(90.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekItem(a: Appointment, operator: String, onClick: () -> Unit) {
    val c = opColor(a.operatorId)
    val st = a.statusEnum
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (st == Status.DA_CONFERMARE) statusColors(st).first else c.soft)
            .alpha(if (st == Status.DISDETTO || st == Status.COMPLETATO) 0.55f else 1f)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(c.strong)
            )
            Spacer(Modifier.width(6.dp))
            Text(hhmm(a.startMin), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Rose.Ink)
        }
        Text(
            a.clientName, fontSize = 13.sp, color = Rose.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
            textDecoration = if (st == Status.DISDETTO) TextDecoration.LineThrough else null,
        )
        if (a.service.isNotBlank()) {
            Text(a.service, fontSize = 11.sp, color = Rose.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Vista mese: calendario 7 colonne, per ogni giorno numero appuntamenti e pallini per operatrice. */
@Composable
fun MonthView(
    vm: AgendaViewModel,
    day: LocalDate,
    operators: List<Operator>,
    showCancelled: Boolean,
    onOpenDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ym = YearMonth.from(day)
    val first = ym.atDay(1)
    val gridStart = first.with(DayOfWeek.MONDAY)
    val gridEnd = ym.atEndOfMonth().with(DayOfWeek.SUNDAY)
    val flow = remember(ym) { vm.appointmentsBetween(gridStart, gridEnd) }
    val list by flow.collectAsState(initial = emptyList())
    val today = LocalDate.now()

    val active = list.filter { it.statusEnum != Status.DISDETTO }
    val byDay = list.filter { showCancelled || it.statusEnum != Status.DISDETTO }.groupBy { it.date }
    val inMonth = active.filter { LocalDate.ofEpochDay(it.date).month == ym.month }
    val weeks = ((gridEnd.toEpochDay() - gridStart.toEpochDay() + 1) / 7).toInt()

    Column(modifier.padding(horizontal = 12.dp)) {
        Row(
            Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Summary("Appuntamenti", inMonth.size.toString())
            Summary("Ore prenotate", durata(inMonth.sumOf { it.durationMin }))
            Summary("Clienti", inMonth.map { it.clientName.lowercase() }.distinct().size.toString())
            val busiest = inMonth.groupBy { it.date }.maxByOrNull { it.value.size }
            if (busiest != null) {
                Summary("Giorno più pieno", LocalDate.ofEpochDay(busiest.key).dayOfMonth.toString() + " (" + busiest.value.size + ")")
            }
        }
        Row(Modifier.fillMaxWidth()) {
            (0..6).forEach { i ->
                Text(
                    DayOfWeek.of(i + 1).getDisplayName(TextStyle.SHORT, IT).uppercase(),
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Rose.Muted,
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 6.dp),
                )
            }
        }
        BoxWithConstraints(Modifier.weight(1f).padding(bottom = 96.dp)) {
            val cellH = (maxHeight / weeks).coerceAtLeast(72.dp)
            Column(Modifier.verticalScroll(rememberScrollState())) {
                (0 until weeks).forEach { w ->
                    Row(Modifier.fillMaxWidth()) {
                        (0..6).forEach { i ->
                            val d = gridStart.plusDays((w * 7 + i).toLong())
                            val items = byDay[d.toEpochDay()].orEmpty()
                            val n = items.count { it.statusEnum != Status.DISDETTO }
                            val inM = d.month == ym.month
                            val isToday = d == today
                            Column(
                                Modifier
                                    .weight(1f)
                                    .height(cellH)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isToday -> Rose.PrimarySoft
                                            inM -> Rose.Surface
                                            else -> Rose.Ground
                                        }
                                    )
                                    .border(1.dp, if (isToday) Rose.Blush else Rose.Line, RoundedCornerShape(8.dp))
                                    .clickable { onOpenDay(d) }
                                    .padding(8.dp)
                                    .alpha(if (inM) 1f else 0.45f),
                            ) {
                                Text(
                                    d.dayOfMonth.toString(),
                                    fontSize = 16.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isToday) Rose.Aqua else Rose.Ink,
                                )
                                Spacer(Modifier.weight(1f))
                                if (n > 0) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        operators.forEach { op ->
                                            val k = items.count { it.operatorId == op.id && it.statusEnum != Status.DISDETTO }
                                            if (k > 0) {
                                                Box(
                                                    Modifier
                                                        .padding(end = 3.dp)
                                                        .size(9.dp)
                                                        .clip(CircleShape)
                                                        .background(opColor(op.id).strong)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.weight(1f))
                                        Text(
                                            n.toString(),
                                            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Rose.Ink,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Summary(label: String, value: String) {
    Column {
        Text(value, style = SerifTitle.copy(fontSize = 24.sp), color = Rose.Ink)
        Text(label, fontSize = 12.sp, color = Rose.Muted)
    }
}
