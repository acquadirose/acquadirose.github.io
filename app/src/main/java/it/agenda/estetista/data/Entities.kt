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
    /** Anno di nascita (0 = non indicato). */
    @ColumnInfo(defaultValue = "0") val birthYear: Int = 0,
    /** Comune di residenza. */
    @ColumnInfo(defaultValue = "") val city: String = "",
)

/** Età in anni compiuti alla data indicata (tiene conto del compleanno se noto). */
fun Client.age(on: java.time.LocalDate = java.time.LocalDate.now()): Int? {
    if (birthYear <= 0) return null
    var a = on.year - birthYear
    val dm = birthdayDayMonth()
    if (dm != null && (on.monthValue < dm.second || (on.monthValue == dm.second && on.dayOfMonth < dm.first))) a -= 1
    return a.takeIf { it in 0..120 }
}

/** Fasce d'età usate in statistiche e filtri. */
enum class AgeBand(val label: String, val range: IntRange) {
    U25("Fino a 24", 0..24),
    A25("25–34", 25..34),
    A35("35–44", 35..44),
    A45("45–54", 45..54),
    A55("55–64", 55..64),
    O65("65 e oltre", 65..120);

    companion object {
        fun of(age: Int?): AgeBand? = age?.let { a -> entries.firstOrNull { a in it.range } }
    }
}

/** Comuni proposti di default (zona di Tortoreto), oltre a quelli già presenti in rubrica. */
val COMUNI_VICINI = listOf(
    "Tortoreto", "Alba Adriatica", "Giulianova", "Martinsicuro", "Colonnella", "Corropoli",
    "Sant'Omero", "Nereto", "Controguerra", "Mosciano Sant'Angelo", "Bellante", "Teramo",
    "Sant'Egidio alla Vibrata", "Ancarano", "Torano Nuovo", "Civitella del Tronto",
    "San Benedetto del Tronto", "Roseto degli Abruzzi",
)

/** "alba adriatica" → "Alba Adriatica"; mantiene apostrofi e preposizioni minuscole. */
fun normalizeCity(raw: String): String {
    val small = setOf("di", "del", "della", "degli", "dei", "alla", "al", "in", "sul", "sulla")
    return raw.trim().replace(Regex("\\s+"), " ").split(" ").mapIndexed { i, w ->
        val lw = w.lowercase()
        if (i > 0 && lw in small) lw
        else lw.split("'").joinToString("'") { part -> part.replaceFirstChar { it.titlecase() } }
    }.joinToString(" ")
}

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
