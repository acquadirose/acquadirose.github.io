@file:OptIn(ExperimentalLayoutApi::class)

package it.agenda.estetista.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.agenda.estetista.AgendaViewModel
import it.agenda.estetista.data.Appointment
import androidx.compose.material3.TextButton
import it.agenda.estetista.data.age
import it.agenda.estetista.data.Client
import it.agenda.estetista.data.AgeBand
import it.agenda.estetista.data.Status
import it.agenda.estetista.data.statusEnum
import it.agenda.estetista.util.IT
import it.agenda.estetista.util.capitalizeIt
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import kotlin.math.roundToInt

private enum class Period(val label: String) { SETTIMANA("Settimana"), MESE("Mese"), ANNO("Anno") }

/** Dashboard: numeri chiave e grafici sugli appuntamenti del periodo scelto. */
@Composable
fun StatsScreen(vm: AgendaViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val operators by vm.operators.collectAsStateWithLifecycle()
    val clients by vm.clients.collectAsStateWithLifecycle()
    val s by vm.settings.collectAsStateWithLifecycle()

    var period by remember { mutableStateOf(Period.MESE) }
    var anchor by remember { mutableStateOf(LocalDate.now()) }

    val (from, to) = when (period) {
        Period.SETTIMANA -> anchor.with(DayOfWeek.MONDAY).let { it to it.plusDays(6) }
        Period.MESE -> YearMonth.from(anchor).let { it.atDay(1) to it.atEndOfMonth() }
        Period.ANNO -> LocalDate.of(anchor.year, 1, 1) to LocalDate.of(anchor.year, 12, 31)
    }
    val title = when (period) {
        Period.SETTIMANA -> {
            val f = DateTimeFormatter.ofPattern("d MMM", IT)
            "${from.format(f)} – ${to.format(f)} ${to.year}"
        }
        Period.MESE -> anchor.format(DateTimeFormatter.ofPattern("MMMM yyyy", IT)).capitalizeIt()
        Period.ANNO -> anchor.year.toString()
    }
    fun move(d: Long) {
        anchor = when (period) {
            Period.SETTIMANA -> anchor.plusWeeks(d)
            Period.MESE -> anchor.plusMonths(d)
            Period.ANNO -> anchor.plusYears(d)
        }
    }

    val flow = remember(from, to) { vm.appointmentsBetween(from, to) }
    val raw by flow.collectAsState(initial = emptyList())

    // ── Filtri anagrafici ──
    var ageFilter by remember { mutableStateOf<AgeBand?>(null) }
    var cityFilter by remember { mutableStateOf<String?>(null) }
    val byId = remember(clients) { clients.associateBy { it.id } }
    val byName = remember(clients) { clients.associateBy { it.name.trim().lowercase() } }
    fun clientOf(a: Appointment): Client? = a.clientId?.let { byId[it] } ?: byName[a.clientName.trim().lowercase()]
    fun matches(c: Client?): Boolean =
        (ageFilter == null || AgeBand.of(c?.age()) == ageFilter) &&
            (cityFilter == null || c?.city.equals(cityFilter, ignoreCase = true))
    val filtering = ageFilter != null || cityFilter != null
    val all = if (filtering) raw.filter { matches(clientOf(it)) } else raw
    val done = all.filter { it.statusEnum != Status.DISDETTO }
    val cancelled = all.size - done.size
    val cities = remember(clients) {
        clients.map { it.city }.filter { it.isNotBlank() }.groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }.map { it.key }
    }
    // clienti (con scheda) che hanno avuto appuntamenti nel periodo
    val periodClients = done.mapNotNull { clientOf(it) }.distinctBy { it.id }

    val zone = ZoneId.systemDefault()
    val newClients = clients.count {
        val d = Instant.ofEpochMilli(it.createdAt).atZone(zone).toLocalDate()
        !d.isBefore(from) && !d.isAfter(to) && matches(it)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp) {
            FlowRow(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro") }
                Text(
                    "Statistiche", style = SerifTitle.copy(fontSize = 26.sp),
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(end = 16.dp),
                )
                Period.entries.forEach { p ->
                    FilterChip(
                        selected = period == p, onClick = { period = p }, label = { Text(p.label) },
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { move(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Periodo precedente") }
                    Text(title, style = SerifTitle.copy(fontSize = 20.sp), color = Rose.Ink)
                    IconButton(onClick = { move(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Periodo successivo") }
                }
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Filtri ──
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Età", color = Rose.Muted)
                    Choice(ageFilter, listOf<AgeBand?>(null) + AgeBand.entries, { it?.label ?: "Tutte" }, { ageFilter = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Comune", color = Rose.Muted)
                    Choice(cityFilter, listOf<String?>(null) + cities, { it ?: "Tutti" }, { cityFilter = it })
                }
                if (filtering) {
                    TextButton(onClick = { ageFilter = null; cityFilter = null }) { Text("Azzera filtri") }
                }
            }
            if (filtering) {
                val excluded = raw.count { clientOf(it).let { c -> c == null || (ageFilter != null && c.age() == null) || (cityFilter != null && c.city.isBlank()) } }
                if (excluded > 0) {
                    Text(
                        "$excluded appuntamenti esclusi perché la cliente non ha ${if (ageFilter != null) "anno di nascita" else "comune"} in scheda.",
                        fontSize = 13.sp, color = Rose.Muted,
                    )
                }
            }

            // ── Numeri chiave ──
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Kpi("Appuntamenti", done.size.toString())
                Kpi("Ore di lavoro", formatHours(done.sumOf { it.durationMin }))
                Kpi("Clienti diverse", done.map { it.clientName.trim().lowercase() }.distinct().size.toString())
                Kpi("Nuove clienti", newClients.toString())
                Kpi(
                    "Disdette",
                    if (all.isEmpty()) "0%" else "${(cancelled * 100f / all.size).roundToInt()}%",
                    sub = "$cancelled su ${all.size}",
                )
                val ages = periodClients.mapNotNull { it.age() }
                Kpi("Età media", if (ages.isEmpty()) "—" else "${ages.average().roundToInt()} anni", sub = if (ages.isEmpty()) null else "su ${ages.size} clienti")
                Kpi(
                    "Durata media",
                    if (done.isEmpty()) "—" else "${done.sumOf { it.durationMin } / done.size} min",
                )
            }

            if (all.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        RoseMark(64.dp)
                        Text("Nessun appuntamento in questo periodo", style = SerifTitle.copy(fontSize = 20.sp), color = Rose.Muted)
                    }
                }
                return@Column
            }

            // ── Andamento nel periodo ──
            ChartCard("Appuntamenti nel periodo") {
                val (labels, values) = when (period) {
                    Period.SETTIMANA -> (0..6).map { from.plusDays(it.toLong()) }.let { days ->
                        days.map { it.dayOfWeek.getDisplayName(TextStyle.SHORT, IT) } to
                            days.map { d -> done.count { it.date == d.toEpochDay() } }
                    }
                    Period.MESE -> (1..to.dayOfMonth).let { ds ->
                        ds.map { if (it == 1 || it % 5 == 0) it.toString() else "" } to
                            ds.map { d -> done.count { LocalDate.ofEpochDay(it.date).dayOfMonth == d } }
                    }
                    Period.ANNO -> (1..12).let { ms ->
                        ms.map { MESI[it - 1].take(3) } to
                            ms.map { m -> done.count { LocalDate.ofEpochDay(it.date).monthValue == m } }
                    }
                }
                BarChart(values, labels, Rose.Primary)
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                maxItemsInEachRow = 2,
            ) {
                // ── Per operatrice ──
                ChartCard("Per operatrice", Modifier.weight(1f).widthIn(min = 320.dp)) {
                    val rows = operators.take(maxOf(s.columns, operators.count { op -> done.any { it.operatorId == op.id } }))
                        .map { op ->
                            val list = done.filter { it.operatorId == op.id }
                            Triple(op.name, list.size, opColor(op.id).strong) to formatHours(list.sumOf { it.durationMin })
                        }
                    HBars(rows.map { it.first }, extra = rows.map { it.second })
                }

                // ── Trattamenti ──
                ChartCard("Trattamenti più richiesti", Modifier.weight(1f).widthIn(min = 320.dp)) {
                    val top = done.groupBy { it.service.trim().ifBlank { "Non indicato" } }
                        .map { (k, v) -> Triple(k, v.size, Rose.Aqua) }
                        .sortedByDescending { it.second }
                        .take(8)
                    HBars(top)
                }

                // ── Giorni della settimana ──
                ChartCard("Giorni più richiesti", Modifier.weight(1f).widthIn(min = 320.dp)) {
                    val counts = (1..7).map { dw -> done.count { LocalDate.ofEpochDay(it.date).dayOfWeek.value == dw } }
                    BarChart(counts, (1..7).map { DayOfWeek.of(it).getDisplayName(TextStyle.SHORT, IT) }, Rose.Aqua, height = 160.dp)
                }

                // ── Fasce orarie ──
                ChartCard("Fasce orarie", Modifier.weight(1f).widthIn(min = 320.dp)) {
                    val hours = (s.openMin / 60 until (s.closeMin + 59) / 60).toList()
                    val counts = hours.map { h -> done.count { it.startMin / 60 == h } }
                    BarChart(counts, hours.map { it.toString() }, Rose.Primary, height = 160.dp)
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                maxItemsInEachRow = 2,
            ) {
                // ── Fasce d'età ──
                ChartCard("Clienti per fascia d'età", Modifier.weight(1f).widthIn(min = 320.dp)) {
                    val counts = AgeBand.entries.map { b -> b.label to periodClients.count { AgeBand.of(it.age()) == b } }
                    val unknown = periodClients.count { it.age() == null }
                    BarChart(counts.map { it.second }, counts.map { it.first }, Rose.Primary, height = 160.dp)
                    if (unknown > 0) Text("$unknown clienti senza anno di nascita", fontSize = 12.sp, color = Rose.Muted)
                }
                // ── Comuni ──
                ChartCard("Clienti per comune", Modifier.weight(1f).widthIn(min = 320.dp)) {
                    val top = periodClients.groupBy { it.city.ifBlank { "Non indicato" } }
                        .map { (k, v) -> Triple(k, v.size, if (k == "Non indicato") Rose.HourLine else Rose.Aqua) }
                        .sortedWith(compareBy<Triple<String, Int, Color>> { it.first == "Non indicato" }.thenByDescending { it.second })
                        .take(8)
                    HBars(top)
                }
            }

            // ── Stato degli appuntamenti ──
            ChartCard("Stato degli appuntamenti") {
                StatusBar(all)
            }

            // ── Clienti più fedeli ──
            ChartCard("Clienti più assidue") {
                val top = done.groupBy { it.clientName.trim() }
                    .map { (k, v) -> Triple(k, v.size, Rose.Primary) }
                    .sortedByDescending { it.second }
                    .take(8)
                HBars(top)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun formatHours(min: Int): String {
    val h = min / 60
    val m = min % 60
    return if (m == 0) "$h h" else "$h h ${m}m"
}

@Composable
private fun Kpi(label: String, value: String, sub: String? = null) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Rose.Line),
        modifier = Modifier.widthIn(min = 170.dp),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Text(label, fontSize = 13.sp, color = Rose.Muted)
            Text(value, style = SerifTitle.copy(fontSize = 30.sp), color = Rose.Ink)
            if (sub != null) Text(sub, fontSize = 12.sp, color = Rose.Muted)
        }
    }
}

@Composable
private fun ChartCard(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, Rose.Line),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = SerifTitle.copy(fontSize = 19.sp), color = Rose.Ink)
            content()
        }
    }
}

/** Istogramma verticale: valore sopra ogni barra, etichetta sotto. */
@Composable
private fun BarChart(values: List<Int>, labels: List<String>, color: Color, height: Dp = 200.dp) {
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    Row(
        Modifier
            .fillMaxWidth()
            .height(height + 40.dp),
        horizontalArrangement = Arrangement.spacedBy(if (values.size > 15) 2.dp else 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEachIndexed { i, v ->
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                if (v > 0 && values.size <= 15) {
                    Text(v.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Rose.Ink)
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(if (v == 0) 2.dp else height * (v.toFloat() / max))
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(if (v == 0) Rose.Line else color)
                )
                Text(
                    labels.getOrElse(i) { "" },
                    fontSize = 11.sp, color = Rose.Muted, maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** Barre orizzontali con nome, barra e numero. */
@Composable
private fun HBars(rows: List<Triple<String, Int, Color>>, extra: List<String>? = null) {
    if (rows.isEmpty()) {
        Text("Nessun dato", color = Rose.Muted)
        return
    }
    val max = (rows.maxOf { it.second }).coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEachIndexed { i, (name, n, c) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    name, fontSize = 14.sp, color = Rose.Ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(150.dp),
                )
                Box(Modifier.weight(1f).height(22.dp)) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(n.toFloat() / max)
                            .clip(RoundedCornerShape(4.dp))
                            .background(c)
                    )
                }
                Text(
                    n.toString() + (extra?.getOrNull(i)?.let { "  ·  $it" } ?: ""),
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Rose.Ink,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(if (extra != null) 110.dp else 44.dp),
                )
            }
        }
    }
}

/** Barra unica divisa per stato, con legenda. */
@Composable
private fun StatusBar(all: List<Appointment>) {
    val counts = Status.entries.map { st -> st to all.count { it.statusEnum == st } }.filter { it.second > 0 }
    val total = all.size.coerceAtLeast(1)
    Row(
        Modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(RoundedCornerShape(6.dp))
    ) {
        counts.forEach { (st, n) ->
            Box(
                Modifier
                    .weight(n.toFloat())
                    .fillMaxHeight()
                    .background(statusColors(st).second)
            )
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        counts.forEach { (st, n) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(statusColors(st).second)
                )
                Spacer(Modifier.width(6.dp))
                Text("${st.label}: $n (${(n * 100f / total).roundToInt()}%)", fontSize = 14.sp)
            }
        }
    }
}
