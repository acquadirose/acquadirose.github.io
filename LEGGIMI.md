# Agenda Acqua di Rose – app Android per tablet

Agenda appuntamenti per **Acqua di Rose – Istituto di Bellezza, Tortoreto**.
Kotlin + Jetpack Compose + Room (SQLite locale). Nessun server, nessun costo ricorrente.

## Compilare e installare
1. Apri la cartella in **Android Studio** (Ladybug o successivo) → *Open*. Al primo avvio scarica Gradle 8.9 e le dipendenze.
2. Tablet in modalità sviluppatore + debug USB → *Run ▶*.
   Oppure *Build › Build App Bundle(s)/APK(s) › Build APK(s)* e copia `app/build/outputs/apk/debug/app-debug.apk` sul tablet.
3. Per una versione "definitiva" usa `assembleRelease`: è firmata con la chiave di debug, va bene per installazione manuale (non per il Play Store).

Requisiti: Android 8.0+ (minSdk 26). Pensata per tablet in orizzontale.

## Funzioni
- Vista giornaliera con **1–4 colonne** (operatrici), nomi e colori configurabili; nascondere una colonna non cancella i suoi appuntamenti.
- Tocco su un orario libero → nuovo appuntamento già posizionato; tocco su un blocco → modifica.
- Listino trattamenti con durata (scelta dal listino imposta la durata).
- Stati: da confermare / confermato / completato / disdetto. Avviso sovrapposizioni nella stessa colonna.
- Autocompletamento clienti già viste (nome + cellulare).
- **Promemoria WhatsApp**: apre la chat con il messaggio già scritto, l'operatrice preme Invio. Elenco “Promemoria” per oggi/domani/dopodomani con spunta degli inviati. Testo modificabile con {nome} {giorno} {ora} {servizio} {centro} {citta}.
- Linea dell'ora corrente, navigazione per giorni e calendario.
- Logo incluso; sostituibile da *Impostazioni › Cambia logo* senza ricompilare.
- **Backup/ripristino** su file JSON (Drive, chiavetta, Download).

## Struttura
```
data/   Entities, AgendaDao, AppDatabase (Room), Prefs (impostazioni), Backup (JSON)
ui/     DayScreen (griglia), AppointmentDialog, RemindersDialog, SettingsScreen, Theme, Common
util/   WhatsApp (link wa.me, normalizzazione +39), Logo, formattazione date
```

## Limiti voluti
- I messaggi WhatsApp non partono da soli: serve un tocco (l'invio automatico richiede la WhatsApp Business Platform a pagamento).
- I dati sono solo sul tablet: fare il backup regolarmente.

## Prenotazioni online
- Pagina clienti: https://acquadirose.github.io/ (cartella `docs/`, GitHub Pages da `main` › `/docs`).
- Database: Firebase progetto `acquadirose-a0ddb` (Authentication email/password + Firestore).
- Regole di sicurezza: `firestore.rules` → da incollare in Firebase › Firestore › Regole.
- Account titolare (tablet): ist.acquadirose@gmail.com.
