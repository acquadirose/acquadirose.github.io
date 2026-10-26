package it.agenda.estetista.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

const val DEFAULT_TEMPLATE =
    "Ciao {nome}! 🌹 Ti ricordiamo il tuo appuntamento da Acqua di Rose " +
        "{giorno} alle {ora} per {servizio}.\n" +
        "Rispondi CONFERMO oppure DISDICO. A presto!\n" +
        "Acqua di Rose – Tortoreto"

const val DEFAULT_BIRTHDAY_TEMPLATE =
    "Buon compleanno {nome}! 🎂 Questo è il tuo mese e da Acqua di Rose vogliamo festeggiarti: " +
        "per te uno sconto del {sconto} su un trattamento a scelta, valido fino al {scadenza}.\n" +
        "Ti aspettiamo! 🌹\nAcqua di Rose – Tortoreto"

data class AppSettings(
    val centerName: String = "Acqua di Rose",
    val city: String = "Tortoreto",
    val columns: Int = 2,
    val openMin: Int = 9 * 60,
    val closeMin: Int = 19 * 60,
    val slotMin: Int = 15,
    val reminderTemplate: String = DEFAULT_TEMPLATE,
    /** Incrementato a ogni cambio logo, per ricaricare l'immagine. */
    val logoVersion: Int = 0,
    val birthdayDiscount: String = "10%",
    val birthdayTemplate: String = DEFAULT_BIRTHDAY_TEMPLATE,
)

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = state

    private fun load(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            centerName = sp.getString("centerName", d.centerName) ?: d.centerName,
            city = sp.getString("city", d.city) ?: d.city,
            columns = sp.getInt("columns", d.columns).coerceIn(1, 4),
            openMin = sp.getInt("openMin", d.openMin),
            closeMin = sp.getInt("closeMin", d.closeMin),
            slotMin = sp.getInt("slotMin", d.slotMin),
            reminderTemplate = sp.getString("reminderTemplate", d.reminderTemplate) ?: d.reminderTemplate,
            logoVersion = sp.getInt("logoVersion", d.logoVersion),
            birthdayDiscount = sp.getString("birthdayDiscount", d.birthdayDiscount) ?: d.birthdayDiscount,
            birthdayTemplate = sp.getString("birthdayTemplate", d.birthdayTemplate) ?: d.birthdayTemplate,
        )
    }

    fun update(s: AppSettings) {
        sp.edit()
            .putString("centerName", s.centerName)
            .putString("city", s.city)
            .putInt("columns", s.columns.coerceIn(1, 4))
            .putInt("openMin", s.openMin)
            .putInt("closeMin", s.closeMin)
            .putInt("slotMin", s.slotMin)
            .putString("reminderTemplate", s.reminderTemplate)
            .putInt("logoVersion", s.logoVersion)
            .putString("birthdayDiscount", s.birthdayDiscount)
            .putString("birthdayTemplate", s.birthdayTemplate)
            .apply()
        state.value = s.copy(columns = s.columns.coerceIn(1, 4))
    }
}
