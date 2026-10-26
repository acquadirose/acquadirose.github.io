package it.agenda.estetista

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.agenda.estetista.ui.AgendaTheme
import it.agenda.estetista.ui.DayScreen
import it.agenda.estetista.ui.LockScreen
import it.agenda.estetista.ui.SettingsScreen
import it.agenda.estetista.ui.ShiftsScreen
import it.agenda.estetista.ui.StatsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgendaTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                ) {
                    val vm: AgendaViewModel = viewModel()
                    val unlocked by vm.unlocked.collectAsStateWithLifecycle()
                    val settings by vm.settings.collectAsStateWithLifecycle()

                    // Ri-blocco dopo 5 minuti in secondo piano (non a ogni apertura di WhatsApp).
                    val owner = LocalLifecycleOwner.current
                    DisposableEffect(owner) {
                        val obs = LifecycleEventObserver { _, e ->
                            when (e) {
                                Lifecycle.Event.ON_STOP -> vm.onAppStopped()
                                Lifecycle.Event.ON_START -> vm.onAppStarted()
                                else -> {}
                            }
                        }
                        owner.lifecycle.addObserver(obs)
                        onDispose { owner.lifecycle.removeObserver(obs) }
                    }

                    if (!unlocked) {
                        LockScreen(settings.logoVersion, onUnlocked = vm::unlock)
                    } else {
                        var screen by rememberSaveable { mutableStateOf("agenda") }
                        when (screen) {
                            "settings" -> SettingsScreen(vm, onBack = { screen = "agenda" })
                            "stats" -> StatsScreen(vm, onBack = { screen = "agenda" })
                            "shifts" -> ShiftsScreen(vm, onBack = { screen = "agenda" })
                            else -> DayScreen(
                                vm,
                                onOpenSettings = { screen = "settings" },
                                onOpenStats = { screen = "stats" },
                                onOpenShifts = { screen = "shifts" },
                            )
                        }
                    }
                }
            }
        }
    }
}
