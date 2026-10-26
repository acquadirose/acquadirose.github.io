package it.agenda.estetista.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import it.agenda.estetista.data.AppSettings
import it.agenda.estetista.data.Appointment
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

val IT: Locale = Locale.ITALIAN

fun hhmm(min: Int): String = "%02d:%02d".format(min / 60, min % 60)

fun durata(min: Int): String = when {
    min < 60 -> "$min min"
    min % 60 == 0 -> "${min / 60} h"
    else -> "${min / 60} h ${min % 60}"
}

fun String.capitalizeIt(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase(IT) else it.toString() }

fun dataLunga(d: LocalDate): String =
    d.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", IT)).capitalizeIt()

fun dataMedia(d: LocalDate): String =
    d.format(DateTimeFormatter.ofPattern("EEEE d MMMM", IT))

object WhatsApp {
    /** Numero in formato internazionale senza '+'. Senza prefisso si assume Italia (+39). */
    fun normalize(phone: String): String? {
        var d = phone.filter { it.isDigit() }
        val international = phone.trim().startsWith("+") || d.startsWith("00")
        if (d.startsWith("00")) d = d.drop(2)
        if (!international && !(d.startsWith("39") && d.length >= 11)) d = "39$d"
        return d.takeIf { it.length >= 9 }
    }

    fun message(template: String, a: Appointment, s: AppSettings): String {
        val nome = a.clientName.trim().substringBefore(' ')
        return template
            .replace("{nome}", nome)
            .replace("{giorno}", dataMedia(LocalDate.ofEpochDay(a.date)))
            .replace("{ora}", hhmm(a.startMin))
            .replace("{servizio}", a.service.ifBlank { "il tuo trattamento" })
            .replace("{centro}", s.centerName)
            .replace("{citta}", s.city)
    }

    fun birthdayMessage(s: AppSettings, c: it.agenda.estetista.data.Client, month: java.time.YearMonth): String {
        val nome = c.name.trim().substringBefore(' ')
        val scadenza = month.atEndOfMonth().format(DateTimeFormatter.ofPattern("d MMMM", IT))
        return s.birthdayTemplate
            .replace("{nome}", nome)
            .replace("{sconto}", s.birthdayDiscount.trim())
            .replace("{scadenza}", scadenza)
            .replace("{mese}", month.format(DateTimeFormatter.ofPattern("MMMM", IT)))
            .replace("{centro}", s.centerName)
            .replace("{citta}", s.city)
    }

    /** Apre la chat con testo già scritto: l'invio lo conferma l'operatrice. */
    fun open(context: Context, phone: String, text: String): Boolean {
        val n = normalize(phone) ?: return false
        val uri = Uri.parse("https://wa.me/$n?text=" + Uri.encode(text))
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    fun call(context: Context, phone: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone.trim())))
        } catch (_: ActivityNotFoundException) {
        }
    }
}

/** Logo del centro salvato nella memoria interna dell'app. */
object Logo {
    private fun file(context: Context) = File(context.filesDir, "logo")

    fun save(context: Context, uri: Uri) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            file(context).outputStream().use { input.copyTo(it) }
        } ?: error("Immagine non leggibile")
    }

    fun remove(context: Context) {
        file(context).delete()
    }

    fun load(context: Context): Bitmap? {
        val f = file(context)
        if (!f.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, bounds)
        var sample = 1
        while (bounds.outWidth / sample > 512 || bounds.outHeight / sample > 512) sample *= 2
        return BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }
}
