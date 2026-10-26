package it.agenda.estetista.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Operatrice: id fissi 1..4; quante colonne mostrare lo decide AppSettings.columns. */
@Entity(tableName = "operators")
data class Operator(
    @PrimaryKey val id: Int,
    val name: String,
)

@Entity(tableName = "services")
data class Service(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val durationMin: Int,
)

@Entity(tableName = "appointments", indices = [Index("date")])
data class Appointment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Giorno come LocalDate.toEpochDay(). */
    val date: Long,
    val operatorId: Int,
    /** Minuti dalla mezzanotte. */
    val startMin: Int,
    val durationMin: Int,
    val clientName: String,
    val phone: String,
    val service: String,
    val notes: String,
    val status: String = Status.CONFERMATO.name,
    val reminderSent: Boolean = false,
    /** Collegamento all'anagrafica (null per appuntamenti senza scheda). */
    val clientId: Long? = null,
)

val Appointment.endMin: Int get() = startMin + durationMin
val Appointment.statusEnum: Status get() = Status.entries.firstOrNull { it.name == status } ?: Status.CONFERMATO

enum class Status(val label: String) {
    DA_CONFERMARE("Da confermare"),
    CONFERMATO("Confermato"),
    COMPLETATO("Completato"),
    DISDETTO("Disdetto"),
}


/** Anagrafica cliente. */
@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    /** Testo libero, es. "12/05". */
    val birthday: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    /** Anno dell'ultimo messaggio di compleanno inviato (0 = mai). */
    @ColumnInfo(defaultValue = "0") val birthdayMsgYear: Int = 0,
)

/** Giorno e mese dal campo compleanno ("12/05", "12-5", "12.05.1990"...). */
fun Client.birthdayDayMonth(): Pair<Int, Int>? {
    val m = Regex("""^\s*(\d{1,2})\s*[/.\- ]\s*(\d{1,2})""").find(birthday) ?: return null
    val d = m.groupValues[1].toInt()
    val mo = m.groupValues[2].toInt()
    return if (d in 1..31 && mo in 1..12) d to mo else null
}

/** Post-it: legato a un giorno (date = epochDay) oppure fisso (date = null). */
@Entity(tableName = "postit")
data class PostIt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long? = null,
    val text: String,
    val color: Int = 0,
    val done: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Turno settimanale: l'operatrice lavora il giorno [dayOfWeek] (1 = lunedì) da startMin a endMin. */
@Entity(tableName = "shifts")
data class Shift(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val operatorId: Int,
    val dayOfWeek: Int,
    val startMin: Int,
    val endMin: Int,
)

/** Assenza (ferie, permesso, malattia) dal giorno fromDate al giorno toDate, nella fascia indicata. */
@Entity(tableName = "absences")
data class Absence(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val operatorId: Int,
    val fromDate: Long,
    val toDate: Long,
    val startMin: Int = 0,
    val endMin: Int = 24 * 60,
    val reason: String = "",
)
