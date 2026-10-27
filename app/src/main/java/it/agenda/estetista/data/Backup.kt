package it.agenda.estetista.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Backup completo in un file JSON scelto dall'utente (Drive, chiavetta, cartella Download...). */
object Backup {

    suspend fun export(context: Context, uri: Uri, db: AppDatabase, prefs: Prefs) =
        withContext(Dispatchers.IO) {
            val dao = db.dao()
            val s = prefs.settings.value
            val root = JSONObject()
                .put("app", "acqua-di-rose-agenda")
                .put("version", 5)
                .put(
                    "settings", JSONObject()
                        .put("centerName", s.centerName)
                        .put("city", s.city)
                        .put("columns", s.columns)
                        .put("openMin", s.openMin)
                        .put("closeMin", s.closeMin)
                        .put("slotMin", s.slotMin)
                        .put("reminderTemplate", s.reminderTemplate)
                        .put("birthdayDiscount", s.birthdayDiscount)
                        .put("birthdayTemplate", s.birthdayTemplate)
                )
                .put("operators", JSONArray(dao.allOperators().map {
                    JSONObject().put("id", it.id).put("name", it.name)
                }))
                .put("services", JSONArray(dao.allServices().map {
                    JSONObject().put("name", it.name).put("durationMin", it.durationMin)
                }))
                .put("appointments", JSONArray(dao.allAppointments().map {
                    JSONObject()
                        .put("date", it.date)
                        .put("operatorId", it.operatorId)
                        .put("startMin", it.startMin)
                        .put("durationMin", it.durationMin)
                        .put("clientName", it.clientName)
                        .put("phone", it.phone)
                        .put("service", it.service)
                        .put("notes", it.notes)
                        .put("status", it.status)
                        .put("reminderSent", it.reminderSent)
                        .put("clientId", it.clientId ?: JSONObject.NULL)
                }))
                .put("clients", JSONArray(dao.allClients().map {
                    JSONObject()
                        .put("id", it.id)
                        .put("name", it.name)
                        .put("phone", it.phone)
                        .put("email", it.email)
                        .put("birthday", it.birthday)
                        .put("notes", it.notes)
                        .put("createdAt", it.createdAt)
                        .put("birthdayMsgYear", it.birthdayMsgYear)
                        .put("birthYear", it.birthYear)
                        .put("city", it.city)
                }))
                .put("shifts", JSONArray(dao.allShifts().map {
                    JSONObject().put("operatorId", it.operatorId).put("dayOfWeek", it.dayOfWeek)
                        .put("startMin", it.startMin).put("endMin", it.endMin)
                }))
                .put("absences", JSONArray(dao.allAbsences().map {
                    JSONObject().put("operatorId", it.operatorId).put("fromDate", it.fromDate).put("toDate", it.toDate)
                        .put("startMin", it.startMin).put("endMin", it.endMin).put("reason", it.reason)
                }))
                .put("postits", JSONArray(dao.allPostIts().map {
                    JSONObject()
                        .put("date", it.date ?: JSONObject.NULL)
                        .put("text", it.text)
                        .put("color", it.color)
                        .put("done", it.done)
                        .put("createdAt", it.createdAt)
                }))
            val out = context.contentResolver.openOutputStream(uri, "wt")
                ?: error("Impossibile scrivere il file")
            out.use { it.write(root.toString(2).toByteArray(Charsets.UTF_8)) }
        }

    /** Sostituisce TUTTI i dati con quelli del backup. Restituisce il numero di appuntamenti. */
    suspend fun import(context: Context, uri: Uri, db: AppDatabase, prefs: Prefs): Int =
        withContext(Dispatchers.IO) {
            val text = context.contentResolver.openInputStream(uri)?.use {
                it.readBytes().toString(Charsets.UTF_8)
            } ?: error("Impossibile leggere il file")
            val root = JSONObject(text)
            require(root.optString("app") == "acqua-di-rose-agenda") { "Il file non è un backup dell'agenda" }

            val ops = root.getJSONArray("operators").objects().map {
                Operator(it.getInt("id"), it.getString("name"))
            }
            val services = root.getJSONArray("services").objects().map {
                Service(name = it.getString("name"), durationMin = it.getInt("durationMin"))
            }
            val appts = root.getJSONArray("appointments").objects().map {
                Appointment(
                    date = it.getLong("date"),
                    operatorId = it.getInt("operatorId"),
                    startMin = it.getInt("startMin"),
                    durationMin = it.getInt("durationMin"),
                    clientName = it.getString("clientName"),
                    phone = it.optString("phone"),
                    service = it.optString("service"),
                    notes = it.optString("notes"),
                    status = it.optString("status", Status.CONFERMATO.name),
                    reminderSent = it.optBoolean("reminderSent"),
                    clientId = if (it.isNull("clientId")) null else it.getLong("clientId"),
                )
            }
            val clients = (root.optJSONArray("clients") ?: JSONArray()).objects().map {
                Client(
                    id = it.getLong("id"),
                    name = it.getString("name"),
                    phone = it.optString("phone"),
                    email = it.optString("email"),
                    birthday = it.optString("birthday"),
                    notes = it.optString("notes"),
                    createdAt = it.optLong("createdAt", System.currentTimeMillis()),
                    birthdayMsgYear = it.optInt("birthdayMsgYear"),
                    birthYear = it.optInt("birthYear"),
                    city = it.optString("city"),
                )
            }
            val shifts = (root.optJSONArray("shifts") ?: JSONArray()).objects().map {
                Shift(operatorId = it.getInt("operatorId"), dayOfWeek = it.getInt("dayOfWeek"),
                    startMin = it.getInt("startMin"), endMin = it.getInt("endMin"))
            }
            val absences = (root.optJSONArray("absences") ?: JSONArray()).objects().map {
                Absence(operatorId = it.getInt("operatorId"), fromDate = it.getLong("fromDate"), toDate = it.getLong("toDate"),
                    startMin = it.optInt("startMin", 0), endMin = it.optInt("endMin", 24 * 60), reason = it.optString("reason"))
            }
            val postits = (root.optJSONArray("postits") ?: JSONArray()).objects().map {
                PostIt(
                    date = if (it.isNull("date")) null else it.getLong("date"),
                    text = it.getString("text"),
                    color = it.optInt("color"),
                    done = it.optBoolean("done"),
                    createdAt = it.optLong("createdAt", System.currentTimeMillis()),
                )
            }

            val dao = db.dao()
            db.withTransaction {
                dao.upsertOperators(ops)
                dao.clearServices()
                dao.insertServices(services)
                dao.clearAppointments()
                dao.clearClients()
                dao.insertClients(clients)
                dao.insertAppointments(appts)
                dao.clearPostIts()
                dao.insertPostIts(postits)
                dao.clearShifts()
                dao.insertShifts(shifts)
                dao.clearAbsences()
                dao.insertAbsences(absences)
            }

            val js = root.getJSONObject("settings")
            val cur = prefs.settings.value
            prefs.update(
                cur.copy(
                    centerName = js.optString("centerName", cur.centerName),
                    city = js.optString("city", cur.city),
                    columns = js.optInt("columns", cur.columns),
                    openMin = js.optInt("openMin", cur.openMin),
                    closeMin = js.optInt("closeMin", cur.closeMin),
                    slotMin = js.optInt("slotMin", cur.slotMin),
                    reminderTemplate = js.optString("reminderTemplate", cur.reminderTemplate),
                    birthdayDiscount = js.optString("birthdayDiscount", cur.birthdayDiscount),
                    birthdayTemplate = js.optString("birthdayTemplate", cur.birthdayTemplate),
                )
            )
            appts.size
        }

    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
}
