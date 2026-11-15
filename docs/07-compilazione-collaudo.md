# Compilazione e collaudo

## Requisiti

JDK 17 o successivo e Android SDK con piattaforma 37 e build-tools 36. Il percorso dell'SDK va in `local.properties` (`sdk.dir=...`, file non versionato) o nella variabile `ANDROID_HOME`.

## Comandi

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew assembleRelease
```

Ogni compilazione produce un APK per architettura (`arm64-v8a`, `armeabi-v7a`, `x86_64`): le librerie della mappa pesano, e ogni telefono ne usa una sola. Quasi tutti i telefoni usano `arm64-v8a`.

- `app/build/outputs/apk/debug/app-arm64-v8a-debug.apk` — **debug**: firmato con la chiave di debug della macchina, installabile, identificativo `app.betterout.debug`.
- `app/build/outputs/apk/release/app-arm64-v8a-release-unsigned.apk` — **release non firmato**: è quello che esce senza la chiave di firma, che non sta nel repository.

## Installare l'APK debug

Con il telefono collegato e il debug USB attivo:

```bash
adb install -r app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
```

## Rilasci firmati

Un tag `vX.Y.Z` su `main` avvia il flusso `Rilascio` (`.github/workflows/release.yml`): test, lint, APK release firmati e una Release su GitHub con i file `BetterOut-X.Y.Z-<architettura>.apk`.

```bash
git tag v0.2.0 && git push origin v0.2.0
```

- La versione dell'app viene dal tag: `0.2.0` diventa `versionName` 0.2.0 e `versionCode` 200 (maggiore × 10 000 + minore × 100 + correzione). Ogni rilascio deve avere un numero più alto del precedente, altrimenti il telefono non lo vede come aggiornamento.
- La chiave di firma sta nei secrets del repository (`BETTEROUT_KEYSTORE_BASE64`, `BETTEROUT_KEYSTORE_PASSWORD`) e in copia fuori dal repository, sulla macchina di sviluppo. Va conservata: senza quella chiave un aggiornamento non si installa sopra le versioni già distribuite. Impronta SHA-256 del certificato: `5B:DA:8F:77:AA:E7:89:18:E1:CB:E2:20:B1:2C:E5:62:E2:DF:54:41:A5:AA:6A:47:FC:24:1C:C3:1F:87:E7:6B`.
- In Obtainium: aggiungere `https://github.com/Mancio24x/BetterOut`; se chiede quale file, scegliere `arm64-v8a`.
- L'APK dei rilasci ha identificativo `app.betterout` e convive con quello di debug (`app.betterout.debug`).
- Finché lo schema del database è provvisorio, un aggiornamento che lo cambia cancella i dati locali (vedi `06-avanzamento.md`).

## GitHub Actions

Il flusso `Android` (`.github/workflows/android.yml`) esegue test, lint e compilazione a ogni push su `main` che tocca il codice, e pubblica fra gli artifact `BetterOut-debug-apk`, `BetterOut-release-NON-FIRMATO` e `rapporti`. Gli artifact si scaricano dalla pagina dell'esecuzione, in fondo.

## Cosa è verificato oggi

| Verifica | Esito | Come |
|---|---|---|
| Compilazione debug e release, lint | Riuscite il 15 nov 2026 | In locale e su GitHub Actions |
| Test delle regole (durata, dislivello, pesi, uscite, checklist, CSV, GPX, statistiche dei percorsi, operazioni dell'editor, risposte dei servizi, regole e cache del meteo) | 105 su 105 | `./gradlew testDebugUnitTest` |
| Installazione e avvio | Riusciti, nessun crash | Emulatore Pixel 7, Android 16 (API 36) |
| Creazione di un'uscita con solo nome e località | Riuscita | Percorso guidato sull'emulatore con `adb` |
| Persistenza dopo chiusura forzata e riavvio | Riuscita | `am force-stop`, riavvio, l'uscita è ancora in Home |
| Duplicazione indipendente | Riuscita | Copia rinominata e con data: l'originale resta invariato |
| Data aggiunta a una bozza → proposta "Programmata" | Riuscita dopo una correzione | Difetto trovato in questa prova e corretto |
| Avviso di modifiche non salvate | Riuscito | Tasto indietro con modifiche: tre scelte |
| Conferma di eliminazione, elenco, filtri, filtro vuoto | Riusciti | Immagini in `docs/verifiche/` |
| Inventario: creazione, modifica, eliminazione | Riuscite | Percorso guidato sull'emulatore |
| Checklist: aggiunta dall'inventario senza doppioni, spunte, quantità, tipo per uscita | Riuscite | Pesi controllati: 626 g base + 1 L d'acqua = 1,63 kg nello zaino |
| Spunte indipendenti fra uscita e copia | Riuscite | Copia con spunte azzerate; spuntare la copia non cambia l'originale |
| Modifica di un oggetto → checklist delle uscite aperte aggiornata | Riuscita | Peso da 86 a 95 g, riga da 2 pezzi a 190 g |
| Eliminazione di un oggetto → riga conservata "non più in inventario" | Riuscita | Dialogo con il numero di uscite coinvolte |
| Azzeramento delle spunte, esportazione CSV e foglio di condivisione | Riusciti | Contenuto del CSV letto dal dispositivo |
| Pulsante "+" letto dai servizi di accessibilità | Riuscito dopo una correzione | Il pulsante flottante standard non compariva nell'albero di accessibilità: sostituito |
| Mappa nella scheda Mappa | Riuscita | MapLibre 13.7.1 disegna anche con la grafica software dell'emulatore; attribuzione visibile |
| Mappa senza rete | Riuscita dopo una correzione | Senza rete e senza copia: avviso e percorsi comunque disegnati; "tocca per riprovare" con la rete tornata; senza rete ma con copia locale: sfondo presente. La libreria da sola non segnalava l'errore |
| Ricerca di una zona | Riuscita | "Misurina" trovata; "Falzarego" senza risultati: stato dichiarato |
| Importazione GPX con quote | Riuscita | 200 punti: 1,9 km, 648 m di salita, profilo |
| GPX con 2 tracciati, 3 segmenti, punti non validi, quote assenti in un tracciato | Riuscita | Avvisi mostrati, 2 punti scartati contati, scelta dei tracciati, durata non stimata con quote parziali |
| GPX troncato, file non GPX, traccia vuota | Riusciti | Messaggio specifico e "Non è stato salvato nulla" |
| GPX da 7,6 MB e 120 000 punti | Riuscita | Letto in pochi secondi, salvato e aperto; la schermata di avanzamento è durata troppo poco per essere osservata |
| File originale conservato | Riuscito | Stessa dimensione del file scelto, nello spazio privato dell'app |
| Editor: tratto su sentiero | Riuscito | Richiesta vera a BRouter: 923 m, 82 m di salita |
| Editor: tratto manuale, inserisci, elimina, annulla, ripristina, inverti, anello | Riusciti | Tratto manuale tratteggiato e segnalato |
| Editor: trascinamento e "Sposta" con tocco | Riusciti | Trascinamento simulato con `adb`; i tratti vicini vengono ricalcolati |
| Editor: routing senza rete | Riuscito | Errore con tre scelte; nessuna linea tracciata; "Traccia a mano" e "Annulla" provati |
| Salvataggio, riapertura in modifica, uscita senza salvare | Riusciti | Conferma se ci sono modifiche |
| "Chiedi le quote" | Riuscito | Richiesta vera: quote completate, dichiarate come stime, durata disponibile |
| Esportazione GPX | Riuscita | File letto dal dispositivo: XML valido, 267 punti, nota sui tratti manuali |
| Rinomina ed eliminazione di un percorso | Riuscite | File rimossi; l'uscita che lo usava resta senza percorso |
| Percorso associato all'uscita da archivio, da GPX e dal disegno; "Togli" | Riusciti | Statistiche e durata nel dettaglio uscita |
| Località dell'uscita: ricerca e punto sulla mappa | Riuscite dopo una correzione | Il foglio di ricerca non compariva con la tastiera aperta |
| Persistenza dei percorsi dopo chiusura forzata | Riuscita | `am force-stop`, riavvio |
| Meteo nel dettaglio dell'uscita | Riuscito | Richiesta vera a Open-Meteo: previsione del primo giorno per punto, con quota, fonte ed età del dato (`docs/verifiche/meteo-uscita.png`) |
| Schermata Meteo: Giorni e Ore | Riuscite | Condizioni, temperature, percepita, precipitazioni, neve, vento e raffiche, zero termico, alba e tramonto; 24 ore; vento per fascia; rientro stimato e margine di luce |
| Due punti in una sola richiesta | Riuscito | Percorso Lagazuoi: partenza 2105 m e quota massima 2755 m, temperature diverse; un percorso con 82 m di salita chiede solo la partenza |
| Meteo senza rete con dato salvato | Riuscito | Avviso con l'età del dato e "Riprova"; il dato resta in vista |
| Meteo senza rete e senza dato | Riuscito | Messaggio e "Riprova"; con la rete tornata "Riprova" carica le previsioni; l'uscita si apre e si modifica comunque |
| Data oltre l'orizzonte | Riuscita | "Previsioni non ancora disponibili" con il primo giorno utile; nessuna richiesta di previsione |
| Clima del periodo | Riuscito | Richiesta vera all'archivio storico: medie di 5 anni alla quota del punto (1763 m e 2755 m), sezione dichiarata "non è una previsione" |
| Uscita di più giorni a cavallo dell'orizzonte | Riuscita | 29 nov – 2 dic: previsti 29 e 30, avviso per i giorni successivi; scelta del giorno nella vista oraria |
| Uscita senza data, senza località, già passata | Riuscite | Ogni caso dice che cosa manca; nessuna richiesta parte |
| Eliminazione dell'uscita → dati meteo rimossi | Riuscita | File della cache controllati sul dispositivo |
| Aggiornamento automatico dopo tre ore | **Non osservato sul dispositivo** | Coperto solo dai test (orologio simulato) |
| Meteo: probabilità di precipitazione oltre i primi giorni | Osservato | Per i giorni lontani il servizio non la dà: la cella mostra "—", non zero |
| APK release firmato, per architettura | Riuscito | Compilato in locale con la chiave, firma controllata con `apksigner`, installato e avviato sull'emulatore: versione 0.2.0 (200), Home, mappa, nuova uscita. 24,6 MB per arm64 (prima 66 MB per tutte) |
| Aggiornamento sopra una versione precedente | **Non eseguito** | Esiste un solo rilascio |
| Build release ottimizzata (R8) | Riuscita | Provata sull'emulatore: ricerca località, importazione GPX, mappa, meteo, salvataggi: nessun errore. 16,4 MB per arm64 |
| Home vuota e pulsante "+" | Riuscita | Dopo il salvataggio l'itinerario compare in Home con mappa, percorso e meteo (`docs/verifiche/home-itinerario.png`) |
| Percorso aggiunto dalla Home | Riuscito dopo una correzione | Dopo "Associa all'uscita" non si tornava indietro: ora si torna alla Home |
| Le tue uscite a pillole | Riuscite | Quota massima, temperatura e distanza; la pillola aperta mostra mappa, meteo ed esportazione GPX |
| Meteo salvato di un'uscita passata | Riuscito | Orologio dell'emulatore spostato avanti di tre giorni: la previsione salvata resta, dichiarata come non misurata; la Home torna vuota |
| Tema nero AMOLED | Riuscito | Scelta salvata e applicata subito (`docs/verifiche/uscite-pillola-aperta-nero.png`) |
| Attrezzatura con simboli e oggetti proposti | Riuscita | "Zaino" aggiunto dal modello con il peso; sparisce dai proposti (`docs/verifiche/attrezzatura-simboli.png`) |
| Fluidità sul telefono | **Non misurata** | L'emulatore non è indicativo: va giudicata sul telefono |
| Tema chiaro forzato, esportazione GPX dalla pillola, importazione GPX dalle impostazioni | **Non eseguiti** | Usano codice già provato altrove, ma questi punti di accesso non sono stati toccati |
| Testo ingrandito, TalkBack | **Non eseguiti** | — |
| Avvio su telefono reale | **Non eseguito** | Nessun dispositivo collegato |

## Emulatore

```bash
$ANDROID_HOME/emulator/emulator -avd betterout -no-window -no-audio
```

Il dispositivo virtuale `betterout` (Pixel 7, API 36) esiste solo sulla macchina di sviluppo. Per le prove la lingua dell'emulatore è stata impostata sull'italiano.
