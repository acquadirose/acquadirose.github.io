package it.agenda.estetista.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.LocalDate

/** Indirizzo della pagina di prenotazione per le clienti. */
const val PORTALE_URL = "https://acquadirose.github.io/"

/** Email della titolare abilitata nelle regole Firestore (vedi firestore.rules). */
const val ADMIN_EMAIL_DEFAULT = "ist.acquadirose@gmail.com"

private const val API_KEY = "AIzaSyDmyji8kB3eXOUOsSENqT7BX6gcn0vhItA"
private const val PROJECT = "acquadirose-a0ddb"
private const val DOCS = "https://firestore.googleapis.com/v1/projects/$PROJECT/databases/(default)/documents"

/** Richiesta arrivata dalla pagina web. */
data class OnlineRequest(
    val id: String,
    val uid: String,
    val nome: String,
    val telefono: String,
    val email: String,
    val data: LocalDate,
    val fascia: String,
    val trattamento: String,
    val note: String,
    val stato: String,
    val creata: Instant?,
)

data class OnlineClient(val uid: String, val nome: String, val telefono: String, val email: String, val comune: String, val annoNascita: Int)

class OnlineException(message: String) : Exception(message)

/**
 * Collegamento a Firebase tramite le API REST (nessuna libreria aggiuntiva):
 * accesso della titolare, lettura/aggiornamento delle richieste, pubblicazione del listino.
 */
class Online(context: Context) {
    private val sp = context.getSharedPreferences("online", Context.MODE_PRIVATE)

    val email: String? get() = sp.getString("email", null)
    val isConnected: Boolean get() = sp.getString("refresh", null) != null

    private var idToken: String? = null
    private var tokenExpiry = 0L

    // ── Accesso ──

    suspend fun signIn(email: String, password: String) = auth("accounts:signInWithPassword", email, password)

    suspend fun signUp(email: String, password: String) = auth("accounts:signUp", email, password)

    private suspend fun auth(path: String, email: String, password: String) {
        val body = JSONObject().put("email", email.trim()).put("password", password).put("returnSecureToken", true)
        val res = http("POST", "https://identitytoolkit.googleapis.com/v1/$path?key=$API_KEY", body.toString(), null)
        storeTokens(
            res.getString("idToken"), res.getString("refreshToken"),
            res.optString("expiresIn", "3600").toLong(), res.optString("email", email.trim()),
        )
    }

    fun signOut() {
        sp.edit().clear().apply()
        idToken = null
        tokenExpiry = 0
    }

    private fun storeTokens(id: String, refresh: String, expiresIn: Long, mail: String?) {
        idToken = id
        tokenExpiry = System.currentTimeMillis() + (expiresIn - 120) * 1000
        sp.edit().putString("refresh", refresh).apply()
        if (mail != null) sp.edit().putString("email", mail).apply()
    }

    private suspend fun token(): String {
        idToken?.let { if (System.currentTimeMillis() < tokenExpiry) return it }
        val refresh = sp.getString("refresh", null) ?: throw OnlineException("Tablet non collegato")
        val form = "grant_type=refresh_token&refresh_token=" + URLEncoder.encode(refresh, "UTF-8")
        val res = http("POST", "https://securetoken.googleapis.com/v1/token?key=$API_KEY", form, null, form = true)
        storeTokens(res.getString("id_token"), res.getString("refresh_token"), res.optString("expires_in", "3600").toLong(), null)
        return idToken!!
    }

    // ── Richieste ──

    suspend fun pendingRequests(): List<OnlineRequest> {
        val q = JSONObject().put(
            "structuredQuery", JSONObject()
                .put("from", JSONArray().put(JSONObject().put("collectionId", "richieste")))
                .put(
                    "where", JSONObject().put(
                        "fieldFilter", JSONObject()
                            .put("field", JSONObject().put("fieldPath", "stato"))
                            .put("op", "EQUAL")
                            .put("value", JSONObject().put("stringValue", "in_attesa"))
                    )
                )
        )
        return runQuery(q).mapNotNull { doc ->
            val f = doc.optJSONObject("fields") ?: return@mapNotNull null
            val data = runCatching { LocalDate.parse(f.str("data")) }.getOrNull() ?: return@mapNotNull null
            OnlineRequest(
                id = doc.getString("name").substringAfterLast('/'),
                uid = f.str("uid"), nome = f.str("nome"), telefono = f.str("telefono"), email = f.str("email"),
                data = data, fascia = f.str("fascia"), trattamento = f.str("trattamento"), note = f.str("note"),
                stato = f.str("stato"),
                creata = f.optJSONObject("creata")?.optString("timestampValue")?.let { runCatching { Instant.parse(it) }.getOrNull() },
            )
        }.sortedBy { it.creata }
    }

    suspend fun clients(): List<OnlineClient> {
        val q = JSONObject().put(
            "structuredQuery", JSONObject().put("from", JSONArray().put(JSONObject().put("collectionId", "clienti")))
        )
        return runQuery(q).mapNotNull { doc ->
            val f = doc.optJSONObject("fields") ?: return@mapNotNull null
            OnlineClient(
                doc.getString("name").substringAfterLast('/'), f.str("nome"), f.str("telefono"), f.str("email"),
                f.str("comune"), f.optJSONObject("annoNascita")?.optString("integerValue")?.toIntOrNull() ?: 0,
            )
        }
    }

    suspend fun confirm(id: String, ora: String, operatrice: String, messaggio: String) =
        patch("richieste/$id", mapOf("stato" to "confermata", "ora" to ora, "operatrice" to operatrice, "messaggio" to messaggio))

    suspend fun reject(id: String, messaggio: String) =
        patch("richieste/$id", mapOf("stato" to "rifiutata", "messaggio" to messaggio))

    /** Listino e giorni di chiusura mostrati sulla pagina delle clienti. */
    suspend fun publishInfo(centro: String, trattamenti: List<String>, giorniChiusi: List<Int>) {
        val fields = JSONObject()
            .put("centro", sv(centro))
            .put("trattamenti", JSONObject().put("arrayValue", JSONObject().put("values", JSONArray(trattamenti.map { sv(it) }))))
            .put(
                "giorniChiusi",
                JSONObject().put("arrayValue", JSONObject().put("values", JSONArray(giorniChiusi.map { JSONObject().put("integerValue", it.toString()) }))),
            )
            .put("aggiornato", JSONObject().put("timestampValue", Instant.now().toString()))
        http("PATCH", "$DOCS/pubblico/info", JSONObject().put("fields", fields).toString(), token())
    }

    private suspend fun patch(path: String, values: Map<String, String>) {
        val fields = JSONObject()
        values.forEach { (k, v) -> fields.put(k, sv(v)) }
        fields.put("aggiornata", JSONObject().put("timestampValue", Instant.now().toString()))
        val mask = (values.keys + "aggiornata").joinToString("&") { "updateMask.fieldPaths=$it" }
        http("PATCH", "$DOCS/$path?$mask&currentDocument.exists=true", JSONObject().put("fields", fields).toString(), token())
    }

    private suspend fun runQuery(q: JSONObject): List<JSONObject> {
        val raw = httpRaw("POST", "$DOCS:runQuery", q.toString(), token())
        val arr = JSONArray(raw)
        return (0 until arr.length()).mapNotNull { arr.getJSONObject(it).optJSONObject("document") }
    }

    // ── HTTP ──

    private fun sv(v: String) = JSONObject().put("stringValue", v)
    private fun JSONObject.str(k: String) = optJSONObject(k)?.optString("stringValue", "") ?: ""

    private suspend fun http(method: String, url: String, body: String?, bearer: String?, form: Boolean = false): JSONObject =
        JSONObject(httpRaw(method, url, body, bearer, form).ifBlank { "{}" })

    private suspend fun httpRaw(method: String, url: String, body: String?, bearer: String?, form: Boolean = false): String =
        withContext(Dispatchers.IO) {
            val c = URL(url).openConnection() as HttpURLConnection
            try {
                if (method == "PATCH") {
                    // HttpURLConnection non supporta PATCH: override standard di Google APIs.
                    c.requestMethod = "POST"
                    c.setRequestProperty("X-HTTP-Method-Override", "PATCH")
                } else {
                    c.requestMethod = method
                }
                c.connectTimeout = 15_000
                c.readTimeout = 20_000
                bearer?.let { c.setRequestProperty("Authorization", "Bearer $it") }
                if (body != null) {
                    c.doOutput = true
                    c.setRequestProperty(
                        "Content-Type",
                        if (form) "application/x-www-form-urlencoded" else "application/json; charset=utf-8",
                    )
                    c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
                val code = c.responseCode
                val text = (if (code in 200..299) c.inputStream else c.errorStream)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
                if (code !in 200..299) throw OnlineException(readableError(text, code))
                text
            } catch (e: IOException) {
                throw OnlineException("Connessione internet assente o lenta")
            } finally {
                c.disconnect()
            }
        }

    private fun readableError(text: String, code: Int): String {
        val msg = runCatching { JSONObject(text).getJSONObject("error").optString("message") }.getOrNull().orEmpty()
        return when {
            msg.startsWith("EMAIL_NOT_FOUND") || msg.startsWith("INVALID_LOGIN_CREDENTIALS") || msg.startsWith("INVALID_PASSWORD") ->
                "Email o password non corrette"
            msg.startsWith("EMAIL_EXISTS") -> "Esiste già un account con questa email: usa «Collega»"
            msg.startsWith("WEAK_PASSWORD") -> "La password deve avere almeno 6 caratteri"
            msg.startsWith("TOO_MANY_ATTEMPTS") -> "Troppi tentativi, riprova più tardi"
            msg.startsWith("TOKEN_EXPIRED") || msg.startsWith("INVALID_REFRESH_TOKEN") -> "Accesso scaduto: ricollega il tablet"
            code == 403 || msg.contains("PERMISSION_DENIED") ->
                "Permesso negato: le regole di Firestore non sono pubblicate o l'email non è quella della titolare"
            msg.isNotBlank() -> msg
            else -> "Errore del server ($code)"
        }
    }
}
