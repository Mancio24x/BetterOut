# Bozze grafiche

- `direzioni/` — le tre direzioni visive iniziali (A Carta, B Rifugio, C Segnavia) su Home e dettaglio uscita, in chiaro e scuro. Scelta: **B**.
- `bozze/` — tutte le schermate e i loro stati nella direzione scelta. Indice: [`INDICE.md`](INDICE.md); galleria: `index.html`.
- `src/` e `tools/` — sorgenti: ogni bozza è una pagina HTML/CSS trasformata in PNG.
- `fonts/` — Archivo, Fraunces, JetBrains Mono (SIL Open Font License 1.1, testi inclusi).

I dati mostrati sono d'esempio. Le bozze guidano lo sviluppo: l'aspetto definitivo è quello dell'app.

## Rigenerare

Servono Node 20+ e Google Chrome.

```bash
node design/tools/build.mjs direzioni
node design/tools/build.mjs bozze
```

Per rigenerare solo alcune schermate si aggiunge un filtro: `node design/tools/build.mjs bozze 07`.
