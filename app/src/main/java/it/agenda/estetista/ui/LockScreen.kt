package it.agenda.estetista.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.agenda.estetista.data.Security
import kotlinx.coroutines.delay

private const val MIN_LEN = 4
private const val MAX_LEN = 8

/**
 * Schermata di accesso con il logo di Acqua di Rose.
 * Al primo avvio chiede di creare la password (due volte), poi la richiede a ogni apertura.
 */
@Composable
fun LockScreen(logoVersion: Int, onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val security = remember { Security(context) }
    val creating = remember { !security.hasPassword() }

    var pin by remember { mutableStateOf("") }
    var first by remember { mutableStateOf<String?>(null) }   // creazione: prima digitazione
    var message by remember { mutableStateOf<String?>(null) }
    var wait by remember { mutableLongStateOf(security.waitMillis()) }
    var showForgot by remember { mutableStateOf(false) }

    LaunchedEffect(wait) {
        if (wait > 0) {
            delay(1000)
            wait = security.waitMillis()
        }
    }

    val title = when {
        creating && first == null -> "Crea la password di accesso"
        creating -> "Ripeti la password"
        else -> "Inserisci la password"
    }

    fun submit() {
        if (pin.length < MIN_LEN) { message = "Almeno $MIN_LEN cifre"; return }
        if (creating) {
            if (first == null) {
                first = pin; pin = ""; message = null
            } else if (first == pin) {
                security.setPassword(pin); onUnlocked()
            } else {
                first = null; pin = ""; message = "Le due password non coincidono, riprova"
            }
        } else {
            if (security.check(pin)) onUnlocked()
            else {
                pin = ""
                wait = security.waitMillis()
                message = if (wait > 0) "Troppi tentativi" else "Password errata"
            }
        }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Rose.Ground)
    ) {
        // Rosa grande in filigrana sullo sfondo.
        RoseMark(
            if (maxWidth > maxHeight) maxHeight * 0.9f else maxWidth * 0.9f,
            Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .alpha(0.07f),
        )
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CenterLogo(logoVersion, 120.dp)
            Spacer(Modifier.height(8.dp))
            Ornament(Modifier.size(width = 280.dp, height = 24.dp))
            Spacer(Modifier.height(24.dp))
            Text(title, style = SerifTitle.copy(fontSize = 24.sp), color = Rose.Ink)
            Spacer(Modifier.height(16.dp))

            // Pallini della password
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.semantics { contentDescription = "${pin.length} cifre inserite" },
            ) {
                repeat(maxOf(MIN_LEN, pin.length).coerceAtMost(MAX_LEN)) { i ->
                    Box(
                        Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (i < pin.length) Rose.Primary else Rose.Surface)
                            .border(1.5.dp, Rose.Primary, CircleShape)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                when {
                    wait > 0 -> "Riprova tra ${wait / 1000 + 1} secondi"
                    message != null -> message!!
                    creating -> "Da $MIN_LEN a $MAX_LEN cifre"
                    else -> " "
                },
                color = if (message != null || wait > 0) Rose.Now else Rose.Muted,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(20.dp))

            // Tastierino
            val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "⌫", "0", "OK")
            keys.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    row.forEach { k ->
                        val enabled = wait == 0L
                        Box(
                            Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(if (k == "OK") Rose.Primary else Rose.Surface)
                                .border(1.dp, Rose.Line, CircleShape)
                                .clickable(enabled = enabled) {
                                    message = null
                                    when (k) {
                                        "⌫" -> pin = pin.dropLast(1)
                                        "OK" -> submit()
                                        else -> if (pin.length < MAX_LEN) pin += k
                                    }
                                }
                                .semantics { contentDescription = if (k == "⌫") "Cancella" else k },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                k,
                                fontSize = if (k == "OK") 20.sp else 28.sp,
                                fontWeight = if (k == "OK") FontWeight.Bold else FontWeight.Normal,
                                color = if (k == "OK") androidx.compose.ui.graphics.Color.White else Rose.Ink,
                            )
                        }
                    }
                }
            }
            if (!creating) {
                TextButton(onClick = { showForgot = true }) { Text("Password dimenticata?", color = Rose.Muted) }
            }
        }
    }

    if (showForgot) {
        AlertDialog(
            onDismissRequest = { showForgot = false },
            title = { Text("Password dimenticata") },
            text = {
                Text(
                    "Per sicurezza la password non si può recuperare. Bisogna disinstallare e reinstallare l'app " +
                        "e poi ripristinare l'ultimo backup da Impostazioni › Backup. " +
                        "Senza un backup gli appuntamenti andrebbero persi."
                )
            },
            confirmButton = { TextButton(onClick = { showForgot = false }) { Text("Ho capito") } },
        )
    }
}
