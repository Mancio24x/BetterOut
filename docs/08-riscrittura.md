# Riscrittura dell'interfaccia

Decisa il 16 novembre 2026 dopo la versione 0.6.0: l'utente trova l'app a scatti, povera di funzioni e brutta. Si rifà **tutta l'interfaccia**; restano database (con le migrazioni: i dati salvati non si perdono), formule e lettori di file (GPX, FIT, UDDF, Subsurface, meteo, ricerca luoghi), che sono testati. Prima la fluidità, poi le funzioni nuove, ognuna in una release.

Telefono dell'utente: **Pixel 9**, schermo a 120 Hz (un fotogramma deve stare in 8,3 ms), app installata da **Obtainium** (nessun profilo di compilazione dal Play Store).

## Misure di partenza (0.6.0, emulatore Pixel con GPU del Mac, 60 Hz)

| Situazione | Fotogrammi in ritardo | Lavoro dell'interfaccia per fotogramma (mediana / 90%) |
|---|---|---|
| Impostazioni di Android (riferimento) | 2,5% | 0,6 / 1,3 ms |
| Home con mappa, scorrimento | 3,7% | 0,8 / 1,5 ms |
| Attrezzatura, scorrimento | 3,2% | 0,8 / 1,9 ms |
| Cambio di schede | 3,9% | 0,8 / 2,2 ms |
| Avvio a freddo | — | 0,25–0,4 s |

Il disegno dell'interfaccia non è il collo di bottiglia. Con l'emulatore senza GPU (`-gpu swangle_indirect`) tutto scatta, anche le app di sistema: le misure vanno fatte con `-gpu host`.

Cause probabili degli scatti percepiti, da eliminare:
1. **MapLibre dentro le pagine che scorrono** (Home, pillole aperte, dettaglio): creazione della mappa, tessere che arrivano, superficie GL da comporre. → Nelle pagine: anteprima statica del percorso (immagine generata una volta e salvata). La mappa vera solo a schermo intero.
2. **Contenuti che arrivano dopo e spostano la pagina** (meteo, temperature, mappa). → Spazi riservati con altezza fissa e segnaposto; dati preparati nei ViewModel prima di mostrare la pagina.
3. **Codice non compilato dopo ogni aggiornamento** (Obtainium). → Profilo di base dell'app (Baseline Profile) generato sull'emulatore e incluso nell'APK.

## Come si misura e si verifica

- Emulatore `betterout` (Android 16, 1080×2400) avviato con `emulator -avd betterout -no-window -no-audio -gpu host`.
- APK release firmato con la chiave di debug per le prove locali; per non perdere i dati di prova dell'emulatore si installa sopra l'APK ufficiale della release.
- Fotogrammi: `adb shell dumpsys gfxinfo <pacchetto> framestats`, lavoro per fotogramma da `HandleInputStart` a `SyncStart` (interfaccia) e a `FrameCompleted` (totale).
- Ogni schermata nuova si guarda con uno screenshot (`adb exec-out screencap -p`) prima di pubblicarla.

## Tappe

1. **Base**: profilo di base generato; tema e componenti Material 3 rivisti (intestazioni che si comprimono scorrendo, tasto modalità meno invadente, spaziature); anteprime statiche al posto delle mappe nelle pagine.
2. **Schermate escursioni** rifatte: Home, uscite, dettaglio, modifica, attrezzatura, checklist, meteo, percorsi.
3. **Schermate immersioni** rifatte con lo stesso sistema.
4. **Registrazione GPS** dell'uscita: servizio in primo piano, traccia live con distanza, dislivello, tempo e pause, salvata come percorso.
5. **Navigazione del percorso**: posizione sulla mappa lungo il percorso, distanza mancante, avviso fuori percorso.
6. **Immersioni complete**: siti, attrezzatura sub con checklist, pianificazione, calcolo nitrox (profondità massima operativa, miscela migliore), statistiche.

## Avanzamento

- 0.6.1 — profilo di base nell'APK (1131 voci del codice di BetterOut, 1546 della mappa).
- 0.6.2 — anteprima statica del percorso al posto della mappa viva nelle pagine; ricerca dei luoghi: risultati in mezzo secondo invece di circa 8 (nome normalizzato nel file, caricamento all'avvio).
- 0.7.0 — nuova Home (scheda piena della prossima uscita, anteprima, azioni rapide, altre uscite) e nuovo elenco delle uscite a sezioni; barra Material 3 che si comprime.
- 0.7.1 — attrezzatura e logbook con la stessa barra; sfondo del tema anche sotto la barra di stato.

- 0.7.2 — dettaglio dell'uscita con il nome nella barra grande e "Prepara lo zaino" fisso in basso.
- 0.8.0 — immersioni: barra in basso con Logbook, Siti e Strumenti; siti ricavati dal logbook con le loro immersioni; calcolo nitrox (profondità massima operativa, miscela migliore, profondità equivalente in aria, ppO₂); statistiche nei totali; dettaglio con il sito nella barra grande. Provato sull'emulatore con le 29 immersioni d'esempio di Subsurface.
- 0.8.1 — nitrox: limiti di ppO₂ anche a 1,2 e 1,3.
- 0.9.0 — attrezzatura sub (scheda dedicata nella modalità immersioni): kit con simboli e spunte di checklist, avanzamento, azzeramento delle spunte; pannello "Da aggiungere" con la griglia dei pezzi tipici e "Altro pezzo". Database alla versione 6 con migrazione (tabella `dive_gear`).

Da rifare: modifica dell'uscita, checklist, meteo, percorsi, schermate delle immersioni oltre al logbook.
