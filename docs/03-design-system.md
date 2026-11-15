# Design system

Direzione scelta il 15 novembre 2026: **B · Rifugio**, fra tre proposte (`design/direzioni/`). Toni caldi, schede morbide, titoli con grazie, verde muschio e ocra. Rispetto alla proposta iniziale, le bozze complete rinforzano il contrasto per l'uso all'aperto: bordi pieni sugli stati e sui controlli, testo secondario più scuro, stati sempre accompagnati da icona e parola.

L'identità è originale: nome provvisorio "BetterOut", marchio provvisorio a tre curve di livello annidate.

## Colori

| Ruolo | Chiaro | Scuro | Uso |
|---|---|---|---|
| Sfondo | `#F3ECDF` | `#191713` | Fondo delle schermate |
| Superficie | `#FFFCF5` | `#25221C` | Schede, campi, righe |
| Superficie 2 | `#EFE7D8` | `#322E26` | Riquadri interni, tracce delle barre |
| Testo | `#2A2620` | `#F1EADC` | Testo principale (12,8:1 sullo sfondo chiaro, 15,0:1 nello scuro) |
| Testo secondario | `#6A6155` | `#B6AC9B` | Dettagli (5,2:1 sullo sfondo chiaro, 8,0:1 nello scuro) |
| Primario | `#3F5F3A` | `#A9CB98` | Azione principale, selezioni, avanzamento |
| Su primario | `#FFFCF5` | `#172413` | Testo sul primario (7,0:1) |
| Ocra | `#B9741A` | `#E3A84A` | Pulsante "+", accenti. Con il "+" chiaro dà 3,7:1: sufficiente per un'icona grande, non per il testo, quindi sull'ocra non si scrive testo piccolo |
| Errore | `#A3271B` | `#FF9C8F` | Errori e azioni distruttive |
| Avviso (fondo/testo) | `#F6E2B8` / `#4A3305` | `#4A3A14` / `#F6E2B8` | Avvisi |
| Percorso sulla mappa | `#B9541A` | `#F0A35E` | Linea del percorso, con bordo chiaro |

I rapporti indicati sono calcolati con la formula WCAG sui colori di questa tabella; vanno ricontrollati sull'app reale con gli strumenti di accessibilità di Android.

## Tipografia

| Stile | Carattere | Misura | Uso |
|---|---|---|---|
| Titolo di schermata | Fraunces 560 | 44 sp | "Le tue uscite" |
| Titolo | Fraunces 600 | 23–31 sp | Nome dell'uscita, titoli di passo e di dialogo |
| Numero | Fraunces 600 | 17–30 sp | Distanza, dislivello, pesi, temperature |
| Corpo | Archivo 400 | 15,5 sp | Testo |
| Corpo forte | Archivo 700 | 15–16,5 sp | Titoli di riga, pulsanti |
| Etichetta | Archivo 500–600 | 12–13,5 sp | Etichette dei campi, didascalie |

Fraunces e Archivo sono distribuiti con licenza SIL Open Font License 1.1 (testi in `design/fonts/`) e inclusi nell'app. Tutte le misure sono in `sp`: seguono la dimensione del testo scelta nel sistema. Le righe non hanno altezza fissa e i numeri affiancati vanno a capo quando il testo è ingrandito.

## Forme e spazi

- Raggi: schede 26 dp, riquadri interni 14–16 dp, campi 14 dp, pulsanti e pillole completamente arrotondati, pulsante "+" 22 dp.
- Margine laterale 20 dp; distanza fra blocchi 12–14 dp; griglia di 4 dp.
- Ombre leggere sulle schede nel tema chiaro; nel tema scuro le schede si distinguono per tono.
- Area toccabile minima 48 dp: pulsanti 56 dp, righe almeno 60 dp, caselle 28 dp dentro un'area di 48 dp.

## Componenti

Scheda, riga d'elenco, campo, selettore segmentato (l'opzione attiva ha anche il segno di spunta), pillola filtro, contatore, interruttore, casella, pulsanti (principale, secondario con bordo, testuale, distruttivo, disattivato tratteggiato), avviso (informazione, attenzione, errore), stato vuoto, indicatore dei passi, barra di avanzamento con testo, foglio dal basso, dialogo, menu, pulsanti flottanti della mappa, barra inferiore.

**Stati dell'uscita**: Bozza (bordo tratteggiato, icona matita), Programmata (bordo pieno, icona calendario), Completata (fondo pieno, icona spunta), Annullata (testo barrato, icona ×).

**Mappa**: percorso su sentiero a linea piena; tratto manuale a linea punteggiata; partenza "P" in cerchio chiaro; arrivo bandierina in cerchio scuro; punto intermedio cerchio vuoto; punto selezionato cerchio pieno e più grande. L'attribuzione cartografica è sempre visibile.

## Accessibilità

- Ogni controllo ha un'etichetta per TalkBack; le icone decorative sono nascoste ai lettori di schermo.
- Stati, errori e selezioni usano sempre forma, icona o testo oltre al colore.
- L'editor della mappa ha alternative ai gesti: il riquadro del punto e un elenco dei punti con sposta, inserisci, elimina.
- Le temperature usano il segno meno tipografico e l'unità è sempre scritta.
