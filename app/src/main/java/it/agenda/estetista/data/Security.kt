package it.agenda.estetista.data

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/** Password numerica di accesso: salvata solo come hash SHA-256 con sale casuale. */
class Security(context: Context) {
    private val sp = context.getSharedPreferences("security", Context.MODE_PRIVATE)

    fun hasPassword(): Boolean = sp.contains("hash")

    fun setPassword(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        sp.edit()
            .putString("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("hash", hash(salt, pin))
            .putInt("fails", 0)
            .apply()
    }

    fun check(pin: String): Boolean {
        val salt = Base64.decode(sp.getString("salt", "") ?: "", Base64.NO_WRAP)
        val ok = hash(salt, pin) == sp.getString("hash", null)
        sp.edit().putInt("fails", if (ok) 0 else failures() + 1).apply()
        if (!ok && failures() % 5 == 0) sp.edit().putLong("lockedUntil", System.currentTimeMillis() + 30_000).apply()
        return ok
    }

    fun failures(): Int = sp.getInt("fails", 0)

    /** Millisecondi di attesa dopo 5 tentativi sbagliati. */
    fun waitMillis(): Long = (sp.getLong("lockedUntil", 0) - System.currentTimeMillis()).coerceAtLeast(0)

    private fun hash(salt: ByteArray, pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        val out = md.digest(pin.toByteArray(Charsets.UTF_8))
        return out.joinToString("") { "%02x".format(it) }
    }
}
