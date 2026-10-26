package it.agenda.estetista.data

import java.time.LocalDate

/**
 * Disponibilità delle operatrici da turni settimanali e assenze.
 * Un'operatrice senza alcun turno impostato è considerata sempre disponibile.
 */
class Availability(shifts: List<Shift>, private val absences: List<Absence>) {
    private val byOp = shifts.groupBy { it.operatorId }

    fun hasShifts(op: Int): Boolean = byOp[op]?.isNotEmpty() == true

    fun absencesOn(op: Int, date: LocalDate): List<Absence> {
        val d = date.toEpochDay()
        return absences.filter { it.operatorId == op && d in it.fromDate..it.toDate }
    }

    /** Intervalli di lavoro [inizio, fine) del giorno, già al netto delle assenze. */
    fun workIntervals(op: Int, date: LocalDate): List<Pair<Int, Int>> {
        var base: List<Pair<Int, Int>> =
            if (!hasShifts(op)) listOf(0 to 24 * 60)
            else byOp[op].orEmpty()
                .filter { it.dayOfWeek == date.dayOfWeek.value }
                .map { it.startMin to it.endMin }
                .sortedBy { it.first }
        for (a in absencesOn(op, date)) base = subtract(base, a.startMin to a.endMin)
        return base
    }

    /** Intervalli NON disponibili dentro l'orario di apertura (per ombreggiare la griglia). */
    fun blockedIntervals(op: Int, date: LocalDate, open: Int, close: Int): List<Pair<Int, Int>> {
        var blocked = listOf(open to close)
        for (w in workIntervals(op, date)) blocked = subtract(blocked, w)
        return blocked
    }

    fun isAvailable(op: Int, date: LocalDate, start: Int, end: Int): Boolean =
        workIntervals(op, date).any { start >= it.first && end <= it.second }

    /** Spiegazione breve, es. "Riposo", "Ferie", "In turno 9:00–13:00". */
    fun reasonUnavailable(op: Int, date: LocalDate): String {
        val abs = absencesOn(op, date).firstOrNull()
        if (abs != null) return abs.reason.ifBlank { "Assente" }
        return if (workIntervals(op, date).isEmpty()) "Riposo" else "Fuori turno"
    }

    companion object {
        fun subtract(list: List<Pair<Int, Int>>, cut: Pair<Int, Int>): List<Pair<Int, Int>> =
            list.flatMap { (s, e) ->
                if (cut.second <= s || cut.first >= e) listOf(s to e)
                else listOfNotNull(
                    (s to cut.first).takeIf { cut.first > s },
                    (cut.second to e).takeIf { cut.second < e },
                )
            }
    }
}
