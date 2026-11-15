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
| Compilazione debug | Riuscita il 15 nov 2026 | `./gradlew assembleDebug` in locale |
| Test delle formule (durata, dislivello, pesi) | 15 su 15 | `./gradlew testDebugUnitTest` |
| Installazione e avvio su emulatore | Riusciti il 15 nov 2026, nessun crash | Emulatore Pixel 7, Android 16 (API 36), `adb install` e avvio; immagine in `docs/verifiche/scheletro-home.png` |
| Cambio di scheda | Riuscito | Tocco sulla barra inferiore nell'emulatore |
| Avvio su telefono reale | **Non eseguito** | Nessun dispositivo collegato |

## Emulatore

```bash
$ANDROID_HOME/emulator/emulator -avd betterout -no-window -no-audio
```

Il dispositivo virtuale `betterout` (Pixel 7, API 36) esiste solo sulla macchina di sviluppo. L'emulatore parte in inglese: l'italiano predefinito arriva con la fase delle impostazioni.
