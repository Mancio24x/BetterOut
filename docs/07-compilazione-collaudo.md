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
| Test delle regole (durata, dislivello, pesi, uscite, checklist, CSV, GPX, statistiche dei percorsi, operazioni dell'editor, risposte dei servizi) | 74 su 74 | `./gradlew testDebugUnitTest` |
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
| Tema scuro, testo ingrandito, TalkBack | **Non eseguiti** | — |
| Avvio su telefono reale | **Non eseguito** | Nessun dispositivo collegato |

## Emulatore

```bash
$ANDROID_HOME/emulator/emulator -avd betterout -no-window -no-audio
```

Il dispositivo virtuale `betterout` (Pixel 7, API 36) esiste solo sulla macchina di sviluppo. Per le prove la lingua dell'emulatore è stata impostata sull'italiano.
