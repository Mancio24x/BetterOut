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
| Meteo | Da fare |
| Suggerimenti | Da fare |
| Backup ed esportazioni | Da fare |
| Verifiche e APK | Da fare |

L'APK attuale gestisce uscite, inventario, checklist dello zaino e percorsi, tutto salvato sul telefono. Le impostazioni mostrano ancora un testo provvisorio. Creazione e modifica dell'uscita usano un'unica pagina, non i sei passi delle bozze. L'interfaccia è solo in italiano e i pesi sono mostrati in chilogrammi (grammi sotto il chilo): la scelta dell'unità arriva con le impostazioni.

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

## Problemi aperti

8. **Peso dell'APK**: con le librerie native della mappa per quattro architetture l'APK debug è passato da 14 a 67 MB. Si può ridurre con un APK per architettura.
9. **Lista dei punti nell'editor**: selezione e azioni hanno i pulsanti, ma aggiungere o spostare un punto richiede comunque un tocco sulla mappa; con TalkBack non è stato provato.
10. **Il file GPX originale** è conservato ma non ancora usato: servirà al backup.

1. **Lingua**: per ora esiste solo l'italiano; la scelta della lingua e l'inglese non sono ancora implementati.
7. **Schema del database provvisorio**: finché non c'è una versione pubblicata, un cambio di schema cancella i dati locali invece di migrarli.
2. **BRouter non pubblica condizioni d'uso**: il routing predefinito dipende da un servizio di volontari senza limiti dichiarati.
3. **Condizioni di Mapterhorn e quote di openrouteservice non lette**: da verificare prima di abilitarli nell'APK.
4. **Curve di livello**: disponibili solo nello strato OpenTopoMap, online. Lo strato escursionistico vettoriale non le ha.
5. **Dati climatici del periodo**: fonte da provare prima di mostrarli.
6. **Open-Meteo è gratuito solo per uso non commerciale.**
