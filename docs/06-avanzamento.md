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
| Uscite e dati locali | Da fare |
| Attrezzatura e checklist | Da fare |
| Mappe e GPX | Da fare |
| Meteo | Da fare |
| Suggerimenti | Da fare |
| Backup ed esportazioni | Da fare |
| Verifiche e APK | Da fare |

L'APK attuale è solo uno scheletro: si apre sulle quattro schede, che mostrano un testo provvisorio. Nessuna funzione dell'app è ancora utilizzabile. Sono già scritte e testate (15 test) le formule di durata, dislivello con soglia e pesi dello zaino.

## Problemi aperti

1. **Lingua**: l'app segue ancora la lingua del telefono (in inglese sull'emulatore); l'italiano predefinito non è ancora imposto.
2. **BRouter non pubblica condizioni d'uso**: il routing predefinito dipende da un servizio di volontari senza limiti dichiarati.
3. **Condizioni di Mapterhorn e quote di openrouteservice non lette**: da verificare prima di abilitarli nell'APK.
4. **Curve di livello**: disponibili solo nello strato OpenTopoMap, online. Lo strato escursionistico vettoriale non le ha.
5. **Dati climatici del periodo**: fonte da provare prima di mostrarli.
6. **Open-Meteo è gratuito solo per uso non commerciale.**
