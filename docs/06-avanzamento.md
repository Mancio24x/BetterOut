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
| Attrezzatura e checklist | Da fare |
| Mappe e GPX | Da fare |
| Meteo | Da fare |
| Suggerimenti | Da fare |
| Backup ed esportazioni | Da fare |
| Verifiche e APK | Da fare |

L'APK attuale gestisce le uscite (creare, modificare, duplicare, cambiare stato, eliminare) e le conserva sul telefono. Mappa, attrezzatura e impostazioni mostrano ancora un testo provvisorio. Creazione e modifica usano per ora un'unica pagina, non i sei passi delle bozze: i passi arrivano insieme a percorso, meteo e zaino. La località è un nome libero, senza coordinate. L'interfaccia è solo in italiano: l'inglese torna con le impostazioni.

## Problemi aperti

1. **Lingua**: per ora esiste solo l'italiano; la scelta della lingua e l'inglese non sono ancora implementati.
7. **Schema del database provvisorio**: finché non c'è una versione pubblicata, un cambio di schema cancella i dati locali invece di migrarli.
2. **BRouter non pubblica condizioni d'uso**: il routing predefinito dipende da un servizio di volontari senza limiti dichiarati.
3. **Condizioni di Mapterhorn e quote di openrouteservice non lette**: da verificare prima di abilitarli nell'APK.
4. **Curve di livello**: disponibili solo nello strato OpenTopoMap, online. Lo strato escursionistico vettoriale non le ha.
5. **Dati climatici del periodo**: fonte da provare prima di mostrarli.
6. **Open-Meteo è gratuito solo per uso non commerciale.**
