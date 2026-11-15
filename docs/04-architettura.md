# Architettura e decisioni

## Tecnologie

Kotlin, Jetpack Compose con Material 3, Navigation Compose, ViewModel, Coroutines e Flow, Room, DataStore. Un solo modulo Gradle (`app`). Nessun backend: tutto gira sul dispositivo.

Versioni fissate in `gradle/libs.versions.toml` e verificate compilando il 15 novembre 2026: Gradle 9.5.0, Android Gradle Plugin 9.3.1 (con Kotlin integrato, 2.2.10), KSP 2.3.6, Compose BOM 2026.09.00, Navigation 2.10.2, Room 2.8.5, DataStore 1.3.0, Lifecycle 2.11.0. `compileSdk` 37, `targetSdk` 36, `minSdk` 26 (Android 8.0). Sono versioni stabili già compatibili fra loro; esistono rilasci più recenti (AGP 9.5.1, Gradle 9.9.1) non ancora adottati.

## Strati

| Pacchetto | Contenuto | Regola |
|---|---|---|
| `app.betterout.domain` | Formule e regole: durata, dislivello, pesi, suggerimenti | Kotlin puro, nessuna dipendenza da Android, coperto da test |
| `app.betterout.data` | Room, DataStore, file GPX, client dei servizi esterni | Espone repository con `Flow`; ogni servizio esterno sta dietro un'interfaccia sostituibile |
| `app.betterout.ui` | Schermate Compose, ViewModel, tema, navigazione | Nessun accesso diretto a database o rete |

`domain` è il solo strato già popolato; `data` nasce con la fase "uscite e dati locali".

## Decisioni

1. **Identificativi UUID** per tutte le entità: rendono possibile l'unione dei backup senza conflitti.
2. **La checklist copia i dati dell'oggetto** (nome, peso, tipo, volume) e ne conserva il collegamento: lo storico non cambia quando cambia l'inventario.
3. **Il GPX originale è conservato come file** nello spazio privato dell'app; la geometria per calcoli e disegno è salvata a parte.
4. **Servizi esterni dietro interfacce** (`RoutingService`, `ElevationService`, `WeatherService`, `GeocodingService`): si possono sostituire e simulare nei test.
5. **Nessuna chiave nel repository.** Le chiavi facoltative le inserisce l'utente e restano in DataStore, fuori dai backup.
6. **Icone proprie** disegnate come vettori (le stesse delle bozze), per non dipendere da librerie di icone.
7. **Caratteri inclusi nell'app** (Fraunces e Archivo, licenza OFL): l'aspetto non dipende dalla rete.
8. **APK release non firmato**: nessuna chiave di firma nel repository né nella CI. L'APK installabile è quello debug.
