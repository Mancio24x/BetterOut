# BetterOut — specifica funzionale

Stato: bozza di lavoro, aggiornata il 15 novembre 2026. Il nome "BetterOut" è provvisorio.

BetterOut è un'app Android nativa per chi va in montagna a piedi: serve a programmare un'uscita, disegnare o importare il percorso, guardare il meteo e preparare lo zaino. Funziona senza account e conserva tutto sul telefono.

Il riferimento funzionale è l'app web Outsiders (osservata il 15 novembre 2026): quattro sezioni (uscite, preparazione, inventario, traccia), preparazione guidata in tre passi, suggerimenti con motivazione, impostazioni con unità, lingua, backup JSON, CSV e soglia di quota. BetterOut ne riprende le funzioni, non l'aspetto né i testi, e aggiunge mappa con editor del percorso, meteo da previsioni reali e programmazione delle uscite.

## 1. Cosa non è

- Non è una PWA né un sito in una WebView.
- Non ha login, sincronizzazione cloud, pubblicità o analytics.
- Non è uno strumento di navigazione sul campo: non registra tracce e non guida lungo il percorso.
- Non certifica sicurezza o percorribilità: stime e suggerimenti aiutano a prepararsi e sono sempre presentati come tali.

## 2. Navigazione

Barra inferiore con quattro destinazioni:

| Destinazione | Contenuto |
|---|---|
| Uscite | Home: prossima uscita, elenco, filtri, pulsante "+" |
| Mappa | Mappa, archivio percorsi, editor, importazione GPX |
| Attrezzatura | Inventario riutilizzabile |
| Impostazioni | Preferenze, backup, esportazioni, stato dei servizi |

Dall'uscita si raggiungono direttamente dettaglio percorso, meteo, preparazione guidata e checklist. Le schermate di secondo livello non mostrano la barra inferiore e hanno il tasto "indietro".

## 3. Uscite

### 3.1 Dati di un'uscita

Nome (unico campo obbligatorio per salvare), descrizione, note, località con coordinate, data di inizio e di fine, ora di partenza, durata, fuso orario della destinazione, tipo di attività, percorso associato, tappe, pernottamento (nessuno, tenda, rifugio, bivacco), disponibilità d'acqua dichiarata (frequente, scarsa, assente, non indicata), ritmo (lento, medio, veloce), ore di cammino al giorno, difficoltà dichiarata (T, E, EE, EEA, non indicata), litri d'acqua da portare, zaino scelto.

Tipi di attività iniziali: escursione giornaliera, trekking di più giorni, via ferrata, uscita invernale con ciaspole o ramponcini, corsa in montagna, altro.

Il fuso orario è quello della destinazione: lo propone il servizio meteo o di ricerca località, altrimenti vale quello del telefono, e resta modificabile. Date e ore sono salvate come data e ora locali della destinazione più l'identificativo del fuso.

### 3.2 Stati

| Stato | Significato | Come ci si arriva |
|---|---|---|
| Bozza | Informazioni incomplete o uscita non confermata | Stato iniziale; salvataggio senza data |
| Programmata | Ha una data ed è confermata | Scelta dell'utente quando c'è una data |
| Completata | Fatta | Scelta dell'utente |
| Annullata | Non si farà, resta in archivio | Scelta dell'utente |

Lo stato lo decide sempre l'utente; l'app propone "Programmata" quando si salva con una data e segnala le uscite programmate con data passata, senza cambiarle da sola. Un'uscita senza data resta utilizzabile in tutto (percorso, zaino, suggerimenti); manca solo il meteo da previsione. Ogni stato è mostrato con icona e testo, mai con il solo colore.

"Prossima uscita" in Home è l'uscita programmata con la data futura più vicina; se non ce n'è, la bozza modificata più di recente.

### 3.3 Creazione dal pulsante "+"

Flusso a passi, ciascuno facoltativo tranne il nome, con "Salva come bozza" sempre disponibile:

1. Titolo e località (ricerca o punto sulla mappa).
2. Data, ora di partenza, durata; oppure "senza data".
3. Percorso: scegli dall'archivio, disegna sulla mappa, importa GPX, oppure salta.
4. Meteo: anteprima per date e luogo scelti.
5. Zaino: avvia la preparazione guidata oppure salta.
6. Riepilogo e salvataggio.

Uscendo con modifiche non salvate l'app chiede se salvare come bozza, scartare o restare.

### 3.4 Modifica, duplicazione, cancellazione

- Modifica: tutti i campi, in qualsiasi stato.
- Duplicazione: crea una nuova uscita in bozza con dati propri. Copia campi, tappe e righe della checklist; chiede la nuova data (o "senza data") e se azzerare le spunte dello zaino (predefinito: sì). Il percorso è condiviso per riferimento, perché i percorsi vivono nell'archivio e modificarne uno è un'azione esplicita che avvisa quante uscite lo usano. Nulla dell'originale cambia.
- Cancellazione: conferma con il nome dell'uscita. Elimina uscita, tappe, checklist e meteo in cache; non elimina il percorso né gli oggetti dell'inventario.

## 4. Percorsi, mappa e GPX

### 4.1 Archivio

Ogni percorso ha nome, origine (disegnato o importato), geometria, punti di interesse, statistiche e, se importato, il file GPX originale conservato così com'è. Un percorso può essere associato a più uscite. Eliminandolo, le uscite che lo usano restano e perdono solo il collegamento (con avviso preventivo).

### 4.2 Editor

Azioni: cercare una zona, aggiungere partenza, arrivo e punti intermedi toccando la mappa, spostare un punto trascinandolo, inserire un punto su un tratto, eliminare un punto, annullare e ripristinare, invertire il percorso, chiudere ad anello, salvare, riaprire, esportare in GPX.

Due modalità, scelte tratto per tratto:

- **Segui sentieri**: il tratto fra due punti è calcolato da un servizio di routing escursionistico.
- **Manuale**: i due punti sono uniti da una linea retta. Il tratto è disegnato tratteggiato e marcato "non verificato su sentiero" sulla mappa, nelle statistiche e nel GPX esportato.

Se il routing fallisce (rete assente, servizio non disponibile, nessun sentiero fra i punti) l'app mostra l'errore e offre di riprovare, usare la modalità manuale per quel tratto o togliere il punto. Non sostituisce mai da sola un tratto calcolato con una linea retta.

### 4.3 Statistiche

Distanza, dislivello positivo e negativo, quota minima e massima, durata stimata, profilo altimetrico.

- Le quote dei tratti calcolati arrivano dal servizio di routing; quelle dei tratti manuali e dei GPX senza quota si possono richiedere, su azione esplicita, a un modello digitale del terreno e sono etichettate "quote da modello del terreno".
- Se le quote mancano, dislivello, quote e profilo mostrano "non disponibile": nessun valore inventato.
- La distanza è la somma delle distanze fra punti consecutivi (formula dell'emisenoverso), senza scavalcare le interruzioni fra segmenti.

**Dislivello con soglia.** Sommare ogni variazione fra punti consecutivi gonfia il risultato con il rumore del GPS; scartare ogni variazione sotto soglia cancella le salite graduali. L'app usa un'isteresi: tiene una quota di riferimento e, quando la quota corrente se ne discosta di almeno la soglia, somma l'intera differenza (alla salita o alla discesa) e sposta il riferimento. Le salite lente vengono così contate per intero non appena superano la soglia. Soglia iniziale 3 m, modificabile nelle impostazioni; ogni segmento è calcolato a parte.

**Durata stimata.** Metodo escursionistico svizzero (DIN 33466), lo stesso usato dal riferimento:

- tempo orizzontale `to = distanza / 4 km/h`
- tempo verticale `tv = salita / 300 m/h + discesa / 500 m/h`
- durata `= max(to, tv) + min(to, tv) / 2`, poi moltiplicata per il ritmo (lento 1,25 · medio 1,00 · veloce 0,80)

Verifica sul riferimento: 14 km, 900 m in salita e in discesa → to 3,5 h, tv 4,8 h → 6,55 h; il riferimento mostra 6,5 h. Le tre velocità sono modificabili nelle impostazioni. È una stima di cammino senza soste, mostrata come "stimata" e correggibile a mano.

### 4.4 Importazione GPX

Si usa il selettore file di Android (nessun permesso di accesso generale alla memoria). Formati: GPX 1.0 e 1.1, con tracce, segmenti, route e waypoint.

| Caso | Comportamento |
|---|---|
| File non GPX o XML non valido | Errore con spiegazione, nulla viene salvato |
| Punti senza coordinate o con coordinate fuori intervallo | Scartati e contati nell'anteprima |
| Nessun punto valido | Errore "traccia vuota" |
| Più tracciati | Elenco con anteprima; si sceglie quali importare, ciascuno come percorso |
| Segmenti separati | Restano separati: non vengono uniti, e le statistiche non contano il vuoto fra loro |
| Quote assenti | Importazione comunque possibile; dislivello e profilo "non disponibili" |
| File grande | Lettura in streaming fuori dal thread dell'interfaccia; la mappa usa una versione semplificata |
| Punti duplicati consecutivi | Ignorati nel calcolo, conservati nei dati |

Il file originale è copiato nello spazio privato dell'app e non viene mai modificato. La semplificazione serve solo al disegno: statistiche ed esportazione usano i punti originali. Dopo l'importazione: anteprima su mappa, statistiche, avvisi, e le azioni "Salva nell'archivio" e "Associa a un'uscita".

## 5. Meteo

Dopo aver scelto luogo e date, l'app chiede le previsioni per i punti rappresentativi dell'uscita, in una sola richiesta: partenza, punto più alto del percorso e luogo del pernottamento, se distinti. Ogni previsione indica a quale punto e quota si riferisce.

Dati mostrati quando disponibili: condizioni, temperatura e temperatura percepita, probabilità e quantità di precipitazione, neve, vento, raffiche e direzione, alba e tramonto, ora dell'aggiornamento e fonte. Vista giornaliera e oraria.

- Data oltre l'orizzonte delle previsioni: "Previsioni non ancora disponibili", con il giorno da cui lo saranno.
- Dati climatici (medie del periodo): sezione separata, intitolata "Clima del periodo — non è una previsione".
- Cache: ogni risposta è salvata con data e ora. Senza rete si mostra l'ultimo dato con la sua età ("aggiornato 2 giorni fa").
- Errori, timeout e limiti del servizio non bloccano mai il salvataggio dell'uscita.
- Aggiornamento: all'apertura dell'uscita se il dato ha più di tre ore, e a richiesta. Un eventuale aggiornamento in background rispetta i vincoli di Android e non promette orari esatti.

## 6. Attrezzatura

### 6.1 Inventario

Oggetti riutilizzabili con: nome, peso in grammi, categoria, note, tipo (base, indossato, consumabile), uso funzionale (ad esempio "riparo", "sacco letto", "guscio antipioggia", "zaino"), volume in litri, capacità in litri per gli zaini, indicazione "peso stimato".

Ricerca per nome e categoria, filtri per categoria e tipo, creazione, modifica, eliminazione. Categorie iniziali: trasporto, ricambi, cucina e acqua, strati, notte, sicurezza; l'utente ne può aggiungere di proprie.

"Cerca il peso online" apre, solo su richiesta, una ricerca nel browser con il nome dell'oggetto: l'app non scarica né indovina pesi.

Il catalogo iniziale è facoltativo: gli oggetti aggiunti da lì hanno il peso marcato "stima tipica", ben riconoscibile e modificabile, finché l'utente non inserisce il proprio.

### 6.2 Checklist dello zaino

Ogni uscita ha la propria checklist. Per ogni riga: oggetto, quantità, tipo (modificabile per quell'uscita), note per quell'uscita, spunta "pronto". Raggruppamento per categoria con subtotali, avanzamento, azzeramento delle spunte, esportazione CSV.

**Pesi.** Per ogni riga `peso riga = peso unitario × quantità`. Ogni riga ha un solo tipo, quindi nessun peso è contato due volte.

- `Base` = somma delle righe di tipo base (zaino compreso).
- `Consumabili` = somma delle righe di tipo consumabile + acqua.
- `Acqua` = litri dichiarati × 1000 g. È una voce a sé: borracce e sacche sono oggetti con il loro peso a vuoto.
- `Indossato` = somma delle righe di tipo indossato.
- `Peso nello zaino` = Base + Consumabili.
- `Peso complessivo` = Base + Consumabili + Indossato.

Le righe senza peso valgono zero e fanno comparire "peso parziale: N oggetti senza peso".

**Indipendenza.** Spunte, quantità, note e tipo di una riga valgono solo per quell'uscita.

**Modifiche all'inventario.** Ogni riga conserva una copia di nome, peso, categoria, tipo e volume, più il collegamento all'oggetto.

- Modifica di un oggetto: le righe delle uscite in bozza o programmate si aggiornano; quelle delle uscite completate o annullate restano com'erano.
- Eliminazione di un oggetto: le righe restano con i dati copiati e la dicitura "non più in inventario".

## 7. Preparazione guidata

Considera distanza, salita, discesa e quota massima, mese e date, ritmo, ore di cammino al giorno, giorni e notti, pernottamento, acqua, zaino scelto e meteo disponibile.

Passi: (1) percorso e date, (2) ritmo, ore al giorno, pernottamento, acqua, con stima di durata, giorni e notti correggibili a mano, (3) cosa portare.

`giorni = arrotonda per eccesso (durata stimata / ore di cammino al giorno)`, `notti = giorni − 1`.

### 7.1 Suggerimenti

Regole dichiarate, eseguite sul telefono, senza servizi di intelligenza artificiale. Ogni suggerimento riporta il motivo.

| Regola | Condizione | Suggerimenti |
|---|---|---|
| Sempre | — | Kit primo soccorso, telo termico, frontale, zaino |
| Giorni | per giorno di cammino | Maglietta, intimo, calze; da 2 giorni: powerbank |
| Notte | almeno 1 notte | Ricambio per la notte; tenda o bivacco: sacco a pelo, materassino, fornello, pentolino, gas (1 cartuccia ogni 3 giorni); solo tenda: tenda; rifugio: sacco lenzuolo |
| Freddo | minima ≤ 5 °C | Pile, berretto, guanti; ≤ 0 °C: intimo termico, piumino |
| Ghiaccio | minima ≤ −2 °C in quota | Ramponcini |
| Pioggia | probabilità ≥ 30 % o previsioni incomplete su più giorni | Giacca impermeabile; ≥ 60 %: coprizaino e sovrapantaloni |
| Neve | neve prevista | Ghette |
| Vento | raffiche ≥ 40 km/h | Guscio antivento |
| Sole in quota | quota massima ≥ 2000 m | Crema solare, occhiali da sole |
| Acqua | dalle ore di cammino e dalla disponibilità dichiarata | Litri da portare e contenitori; con fonti naturali: filtro |

Le soglie sono costanti documentate nel codice e in questa tabella. Quando la temperatura viene dal clima del periodo e non da una previsione, il motivo lo dice ("minima tipica del periodo").

Per ogni suggerimento l'app distingue: già nella checklist, disponibile in inventario (con le alternative che hanno lo stesso uso funzionale), mancante. L'utente accetta o rifiuta ("non serve") ogni voce prima di applicarla. Le decisioni sono ricordate: rieseguire i suggerimenti non ripropone le voci rifiutate, non duplica quelle presenti e non tocca quantità e spunte.

Il volume stimato (somma dei volumi delle righe non indossate) è confrontato con la capacità dello zaino; gli oggetti senza volume sono contati a parte come "dati mancanti".

## 8. Dati, impostazioni, esportazioni

Tutto è salvato sul dispositivo. Senza rete funzionano uscite, inventario, checklist, tracce salvate, statistiche già calcolate e note. La mappa di sfondo senza rete è una cosa diversa: è disponibile solo per le aree scaricate esplicitamente.

Impostazioni: lingua (italiano predefinito, inglese), tema (chiaro, scuro, di sistema), unità di peso (g, hg, kg), soglia del dislivello, velocità per la stima della durata, servizi e chiavi facoltative, stato di dati e servizi, gestione della cache, privacy e attribuzioni.

**Backup completo.** Un file JSON con formato e numero di versione, che contiene inventario, categorie, percorsi (geometria e GPX originale), uscite, tappe, checklist, decisioni sui suggerimenti e impostazioni. Le chiavi dei servizi non sono incluse.

**Ripristino.** Validazione (formato, versione, riferimenti fra entità), anteprima con i conteggi, poi la scelta:

- *Unisci*: aggiunge ciò che manca; per gli elementi con lo stesso identificativo vince la modifica più recente; non elimina nulla.
- *Sostituisci*: prima salva un backup di sicurezza dello stato attuale, poi rimpiazza tutto.

**Altre esportazioni.** CSV dell'inventario e di una checklist, GPX di un percorso, condivisione tramite il foglio di condivisione di Android.

## 9. Accessibilità

Tema chiaro e scuro ad alto contrasto, testo ridimensionabile senza perdita di contenuto, descrizioni per TalkBack su ogni controllo, aree toccabili di almeno 48 dp, stati mai comunicati con il solo colore. I gesti sulla mappa hanno alternative a pulsante (elenco dei punti con sposta, inserisci, elimina).
