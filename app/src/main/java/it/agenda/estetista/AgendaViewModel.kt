@file:OptIn(kotlinx.coroutines.FlowPreview::class)

package it.agenda.estetista

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.agenda.estetista.data.endMin
import it.agenda.estetista.data.AppDatabase
import it.agenda.estetista.data.AppSettings
import it.agenda.estetista.data.Appointment
import it.agenda.estetista.data.Backup
import it.agenda.estetista.data.Client
import it.agenda.estetista.data.birthdayDayMonth
import it.agenda.estetista.data.PostIt
import it.agenda.estetista.data.Shift
import it.agenda.estetista.data.normalizeCity
import it.agenda.estetista.data.Online
import it.agenda.estetista.data.OnlineRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.collectLatest
import it.agenda.estetista.util.dataMedia
import it.agenda.estetista.util.hhmm
import it.agenda.estetista.data.Absence
import it.agenda.estetista.data.Availability
import androidx.room.withTransaction
import kotlinx.coroutines.flow.combine
import it.agenda.estetista.data.Operator
import it.agenda.estetista.data.Prefs
import it.agenda.estetista.data.Service
import it.agenda.estetista.util.Logo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

enum class ViewMode(val label: String) { GIORNO("Giorno"), SETTIMANA("Settimana"), MESE("Mese") }

/** Cliente con compleanno nel mese considerato. */
data class BirthdayEntry(val client: Client, val day: Int, val month: YearMonth, val sent: Boolean)

class AgendaViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val dao = db.dao()
    private val prefs = Prefs(app)

    val settings: StateFlow<AppSettings> = prefs.settings

    private val _day = MutableStateFlow(LocalDate.now())
    val day: StateFlow<LocalDate> = _day

    val operators: StateFlow<List<Operator>> =
        dao.operators().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val services: StateFlow<List<Service>> =
        dao.services().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val appointments: StateFlow<List<Appointment>> =
        _day.flatMapLatest { dao.appointmentsOn(it.toEpochDay()) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val clients: StateFlow<List<Client>> =
        dao.clients().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Numero di visite (appuntamenti non disdetti) per cliente. */
    val visitCounts: StateFlow<Map<Long, Int>> =
        dao.visitCounts().map { l -> l.associate { it.id to it.n } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    val postIts: StateFlow<List<PostIt>> =
        _day.flatMapLatest { dao.postItsFor(it.toEpochDay()) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val shifts: StateFlow<List<Shift>> =
        dao.shifts().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val absences: StateFlow<List<Absence>> =
        dao.absences().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val availability: StateFlow<Availability> =
        combine(dao.shifts(), dao.absences()) { s, a -> Availability(s, a) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, Availability(emptyList(), emptyList()))

    /** Sostituisce i turni di un'operatrice nei giorni indicati con le fasce date (vuote = riposo). */
    fun saveShifts(op: Int, days: List<Int>, ranges: List<Pair<Int, Int>>) = viewModelScope.launch {
        db.withTransaction {
            days.forEach { dow ->
                dao.deleteShifts(op, dow)
                dao.insertShifts(ranges.map { (s, e) -> Shift(operatorId = op, dayOfWeek = dow, startMin = s, endMin = e) })
            }
        }
    }

    /** Toglie tutti i turni: l'operatrice torna "sempre disponibile". */
    fun clearShifts(op: Int) = viewModelScope.launch { dao.deleteAllShifts(op) }

    fun saveAbsence(a: Absence) = viewModelScope.launch { dao.upsertAbsence(a) }
    fun deleteAbsence(a: Absence) = viewModelScope.launch { dao.deleteAbsence(a) }

    init {
        viewModelScope.launch {
            if (dao.operatorCount() == 0) {
                dao.upsertOperators((1..4).map { Operator(it, "Operatrice $it") })
            }
            if (dao.serviceCount() == 0) {
                dao.insertServices(
                    listOf(
                        Service(name = "Pulizia viso", durationMin = 60),
                        Service(name = "Manicure", durationMin = 45),
                        Service(name = "Semipermanente mani", durationMin = 60),
                        Service(name = "Pedicure", durationMin = 60),
                        Service(name = "Ceretta gambe", durationMin = 45),
                        Service(name = "Sopracciglia", durationMin = 15),
                        Service(name = "Massaggio", durationMin = 60),
                    )
                )
            }
        }
    }

    fun appointmentsFor(d: LocalDate): Flow<List<Appointment>> = dao.appointmentsOn(d.toEpochDay())

    // Blocco con password: sbloccato finché l'app resta aperta o va in secondo piano per meno di 5 minuti.
    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked
    private var backgroundAt = 0L
    fun unlock() { _unlocked.value = true }
    fun lock() { _unlocked.value = false }
    fun onAppStopped() { backgroundAt = System.currentTimeMillis() }
    fun onAppStarted() {
        if (backgroundAt > 0 && System.currentTimeMillis() - backgroundAt > 5 * 60_000) _unlocked.value = false
        backgroundAt = 0
    }

    // Navigazione
    private val _mode = MutableStateFlow(ViewMode.GIORNO)
    val mode: StateFlow<ViewMode> = _mode
    fun setMode(m: ViewMode) { _mode.value = m }

    fun setDay(d: LocalDate) { _day.value = d }
    fun shiftDay(delta: Long) { _day.value = _day.value.plusDays(delta) }

    /** Avanti/indietro di un giorno, una settimana o un mese secondo la vista. */
    fun shift(delta: Long) {
        _day.value = when (_mode.value) {
            ViewMode.GIORNO -> _day.value.plusDays(delta)
            ViewMode.SETTIMANA -> _day.value.plusWeeks(delta)
            ViewMode.MESE -> _day.value.plusMonths(delta)
        }
    }

    fun appointmentsBetween(from: LocalDate, to: LocalDate): Flow<List<Appointment>> =
        dao.appointmentsBetween(from.toEpochDay(), to.toEpochDay())

    // Compleanni
    fun birthdaysIn(month: YearMonth, list: List<Client>): List<BirthdayEntry> =
        list.mapNotNull { c ->
            val (d, m) = c.birthdayDayMonth() ?: return@mapNotNull null
            if (m != month.monthValue) null
            else BirthdayEntry(c, d, month, c.birthdayMsgYear >= month.year)
        }.sortedBy { it.day }

    fun markBirthdaySent(c: Client, year: Int) = viewModelScope.launch { dao.setBirthdaySent(c.id, year) }

    // Appuntamenti
    /**
     * Salva l'appuntamento e tiene allineata l'anagrafica:
     * cliente nuova → creata; cliente esistente con nuovo numero → numero aggiornato.
     */
    fun save(a: Appointment) = viewModelScope.launch {
        val name = a.clientName.trim()
        var clientId = a.clientId
        if (name.isNotEmpty()) {
            val linked = clientId?.let { dao.clientById(it) }
            val c = linked ?: dao.clientByName(name)
            if (c == null) {
                clientId = dao.insertClient(Client(name = name, phone = a.phone.trim()))
            } else {
                clientId = c.id
                if (a.phone.isNotBlank() && a.phone.trim() != c.phone) dao.upsertClient(c.copy(phone = a.phone.trim()))
            }
        }
        dao.upsertAppointment(a.copy(clientName = name, clientId = clientId))
    }
    fun delete(a: Appointment) = viewModelScope.launch { dao.deleteAppointment(a) }

    suspend fun conflicts(a: Appointment): List<Appointment> =
        dao.sameColumn(a.date, a.operatorId, a.id)
            .filter { it.startMin < a.endMin && a.startMin < it.endMin }

    suspend fun searchClients(q: String): List<Client> = dao.searchClients(q.trim())

    // Anagrafica
    fun appointmentsOfClient(id: Long): Flow<List<Appointment>> = dao.appointmentsOfClient(id)

    /** Salva la scheda; restituisce l'id. Nome/telefono si propagano agli appuntamenti futuri. */
    suspend fun saveClient(c: Client): Long {
        val clean = c.copy(name = c.name.trim(), phone = c.phone.trim(), city = normalizeCity(c.city))
        return if (clean.id == 0L) {
            dao.insertClient(clean)
        } else {
            dao.upsertClient(clean)
            dao.syncClientInFuture(clean.id, clean.name, clean.phone, LocalDate.now().toEpochDay())
            clean.id
        }
    }

    fun deleteClient(c: Client) = viewModelScope.launch {
        dao.unlinkClient(c.id)
        dao.deleteClient(c)
    }

    // Post-it
    fun savePostIt(p: PostIt) = viewModelScope.launch { dao.upsertPostIt(p.copy(text = p.text.trim())) }
    fun deletePostIt(p: PostIt) = viewModelScope.launch { dao.deletePostIt(p) }

    // Impostazioni
    fun updateSettings(s: AppSettings) = prefs.update(s)

    fun renameOperators(names: Map<Int, String>) = viewModelScope.launch {
        dao.upsertOperators(names.map { (id, n) -> Operator(id, n.ifBlank { "Operatrice $id" }) })
    }

    fun saveService(s: Service) = viewModelScope.launch { dao.upsertService(s) }
    fun deleteService(s: Service) = viewModelScope.launch { dao.deleteService(s) }

    // Logo
    suspend fun setLogo(uri: Uri?) {
        withContext(Dispatchers.IO) {
            if (uri == null) Logo.remove(getApplication()) else Logo.save(getApplication(), uri)
        }
        val s = settings.value
        prefs.update(s.copy(logoVersion = s.logoVersion + 1))
    }

    // Backup
    suspend fun exportTo(uri: Uri) = Backup.export(getApplication(), uri, db, prefs)
    suspend fun importFrom(uri: Uri): Int = Backup.import(getApplication(), uri, db, prefs)

    // ── Prenotazioni online ──

    data class OnlineState(
        val connected: Boolean = false,
        val email: String? = null,
        val requests: List<OnlineRequest> = emptyList(),
        val error: String? = null,
        val loading: Boolean = false,
        val lastSync: Long = 0,
    )

    private val online = Online(app)
    private val _online = MutableStateFlow(OnlineState(online.isConnected, online.email))
    val onlineState: StateFlow<OnlineState> = _online

    suspend fun onlineConnect(email: String, password: String, create: Boolean) {
        if (create) online.signUp(email, password) else online.signIn(email, password)
        _online.value = OnlineState(true, online.email)
        publishNow()
        refreshOnline()
    }

    fun onlineDisconnect() {
        online.signOut()
        _online.value = OnlineState(false, null)
    }

    /** Scarica le richieste in attesa e importa in rubrica le clienti registrate online. */
    suspend fun refreshOnline() {
        if (!online.isConnected) return
        _online.value = _online.value.copy(loading = true)
        try {
            val reqs = online.pendingRequests()
            importOnlineClients()
            _online.value = _online.value.copy(
                connected = true, requests = reqs, error = null, loading = false, lastSync = System.currentTimeMillis(),
            )
        } catch (e: Exception) {
            _online.value = _online.value.copy(error = e.message, loading = false)
        }
    }

    private suspend fun importOnlineClients() {
        val remote = online.clients()
        val local = dao.allClients()
        fun digits(p: String) = p.filter { it.isDigit() }.takeLast(9)
        for (r in remote) {
            if (r.nome.isBlank()) continue
            val match = local.firstOrNull {
                (r.telefono.isNotBlank() && digits(it.phone).isNotEmpty() && digits(it.phone) == digits(r.telefono)) ||
                    it.name.equals(r.nome.trim(), ignoreCase = true)
            }
            val city = normalizeCity(r.comune)
            if (match == null) {
                dao.insertClient(
                    Client(
                        name = r.nome.trim(), phone = r.telefono.trim(), email = r.email, notes = "Registrata online",
                        city = city, birthYear = r.annoNascita,
                    )
                )
            } else {
                val upd = match.copy(
                    email = match.email.ifBlank { r.email },
                    city = match.city.ifBlank { city },
                    birthYear = if (match.birthYear > 0) match.birthYear else r.annoNascita,
                )
                if (upd != match) dao.upsertClient(upd)
            }
        }
    }

    suspend fun confirmOnline(req: OnlineRequest, a: Appointment, operatorName: String) {
        val msg = "Ti aspettiamo " + dataMedia(LocalDate.ofEpochDay(a.date)) + " alle " + hhmm(a.startMin) + "."
        online.confirm(req.id, hhmm(a.startMin), operatorName, msg)
        _online.value = _online.value.copy(requests = _online.value.requests.filterNot { it.id == req.id })
    }

    suspend fun rejectOnline(req: OnlineRequest, message: String) {
        online.reject(req.id, message)
        _online.value = _online.value.copy(requests = _online.value.requests.filterNot { it.id == req.id })
    }

    /** Pubblica listino e giorni di chiusura sulla pagina delle clienti. */
    suspend fun publishNow() {
        if (!online.isConnected) return
        val ops = operators.value.take(settings.value.columns).map { it.id }
        val av = availability.value
        val closed = (1..7).filter { dow ->
            ops.isNotEmpty() && ops.all { op -> av.hasShifts(op) } &&
                shifts.value.none { it.dayOfWeek == dow && it.operatorId in ops }
        }
        online.publishInfo(settings.value.centerName, services.value.map { it.name }, closed)
    }

    init {
        // Aggiornamento richieste ogni minuto mentre l'app è sbloccata.
        viewModelScope.launch {
            while (true) {
                if (_unlocked.value && online.isConnected) refreshOnline()
                delay(60_000)
            }
        }
        // Listino e turni modificati → ripubblica (dopo qualche secondo di calma).
        viewModelScope.launch {
            combine(dao.services(), dao.shifts(), prefs.settings) { a, b, c -> Triple(a, b, c.columns) }
                .debounce(3_000)
                .collectLatest { runCatching { publishNow() } }
        }
    }
}
