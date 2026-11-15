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
| Uscite e dati locali | Fatta in parte: database, Home, elenco con filtri, creazione, modifica, stati, duplicazione, eliminazione. Mancano tappe, ricerca della località e i passi percorso, meteo e zaino del flusso "+" |
| Attrezzatura e checklist | Fatta: inventario con ricerca, filtri e categorie proprie, scheda oggetto, checklist per uscita con quantità, tipo, note, spunte, pesi, acqua, azzeramento ed esportazione CSV. Manca il catalogo iniziale facoltativo |
| Mappe e GPX | Da fare |
| Meteo | Da fare |
| Suggerimenti | Da fare |
| Backup ed esportazioni | Da fare |
| Verifiche e APK | Da fare |

L'APK attuale gestisce uscite, inventario e checklist dello zaino, tutto salvato sul telefono. Mappa e impostazioni mostrano ancora un testo provvisorio. Creazione e modifica dell'uscita usano un'unica pagina, non i sei passi delle bozze. La località è un nome libero, senza coordinate. L'interfaccia è solo in italiano e i pesi sono mostrati in chilogrammi (grammi sotto il chilo): la scelta dell'unità arriva con le impostazioni.

## Problemi aperti

1. **Lingua**: per ora esiste solo l'italiano; la scelta della lingua e l'inglese non sono ancora implementati.
7. **Schema del database provvisorio**: finché non c'è una versione pubblicata, un cambio di schema cancella i dati locali invece di migrarli.
2. **BRouter non pubblica condizioni d'uso**: il routing predefinito dipende da un servizio di volontari senza limiti dichiarati.
3. **Condizioni di Mapterhorn e quote di openrouteservice non lette**: da verificare prima di abilitarli nell'APK.
4. **Curve di livello**: disponibili solo nello strato OpenTopoMap, online. Lo strato escursionistico vettoriale non le ha.
5. **Dati climatici del periodo**: fonte da provare prima di mostrarli.
6. **Open-Meteo è gratuito solo per uso non commerciale.**
