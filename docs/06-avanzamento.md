# Avanzamento e problemi aperti

Aggiornato il 15 novembre 2026.

| Fase | Stato |
|---|---|
| Analisi del repository e del riferimento | Fatta |
| Verifica dei servizi esterni | Fatta, con i limiti indicati in `05-servizi-esterni.md` |
| Specifica funzionale | Fatta (`01-specifica-funzionale.md`) |
| Tre direzioni visive | Fatte; scelta B · Rifugio |
| Bozze di tutte le schermate | Fatte: 58 immagini (`design/INDICE.md`) |
| Struttura Android e CI | Da fare |
| Uscite e dati locali | Da fare |
| Attrezzatura e checklist | Da fare |
| Mappe e GPX | Da fare |
| Meteo | Da fare |
| Suggerimenti | Da fare |
| Backup ed esportazioni | Da fare |
| Verifiche e APK | Da fare |

Non esiste ancora codice Android né un APK: finora ci sono documenti e bozze.

## Problemi aperti

1. **Nessun emulatore né dispositivo collegato** sulla macchina di sviluppo: mancano l'emulatore Android e le immagini di sistema. Finché non ci sono, l'avvio dell'app non può essere verificato qui; restano possibili compilazione, test sulla JVM e immagini delle schermate generate dai test.
2. **BRouter non pubblica condizioni d'uso**: il routing predefinito dipende da un servizio di volontari senza limiti dichiarati.
3. **Condizioni di Mapterhorn e quote di openrouteservice non lette**: da verificare prima di abilitarli nell'APK.
4. **Curve di livello**: disponibili solo nello strato OpenTopoMap, online. Lo strato escursionistico vettoriale non le ha.
5. **Dati climatici del periodo**: fonte da provare prima di mostrarli.
6. **Open-Meteo è gratuito solo per uso non commerciale.**
