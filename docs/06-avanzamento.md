# Avanzamento e problemi aperti

Aggiornato il 15 novembre 2026.

| Fase | Stato |
|---|---|
| Analisi del repository e del riferimento | Fatta |
| Verifica dei servizi esterni | Fatta, con i limiti indicati in `05-servizi-esterni.md` |
| Specifica funzionale | Fatta (`01-specifica-funzionale.md`) |
| Tre direzioni visive | Fatte; scelta B · Rifugio |
| Bozze di tutte le schermate | Fatte: 58 immagini (`design/INDICE.md`) |
| Struttura Android e CI | Fatta: progetto compilabile, tema, navigazione a quattro schede, flusso GitHub Actions |
| Uscite e dati locali | Fatta in parte: database, Home, elenco con filtri, creazione, modifica, stati, duplicazione, eliminazione, località con ricerca o punto sulla mappa. Mancano le tappe e i sei passi del flusso "+" |
| Attrezzatura e checklist | Fatta: inventario con ricerca, filtri e categorie proprie, scheda oggetto, checklist per uscita con quantità, tipo, note, spunte, pesi, acqua, azzeramento ed esportazione CSV. Manca il catalogo iniziale facoltativo |
| Mappe e GPX | Fatta in parte: mappa, archivio percorsi, editor con routing per tratto, importazione ed esportazione GPX, dettaglio con profilo, quote dal modello del terreno, percorso associato all'uscita. Mancano strati alternativi, mappe offline, posizione dell'utente e modifica dei percorsi importati (vedi sotto) |
| Meteo | Fatta in parte: previsioni per partenza e punto più alto, vista giornaliera e oraria, cache con età del dato, clima del periodo. Mancano il punto del pernottamento e l'aggiornamento in background (vedi sotto) |
| Suggerimenti | Da fare |
| Backup ed esportazioni | Da fare |
| Verifiche e APK | Fatta in parte: rilasci firmati su GitHub a ogni tag, un APK per architettura. Mancano le prove su telefono reale |

L'APK attuale gestisce uscite, inventario, checklist dello zaino, percorsi e meteo, tutto salvato sul telefono. Le impostazioni mostrano ancora un testo provvisorio. Creazione e modifica dell'uscita usano un'unica pagina, non i sei passi delle bozze. L'interfaccia è solo in italiano e i pesi sono mostrati in chilogrammi (grammi sotto il chilo): la scelta dell'unità arriva con le impostazioni.

## Versione 0.3.0: nuova struttura dell'app

Richiesta dall'utente dopo la prima prova sul telefono, e diversa dalle bozze iniziali (`design/`), che restano come riferimento visivo:

- **Barra in basso a tre tasti**: Uscite · Home (bussola, al centro, schermata iniziale) · Attrezzatura. Le impostazioni si aprono dalla rotellina in alto a destra; mappa e archivio dei percorsi dall'icona della mappa in Home e dalle impostazioni.
- **Home**: vuota con il pulsante tondo "+" finché non c'è un itinerario; poi mostra in grande il prossimo (quello programmato più vicino, altrimenti l'ultima bozza) con mappa e percorso, statistiche, meteo, zaino e dettagli. Dopo il salvataggio di una nuova uscita si torna qui.
- **Le tue uscite**: tutte le uscite una dopo l'altra, a pillole, con nome, quota massima, temperatura e distanza. Toccandone una si apre con mappa, meteo, esportazione GPX, attrezzatura e il collegamento al dettaglio completo. Non ci sono più i filtri per stato.
- **Meteo delle uscite passate**: resta l'ultima previsione salvata, dichiarata come "tempo previsto, non misurato". La temperatura della pillola viene da lì (partenza, primo giorno).
- **Impostazioni**: tema (di sistema, chiaro, nero AMOLED), importazione GPX, archivio dei percorsi, versione e fonti.
- **Attrezzatura**: elenco con simbolo e nome; sotto gli oggetti già compilati, 17 oggetti proposti (zaino, scarpe, borraccia…) che si aggiungono con un tocco inserendo peso e volume. Tolti ricerca e filtri per categoria dall'elenco.
- **Fluidità**: la build dei rilasci è ora ottimizzata con R8 (prima non lo era); l'APK arm64 scende a 16 MB.

Non ancora fatto di quanto richiesto: scelta della lingua (serve la traduzione inglese di tutti i testi), esportazione e importazione di tutti i dati (backup), meteo misurato a posteriori per le uscite passate.

## Modalità immersioni (prima tappa)

Il tasto con l'onda, accanto alla rotellina in ogni scheda principale, porta l'app nella modalità immersioni: tema blu (chiaro, scuro e nero AMOLED) e logbook al posto delle uscite. Il tasto con la montagna riporta alle escursioni. L'app riapre nell'ultima modalità usata.

- Logbook compilato a mano: numero progressivo (modificabile), sito con punto sulla mappa, data e ora, durata, profondità massima e media, temperatura dell'acqua, visibilità, gas (aria o nitrox con la percentuale di ossigeno), bombola, pressione all'inizio e alla fine, compagno, note.
- Totali: numero di immersioni, tempo sott'acqua, profondità record. Nel dettaglio il consumo in superficie (SAC), solo con tutti i dati necessari.
- Database alla versione 5: la tabella `dives` arriva con una migrazione, le uscite già salvate restano. Un test controlla che la migrazione coincida con lo schema generato da Room.

Prossime tappe: siti d'immersione, attrezzatura sub con checklist, immersioni pianificate, calcolo nitrox (profondità massima operativa, miscela migliore), importazione dal computer subacqueo.

## Mappe e GPX: cosa c'è e cosa manca

Fatto e provato sull'emulatore (dettaglio in `07-compilazione-collaudo.md`):

- Scheda Mappa con sfondo OpenFreeMap, sentieri messi in evidenza, attribuzione sempre visibile, ricerca di una zona, percorsi salvati disegnati, archivio in elenco.
- Editor: punti con un tocco, trascinamento, "Sposta", "Inserisci", "Fine qui", "Elimina", annulla e ripristina, inverti, anello, salvataggio e riapertura. Ogni tratto è "Segui sentieri" (BRouter, una richiesta per tratto) o "Manuale" (retta tratteggiata, dichiarata non verificata in mappa, statistiche e GPX). Se il routing non riesce l'errore è mostrato con tre scelte: riprova, traccia a mano, annulla la modifica. Nessuna retta viene messa al posto di un tratto non calcolato.
- Importazione GPX 1.0 e 1.1 dal selettore di Android: tracciati, segmenti separati, itinerari, punti di interesse; anteprima con avvisi; scelta dei tracciati; file originale copiato senza modifiche.
- Dettaglio del percorso: mappa, statistiche, profilo altimetrico, fonte delle quote, rinomina, eliminazione (le uscite restano, senza percorso), esportazione GPX.
- "Chiedi le quote": solo su richiesta, al modello del terreno di Open-Meteo, per i punti che non ne hanno (al più 300, presi a intervalli regolari); il risultato è dichiarato come stima.
- Uscita: riquadro del percorso con statistiche e durata secondo il ritmo dell'uscita; scelta fra disegno, GPX e archivio.

Scelte fatte in questa fase:

- **Durata non stimata se le quote sono incomplete.** Con il dislivello parziale la stima sarebbe troppo bassa: viene mostrato "non disponibile" finché le quote non ci sono tutte.
- **I percorsi importati non si modificano nell'editor**: si possono rinominare, esportare, eliminare. L'editor lavora per punti di controllo, che un tracciato registrato non ha.
- **Lo stile della mappa lo scarica l'app** e ne tiene una copia: senza rete e senza copia lo sfondo manca, viene detto, e i percorsi restano visibili.
- **I punti dei percorsi stanno in file**, non nel database: un tracciato lungo non entra in una riga.

Non ancora fatto:

- Strati alternativi (OpenTopoMap, rilievo ombreggiato) e scaricamento di aree per l'uso offline.
- Posizione dell'utente sulla mappa.
- Punti di interesse del GPX: sono salvati ed esportati, ma non ancora disegnati.
- "Chiedi le quote" direttamente nell'anteprima di importazione (c'è nel dettaglio, dopo il salvataggio).
- openrouteservice con chiave dell'utente; scelta del fornitore di routing.
- Tipo del luogo e località recenti nella ricerca.

## Meteo: cosa c'è e cosa manca

Fatto e provato sull'emulatore con richieste vere (dettaglio in `07-compilazione-collaudo.md`):

- Riquadro "Meteo" nel dettaglio dell'uscita: il primo giorno, per ogni punto, con quota, fonte ed età del dato.
- Schermata Meteo: "Giorni" (condizioni, minima e massima, percepita, precipitazioni, neve, vento, raffiche e direzione, zero termico, alba e tramonto, giorni vicini) e "Ore" (24 ore, vento di mattina e pomeriggio, tramonto). Per un'uscita di un giorno con ora di partenza e durata stimata: rientro stimato e margine di luce.
- Una sola richiesta per tutti i punti. Con un percorso: la partenza e, se è almeno 100 m più in alto, il punto più alto, ciascuno con la sua quota. Senza percorso: la località, con la quota stimata dal servizio e dichiarata come stima.
- Cache su file per uscita, con data e ora. Richiesta all'apertura dell'uscita se il dato manca o ha più di tre ore, e con il pulsante di aggiornamento. Se la richiesta non riesce resta l'ultimo dato con la sua età.
- Data oltre l'orizzonte: "Previsioni non ancora disponibili" con il primo giorno utile, e la sezione "Clima del periodo — non è una previsione".
- Senza data, senza località né percorso, uscita passata: il riquadro dice che cosa manca.

Scelte fatte in questa fase:

- **I dati meteo stanno in file** (`filesDir/meteo/`), non nel database: lo schema non cambia e i dati locali non vengono cancellati.
- **Orizzonte calcolato dall'app**: oggi + 15 giorni nel fuso della destinazione, come provato sul servizio; oltre non parte nessuna richiesta di previsione.
- **Un dato salvato per altri punti o altre date non viene mostrato**: cambiando data, località o percorso si chiede di nuovo.
- **Clima del periodo**: gli stessi giorni dell'uscita, con tre giorni di margine per parte, nei cinque anni precedenti; media delle minime, media delle massime e giorni con almeno 1 mm. Chiesto per il punto più alto, una volta sola.
- **Errore di rete**: l'app non distingue fra rete assente e servizio che non risponde, e lo dice così.
- **Quota massima solo se la partenza ha la quota**: la richiesta porta le quote di tutti i punti o di nessuno.

Non ancora fatto:

- Previsione per il luogo del pernottamento: le uscite non hanno ancora le tappe, quindi quel luogo non esiste.
- Aggiornamento in background (la specifica lo indica come eventuale).
- Riga del meteo nella scheda della prossima uscita in Home.
- Il meteo non entra ancora nei suggerimenti: arriva con la fase successiva.

## Problemi aperti

8. **Peso dell'APK**: risolto con un APK per architettura (24,6 MB per arm64 invece di 66 MB).
9. **Lista dei punti nell'editor**: selezione e azioni hanno i pulsanti, ma aggiungere o spostare un punto richiede comunque un tocco sulla mappa; con TalkBack non è stato provato.
10. **Il file GPX originale** è conservato ma non ancora usato: servirà al backup.

1. **Lingua**: per ora esiste solo l'italiano; la scelta della lingua e l'inglese non sono ancora implementati.
7. **Schema del database provvisorio**: finché non c'è una versione pubblicata, un cambio di schema cancella i dati locali invece di migrarli.
2. **BRouter non pubblica condizioni d'uso**: il routing predefinito dipende da un servizio di volontari senza limiti dichiarati.
3. **Condizioni di Mapterhorn e quote di openrouteservice non lette**: da verificare prima di abilitarli nell'APK.
4. **Curve di livello**: disponibili solo nello strato OpenTopoMap, online. Lo strato escursionistico vettoriale non le ha.
5. **Dati climatici del periodo**: fonte provata (archivio storico di Open-Meteo) e in uso; le sue condizioni specifiche non sono state lette oltre a quelle generali di Open-Meteo.
11. **Codice del tempo giornaliero**: il servizio dà per il giorno il fenomeno più intenso fra le ore; può comparire "Neve debole" con 0 cm previsti. È mostrato com'è.
6. **Open-Meteo è gratuito solo per uso non commerciale.**
