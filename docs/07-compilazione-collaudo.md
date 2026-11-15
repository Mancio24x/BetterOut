# Compilazione e collaudo

## Requisiti

JDK 17 o successivo e Android SDK con piattaforma 37 e build-tools 36. Il percorso dell'SDK va in `local.properties` (`sdk.dir=...`, file non versionato) o nella variabile `ANDROID_HOME`.

## Comandi

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew assembleRelease
```

- `app/build/outputs/apk/debug/app-debug.apk` — **debug**: firmato con la chiave di debug, installabile, identificativo `app.betterout.debug`.
- `app/build/outputs/apk/release/app-release-unsigned.apk` — **release non firmato**: non installabile finché non viene firmato con una chiave propria, che non sta nel repository.

## Installare l'APK debug

Con il telefono collegato e il debug USB attivo:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Oppure si copia il file sul telefono e lo si apre, autorizzando l'installazione da quella sorgente.

## GitHub Actions

Il flusso `Android` (`.github/workflows/android.yml`) esegue test, lint e compilazione a ogni push su `main` che tocca il codice, e pubblica fra gli artifact `BetterOut-debug-apk`, `BetterOut-release-NON-FIRMATO` e `rapporti`. Gli artifact si scaricano dalla pagina dell'esecuzione, in fondo.

## Cosa è verificato oggi

| Verifica | Esito | Come |
|---|---|---|
| Compilazione debug e release, lint | Riuscite il 15 nov 2026 | In locale e su GitHub Actions |
| Test delle regole (durata, dislivello, pesi, uscite) | 23 su 23 | `./gradlew testDebugUnitTest` |
| Installazione e avvio | Riusciti, nessun crash | Emulatore Pixel 7, Android 16 (API 36) |
| Creazione di un'uscita con solo nome e località | Riuscita | Percorso guidato sull'emulatore con `adb` |
| Persistenza dopo chiusura forzata e riavvio | Riuscita | `am force-stop`, riavvio, l'uscita è ancora in Home |
| Duplicazione indipendente | Riuscita | Copia rinominata e con data: l'originale resta invariato |
| Data aggiunta a una bozza → proposta "Programmata" | Riuscita dopo una correzione | Difetto trovato in questa prova e corretto |
| Avviso di modifiche non salvate | Riuscito | Tasto indietro con modifiche: tre scelte |
| Conferma di eliminazione, elenco, filtri, filtro vuoto | Riusciti | Immagini in `docs/verifiche/` |
| Tema scuro, testo ingrandito, TalkBack | **Non eseguiti** | — |
| Avvio su telefono reale | **Non eseguito** | Nessun dispositivo collegato |

## Emulatore

```bash
$ANDROID_HOME/emulator/emulator -avd betterout -no-window -no-audio
```

Il dispositivo virtuale `betterout` (Pixel 7, API 36) esiste solo sulla macchina di sviluppo. Per le prove la lingua dell'emulatore è stata impostata sull'italiano.
